package com.template.framework.api

import com.template.framework.Framework
import com.template.framework.constants.FrameworkConstants
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * 网络模块
 *
 * 提供 OkHttpClient / Retrofit / ApiService 的工厂方法。
 *
 * ## SSL 证书策略
 * - 默认使用系统信任链和主机名校验
 * - 配置 [FrameworkConfig.sslCertRawResId] 时使用指定的 CA 或服务器证书
 *
 * ## Token 失效回调
 * 通过 [setOnTokenExpired] 或 [com.template.framework.Framework.setOnTokenExpired] 设置。
 *
 * ## 自定义 ApiService
 * 默认提供 [ApiService]；如需更多接口，可在 App 模块自定义并通过 [createApiService] 创建。
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
object NetworkModule {

    private var retrofit: Retrofit? = null

    /**
     * Token 失效全局回调（AuthErrorInterceptor 触发时调用）
     */
    var onTokenExpired: (() -> Unit)? = null

    /**
     * 配置 SSL 证书处理
     *
     * @param builder OkHttpClient.Builder
     */
    fun configureSslSocketFactory(builder: OkHttpClient.Builder) {
        val config = Framework.getConfig()
        config.sslCertRawResId?.let { trustCustomCert(builder, it) }
    }

    private fun trustCustomCert(builder: OkHttpClient.Builder, certResId: Int) {
        try {
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val certificate = Framework.getContext().resources
                .openRawResource(certResId)
                .use(certificateFactory::generateCertificate)

            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null, null)
                setCertificateEntry("custom_server", certificate)
            }
            val trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            ).apply { init(keyStore) }
            val trustManager = trustManagerFactory.trustManagers
                .filterIsInstance<X509TrustManager>()
                .single()

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(trustManager), java.security.SecureRandom())
            builder.sslSocketFactory(sslContext.socketFactory, trustManager)
        } catch (e: Exception) {
            throw IllegalStateException("服务器证书配置失败", e)
        }
    }

    /**
     * 创建 OkHttpClient
     *
     * @param getToken 获取 token 的函数，传 null 则不添加 Authorization
     * @param clearToken Token 失效时的清除函数，传 null 则不清理
     */
    fun createOkHttpClient(
        getToken: suspend () -> String? = { null },
        clearToken: suspend () -> Unit = {}
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(FrameworkConstants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(FrameworkConstants.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(FrameworkConstants.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor())
            .addInterceptor(VersionInterceptor())
            .addInterceptor(TokenInterceptor(getToken))
            .addInterceptor(AuthErrorInterceptor(
                onTokenExpired = { onTokenExpired?.invoke() },
                clearToken = clearToken
            ))

        configureSslSocketFactory(builder)
        return builder.build()
    }

    /**
     * 创建 Retrofit 实例
     *
     * @param baseUrl 服务器基础 URL（格式：http://ip:port 或 https://ip:port），必须以 `/` 结尾
     */
    fun createRetrofit(
        baseUrl: String,
        getToken: suspend () -> String? = { null },
        clearToken: suspend () -> Unit = {}
    ): Retrofit {
        val client = createOkHttpClient(getToken, clearToken)
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * 创建默认的 ApiService
     */
    fun createApiService(
        baseUrl: String,
        getToken: suspend () -> String? = { null },
        clearToken: suspend () -> Unit = {}
    ): ApiService {
        return createRetrofit(baseUrl, getToken, clearToken).create(ApiService::class.java)
    }

    /**
     * 创建自定义 ApiService
     *
     * 使用示例：
     * ```kotlin
     * interface MyApi {
     *     @GET("custom")
     *     suspend fun getCustom(): ApiResponse<MyData>
     * }
     *
     * val myApi = NetworkModule.createApiService<MyApi>(baseUrl)
     * ```
     */
    inline fun <reified T> createApiService(
        baseUrl: String,
        noinline getToken: suspend () -> String? = { null },
        noinline clearToken: suspend () -> Unit = {}
    ): T {
        return createRetrofit(baseUrl, getToken, clearToken).create(T::class.java)
    }

    /**
     * 缓存新的 Retrofit 实例
     */
    fun cacheRetrofit(retrofit: Retrofit) {
        this.retrofit = retrofit
    }

    /**
     * 获取缓存的 Retrofit 实例（可能为 null）
     */
    fun getCachedRetrofit(): Retrofit? = retrofit
}
