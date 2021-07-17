package com.soywiz.korim.bitmap

import com.soywiz.korim.color.*

open class Palette(
    val colors: RgbaArray,
    val names: Array<String?>? = null,
    val changeStart: Int = 0,
    val changeEnd: Int = 0
) {
}
