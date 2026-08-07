package com.template.framework.api.model.production

import com.google.gson.annotations.SerializedName

/**
 * Request body used by the sample product-save endpoint.
 *
 * - 中文：用于演示 `@POST` + `@Body` 的复合请求模型。
 *
 * @property snNumber device serial number
 * @property lineId optional production line identifier
 * @property postId optional workstation identifier
 * @property productCode optional product code
 * @property type sample operation type defined by the backend
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
