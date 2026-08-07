package com.template.framework.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.template.framework.api.model.ApiResponse
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

/**
 * Detects an expired access token from either HTTP `401` or JSON business code `401`.
 *
 * On expiry, [clearToken] runs first and [onTokenExpired] runs second. Both execute on the OkHttp
 * interceptor thread; UI work must be dispatched to the main thread.
 * - 中文：同时处理 HTTP 401 和业务码 401，并在清除本地 Token 后通知 App。
 *
 * @param onTokenExpired callback that informs the App about the expired session
 * @param clearToken suspending action that removes the persisted access token
 */
class AuthErrorInterceptor(
    private val onTokenExpired: () -> Unit,
    private val clearToken: suspend () -> Unit
) : Interceptor {

    private val gson = Gson()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        var isTokenExpired = false

        // 1) HTTP 状态码 401
        if (response.code == 401) {
            Timber.tag(TAG).e("Token 已过期, 收到 HTTP 401 Unauthorized")
            isTokenExpired = true
        } else {
            // 2) HTTP 200 但业务 code == 401
            val responseBody = response.peekBody(Long.MAX_VALUE)
            val contentType = responseBody.contentType()
            if (contentType?.type == "application" && contentType.subtype == "json") {
                try {
                    val jsonObject = gson.fromJson(responseBody.string(), JsonObject::class.java)
                    if (jsonObject.has("code")) {
                        val code = jsonObject.get("code").asInt
                        if (code == ApiResponse.TOKEN_EXPIRED_CODE) {
                            Timber.tag(TAG).w("Token 业务码 401")
                            isTokenExpired = true
                        }
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).d("解析响应体失败：${e.message}")
                }
            }
        }

        if (isTokenExpired) {
            runBlocking { clearToken() }
            onTokenExpired()
        }

        return response
    }

    companion object {
        private const val TAG = "AuthError"
    }
}
