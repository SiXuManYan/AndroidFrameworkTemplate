package com.template.framework.api.model.auth

/**
 * 设备登录请求
 *
 * 示例：
 * ```kotlin
 * val request = DeviceLoginRequest(
 *     grantType = "device",
 *     userId = "123456",
 *     snNumber = "SN001"
 * )
 * ```
 *
 * @param clientId 客户端标识（可选，默认 null 时由 TokenInterceptor 添加）
 * @param grantType 授权类型，由 App 业务定义，如 "device"、"face"、"card"
 * @param userId 用户 ID（可空）
 * @param cardNo 卡号（可空）
 * @param snNumber 设备序列号
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
data class DeviceLoginRequest(
    val clientId: String? = null,
    val grantType: String,
    val userId: String? = null,
    val cardNo: String? = null,
    val snNumber: String
)