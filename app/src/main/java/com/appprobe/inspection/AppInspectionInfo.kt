package com.appprobe.inspection

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Detailed metadata for a single inspected application.
 */
data class AppInspectionInfo(
    val appName: String,
    val packageName: String,
    val icon: ImageBitmap? = null,
    val isSystemApp: Boolean = false,
    val versionName: String,
    val versionCode: Long,
    val targetSdk: Int,
    val minSdk: Int,
    val uid: Int,
    val firstInstallTime: Long,
    val lastUpdateTime: Long
)

