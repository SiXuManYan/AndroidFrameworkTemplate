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
 * 框架默认 API 服务
 *
 * 仅保留示例接口，覆盖 Retrofit 常用注解：
 * - `@POST` + `@Body`：登录、复杂请求
 * - `@GET` + `@Query`：带参数的 GET
 * - `ApiResponse<T>` / `ApiResponse<List<T>>` / `ApiResponse<Unit>`
 *
 * 业务可自行扩展：
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
 * @author Shiwei Wang
 * @date 2026-02
 */
interface ApiService {

    /**
     * 登录示例
     *
     * - `@POST` + `@Body`：复杂对象请求
     * - `ApiResponse<LoginDataResponse>`：业务响应
     */
    @POST("auth/login/device")
    suspend fun deviceLogin(@Body request: DeviceLoginRequest): ApiResponse<LoginDataResponse>

    /**
     * 列表数据示例（GET 列表）
     *
     * - `@GET` + 无参
     * - `ApiResponse<List<LineAndPostResponse>>`：列表响应
     */
    @GET("production/lines/getLineAndPost")
    suspend fun getLineAndPost(): ApiResponse<List<LineAndPostResponse>>

    /**
     * 保存数据示例（POST 复杂对象）
     *
     * - `@POST` + `@Body`
     * - `ApiResponse<SaveProductsResponse>`：返回业务数据
     */
    @POST("production/product/saveProducts")
    suspend fun saveProducts(@Body request: SaveProductsRequest): ApiResponse<SaveProductsResponse>

    /**
     * 无返回数据示例（POST + ApiResponse<Unit>）
     *
     * - 仅关心成功/失败，不关心返回内容
     */
    @POST("demo/action")
    suspend fun doAction(@Query("id") id: String): ApiResponse<Unit>
}