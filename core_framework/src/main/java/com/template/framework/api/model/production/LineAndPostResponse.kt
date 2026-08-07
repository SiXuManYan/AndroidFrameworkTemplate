package com.template.framework.api.model.production

import com.google.gson.annotations.SerializedName

/**
 * Production line returned by the sample list endpoint.
 *
 * - 中文：用于演示 `ApiResponse<List<T>>` 以及嵌套工位列表的响应模型。
 *
 * @property id line identifier
 * @property tenantId optional tenant identifier
 * @property lineCode line code
 * @property lineName display name
 * @property linePositions positions belonging to this line
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
 * Position nested under a [LineAndPostResponse].
 *
 * @property id position record identifier
 * @property lineId parent line identifier
 * @property postId backend post identifier
 * @property postCode post code
 * @property postName display name
 * @property enName optional English display name
 * @property startTime optional backend-formatted start time
 * @property status optional status value
 * @property remark optional free-form remark
 * @property delFlag optional soft-delete flag
 * @property sortOrder display order within the line
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
