package com.soywiz.korim.bitmap

import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.encoding.*

abstract class NativeImage(width: Int, height: Int, val data: Any?, premultiplied: Boolean) : Bitmap(width, height, 32, premultiplied, null) {
    /** Allow to force to use a texture id from OpenGL. For example a video texture from Android */
    open val forcedTexId: Int get() = -1
    /** Allow to force to use a texture target from OpenGL. For example a video texture from Android (-1 means GL_TEXTURE_2D) */
    open val forcedTexTarget: Int get() = -1

	open val name: String = "NativeImage"
    open fun toUri(): String = "data:image/png;base64," + PNG.encode(this, ImageEncodingProps("out.png")).toBase64()

	fun toNonNativeBmp(): Bitmap = toBMP32()
    override fun toBMP32(): Bitmap32 = Bitmap32(width, height, Colors.TRANSPARENT_BLACK, premultiplied).also { readPixelsUnsafe(0, 0, width, height, it.data, 0) }

    abstract override fun readPixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int)
    abstract override fun writePixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int)

    override fun setRgba(x: Int, y: Int, v: RGBA) {
        this.tempRgba[0] = v
        writePixelsUnsafe(x, y, 1, 1, tempRgba, 0)
    }

    override fun getRgba(x: Int, y: Int): RGBA {
        readPixelsUnsafe(x, y, 1, 1, tempRgba, 0)
        return tempRgba[0]
    }

    override fun setInt(x: Int, y: Int, color: Int) = setRgba(x, y, RGBA(color))
    override fun getInt(x: Int, y: Int): Int = getRgba(x, y).value

    override fun swapRows(y0: Int, y1: Int) {
        readPixelsUnsafe(0, y0, width, 1, tempRgba, 0)
        readPixelsUnsafe(0, y1, width, 1, tempRgba, width)
        writePixelsUnsafe(0, y1, width, 1, tempRgba, 0)
        writePixelsUnsafe(0, y0, width, 1, tempRgba, width)
    }

	override fun createWithThisFormat(width: Int, height: Int): Bitmap = NativeImage(width, height)
    override fun toString(): String = "$name($width, $height)"
}

fun Bitmap.mipmap(levels: Int): NativeImage = nativeImageFormatProvider.mipmap(this, levels)

fun Bitmap.toUri(): String {
	if (this is NativeImage) return this.toUri()
	return "data:image/png;base64," + PNG.encode(this, ImageEncodingProps("out.png")).toBase64()
}

fun NativeImageOrBitmap32(width: Int, height: Int, native: Boolean = true) =
    if (native) NativeImage(width, height) else Bitmap32(width, height, premultiplied = true)
fun NativeImage(width: Int, height: Int) = nativeImageFormatProvider.create(width, height)

fun NativeImage(
	width: Int,
	height: Int,
	d: Drawable,
	scaleX: Double = 1.0,
	scaleY: Double = scaleX
): NativeImage {
	val bmp = NativeImage(width, height)
	try {
		val ctx = bmp.getContext2d()
		ctx.keep {
			ctx.scale(scaleX, scaleY)
			ctx.draw(d)
		}
	} catch (e: Throwable) {
		e.printStackTrace()
	}
	return bmp
}

fun NativeImage(d: SizedDrawable, scaleX: Double = 1.0, scaleY: Double = scaleX): NativeImage =
    NativeImage((d.width * scaleX).toInt(), (d.height * scaleY).toInt(), d, scaleX, scaleY)

fun Bitmap.ensureNative() = when (this) {
	is NativeImage -> this
	else -> nativeImageFormatProvider.copy(this)
}

fun SizedDrawable.raster(scaleX: Double = 1.0, scaleY: Double = scaleX) = NativeImage(this, scaleX, scaleY)
