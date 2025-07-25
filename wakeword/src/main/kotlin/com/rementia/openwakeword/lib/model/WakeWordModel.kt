package com.rementia.openwakeword.lib.model

/**
 * Configuration for a wake word model.
 * 
 * This data class defines the parameters for a single wake word that the engine will detect.
 * Each model represents a unique wake word or phrase that can trigger detection events.
 * 
 * ## Model Files
 * 
 * Model files should be placed in the `assets` directory of your Android app:
 * ```
 * app/src/main/assets/
 * ├── hello_world.onnx
 * ├── hey_assistant.onnx
 * └── wake_word_models/
 *     └── custom_model.onnx
 * ```
 * 
 * ## Threshold Guidelines
 * 
 * The threshold value controls the sensitivity of wake word detection:
 * - **Lower values (0.01-0.1)**: More sensitive, may have false positives
 * - **Medium values (0.1-0.3)**: Balanced sensitivity and accuracy
 * - **Higher values (0.3-0.5)**: More accurate, may miss some detections
 * 
 * ## Examples
 * 
 * ```kotlin
 * // Single wake word with default threshold
 * val model = WakeWordModel(
 *     name = "Hey Assistant",
 *     modelPath = "hey_assistant.onnx"
 * )
 * 
 * // Custom threshold for noisy environment
 * val strictModel = WakeWordModel(
 *     name = "Computer",
 *     modelPath = "computer.onnx",
 *     threshold = 0.3f // Higher threshold for accuracy
 * )
 * 
 * // Model in subdirectory
 * val customModel = WakeWordModel(
 *     name = "Custom Wake Word",
 *     modelPath = "models/custom.onnx",
 *     threshold = 0.08f
 * )
 * ```
 * 
 * ## Performance Tips
 * 
 * - Keep model files under 1MB for optimal loading time
 * - Use lower thresholds (0.05-0.1) for quiet environments
 * - Use higher thresholds (0.2-0.4) for noisy environments
 * - Test thoroughly on target devices to find optimal threshold
 * 
 * @property name Human-readable name for the wake word. Used in detection events and logging.
 * @property modelPath Path to the ONNX model file relative to the assets directory.
 * @property threshold Detection threshold between 0.0 and 1.0. Detections with scores above this value will trigger events. Default is 0.5f.
 * 
 * @constructor Creates a wake word model configuration
 */
data class WakeWordModel(
    val name: String,
    val modelPath: String,
    val threshold: Float = 0.5f
)