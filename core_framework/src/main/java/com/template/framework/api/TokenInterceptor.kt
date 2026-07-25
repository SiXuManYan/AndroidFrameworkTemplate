package com.template.framework.api

import com.template.framework.constants.FrameworkConstants
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Token 拦截器
 *
 * 自动为每个请求添加：
 * 1. `clientid` Header（标识客户端类型，可被业务覆盖）
 * 2. `Authorization: Bearer {token}` Header（如果存在 token）
 *
 * 使用示例：
 * ```kotlin
 * val tokenInterceptor = TokenInterceptor(
 *     getToken = { preferencesManager.accessToken.first() }
 * )
 * ```
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
class TokenInterceptor(
    private val getToken: suspend () -> String?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBuilder = request.newBuilder()

        // 添加 clientid header（如果请求未指定）
        if (request.header(FrameworkConstants.HEADER_CLIENT_ID) == null) {
            requestBuilder.header(FrameworkConstants.HEADER_CLIENT_ID, DEFAULT_CLIENT_ID)
        }

        // 添加 Authorization header（如果请求未指定）
        if (request.header(FrameworkConstants.HEADER_AUTHORIZATION) == null) {
            val token = runBlocking { getToken() }
            if (!token.isNullOrEmpty()) {
                requestBuilder.header(
                    FrameworkConstants.HEADER_AUTHORIZATION,
                    "${FrameworkConstants.HEADER_AUTHORIZATION_PREFIX}$token"
                )
            }
        }

        return chain.proceed(requestBuilder.build())
    }

    companion object {
        /**
         * 默认客户端 ID
         * 业务可在创建 TokenInterceptor 时自行覆盖此值
         */
        const val DEFAULT_CLIENT_ID = "428a8310cd442757ae699df5d894f051"
    }
}