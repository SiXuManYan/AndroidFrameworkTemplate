package com.template.framework.api.model.production

import com.google.gson.annotations.SerializedName

/**
 * 复杂 POST 请求示例
 *
 * 演示 `@POST` + `@Body` 的复杂对象请求。
 * 实际业务中可重命名为符合业务的实体。
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
data class SaveProductsRequest(
    @SerializedName("snNumber")
    val snNumber: String,
    @SerializedName("lineId")
    val lineId: String? = null,
    @SerializedName("postId")
    val postId: String? = null,
    @SerializedName("productCode")
    val productCode: String? = null,
    @SerializedName("type")
    val type: Int = 0
)