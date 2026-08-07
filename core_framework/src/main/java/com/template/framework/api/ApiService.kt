package com.template.framework.api

import com.template.framework.api.model.ApiResponse
import com.template.framework.api.model.auth.DeviceLoginRequest
import com.template.framework.api.model.auth.LoginDataResponse
import com.template.framework.api.model.production.LineAndPostResponse
import com.template.framework.api.model.production.SaveProductsRequest
import com.template.framework.api.model.production.SaveProductsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Example Retrofit contract bundled with the framework.
 *
 * These endpoints demonstrate common request and response shapes. Real service contracts should
 * live in the App or feature module instead of being added to this generic interface.
 * - 中文：这里的接口用于演示 Retrofit 写法，真实业务接口建议放在业务模块。
 *
 * ## Custom API example
 * ```kotlin
 * interface MyApi {
 *     @GET("user/info")
 *     suspend fun getUserInfo(): ApiResponse<UserInfo>
 *
 *     @PUT("user/{id}")
 *     suspend fun updateUser(@Path("id") id: String, @Body user: User): ApiResponse<Unit>
 * }
 *
 * val myApi = NetworkModule.createApiService<MyApi>(baseUrl)
 * ```
 *
 */
interface ApiService {

    /**
     * Sends the example device-login request.
     *
     * @param request login payload defined by the sample backend contract
     * @return the backend envelope containing login tokens and user metadata
     */
    @POST("auth/login/device")
    suspend fun deviceLogin(@Body request: DeviceLoginRequest): ApiResponse<LoginDataResponse>

    /** Returns the example production lines and their positions. */
    @GET("production/lines/getLineAndPost")
    suspend fun getLineAndPost(): ApiResponse<List<LineAndPostResponse>>

    /**
     * Saves the example production payload.
     *
     * @param request product and workstation data expected by the sample backend
     * @return the saved product summary wrapped in [ApiResponse]
     */
    @POST("production/product/saveProducts")
    suspend fun saveProducts(@Body request: SaveProductsRequest): ApiResponse<SaveProductsResponse>

    /**
     * Demonstrates an operation whose response has no useful data payload.
     *
     * @param id identifier consumed by the sample action
     */
    @POST("demo/action")
    suspend fun doAction(@Query("id") id: String): ApiResponse<Unit>
}
