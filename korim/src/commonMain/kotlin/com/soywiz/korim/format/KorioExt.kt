package com.soywiz.korim.format

import com.soywiz.klogger.Console
import com.soywiz.korim.atlas.MutableAtlas
import com.soywiz.korim.atlas.MutableAtlasUnit
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.BitmapSlice
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.slice
import com.soywiz.korim.vector.SizedDrawable
import com.soywiz.korim.vector.format.readSVG
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.VfsOpenMode
import com.soywiz.korio.file.baseName
import com.soywiz.korio.lang.FileNotFoundException
import com.soywiz.korio.stream.AsyncInputStream
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.openSync
import com.soywiz.korio.stream.readAll
import kotlinx.coroutines.CancellationException

suspend fun ImageFormat.decode(s: VfsFile, props: ImageDecodingProps = ImageDecodingProps()) =
	this.read(s.readAsSyncStream(), props.copy(filename = s.baseName))

suspend fun ImageFormat.decode(s: AsyncStream, filename: String) = this.read(s.readAll(), ImageDecodingProps(filename))
suspend fun ImageFormat.decode(s: AsyncStream, props: ImageDecodingProps = ImageDecodingProps()) =
	this.read(s.readAll(), props)

val nativeImageFormatProviders: List<NativeImageFormatProvider> get() = listOf(nativeImageFormatProvider)

suspend fun displayImage(bmp: Bitmap, kind: Int = 0) = nativeImageFormatProvider.display(bmp, kind)

suspend fun decodeImageBytes(bytes: ByteArray): NativeImage {
    val exceptions = arrayListOf<Throwable>()
	for (nip in nativeImageFormatProviders) {
		try {
			return nip.decode(bytes)
		} catch (e: Throwable) {
            if (e is CancellationException) throw e
            exceptions += e
		}
	}
    for (v in nativeImageFormatProviders) println(v)
    for (v in exceptions) {
        if (v !is FileNotFoundException) {
            v.printStackTrace()
        }
    }
	throw UnsupportedOperationException("No format supported trying to decode ByteArray")
}

suspend fun decodeImageFile(file: VfsFile): NativeImage {
    val exceptions = arrayListOf<Throwable>()
	for (nip in nativeImageFormatProviders) {
		try {
			return nip.decode(file.vfs, file.path)
		} catch (e: Throwable) {
            if (e is CancellationException) throw e
            exceptions += e
		}
	}
    for (v in nativeImageFormatProviders) println(v)
    for (e in exceptions) {
        if (e !is FileNotFoundException) {
            e.printStackTrace()
        }
    }
	throw UnsupportedOperationException("No format supported trying to decode $file")
}

suspend fun VfsFile.readNativeImage(): NativeImage = decodeImageFile(this)

suspend fun AsyncInputStream.readNativeImage(): NativeImage = decodeImageBytes(this.readAll())
suspend fun AsyncInputStream.readImageData(formats: ImageFormat = RegisteredImageFormats, basename: String = "file.bin"): ImageData =
	formats.readImage(this.readAll().openSync(), ImageDecodingProps(basename))

suspend fun AsyncInputStream.readBitmapListNoNative(formats: ImageFormat): List<Bitmap> =
	this.readImageData(formats).frames.map { it.bitmap }

suspend fun VfsFile.readBitmapInfo(
	formats: ImageFormat = RegisteredImageFormats,
	props: ImageDecodingProps = ImageDecodingProps.DEFAULT_PREMULT
): ImageInfo? =
	formats.decodeHeader(this.readAsSyncStream(), props)

suspend fun VfsFile.readImageInfo(formats: ImageFormat = RegisteredImageFormats, props: ImageDecodingProps = ImageDecodingProps()): ImageInfo? =
    openUse(VfsOpenMode.READ) {
        formats.decodeHeaderSuspend(this, props)
    }


suspend fun VfsFile.readImageData(formats: ImageFormat = RegisteredImageFormats, props: ImageDecodingProps = ImageDecodingProps(), atlas: MutableAtlas<Unit>? = null): ImageData =
    readImageDataContainer(formats, props, atlas).default

suspend fun VfsFile.readImageDataContainer(formats: ImageFormat = RegisteredImageFormats, props: ImageDecodingProps = ImageDecodingProps(), atlas: MutableAtlas<Unit>? = null): ImageDataContainer {
    val out = formats.readImageContainer(this.readAsSyncStream(), props.copy(filename = this.baseName))
    return if (atlas != null) out.packInMutableAtlas(atlas) else out
}

suspend fun VfsFile.readBitmapListNoNative(formats: ImageFormat): List<Bitmap> =
	this.readImageData(formats).frames.map { it.bitmap }

suspend fun VfsFile.readBitmapImageData(formats: ImageFormat) = readImageData(formats)

suspend fun AsyncInputStream.readBitmap(basename: String, formats: ImageFormat): Bitmap {
	return readBitmap(formats, ImageDecodingProps(basename))
}

suspend fun AsyncInputStream.readBitmap(
	formats: ImageFormat = RegisteredImageFormats,
	props: ImageDecodingProps = ImageDecodingProps("file.bin")
): Bitmap {
	val bytes = this.readAll()
	return try {
		if (nativeImageLoadingEnabled) decodeImageBytes(bytes) else formats.decode(bytes, props)
	} catch (e: Throwable) {
        if (e is CancellationException) throw e
		formats.decode(bytes, props)
	}
}


suspend fun VfsFile.readBitmapInfo(formats: ImageFormat): ImageInfo? =
	formats.decodeHeader(this.readAsSyncStream())

suspend fun VfsFile.readBitmapOptimized(formats: ImageFormat = RegisteredImageFormats, premultiplied: Boolean? = null, props: ImageDecodingProps = ImageDecodingProps()): Bitmap {
    val rprops = if (premultiplied != null) props.copy(premultiplied = premultiplied) else props
    return try {
        nativeImageFormatProvider.decode(this, rprops)
    } catch (e: Throwable) {
        if (e is CancellationException) throw e
        if (e !is FileNotFoundException) e.printStackTrace()
        this.readBitmap(formats, rprops)
    }
}

suspend fun VfsFile.readBitmap(
    formats: ImageFormat = RegisteredImageFormats,
    props: ImageDecodingProps = ImageDecodingProps()
): Bitmap = when {
    nativeImageLoadingEnabled -> {
        try {
            nativeImageFormatProvider.decode(this, props)
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            if (e is FileNotFoundException) throw e
            Console.error("Couldn't read native image (fallback to non-native decoders): $e")
            formats.decode(this.read(), props.copy(filename = this.baseName))
        }
    }
    else -> formats.decode(this.read(), props.copy(filename = this.baseName))
}

suspend fun VfsFile.readBitmapSlice(premultiplied: Boolean = true, name: String? = null, atlas: MutableAtlasUnit? = null): BitmapSlice<Bitmap> {
    val result = readBitmapOptimized(premultiplied = premultiplied)
    return when {
        atlas != null -> atlas.add(result.toBMP32IfRequired(), Unit, name).slice
        else -> result.slice()
    }
}

fun BmpSlice.toAtlas(atlas: MutableAtlasUnit): BitmapSlice<Bitmap32> = atlas.add(this, Unit).slice
fun List<BmpSlice>.toAtlas(atlas: MutableAtlasUnit): List<BitmapSlice<Bitmap32>> = this.map { it.toAtlas(atlas) }

suspend fun VfsFile.readVectorImage(): SizedDrawable = readSVG()

var nativeImageLoadingEnabled = true

suspend inline fun disableNativeImageLoading(callback: () -> Unit) {
	val oldNativeImageLoadingEnabled = nativeImageLoadingEnabled
	try {
		nativeImageLoadingEnabled = false
		callback()
	} finally {
		nativeImageLoadingEnabled = oldNativeImageLoadingEnabled
	}
}

suspend fun VfsFile.readBitmapNoNative(
	formats: ImageFormat = RegisteredImageFormats,
	props: ImageDecodingProps = ImageDecodingProps()
): Bitmap = formats.readImage(this.readAsSyncStream(), props).mainBitmap

suspend fun VfsFile.readBitmapNoNative(formats: ImageFormat = RegisteredImageFormats): Bitmap =
	formats.decode(this, ImageDecodingProps(this.baseName))

suspend fun VfsFile.writeBitmap(
	bitmap: Bitmap,
	format: ImageFormat,
	props: ImageEncodingProps = ImageEncodingProps()
) {
	this.write(format.encode(bitmap, props.copy(filename = this.baseName)))
}
//suspend fun VfsFile.writeBitmap(bitmap: Bitmap, format: ImageFormat =
// defaultImageFormats, props: ImageEncodingProps = ImageEncodingProps()) {
//	this.write(format.encodeInWorker(bitmap, this.basename, props))
//}
