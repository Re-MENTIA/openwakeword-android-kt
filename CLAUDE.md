# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android Kotlin project for implementing wake word detection using ONNX Runtime. It consists of two modules:
- `:app` - Main Android application
- `:wakeword` - Library module for wake word detection functionality

## Common Development Commands

### Build Commands
```bash
./gradlew build              # Build entire project
./gradlew assembleDebug      # Build debug APK
./gradlew assembleRelease    # Build release APK
./gradlew installDebug       # Install debug build on connected device
./gradlew clean              # Clean build artifacts
```

### Testing Commands
```bash
./gradlew test               # Run all unit tests
./gradlew connectedAndroidTest # Run instrumented tests on device/emulator
./gradlew testDebugUnitTest  # Run only debug variant unit tests
```

### Module-Specific Commands
```bash
./gradlew :app:assembleDebug    # Build only app module
./gradlew :wakeword:test        # Test only wakeword library
```

## Architecture & Structure

### Module Dependencies
- App module depends on wakeword library module
- Dependencies managed via version catalog (`gradle/libs.versions.toml`)
- Kotlin DSL used for all Gradle files

### Key Technologies
- **ML Inference**: ONNX Runtime Android (1.18.0) for running wake word models
- **Audio Processing**: Apache Commons Math3 for signal processing
- **Async**: Kotlin Coroutines for background operations
- **UI**: Material Design Components

### Package Structure
- App: `com.rementia.openwakeword`
- Library: `com.rementia.openwakeword.lib`

### Build Configuration
- Compile/Target SDK: 34 (Android 14)
- Min SDK: 23 (Android 6.0)
- Java: JDK 17 (toolchain)
- Kotlin: 1.9.23
- AGP: 8.4.0

## Development Guidelines

### When Adding Wake Word Detection Features
1. Implement core detection logic in `:wakeword` module
2. Keep audio processing and ML inference isolated in the library
3. Use coroutines for background audio processing
4. Handle Android audio permissions in the app module

### Testing Wake Word Detection
1. Unit tests for signal processing algorithms in `:wakeword/src/test`
2. Instrumented tests for audio recording in `:app/src/androidTest`
3. Test on various Android versions (min SDK 23 to target SDK 34)

### Performance Considerations
- ONNX Runtime runs inference on device
- Consider battery impact of continuous audio processing
- Use appropriate audio buffer sizes for real-time processing