# Rakah Counter (Kotlin + CameraX + TensorFlow Lite)

This project is an Android app that estimates prayer posture from live camera frames and counts rakahs using a posture-state transition rule set.

It is organized as a Kotlin multi-module project:

- `androidApp`: Android runtime (CameraX, Compose UI, TensorFlow Lite model execution).
- `shared`: platform-agnostic posture/domain logic (geometry, classifier, smoothing, rakah finite-state machine).

## What The System Does

For each camera frame:

1. Capture frame with CameraX `ImageAnalysis`.
2. Convert frame to RGBA bitmap.
3. Rotate upright based on camera metadata.
4. Center-crop and resize to MoveNet input size.
5. Run MoveNet and read 17 keypoints `(y, x, score)`.
6. Map keypoints back to original preview coordinates.
7. Build `PoseFrame` for shared logic.
8. Classify raw posture (`QIYAM`, `RUKU`, `JALSA`, `SUJOOD`, `UNKNOWN`).
9. Smooth posture over consecutive frames.
10. Update rakah FSM and emit UI state.

## Module Walkthrough

### `shared` (domain + logic)

- Domain model:
  - `Posture`, `Keypoint`, `PoseFrame`, `RakahResult`, `StandingCalib`
  - File: `shared/src/commonMain/kotlin/com/example/rakah/model/Domain.kt`
- Geometry helpers:
  - knee angle, torso incline, midpoint
  - File: `shared/src/commonMain/kotlin/com/example/rakah/math/Geometry.kt`
- Posture thresholds:
  - enter and exit thresholds (exit is looser for hysteresis)
  - File: `shared/src/commonMain/kotlin/com/example/rakah/classifier/Thresholds.kt`
- Pose classifier:
  - computes pose features from keypoints
  - rejects low-confidence keypoint sets
  - keeps previous posture when exit-threshold checks still pass (hysteresis)
  - opportunistically updates standing calibration with EMA
  - File: `shared/src/commonMain/kotlin/com/example/rakah/classifier/DefaultPoseClassifier.kt`
- Smoother:
  - requires `confirmFrames` consistent raw labels before changing stable posture
  - File: `shared/src/commonMain/kotlin/com/example/rakah/fsm/PostureSmoother.kt`
- Rakah FSM:
  - increments when transition to `QIYAM` occurs after seeing `SUJOOD -> JALSA`
  - File: `shared/src/commonMain/kotlin/com/example/rakah/fsm/RakahFSM.kt`
- Engine:
  - pipeline glue: classify -> smooth -> FSM
  - File: `shared/src/commonMain/kotlin/com/example/rakah/engine/RakahEngine.kt`

### `androidApp` (camera + ML + UI)

- Camera permission, Compose layout, overlay, counters:
  - `androidApp/src/main/java/com/example/rakah/android/MainActivity.kt`
- Camera binding:
  - `Preview` + `ImageAnalysis` bound via lifecycle
  - uses `STRATEGY_KEEP_ONLY_LATEST`
  - requests RGBA_8888 analysis output
  - File: `androidApp/src/main/java/com/example/rakah/android/camera/CameraBinding.kt`
- Image conversion:
  - parses RGBA bytes from analysis plane 0 directly into ARGB bitmap
  - File: `androidApp/src/main/java/com/example/rakah/android/ml/ImageUtils.kt`
- MoveNet wrapper:
  - GPU delegate when supported, CPU fallback (`setNumThreads(4)`) otherwise
  - supports both `[1,17,3]` and `[1,1,17,3]` output layouts
  - returns crop metadata for coordinate remapping
  - File: `androidApp/src/main/java/com/example/rakah/android/ml/MoveNetInterpreter.kt`
- ViewModel:
  - owns analyzer executor, interpreter lifecycle, and `StateFlow` UI model
  - maps model output back to preview coordinate system
  - File: `androidApp/src/main/java/com/example/rakah/android/vm/RakahViewModel.kt`
- Overlay:
  - draws keypoint circles and skeleton edges
  - File: `androidApp/src/main/java/com/example/rakah/android/ui/overlay/PoseOverlay.kt`

## Improvements Implemented (This Pass)

1. Analyzer now runs on a dedicated single-thread executor instead of the main thread.
2. Camera binding moved away from per-recomposition rebinding into `DisposableEffect` lifecycle binding.
3. Switched analysis output to `OUTPUT_IMAGE_FORMAT_RGBA_8888` and removed YUV->JPEG conversion path.
4. MoveNet output parsing now supports both common tensor layouts (`[1,17,3]` and `[1,1,17,3]`).
5. Center-crop coordinate remapping fixed so keypoints are projected back into full preview coordinates.
6. Camera permission flow is now reactive (`StateFlow`) instead of a plain mutable property.
7. Shared classifier now computes posture confidence from keypoint scores and rejects low-confidence frames.
8. Shared thresholds now use looser exit bands (hysteresis) to reduce posture flicker.
9. Standing calibration is now adaptive (EMA) rather than fully static.
10. UI now exposes posture confidence.

## Design Notes

- Rakah counting is intentionally conservative:
  - It counts on transition into `QIYAM` only after a completed `SUJOOD -> JALSA` sequence.
  - This avoids many double-count paths but can undercount if posture detection is unstable.
- Classifier thresholding is heuristic and camera-view dependent:
  - Device angle, body framing, and clothing can shift score distributions.
- The current model is single-person MoveNet:
  - Behavior is best when one subject is centered and mostly visible.

## Build / Run

### Prerequisites

- Android SDK + platform tools
- JDK 17 configured correctly (`JAVA_HOME`)

### Useful commands

- Assemble debug APK:
  - `./gradlew :androidApp:assembleDebug`
- Install debug APK:
  - `./gradlew :androidApp:installDebug`
- Launch activity:
  - `adb shell am start -n com.example.rakah/com.example.rakah.android.MainActivity`

VS Code tasks for these commands exist in `.vscode/tasks.json`.

## Current Environment Note

On this machine, automated build validation failed because `JAVA_HOME` is set to an invalid path (`C:\Program Files\JavaJDK\jdk-17`). Update `JAVA_HOME` to a valid JDK 17 installation before running Gradle.

## Research-Backed Rationale

The changes above were selected to align with primary platform/model guidance:

- CameraX analyzer guidance emphasizes:
  - analyze quickly,
  - close `ImageProxy`,
  - use backpressure strategies when processing cannot keep up.
- CameraX RGBA mode documents byte packing in plane 0 as alpha, red, green, blue.
- CameraX supports `ImageAnalysis.setAnalyzer(executor, ...)` and `STRATEGY_KEEP_ONLY_LATEST`.
- MoveNet tutorial documents:
  - 17 keypoints,
  - common output shape `[1,1,17,3]`,
  - Lightning input size 192,
  - score-based keypoint reliability and coordinate remapping from crop back to source.
- LiteRT/TFLite GPU delegate docs note thread affinity:
  - delegate/interpreter usage must stay on the same thread where initialized.

## Sources

- CameraX image analysis guide:
  - https://developer.android.com/media/camera/camerax/analyze
- Android `Image` format/plane layout reference:
  - https://developer.android.com/reference/kotlin/android/media/Image
- MoveNet TF Hub tutorial:
  - https://www.tensorflow.org/hub/tutorials/movenet
- LiteRT GPU delegate (Interpreter API) guidance:
  - https://www.tensorflow.org/lite/android/delegates/gpu

