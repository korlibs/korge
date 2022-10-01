package com.soywiz.korim.format

import com.soywiz.korim.color.BGRA
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.RgbaArray

// https://developer.android.com/reference/android/graphics/Color
fun RGBA.toAndroidColor(): Int {
    return BGRA.rgbaToBgra(this.value)
    //return android.graphics.Color.argb(a, r, g, b)
}
fun RGBA.Companion.fromAndroidColor(color: Int): RGBA {
    return RGBA(BGRA.bgraToRgba(color))
    //return RGBA(
    //    android.graphics.Color.red(color),
    //    android.graphics.Color.green(color),
    //    android.graphics.Color.blue(color),
    //    android.graphics.Color.alpha(color)
    //)
}

object AndroidColor {
    fun rgbaToAndroid(array: RgbaArray, offset: Int, count: Int, out: RgbaArray = array) {
        for (n in offset until offset + count) {
            out.ints[n] = array[n].toAndroidColor()
        }
    }
    fun androidToRgba(array: RgbaArray, offset: Int, count: Int, out: RgbaArray = array) {
        for (n in offset until offset + count) {
            out[n] = RGBA.fromAndroidColor(array.ints[n])
        }
    }
}
