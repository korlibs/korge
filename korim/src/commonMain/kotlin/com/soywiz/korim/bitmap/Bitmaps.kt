package com.soywiz.korim.bitmap

import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RgbaArray
import kotlin.native.concurrent.SharedImmutable

@SharedImmutable @PublishedApi internal val Bitmaps_transparent: BmpSlice32 = Bitmap32(1, 1, Colors.TRANSPARENT_BLACK.premultiplied).slice(name = "transparent")
@SharedImmutable @PublishedApi internal val Bitmaps_white: BmpSlice32 = Bitmap32(1, 1, Colors.WHITE.premultiplied).slice(name = "white")

object Bitmaps {
    inline val transparent: BmpSlice32 get() = Bitmaps_transparent
    inline val white: BmpSlice32 get() = Bitmaps_white
}
