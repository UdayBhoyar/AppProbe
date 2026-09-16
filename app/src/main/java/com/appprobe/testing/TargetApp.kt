package com.appprobe.testing

/**
 * Model representing the selected target application for testing and analysis.
 * The [packageName] serves as the primary and authoritative identifier.
 */
data class TargetApp(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false
)

