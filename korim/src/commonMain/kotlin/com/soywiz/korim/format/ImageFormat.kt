package com.soywiz.korim.format

import com.soywiz.kds.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korma.geom.*
import kotlin.math.*

abstract class ImageFormatWithContainer(vararg exts: String) : ImageFormat(*exts) {
    override fun readImageContainer(s: SyncStream, props: ImageDecodingProps): ImageDataContainer = TODO()
    final override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData = readImageContainer(s, props).imageDatas.first()
}

abstract class ImageFormatSuspend(vararg exts: String) : ImageFormat(*exts) {
    override suspend fun decodeHeaderSuspend(s: AsyncStream, props: ImageDecodingProps): ImageInfo? = TODO()

    final override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? =
        runBlockingNoSuspensionsNullable { decodeHeaderSuspend(s.sliceHere().toAsync(), props) }
}

abstract class ImageFormat(vararg exts: String) {
	val extensions = exts.map { it.toLowerCase().trim() }.toSet()
    open fun readImageContainer(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps()): ImageDataContainer = ImageDataContainer(listOf(readImage(s, props)))
	open fun readImage(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps()): ImageData = TODO()
	open fun writeImage(
		image: ImageData,
		s: SyncStream,
		props: ImageEncodingProps = ImageEncodingProps("unknown")
	): Unit = throw UnsupportedOperationException()

    open suspend fun decodeHeaderSuspend(s: AsyncStream, props: ImageDecodingProps = ImageDecodingProps()): ImageInfo? {
        return decodeHeader(s.toSyncOrNull() ?: s.readAll().openSync())
    }

	open fun decodeHeader(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps()): ImageInfo? =
		runIgnoringExceptions(show = true) {
			val bmp = read(s, props)
			ImageInfo().apply {
				this.width = bmp.width
				this.height = bmp.height
				this.bitsPerPixel = bmp.bpp
			}
		}

	fun read(s: SyncStream, filename: String = "unknown"): Bitmap =
		readImage(s, ImageDecodingProps().copy(filename = filename)).mainBitmap

	suspend fun read(file: VfsFile) = this.read(file.readAsSyncStream(), file.baseName)
	//fun read(file: File) = this.read(file.openSync(), file.name)
	fun read(s: ByteArray, filename: String = "unknown"): Bitmap = read(s.openSync(), filename)

	fun read(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps()): Bitmap = readImage(s, props).mainBitmap
	//fun read(file: File, props: ImageDecodingProps = ImageDecodingProps()) = this.read(file.openSync(), props.copy(filename = file.name))
	fun read(s: ByteArray, props: ImageDecodingProps = ImageDecodingProps()): Bitmap = read(s.openSync(), props)

	fun check(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps()): Boolean =
        runIgnoringExceptions(show = true) { decodeHeader(s, props) != null } ?: false

	fun decode(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps()) = this.read(s, props)
	//fun decode(file: File, props: ImageDecodingProps = ImageDecodingProps()) = this.read(file.openSync("r"), props.copy(filename = file.name))
	fun decode(s: ByteArray, props: ImageDecodingProps = ImageDecodingProps()): Bitmap = read(s.openSync(), props)

	//fun decode(s: SyncStream, filename: String = "unknown") = this.read(s, filename)
	suspend fun decode(file: VfsFile) = this.read(file.readAsSyncStream(), file.baseName)
	//fun decode(file: File) = this.read(file.openSync("r"), file.name)
	//fun decode(s: ByteArray, filename: String = "unknown"): Bitmap = read(s.openSync(), filename)

	fun encode(frames: List<ImageFrame>, props: ImageEncodingProps = ImageEncodingProps("unknown")): ByteArray =
		MemorySyncStreamToByteArray(frames.area * 4) { writeImage(ImageData(frames), this, props) }

	fun encode(image: ImageData, props: ImageEncodingProps = ImageEncodingProps("unknown")): ByteArray =
		MemorySyncStreamToByteArray(image.area * 4) { writeImage(image, this, props) }

	fun encode(bitmap: Bitmap, props: ImageEncodingProps = ImageEncodingProps("unknown")): ByteArray =
		encode(listOf(ImageFrame(bitmap)), props)

	suspend fun read(file: VfsFile, props: ImageDecodingProps = ImageDecodingProps()) =
		this.readImage(file.readAll().openSync(), props.copy(filename = file.baseName))

	override fun toString(): String = "ImageFormat($extensions)"
}

data class ImageDecodingProps(
    val filename: String = "unknown",
    val width: Int? = null,
    val height: Int? = null,
    val premultiplied: Boolean = true,
    // Requested but not enforced. Max width and max height
    val requestedMaxSize: Int? = null,
    override var extra: ExtraType = null
) : Extra {

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
        val DEFAULT_PREMULT = ImageDecodingProps(premultiplied = true)
        val DEFAULT = ImageDecodingProps(premultiplied = false)
        fun DEFAULT(premultiplied: Boolean) = if (premultiplied) DEFAULT_PREMULT else DEFAULT
    }
}

data class ImageEncodingProps(
    val filename: String = "",
    val quality: Double = 0.81,
    override var extra: ExtraType = null
) : Extra

