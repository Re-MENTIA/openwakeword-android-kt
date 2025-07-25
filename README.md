# openwakeword-android-kt  
*Kotlin library for on‑device multi‑wake‑word detection with Open Wake Word & ONNX Runtime.*

License: Apache‑2.0

---

## Features

| ★ | Description |
|---|-------------|
| 🔊 **Multi‑Wake‑Word** | Register unlimited ONNX classifier models at runtime. |
| 🧩 **Straightforward API** | 3 lines to start listening. |
| 🚀 **Kotlin Coroutines** | Background audio + inference without blocking UI. |

---

## Quick Start

Add the dependency (JitPack example):

~~~gradle
repositories { maven { url 'https://jitpack.io' } }
dependencies { implementation 'com.github.your‑org:openwakeword-android-kt:1.0.0' }
~~~

Load the common models once, then add any number of wake‑words:

~~~kotlin
val detector = WakeWordDetectorManager(context)

lifecycleScope.launch {
    detector.initCommonModels(
        melModelAsset = "melspectrogram.onnx",
        embedModelAsset = "embedding.onnx"
    )

    detector.addModel(
        WakeWordModel("hey_nugget", "hey_nugget.onnx", 0.05f) { key, score ->
            Toast.makeText(this@MainActivity, "$key detected @$score", Toast.LENGTH_SHORT).show()
        }
    )

    detector.startListening()   // microphone ON
}
~~~

Stop when no longer needed:

~~~kotlin
detector.stopListening()
~~~

---

## Architecture

~~~text
WakeWordDetectorManager
 ├─ AudioIO (AudioRecord 16 kHz)
 ├─ Pipeline (MelSpectrogram + Embedding OrtSession)
 └─ ModelManager (n × classifier OrtSession + threshold + callback)
~~~

The heavy Mel/Embedding computation runs **once per audio chunk** and is reused by every classifier, minimizing CPU & battery drain.

---

## Building from Source

~~~bash
git clone https://github.com/your‑org/openwakeword-android-kt
./gradlew :sample:installDebug
~~~

Run the `sample` app on an Android 6.0+ device; say *“hey nugget”* to see a Toast.

---

## Training New Wake‑Words

Use the [OpenWakeWord](https://github.com/dscripka/openWakeWord) Python toolkit to create a custom `.onnx` classifier, then drop it into your app’s `assets/` folder and register via `addModel()`.

---

## License & Credits

* **Apache License 2.0** – see [`LICENSE`](LICENSE).  
* Portions adapted from the original Java POC by @hasanatlodhi in **[OpenwakewordforAndroid](https://github.com/hasanatlodhi/OpenwakewordforAndroid)** (Apache‑2.0).  
* Powered by [ONNX Runtime](https://onnxruntime.ai) and the Open Wake Word research project.

---

> Give the repo a ⭐ if you like it—PRs & issues welcome!
