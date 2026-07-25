package com.template.framework.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresPermission
import java.net.NetworkInterface

/**
 * 设备工具类
 *
 * 提供：
 * - 获取设备局域网 IPv4 地址
 * - 获取设备序列号（SN）
 * - 英文转大写（保留中文）
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
object DeviceUtils {

    /**
     * 获取设备当前所在网络的局域网 IPv4 地址
     * 优先返回非回环、非 IPv6 的地址。
     *
     * @return 局域网 IPv4 字符串，获取失败返回空字符串
     */
    fun getDeviceLocalIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address.hostAddress?.contains(":") == false) {
                        return address.hostAddress ?: ""
                    }
                }
            }
            ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 获取设备序列号（SN 码）
     *
     * 优先尝试 Build.SERIAL，失败时回退到 Android ID。
     *
     * 注意：
     * - Android 8.0+ 需要 READ_PHONE_STATE 权限才能获取序列号
     * - Android 10+ 需要系统级权限 READ_PRIVILEGED_PHONE_STATE，普通应用无法获取
     * - 如果没有权限或获取失败，会自动降级用 Android ID
     *
     * @param context 上下文
     * @return 设备序列号字符串
     */
    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    fun getDeviceSerialNumber(context: Context): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val serial = Build.getSerial()
                    if (serial != "unknown" && serial.isNotEmpty()) {
                        serial
                    } else {
                        getAndroidId(context)
                    }
                } catch (e: SecurityException) {
                    getAndroidId(context)
                }
            } else {
                @Suppress("DEPRECATION")
                val serial = Build.SERIAL
                if (serial.isNotEmpty() && serial != "unknown") serial else getAndroidId(context)
            }
        } catch (e: Exception) {
            getAndroidId(context)
        }
    }

    /**
     * 获取 Android ID
     */
    private fun getAndroidId(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 将字符串中的英文全部转为大写，中文和其他字符保持不变
     */
    fun toUpperCaseEnglish(text: String): String {
        return text.map { char ->
            if (char.isLetter() && char.code < 128) char.uppercaseChar() else char
        }.joinToString("")
    }
}