package com.rementia.openwakeword.lib.model

/**
 * Represents a wake word model configuration.
 * 
 * @property name The display name of the wake word (e.g., "Hey Nugget", "Computer")
 * @property modelPath The path to the ONNX model file in assets
 * @property threshold The detection threshold (0.0 to 1.0)
 */
data class WakeWordModel(
    val name: String,
    val modelPath: String,
    val threshold: Float = 0.5f
)