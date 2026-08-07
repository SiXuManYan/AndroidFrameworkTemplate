package com.template.framework.api

import com.template.framework.constants.FrameworkConstants
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the default client identifier and optional bearer token to outgoing requests.
 *
 * Existing `clientid` and `Authorization` headers are preserved, allowing individual requests to
 * override the defaults. Token lookup uses `runBlocking` on the OkHttp interceptor thread, so the
 * provider should return promptly.
 * - 中文：自动补充客户端标识和 Token，但不会覆盖请求中已经设置的同名 Header。
 *
 * ## Usage
 * ```kotlin
 * val tokenInterceptor = TokenInterceptor(
 *     getToken = { preferencesManager.accessToken.first() }
 * )
 * ```
 *
 * @param getToken suspending provider queried for the latest token on each request
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
        /** Default client identifier used when a request does not provide `clientid`. */
        const val DEFAULT_CLIENT_ID = "428a8310cd442757ae699df5d894f051"
    }
}
