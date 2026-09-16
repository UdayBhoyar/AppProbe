# AppProbe — Stage 6: Test Scenario Execution

## Objective

Implement the first real execution layer of AppProbe.

Stage 1–5 established:

* Android project foundation
* Application discovery
* Application inspection
* Target application selection
* Test scenario configuration

Stage 6 must take the configured `TestScenario` from Stage 5 and execute its actions sequentially against the selected target application on the Android emulator.

The execution layer must be designed so that future stages can attach runtime monitoring and analysis to the execution process.

## IMPORTANT: READ FIRST

Before making any changes:

1. Read `progress.md`.
2. Inspect the existing Stage 1–5 implementation.
3. Inspect:

   * `TestAction.kt`
   * `TestScenario.kt`
   * `ScenarioBuilderViewModel.kt`
   * `ScenarioBuilderScreen.kt`
   * `TargetApp.kt`
   * `TargetAppViewModel.kt`
   * `MainActivity.kt`
4. Understand the existing navigation and state architecture.

Do not recreate existing functionality.

Do not refactor unrelated code.

---

# Stage 6 Scope

## 1. Extend Test Actions With Execution Parameters

The existing actions must contain enough information to actually execute them.

Keep the action model extensible.

Actions requiring parameters should support them.

### Launch App

No additional user parameter is required.

The target package should be used.

### Wait

Support:

* duration in milliseconds

Example:

`Wait(2000)`

### Tap

Support:

* X coordinate
* Y coordinate

Example:

`Tap(500, 1000)`

### Swipe

Support:

* start X
* start Y
* end X
* end Y
* duration in milliseconds

Example:

`Swipe(500, 1500, 500, 500, 500)`

### Press Back

No additional parameter.

### Rotate

Support a simple rotation operation.

Use Android/ADB-supported rotation behavior.

### Background App

No additional parameter.

### Resume App

The target application should be brought back to the foreground.

Use the target package already stored in `TestScenario`.

Avoid creating duplicate target-selection logic.

---

# 2. Execution Engine

Create an execution layer under:

`com.appprobe.execution`

Create an `ExecutionEngine` responsible for executing a `TestScenario`.

The engine must:

1. Receive a `TestScenario`.
2. Execute actions in their existing order.
3. Wait for each action to complete before continuing.
4. Report success/failure for individual actions.
5. Stop execution when an unrecoverable action fails.
6. Support cancellation.

The execution engine must not contain Compose/UI code.

Keep the execution logic independent from the UI.

---

# 3. ADB Integration

Stage 6 may use ADB to control the emulator.

The implementation should execute the necessary ADB commands for:

### Launch

Use the target package to launch the application.

### Wait

Use a coroutine delay.

Do not unnecessarily execute a shell command for waiting.

### Tap

Use:

`adb shell input tap X Y`

### Swipe

Use:

`adb shell input swipe X1 Y1 X2 Y2 DURATION`

### Back

Use:

`adb shell input keyevent KEYCODE_BACK`

### Rotate

Use appropriate ADB/device commands to change the device rotation.

### Background

Use an Android-compatible mechanism to move the target app out of the foreground.

### Resume

Bring the target package back to the foreground.

Do not hard-code a specific application package.

The execution layer must use the target package from `TestScenario`.

---

# 4. ADB Command Abstraction

Do NOT scatter raw shell-command execution throughout the application.

Create a small abstraction under:

`com.appprobe.execution`

For example:

* `AdbExecutor`
* or an equivalent appropriately named abstraction.

Its responsibility is to execute commands and return:

* success/failure
* exit status when available
* stdout
* stderr

Keep this abstraction replaceable so that a future device/emulator abstraction can be introduced without rewriting the execution engine.

Do not add external ADB libraries.

Use the local Android SDK / ADB executable available in the development environment.

---

# 5. Execution State

Create execution state under the existing architecture.

Support at least:

```text
Idle
Running
Completed
Failed
Cancelled
```

While running, expose:

* total number of actions
* current action index
* current action
* completed action count
* action status/message

The UI must be able to observe this state.

Use Kotlin coroutines / StateFlow consistently with the existing architecture.

Do not introduce a database.

Do not persist execution history yet.

---

# 6. Execution Result Model

Create an execution-result representation.

Each action execution should record:

* action
* action index
* success/failure
* optional message
* execution duration

The overall result should contain:

* target package
* scenario name
* start time
* end time
* action results
* overall status

Keep this model independent from Compose.

This result will later be consumed by performance and memory-analysis stages.

---

# 7. Execution Screen

Create:

`ScenarioExecutionScreen.kt`

under the existing UI package.

The screen should display:

### Header

* AppProbe
* target application name
* target package
* scenario name

### Progress

Display something similar to:

```text
Executing Test Scenario

Action 3 of 8

Swipe
```

Show a progress indicator.

### Action List

Display all scenario actions.

Each action should visually indicate:

* Pending
* Running
* Passed
* Failed
* Cancelled

### Controls

Provide:

* Start / Execute
* Cancel

After completion:

* Show overall result.
* Provide a Back button.

Keep the UI simple and functional.

Do not over-design it.

---

# 8. Navigation

Extend the existing flow:

```text
Discovery
   ↓
Inspection
   ↓
Target Ready
   ↓
Scenario Builder
   ↓
Scenario Execution
```

Add an execution action from the Scenario Builder.

The configured scenario must be passed to the execution layer.

Do not create a second scenario.

Do not create a second target-selection system.

The target package must come from the existing `TestScenario`.

Back navigation must continue working.

---

# 9. Important Execution Safety

Before executing:

* Verify that a target package exists.
* Verify that the scenario contains at least one action.
* Show an appropriate error instead of crashing if execution cannot start.

During execution:

* Respect coroutine cancellation.
* Do not block the main/UI thread.
* Do not execute long-running processes synchronously on the UI thread.
* Handle ADB failures gracefully.
* Do not continue blindly after a critical action failure.

The application itself must remain responsive while execution is running.

---

# 10. Emulator Compatibility

The current development environment is:

* Fedora Linux
* Android SDK:
  `/home/ulias/Android/Sdk`
* ADB:
  `/home/ulias/Android/Sdk/platform-tools/adb`
* Emulator:
  `Pixel_6`
* Android:
  API 35 / Android 15

Do not hard-code `/home/ulias` into the AppProbe application.

The development machine may use that path, but the application architecture should keep the ADB executable path configurable/discoverable.

---

# 11. What NOT To Implement

This is extremely important.

DO NOT implement:

* memory monitoring
* heap dumps
* allocation tracking
* CPU monitoring
* GPU monitoring
* battery monitoring
* thread monitoring
* lifecycle analytics
* memory leak detection
* retained-object analysis
* performance graphs
* reports
* PostgreSQL
* Redis/Valkey
* MinIO
* FastAPI
* Next.js
* network communication
* cloud services
* AI analysis

Those belong to later stages.

Stage 6 is ONLY about reliable scenario execution.

---

# 12. Dependencies

Do not add a new dependency unless absolutely necessary.

Reuse the existing:

* Kotlin
* Coroutines
* StateFlow
* Jetpack Compose
* Material 3
* ViewModel

Do not introduce an ADB third-party library.

---

# 13. Verification

After implementation:

## Build

Run:

```bash
./gradlew assembleDebug
```

The build must complete successfully.

## Emulator

Use the existing Pixel_6 Android 15 / API 35 emulator.

Verify:

```bash
adb devices
```

Expected:

```text
emulator-5554    device
```

## Installation

Install the generated APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Runtime

Launch:

```bash
adb shell am start -n com.appprobe/.MainActivity
```

Verify the complete flow:

```text
Discovery
→ Inspection
→ Target Selection
→ Target Ready
→ Scenario Builder
→ Scenario Execution
```

## Execution Test

Create a small test scenario such as:

```text
1. Launch App
2. Wait 2000 ms
3. Press Back
4. Launch App
```

Then execute it.

Verify:

* Target application launches.
* Wait executes correctly.
* Back executes correctly.
* Target application can be launched again.
* Action progress updates correctly.
* Individual action statuses update.
* Overall execution completes.
* Cancel works during execution.
* App remains responsive.

Also test:

```text
Tap
Swipe
Rotate
Background
Resume
```

where practical.

## Failure Test

Verify that an invalid/unavailable execution condition does not crash AppProbe.

The UI should display an appropriate failure state.

## Logcat

Check:

```bash
adb logcat -d *:E | grep -i com.appprobe
```

There should be no AppProbe crash or uncaught exception.

---

# 14. Progress Log

After successful verification, update:

`progress.md`

Add:

```text
Stage 6: Test Scenario Execution — COMPLETED
```

Include:

* files created
* files modified
* execution capabilities
* ADB integration
* execution state
* execution result model
* UI/navigation changes
* build result
* emulator verification
* logcat result
* remaining issues

Only mark Stage 6 complete after actual runtime execution has been verified.

---

# 15. Token-Efficient Development Rules

This project is being developed incrementally.

Follow these rules strictly:

1. Inspect existing files before editing.
2. Only inspect files relevant to Stage 6.
3. Do not repeatedly reread the entire project.
4. Reuse existing architecture.
5. Do not rewrite working Stage 1–5 files unnecessarily.
6. Do not add future-stage functionality.
7. Do not add dependencies unless required.
8. Make small, focused changes.
9. Build after implementation.
10. Test the actual execution flow on the emulator.
11. Fix only issues related to Stage 6.
12. Update `progress.md` only after verification.

## Completion Rule

When Stage 6 is implemented and verified successfully:

STOP.

Do not begin Stage 7.

Do not implement memory/performance analysis yet.

Report:

* what was implemented
* files created/modified
* build result
* runtime tests performed
* ADB execution results
* any remaining issues
