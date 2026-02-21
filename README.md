# GlassInterface AI Engine

**On-device assistive vision system** — detects objects, estimates distance, and generates safety-critical alerts in real-time.

> [!NOTE] 
> **Looking for the Python server & SDK?**
> The server-based Python architecture (`FastAPI`, `uvicorn`, `BoT-SORT`, `Ultralytics`) has been moved to the legacy branch:
> [👉 View `ai-integration` branch](https://github.com/AnuranjanJain/GlassInterface/tree/ai-integration)

This `main` branch now contains the **Fully Native Android** implementation (v2.0) that runs on-device using TensorFlow Lite, eliminating the need for a separate PC or Wi-Fi network.

## Architecture

```
CameraX / ESP32-CAM → TFLite (YOLOv8s int8) → Native DistanceEstimator → RiskScorer → Overlay & TTS
```

## Features

- **On-Device Inference**: Uses a quantized YOLOv8s `.tflite` model executed completely locally on the Android device via CPU/NNAPI processors.
- **Embedded Assistive AI**: Native Kotlin ports of the Risk Scoring and Bounding Box scaling logic.
- **Dual Camera Ingest**: Use the phone's built-in camera via `CameraX` or toggle to intercept an external `MJPEG` HTTP stream from a wearable ESP32-CAM device.
- **Adaptive Alerts**: Dynamically tracks bounding boxes and announces context-aware navigation hints via Text-To-Speech (TTS).

## Installation & Build

### Option 1: Install the Pre-compiled APK
1. Download a release `.apk` (or build a debug one).
2. Transfer it to your Android device.
3. Install via a File Manager (requires "Install from unknown sources").

### Option 2: Compile with Android Studio / Gradle
Ensure you have the Android SDK installed with `cmdline-tools` or Android Studio.

```bash
# Clone the repository
git clone https://github.com/AnuranjanJain/GlassInterface.git
cd GlassInterface

# Build the Android APK using Gradle
./gradlew assembleDebug

# The built APK will be located at:
# app/build/outputs/apk/debug/app-debug.apk
```

## ESP32-CAM Usage

GlassInterface v2 can detach from the phone camera and act as a wearable headset processor:

1. Flash an ESP32-CAM with a standard `CameraWebServer` or MJPEG sketch.
2. Connect the phone to the ESP32's Wi-Fi network (or put them both on the same local network).
3. Open the **GlassInterface** app.
4. Tap the Gear icon to enter **Settings**.
5. Enable the **Use External Wi-Fi Camera** toggle.
6. Enter the stream URL (e.g., `http://192.168.4.1:81/stream`).
7. The app overlay will instantly switch to processing the external hardware's POV camera.

## Project Structure

```text
app/                    # Core UI, MainViewModel, Jetpack Compose UI
core/                   
├── ai-bridge/          # LocalAIEngine.kt, TFLite Interpreter, RiskScorer.kt
├── camera/             # CameraX FrameProvider, MjpegInputStream.kt
├── common/             # AlertConfig, BoundingBox, and SceneMode Data Models
├── overlay/            # Canvas UI Drawing logic
└── tts/                # Android TextToSpeech engine wrappers
feature/
└── settings/           # Configuration UI, DataStore repository
```