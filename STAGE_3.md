# AppProbe — Stage 3: Application Inspection

Implement Stage 3 only.

## Goal

Extend AppProbe so that a user can select an installed application from the Stage 2 application list and view basic application metadata on a dedicated inspection screen.

Do NOT implement automated testing, APK analysis, permission analysis, process monitoring, network inspection, or reporting yet.

## Requirements

### 1. App selection

Make each application item in AppDiscoveryScreen clickable.

When the user taps an application, navigate to its inspection screen.

Use the existing packageName as the identifier.

### 2. Application inspection

Create an inspection package under:

com.appprobe.inspection

Create an appropriate model, repository, ViewModel, and Compose UI where necessary.

The inspection screen should display:

- Application name
- Package name
- Version name
- Version code
- Target SDK
- Minimum SDK
- UID
- System app / User app classification
- First install time
- Last update time

Use Android PackageManager as the source of truth.

Do not hardcode application information.

### 3. Architecture

Keep responsibilities separated:

PackageManager access:
com.appprobe.inspection

State/business logic:
ViewModel

UI:
com.appprobe.ui

Do not put PackageManager queries directly inside composables.

Perform PackageManager work off the main thread where appropriate.

Reuse the existing AppInfo/package discovery architecture instead of creating duplicate application-discovery logic.

### 4. UI

Create:

AppInspectionScreen.kt

Use Jetpack Compose Material 3.

The screen should contain:

- Top app bar
- Back navigation
- Application icon
- Application name
- Package name
- Metadata sections/cards

Make the layout readable on the Pixel 6 emulator.

Handle loading and error states.

If an application cannot be found, display a clear error instead of crashing.

### 5. Navigation

Add simple navigation between:

AppDiscoveryScreen
        ↓
AppInspectionScreen

Do not introduce unnecessary navigation architecture.

Use the project's existing dependencies where possible. Add a navigation dependency only if required.

### 6. Compatibility

Maintain:

compileSdk = 36
targetSdk = 35
minSdk = 26

The emulator being used for development is:

Pixel_6
Android 15
API 35

Do not change the Android SDK target configuration.

### 7. Verification

After implementation:

1. Build:

./gradlew assembleDebug

2. Install:

adb install -r app/build/outputs/apk/debug/app-debug.apk

3. Launch:

adb shell am start -n com.appprobe/.MainActivity

4. Verify:

- Application list appears.
- Tapping an application opens inspection.
- Correct application name is displayed.
- Correct package name is displayed.
- Version information is displayed.
- SDK information is displayed.
- UID is displayed.
- Install/update timestamps are displayed.
- Back button returns to the application list.
- Selecting multiple different applications shows their respective metadata.
- No crash occurs.

5. Check crashes:

adb logcat -d *:E | grep -i com.appprobe

## Important implementation rule

Implement ONLY Stage 3.

Do not begin Stage 4 or add speculative features.

When finished, report:

- Files created
- Files modified
- Dependencies added
- Build result
- Emulator verification result
- Any issues encountered

Do not modify unrelated project files.