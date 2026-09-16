package com.appprobe.inspection

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppInspectionRepository(private val context: Context) {

    suspend fun getAppInspectionInfo(packageName: String): Result<AppInspectionInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val pm = context.packageManager
                val flags = PackageManager.GET_META_DATA

                val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(packageName, flags)
                }

                val appInfo: ApplicationInfo = packageInfo.applicationInfo
                    ?: throw IllegalStateException("ApplicationInfo unavailable for $packageName")

                val appName = pm.getApplicationLabel(appInfo).toString().ifBlank { packageName }
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                        (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                val icon: ImageBitmap? = try {
                    drawableToImageBitmap(pm.getApplicationIcon(appInfo))
                } catch (e: Exception) {
                    null
                }

                val versionCode: Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }

                AppInspectionInfo(
                    appName = appName,
                    packageName = packageName,
                    icon = icon,
                    isSystemApp = isSystemApp,
                    versionName = packageInfo.versionName ?: "N/A",
                    versionCode = versionCode,
                    targetSdk = appInfo.targetSdkVersion,
                    minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        appInfo.minSdkVersion
                    } else 0,
                    uid = appInfo.uid,
                    firstInstallTime = packageInfo.firstInstallTime,
                    lastUpdateTime = packageInfo.lastUpdateTime
                )
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

