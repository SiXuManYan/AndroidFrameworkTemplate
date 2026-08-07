package com.template.framework.api.model.auth

import com.google.gson.annotations.SerializedName

/**
 * Token and user metadata returned by the sample login endpoint.
 *
 * - 中文：示例登录响应数据，包含访问令牌、刷新令牌及可选用户信息。
 *
 * @property accessToken bearer token used for authenticated requests
 * @property refreshToken optional token used by a business-defined refresh flow
 * @property expireIn access-token lifetime reported by the backend
 * @property refreshExpireIn optional refresh-token lifetime
 * @property clientId optional backend client identifier
 * @property scope optional authorization scope
 * @property openid optional OpenID-style identifier
 * @property nickName optional display name
 * @property userId optional user identifier
 * @property msg optional backend message
 */
data class LoginDataResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String? = null,
    @SerializedName("expire_in")
    val expireIn: Int = 0,
    @SerializedName("refresh_expire_in")
    val refreshExpireIn: Int? = null,
    @SerializedName("client_id")
    val clientId: String? = null,
    val scope: String? = null,
    val openid: String? = null,
    @SerializedName("nickname")
    val nickName: String? = null,
    @SerializedName("userId")
    val userId: String? = null,
    val msg: String? = null
)
