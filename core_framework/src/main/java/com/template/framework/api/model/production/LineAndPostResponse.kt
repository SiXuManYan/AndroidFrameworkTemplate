package com.template.framework.api.model.production

import com.google.gson.annotations.SerializedName

/**
 * 列表数据响应示例
 *
 * 用于演示 `@GET` 返回 `ApiResponse<List<T>>` 的用法。
 * 实际业务中可重命名为符合业务的实体，如 ProductResponse / OrderResponse 等。
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
data class LineAndPostResponse(
    val id: String,
    @SerializedName("tenantId")
    val tenantId: String? = null,
    @SerializedName("lineCode")
    val lineCode: String,
    @SerializedName("lineName")
    val lineName: String,
    @SerializedName("linePositions")
    val linePositions: List<LinePosition> = emptyList()
)

/**
 * 列表元素的子项（演示一对多嵌套）
 */
data class LinePosition(
    val id: Int,
    @SerializedName("lineId")
    val lineId: String,
    @SerializedName("postId")
    val postId: String,
    @SerializedName("postCode")
    val postCode: String,
    @SerializedName("postName")
    val postName: String,
    @SerializedName("enName")
    val enName: String? = null,
    @SerializedName("startTime")
    val startTime: String? = null,
    val status: String? = null,
    val remark: String? = null,
    @SerializedName("delFlag")
    val delFlag: String? = null,
    @SerializedName("sortOrder")
    val sortOrder: Int = 0
)