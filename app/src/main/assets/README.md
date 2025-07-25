# Model Files

This directory contains ONNX model files required for wake word detection.

## Model Files

### 1. hello_world.onnx
- **Description**: Wake word detection model for "Hello World"
- **Size**: 207KB
- **License**: [Specify your model's license here]
- **Source**: [Specify the source/training method]

### 2. melspectrogram.onnx
- **Description**: Audio preprocessing model that converts raw audio to mel-spectrogram features
- **License**: Apache License 2.0
- **Source**: OpenWakeWord project (https://github.com/dscripka/openWakeWord)
- **Copyright**: Copyright (c) 2022-2024 David Scripka

### 3. embedding_model.onnx
- **Description**: Feature extraction model that converts mel-spectrograms to speech embeddings
- **License**: Apache License 2.0
- **Source**: Originally from Google's speech embedding model, converted to ONNX format by OpenWakeWord
- **Copyright**: 
  - Original TensorFlow model: Copyright Google LLC
  - ONNX conversion: Copyright (c) 2022-2024 David Scripka

## License Notice

The melspectrogram.onnx and embedding_model.onnx models are part of the OpenWakeWord project and are licensed under the Apache License 2.0. The original embedding model was created by Google and released under the Apache License 2.0.

```
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

## Attribution

This project uses models from:
- **OpenWakeWord** by David Scripka: https://github.com/dscripka/openWakeWord
- **Google Speech Embedding**: Original TensorFlow Hub model by Google

## Model Architecture

The wake word detection pipeline consists of three stages:
1. **Audio → Mel-spectrogram**: Raw audio (16kHz) is converted to mel-spectrogram features
2. **Mel-spectrogram → Embeddings**: Features are converted to speech embeddings
3. **Embeddings → Detection**: Wake word model processes embeddings to detect the target phrase

## Important Notes

- All three model files are required for the system to function
- Models must be placed directly in this assets directory (no subdirectories)
- The models are loaded at runtime by the ONNX Runtime Android library
- Total model size: ~2.6 MB (including your hello_world.onnx)

## Performance

- Inference runs entirely on-device
- No internet connection required
- Optimized for real-time audio processing on mobile devices