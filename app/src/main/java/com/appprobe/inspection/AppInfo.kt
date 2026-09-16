package com.appprobe.inspection

import androidx.compose.ui.graphics.ImageBitmap

data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: ImageBitmap? = null,
    val isSystemApp: Boolean = false
)

