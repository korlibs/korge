package com.soywiz.korma.internal

import com.soywiz.korma.math.*
import kotlin.math.*

internal val Float.niceStr: String get() = if (almostEquals(this.toLong().toFloat(), this)) "${this.toLong()}" else "$this"
internal val Double.niceStr: String get() = if (almostEquals(this.toLong().toDouble(), this)) "${this.toLong()}" else "$this"

internal infix fun Double.umod(other: Double): Double {
    val remainder = this % other
    return when {
        remainder < 0 -> remainder + other
        else -> remainder
    }
}

internal infix fun Float.umod(other: Float): Float {
    val remainder = this % other
    return when {
        remainder < 0 -> remainder + other
        else -> remainder
    }
}

@PublishedApi
internal fun floorCeil(v: Double): Double = if (v < 0.0) ceil(v) else floor(v)

@PublishedApi internal fun min2(a: Int, b: Int) = if (a < b) a else b
@PublishedApi internal fun max2(a: Int, b: Int) = if (a > b) a else b

@PublishedApi internal fun min2(a: Float, b: Float) = if (a < b) a else b
@PublishedApi internal fun max2(a: Float, b: Float) = if (a > b) a else b

@PublishedApi internal fun min2(a: Double, b: Double) = if (a < b) a else b
@PublishedApi internal fun max2(a: Double, b: Double) = if (a > b) a else b
