package com.template.framework.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.template.framework.database.entity.ProductHistory
import kotlinx.coroutines.flow.Flow

/**
 * 示例 DAO：基本 CRUD + Flow 自动刷新
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
@Dao
interface ProductHistoryDao {

    @Query("SELECT * FROM product_history ORDER BY startTime DESC")
    fun getAllHistory(): Flow<List<ProductHistory>>

    @Query("SELECT * FROM product_history ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int): List<ProductHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ProductHistory): Long

    @Delete
    suspend fun deleteHistory(history: ProductHistory)

    @Query("DELETE FROM product_history")
    suspend fun deleteAllHistory()

    @Query("SELECT * FROM product_history WHERE productCode = :productCode ORDER BY startTime DESC")
    suspend fun getHistoryByProductCode(productCode: String): List<ProductHistory>
}