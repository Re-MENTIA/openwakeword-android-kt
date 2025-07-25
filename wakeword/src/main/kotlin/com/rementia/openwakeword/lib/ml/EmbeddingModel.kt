package com.rementia.openwakeword.lib.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.res.AssetManager

/**
 * Handles embedding generation from mel-spectrograms using ONNX model.
 */
internal class EmbeddingModel(
    private val assetManager: AssetManager
) : AutoCloseable {
    
    companion object {
        private const val EMBEDDING_MODEL = "embedding_model.onnx"
    }
    
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    
    /**
     * Generate embeddings from mel-spectrogram windows.
     * 
     * @param input 4D array of shape [batch, height, width, channels]
     * @return 2D array of embeddings
     */
    fun generateEmbeddings(input: Array<Array<Array<FloatArray>>>): Array<FloatArray> {
        var session: OrtSession? = null
        var inputTensor: OnnxTensor? = null
        
        return try {
            assetManager.open(EMBEDDING_MODEL).use { inputStream ->
                val modelBytes = inputStream.readBytes()
                session = env.createSession(modelBytes)
            }
            
            inputTensor = OnnxTensor.createTensor(env, input)
            
            session!!.run(mapOf("input_1" to inputTensor)).use { results ->
                val rawOutput = results[0].value as Array<Array<Array<FloatArray>>>
                
                // Reshape from (41, 1, 1, 96) to (41, 96)
                Array(rawOutput.size) { i ->
                    rawOutput[i][0][0].copyOf()
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to generate embeddings", e)
        } finally {
            inputTensor?.close()
            session?.close()
        }
    }
    
    override fun close() {
        // env is managed globally by OrtEnvironment
    }
}