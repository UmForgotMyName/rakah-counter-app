# TODO - Rakah Counter

Status legend:

- `[x]` completed in current pass
- `[ ]` pending

## A. Runtime Correctness

- [x] Fix keypoint coordinate remapping when model uses center-crop input.
  - Implemented in `MoveNetInterpreter` + `RakahViewModel` using crop metadata.
- [x] Add MoveNet output-shape compatibility for both `[1,17,3]` and `[1,1,17,3]`.
  - Implemented in `MoveNetInterpreter` with runtime shape check.
- [x] Remove hardcoded posture confidence (`1f`) and compute signal-derived confidence.
  - Implemented in shared classifier + FSM result propagation.
- [x] Add low-confidence keypoint gating before posture classification.
  - Implemented with minimum required keypoint score threshold.

## B. Stability / Counting Quality

- [x] Use threshold hysteresis (enter vs exit) to reduce posture flicker.
  - Implemented by honoring `exit` thresholds when retaining previous stable posture.
- [x] Make standing calibration adaptive instead of fixed.
  - Implemented with EMA updates in `StandingCalib`.
- [ ] Add explicit calibration mode in UI (countdown + body framing instructions).
  - Acceptance: user can complete calibration in <10s with visual progress.
- [ ] Add pause/resume/reset controls for prayer session state.
  - Acceptance: no count mutation while paused; reset clears FSM and smoothing state.

## C. Performance

- [x] Move analyzer off main thread using dedicated single-thread executor.
- [x] Prevent camera rebind on every recomposition.
  - Implemented with `DisposableEffect` lifecycle binding.
- [x] Replace YUV->JPEG conversion path with RGBA direct byte parsing.
- [ ] Add lightweight perf telemetry (inference time, fps, dropped frames).
  - Acceptance: debug overlay reports rolling averages each second.
- [ ] Reuse bitmap buffers / avoid per-frame bitmap allocation where possible.
  - Acceptance: sustained GC reduction during 2-minute session.

## D. Tests

- [ ] Add shared unit tests for:
  - classifier threshold boundaries
  - hysteresis retention behavior
  - smoother confirmation logic
  - FSM transition counting rules
- [ ] Add synthetic keypoint replay tests from captured frame traces.
  - Acceptance: deterministic expected rakah count across known sequences.

## E. Platform / Product

- [ ] Re-enable and validate iOS targets in `shared/build.gradle.kts`.
- [ ] Add structured app configuration (threshold presets per camera setup).
- [ ] Add session persistence + export (JSON summary with timestamps/events).

## F. Tooling / Docs

- [x] Create deep `README.md` with architecture, pipeline, and source-backed rationale.
- [x] Create `AGENT.md` with development invariants and contributor runbook.
- [ ] Add CI pipeline for `assembleDebug` and shared tests.

