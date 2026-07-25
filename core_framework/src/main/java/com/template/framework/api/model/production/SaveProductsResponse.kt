package com.template.framework.api.model.production

import com.google.gson.annotations.SerializedName

/**
 * 简单 POST 响应示例
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
data class SaveProductsResponse(
    @SerializedName("productCode")
    val productCode: String,
    @SerializedName("productName")
    val productName: String
)