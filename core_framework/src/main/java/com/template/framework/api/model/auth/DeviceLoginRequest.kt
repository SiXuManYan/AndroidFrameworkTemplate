package com.template.framework.api.model.auth

/**
 * Request body for the sample device-login endpoint.
 *
 * - 中文：示例设备登录请求；字段含义最终以真实服务端协议为准。
 *
 * ## Example
 * ```kotlin
 * val request = DeviceLoginRequest(
 *     grantType = "device",
 *     userId = "123456",
 *     snNumber = "SN001"
 * )
 * ```
 *
 * @property clientId optional client identifier in the JSON body; unrelated to the interceptor
 * header with the same meaning
 * @property grantType authorization strategy such as `device`, `face`, or `card`
 * @property userId optional user identifier
 * @property cardNo optional card number
 * @property snNumber device serial number
 */
data class DeviceLoginRequest(
    val clientId: String? = null,
    val grantType: String,
    val userId: String? = null,
    val cardNo: String? = null,
    val snNumber: String
)
