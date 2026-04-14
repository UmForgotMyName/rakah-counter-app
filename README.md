# Rakah Counter (KMP: Android + iOS Direct Integration)

This repository contains a Kotlin Multiplatform prayer guidance system with:

- Android live camera pose tracking (`androidApp`)
- Shared prayer logic (`shared`)
- iOS direct-integration starter assets and SwiftUI scaffolding (`iosApp`)

## Documentation

- Root architecture and workflow: `README.md` (this file)
- Contributor/agent runbook: `AGENTS.md`
- iOS direct integration details: `iosApp/README.md`
- Work backlog and status: `TODO.md`

## Architecture

### `shared` (cross-platform core)

The shared module now contains the full prayer domain and state machine logic:

- prayer types (`FAJR`, `DHUHR`, `ASR`, `MAGHRIB`, `ISHA`)
- prayer steps (`QIYAM`, `RUKU`, `QAWMAH`, `SUJOOD`, `JALSA_BETWEEN_SUJOOD`, `SECOND_SUJOOD`, `TASHAHHUD`, `SALAAM_RIGHT`, `SALAAM_LEFT`, `DONE`)
- per-prayer rakah + tashahhud checkpoints
- posture smoothing + guidance FSM
- result payload with:
  - current/previous/next step
  - current/completed rakah
  - confidence + event

Primary files:

- `shared/src/commonMain/kotlin/com/example/rakah/model/Domain.kt`
- `shared/src/commonMain/kotlin/com/example/rakah/fsm/RakahFSM.kt`
- `shared/src/commonMain/kotlin/com/example/rakah/engine/RakahEngine.kt`
- `shared/src/commonMain/kotlin/com/example/rakah/engine/PrayerSessionController.kt`

### `androidApp` (Android runtime)

- CameraX preview + analysis
- MoveNet inference
- Pose overlay
- guidance UI (previous/current/next/then pose cards)

Primary files:

- `androidApp/src/main/java/com/example/rakah/android/MainActivity.kt`
- `androidApp/src/main/java/com/example/rakah/android/vm/RakahViewModel.kt`
- `androidApp/src/main/java/com/example/rakah/android/ui/guidance/StepGuidanceStrip.kt`

### `iosApp` (direct iOS integration starter)

- direct integration guide
- SwiftUI starter files that call shared `PrayerSessionFacade`
- script to sync pose PNG assets from Android resources

Primary files:

- `iosApp/README.md`
- `iosApp/StarterApp/PrayerCompanionApp.swift`
- `iosApp/StarterApp/PrayerGuidanceViewModel.swift`
- `iosApp/StarterApp/ContentView.swift`
- `iosApp/scripts/sync_step_assets.sh`

## iOS Integration Mode (Chosen)

This repo uses **direct iOS integration** (Xcode + Gradle framework embedding), not CocoaPods.

`shared` now defines iOS targets and outputs a framework named `SharedKit` for:

- `iosArm64`
- `iosSimulatorArm64`
- `iosX64`

See:

- `shared/build.gradle.kts`
- `iosApp/README.md`

## Cross-Platform Session API

To reduce platform friction, shared includes:

- `PrayerSessionController`:
  - full strongly-typed API (`PrayerType`, `Posture`, `RakahResult`)
  - supports frame-based and posture-fed workflows
- `PrayerSessionFacade`:
  - Swift-friendly string-based API (`GuidanceSnapshot`)
  - useful for rapid iOS UI wiring before native pose model integration

This lets Android and iOS keep different camera/ML stacks while sharing identical prayer guidance logic.

## Build

### Android

- Assemble:
  - `./gradlew :androidApp:assembleDebug`
- Install:
  - `./gradlew :androidApp:installDebug`

### Shared / Apple framework artifacts

- `./gradlew :shared:assemble`
- In Xcode direct mode, add script phase:
  - `./gradlew :shared:embedAndSignAppleFrameworkForXcode`

## Assets

Pose guide images live in:

- `androidApp/src/main/res/drawable-nodpi/step_*.png`

You can sync these to iOS resource folders with:

- `bash iosApp/scripts/sync_step_assets.sh`

## Current Validation Limitation

On this machine, Gradle compile checks cannot run until `JAVA_HOME` points to a valid JDK 17 installation.
