package com.soywiz.korim.format.cg

import platform.CoreGraphics.*

expect fun Double.toCgFloat(): CGFloat
inline fun Number.toCgFloat(): CGFloat = this.toDouble().toCgFloat()
inline val Number.cg get() = this.toDouble().toCgFloat()
