package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korio.util.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.lang.ASCII
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.encoding.*
import com.soywiz.krypto.encoding.*

class ImageFormats(formats: Iterable<ImageFormat>) : ImageFormat("") {
    constructor(vararg formats: ImageFormat) : this(formats.toList())

	val formats: Set<ImageFormat> = formats.flatMap { if (it is ImageFormats) it.formats.toList() else listOf(it) }.toSet()

	override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? {
		for (format in formats) return try {
			format.decodeHeader(s.sliceStart(), props) ?: continue
		} catch (e: Throwable) {
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

operator fun ImageFormat.plus(format: ImageFormat) = ImageFormats(this, format)
operator fun ImageFormat.plus(format: Iterable<ImageFormat>) = ImageFormats(listOf(this) + format)

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
