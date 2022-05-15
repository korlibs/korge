package com.soywiz.korim.bitmap

import com.soywiz.kmem.arraycopy
import com.soywiz.korim.color.ColorFormat
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.RGBA_4444
import com.soywiz.korim.color.packRGBA
import com.soywiz.korim.color.unpackToRGBA
import com.soywiz.korio.lang.assert

class Bitmap16(
    width: Int,
    height: Int,
    val data: ShortArray = ShortArray(width * height),
    val format: ColorFormat = RGBA_4444,
    premultiplied: Boolean = false
) : Bitmap(width, height, 16, premultiplied, data) {
    init {
        assert(data.size >= width * height)
    }
	override fun createWithThisFormat(width: Int, height: Int): Bitmap =
		Bitmap16(width, height, format = format, premultiplied = premultiplied)

    override fun clone() = Bitmap16(width, height, data.copyOf(), format, premultiplied)

    operator fun set(x: Int, y: Int, color: Int) = setInt(x, y, color)
	operator fun get(x: Int, y: Int): Int = getInt(x, y)

	override fun setInt(x: Int, y: Int, color: Int) = Unit.apply { data[index(x, y)] = color.toShort() }
	override fun getInt(x: Int, y: Int): Int = data[index(x, y)].toInt() and 0xFFFF

	override fun setRgba(x: Int, y: Int, v: RGBA) = setInt(x, y, format.packRGBA(v))
	override fun getRgba(x: Int, y: Int): RGBA = format.unpackToRGBA(data[index(x, y)].toInt())

	override fun copyUnchecked(srcX: Int, srcY: Int, dst: Bitmap, dstX: Int, dstY: Int, width: Int, height: Int) {
        if (dst !is Bitmap16) return super.copyUnchecked(srcX, srcY, dst, dstX, dstY, width, height)
        val src = this
        val srcArray = src.data
        val dstArray = dst.data
        for (y in 0 until height) {
            arraycopy(srcArray, src.index(srcX, srcY + y), dstArray, dst.index(dstX, dstY + y), width)
        }
	}

	override fun toString(): String = "Bitmap16($width, $height, format=$format)"
}
