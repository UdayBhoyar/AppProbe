package com.appprobe.inspection

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppDiscoveryRepository(private val context: Context) {

    suspend fun getInstalledApplications(): Result<List<AppInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val packageManager = context.packageManager
            val flags = PackageManager.GET_META_DATA
            val installedApps = packageManager.getInstalledApplications(flags)

            installedApps.map { appInfo ->
                val appName = packageManager.getApplicationLabel(appInfo).toString().ifBlank {
                    appInfo.packageName
                }
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                        (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                val iconBitmap = try {
                    val drawable = packageManager.getApplicationIcon(appInfo)
                    drawableToImageBitmap(drawable)
                } catch (e: Exception) {
                    null
                }

                AppInfo(
                    appName = appName,
                    packageName = appInfo.packageName,
                    icon = iconBitmap,
                    isSystemApp = isSystemApp
                )
            }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
        }
    }

    private fun drawableToImageBitmap(drawable: Drawable): ImageBitmap? {
        return try {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                return drawable.bitmap.asImageBitmap()
            }
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}

