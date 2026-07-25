package com.template.framework.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * DataStore 备份工具类
 *
 * 实现 DataStore + SharedPreferences 双写双读模式：
 * - 保存：同时写入 DataStore 和 SharedPreferences
 * - 读取：优先从 DataStore 读取，如果为空则从 SharedPreferences 恢复
 * - 删除：同时从 DataStore 和 SharedPreferences 删除
 *
 * 适用场景：
 * - 防止 DataStore 异常（如升级失败、数据损坏）导致配置丢失
 * - 兼顾新用户（DataStore）与老用户（SharedPreferences）的迁移
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
class DataStoreBackupHelper(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
    backupPrefsName: String = "framework_preferences_backup"
) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(backupPrefsName, Context.MODE_PRIVATE)

    private fun isBlankValue(value: String?): Boolean = value.isNullOrBlank()

    // region [保存 - String]

    suspend fun putString(key: Preferences.Key<String>, value: String, useCommit: Boolean = true) {
        try {
            dataStore.edit { it[key] = value }
            val editor = sharedPreferences.edit().putString(key.name, value)
            if (useCommit) editor.commit() else editor.apply()
            Timber.tag(TAG).i("Saved ${key.name}")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to save ${key.name}")
            throw e
        }
    }

    // endregion

    // region [保存 - Int]

    suspend fun putInt(key: Preferences.Key<Int>, value: Int, useCommit: Boolean = true) {
        try {
            dataStore.edit { it[key] = value }
            val editor = sharedPreferences.edit().putInt(key.name, value)
            if (useCommit) editor.commit() else editor.apply()
            Timber.tag(TAG).i("Saved ${key.name}")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to save ${key.name}")
            throw e
        }
    }

    // endregion

    // region [保存 - Boolean]

    suspend fun putBoolean(key: Preferences.Key<Boolean>, value: Boolean, useCommit: Boolean = true) {
        try {
            dataStore.edit { it[key] = value }
            val editor = sharedPreferences.edit().putBoolean(key.name, value)
            if (useCommit) editor.commit() else editor.apply()
            Timber.tag(TAG).i("Saved ${key.name}")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to save ${key.name}")
            throw e
        }
    }

    // endregion

    // region [读取 - String（异步）]

    suspend fun getString(key: Preferences.Key<String>, defaultValue: String? = null): String? {
        return try {
            val dsValue = dataStore.data.first()[key]
            if (!isBlankValue(dsValue)) return dsValue
            val backup = sharedPreferences.getString(key.name, defaultValue)
            if (!isBlankValue(backup)) {
                CoroutineScope(Dispatchers.IO).launch {
                    runCatching { putString(key, backup!!, useCommit = true) }
                }
                backup
            } else defaultValue
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to get ${key.name}")
            sharedPreferences.getString(key.name, defaultValue)
        }
    }

    // endregion

    // region [读取 - Int（异步）]

    suspend fun getInt(key: Preferences.Key<Int>, defaultValue: Int? = null): Int? {
        return try {
            val dsValue = dataStore.data.first()[key]
            if (dsValue != null) return dsValue
            val backup = if (sharedPreferences.contains(key.name)) {
                sharedPreferences.getInt(key.name, defaultValue ?: 0)
            } else defaultValue
            if (backup != null && (defaultValue == null || backup != defaultValue)) {
                CoroutineScope(Dispatchers.IO).launch {
                    runCatching { putInt(key, backup, useCommit = true) }
                }
            }
            backup
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to get ${key.name}")
            if (sharedPreferences.contains(key.name)) sharedPreferences.getInt(key.name, defaultValue ?: 0)
            else defaultValue
        }
    }

    // endregion

    // region [读取 - Boolean（异步）]

    suspend fun getBoolean(key: Preferences.Key<Boolean>, defaultValue: Boolean? = null): Boolean? {
        return try {
            val dsValue = dataStore.data.first()[key]
            if (dsValue != null) return dsValue
            val backup = if (sharedPreferences.contains(key.name)) {
                sharedPreferences.getBoolean(key.name, defaultValue ?: false)
            } else defaultValue
            if (backup != null && (defaultValue == null || backup != defaultValue)) {
                CoroutineScope(Dispatchers.IO).launch {
                    runCatching { putBoolean(key, backup, useCommit = true) }
                }
            }
            backup
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to get ${key.name}")
            if (sharedPreferences.contains(key.name)) sharedPreferences.getBoolean(key.name, defaultValue ?: false)
            else defaultValue
        }
    }

    // endregion

    // region [删除]

    suspend fun remove(key: Preferences.Key<*>) {
        try {
            dataStore.edit { it.remove(key) }
            sharedPreferences.edit().remove(key.name).commit()
            Timber.tag(TAG).d("Removed ${key.name}")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to remove ${key.name}")
            throw e
        }
    }

    // endregion

    // region [Flow - String]

    fun getStringFlow(key: Preferences.Key<String>, defaultValue: String? = null): Flow<String?> {
        return dataStore.data.map { prefs ->
            val v = prefs[key]
            if (!isBlankValue(v)) v
            else {
                val backup = sharedPreferences.getString(key.name, defaultValue)
                if (!isBlankValue(backup)) {
                    CoroutineScope(Dispatchers.IO).launch {
                        runCatching { putString(key, backup!!, useCommit = true) }
                    }
                    backup
                } else defaultValue
            }
        }.distinctUntilChanged()
    }

    // endregion

    // region [Flow - Int]

    fun getIntFlow(key: Preferences.Key<Int>, defaultValue: Int? = null): Flow<Int?> {
        return dataStore.data.map { prefs ->
            val v = prefs[key]
            if (v != null) v
            else {
                val backup = if (sharedPreferences.contains(key.name)) {
                    sharedPreferences.getInt(key.name, defaultValue ?: 0)
                } else defaultValue
                if (backup != null && (defaultValue == null || backup != defaultValue)) {
                    CoroutineScope(Dispatchers.IO).launch {
                        runCatching { putInt(key, backup, useCommit = true) }
                    }
                }
                backup
            }
        }.distinctUntilChanged()
    }

    // endregion

    // region [Flow - Boolean]

    fun getBooleanFlow(key: Preferences.Key<Boolean>, defaultValue: Boolean? = null): Flow<Boolean?> {
        return dataStore.data.map { prefs ->
            val v = prefs[key]
            if (v != null) v
            else {
                val backup = if (sharedPreferences.contains(key.name)) {
                    sharedPreferences.getBoolean(key.name, defaultValue ?: false)
                } else defaultValue
                if (backup != null && (defaultValue == null || backup != defaultValue)) {
                    CoroutineScope(Dispatchers.IO).launch {
                        runCatching { putBoolean(key, backup, useCommit = true) }
                    }
                }
                backup
            }
        }.distinctUntilChanged()
    }

    // endregion

    private companion object {
        const val TAG = "DataStoreBackup"
    }
}