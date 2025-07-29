package com.rementia.openwakeword.lib

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.rementia.openwakeword.lib.audio.AudioProcessor
import com.rementia.openwakeword.lib.audio.AudioRecorder
import com.rementia.openwakeword.lib.ml.OnnxModelRunner
import com.rementia.openwakeword.lib.model.WakeWordDetection
import com.rementia.openwakeword.lib.model.WakeWordModel
import com.rementia.openwakeword.lib.model.WakeWordScore
import com.rementia.openwakeword.lib.model.DetectionMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Main entry point for wake word detection using ONNX Runtime.
 * 
 * This class manages multiple wake word models and emits detection events through a Kotlin Flow.
 * It provides real-time audio processing with configurable detection modes and cooldown periods.
 * 
 * ## Basic Usage
 * 
 * ```kotlin
 * // Configure wake word models
 * val models = listOf(
 *     WakeWordModel("Hey Assistant", "hey_assistant.onnx", threshold = 0.05f),
 *     WakeWordModel("OK Computer", "ok_computer.onnx", threshold = 0.08f)
 * )
 * 
 * // Create engine
 * val engine = WakeWordEngine(
 *     context = context,
 *     models = models,
 *     detectionMode = DetectionMode.SINGLE_BEST,
 *     detectionCooldownMs = 2000L
 * )
 * 
 * // Start detection
 * engine.start()
 * 
 * // Collect detection events
 * lifecycleScope.launch {
 *     engine.detections.collect { detection ->
 *         Log.d("WakeWord", "Detected: ${detection.model.name} (score: ${detection.score})")
 *     }
 * }
 * 
 * // Clean up when done
 * override fun onDestroy() {
 *     super.onDestroy()
 *     engine.release()
 * }
 * ```
 * 
 * ## Requirements
 * - Android 6.0+ (API 23+)
 * - RECORD_AUDIO permission
 * - ONNX model files in assets directory
 * 
 * @property context Android context for accessing resources and assets
 * @property models List of wake word models to detect. At least one model is required.
 * @property detectionMode Mode for handling multiple simultaneous detections. See [DetectionMode]
 * @property detectionCooldownMs Cooldown period in milliseconds to prevent duplicate detections. Set to 0 to disable cooldown.
 * @property scope CoroutineScope for background operations. Defaults to Dispatchers.Default for optimal performance.
 * 
 * @constructor Creates a new wake word detection engine
 * @throws IllegalArgumentException if models list is empty
 * 
 * @see WakeWordModel
 * @see WakeWordDetection
 * @see DetectionMode
 */
class WakeWordEngine(
    private val context: Context,
    private val models: List<WakeWordModel>,
    private val detectionMode: DetectionMode = DetectionMode.SINGLE_BEST,
    private val detectionCooldownMs: Long = 2000L,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    
    companion object {
        private const val TAG = "WakeWordEngine"
    }
    
    private val assetManager: AssetManager = context.assets
    private val audioRecorder = AudioRecorder(context)
    private val modelProcessors = mutableMapOf<WakeWordModel, ModelProcessor>()
    private val detectionCooldowns = mutableMapOf<String, Long>()
    
    private val _detections = MutableSharedFlow<WakeWordDetection>()
    private val _scores = MutableSharedFlow<WakeWordScore>()
    
    /**
     * Flow of wake word detection events.
     * 
     * This Flow emits [WakeWordDetection] objects whenever a wake word is detected.
     * The Flow is hot and shared, meaning multiple collectors will receive the same events.
     * 
     * ## Example: Basic Collection
     * ```kotlin
     * engine.detections.collect { detection ->
     *     showToast("${detection.model.name} detected!")
     * }
     * ```
     * 
     * ## Example: Filtering High-Confidence Detections
     * ```kotlin
     * engine.detections
     *     .filter { it.score > 0.8f }
     *     .collect { detection ->
     *         // Only process high-confidence detections
     *     }
     * ```
     * 
     * ## Example: Debouncing Rapid Detections
     * ```kotlin
     * engine.detections
     *     .debounce(500) // Additional debounce on top of cooldown
     *     .collect { detection ->
     *         // Process debounced detections
     *     }
     * ```
     */
    val detections: Flow<WakeWordDetection> = _detections.asSharedFlow()
    
    /**
     * Flow of real-time wake word scores.
     * 
     * This Flow emits [WakeWordScore] objects continuously for all models,
     * regardless of whether they exceed the detection threshold.
     * Useful for real-time monitoring and visualization.
     */
    val scores: Flow<WakeWordScore> = _scores.asSharedFlow()
    
    private var recordingJob: Job? = null
    
    init {
        require(models.isNotEmpty()) { "At least one wake word model must be provided" }
        initializeModels()
    }
    
    private fun initializeModels() {
        models.forEach { model ->
            val processor = ModelProcessor(assetManager, model)
            modelProcessors[model] = processor
        }
    }
    
    /**
     * Starts wake word detection.
     * 
     * This method begins audio recording and real-time wake word detection.
     * Detection events will be emitted through the [detections] Flow.
     * 
     * ## Requirements
     * - RECORD_AUDIO permission must be granted
     * - Audio recording hardware must be available
     * 
     * ## Example
     * ```kotlin
     * // Check permission first
     * if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
     *     == PackageManager.PERMISSION_GRANTED) {
     *     engine.start()
     * } else {
     *     // Request permission
     * }
     * ```
     * 
     * @throws IllegalStateException if RECORD_AUDIO permission is not granted
     * @throws RuntimeException if audio recording fails to initialize
     * 
     * @see stop
     * @see detections
     */
    fun start() {
        require(audioRecorder.hasRecordPermission()) { 
            "RECORD_AUDIO permission is required for wake word detection" 
        }
        
        recordingJob?.cancel()
        recordingJob = scope.launch {
            audioRecorder.startRecording()
                .collect { audioBuffer ->
                    // Process all models in parallel and collect results
                    val detectionResults = models.mapIndexed { index, model ->
                        async {
                            try {
                                val processor = modelProcessors[model]!!
                                val score = processor.process(audioBuffer)
                                Log.d(TAG, "${model.name} - Score: ${String.format("%.5f", score)}, Threshold: ${String.format("%.5f", model.threshold)}")
                                
                                // Emit real-time score
                                _scores.emit(WakeWordScore(model, score))
                                
                                if (score > model.threshold) {
                                    Log.d(TAG, "DETECTION! ${model.name} - Score: ${String.format("%.5f", score)} > Threshold: ${String.format("%.5f", model.threshold)}")
                                    DetectionResult(
                                        model = model,
                                        score = score,
                                        difference = score - model.threshold,
                                        index = index
                                    )
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing model ${model.name}", e)
                                e.printStackTrace()
                                null
                            }
                        }
                    }.awaitAll().filterNotNull()
                    
                    // Process results based on detection mode
                    when (detectionMode) {
                        DetectionMode.SINGLE_BEST -> {
                            // Select the best detection based on score-threshold difference
                            detectionResults.maxByOrNull { result ->
                                // Primary: difference, Secondary: inverse index (lower index = higher priority)
                                result.difference * 1000 - result.index * 0.001
                            }?.let { result ->
                                emitDetection(result.model, result.score)
                            }
                        }
                        DetectionMode.ALL -> {
                            // Emit all detections that passed threshold
                            detectionResults.forEach { result ->
                                emitDetection(result.model, result.score)
                            }
                        }
                    }
                }
        }
    }
    
    /**
     * Emits a detection event with cooldown check.
     * 
     * This internal method handles the cooldown logic to prevent duplicate detections
     * within the configured cooldown period.
     * 
     * @param model The wake word model that triggered the detection
     * @param score The confidence score of the detection
     */
    private suspend fun emitDetection(model: WakeWordModel, score: Float) {
        val now = System.currentTimeMillis()
        val lastDetection = detectionCooldowns[model.name]
        
        if (lastDetection == null || detectionCooldownMs == 0L || now - lastDetection >= detectionCooldownMs) {
            Log.d(TAG, "Emitting detection for ${model.name} with score ${String.format("%.5f", score)}")
            _detections.emit(
                WakeWordDetection(
                    model = model,
                    score = score
                )
            )
            detectionCooldowns[model.name] = now
        } else {
            Log.d(TAG, "Detection skipped due to cooldown: ${model.name}")
        }
    }
    
    /**
     * Stops wake word detection.
     * 
     * This method stops audio recording and cancels all ongoing detection processing.
     * The engine can be restarted by calling [start] again.
     * 
     * ## Example
     * ```kotlin
     * override fun onPause() {
     *     super.onPause()
     *     engine.stop() // Stop detection when app goes to background
     * }
     * ```
     * 
     * @see start
     */
    fun stop() {
        recordingJob?.cancel()
        recordingJob = null
    }
    
    /**
     * Releases all resources used by the engine.
     * 
     * This method should be called when the engine is no longer needed to free up memory
     * and system resources. After calling this method, the engine cannot be reused.
     * 
     * ## Important
     * Always call this method in your Activity/Fragment's onDestroy() to prevent memory leaks.
     * 
     * ## Example
     * ```kotlin
     * override fun onDestroy() {
     *     super.onDestroy()
     *     wakeWordEngine?.release()
     * }
     * ```
     * 
     * This method will:
     * - Stop any ongoing detection
     * - Release ONNX Runtime sessions
     * - Free audio processing resources
     * - Clear internal caches
     */
    fun release() {
        stop()
        modelProcessors.values.forEach { it.close() }
        modelProcessors.clear()
    }
    
    /**
     * Internal data class for detection results with metadata.
     */
    private data class DetectionResult(
        val model: WakeWordModel,
        val score: Float,
        val difference: Float,
        val index: Int
    )
    
    /**
     * Internal class to process audio for a specific model.
     */
    private inner class ModelProcessor(
        assetManager: AssetManager,
        private val model: WakeWordModel
    ) : AutoCloseable {
        
        private val modelRunner = OnnxModelRunner(assetManager, model.modelPath)
        private val audioProcessor = AudioProcessor(assetManager, modelRunner)
        
        fun process(audioBuffer: FloatArray): Float {
            return audioProcessor.predictWakeWord(audioBuffer)
        }
        
        override fun close() {
            audioProcessor.close()
            modelRunner.close()
        }
    }
}