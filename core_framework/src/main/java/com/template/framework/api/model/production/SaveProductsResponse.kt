package com.template.framework.api.model.production

import com.google.gson.annotations.SerializedName

/**
 * Product summary returned by the sample save endpoint.
 *
 * @property productCode saved product code
 * @property productName saved product display name
 */
data class SaveProductsResponse(
    @SerializedName("productCode")
    val productCode: String,
    @SerializedName("productName")
    val productName: String
)
