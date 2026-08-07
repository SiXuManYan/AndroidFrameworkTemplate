package com.template.framework.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Child entity belonging to a sample [Line].
 *
 * Deleting the parent line cascades to its positions, and [lineId] is indexed for lookup.
 * - 中文：产线删除时会级联删除工位记录。
 *
 * @property id position primary key
 * @property lineId parent [Line.id]
 * @property postId backend post identifier
 * @property postCode post code
 * @property postName display name
 * @property enName optional English name
 * @property startTime optional backend-formatted start time
 * @property status optional status value
 * @property remark optional remark
 * @property delFlag optional soft-delete flag
 * @property sortOrder display order within the parent line
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
