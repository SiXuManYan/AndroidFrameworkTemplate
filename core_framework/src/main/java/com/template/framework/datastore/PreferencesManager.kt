package com.template.framework.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.template.framework.constants.FrameworkConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * 框架默认的 DataStore 实例
 *
 * 注：DataStore 名称在编译期固定为 "framework_preferences"。
 * 业务需要不同名称时，可在自己的模块中创建独立的 Manager，
 * 通过相同的方式使用 `preferencesDataStore` 委托。
 */
private val Context.frameworkDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "framework_preferences"
)

/**
 * Preferences 管理器
 *
 * 框架默认提供 4 个通用 Key：
 * - [serverIp]   - 服务器 IP
 * - [serverPort] - 服务器端口
 * - [accessToken] - 访问令牌
 * - [language]   - 语言
 *
 * 业务可在自己的 Manager 中继承或包装此类，添加自定义 Key。
 *
 * ## DataStore + SharedPreferences 双写双读
 * 通过 [DataStoreBackupHelper] 实现，防止 DataStore 异常导致配置丢失。
 *
 * ## 同步访问
 * 部分场景（如 Activity.onCreate）需要同步获取值，
 * 可调用 [getServerIpSync] / [getServerPortSync] / [getAccessTokenSync] /
 * [getLanguageSync]（内部使用 runBlocking，谨慎使用）。
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
open class PreferencesManager(context: Context) {

    private val appContext = context.applicationContext

    private val dataStore: DataStore<Preferences> = appContext.frameworkDataStore

    private val backup = DataStoreBackupHelper(appContext, dataStore)

    private companion object {
        val SERVER_IP = stringPreferencesKey(FrameworkConstants.PREF_SERVER_IP)
        val SERVER_PORT = stringPreferencesKey(FrameworkConstants.PREF_SERVER_PORT)
        val ACCESS_TOKEN = stringPreferencesKey(FrameworkConstants.PREF_ACCESS_TOKEN)
        val LANGUAGE = stringPreferencesKey(FrameworkConstants.PREF_LANGUAGE)
    }

    // region [Server IP]

    /** 服务器 IP（Flow，默认为空字符串） */
    val serverIp: Flow<String> = backup.getStringFlow(SERVER_IP, "")
        .map { it ?: "" }
        .distinctUntilChanged()

    /**
     * 同步读取服务器 IP
     * 内部使用 runBlocking，应仅在确实需要同步访问时使用（如 Activity.onCreate）
     */
    fun getServerIpSync(): String = runBlocking { backup.getString(SERVER_IP, "") ?: "" }

    /** 保存服务器 IP */
    suspend fun saveServerIp(ip: String) = backup.putString(SERVER_IP, ip)

    // endregion

    // region [Server Port]

    val serverPort: Flow<String> = backup.getStringFlow(SERVER_PORT, "")
        .map { it ?: "" }
        .distinctUntilChanged()

    fun getServerPortSync(): String = runBlocking { backup.getString(SERVER_PORT, "") ?: "" }

    suspend fun saveServerPort(port: String) = backup.putString(SERVER_PORT, port)

    // endregion

    // region [Access Token]

    val accessToken: Flow<String?> = backup.getStringFlow(ACCESS_TOKEN).distinctUntilChanged()

    fun getAccessTokenSync(): String? = runBlocking { backup.getString(ACCESS_TOKEN) }

    suspend fun saveAccessToken(token: String) = backup.putString(ACCESS_TOKEN, token)

    suspend fun clearAccessToken() = backup.remove(ACCESS_TOKEN)

    // endregion

    // region [Language]

    val language: Flow<String> = backup.getStringFlow(LANGUAGE, "")
        .map { it ?: "" }
        .distinctUntilChanged()

    fun getLanguageSync(): String = runBlocking { backup.getString(LANGUAGE, "") ?: "" }

    suspend fun saveLanguage(lang: String) = backup.putString(LANGUAGE, lang)

    // endregion

    /**
     * 清除所有框架 Key
     */
    suspend fun clearAll() {
        backup.remove(SERVER_IP)
        backup.remove(SERVER_PORT)
        backup.remove(ACCESS_TOKEN)
        backup.remove(LANGUAGE)
    }
}