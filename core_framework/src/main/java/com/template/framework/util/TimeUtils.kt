package com.template.framework.util

/**
 * 时间格式化工具类
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
object TimeUtils {

    /**
     * 将毫秒转换为时分秒格式
     *
     * - 小于 1 小时："MM:SS"
     * - 大于等于 1 小时："HH:MM:SS"
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

    /**
     * 固定显示小时
     */
    fun formatTimeWithHours(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}