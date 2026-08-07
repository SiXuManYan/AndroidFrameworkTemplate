package com.template.framework.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

/** Reads package metadata for the current application. */
object AppInfoUtils {

    /** Creates a permission-free dialer intent without placing the call directly. */
    @JvmStatic
    fun createDialIntent(phoneNumber: String): Intent = Intent(
        Intent.ACTION_DIAL,
        Uri.fromParts("tel", phoneNumber.trim(), null)
    )

    /** Opens the system dialer when one is available. Blank numbers are rejected. */
    @JvmStatic
    fun openDialer(context: Context, phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false

        val intent = createDialIntent(phoneNumber)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(context.packageManager) == null) return false

        return runCatching { context.startActivity(intent) }.isSuccess
    }

    @JvmStatic
    fun getVersionName(context: Context): String? = getPackageInfo(context)?.versionName

    @JvmStatic
    fun getVersionCode(context: Context): Long? {
        val packageInfo = getPackageInfo(context) ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }

    @JvmStatic
    fun getPackageInfo(context: Context): PackageInfo? {
        val appContext = context.applicationContext
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.packageManager.getPackageInfo(
                    appContext.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}
