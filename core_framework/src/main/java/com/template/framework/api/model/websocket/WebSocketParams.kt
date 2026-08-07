package com.template.framework.api.model.websocket

/**
 * Shared names used by the sample WebSocket handshake and endpoint.
 *
 * - 中文：集中维护 WebSocket 路径、查询参数和 Header 名称。
 *
 * @property value serialized path segment, query key, or header name
 */
enum class WebSocketParams(val value: String) {

    /**
     * Endpoint path without scheme or host.
     * - 中文：不含协议和主机的路径。
     */
    Path("resource/websocket"),

    /** Authorization query key retained for protocol compatibility. */
    QueryAuth("Authorization"),

    /** Bearer prefix retained for protocol compatibility. */
    QueryBearerPrefix("Bearer "),

    /**
     * Client identifier header name.
     * - 中文：客户端标识 Header。
     */
    HeaderClientId("clientid"),

    /**
     * Device serial-number header name.
     * - 中文：设备序列号 Header。
     */
    HeaderSnNumber("snNumber")
}
