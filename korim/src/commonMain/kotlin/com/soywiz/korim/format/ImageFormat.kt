package com.soywiz.korim.format

import com.soywiz.kds.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*

abstract class ImageFormat(vararg exts: String) {
	val extensions = exts.map { it.toLowerCase().trim() }.toSet()
	open fun readImage(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps()): ImageData = TODO()
	open fun writeImage(
		image: ImageData,
		s: SyncStream,
		props: ImageEncodingProps = ImageEncodingProps("unknown")
	): Unit = throw UnsupportedOperationException()

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
    override var extra: LinkedHashMap<String, Any?>? = null
) : Extra

data class ImageEncodingProps(
    val filename: String = "",
    val quality: Double = 0.81,
    override var extra: LinkedHashMap<String, Any?>? = null
) : Extra

