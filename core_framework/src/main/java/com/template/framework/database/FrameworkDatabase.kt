package com.template.framework.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.template.framework.Framework
import com.template.framework.constants.FrameworkConstants
import com.template.framework.database.dao.LineDao
import com.template.framework.database.dao.ProductHistoryDao
import com.template.framework.database.entity.Line
import com.template.framework.database.entity.LinePosition
import com.template.framework.database.entity.ProductHistory

/**
 * 框架数据库
 *
 * 业务可在自己的 AppDatabase 中继承此类，或直接基于此扩展：
 * ```kotlin
 * @Database(
 *     entities = [...],
 *     version = 2,
 *     exportSchema = false
 * )
 * abstract class AppDatabase : FrameworkDatabase() {
 *     abstract fun myDao(): MyDao
 * }
 * ```
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
@Database(
    entities = [ProductHistory::class, Line::class, LinePosition::class],
    version = 1,
    exportSchema = false
)
abstract class FrameworkDatabase : RoomDatabase() {

    abstract fun productHistoryDao(): ProductHistoryDao
    abstract fun lineDao(): LineDao

    companion object {
        @Volatile
        private var INSTANCE: FrameworkDatabase? = null

        /**
         * 获取单例实例
         *
         * @param context 上下文
         */
        fun getDatabase(context: Context): FrameworkDatabase {
            return INSTANCE ?: synchronized(this) {
                val dbName = runCatching {
                    Framework.getConfig().let { "${it.dataStoreName.replace("preferences", "db")}" }
                }.getOrNull() ?: FrameworkConstants.DATABASE_NAME

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FrameworkDatabase::class.java,
                    dbName
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}