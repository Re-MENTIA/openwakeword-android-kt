package com.rementia.openwakeword.lib.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.res.AssetManager
import android.util.Log
import java.io.IOException
import java.nio.FloatBuffer

/**
 * Handles ONNX model loading and inference for wake word detection.
 */
internal class OnnxModelRunner(
    private val assetManager: AssetManager,
    private val modelPath: String
) : AutoCloseable {
    
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession = createSession()
    
    companion object {
        private const val TAG = "OnnxModelRunner"
        private const val BATCH_SIZE = 1
    }
    
    private fun createSession(): OrtSession {
        return try {
            assetManager.open(modelPath).use { inputStream ->
                val modelBytes = inputStream.readBytes()
                env.createSession(modelBytes)
            }
        } catch (e: IOException) {
            throw RuntimeException("Failed to load model: $modelPath", e)
        }
    }
    
    /**
     * Run inference on the wake word detection model.
     * 
     * @param inputArray 3D float array of shape [1, features, embeddings]
     * @return Prediction score between 0.0 and 1.0
     */
    fun predictWakeWord(inputArray: Array<Array<FloatArray>>): Float {
        var inputTensor: OnnxTensor? = null
        
        return try {
            inputTensor = OnnxTensor.createTensor(env, inputArray)
            
            session.run(mapOf(session.inputNames.first() to inputTensor)).use { outputs ->
                val result = outputs[0].value as Array<FloatArray>
                val score = result[0][0]
                Log.d(TAG, "Model: $modelPath - Raw inference output: ${String.format("%.5f", score)}")
                score
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to run inference", e)
        } finally {
            inputTensor?.close()
        }
    }
    
    override fun close() {
        session.close()
    }
}