package com.template.framework.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.template.framework.database.entity.Line
import com.template.framework.database.entity.LinePosition
import kotlinx.coroutines.flow.Flow

/**
 * Room access for sample production lines and their child positions.
 *
 * - 中文：演示一对多表的响应式查询和批量写入。
 */
@Dao
interface LineDao {

    /** Observes all lines ordered by [Line.lineCode]. */
    @Query("SELECT * FROM line ORDER BY lineCode")
    fun getAllLines(): Flow<List<Line>>

    /** Returns positions for [lineId] in display order. */
    @Query("SELECT * FROM line_position WHERE lineId = :lineId ORDER BY sortOrder")
    suspend fun getLinePositionsByLineId(lineId: String): List<LinePosition>

    /** Returns the line identified by [lineId], or `null` when it does not exist. */
    @Query("SELECT * FROM line WHERE id = :lineId LIMIT 1")
    suspend fun getLineById(lineId: String): Line?

    /** Returns the first position matching [postId], or `null` when absent. */
    @Query("SELECT * FROM line_position WHERE postId = :postId LIMIT 1")
    suspend fun getLinePositionByPostId(postId: String): LinePosition?

    /** Inserts or replaces all [lines] by primary key. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLines(lines: List<Line>)

    /** Inserts or replaces all [positions] by primary key. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLinePositions(positions: List<LinePosition>)

    /** Deletes every line; child positions are removed through the foreign-key cascade. */
    @Query("DELETE FROM line")
    suspend fun deleteAllLines()

    /** Deletes every line-position row. */
    @Query("DELETE FROM line_position")
    suspend fun deleteAllLinePositions()
}
