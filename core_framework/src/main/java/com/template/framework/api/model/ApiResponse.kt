package com.template.framework.api.model

import com.google.gson.annotations.SerializedName

/**
 * API 统一响应封装
 *
 * 后端约定的标准格式：
 * ```json
 * {
 *   "code": 200,
 *   "msg": "成功",
 *   "data": { ... } | [ ... ]
 * }
 * ```
 *
 * 使用示例：
 * ```kotlin
 * @GET("user/info")
 * suspend fun getUserInfo(): ApiResponse<UserInfo>
 * ```
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
data class ApiResponse<T>(
    val code: Int,
    @SerializedName("msg")
    val message: String,
    val data: T?
) {
    /**
     * 是否成功（code == 200）
     */
    val isSuccess: Boolean
        get() = code == SUCCESS_CODE

    companion object {
        /** 业务成功 code */
        const val SUCCESS_CODE = 200

        /** Token 失效 code（业务层 401） */
        const val TOKEN_EXPIRED_CODE = 401
    }
}