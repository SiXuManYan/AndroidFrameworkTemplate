package com.template.framework.api.model.websocket

/**
 * WebSocket 消息类型示例
 *
 * 推荐做法：业务侧定义自己的消息类型枚举，实现 [fromValue] 与解析逻辑。
 *
 * 使用示例：
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
 * @author Shiwei Wang
 * @date 2026-02
 */
enum class WebSocketMessageType(val value: String) {

    /** 心跳 */
    PING("ping"),

    /** 心跳响应 */
    PONG("pong"),

    /** 通用业务消息 */
    BUSINESS("business");

    companion object {
        /**
         * 根据字符串值获取枚举
         */
        fun fromValue(value: String): WebSocketMessageType? {
            return values().find { it.value == value }
        }
    }
}