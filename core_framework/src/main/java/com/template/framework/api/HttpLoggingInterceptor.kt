package com.template.framework.api

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import timber.log.Timber
import java.io.IOException

/**
 * Logs HTTP request/response metadata, bodies, and elapsed time through Timber.
 *
 * Response output is capped at 1 MiB and reads from a cloned buffer, so the downstream response
 * body remains consumable. The current implementation still buffers the complete response before
 * logging. [com.template.framework.util.TimberUtil] suppresses output in release mode.
 *
 * - 中文：记录请求与响应详情，响应正文最多打印 1 MiB，且不会消费原始响应流。
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
