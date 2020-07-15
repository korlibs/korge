package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.FontMetrics
import com.soywiz.korim.font.GlyphMetrics
import com.soywiz.korim.font.SystemFont
import com.soywiz.korim.vector.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.*
import kotlin.jvm.JvmOverloads
import kotlin.math.*

expect val nativeImageFormatProvider: NativeImageFormatProvider

data class NativeImageConfig(val premultiplied: Boolean)

abstract class NativeImageFormatProvider {
	abstract suspend fun decode(data: ByteArray, premultiplied: Boolean): NativeImage
    open suspend fun decode(vfs: Vfs, path: String, premultiplied: Boolean): NativeImage = decode(vfs.file(path).readBytes())
    suspend fun decode(file: FinalVfsFile, premultiplied: Boolean): Bitmap = decode(file.vfs, file.path, premultiplied)
	suspend fun decode(file: VfsFile, premultiplied: Boolean): Bitmap = decode(file.getUnderlyingUnscapedFile(), premultiplied)

    suspend fun decode(data: ByteArray): NativeImage = decode(data, premultiplied = true)
    suspend fun decode(vfs: Vfs, path: String): NativeImage = decode(vfs, path, premultiplied = true)
    suspend fun decode(file: FinalVfsFile): Bitmap = decode(file, premultiplied = true)
    suspend fun decode(file: VfsFile): Bitmap = decode(file, premultiplied = true)

    abstract suspend fun display(bitmap: Bitmap, kind: Int): Unit
	abstract fun create(width: Int, height: Int): NativeImage
    //open fun create(width: Int, height: Int, premultiplied: Boolean): NativeImage = create(width, height)
	open fun copy(bmp: Bitmap): NativeImage = create(bmp.width, bmp.height).apply { context2d { drawImage(bmp, 0, 0) } }
	open fun mipmap(bmp: Bitmap, levels: Int): NativeImage = bmp.toBMP32().mipmap(levels).ensureNative()
	open fun mipmap(bmp: Bitmap): NativeImage {
        val out = NativeImage(ceil(bmp.width * 0.5).toInt(), ceil(bmp.height * 0.5).toInt())
        out.getContext2d(antialiasing = true).renderer.drawImage(bmp, 0, 0, out.width, out.height)
        return out
    }
}

suspend fun Bitmap.showImageAndWait(kind: Int = 0) = nativeImageFormatProvider.display(this, kind)
suspend fun ImageData.showImagesAndWait(kind: Int = 0) = run { for (frame in frames) frame.bitmap.showImageAndWait(kind) }
suspend fun SizedDrawable.showImageAndWait(kind: Int = 0) = this.render().toBMP32().showImageAndWait(kind)

open class BaseNativeImageFormatProvider : NativeImageFormatProvider() {
    open val formats: ImageFormat get() = RegisteredImageFormats

    override suspend fun decode(data: ByteArray, premultiplied: Boolean): NativeImage = wrapNative(formats.decode(data), premultiplied)
    override suspend fun decode(vfs: Vfs, path: String, premultiplied: Boolean): NativeImage = decode(vfs[path].readBytes(), premultiplied)
    protected open fun createBitmapNativeImage(bmp: Bitmap) = BitmapNativeImage(bmp)
    protected open fun wrapNative(bmp: Bitmap, premultiplied: Boolean): BitmapNativeImage {
        val bmp32: Bitmap32 = bmp.toBMP32()
        //bmp32.premultiplyInPlace()
        //return BitmapNativeImage(bmp32)
        return createBitmapNativeImage(if (premultiplied) bmp32.premultipliedIfRequired() else bmp32.depremultipliedIfRequired())
    }
    protected fun Bitmap.wrapNativeExt(premultiplied: Boolean = true) = wrapNative(this, premultiplied)

    override fun create(width: Int, height: Int): NativeImage = createBitmapNativeImage(Bitmap32(width, height, premultiplied = true))
    override fun copy(bmp: Bitmap): NativeImage = createBitmapNativeImage(bmp)
    override suspend fun display(bitmap: Bitmap, kind: Int) {
        println("TODO: NativeNativeImageFormatProvider.display(bitmap=$bitmap, kind=$kind)")
    }
    override fun mipmap(bmp: Bitmap, levels: Int): NativeImage = createBitmapNativeImage(bmp)
    override fun mipmap(bmp: Bitmap): NativeImage = createBitmapNativeImage(bmp)
}

open class BitmapNativeImage(val bitmap: Bitmap32) : NativeImage(bitmap.width, bitmap.height, bitmap, bitmap.premultiplied) {
    @Suppress("unused")
    val intData: IntArray = bitmap.data.ints
    constructor(bitmap: Bitmap) : this(bitmap.toBMP32())
    override fun getContext2d(antialiasing: Boolean): Context2d = bitmap.getContext2d(antialiasing)
    override fun toBMP32(): Bitmap32 = bitmap
    override fun readPixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int) = bitmap.readPixelsUnsafe(x, y, width, height, out, offset)
    override fun writePixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int) = bitmap.writePixelsUnsafe(x, y, width, height, out, offset)
    override fun setRgba(x: Int, y: Int, v: RGBA) = bitmap.setRgba(x, y, v)
    override fun getRgba(x: Int, y: Int): RGBA = bitmap.getRgba(x, y)
}
