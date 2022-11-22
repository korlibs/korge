package com.soywiz.korma.internal

import com.soywiz.korma.math.*
import kotlin.math.*

//internal val Float.niceStr: String get() = if (almostEquals(this.toLong().toFloat(), this)) "${this.toLong()}" else "$this"
//internal val Double.niceStr: String get() = if (almostEquals(this.toLong().toDouble(), this)) "${this.toLong()}" else "$this"

internal val Float.niceStr: String get() = buildString { appendNice(this@niceStr) }
internal val Double.niceStr: String get() = buildString { appendNice(this@niceStr) }
internal fun Float.niceStr(decimalPlaces: Int): String = roundDecimalPlaces(decimalPlaces).niceStr
internal fun Double.niceStr(decimalPlaces: Int): String = roundDecimalPlaces(decimalPlaces).niceStr

internal fun StringBuilder.appendNice(value: Double) {
    when {
        round(value).isAlmostEquals(value) -> when {
            value >= Int.MIN_VALUE.toDouble() && value <= Int.MAX_VALUE.toDouble() -> append(value.toInt())
            else -> append(value.toLong())
        }
        else -> append(value)
    }
}

internal fun StringBuilder.appendNice(value: Float) {
    when {
        round(value).isAlmostEquals(value) -> when {
            value >= Int.MIN_VALUE.toFloat() && value <= Int.MAX_VALUE.toFloat() -> append(value.toInt())
            else -> append(value.toLong())
        }
        else -> append(value)
    }
}

@PublishedApi internal infix fun Int.umod(other: Int): Int {
    val remainder = this % other
    return when {
        remainder < 0 -> remainder + other
        else -> remainder
    }
}

@PublishedApi internal infix fun Double.umod(other: Double): Double {
    val remainder = this % other
    return when {
        remainder < 0 -> remainder + other
        else -> remainder
    }
}

@PublishedApi internal infix fun Float.umod(other: Float): Float {
    val remainder = this % other
    return when {
        remainder < 0 -> remainder + other
        else -> remainder
    }
}

@PublishedApi
internal fun floorCeil(v: Double): Double = if (v < 0.0) ceil(v) else floor(v)









