package com.template.framework.util

import java.math.BigDecimal
import java.math.RoundingMode

/** Decimal arithmetic with an explicit, consistent rounding policy. */
object DecimalUtils {

    const val DEFAULT_SCALE = 2

    val DEFAULT_ROUNDING_MODE: RoundingMode = RoundingMode.HALF_UP

    /** Creates a decimal without inheriting the binary representation error of [Double]. */
    @JvmStatic
    @JvmOverloads
    fun fromDouble(
        value: Double,
        scale: Int = DEFAULT_SCALE,
        roundingMode: RoundingMode = DEFAULT_ROUNDING_MODE
    ): BigDecimal = round(BigDecimal.valueOf(value), scale, roundingMode)

    @JvmStatic
    @JvmOverloads
    fun round(
        value: BigDecimal,
        scale: Int = DEFAULT_SCALE,
        roundingMode: RoundingMode = DEFAULT_ROUNDING_MODE
    ): BigDecimal = value.setScale(scale, roundingMode)

    @JvmStatic
    @JvmOverloads
    fun add(
        left: BigDecimal,
        right: BigDecimal,
        scale: Int = DEFAULT_SCALE,
        roundingMode: RoundingMode = DEFAULT_ROUNDING_MODE
    ): BigDecimal = round(left.add(right), scale, roundingMode)

    /** Sums all values before rounding, avoiding accumulated intermediate rounding error. */
    @JvmStatic
    @JvmOverloads
    fun sum(
        values: Iterable<BigDecimal>,
        scale: Int = DEFAULT_SCALE,
        roundingMode: RoundingMode = DEFAULT_ROUNDING_MODE
    ): BigDecimal {
        val total = values.fold(BigDecimal.ZERO, BigDecimal::add)
        return round(total, scale, roundingMode)
    }

    @JvmStatic
    @JvmOverloads
    fun subtract(
        left: BigDecimal,
        right: BigDecimal,
        scale: Int = DEFAULT_SCALE,
        roundingMode: RoundingMode = DEFAULT_ROUNDING_MODE
    ): BigDecimal = round(left.subtract(right), scale, roundingMode)

    @JvmStatic
    @JvmOverloads
    fun multiply(
        left: BigDecimal,
        right: BigDecimal,
        scale: Int = DEFAULT_SCALE,
        roundingMode: RoundingMode = DEFAULT_ROUNDING_MODE
    ): BigDecimal = round(left.multiply(right), scale, roundingMode)

    @JvmStatic
    @JvmOverloads
    fun divide(
        dividend: BigDecimal,
        divisor: BigDecimal,
        scale: Int = DEFAULT_SCALE,
        roundingMode: RoundingMode = DEFAULT_ROUNDING_MODE
    ): BigDecimal = dividend.divide(divisor, scale, roundingMode)
}
