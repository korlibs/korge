package com.soywiz.korim.format

import com.soywiz.kds.Extra
import com.soywiz.kds.ExtraType
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korio.async.runBlockingNoSuspensionsNullable
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName
import com.soywiz.korio.lang.runIgnoringExceptions
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.MemorySyncStreamToByteArray
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.openSync
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.stream.sliceHere
import com.soywiz.korio.stream.toAsync
import com.soywiz.korio.stream.toSyncOrNull
import kotlin.math.max

abstract class ImageFormatWithContainer(vararg exts: String) : ImageFormat(*exts) {
    override fun readImageContainer(s: SyncStream, props: ImageDecodingProps): ImageDataContainer = TODO()
    final override fun readImage(s: SyncStream, props: ImageDecodingProps, out: Bitmap?): ImageData = readImageContainer(s, props).imageDatas.first()
}

abstract class ImageFormatSuspend(vararg exts: String) : ImageFormat(*exts) {
    override suspend fun decodeHeaderSuspend(s: AsyncStream, props: ImageDecodingProps): ImageInfo? = TODO()

    final override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? =
        runBlockingNoSuspensionsNullable { decodeHeaderSuspend(s.sliceHere().toAsync(), props) }
}

interface ImageFormatDecoder {
    suspend fun decode(file: VfsFile, props: ImageDecodingProps = ImageDecodingProps.DEFAULT): Bitmap
    suspend fun decodeSuspend(data: ByteArray, props: ImageDecodingProps = ImageDecodingProps.DEFAULT): Bitmap
}

abstract class ImageFormat(vararg exts: String) : ImageFormatDecoder {
	val extensions = exts.map { it.toLowerCase().trim() }.toSet()
    open fun readImageContainer(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps.DEFAULT): ImageDataContainer = ImageDataContainer(listOf(readImage(s, props)))
	open fun readImage(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps.DEFAULT, out: Bitmap? = null): ImageData = TODO()
	open fun writeImage(
		image: ImageData,
		s: SyncStream,
		props: ImageEncodingProps = ImageEncodingProps("unknown")
	): Unit = throw UnsupportedOperationException()

    open suspend fun decodeHeaderSuspend(s: AsyncStream, props: ImageDecodingProps = ImageDecodingProps.DEFAULT): ImageInfo? {
        return decodeHeader(s.toSyncOrNull() ?: s.readAll().openSync())
    }

	open fun decodeHeader(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps.DEFAULT): ImageInfo? =
		runIgnoringExceptions(show = true) {
			val bmp = read(s, props)
			ImageInfo().apply {
				this.width = bmp.width
				this.height = bmp.height
				this.bitsPerPixel = bmp.bpp
			}
		}

	fun read(s: SyncStream, filename: String = "unknown"): Bitmap =
		readImage(s, ImageDecodingProps.DEFAULT.copy(filename = filename)).mainBitmap

	suspend fun read(file: VfsFile) = this.read(file.readAsSyncStream(), file.baseName)
	//fun read(file: File) = this.read(file.openSync(), file.name)
	fun read(s: ByteArray, filename: String = "unknown"): Bitmap = read(s.openSync(), filename)

	fun read(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps.DEFAULT, out: Bitmap? = null): Bitmap = readImage(s, props, out).mainBitmap
	//fun read(file: File, props: ImageDecodingProps = ImageDecodingProps()) = this.read(file.openSync(), props.copy(filename = file.name))
	fun read(s: ByteArray, props: ImageDecodingProps = ImageDecodingProps.DEFAULT, out: Bitmap? = null): Bitmap = read(s.openSync(), props, out)

	fun check(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps.DEFAULT): Boolean =
        runIgnoringExceptions(show = true) { decodeHeader(s, props) != null } ?: false

	fun decode(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps.DEFAULT): Bitmap = this.read(s, props)
	//fun decode(file: File, props: ImageDecodingProps = ImageDecodingProps()) = this.read(file.openSync("r"), props.copy(filename = file.name))

    // Decodes a given byte array to a bitmap based on the image format.
    // Provides an `out` parameter to reuse an existing Bitmap to reduce allocations.
    // Note though that not all formats may use the bitmap provided by the `out` param.
    // In those cases, they will return a newly allocated Bitmap instead.
    fun decode(data: ByteArray, props: ImageDecodingProps = ImageDecodingProps.DEFAULT, out: Bitmap? = null): Bitmap = read(data.openSync(), props, out)

	override suspend fun decodeSuspend(data: ByteArray, props: ImageDecodingProps): Bitmap = decode(data, props)

	//fun decode(s: SyncStream, filename: String = "unknown") = this.read(s, filename)
	suspend fun decode(file: VfsFile) = this.read(file.readAsSyncStream(), file.baseName)
	//fun decode(file: File) = this.read(file.openSync("r"), file.name)
	//fun decode(s: ByteArray, filename: String = "unknown"): Bitmap = read(s.openSync(), filename)

    override suspend fun decode(s: VfsFile, props: ImageDecodingProps): Bitmap =
        this.read(s.readAsSyncStream(), props.copy(filename = s.baseName))

    suspend fun decode(s: AsyncStream, filename: String) = this.read(s.readAll(), ImageDecodingProps(filename))
    suspend fun decode(s: AsyncStream, props: ImageDecodingProps = ImageDecodingProps.DEFAULT) =
        this.read(s.readAll(), props)


    fun encode(frames: List<ImageFrame>, props: ImageEncodingProps = ImageEncodingProps("unknown")): ByteArray =
		MemorySyncStreamToByteArray(frames.area * 4) { writeImage(ImageData(frames), this, props) }

	fun encode(image: ImageData, props: ImageEncodingProps = ImageEncodingProps("unknown")): ByteArray =
		MemorySyncStreamToByteArray(image.area * 4) { writeImage(image, this, props) }

	fun encode(bitmap: Bitmap, props: ImageEncodingProps = ImageEncodingProps("unknown")): ByteArray =
		encode(listOf(ImageFrame(bitmap)), props)

	suspend fun read(file: VfsFile, props: ImageDecodingProps = ImageDecodingProps.DEFAULT): ImageData =
		this.readImage(file.readAll().openSync(), props.copy(filename = file.baseName))

	override fun toString(): String = "ImageFormat($extensions)"
}

class ImageDecoderNotFoundException : Exception("Can't read image using AWT. No available decoder for input")

data class ImageDecodingProps constructor(
    val filename: String = "unknown",
    val width: Int? = null,
    val height: Int? = null,
    val premultiplied: Boolean? = null,
    val asumePremultiplied: Boolean = false,
    // Requested but not enforced. Max width and max height
    val requestedMaxSize: Int? = null,
    val debug: Boolean = false,
    val preferKotlinDecoder: Boolean = false,
    val tryNativeDecode: Boolean = true,
    val format: ImageFormat? = RegisteredImageFormats,
    override var extra: ExtraType = null
) : Extra {

    val premultipliedSure: Boolean get() = premultiplied ?: true
    val formatSure: ImageFormat get() = format ?: RegisteredImageFormats

    // https://developer.android.com/reference/android/graphics/BitmapFactory.Options#inSampleSize
    fun getSampleSize(originalWidth: Int, originalHeight: Int): Int {
        var sampleSize = 1
        var width = originalWidth
        var height = originalHeight
        val maxWidth = max(1, requestedMaxSize ?: originalWidth)
        val maxHeight = max(1, requestedMaxSize ?: originalHeight)
        while (width > maxWidth || height > maxHeight) {
            width /= 2
            height /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    companion object {
        val DEFAULT: ImageDecodingProps = ImageDecodingProps(premultiplied = true)
        //val DEFAULT: ImageDecodingProps = ImageDecodingProps(premultiplied = false)
        val DEFAULT_PREMULT: ImageDecodingProps = ImageDecodingProps(premultiplied = true)
        val DEFAULT_STRAIGHT: ImageDecodingProps = ImageDecodingProps(premultiplied = false)
        fun DEFAULT(premultiplied: Boolean): ImageDecodingProps = if (premultiplied) DEFAULT_PREMULT else DEFAULT_STRAIGHT
    }
}

fun ImageFormat.toProps(props: ImageDecodingProps = ImageDecodingProps.DEFAULT): ImageDecodingProps =
    props.copy(format = this)

data class ImageEncodingProps(
    val filename: String = "",
    val quality: Double = 0.81,
    override var extra: ExtraType = null
) : Extra

