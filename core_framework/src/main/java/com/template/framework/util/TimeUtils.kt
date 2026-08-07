package com.template.framework.util

/**
 * Formats non-negative durations for compact UI display.
 *
 * - 中文：将毫秒时长格式化为播放器常用的时间文本。
 */
object TimeUtils {

    /**
     * Formats [milliseconds] as `MM:SS`, adding `HH:` when the duration reaches one hour.
     */
    fun formatTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    /** Formats [milliseconds] as `HH:MM:SS`, including zero hours. */
    fun formatTimeWithHours(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
