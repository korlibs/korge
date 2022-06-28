package com.soywiz.korim.vector

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.NativeImageOrBitmap32
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.RgbaArray
import com.soywiz.korma.geom.Rectangle

class BitmapVector(
    val shape: BoundsDrawable,
    val bounds: Rectangle = shape.bounds,
    val scale: Double = 1.0,
    val rasterizerMethod: ShapeRasterizerMethod = ShapeRasterizerMethod.X4,
    val antialiasing: Boolean = true,
    width: Int = (bounds.width * scale).toInt(),
    height: Int = (bounds.height * scale).toInt(),
    premultiplied: Boolean,
    val native: Boolean = true
)
    : Bitmap(width, height, 32, premultiplied, null)
{
    // Displacements
    val left = bounds.x
    val top = bounds.y

    init {
        //println("BitmapVector: $width, $height")
        if (width >= 4096 || height >= 4096) error("Bitmap is too big")
    }

    val nativeImage: Bitmap by lazy {
        NativeImageOrBitmap32(width, height, native = native, premultiplied = premultiplied).context2d(antialiasing) {
            scale(scale, scale)
            translate(-bounds.x, -bounds.y)
            drawShape(shape, rasterizerMethod)
        }
    }

    private val bmp32: Bitmap32 by lazy {
        nativeImage.toBMP32IfRequired()
    }

    override fun lock() = nativeImage.lock()
    override fun unlock(rect: Rectangle?): Int = nativeImage.unlock(rect)
    override fun readPixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int): Unit = nativeImage.readPixelsUnsafe(x, y, width, height, out, offset)
    override fun writePixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int) = nativeImage.writePixelsUnsafe(x, y, width, height, out, offset)
    override fun setRgba(x: Int, y: Int, v: RGBA) = nativeImage.setRgba(x, y, v)
    override fun getRgba(x: Int, y: Int): RGBA = nativeImage.getRgba(x, y)
    override fun setInt(x: Int, y: Int, color: Int) = nativeImage.setInt(x, y, color)
    override fun getInt(x: Int, y: Int): Int = nativeImage.getInt(x, y)
    override fun copyUnchecked(srcX: Int, srcY: Int, dst: Bitmap, dstX: Int, dstY: Int, width: Int, height: Int) = nativeImage.copyUnchecked(srcX, srcY, dst, dstX, dstY, width, height)
    override fun swapRows(y0: Int, y1: Int) = nativeImage.swapRows(y0, y1)
    override fun swapColumns(x0: Int, x1: Int) = nativeImage.swapColumns(x0, x1)
    override fun createWithThisFormat(width: Int, height: Int): Bitmap = nativeImage.createWithThisFormat(width, height)
    override fun toBMP32(): Bitmap32 = bmp32
    override fun clone(): Bitmap = BitmapVector(shape, bounds, scale, rasterizerMethod, antialiasing, width, height, premultiplied)

    // @TODO: Maybe here we can expand this shape
    override fun getContext2d(antialiasing: Boolean): Context2d = nativeImage.getContext2d()
}
