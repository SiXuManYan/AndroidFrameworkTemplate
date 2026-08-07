package com.template.framework.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.template.framework.database.entity.ProductHistory
import kotlinx.coroutines.flow.Flow

/**
 * Room access for sample product-history records.
 *
 * - 中文：演示基础 CRUD 与随表变化自动更新的 `Flow` 查询。
 */
@Dao
interface ProductHistoryDao {

    /** Observes the complete history, newest first. */
    @Query("SELECT * FROM product_history ORDER BY startTime DESC")
    fun getAllHistory(): Flow<List<ProductHistory>>

    /** Returns at most [limit] newest records. */
    @Query("SELECT * FROM product_history ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int): List<ProductHistory>

    /** Inserts or replaces [history] and returns its row ID. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ProductHistory): Long

    /** Deletes the row matching [history]'s primary key. */
    @Delete
    suspend fun deleteHistory(history: ProductHistory)

    /** Deletes all product-history rows. */
    @Query("DELETE FROM product_history")
    suspend fun deleteAllHistory()

    /** Returns records matching [productCode], newest first. */
    @Query("SELECT * FROM product_history WHERE productCode = :productCode ORDER BY startTime DESC")
    suspend fun getHistoryByProductCode(productCode: String): List<ProductHistory>
}
