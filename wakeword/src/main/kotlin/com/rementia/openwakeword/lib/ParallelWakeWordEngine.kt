package com.rementia.openwakeword.lib

import android.content.Context
import com.rementia.openwakeword.lib.audio.AudioRecorder
import com.rementia.openwakeword.lib.model.*
import com.rementia.openwakeword.lib.ml.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import android.util.Log

/**
 * Fully parallel wake word detection engine.
 * Each model processes audio independently without blocking.
 */
class ParallelWakeWordEngine(
    private val context: Context,
    private val models: List<WakeWordModel>,
    private val detectionMode: DetectionMode = DetectionMode.SINGLE_BEST,
    private val detectionCooldownMs: Long = 2000L,
    private val maxWorkers: Int = 8,  // Increased for better parallelism
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    companion object {
        // Model requirements: 76 mel-spec frames = ~1.5 seconds
        private const val REQUIRED_AUDIO_SECONDS = 2.0
        private const val SAMPLE_RATE = 16000
        private const val BUFFER_SIZE_SAMPLES = (SAMPLE_RATE * REQUIRED_AUDIO_SECONDS).toInt()
        private const val TAG = "ParallelWakeWordEngine"
        private val IS_DEBUG = true // Force debug logging for development
    }

    private val audioRecorder = AudioRecorder(context)
    private val activeWorkers = AtomicInteger(0)
    
    // Shared components
    private val melSpectrogram = MelSpectrogram(context.assets)
    private val embeddingModel = EmbeddingModel(context.assets)
    
    // Per-model processors
    private val modelRunners = models.associate { model ->
        model to OnnxModelRunner(context.assets, model.modelPath)
    }
    
    // Audio buffer shared across all models
    private val audioRingBuffer = RingBuffer(BUFFER_SIZE_SAMPLES)
    
    // Detection output
    private val _detections = MutableSharedFlow<WakeWordDetection>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val detections: Flow<WakeWordDetection> = _detections.asSharedFlow()
    
    // Jobs
    private var audioJob: Job? = null
    private val modelJobs = mutableMapOf<WakeWordModel, Job>()
    
    /**
     * Start wake word detection with fully parallel processing.
     */
    fun start() {
        require(audioRecorder.hasRecordPermission()) { 
            "RECORD_AUDIO permission is required" 
        }
        
        // Reduce GC pressure by hinting to the runtime
        System.gc()
        if (IS_DEBUG) {
            Log.d(TAG, "Starting ParallelWakeWordEngine with ${models.size} models")
        }
        
        stop() // Clean up any existing jobs
        
        // Start audio collection
        audioJob = scope.launch {
            audioRecorder.startRecording()
                .collect { audioBuffer ->
                    // Just update the ring buffer, no blocking
                    audioRingBuffer.write(audioBuffer)
                }
        }
        
        // Start independent processing for each model
        models.forEach { model ->
            modelJobs[model] = scope.launch {
                processModelAsync(model)
            }
        }
    }
    
    /**
     * Process a single model completely asynchronously.
     */
    private suspend fun processModelAsync(model: WakeWordModel) {
        // Track last detection time for cooldown
        var lastDetectionTime = 0L
        
        // Pre-allocate buffer to avoid repeated allocations
        val audioBuffer = FloatArray(BUFFER_SIZE_SAMPLES)
        
        while (currentCoroutineContext().isActive) {
            // Skip if too many workers
            if (activeWorkers.get() >= maxWorkers) {
                delay(50) // Back off
                continue
            }
            
            // Get current audio snapshot
            val hasData = audioRingBuffer.readInto(audioBuffer)
            if (!hasData) {
                delay(100) // Not enough data yet
                continue
            }
            
            // Process in background without blocking
            activeWorkers.incrementAndGet()
            try {
                val score = withContext(Dispatchers.Default) {
                    processAudioForModel(audioBuffer, model)
                }
                
                // Debug logging - log all scores
                if (IS_DEBUG) {
                    Log.d(TAG, "${model.name} - Score: ${String.format("%.5f", score)}, Threshold: ${String.format("%.5f", model.threshold)}")
                }
                
                // Check detection
                if (score > model.threshold) {
                    val now = System.currentTimeMillis()
                    if (now - lastDetectionTime >= detectionCooldownMs) {
                        if (IS_DEBUG) {
                            Log.d(TAG, "DETECTION! ${model.name} - Score: ${String.format("%.5f", score)} > Threshold: ${String.format("%.5f", model.threshold)}")
                        }
                        handleDetection(model, score)
                        lastDetectionTime = now
                    } else {
                        if (IS_DEBUG) {
                            Log.d(TAG, "Detection skipped due to cooldown: ${model.name}")
                        }
                    }
                }
            } finally {
                activeWorkers.decrementAndGet()
            }
            
            // Slide window by ~50ms for better temporal resolution
            delay(50)
        }
    }
    
    /**
     * Process audio data for a specific model.
     */
    private fun processAudioForModel(audioData: FloatArray, model: WakeWordModel): Float {
        // Compute mel-spectrogram
        val melSpec = melSpectrogram.computeMelSpectrogram(audioData)
        
        // Extract features using sliding window
        val features = extractFeatures(melSpec)
        
        // Run model inference
        val modelRunner = modelRunners[model] ?: return 0f
        return modelRunner.predictWakeWord(features)
    }
    
    /**
     * Extract features with proper windowing for the model.
     */
    private fun extractFeatures(melSpec: Array<FloatArray>): Array<Array<FloatArray>> {
        val windowSize = 76
        val stepSize = 4  // Reduced from 8 for finer granularity
        val numFeatures = 16
        
        // Need enough frames for sliding windows
        if (melSpec.size < windowSize + (numFeatures - 1) * stepSize) {
            return arrayOf(Array(numFeatures) { FloatArray(96) { 0f } })
        }
        
        // Generate embeddings for sliding windows
        val embeddings = mutableListOf<FloatArray>()
        val maxStart = melSpec.size - windowSize
        
        for (start in (maxStart - (numFeatures - 1) * stepSize)..maxStart step stepSize) {
            if (start >= 0 && start + windowSize <= melSpec.size) {
                val window = melSpec.sliceArray(start until start + windowSize)
                val input = arrayOf(Array(windowSize) { i ->
                    Array(32) { j -> FloatArray(1) { window[i][j] } }
                })
                
                val embedding = embeddingModel.generateEmbeddings(input)
                embeddings.addAll(embedding)
            }
        }
        
        // Take last numFeatures embeddings
        return arrayOf(embeddings.takeLast(numFeatures).toTypedArray())
    }
    
    /**
     * Handle detection based on detection mode.
     */
    private suspend fun handleDetection(model: WakeWordModel, score: Float) {
        if (IS_DEBUG) {
            Log.d(TAG, "Emitting detection for ${model.name} with score ${String.format("%.5f", score)}")
        }
        
        when (detectionMode) {
            DetectionMode.ALL -> {
                // Emit immediately
                val emitted = _detections.tryEmit(WakeWordDetection(model, score))
                if (IS_DEBUG) {
                    Log.d(TAG, "Detection emitted in ALL mode: $emitted")
                }
                if (!emitted) {
                    Log.w(TAG, "Failed to emit detection - buffer full?")
                }
            }
            DetectionMode.SINGLE_BEST -> {
                // Store and compare (simplified for now)
                val emitted = _detections.tryEmit(WakeWordDetection(model, score))
                if (IS_DEBUG) {
                    Log.d(TAG, "Detection emitted in SINGLE_BEST mode: $emitted")
                }
                if (!emitted) {
                    Log.w(TAG, "Failed to emit detection - buffer full?")
                }
            }
        }
    }
    
    /**
     * Stop all processing.
     */
    fun stop() {
        audioJob?.cancel()
        audioJob = null
        
        modelJobs.values.forEach { it.cancel() }
        modelJobs.clear()
    }
    
    /**
     * Release all resources.
     */
    fun release() {
        stop()
        
        melSpectrogram.close()
        embeddingModel.close()
        modelRunners.values.forEach { it.close() }
        
        scope.cancel()
    }
}

/**
 * Simple ring buffer for audio data.
 */
private class RingBuffer(private val capacity: Int) {
    private val buffer = FloatArray(capacity)
    private var writePos = 0
    private val lock = Any()
    
    fun write(data: FloatArray) = synchronized(lock) {
        for (sample in data) {
            buffer[writePos] = sample
            writePos = (writePos + 1) % capacity
        }
    }
    
    fun read(): FloatArray = synchronized(lock) {
        // Return a snapshot starting from oldest data
        val result = FloatArray(capacity)
        val startPos = writePos
        
        for (i in 0 until capacity) {
            result[i] = buffer[(startPos + i) % capacity]
        }
        
        return result
    }
    
    fun readInto(target: FloatArray): Boolean = synchronized(lock) {
        if (target.size != capacity) return false
        
        val startPos = writePos
        for (i in 0 until capacity) {
            target[i] = buffer[(startPos + i) % capacity]
        }
        
        return true
    }
}