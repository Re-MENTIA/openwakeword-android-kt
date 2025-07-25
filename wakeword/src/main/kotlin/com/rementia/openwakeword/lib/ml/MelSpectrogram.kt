package com.rementia.openwakeword.lib.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.res.AssetManager
import java.nio.FloatBuffer

/**
 * Handles mel-spectrogram computation using ONNX model.
 */
internal class MelSpectrogram(
    private val assetManager: AssetManager
) : AutoCloseable {
    
    companion object {
        private const val MEL_SPECTROGRAM_MODEL = "melspectrogram.onnx"
        private const val BATCH_SIZE = 1
    }
    
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    
    /**
     * Compute mel-spectrogram from audio samples.
     * 
     * @param audioSamples Float array of audio samples
     * @return 2D array representing mel-spectrogram
     */
    fun computeMelSpectrogram(audioSamples: FloatArray): Array<FloatArray> {
        var session: OrtSession? = null
        var inputTensor: OnnxTensor? = null
        
        return try {
            assetManager.open(MEL_SPECTROGRAM_MODEL).use { inputStream ->
                val modelBytes = inputStream.readBytes()
                session = env.createSession(modelBytes)
            }
            
            val floatBuffer = FloatBuffer.wrap(audioSamples)
            inputTensor = OnnxTensor.createTensor(
                env, 
                floatBuffer, 
                longArrayOf(BATCH_SIZE.toLong(), audioSamples.size.toLong())
            )
            
            session!!.run(mapOf(session!!.inputNames.first() to inputTensor)).use { results ->
                val outputTensor = results[0].value as Array<Array<Array<FloatArray>>>
                
                // Squeeze dimensions and apply transform
                val squeezed = squeeze(outputTensor)
                applyMelSpecTransform(squeezed)
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to compute mel-spectrogram", e)
        } finally {
            inputTensor?.close()
            session?.close()
        }
    }
    
    private fun squeeze(originalArray: Array<Array<Array<FloatArray>>>): Array<FloatArray> {
        val squeezed = Array(originalArray[0][0].size) { i ->
            FloatArray(originalArray[0][0][0].size) { j ->
                originalArray[0][0][i][j]
            }
        }
        return squeezed
    }
    
    private fun applyMelSpecTransform(array: Array<FloatArray>): Array<FloatArray> {
        return Array(array.size) { i ->
            FloatArray(array[i].size) { j ->
                array[i][j] / 10.0f + 2.0f
            }
        }
    }
    
    override fun close() {
        // env is managed globally by OrtEnvironment
    }
}