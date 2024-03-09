package korlibs.image.format

import korlibs.image.atlas.*
import korlibs.image.bitmap.*
import korlibs.image.vector.*
import korlibs.image.vector.format.*
import korlibs.io.file.*
import korlibs.io.lang.*
import korlibs.io.stream.*
import korlibs.logger.*
import kotlinx.coroutines.*

suspend fun displayImage(bmp: Bitmap, kind: Int = 0) = nativeImageFormatProvider.display(bmp, kind)

// Read bitmaps from files

suspend fun VfsFile.readNativeImage(props: BaseImageDecodingProps = ImageDecodingProps.DEFAULT): NativeImage =
    readBitmapNative(props) as NativeImage
suspend fun VfsFile.readBitmapNative(props: BaseImageDecodingProps = ImageDecodingProps.DEFAULT): Bitmap =
    readBitmap(props.decodingProps.copy(tryNativeDecode = true, format = null))
suspend fun VfsFile.readBitmapNoNative(props: BaseImageDecodingProps = ImageDecodingProps.DEFAULT): Bitmap =
    readBitmap(props.decodingProps.copy(tryNativeDecode = false, format = props.decodingProps.format ?: RegisteredImageFormats))
suspend fun VfsFile.readBitmap(props: BaseImageDecodingProps = ImageDecodingProps.DEFAULT): Bitmap =
    _readBitmap(file = this, props = props.decodingProps)

// Read bitmaps from AsyncInputStream

suspend fun AsyncInputStream.readNativeImage(props: BaseImageDecodingProps = ImageDecodingProps.DEFAULT): NativeImage =
    readBitmap(props.decodingProps.copy(tryNativeDecode = true, format = null)) as NativeImage
suspend fun AsyncInputStream.readBitmap(props: BaseImageDecodingProps = ImageDecodingProps("file.bin")): Bitmap =
    _readBitmap(bytes = this.readAll(), props = props.decodingProps)

// Read extended image data

suspend fun AsyncInputStream.readImageData(props: BaseImageDecodingProps = ImageDecodingProps.DEFAULT): ImageData =
    props.decodingProps.formatSure.readImage(this.readAll().openSync(), ImageDecodingProps(props.decodingProps.filename))
suspend fun AsyncInputStream.readBitmapListNoNative(props: ImageDecodingProps = ImageDecodingProps.DEFAULT): List<Bitmap> =
	readImageData(props).frames.map { it.bitmap }
suspend fun VfsFile.readBitmapInfo(props: BaseImageDecodingProps = ImageDecodingProps.DEFAULT): ImageInfo? =
    props.decodingProps.formatSure.decodeHeader(this.readAsSyncStream(), props.decodingProps)
suspend fun VfsFile.readImageInfo(props: BaseImageDecodingProps = ImageDecodingProps.DEFAULT): ImageInfo? =
    openUse(VfsOpenMode.READ) { props.decodingProps.formatSure.decodeHeaderSuspend(this, props.decodingProps) }
suspend fun VfsFile.readImageData(props: BaseImageDecodingProps = ImageDecodingProps.DEFAULT, atlas: MutableAtlas<Unit>? = null): ImageData =
    readImageDataContainer(props, atlas).default

suspend fun VfsFile.readImageDataContainer(props: BaseImageDecodingProps = ImageDecodingProps.DEFAULT, atlas: MutableAtlas<Unit>? = null): ImageDataContainer {
    val out = props.decodingProps.formatSure.readImageContainer(this.readAsSyncStream(), props.decodingProps.withFile(this))
    return if (atlas != null) out.packInMutableAtlas(atlas) else out
}

suspend fun VfsFile.readBitmapListNoNative(props: BaseImageDecodingProps = ImageDecodingProps.DEFAULT): List<Bitmap> =
	readImageData(props).frames.map { it.bitmap }

suspend fun VfsFile.readBitmapImageData(props: BaseImageDecodingProps = ImageDecodingProps.DEFAULT): ImageData =
    readImageData(props)

suspend fun VfsFile.readBitmapSlice(
    name: String? = null,
    atlas: MutableAtlasUnit? = null,
    props: BaseImageDecodingProps = ImageDecodingProps.DEFAULT,
): BmpSlice = readBitmapSlice(props, name, atlas)

suspend fun VfsFile.readBitmapSlice(
    bprops: BaseImageDecodingProps,
    name: String? = null,
    atlas: MutableAtlasUnit? = null,
): BmpSlice {
    val result = readBitmap(props = bprops)
    return when {
        atlas != null -> atlas.add(result.toBMP32IfRequired(), Unit, name).slice
        else -> result.slice(name = name)
    }
}

// Atlas variants

fun BmpSlice.toAtlas(atlas: MutableAtlasUnit): BmpSlice32 = atlas.add(this, Unit).slice
fun List<BmpSlice>.toAtlas(atlas: MutableAtlasUnit): List<BmpSlice32> = this.map { it.toAtlas(atlas) }

suspend fun VfsFile.readVectorImage(): SizedDrawable = readSVG()

suspend fun VfsFile.writeBitmap(
	bitmap: Bitmap,
	format: ImageFormat,
	props: ImageEncodingProps = ImageEncodingProps()
) {
	this.write(format.encode(bitmap, props.withFile(this)))
}

//////////////////////////

val imageLoadingLogger = Logger("ImageLoading")

private suspend fun _readBitmap(
    file: VfsFile? = null,
    bytes: ByteArray? = null,
    props: ImageDecodingProps = ImageDecodingProps.DEFAULT
): Bitmap {
    val prop = if (file != null) props.withFile(file) else props
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
