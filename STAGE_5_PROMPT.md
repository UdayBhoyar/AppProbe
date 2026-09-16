# AppProbe — Stage 5: Test Scenario Configuration

## Objective

Implement the foundation for configuring automated test scenarios for the currently selected target application.

The final AppProbe goal is to analyze Android applications by executing controlled test scenarios and later collecting runtime information such as memory, CPU, GPU, threads, battery, lifecycle events, and potential memory-leak indicators.

**Stage 5 must ONLY build the test-scenario configuration layer. Do NOT implement test execution or runtime analysis yet.**

## First

Read the existing project structure and `progress.md`.

Understand the existing Stage 1–4 implementation before modifying anything.

Do not recreate existing functionality.

## Required Functionality

### 1. Test Action Model

Create a test-action representation under:

`com.appprobe.testing`

Support these actions:

* Launch App
* Wait
* Tap
* Swipe
* Press Back
* Rotate
* Background App
* Resume App

The model should be extensible because future stages will add more actions.

Actions must preserve their order.

### 2. Test Scenario Model

Create a scenario model containing:

* scenario name
* target application/package
* ordered list of test actions

The scenario should be independent from the UI.

### 3. Scenario Builder Screen

Create a Compose screen under the existing UI architecture.

The screen should contain:

* Top app bar
* Selected target application name
* Selected target package name
* Scenario name
* Ordered action list
* Add Action button
* Remove action functionality
* Move action up
* Move action down
* Clear scenario

When adding an action, provide a simple Material 3 action-selection UI.

Keep the UI clean and functional. Do not over-design it.

### 4. Navigation

Extend the existing Stage 4 flow:

Discovery
→ Inspection
→ Target Ready
→ Scenario Builder

The Scenario Builder must use the target application already selected through Stage 4.

Do not create a second target-selection mechanism.

Back navigation must continue to work correctly.

### 5. State Management

Use the existing ViewModel/state architecture.

The scenario state should survive Compose recomposition.

Keep UI state separate from the data models.

Do not introduce a database or persistence mechanism in Stage 5.

### 6. Important Constraints

Do NOT implement:

* ADB execution
* shell commands
* automated taps
* automated swipes
* emulator control
* memory monitoring
* CPU monitoring
* GPU monitoring
* battery monitoring
* lifecycle monitoring
* leak detection
* reports
* backend/API
* database
* network communication
* cloud services

These are future-stage responsibilities.

Do not add dependencies unless absolutely necessary.

Reuse the existing Material 3 / Compose setup.

Do not modify working Stage 1–4 functionality unnecessarily.

Do not refactor unrelated code.

## Verification

After implementation:

1. Run:

`./gradlew assembleDebug`

2. Install the APK on the existing Pixel_6 Android 15 / API 35 emulator.

3. Launch AppProbe.

4. Verify:

* Discovery still works.
* Application inspection still works.
* Target selection still works.
* Target Ready screen still works.
* Scenario Builder opens after target selection.
* Target application information is displayed correctly.
* Actions can be added.
* Actions remain in the correct order.
* Actions can be removed.
* Actions can be moved up/down.
* Scenario can be cleared.
* Back navigation works.
* App does not crash.

5. Check logcat for AppProbe errors.

## Completion Rule

When all Stage 5 functionality is implemented and verified, STOP.

Do not begin Stage 6.

Update `progress.md` with:

* Stage 5 status
* files created/modified
* implemented functionality
* build result
* runtime verification result
* any remaining issues

## Token Efficiency

Work incrementally.

Before changing files, inspect only the files relevant to Stage 5 and the existing navigation/state architecture.

Prefer small modifications over rewriting files.

Do not generate explanations for code that does not need changing.

Do not implement future stages.

If the existing architecture already supports something required here, reuse it instead of creating another implementation.
