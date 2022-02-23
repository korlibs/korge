package com.soywiz.korim.format.cg

import platform.CoreGraphics.*

// @TODO: K/N .convert() doesn't work to convert integers to doubles
expect fun Double.toCgFloatInternal(): CGFloat

inline fun Float.toCgFloat(): CGFloat = this.toDouble().toCgFloatInternal()
inline fun Double.toCgFloat(): CGFloat = this.toDouble().toCgFloatInternal()
inline fun Number.toCgFloat(): CGFloat = this.toDouble().toCgFloatInternal()
inline val Number.cg get() = this.toDouble().toCgFloat()

// @TODO: This is generic, but likely slow unless optimized?
// @TODO: I would like this to be optimized on both Debug and Release builds
inline fun <reified T : Number> Number.convertFloat(): T {
    return when (T::class) {
        Double::class -> this.toDouble() as T
        Float::class -> this.toFloat() as T
        else -> TODO()
    }
}
