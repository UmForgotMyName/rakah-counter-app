# AGENT.md - Engineering Playbook For This Repository

This document is for humans and coding agents working on `rakah-counter-app`.

## Mission

Improve posture robustness and rakah-count correctness while preserving real-time performance on mid-range Android devices.

## System Boundaries

- `shared/` contains counting/business logic and should stay platform-neutral.
- `androidApp/` contains camera, model execution, and UI integration.
- If a change can live in `shared`, prefer putting it there.

## Critical Invariants

1. **Always close `ImageProxy`** in analyzer paths.
2. **Do not run heavy image/model work on main thread**.
3. **If GPU delegate is used, keep interpreter usage thread-consistent**.
4. **Preserve coordinate correctness** when model input uses crop/resize.
5. **Avoid per-frame camera rebinds** on recomposition.
6. **Never count rakah directly from raw posture**; only from stable posture transitions.

## Runtime Dataflow (Canonical)

`CameraX ImageAnalysis` -> `Bitmap` -> `MoveNet` -> `keypoints` -> `PoseFrame` -> `DefaultPoseClassifier` -> `PostureSmoother` -> `RakahFSM` -> `UiState`

## Ownership Map

- Camera setup and lifecycle:
  - `androidApp/src/main/java/com/example/rakah/android/camera/CameraBinding.kt`
- Frame conversion + model:
  - `androidApp/src/main/java/com/example/rakah/android/ml/ImageUtils.kt`
  - `androidApp/src/main/java/com/example/rakah/android/ml/MoveNetInterpreter.kt`
- Android state orchestration:
  - `androidApp/src/main/java/com/example/rakah/android/vm/RakahViewModel.kt`
- Shared posture logic:
  - `shared/src/commonMain/kotlin/com/example/rakah/classifier/*`
  - `shared/src/commonMain/kotlin/com/example/rakah/fsm/*`
  - `shared/src/commonMain/kotlin/com/example/rakah/engine/*`

## Change Strategy

When implementing fixes, apply this order:

1. Correctness bugs (coordinate math, state transitions, confidence misuse).
2. Performance hazards (main thread work, redundant allocations, repeated binding).
3. Robustness controls (hysteresis, thresholding, fallback handling).
4. UX/observability (confidence display, event telemetry).

## Standards For ML/Camera Changes

- Document expected tensor shapes in code comments.
- Fail fast on unsupported tensor layouts.
- Keep conversion paths explicit (RGBA/YUV assumptions must be validated).
- Reuse buffers where possible; avoid per-frame large object churn.
- Keep analyzer backpressure behavior explicit.

## Standards For Classifier/FSM Changes

- Include reasoning for each threshold.
- Use enter/exit thresholds (hysteresis), not single-boundary toggles.
- Prefer adding confidence gating before adding new posture heuristics.
- Keep FSM transitions minimal and auditable.

## Verification Checklist

Before marking a change complete:

1. Static review:
   - no obvious thread violations
   - no camera lifecycle leaks
   - no unclosed images/interpreters
2. Build check:
   - `:androidApp:assembleDebug`
3. Functional smoke:
   - permission denied flow
   - posture overlay aligns with body in preview
   - count increments only after expected transition chain
4. Regression sanity:
   - no per-frame camera bind/unbind
   - UI remains responsive under live analysis

## If Build Fails In This Environment

Known local issue: invalid `JAVA_HOME`.

- Set `JAVA_HOME` to a valid JDK 17 path.
- Re-run: `./gradlew :androidApp:assembleDebug`

## Immediate Backlog (High Impact)

1. Add deterministic shared tests for classifier + smoother + FSM transitions.
2. Replace static standing seed with explicit calibration stage and lock-in criteria.
3. Add telemetry overlay (FPS, inference ms, dropped frame ratio, confidence histogram).
4. Introduce dynamic crop tracking (MoveNet region-of-interest updates frame to frame).

