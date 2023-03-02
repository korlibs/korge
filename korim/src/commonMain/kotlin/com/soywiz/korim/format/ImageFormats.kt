package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korio.concurrent.atomic.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.lang.ASCII
import com.soywiz.korio.stream.*
import com.soywiz.krypto.encoding.*
import kotlinx.coroutines.*

open class ImageFormats(formats: Iterable<ImageFormat>) : ImageFormat("") {
    constructor(vararg formats: ImageFormat) : this(formats.toList())

    @PublishedApi
    internal var _formats: Set<ImageFormat> by KorAtomicRef(formats.listFormats())
	val formats: Set<ImageFormat> get() = _formats

    override suspend fun decodeHeaderSuspend(s: AsyncStream, props: ImageDecodingProps): ImageInfo? {
        for (format in formats) return try {
            format.decodeHeaderSuspend(s.sliceStart(), props) ?: continue
        } catch (e: Throwable) {
            imageLoadingLogger.info { e }

            if (e is CancellationException) throw e
            continue
        }
        return null
    }

    override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? {
		for (format in formats) return try {
			format.decodeHeader(s.sliceStart(), props) ?: continue
		} catch (e: Throwable) {
            if (e is CancellationException) throw e
			continue
		}
		return null
	}

	override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
		//val format = formats.firstOrNull { it.check(s.sliceStart(), props) }
		//println("--------------")
		//println("FORMATS: $formats, props=$props")
		for (format in formats) {
			if (format.check(s.sliceStart(), props)) {
				//println("FORMAT CHECK: $format")
				return format.readImage(s.sliceStart(), props)
			}
		}
		//if (format != null) return format.readImage(s.sliceStart(), props)
		throw UnsupportedOperationException(
			"No suitable image format : MAGIC:" + s.sliceStart().readString(4, ASCII) +
					"(" + s.sliceStart().readBytes(4).hex + ") (" + s.sliceStart().readBytes(4).toString(ASCII) + ")"
		)
	}

	override fun writeImage(image: ImageData, s: SyncStream, props: ImageEncodingProps) {
		val ext = PathInfo(props.filename).extensionLC
		//println("filename: $filename")
		val format = formats.firstOrNull { ext in it.extensions }
				?: throw UnsupportedOperationException("Don't know how to generate file for extension '$ext' (supported extensions ${formats.flatMap { it.extensions }}) (props $props)")
		format.writeImage(image, s, props)
	}
}


fun Array<out ImageFormat>.listFormats(): Set<ImageFormat> = this.toList().listFormats()
fun Iterable<ImageFormat>.listFormats(): Set<ImageFormat> =
    flatMap { it.listFormats() }.toMutableSet()

fun ImageFormat.listFormats(): Set<ImageFormat> = when (this) {
    is ImageFormats -> this.formats
    else -> setOf(this)
}

operator fun ImageFormat.plus(format: ImageFormat): ImageFormats = ImageFormats(this.listFormats() + format.listFormats())
operator fun ImageFormat.plus(formats: Iterable<ImageFormat>) = ImageFormats(this.listFormats() + formats.flatMap { it.listFormats() })

@Suppress("unused")
suspend fun Bitmap.writeTo(
	file: VfsFile,
	formats: ImageFormat = RegisteredImageFormats,
	props: ImageEncodingProps = ImageEncodingProps()
) = file.writeBytes(formats.encode(this, props.copy(filename = file.baseName)))

@Suppress("unused")
suspend fun BmpSlice.writeTo(
    file: VfsFile,
    formats: ImageFormat = RegisteredImageFormats,
    props: ImageEncodingProps = ImageEncodingProps()
) = this.extract().writeTo(file, formats, props)

suspend fun Bitmap.encode(formats: ImageFormat = RegisteredImageFormats, props: ImageEncodingProps = ImageEncodingProps()) = formats.encode(this, props)
