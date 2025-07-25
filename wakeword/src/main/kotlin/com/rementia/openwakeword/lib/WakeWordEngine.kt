package com.rementia.openwakeword.lib

import android.content.Context
import android.content.res.AssetManager
import com.rementia.openwakeword.lib.audio.AudioProcessor
import com.rementia.openwakeword.lib.audio.AudioRecorder
import com.rementia.openwakeword.lib.ml.OnnxModelRunner
import com.rementia.openwakeword.lib.model.WakeWordDetection
import com.rementia.openwakeword.lib.model.WakeWordModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Main entry point for wake word detection.
 * Manages multiple wake word models and emits detection events.
 * 
 * @property context Android context for accessing resources
 * @property models List of wake word models to detect
 * @property detectionCooldownMs Cooldown period in milliseconds to prevent duplicate detections (0 to disable)
 * @property scope CoroutineScope for background operations
 */
class WakeWordEngine(
    private val context: Context,
    private val models: List<WakeWordModel>,
    private val detectionCooldownMs: Long = 2000L,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    
    private val assetManager: AssetManager = context.assets
    private val audioRecorder = AudioRecorder(context)
    private val modelProcessors = mutableMapOf<WakeWordModel, ModelProcessor>()
    private val detectionCooldowns = mutableMapOf<String, Long>()
    
    private val _detections = MutableSharedFlow<WakeWordDetection>()
    
    /**
     * Flow of wake word detection events.
     */
    val detections: Flow<WakeWordDetection> = _detections.asSharedFlow()
    
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
     * Start wake word detection.
     * Requires RECORD_AUDIO permission.
     */
    fun start() {
        require(audioRecorder.hasRecordPermission()) { 
            "RECORD_AUDIO permission is required for wake word detection" 
        }
        
        recordingJob?.cancel()
        recordingJob = scope.launch {
            audioRecorder.startRecording()
                .collect { audioBuffer ->
                    modelProcessors.entries.map { (model, processor) ->
                        async {
                            try {
                                val score = processor.process(audioBuffer)
                                if (score > model.threshold) {
                                    val now = System.currentTimeMillis()
                                    val lastDetection = detectionCooldowns[model.name] ?: 0L
                                    
                                    if (detectionCooldownMs == 0L || now - lastDetection >= detectionCooldownMs) {
                                        _detections.emit(
                                            WakeWordDetection(
                                                model = model,
                                                score = score
                                            )
                                        )
                                        detectionCooldowns[model.name] = now
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }.awaitAll()
                }
        }
    }
    
    /**
     * Stop wake word detection.
     */
    fun stop() {
        recordingJob?.cancel()
        recordingJob = null
    }
    
    /**
     * Release all resources.
     * Call this when done using the engine.
     */
    fun release() {
        stop()
        modelProcessors.values.forEach { it.close() }
        modelProcessors.clear()
    }
    
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