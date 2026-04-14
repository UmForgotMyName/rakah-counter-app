# Development Setup, Build, and Test Guide

This guide covers local setup and day-to-day workflows for Android and iOS.

Command note: on Windows PowerShell, replace `./gradlew` with `.\gradlew.bat`.

## 1. Host OS Support

- Android development: Windows, macOS, or Linux.
- iOS development: macOS only (Xcode is required).
- Shared Kotlin module (`shared`): can be built on any host with JDK 17; iOS runtime testing still requires macOS.

## 2. Prerequisites

- Git
- JDK 17
- Gradle wrapper (already committed as `gradlew` / `gradlew.bat`)
- Android Studio (latest stable recommended)
- Android SDK Platform 34
- Android SDK Build-Tools
- Android SDK Platform-Tools (`adb`)
- One Android emulator or physical device (API 24+)
- iOS-specific prerequisites:
- Xcode 15+ on macOS
- Xcode Command Line Tools (`xcode-select --install`)

## 3. Repository Setup

1. Clone and open the repo root:

```bash
git clone <repo-url>
cd rakah-counter-app
```

2. Ensure `JAVA_HOME` points to a valid JDK 17 installation.

Windows (PowerShell example):

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

macOS/Linux example:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"
```

3. Verify Gradle can run:

```bash
./gradlew -v
```

Windows:

```powershell
.\gradlew.bat -v
```

## 4. Build and Run: Android

Debug build:

```bash
./gradlew :androidApp:assembleDebug
```

Install to connected device/emulator:

```bash
./gradlew :androidApp:installDebug
```

Launch app by activity (optional):

```bash
adb shell am start -n com.example.rakah/com.example.rakah.android.MainActivity
```

From Android Studio:

1. Open project root.
2. Sync Gradle.
3. Run `androidApp` on a device/emulator.
4. Grant camera permission when prompted.

## 5. Build and Run: iOS (Direct Integration)

This project uses direct framework integration (`SharedKit`) from `shared` into Xcode.

1. On macOS, create/open an iOS app project under `iosApp`.
2. Add this Run Script Phase before `Compile Sources`:

```bash
cd "$SRCROOT/.."
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

3. Add Swift starter files from `iosApp/StarterApp` to your iOS target.
4. (Optional) Sync pose step images:

```bash
bash iosApp/scripts/sync_step_assets.sh
```

5. Build and run from Xcode on simulator/device.

Standalone shared Apple artifacts can be built with:

```bash
./gradlew :shared:assemble
```

## 6. Testing Workflows

### Automated checks currently available

- Shared checks:

```bash
./gradlew :shared:check
```

- Android compile check:

```bash
./gradlew :androidApp:assembleDebug
```

Note: There are currently no committed dedicated unit test source sets in `shared/src/commonTest` or Android test directories yet; `TODO.md` tracks this gap.

### Manual smoke test: Android

1. Start app and grant camera permission.
2. Select each prayer type and confirm rakah total updates correctly.
3. Confirm pose transitions update `previous/current/next` guidance.
4. Confirm salaam actions complete session as expected.

### Manual smoke test: iOS starter

1. Run iOS app in Xcode.
2. Change prayer with segmented control and verify totals/checkpoints change.
3. Use manual posture feed buttons and verify guidance snapshot updates.
4. Trigger `Salaam Right` and `Salaam Left` and verify progression to completion.

## 7. Troubleshooting

- `JAVA_HOME is set to an invalid directory`: point `JAVA_HOME` to the real JDK 17 folder and rerun `gradlew`.
- Xcode build cannot find/update `SharedKit`: confirm the Run Script Phase exists and executes before `Compile Sources`.
- iOS step images missing: run `bash iosApp/scripts/sync_step_assets.sh` and ensure files are included in target resources.
