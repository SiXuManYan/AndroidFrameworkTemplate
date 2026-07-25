package com.template.framework.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 一对多父实体示例
 *
 * 与 [LinePosition] 演示 `@ForeignKey` + `@Index` 的 Room 一对多关联。
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
@Entity(tableName = "line")
data class Line(
    @PrimaryKey
    val id: String,
    val lineCode: String,
    val lineName: String
)