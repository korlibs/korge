package com.soywiz.korio.internal

@PublishedApi internal fun min2(a: Int, b: Int) = if (a < b) a else b
@PublishedApi internal fun max2(a: Int, b: Int) = if (a > b) a else b

@PublishedApi internal fun min2(a: Float, b: Float) = if (a < b) a else b
@PublishedApi internal fun max2(a: Float, b: Float) = if (a > b) a else b

@PublishedApi internal fun min2(a: Double, b: Double) = if (a < b) a else b
@PublishedApi internal fun max2(a: Double, b: Double) = if (a > b) a else b

@PublishedApi internal fun min2(a: Long, b: Long) = if (a < b) a else b
@PublishedApi internal fun max2(a: Long, b: Long) = if (a > b) a else b

internal infix fun Int.divCeil(other: Int): Int {
    val res = this / other
    if (this % other != 0) return res + 1
    return res
}

internal infix fun Long.divCeil(other: Long): Long {
    val res = this / other
    if (this % other != 0L) return res + 1
    return res
}

// @TODO: Move to KDS?
internal inline fun <T> List<T>.without(element: T): List<T> = this - element
