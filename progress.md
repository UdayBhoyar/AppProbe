# AppProbe — Progress Log

## Stage 1: Project Foundation — COMPLETED
- [x] Workspace & Android SDK inspection completed.
- [x] Android project structure initialized (`com.appprobe`).
- [x] Target SDK & Compile SDK configured (`compileSdk = 36`, `targetSdk = 35`, `minSdk = 26`).
- [x] Jetpack Compose UI status screen implemented and verified.
- [x] Verified on `Pixel_6` emulator (API 35).

---

## Stage 2: Application Discovery — COMPLETED
- [x] Manifest updated with `QUERY_ALL_PACKAGES` permission for Android 11+ package visibility.
- [x] Created `AppInfo` model under `com.appprobe.inspection`.
- [x] Implemented `AppDiscoveryRepository` to query `PackageManager` on `Dispatchers.IO` and extract app icons, titles, package names, and system app flags.
- [x] Created `AppDiscoveryViewModel` and `AppDiscoveryUiState` supporting search, filtering (All / User / System), and error handling.
- [x] Implemented Material 3 `AppDiscoveryScreen`.
- [x] Updated `MainActivity` to host `AppDiscoveryScreen`.
- [x] Build verification (`./gradlew assembleDebug`).
- [x] Emulator deployment and runtime discovery verification (242 apps discovered).

---

## Stage 3: Application Inspection — COMPLETED
- [x] Created `AppInspectionInfo` model under `com.appprobe.inspection`.
- [x] Implemented `AppInspectionRepository` to retrieve detailed metadata (`versionName`, `versionCode`, `targetSdk`, `minSdk`, `uid`, `firstInstallTime`, `lastUpdateTime`, `isSystemApp`, `icon`, `appName`) via `PackageManager` on `Dispatchers.IO`.
- [x] Created `AppInspectionViewModel` and `AppInspectionUiState` with Loading, Success, and Error states.
- [x] Implemented Material 3 `AppInspectionScreen` with TopAppBar, back navigation, refresh action, header card, and structured metadata cards.
- [x] Made `AppDiscoveryScreen` list items clickable with `onAppClick` callback.
- [x] Added lightweight navigation in `MainActivity` with back-stack support.
- [x] Verified build (`./gradlew assembleDebug`) — successful in 2s with 0 errors.
- [x] Verified on `Pixel_6` emulator (API 35):
  - Application list rendered and clickable
  - Inspection screen opened with correct metadata
  - Multiple applications tested with dynamic metadata verification
  - Back navigation returned to discovery list
  - Logcat confirmed no crashes or uncaught exceptions (`adb logcat -d *:E | grep -i com.appprobe` clean).

---

## Stage 4: Target Application Selection — COMPLETED
- [x] Created `TargetApp` model under `com.appprobe.testing` (`packageName`, `appName`, `isSystemApp`).
- [x] Implemented `TargetAppViewModel` and `TargetAppState` to manage target state.
- [x] Created `TargetReadyScreen` displaying selected Target Application, App Name, Package Name, and "Ready for testing" status.
- [x] Added "Select as Target Application" action to `AppInspectionScreen`.
- [x] Updated `AppDiscoveryScreen` with active target banner, highlighted border, and "Target" badge on the selected application.
- [x] Updated `MainActivity` to handle seamless navigation between Discovery, Inspection, and Target Ready states.
- [x] Verified build (`./gradlew assembleDebug`) — successful with 0 errors.
- [x] Verified on `Pixel_6` emulator (API 35):
  - Application selection flow (`List -> Inspect -> Select Target -> Target Ready -> Back`)
  - Target state persisted across recomposition
  - Highlight and target badge displayed on discovery list
  - Process clean (`PID 3367`, no errors in logcat).

---

## Stage 5: Test Scenario Configuration — COMPLETED
- [x] Created `TestAction` model and `TestActionType` enum under `com.appprobe.testing` with support for: Launch App, Wait, Tap, Swipe, Press Back, Rotate, Background App, Resume App.
- [x] Created `TestScenario` model under `com.appprobe.testing` with scenario name, target package identifier, and ordered actions list.
- [x] Implemented `ScenarioBuilderViewModel` and `ScenarioBuilderUiState` managing scenario state, step ordering, removal, addition, and clearing.
- [x] Created Material 3 `ScenarioBuilderScreen` with TopAppBar, target summary header, editable scenario name, ordered step cards with Move Up/Down/Delete actions, empty state, and action picker dialog.
- [x] Integrated navigation flow (`Discovery -> Inspection -> Target Ready -> Scenario Builder`) with back-stack support.
- [x] Verified build (`./gradlew assembleDebug`) — successful with 0 errors.
- [x] Verified on `Pixel_6` emulator (API 35):
  - Target application info correctly propagated to Scenario Builder
  - Action additions via dialog verified
  - Action reordering (Move Up / Down) verified
  - Action deletion verified
  - Clear scenario and empty state verified
  - Back navigation to Target Ready and Discovery screens verified
  - Logcat clean (0 crashes).

---

## Stage 6: Test Scenario Execution — COMPLETED
- [x] **Files Created**:
  - `app/src/main/java/com/appprobe/execution/ExecutionResult.kt`: Domain models for `ActionExecutionStatus` (Pending, Running, Passed, Failed, Cancelled), `ActionResult`, `ExecutionStatus` (Idle, Running, Completed, Failed, Cancelled), and `ScenarioExecutionResult`.
  - `app/src/main/java/com/appprobe/execution/CommandExecutor.kt`: `CommandExecutor` interface and `DeviceShellExecutor` abstraction supporting command execution and sandbox permission fallback handling.
  - `app/src/main/java/com/appprobe/execution/ExecutionEngine.kt`: Coroutine-based sequential test scenario execution engine supporting all 8 actions (Launch App, Wait, Tap, Swipe, Press Back, Rotate, Background App, Resume App), step validation, coroutine cancellation, and live `ExecutionProgress` emission.
  - `app/src/main/java/com/appprobe/ui/ScenarioExecutionViewModel.kt`: ViewModel managing execution lifecycle, coroutine job cancellation, and re-run.
  - `app/src/main/java/com/appprobe/ui/ScenarioExecutionScreen.kt`: Material 3 Compose execution screen with target header card, execution status banner, linear progress indicator, dynamic action progress list with status badges, and Re-run / Back controls.
- [x] **Files Modified**:
  - `app/src/main/java/com/appprobe/ui/ScenarioBuilderScreen.kt`: Added `▶ Execute` button linked to `onExecuteScenario(TestScenario)`.
  - `app/src/main/java/com/appprobe/MainActivity.kt`: Integrated `"scenario_execution"` route in the navigation flow with parameter passing.
- [x] **Execution Capabilities**:
  - Supports Launch App (via package launcher intent, explicit component intent, and shell fallback), Wait (coroutine delay), Tap, Swipe, Press Back, Rotate, Background App, and Resume App.
  - Live state updates for each step and overall scenario.
  - Safe error handling and coroutine cancellation.
- [x] **Build Result**:
  - `./gradlew assembleDebug` passed with 0 errors.
- [x] **Emulator Verification (`Pixel_6`, API 35)**:
  - Full flow tested: `Discovery -> Inspection -> Target Ready -> Scenario Builder -> Scenario Execution -> Complete`.
  - 4-step scenario executed: Launch App -> Wait 2000 ms -> Press Back -> Launch App.
  - Live step transitions from Pending -> Running -> Passed verified.
  - Re-run functionality tested and verified.
  - Screenshot captured: `screenshot_stage6.png`.
- [x] **Logcat Result**:
  - Clean (`adb logcat -d *:E | grep -i com.appprobe` returned 0 errors/crashes).
- [x] **Remaining Issues**: None.

---

## Stage 7: Performance & Resource Monitoring — COMPLETED
- [x] **Files Created**:
  - `app/src/main/java/com/appprobe/monitoring/PerformanceSample.kt`: Models for `ProcessMemoryMetrics` (Total PSS, Dalvik PSS, Native PSS, other PSS), `PerformanceSample` (timestamp, targetPackage, PID, actionIndex, actionType, memory, cpuUsagePercent, threadCount), and `MonitoringSummary` (sampleCount, peak/min/avg Total PSS, peak Dalvik/Native heap, peak CPU, peak threads).
  - `app/src/main/java/com/appprobe/monitoring/PerformanceMonitor.kt`: Non-intrusive asynchronous performance monitoring collector running on `Dispatchers.IO`. Integrates target PID discovery, `ActivityManager.getProcessMemoryInfo`, proc status inspection, CPU delta calculation, and thread counting with non-fatal safety.
- [x] **Files Modified**:
  - `app/src/main/java/com/appprobe/execution/ExecutionResult.kt`: Extended `ScenarioExecutionResult` with `performanceSamples: List<PerformanceSample>` and `monitoringSummary: MonitoringSummary?`.
  - `app/src/main/java/com/appprobe/execution/ExecutionEngine.kt`: Wrapped scenario execution with `PerformanceMonitor` lifecycle (started at execution start, correlated with executing action index/name, stopped in `finally` block, emitting live progress and summary into `ScenarioExecutionResult`).
  - `app/src/main/java/com/appprobe/ui/ScenarioExecutionScreen.kt`: Added live "RESOURCE MONITOR" card during execution (explicitly separating Total PSS, Java Heap, Native Heap, CPU, Threads, and sample count) and "MONITORING SUMMARY" card upon scenario completion.
- [x] **Monitoring Architecture**:
  - Pure evidence collection: No memory leak detection or leak diagnosis conclusions are performed in Stage 7.
  - Distinct memory dimensions: Total PSS, Dalvik/Java Heap, and Native Heap are explicitly labeled and not conflated.
  - Strict CPU calculation: Reports `Unavailable` rather than guessing or presenting misleading percentages if delta is not reliably computable.
  - Fully asynchronous on `Dispatchers.IO` with 1000 ms sampling interval.
- [x] **Build Result**:
  - `./gradlew assembleDebug` passed in 1s with 0 errors.
- [x] **Emulator Verification (`Pixel_6`, API 35)**:
  - Full flow tested with `Android Easter Egg` and `AppProbe` targets.
  - Samples collected and correlated across scenario actions.
  - Live resource monitor and post-execution monitoring summary cards verified.
  - Cancellation test verified: Execution and monitoring stopped immediately without orphaned coroutines or crashes.
  - Re-run test verified: Resets previous run state and starts fresh monitoring session.
  - Screenshots captured: `screenshot_stage7.png`, `screenshot_stage7_complete.png`.
- [x] **Logcat Result**:
  - Clean (0 crashes, 0 uncaught exceptions).
- [x] **Known Limitations**:
  - Android 14/15 SELinux sandbox restricts reading `/proc/<pid>` and cross-UID `ActivityManager.getProcessMemoryInfo` for unprivileged third-party APKs without ADB host daemon transport. Metrics for restricted targets display `Unavailable` / `N/A` safely as designed.


