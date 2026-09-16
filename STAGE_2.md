# AppProbe — Stage 2: Application Discovery

## Goal

Implement the first functional AppProbe feature:

**Discover installed Android applications and display them in the AppProbe UI.**

Do not implement testing, automated analysis, monitoring, screenshots, reports, or advanced inspection yet.

## Requirements

### 1. Application Discovery

Create an application-discovery component under:

`com.appprobe.inspection`

It should use Android's `PackageManager` to retrieve installed applications.

For each application, collect only the information currently needed:

* Application name
* Package name
* Application icon
* Whether it is a system application

Create a clean model such as:

`AppInfo`

Keep Android-specific PackageManager logic out of the Compose UI.

### 2. Architecture

Use this basic flow:

`PackageManager → AppDiscoveryRepository → ViewModel → Compose UI`

Create appropriate classes under:

* `com.appprobe.inspection`
* `com.appprobe.ui`

Use a ViewModel for UI state.

Do not introduce unnecessary libraries or architectural complexity.

### 3. UI

Replace the Stage 1 status screen with an application-discovery screen.

The screen should contain:

* AppProbe title
* Short description
* Loading state
* List of discovered applications
* Application icon
* Application name
* Package name
* System/application indicator where appropriate

The UI should remain simple and functional.

### 4. Error Handling

Handle:

* Empty application list
* PackageManager errors
* Loading state

Do not allow an exception during application discovery to crash AppProbe.

Display a useful error message if discovery fails.

### 5. Permissions / Android Compatibility

Use only permissions that are actually required.

Do not request dangerous permissions.

Keep the implementation compatible with:

* compileSdk 35
* targetSdk 35
* minSdk 26

Do not change the SDK targets unless absolutely necessary.

## Important Constraints

* Kotlin only.
* Jetpack Compose + Material 3.
* Keep existing project structure.
* Do not add unrelated dependencies.
* Do not modify the emulator configuration.
* Do not start Stage 3.
* Do not implement automated testing yet.
* Do not implement accessibility-service functionality yet.
* Do not implement network monitoring yet.
* Do not implement performance/memory analysis yet.

## Verification

After implementation:

1. Build the project:

`./gradlew assembleDebug`

2. Install the APK:

`adb install -r app/build/outputs/apk/debug/app-debug.apk`

3. Launch:

`adb shell am start -n com.appprobe/.MainActivity`

4. Verify that AppProbe displays installed applications from the Pixel 6 emulator.

5. Verify that AppProbe remains stable and does not crash.

6. Check Logcat for AppProbe-related crashes.

## Completion Condition

Stage 2 is complete only when:

* The project builds successfully.
* The APK installs successfully.
* AppProbe launches successfully.
* Installed applications are discovered through PackageManager.
* Applications appear in the Compose UI.
* No major runtime crashes occur.

Stop after Stage 2 verification.

Do not proceed to Stage 3 automatically.

