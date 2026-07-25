package com.template.framework.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.template.framework.database.entity.Line
import com.template.framework.database.entity.LinePosition
import kotlinx.coroutines.flow.Flow

/**
 * 示例 DAO：一对多关系 + Flow 响应式查询
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
@Dao
interface LineDao {

    @Query("SELECT * FROM line ORDER BY lineCode")
    fun getAllLines(): Flow<List<Line>>

    @Query("SELECT * FROM line_position WHERE lineId = :lineId ORDER BY sortOrder")
    suspend fun getLinePositionsByLineId(lineId: String): List<LinePosition>

    @Query("SELECT * FROM line WHERE id = :lineId LIMIT 1")
    suspend fun getLineById(lineId: String): Line?

    @Query("SELECT * FROM line_position WHERE postId = :postId LIMIT 1")
    suspend fun getLinePositionByPostId(postId: String): LinePosition?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLines(lines: List<Line>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLinePositions(positions: List<LinePosition>)

    @Query("DELETE FROM line")
    suspend fun deleteAllLines()

    @Query("DELETE FROM line_position")
    suspend fun deleteAllLinePositions()
}