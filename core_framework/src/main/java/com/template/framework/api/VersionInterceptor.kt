package com.template.framework.api

import com.template.framework.Framework
import com.template.framework.constants.FrameworkConstants
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 版本拦截器
 *
 * 自动为每个请求添加：
 * - `VersionCode` Header
 * - `VersionName` Header
 *
 * 版本号从 [com.template.framework.api.FrameworkConfig] 中读取，
 * 业务可在 Application.onCreate 中通过 Framework.init() 注入。
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
class VersionInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBuilder = request.newBuilder()

        val config = runCatching { Framework.getConfig() }.getOrNull()
        if (config != null) {
            if (request.header(FrameworkConstants.HEADER_VERSION_CODE) == null) {
                requestBuilder.header(
                    FrameworkConstants.HEADER_VERSION_CODE,
                    config.versionCode.toString()
                )
            }
            if (request.header(FrameworkConstants.HEADER_VERSION_NAME) == null) {
                requestBuilder.header(
                    FrameworkConstants.HEADER_VERSION_NAME,
                    config.versionName
                )
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}