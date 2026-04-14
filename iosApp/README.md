# iOS Direct Integration (Xcode + SharedKit)

This project uses direct Kotlin Multiplatform integration for iOS.

No CocoaPods bridge is required.

For overall architecture, see root `README.md`.
For full environment/build/test setup, see root `DEVELOPMENT.md`.
For contributor workflow rules, see root `AGENTS.md`.

## What is generated from `shared`

The `shared` module now produces Apple frameworks with base name `SharedKit` for:

- `iosArm64` (physical device)
- `iosSimulatorArm64` (Apple Silicon simulator)
- `iosX64` (Intel simulator)

Gradle config is in:

- `shared/build.gradle.kts`

## One-time setup on macOS

1. Open Xcode and create a new iOS app project inside this `iosApp` directory.
2. Set deployment target (for example iOS 15+).
3. Add a new **Run Script Phase** before "Compile Sources" in the iOS target:

```bash
cd "$SRCROOT/.."
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

4. Ensure "Framework Search Paths" and script ordering are default (Xcode + Kotlin plugin tasks handle the framework location).
5. Build and run from Xcode.

## Swift starter files included

Use these starter files from this folder:

- `StarterApp/PrayerCompanionApp.swift`
- `StarterApp/ContentView.swift`
- `StarterApp/PrayerGuidanceViewModel.swift`

These files use `PrayerSessionFacade` from shared code for a direct SwiftUI demo workflow.

## Step image sharing

To copy Android pose guide images into iOS resources:

```bash
bash iosApp/scripts/sync_step_assets.sh
```

This copies `step_*.png` assets from:

- `androidApp/src/main/res/drawable-nodpi`

into:

- `iosApp/Resources/StepImages`

Then add those files to your Xcode target's "Copy Bundle Resources".

## Suggested platform split

- Shared (`shared`):
  - prayer FSM
  - smoothing
  - session control and guidance state
- Android:
  - CameraX + MoveNet
- iOS:
  - AVFoundation/Vision or TFLite iOS
  - feed classified posture into `PrayerSessionController.onClassifiedPosture(...)`

## Build tasks

- Build Apple framework artifacts:

```bash
./gradlew :shared:assemble
```

- Common shared checks:

```bash
./gradlew :shared:check
```
