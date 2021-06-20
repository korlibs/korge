package com.soywiz.korio.internal













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
