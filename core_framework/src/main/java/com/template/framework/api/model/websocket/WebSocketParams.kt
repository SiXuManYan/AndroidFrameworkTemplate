package com.template.framework.api.model.websocket

/**
 * WebSocket 协议参数
 *
 * 集中管理 WebSocket 的 scheme / 路径 / Header 名称等参数，
 * 避免在代码中硬编码字符串。
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
enum class WebSocketParams(val value: String) {

    /** WebSocket 路径（不含 scheme 与 host） */
    Path("resource/websocket"),

    /** URL 查询参数名：Authorization */
    QueryAuth("Authorization"),

    /** URL 查询参数值前缀：Bearer */
    QueryBearerPrefix("Bearer "),

    /** HTTP Header 名称：clientid */
    HeaderClientId("clientid"),

    /** HTTP Header 名称：snNumber */
    HeaderSnNumber("snNumber")
}