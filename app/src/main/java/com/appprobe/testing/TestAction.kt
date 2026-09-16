package com.appprobe.testing

import java.util.UUID

/**
 * Supported test action types for scenario building.
 */
enum class TestActionType(val displayName: String, val description: String) {
    LAUNCH_APP("Launch App", "Starts the target application"),
    WAIT("Wait", "Pauses execution for a specified duration"),
    TAP("Tap", "Simulates a screen touch/tap action"),
    SWIPE("Swipe", "Performs a touch swipe gesture"),
    PRESS_BACK("Press Back", "Sends back navigation key event"),
    ROTATE("Rotate", "Rotates device screen orientation"),
    BACKGROUND_APP("Background App", "Sends app to background"),
    RESUME_APP("Resume App", "Brings app back to foreground")
}

/**
 * Model representing a single ordered test action in a scenario.
 */
data class TestAction(
    val id: String = UUID.randomUUID().toString(),
    val type: TestActionType,
    val parameter: String = ""
)

