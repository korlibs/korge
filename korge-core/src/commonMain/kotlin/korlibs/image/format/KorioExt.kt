package korlibs.image.format

import korlibs.logger.*
import korlibs.image.atlas.MutableAtlas
import korlibs.image.atlas.MutableAtlasUnit
import korlibs.image.bitmap.*
import korlibs.image.vector.SizedDrawable
import korlibs.image.vector.format.readSVG
import korlibs.io.file.VfsFile
import korlibs.io.file.VfsOpenMode
import korlibs.io.file.baseName
import korlibs.io.lang.FileNotFoundException
import korlibs.io.stream.AsyncInputStream
import korlibs.io.stream.openSync
import korlibs.io.stream.readAll
import kotlinx.coroutines.CancellationException

suspend fun displayImage(bmp: Bitmap, kind: Int = 0) = nativeImageFormatProvider.display(bmp, kind)

// Read bitmaps from files

suspend fun VfsFile.readNativeImage(props: ImageDecodingProps = ImageDecodingProps.DEFAULT): NativeImage =
    readBitmapNative(props) as NativeImage
suspend fun VfsFile.readBitmapNative(props: ImageDecodingProps = ImageDecodingProps.DEFAULT): Bitmap =
    readBitmap(props.copy(tryNativeDecode = true, format = null))
suspend fun VfsFile.readBitmapNoNative(props: ImageDecodingProps = ImageDecodingProps.DEFAULT): Bitmap =
    readBitmap(props.copy(tryNativeDecode = false, format = props.format ?: RegisteredImageFormats))
suspend fun VfsFile.readBitmap(props: ImageDecodingProps = ImageDecodingProps.DEFAULT): Bitmap =
    _readBitmap(file = this, props = props)

// Read bitmaps from AsyncInputStream

suspend fun AsyncInputStream.readNativeImage(props: ImageDecodingProps = ImageDecodingProps.DEFAULT): NativeImage =
    readBitmap(props.copy(tryNativeDecode = true, format = null)) as NativeImage
suspend fun AsyncInputStream.readBitmap(props: ImageDecodingProps = ImageDecodingProps("file.bin")): Bitmap =
    _readBitmap(bytes = this.readAll(), props = props)

// Read extended image data

suspend fun AsyncInputStream.readImageData(props: ImageDecodingProps = ImageDecodingProps.DEFAULT): ImageData =
    props.formatSure.readImage(this.readAll().openSync(), ImageDecodingProps(props.filename))
suspend fun AsyncInputStream.readBitmapListNoNative(props: ImageDecodingProps = ImageDecodingProps.DEFAULT): List<Bitmap> =
	readImageData(props).frames.map { it.bitmap }
suspend fun VfsFile.readBitmapInfo(props: ImageDecodingProps = ImageDecodingProps.DEFAULT): ImageInfo? =
    props.formatSure.decodeHeader(this.readAsSyncStream(), props)
suspend fun VfsFile.readImageInfo(props: ImageDecodingProps = ImageDecodingProps.DEFAULT): ImageInfo? =
    openUse(VfsOpenMode.READ) { props.formatSure.decodeHeaderSuspend(this, props) }
suspend fun VfsFile.readImageData(props: ImageDecodingProps = ImageDecodingProps.DEFAULT, atlas: MutableAtlas<Unit>? = null): ImageData =
    readImageDataContainer(props, atlas).default

suspend fun VfsFile.readImageDataContainer(props: ImageDecodingProps = ImageDecodingProps.DEFAULT, atlas: MutableAtlas<Unit>? = null): ImageDataContainer {
    val out = props.formatSure.readImageContainer(this.readAsSyncStream(), props.copy(filename = this.baseName))
    return if (atlas != null) out.packInMutableAtlas(atlas) else out
}

suspend fun VfsFile.readBitmapListNoNative(props: ImageDecodingProps = ImageDecodingProps.DEFAULT): List<Bitmap> =
	readImageData(props).frames.map { it.bitmap }

suspend fun VfsFile.readBitmapImageData(props: ImageDecodingProps = ImageDecodingProps.DEFAULT): ImageData =
    readImageData(props)

suspend fun VfsFile.readBitmapSlice(
    name: String? = null,
    atlas: MutableAtlasUnit? = null,
    props: ImageDecodingProps = ImageDecodingProps.DEFAULT,
): BmpSlice {
    val result = readBitmap(props = props)
    return when {
        atlas != null -> atlas.add(result.toBMP32IfRequired(), Unit, name).slice
        else -> result.slice()
    }
}

// ImageFormat variants

suspend fun VfsFile.readBitmapListNoNative(format: ImageFormat): List<Bitmap> =
    readBitmapListNoNative(format.toProps())
suspend fun VfsFile.readBitmap(format: ImageFormat): Bitmap =
    readBitmap(format.toProps())
suspend fun VfsFile.readBitmapNative(format: ImageFormat): Bitmap =
    readBitmapNative(format.toProps())
suspend fun VfsFile.readBitmapNoNative(format: ImageFormat): Bitmap =
    readBitmapNoNative(format.toProps())
suspend fun VfsFile.readImageInfo(format: ImageFormat): ImageInfo? =
    readImageInfo(format.toProps())
suspend fun VfsFile.readBitmapInfo(format: ImageFormat): ImageInfo? =
    readBitmapInfo(format.toProps())

// Atlas variants

fun BmpSlice.toAtlas(atlas: MutableAtlasUnit): BmpSlice32 = atlas.add(this, Unit).slice
fun List<BmpSlice>.toAtlas(atlas: MutableAtlasUnit): List<BmpSlice32> = this.map { it.toAtlas(atlas) }

suspend fun VfsFile.readVectorImage(): SizedDrawable = readSVG()

suspend fun VfsFile.writeBitmap(
	bitmap: Bitmap,
	format: ImageFormat,
	props: ImageEncodingProps = ImageEncodingProps()
) {
	this.write(format.encode(bitmap, props.copy(filename = this.baseName)))
}

//////////////////////////

val imageLoadingLogger = Logger("ImageLoading")

private suspend fun _readBitmap(
    file: VfsFile? = null,
    bytes: ByteArray? = null,
    props: ImageDecodingProps = ImageDecodingProps.DEFAULT
): Bitmap {
    val prop = if (file != null) props.copy(filename = file.baseName) else props
    val rformats = when {
        !prop.tryNativeDecode -> listOf(prop.format)
        prop.format == null -> listOf(null)
        prop.preferKotlinDecoder -> listOf(prop.format, null)
        else -> listOf(null, prop.format)
    }
    var lastError: Throwable? = null
    for (format in rformats) {
        try {
            val rformat: ImageFormatDecoder = format ?: nativeImageFormatProvider
            val bmp = when {
                bytes != null -> rformat.decodeSuspend(bytes, prop)
                file != null -> rformat.decode(file, prop)
                else -> TODO()
            }
            return bmp.also {
                if (prop.asumePremultiplied) it.asumePremultiplied()
            }
        } catch (e: Throwable) {
            lastError = e
            when (e) {
                is CancellationException -> throw e
                is FileNotFoundException, is ImageDecoderNotFoundException -> Unit
                else -> {
                    imageLoadingLogger.info { e }
                    //e.printStackTrace()
                }
            }
        }
    }
    throw lastError!!
}
