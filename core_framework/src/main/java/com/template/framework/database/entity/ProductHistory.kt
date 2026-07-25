package com.template.framework.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 列表项实体示例 1
 *
 * 演示：
 * - `@Entity(tableName = ...)` 定义表名
 * - `@PrimaryKey(autoGenerate = true)` 自增主键
 * - 基本字段定义
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
@Entity(tableName = "product_history")
data class ProductHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productCode: String,
    val productName: String,
    val startTime: Long
)