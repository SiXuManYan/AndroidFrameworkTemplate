package com.template.framework.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Sample record of a product-processing event.
 *
 * @property id auto-generated Room primary key; keep `0` for new records
 * @property productCode product identifier
 * @property productName product display name
 * @property startTime event start time in epoch milliseconds
 */
@Entity(tableName = "product_history")
data class ProductHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productCode: String,
    val productName: String,
    val startTime: Long
)
