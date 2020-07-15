package com.soywiz.korim.bitmap

import com.soywiz.korim.color.*
import kotlin.native.concurrent.*

@ThreadLocal
object Bitmaps {
    val transparent: BitmapSlice<Bitmap32> = Bitmap32(1, 1).slice(name = "transparent")
    val white: BitmapSlice<Bitmap32> = Bitmap32(1, 1, RgbaArray(1) { Colors.WHITE }).slice(name = "white")
}
