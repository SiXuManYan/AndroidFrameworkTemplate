package com.example.template.data.api

import com.template.framework.api.model.ApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** 通用示例契约，没有配套真实后端；接入时替换为自己的 API 和模型。 */
interface ExampleApi {
    @GET("example/items")
    suspend fun getItems(@Query("page") page: Int = 1): ApiResponse<List<ExampleItem>>

    @POST("example/items")
    suspend fun saveItem(@Body item: ExampleItem): ApiResponse<ExampleItem>
}

data class ExampleItem(val id: Long, val title: String)
