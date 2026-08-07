package com.template.framework.util

import java.math.BigDecimal

/** Parses an integer after trimming whitespace, or returns [defaultValue]. */
fun String?.toIntOrDefault(defaultValue: Int = 0): Int {
    return this?.trim()?.takeIf(String::isNotEmpty)?.toIntOrNull() ?: defaultValue
}

/** Parses a long after trimming whitespace, or returns [defaultValue]. */
fun String?.toLongOrDefault(defaultValue: Long = 0L): Long {
    return this?.trim()?.takeIf(String::isNotEmpty)?.toLongOrNull() ?: defaultValue
}

/** Parses a decimal after trimming whitespace, or returns [defaultValue]. */
fun String?.toBigDecimalOrDefault(defaultValue: BigDecimal = BigDecimal.ZERO): BigDecimal {
    val normalized = this?.trim()?.takeIf(String::isNotEmpty) ?: return defaultValue
    return normalized.toBigDecimalOrNull() ?: defaultValue
}
