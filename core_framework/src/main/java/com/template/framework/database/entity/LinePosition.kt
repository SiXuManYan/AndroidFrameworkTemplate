package com.template.framework.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一对多子实体示例
 *
 * 演示：
 * - `@ForeignKey` + `onDelete = CASCADE`：父记录删除时自动删除子记录
 * - `@Index`：提升外键查询性能
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
@Entity(
    tableName = "line_position",
    foreignKeys = [
        ForeignKey(
            entity = Line::class,
            parentColumns = ["id"],
            childColumns = ["lineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["lineId"])]
)
data class LinePosition(
    @PrimaryKey
    val id: Int,
    val lineId: String,
    val postId: String,
    val postCode: String,
    val postName: String,
    val enName: String? = null,
    val startTime: String? = null,
    val status: String? = null,
    val remark: String? = null,
    val delFlag: String? = null,
    val sortOrder: Int = 0
)