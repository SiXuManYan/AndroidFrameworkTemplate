package com.template.framework.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Parent entity in the sample one-to-many line relationship.
 *
 * @property id stable primary key referenced by [LinePosition.lineId]
 * @property lineCode business-facing line code
 * @property lineName display name
 */
@Entity(tableName = "line")
data class Line(
    @PrimaryKey
    val id: String,
    val lineCode: String,
    val lineName: String
)
