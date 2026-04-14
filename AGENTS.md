# AGENTS.md - Engineering Runbook

This runbook is for humans and coding agents working in `rakah-counter-app`.

## Mission

Deliver robust prayer-step guidance with shared logic across Android and iOS, while keeping mobile camera/inference stacks platform-native.

## Canonical Docs

Use these as source-of-truth, in this order:

1. `README.md`:
   - project architecture
   - module map
   - primary build commands
2. `AGENTS.md` (this file):
   - engineering rules
   - workflow expectations
   - verification checklist
3. `iosApp/README.md`:
   - direct iOS integration setup in Xcode
4. `TODO.md`:
   - execution backlog with status

If docs conflict, update all affected docs in the same change.

## System Boundaries

- `shared/`:
  - domain model
  - prayer FSM/state guidance
  - smoothing and session orchestration
  - must stay platform-neutral
- `androidApp/`:
  - CameraX + TensorFlow Lite pipeline
  - Android UI and overlay
- `iosApp/`:
  - direct-integration starter files
  - Xcode setup docs + asset sync scripts
  - iOS-side camera/ML implementation remains native

## Critical Invariants

1. Always close `ImageProxy` in analyzer code paths.
2. Do not run heavy image/model work on the main thread.
3. Preserve coordinate correctness when crop/resize is used.
4. Keep shared FSM authoritative for prayer progression.
5. Never duplicate prayer transition logic separately on each platform.
6. Keep iOS integration direct (no CocoaPods requirement unless explicitly chosen later).

## Shared API Contract

Preferred shared entrypoints:

- `PrayerSessionController` (typed Kotlin API)
- `PrayerSessionFacade` (Swift-friendly string API)

Both platforms must consume one of these interfaces for prayer guidance so behavior stays identical.

## iOS Direct Integration Workflow

1. In Xcode iOS target, add Run Script Phase:

```bash
cd "$SRCROOT/.."
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

2. Use `SharedKit` framework output from `shared`.
3. Keep SwiftUI starter files in `iosApp/StarterApp` as reference implementation.
4. Sync pose guide assets when updated:

```bash
bash iosApp/scripts/sync_step_assets.sh
```

## Change Strategy

Apply work in this priority order:

1. Correctness:
   - FSM transitions
   - prayer/rakah checkpoints
   - coordinate mappings
2. Robustness:
   - confidence gating
   - hysteresis
   - transition recovery
3. Performance:
   - analyzer throughput
   - allocation control
   - UI responsiveness
4. Product UX:
   - clear step guidance
   - previous/current/next visuals
   - predictable manual fallbacks (e.g., salaam confirmation)

## Verification Checklist

Before marking a change complete:

1. Static review:
   - no lifecycle leaks
   - no thread-safety regressions
   - no stale docs references
2. Shared logic check:
   - prayer progression still valid for all 5 prayers
   - tashahhud checkpoints preserved
3. Android smoke:
   - camera permission flow
   - overlay alignment
   - step guidance updates live
4. iOS scaffold sanity:
   - direct integration instructions still accurate
   - Swift starter files still compile against shared API signatures
5. Commands:
   - `./gradlew :shared:check`
   - `./gradlew :androidApp:assembleDebug` (when environment allows)

## Known Local Limitation

Current environment may fail Gradle builds if `JAVA_HOME` is invalid.

Set `JAVA_HOME` to a valid JDK 17 and re-run checks.

## Backlog Focus (Near-Term)

1. Add shared tests for FSM transitions across all prayers.
2. Add calibration UX flow.
3. Add telemetry overlay (fps/inference time/dropped frames).
4. Add iOS native posture detector adapter that feeds `PrayerSessionController.onClassifiedPosture(...)`.

