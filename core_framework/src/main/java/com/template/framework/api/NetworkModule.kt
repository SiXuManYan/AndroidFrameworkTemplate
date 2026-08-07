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
 * Factory for framework-configured [OkHttpClient], [Retrofit], and API services.
 *
 * ## TLS policy
 * - With no custom certificate, OkHttp uses the platform trust store and hostname verifier.
 * - [FrameworkConfig.sslCertRawResId] replaces the trust store with the supplied certificate.
 *
 * ## Authentication
 * The client injects version and token headers and forwards `401` responses to
 * [com.template.framework.Framework.setOnTokenExpired].
 *
 * - 中文：集中创建网络客户端，并统一处理 Header、日志、401 与证书配置。
 */
object NetworkModule {

    private var retrofit: Retrofit? = null

    /**
     * Callback invoked by [AuthErrorInterceptor] after an expired token is cleared.
     *
     * This callback may run on an OkHttp worker thread.
     */
    var onTokenExpired: (() -> Unit)? = null

    /**
     * Applies the optional certificate from [FrameworkConfig.sslCertRawResId] to [builder].
     *
     * If no certificate is configured, the builder remains on OkHttp's platform defaults.
     *
     * @param builder client builder to configure
     * @throws IllegalStateException when the configured certificate cannot be parsed or installed
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
     * Creates an OkHttp client with framework timeouts and interceptors.
     *
     * @param getToken suspending provider used to read the latest access token for each request
     * @param clearToken suspending action run when [AuthErrorInterceptor] detects an expired token
     * @return a new client; this method does not cache the instance
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
     * Creates a Retrofit instance backed by [createOkHttpClient].
     *
     * A missing trailing slash is added automatically.
     *
     * @param baseUrl absolute HTTP(S) base URL
     * @param getToken provider for the latest access token
     * @param clearToken action used to clear an expired access token
     * @return a new Retrofit instance using Gson conversion
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
     * Creates the framework's example [ApiService].
     *
     * @param baseUrl absolute HTTP(S) base URL
     * @param getToken provider for the latest access token
     * @param clearToken action used to clear an expired access token
     */
    fun createApiService(
        baseUrl: String,
        getToken: suspend () -> String? = { null },
        clearToken: suspend () -> Unit = {}
    ): ApiService {
        return createRetrofit(baseUrl, getToken, clearToken).create(ApiService::class.java)
    }

    /**
     * Creates a caller-defined Retrofit service interface.
     *
     * ## Example
     * ```kotlin
     * interface MyApi {
     *     @GET("custom")
     *     suspend fun getCustom(): ApiResponse<MyData>
     * }
     *
     * val myApi = NetworkModule.createApiService<MyApi>(baseUrl)
     * ```
     *
     * @param T Retrofit service interface type
     * @param baseUrl absolute HTTP(S) base URL
     * @param getToken provider for the latest access token
     * @param clearToken action used to clear an expired access token
     */
    inline fun <reified T> createApiService(
        baseUrl: String,
        noinline getToken: suspend () -> String? = { null },
        noinline clearToken: suspend () -> Unit = {}
    ): T {
        return createRetrofit(baseUrl, getToken, clearToken).create(T::class.java)
    }

    /** Stores [retrofit] in the optional module-level cache. */
    fun cacheRetrofit(retrofit: Retrofit) {
        this.retrofit = retrofit
    }

    /** Returns the instance last supplied to [cacheRetrofit], or `null` when none was cached. */
    fun getCachedRetrofit(): Retrofit? = retrofit
}
