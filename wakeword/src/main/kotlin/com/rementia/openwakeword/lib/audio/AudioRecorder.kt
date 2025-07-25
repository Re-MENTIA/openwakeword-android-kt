package com.rementia.openwakeword.lib.audio

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import android.content.Context
import kotlin.coroutines.coroutineContext
import android.annotation.SuppressLint

/**
 * Handles audio recording from device microphone.
 * Emits audio buffers as a Flow for processing.
 */
internal class AudioRecorder(
    private val context: Context
) {
    
    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val BUFFER_SIZE_IN_SHORTS = 1280
    }
    
    /**
     * Check if audio recording permission is granted.
     */
    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Start recording audio and emit buffers as Flow.
     * 
     * @return Flow of float arrays containing audio samples
     */
    @SuppressLint("MissingPermission")
    fun startRecording(): Flow<FloatArray> = flow {
        require(hasRecordPermission()) { "RECORD_AUDIO permission not granted" }
        
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = maxOf(minBufferSize, BUFFER_SIZE_IN_SHORTS * 2)
        
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )
        
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("Failed to initialize AudioRecord")
        }
        
        val audioBuffer = ShortArray(BUFFER_SIZE_IN_SHORTS)
        
        try {
            audioRecord.startRecording()
            
            while (coroutineContext.isActive) {
                val readCount = audioRecord.read(audioBuffer, 0, audioBuffer.size)
                
                if (readCount > 0) {
                    // Convert short array to float array (normalize to -1.0 to 1.0)
                    val floatBuffer = FloatArray(readCount) { i ->
                        audioBuffer[i] / 32768.0f
                    }
                    emit(floatBuffer)
                }
            }
        } finally {
            if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop()
            }
            audioRecord.release()
        }
    }.flowOn(Dispatchers.IO)
}