package com.soywiz.korim.format.cg

import platform.CoreGraphics.*

// @TODO: K/N .convert() doesn't work to convert integers to doubles
expect inline fun Double.toCgFloat(): CGFloat
expect inline fun Float.toCgFloat(): CGFloat

inline val Int.cg get() = this.toDouble().toCgFloat()
inline val Float.cg get() = this.toCgFloat()
inline val Double.cg get() = this.toCgFloat()
