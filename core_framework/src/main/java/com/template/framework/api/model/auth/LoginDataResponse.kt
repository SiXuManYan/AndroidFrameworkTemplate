package com.template.framework.api.model.auth

import com.google.gson.annotations.SerializedName

/**
 * 登录响应数据
 *
 * @author Shiwei Wang
 * @date 2026-02
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