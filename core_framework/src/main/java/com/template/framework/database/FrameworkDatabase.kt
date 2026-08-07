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
 * Sample Room database containing production-line and product-history tables.
 *
 * [getDatabase] uses destructive fallback migration, which is convenient for a template but can
 * delete user data. Production apps should define their own database and explicit migrations.
 * - 中文：示例数据库升级时可能清表，正式项目应建立独立数据库并编写迁移。
 */
@Database(
    entities = [ProductHistory::class, Line::class, LinePosition::class],
    version = 1,
    exportSchema = false
)
abstract class FrameworkDatabase : RoomDatabase() {

    /** Returns the DAO for product-history records. */
    abstract fun productHistoryDao(): ProductHistoryDao

    /** Returns the DAO for lines and their positions. */
    abstract fun lineDao(): LineDao

    companion object {
        @Volatile
        private var INSTANCE: FrameworkDatabase? = null

        /**
         * Returns the lazily created process-wide sample database.
         *
         * @param context any context; only its application context is retained
         * @return the shared [FrameworkDatabase] instance
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
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
