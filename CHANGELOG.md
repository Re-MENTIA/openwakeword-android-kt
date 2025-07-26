# Changelog

All notable changes to the OpenWakeWord Android Kotlin library will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.4] - 2025-01-26

### Added
- **ParallelWakeWordEngine**: New high-performance engine with non-blocking parallel processing
  - Fully asynchronous architecture prevents latency accumulation
  - Ring buffer implementation for efficient audio data management
  - Worker pool with configurable `maxWorkers` parameter
  - Fine-grained 50ms sliding windows for improved temporal resolution
  - Ideal for running 3+ wake word models simultaneously
- Comprehensive documentation for choosing between WakeWordEngine and ParallelWakeWordEngine
- Performance optimization examples in README

### Fixed
- Latency accumulation issue when running multiple models
- Memory allocation overhead in audio processing pipeline

### Changed
- Updated README with engine selection guidance and performance tips
- Enhanced documentation with real-world usage examples

## [0.1.3] - 2025-01-26

### Added
- Initial release with Maven Central publishing
- Basic WakeWordEngine with sequential processing
- Support for multiple wake word models
- SINGLE_BEST and ALL detection modes
- Configurable detection cooldown
- Flow-based API for Kotlin coroutines

### Features
- On-device wake word detection using ONNX Runtime
- 16kHz mono audio recording
- Real-time mel-spectrogram computation
- Embedding model for feature extraction
- Sliding window processing

[0.1.4]: https://github.com/Re-MENTIA/openwakeword-android-kt/compare/v0.1.3...v0.1.4
[0.1.3]: https://github.com/Re-MENTIA/openwakeword-android-kt/releases/tag/v0.1.3