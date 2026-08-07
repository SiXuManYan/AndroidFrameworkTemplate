package com.template.framework.api.model.websocket

/**
 * Example message types for a WebSocket protocol.
 *
 * Define business-specific values in the App module and provide a parser similar to [fromValue].
 * - 中文：这里只保留演示值，真实消息类型应由业务模块定义。
 *
 * ## Example
 * ```kotlin
 * enum class MyMessageType(val value: String) {
 *     PING("ping"),
 *     PONG("pong"),
 *     BUSINESS("business");
 *
 *     companion object {
 *         fun fromValue(value: String): MyMessageType? = values().find { it.value == value }
 *     }
 * }
 * ```
 *
 * @property value serialized protocol value
 */
enum class WebSocketMessageType(val value: String) {

    /**
     * Heartbeat request.
     * - 中文：心跳请求。
     */
    PING("ping"),

    /**
     * Heartbeat response.
     * - 中文：心跳响应。
     */
    PONG("pong"),

    /**
     * Placeholder for a business payload.
     * - 中文：通用业务消息占位。
     */
    BUSINESS("business");

    companion object {
        /** Returns the matching message type, or `null` for an unknown protocol value. */
        fun fromValue(value: String): WebSocketMessageType? {
            return values().find { it.value == value }
        }
    }
}
