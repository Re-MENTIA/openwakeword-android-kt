# OpenWakeWord Android Library

A Kotlin library for on-device wake word detection using ONNX Runtime.

## Usage

### Basic Setup

```kotlin
// Initialize with wake word models
val models = listOf(
    WakeWordModel(
        name = "Hello World",
        modelPath = "hello_world.onnx",
        threshold = 0.5f
    )
)

val wakeWordEngine = WakeWordEngine(
    context = context,
    models = models
)

// Start detection
wakeWordEngine.start()

// Listen for detections
wakeWordEngine.detections.collect { detection ->
    println("Wake word detected: ${detection.model.name} (score: ${detection.score})")
}

// Stop when done
wakeWordEngine.stop()
wakeWordEngine.release()
```

### Important Usage Notes

#### Coroutine Scope and Threading

**⚠️ DO NOT pass a UI-bound CoroutineScope to WakeWordEngine**

```kotlin
// ❌ WRONG - This will freeze your UI!
@Composable
fun MyScreen() {
    val scope = rememberCoroutineScope() // This runs on Main dispatcher
    val engine = WakeWordEngine(
        context = context,
        models = models,
        scope = scope  // DON'T DO THIS!
    )
}

// ✅ CORRECT - Let WakeWordEngine use its default scope
@Composable
fun MyScreen() {
    val engine = WakeWordEngine(
        context = context,
        models = models
        // Uses Dispatchers.Default internally
    )
}
```

The WakeWordEngine performs heavy operations including:
- Continuous audio recording and processing
- Real-time ML model inference
- Signal processing (FFT, mel-spectrogram generation)

These operations MUST run on background threads. The library handles this automatically
when you don't override the default scope.

### Architecture

The library uses a three-stage processing pipeline:
1. **Audio Recording** - Records audio at 16kHz mono
2. **Feature Extraction** - Converts audio to mel-spectrograms
3. **Model Inference** - Runs ONNX models to detect wake words

All stages run on appropriate background dispatchers:
- Audio recording: `Dispatchers.IO`
- Processing & inference: `Dispatchers.Default`

### Models

Place your ONNX model files in your app's assets folder. The library expects:
- Wake word detection models (e.g., `hello_world.onnx`)
- Optional: melspectrogram.onnx and embedding_model.onnx for advanced processing

### Permissions

Add to your AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

Request the permission at runtime before calling `wakeWordEngine.start()`.

## API Reference

### WakeWordEngine

Main entry point for wake word detection.

```kotlin
class WakeWordEngine(
    context: Context,
    models: List<WakeWordModel>,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
)
```

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

Detection event emitted when a wake word is recognized.

```kotlin
data class WakeWordDetection(
    val model: WakeWordModel,
    val score: Float,
    val timestamp: Long = System.currentTimeMillis()
)
```