package com.soywiz.korim.format.cg

import platform.CoreGraphics.*

expect fun Double.toCgFloatInternal(): CGFloat
inline fun Float.toCgFloat(): CGFloat = this.toDouble().toCgFloatInternal()
inline fun Double.toCgFloat(): CGFloat = this.toDouble().toCgFloatInternal()
inline fun Number.toCgFloat(): CGFloat = this.toDouble().toCgFloatInternal()
inline val Number.cg get() = this.toDouble().toCgFloat()
