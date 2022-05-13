package com.soywiz.korim.bitmap

import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.RGBAf

class FloatBitmap32(
    width: Int,
    height: Int,
    val data: FloatArray = FloatArray(width * height * 4),
    premultiplied: Boolean = false
) : Bitmap(width, height, 32, premultiplied, data) {
    private fun index4(x: Int, y: Int) = index(x, y) * 4

    override fun setRgba(x: Int, y: Int, v: RGBA) {
        val rindex = index4(x, y)
        data[rindex + 0] = v.rf
        data[rindex + 1] = v.gf
        data[rindex + 2] = v.bf
        data[rindex + 3] = v.af
    }
    override fun getRgba(x: Int, y: Int): RGBA {
        val rindex = index4(x, y)
        return RGBA.float(data[rindex + 0], data[rindex + 1], data[rindex + 2], data[rindex + 3])
    }

    fun setRgbaf(x: Int, y: Int, r: Float, g: Float, b: Float, a: Float) {
        val rindex = index4(x, y)
        data[rindex + 0] = r
        data[rindex + 1] = g
        data[rindex + 2] = b
        data[rindex + 3] = a
    }
    fun setRgbaf(x: Int, y: Int, rgbaf: RGBAf) {
        setRgbaf(x, y, rgbaf.r, rgbaf.g, rgbaf.b, rgbaf.a)
    }

    fun setRgbaf(x: Int, y: Int, v: FloatArray): Unit = setRgbaf(x, y, v[0], v[1], v[2], v[3])
    fun getRgbaf(x: Int, y: Int, out: RGBAf = RGBAf()): RGBAf {
        val rindex = index4(x, y)
        out.r = data[rindex + 0]
        out.g = data[rindex + 1]
        out.b = data[rindex + 2]
        out.a = data[rindex + 3]
        return out
    }
}

fun Bitmap.toFloatBMP32(out: FloatBitmap32 = FloatBitmap32(width, height, premultiplied = premultiplied)): FloatBitmap32 {
    for (y in 0 until height) {
        for (x in 0 until width) {
            val col = this.getRgba(x ,y)
            out.setRgbaf(x, y, col.rf, col.gf, col.bf, col.af)
        }
    }
    return out
}
