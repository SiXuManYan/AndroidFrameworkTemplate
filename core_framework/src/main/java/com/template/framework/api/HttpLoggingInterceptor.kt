package com.template.framework.api

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import timber.log.Timber
import java.io.IOException

/**
 * 自定义 HTTP 日志拦截器
 *
 * 详细打印：
 * - 请求方法、URL、Headers、Body
 * - 响应状态码、Headers、Body
 * - 请求耗时
 *
 * 使用 Timber 输出，可被 [com.template.framework.util.TimberUtil] 控制 Release 模式下是否输出。
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
class HttpLoggingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        logRequest(request)

        val startTime = System.currentTimeMillis()
        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "请求失败：${e.message}")
            throw e
        }
        val duration = System.currentTimeMillis() - startTime

        logResponse(response, duration)
        return response
    }

    private fun logRequest(request: Request) {
        Timber.tag(TAG).d("═══════════════════════════════════════════════════════")
        Timber.tag(TAG).d("--> ${request.method} ${request.url}")
        Timber.tag(TAG).d("【Request Headers】")
        request.headers.forEach { (k, v) -> Timber.tag(TAG).d("  $k: $v") }

        val body = request.body
        if (body != null) {
            Timber.tag(TAG).d("【Request Body】")
            try {
                val buffer = Buffer()
                body.writeTo(buffer)
                Timber.tag(TAG).d("  ${buffer.readUtf8()}")
            } catch (e: IOException) {
                Timber.tag(TAG).e(e, "  读取请求 Body 失败")
            }
        }
        Timber.tag(TAG).d("--> END ${request.method}")
    }

    private fun logResponse(response: Response, duration: Long) {
        Timber.tag(TAG).d("")
        Timber.tag(TAG).d("<-- ${response.code} ${response.message} ${response.request.url} (${duration}ms)")
        Timber.tag(TAG).d("【Response Headers】")
        response.headers.forEach { (k, v) -> Timber.tag(TAG).d("  $k: $v") }

        val body: ResponseBody? = response.body
        if (body != null) {
            Timber.tag(TAG).d("【Response Body】")
            try {
                val source = body.source()
                source.request(Long.MAX_VALUE)
                val buffer = source.buffer
                val maxBytes = minOf(buffer.size, 1024L * 1024L)
                Timber.tag(TAG).d("  ${buffer.clone().readUtf8(maxBytes)}")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "  读取响应 Body 失败：${e.message}")
            }
        }
        Timber.tag(TAG).d("<-- END HTTP")
        Timber.tag(TAG).d("═══════════════════════════════════════════════════════")
    }

    companion object {
        private const val TAG = "OkHttp"
    }
}