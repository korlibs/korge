package com.soywiz.korge.util

import com.soywiz.korim.bitmap.*

//@Deprecated("", level = DeprecationLevel.HIDDEN)
typealias IntArray2 = Bitmap32

fun Bitmap32.toIntArray2(): com.soywiz.kds.IntArray2 = com.soywiz.kds.IntArray2(width, height, data.ints)
