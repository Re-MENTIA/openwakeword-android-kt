package com.rementia.openwakeword.lib.model

/**
 * Represents a wake word detection event.
 * 
 * @property model The model that detected the wake word
 * @property score The confidence score of the detection (0.0 to 1.0)
 * @property timestamp The timestamp when the detection occurred
 */
data class WakeWordDetection(
    val model: WakeWordModel,
    val score: Float,
    val timestamp: Long = System.currentTimeMillis()
)