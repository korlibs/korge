package com.soywiz.korim.format.cg

import kotlinx.cinterop.UnsafeNumber
import platform.CoreGraphics.*

// @TODO: K/N .convert() doesn't work to convert integers to doubles
@OptIn(UnsafeNumber::class)
expect inline fun Double.toCgFloat(): CGFloat
@OptIn(UnsafeNumber::class)
expect inline fun Float.toCgFloat(): CGFloat

@OptIn(UnsafeNumber::class)
inline val Int.cg: CGFloat get() = this.toDouble().toCgFloat()
@OptIn(UnsafeNumber::class)
inline val Float.cg: CGFloat get() = this.toCgFloat()
@OptIn(UnsafeNumber::class)
inline val Double.cg: CGFloat get() = this.toCgFloat()
