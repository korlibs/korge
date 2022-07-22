package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.asumePremultiplied
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.bitmap.ensureNative
import com.soywiz.korim.bitmap.extract
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.RgbaArray
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.SizedDrawable
import com.soywiz.korim.vector.render
import com.soywiz.korio.file.FinalVfsFile
import com.soywiz.korio.file.Vfs
import com.soywiz.korio.file.VfsFile
import kotlinx.coroutines.CancellationException
import kotlin.math.ceil
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
expect val nativeImageFormatProvider: NativeImageFormatProvider

data class NativeImageResult(
    val image: NativeImage,
    val originalWidth: Int = image.width,
    val originalHeight: Int = image.height,
)

abstract class NativeImageFormatProvider : ImageFormatDecoder {
    protected open suspend fun decodeHeaderInternal(data: ByteArray): ImageInfo {
        val result = decodeInternal(data, ImageDecodingProps.DEFAULT)
        return ImageInfo().also {
            it.width = result.originalWidth
            it.height = result.originalHeight
        }
    }

    protected abstract suspend fun decodeInternal(data: ByteArray, props: ImageDecodingProps): NativeImageResult
    protected open suspend fun decodeInternal(vfs: Vfs, path: String, props: ImageDecodingProps): NativeImageResult = decodeInternal(vfs.file(path).readBytes(), props)

    protected fun NativeImage.result(props: ImageDecodingProps): NativeImageResult {
        return NativeImageResult(when {
            props.asumePremultiplied -> this.asumePremultiplied()
            else -> this
        })
    }

    suspend fun decodeHeader(data: ByteArray): ImageInfo = decodeHeaderInternal(data)
    suspend fun decodeHeaderOrNull(data: ByteArray): ImageInfo? = try {
        decodeHeaderInternal(data)
    } catch (e: Throwable) {
        if (e is CancellationException) throw e
        null
    }

    suspend fun decode(vfs: Vfs, path: String, props: ImageDecodingProps): NativeImage = decodeInternal(vfs, path, props).image
    suspend fun decode(data: ByteArray, props: ImageDecodingProps = ImageDecodingProps.DEFAULT): NativeImage = decodeInternal(data, props).image
    override suspend fun decodeSuspend(data: ByteArray, props: ImageDecodingProps): NativeImage = decodeInternal(data, props).image
    suspend fun decode(file: FinalVfsFile, props: ImageDecodingProps): Bitmap = decodeInternal(file.vfs, file.path, props).image
    override suspend fun decode(file: VfsFile, props: ImageDecodingProps): Bitmap = decode(file.getUnderlyingUnscapedFile(), props)

    suspend fun decode(vfs: Vfs, path: String, premultiplied: Boolean = true): NativeImage = decode(vfs, path, ImageDecodingProps.DEFAULT(premultiplied))
    suspend fun decode(data: ByteArray, premultiplied: Boolean): NativeImage = decode(data, ImageDecodingProps.DEFAULT(premultiplied))
    suspend fun decode(file: FinalVfsFile, premultiplied: Boolean = true): Bitmap = decode(file, ImageDecodingProps.DEFAULT(premultiplied))
    suspend fun decode(file: VfsFile, premultiplied: Boolean): Bitmap = decode(file, ImageDecodingProps.DEFAULT(premultiplied))

    abstract suspend fun display(bitmap: Bitmap, kind: Int): Unit
    abstract fun create(width: Int, height: Int, premultiplied: Boolean? = null): NativeImage
    open fun create(width: Int, height: Int, pixels: RgbaArray, premultiplied: Boolean? = null): NativeImage {
        val image = create(width, height, premultiplied)
        image.writePixelsUnsafe(0, 0, width, height, pixels)
        return image
    }
    //open fun create(width: Int, height: Int, premultiplied: Boolean): NativeImage = create(width, height)
	open fun copy(bmp: Bitmap): NativeImage = create(bmp.width, bmp.height, bmp.premultiplied).apply { context2d { drawImage(bmp, 0, 0) } }
	open fun mipmap(bmp: Bitmap, levels: Int): NativeImage = bmp.toBMP32().mipmap(levels).ensureNative()
	open fun mipmap(bmp: Bitmap): NativeImage {
        val out = NativeImage(ceil(bmp.width * 0.5).toInt(), ceil(bmp.height * 0.5).toInt())
        out.context2d(antialiased = true) {
            renderer.drawImage(bmp, 0, 0, out.width, out.height)
        }
        return out
    }
}

suspend fun BmpSlice.showImageAndWait(kind: Int = 0) = extract().showImageAndWait(kind)
suspend fun Bitmap.showImageAndWait(kind: Int = 0) = nativeImageFormatProvider.display(this, kind)
suspend fun ImageData.showImagesAndWait(kind: Int = 0) { for (frame in frames) frame.bitmap.showImageAndWait(kind) }
suspend fun List<Bitmap>.showImagesAndWait(kind: Int = 0) { for (bitmap in this) bitmap.showImageAndWait(kind) }
suspend fun SizedDrawable.showImageAndWait(kind: Int = 0) = this.render().toBMP32().showImageAndWait(kind)

open class BaseNativeImageFormatProvider : NativeImageFormatProvider() {
    open val formats: ImageFormat get() = RegisteredImageFormats

    override suspend fun decodeInternal(data: ByteArray, props: ImageDecodingProps): NativeImageResult = wrapNative(formats.decode(data, props), props)

    protected open fun createBitmapNativeImage(bmp: Bitmap): BitmapNativeImage = BitmapNativeImage(bmp)
    protected open fun wrapNative(bmp: Bitmap, props: ImageDecodingProps): NativeImageResult {
        val bmp32: Bitmap32 = bmp.toBMP32IfRequired()
        //bmp32.premultiplyInPlace()
        //return BitmapNativeImage(bmp32)
        return NativeImageResult(createBitmapNativeImage(
            when {
                props.asumePremultiplied -> bmp32.asumePremultiplied()
                props.premultipliedSure -> bmp32.premultipliedIfRequired()
                else -> bmp32.depremultipliedIfRequired()
            }
        ))
    }
    protected fun Bitmap.wrapNativeExt(props: ImageDecodingProps = ImageDecodingProps.DEFAULT_PREMULT) = wrapNative(this, props)

    override fun create(width: Int, height: Int, premultiplied: Boolean?): NativeImage = createBitmapNativeImage(Bitmap32(width, height, premultiplied = premultiplied ?: true))
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
    constructor(bitmap: Bitmap) : this(bitmap.toBMP32IfRequired())
    override fun getContext2d(antialiasing: Boolean): Context2d = bitmap.getContext2d(antialiasing)
    override fun toBMP32(): Bitmap32 = bitmap
    override fun readPixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int) = bitmap.readPixelsUnsafe(x, y, width, height, out, offset)
    override fun writePixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int) = bitmap.writePixelsUnsafe(x, y, width, height, out, offset)
    override fun setRgbaRaw(x: Int, y: Int, v: RGBA) = bitmap.setRgbaRaw(x, y, v)
    override fun getRgbaRaw(x: Int, y: Int): RGBA = bitmap.getRgbaRaw(x, y)
}
