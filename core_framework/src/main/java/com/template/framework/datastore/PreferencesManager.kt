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
 * Framework-owned DataStore whose file name is fixed at compile time.
 *
 * Business preferences should use a separate DataStore in the App module.
 */
private val Context.frameworkDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "framework_preferences"
)

/**
 * Manages framework connection, authentication, and language preferences.
 *
 * Values are mirrored by [DataStoreBackupHelper]. Prefer the exposed [Flow] properties and suspend
 * setters. The `*Sync` methods use `runBlocking` and should be limited to startup code that cannot
 * collect asynchronously.
 *
 * - 中文：管理服务器、Token 和语言配置；优先使用 Flow 与挂起函数，同步读取会阻塞线程。
 *
 * @param context any context; only its application context is retained
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

    /** Observes the server IP; emits an empty string when unset. */
    val serverIp: Flow<String> = backup.getStringFlow(SERVER_IP, "")
        .map { it ?: "" }
        .distinctUntilChanged()

    /** Returns the server IP while blocking the caller until the first value is available. */
    fun getServerIpSync(): String = runBlocking { backup.getString(SERVER_IP, "") ?: "" }

    /** Persists [ip] to DataStore and its backup. */
    suspend fun saveServerIp(ip: String) = backup.putString(SERVER_IP, ip)

    // endregion

    // region [Server Port]

    /** Observes the server port; emits an empty string when unset. */
    val serverPort: Flow<String> = backup.getStringFlow(SERVER_PORT, "")
        .map { it ?: "" }
        .distinctUntilChanged()

    /** Returns the server port while blocking the caller. */
    fun getServerPortSync(): String = runBlocking { backup.getString(SERVER_PORT, "") ?: "" }

    /** Persists [port] to DataStore and its backup. */
    suspend fun saveServerPort(port: String) = backup.putString(SERVER_PORT, port)

    // endregion

    // region [Access Token]

    /** Observes the access token; emits `null` when no authenticated session exists. */
    val accessToken: Flow<String?> = backup.getStringFlow(ACCESS_TOKEN).distinctUntilChanged()

    /** Returns the access token while blocking the caller. */
    fun getAccessTokenSync(): String? = runBlocking { backup.getString(ACCESS_TOKEN) }

    /** Persists the latest [token]. */
    suspend fun saveAccessToken(token: String) = backup.putString(ACCESS_TOKEN, token)

    /** Removes the access token from both stores. */
    suspend fun clearAccessToken() = backup.remove(ACCESS_TOKEN)

    // endregion

    // region [Language]

    /** Observes the selected language code; emits an empty string when unset. */
    val language: Flow<String> = backup.getStringFlow(LANGUAGE, "")
        .map { it ?: "" }
        .distinctUntilChanged()

    /** Returns the selected language code while blocking the caller. */
    fun getLanguageSync(): String = runBlocking { backup.getString(LANGUAGE, "") ?: "" }

    /** Persists a language code such as `zh` or `en`. */
    suspend fun saveLanguage(lang: String) = backup.putString(LANGUAGE, lang)

    // endregion

    /** Clears every framework-owned preference without touching App-owned DataStores. */
    suspend fun clearAll() {
        backup.remove(SERVER_IP)
        backup.remove(SERVER_PORT)
        backup.remove(ACCESS_TOKEN)
        backup.remove(LANGUAGE)
    }
}
