package com.soywiz.korim.format.cg

import platform.CoreGraphics.*

actual inline fun Double.toCgFloat(): CGFloat = this.toFloat()
actual inline fun Float.toCgFloat(): CGFloat = this
