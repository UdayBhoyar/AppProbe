# AppProbe — Stage 4: Target Application Selection

## Overall Project Goal

AppProbe is a local Android application testing and analysis platform.

The final system should allow the user to:

1. Discover installed Android applications.
2. Select an application to analyze.
3. Configure a scripted test scenario.
4. Launch/control the target application.
5. Execute actions such as open, scroll, navigate, rotate, background/resume, and repeat.
6. Collect runtime metrics including:
   - memory/heap
   - allocations
   - CPU
   - GPU
   - threads
   - battery
   - lifecycle events
7. Analyze collected data for abnormal memory growth and possible memory leaks.
8. Generate a readable analysis/report.

Do NOT implement the complete testing or memory-analysis system yet.

---

# Current Progress

Stage 1 — Project Foundation: COMPLETED

Stage 2 — Application Discovery: COMPLETED

Stage 3 — Application Inspection: COMPLETED

Current architecture includes:

- com.appprobe.inspection
- com.appprobe.testing
- com.appprobe.device
- com.appprobe.analysis
- com.appprobe.execution
- com.appprobe.reporting
- com.appprobe.ui

The app currently:

- discovers installed applications
- displays application information
- opens an inspection screen
- supports back navigation
- runs successfully on Pixel_6
- targets Android 15 / API 35
- uses compileSdk 36
- uses minSdk 26

---

# Stage 4 Goal

Implement the foundation for selecting one installed application as the
TARGET APPLICATION for future testing.

The user should be able to:

1. Browse installed applications.
2. Select an application.
3. Clearly see which application is selected.
4. Confirm the selection.
5. Return to the discovery screen without losing stability.

The selected application must be represented by its package name.

The package name is the authoritative identifier.

---

# Important Architecture Rule

Do NOT duplicate application-discovery logic.

Reuse the existing AppInfo model and existing discovery/repository logic
where appropriate.

Do not rewrite Stage 2 or Stage 3 code unless required for integration.

Keep responsibilities separated:

inspection/
    application metadata

testing/
    future test configuration

device/
    future emulator/device control

execution/
    future test execution

analysis/
    future runtime analysis

reporting/
    future reports

ui/
    Compose screens and ViewModels

---

# Required Implementation

## 1. Target Application Model

Create a small model representing the selected target.

Example responsibility:

- packageName
- appName
- optional icon/reference information if already available

Do not store unnecessary duplicated metadata.

The package name must remain the primary identifier.

---

## 2. Target Application State

Create state management for the currently selected application.

It should support:

- no application selected
- application selected

The state should survive normal Compose recomposition.

Do not introduce a database yet.

Do not introduce DataStore yet.

Do not introduce a network layer.

---

## 3. Selection UI

Modify the existing discovery UI minimally.

When an application is selected:

- visually indicate the selected application
- show its name
- show its package name
- provide a clear selection/confirmation action

Do not redesign the entire Stage 2 UI.

---

## 4. Selection Confirmation

After confirmation, show a simple target-application state screen/card.

Example information:

Target Application

App Name
Package Name

Status:
Ready for testing

This is only a foundation for future stages.

Do NOT start the test automatically.

---

## 5. Navigation

Maintain the existing navigation behavior.

Required flow:

Application List
    ↓
Select Application
    ↓
Confirm Target
    ↓
Target Ready

Back navigation must continue working.

Do not introduce Navigation Compose unless there is a strong existing
architectural reason.

The current lightweight navigation approach can remain.

---

# What NOT To Implement In Stage 4

Do NOT implement:

- test execution
- AccessibilityService
- UI automation
- ADB automation
- memory monitoring
- heap dumps
- CPU monitoring
- GPU monitoring
- battery monitoring
- leak detection
- scripted actions
- Redis
- PostgreSQL
- MinIO
- FastAPI
- Next.js
- cloud services
- network APIs

Those belong to later stages.

---

# Verification

Build:

./gradlew assembleDebug

Install:

adb install -r app/build/outputs/apk/debug/app-debug.apk

Launch:

adb shell am start -n com.appprobe/.MainActivity

Verify manually:

1. Application list appears.
2. Select an application.
3. Selected state is visually obvious.
4. Confirm selection.
5. Target application information is displayed.
6. Package name is correct.
7. Navigate back.
8. Application does not crash.

Check process:

adb shell pidof com.appprobe

Check errors:

adb logcat -d *:E | grep -i com.appprobe || echo "Clean!"

---

# Important Things To Remember

1. Do not break working Stage 1–3 functionality.
2. Do not rewrite existing repositories unnecessarily.
3. Package name is the authoritative target identifier.
4. Keep Stage 4 small and focused.
5. Do not implement future-stage functionality early.
6. Prefer existing architecture and dependencies.
7. Do not add dependencies unless genuinely necessary.
8. Do not introduce persistent storage yet.
9. Keep UI implementation simple and functional.
10. Build and verify before declaring Stage 4 complete.

---

# Token-Efficient Development Instructions

Before modifying anything:

1. Read this STAGE_4_PROMPT.md.
2. Read the existing project structure.
3. Inspect only files directly relevant to:
   - MainActivity
   - AppDiscoveryScreen
   - AppInfo
   - existing ViewModels/state
4. Do not dump or reread the entire project.
5. Reuse existing code wherever possible.

Implementation rules:

- Make the minimum necessary changes.
- Do not rewrite working Stage 2/3 code.
- Do not create speculative abstractions.
- Do not add dependencies unless required.
- Do not implement future stages.
- Keep code idiomatic Kotlin/Compose.
- After implementation, compile the project.
- Fix only errors related to Stage 4.
- Report the changed files and verification result concisely.

At the end, provide:

## Stage 4 Result

- Files created:
- Files modified:
- Build result:
- Runtime result:
- Remaining issues:
- Suggested next stage: