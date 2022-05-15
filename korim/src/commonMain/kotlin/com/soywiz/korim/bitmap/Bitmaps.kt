package com.soywiz.korim.bitmap

import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RgbaArray
import kotlin.native.concurrent.SharedImmutable

@SharedImmutable @PublishedApi internal val Bitmaps_transparent: BitmapSlice<Bitmap32> = Bitmap32(1, 1).slice(name = "transparent")
@SharedImmutable @PublishedApi internal val Bitmaps_white: BitmapSlice<Bitmap32> = Bitmap32(1, 1, RgbaArray(1) { Colors.WHITE }).slice(name = "white")

object Bitmaps {
    inline val transparent: BitmapSlice<Bitmap32> get() = Bitmaps_transparent
    inline val white: BitmapSlice<Bitmap32> get() = Bitmaps_white
}
