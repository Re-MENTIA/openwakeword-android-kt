# OpenWakeWord Android Kotlin Library

A Kotlin library for wake word detection on Android using ONNX Runtime. This library provides a clean, coroutine-based API for detecting wake words in real-time audio streams.

## Features

- 100% Kotlin implementation
- Coroutine-based architecture for efficient background processing
- Support for multiple wake word models with intelligent detection modes
- SINGLE_BEST mode: Automatically selects the most confident detection
- ALL mode: Process multiple wake words simultaneously for complex interactions
- Easy-to-use API with Flow-based detection events
- ONNX Runtime 1.18.0 for efficient on-device inference
- Configurable detection cooldown to prevent duplicate notifications
- Minimal dependencies and clean architecture

## Requirements

- Android SDK 23+ (Android 6.0 Marshmallow)
- RECORD_AUDIO permission

## Installation

### MavenLocal

First, publish the library to your local Maven repository:

```bash
./gradlew :wakeword:publishToMavenLocal
```

Then add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.rementia:openwakeword:0.1.0")
}
```

## Usage

For detailed library documentation and important usage notes, see the [library README](wakeword/README.md).

### Basic Setup

```kotlin
// Configure wake word models
// TIP: Lower threshold = more sensitive, but may cause false positives
// TIP: Higher threshold = more accurate, but may miss some detections
val models = listOf(
    WakeWordModel(
        name = "Hey Nugget",
        modelPath = "hey_nugget_new.onnx",  // Place model files in app/src/main/assets/
        threshold = 0.05f                    // Recommended: 0.03-0.1 for most models
    )
)

// Create the engine with configuration
val wakeWordEngine = WakeWordEngine(
    context = context,
    models = models,
    detectionMode = DetectionMode.SINGLE_BEST,  // Only emit the most confident detection
    detectionCooldownMs = 2000L                 // Prevents multiple toasts for single utterance
)

// Alternative: Use ALL mode for multi-command systems
val multiCommandEngine = WakeWordEngine(
    context = context,
    models = models,
    detectionMode = DetectionMode.ALL,  // Emit all detections above threshold
    detectionCooldownMs = 500L          // Shorter cooldown for rapid commands
)

// Listen for wake word detections
// IMPORTANT: This runs indefinitely, so manage the coroutine lifecycle properly
lifecycleScope.launch {
    wakeWordEngine.detections.collect { detection ->
        // This is called when wake word score exceeds threshold
        println("Wake word detected: ${detection.model.name} (score: ${detection.score})")
        
        // TIP: Use the score to show confidence in UI
        // Scores typically range from 0.0 to 1.0
        when {
            detection.score > 0.8f -> println("Very confident detection!")
            detection.score > 0.6f -> println("Good detection")
            else -> println("Marginal detection - consider adjusting threshold")
        }
    }
}

// Start detection (requires RECORD_AUDIO permission)
// TIP: Check permission before calling start()
if (hasAudioPermission()) {
    wakeWordEngine.start()
}

// Stop detection when not needed to save battery
// IMPORTANT: Always stop when your activity/fragment is paused
override fun onPause() {
    super.onPause()
    wakeWordEngine.stop()
}

// Release resources when completely done
// IMPORTANT: Call this in onDestroy() to prevent memory leaks
override fun onDestroy() {
    super.onDestroy()
    wakeWordEngine.release()
}
```

### Required Models

Place the following ONNX model files in your app's `assets` directory:

- `hey_nugget_new.onnx` - Wake word detection model
- `melspectrogram.onnx` - Mel-spectrogram computation model
- `embedding_model.onnx` - Feature embedding model

## API Reference

### WakeWordEngine

The main entry point for wake word detection.

```kotlin
class WakeWordEngine(
    context: Context,
    models: List<WakeWordModel>,
    detectionMode: DetectionMode = DetectionMode.SINGLE_BEST,
    detectionCooldownMs: Long = 2000L,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
)
```

**Properties:**
- `detections: Flow<WakeWordDetection>` - Flow of detection events

**Methods:**
- `start()` - Start wake word detection
- `stop()` - Stop wake word detection
- `release()` - Release all resources

### WakeWordModel

Configuration for a wake word model.

```kotlin
data class WakeWordModel(
    val name: String,
    val modelPath: String,
    val threshold: Float = 0.5f
)
```

### WakeWordDetection

Detection event data.

```kotlin
data class WakeWordDetection(
    val model: WakeWordModel,
    val score: Float,
    val timestamp: Long = System.currentTimeMillis()
)
```

## Building from Source

```bash
# Clone the repository
git clone https://github.com/rementia/openwakeword-android-kt.git
cd openwakeword-android-kt

# Build the project
./gradlew build

# Run tests
./gradlew test

# Build and install sample app
./gradlew :app:installDebug
```

## Sample App

The repository includes a sample app demonstrating the library usage. The app:
- Requests microphone permission
- Displays real-time detection scores
- Shows toast notifications when wake words are detected

## License

```
Copyright 2024 Rementia

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Acknowledgments

This library is based on the original [OpenWakeWord for Android](https://github.com/hasanatlodhi/OpenwakewordforAndroid) implementation by hasanatlodhi. We've ported it to Kotlin and restructured it as a reusable library with a modern API.

### Model Attribution

This project uses models from the [OpenWakeWord](https://github.com/dscripka/openWakeWord) project by David Scripka:
- `melspectrogram.onnx` - Audio preprocessing model (Apache 2.0 License)
- `embedding_model.onnx` - Speech embedding model (Apache 2.0 License, originally from Google)

The embedding model is based on Google's speech embedding model released under Apache 2.0 License.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.