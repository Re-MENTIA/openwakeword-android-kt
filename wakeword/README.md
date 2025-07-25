# OpenWakeWord Android Library

A Kotlin library for on-device wake word detection using ONNX Runtime.

## Usage

### Basic Setup

```kotlin
// Step 1: Configure wake word models
// Each model can have different thresholds based on its characteristics
val models = listOf(
    WakeWordModel(
        name = "Hello World",
        modelPath = "hello_world.onnx",  // Path relative to assets folder
        threshold = 0.5f                 // Higher threshold for demo model
    ),
    // TIP: You can add multiple wake words simultaneously
    WakeWordModel(
        name = "Hey Assistant",
        modelPath = "hey_assistant.onnx",
        threshold = 0.08f               // Production models often need lower thresholds
    )
)

// Step 2: Create the engine with appropriate configuration
val wakeWordEngine = WakeWordEngine(
    context = context,
    models = models,
    detectionCooldownMs = 2000L  // 2 second cooldown prevents duplicate notifications
)

// Step 3: Start detection (after checking permissions!)
try {
    wakeWordEngine.start()
} catch (e: IllegalStateException) {
    // Handle missing RECORD_AUDIO permission
    Log.e("WakeWord", "Missing audio permission", e)
}

// Step 4: Collect detection events in a coroutine
// TIP: Use lifecycleScope in Activities/Fragments for automatic cancellation
lifecycleScope.launch {
    wakeWordEngine.detections.collect { detection ->
        // Called whenever a wake word exceeds its threshold
        Log.d("WakeWord", "${detection.model.name} detected with score ${detection.score}")
        
        // TIP: Different actions for different wake words
        when (detection.model.name) {
            "Hello World" -> startMainFlow()
            "Hey Assistant" -> startAssistantMode()
        }
    }
}

// Step 5: Properly manage lifecycle
// IMPORTANT: Stop detection when app goes to background
override fun onPause() {
    super.onPause()
    wakeWordEngine.stop()  // Stops audio recording
}

override fun onResume() {
    super.onResume()
    if (hasAudioPermission()) {
        wakeWordEngine.start()  // Resume detection
    }
}

// Step 6: Clean up resources
// CRITICAL: Always release when done to prevent memory leaks
override fun onDestroy() {
    super.onDestroy()
    wakeWordEngine.release()  // Releases ONNX models and audio resources
}
```

### Detection Cooldown

To prevent multiple notifications for a single wake word utterance, the library includes a configurable cooldown period:

```kotlin
// Default: 2 second cooldown between detections
val wakeWordEngine = WakeWordEngine(
    context = context,
    models = models,
    detectionCooldownMs = 2000L  // Recommended for voice assistants
)

// Shorter cooldown for gaming/rapid commands
val gamingEngine = WakeWordEngine(
    context = context,
    models = models,
    detectionCooldownMs = 500L   // 0.5 second cooldown
)

// Disable cooldown entirely (use with caution!)
// WARNING: This will trigger 3-5 events per utterance due to sliding window
val rawEngine = WakeWordEngine(
    context = context,
    models = models,
    detectionCooldownMs = 0L     // All detections pass through
)
```

The cooldown period helps prevent duplicate detections caused by the sliding window processing approach. When a wake word is detected, subsequent detections for the same model are ignored until the cooldown period expires.

### Important Usage Notes

#### Coroutine Scope and Threading

**⚠️ DO NOT pass a UI-bound CoroutineScope to WakeWordEngine**

```kotlin
// ❌ WRONG - This will freeze your UI after ~13 seconds!
@Composable
fun MyScreen() {
    val scope = rememberCoroutineScope() // This runs on Main dispatcher
    val engine = WakeWordEngine(
        context = context,
        models = models,
        scope = scope  // DON'T DO THIS! Heavy ML inference will block UI thread
    )
}

// ✅ CORRECT - Let WakeWordEngine manage its own threading
@Composable
fun MyScreen() {
    val engine = WakeWordEngine(
        context = context,
        models = models
        // Automatically uses Dispatchers.Default for background processing
    )
    
    // Use a separate UI scope for collecting results
    LaunchedEffect(engine) {
        engine.detections.collect { detection ->
            // This runs on Main thread - safe for UI updates
            showToast("${detection.model.name} detected!")
        }
    }
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

Request the permission at runtime before calling `wakeWordEngine.start()`:

```kotlin
// Example permission handling
private fun checkAndRequestPermission() {
    when {
        ContextCompat.checkSelfPermission(
            this, 
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED -> {
            // Permission granted - start detection
            wakeWordEngine.start()
        }
        shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
            // Show explanation why audio is needed
            showPermissionRationale()
        }
        else -> {
            // Request permission
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}
```

### Optimization Tips

#### 1. **Threshold Tuning**
```kotlin
// Start with manufacturer recommendations
// Test in your target environment and adjust
val model = WakeWordModel(
    name = "Hey Assistant",
    modelPath = "model.onnx",
    threshold = 0.1f  // Start here, then tune based on:
                      // - False positives: increase threshold
                      // - Missed detections: decrease threshold
)
```

#### 2. **Battery Optimization**
```kotlin
// Stop detection when screen is off
class MyService : Service() {
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> wakeWordEngine.stop()
                Intent.ACTION_SCREEN_ON -> wakeWordEngine.start()
            }
        }
    }
}
```

#### 3. **Multiple Wake Words**
```kotlin
// Different thresholds for different use cases
val models = listOf(
    WakeWordModel("Emergency", "emergency.onnx", 0.3f),    // Higher threshold
    WakeWordModel("Assistant", "assistant.onnx", 0.08f),   // Normal threshold
    WakeWordModel("Computer", "computer.onnx", 0.05f)      // Lower threshold
)

// React differently based on which word was detected
wakeWordEngine.detections.collect { detection ->
    when (detection.model.name) {
        "Emergency" -> handleEmergencyCommand()
        "Assistant" -> startListening()
        "Computer" -> showCommandMenu()
    }
}
```

#### 4. **Performance Monitoring**
```kotlin
// Log detection performance for debugging
wakeWordEngine.detections.collect { detection ->
    Log.d("WakeWord", buildString {
        append("Model: ${detection.model.name}")
        append(", Score: %.3f".format(detection.score))
        append(", Threshold: ${detection.model.threshold}")
        append(", Time: ${System.currentTimeMillis()}")
    })
}
```

## API Reference

### WakeWordEngine

Main entry point for wake word detection.

```kotlin
class WakeWordEngine(
    context: Context,
    models: List<WakeWordModel>,
    detectionCooldownMs: Long = 2000L,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
)
```

Parameters:
- `context`: Android context for accessing resources
- `models`: List of wake word models to detect
- `detectionCooldownMs`: Cooldown period in milliseconds to prevent duplicate detections (default: 2000ms, use 0 to disable)
- `scope`: CoroutineScope for background operations (uses Dispatchers.Default by default)

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