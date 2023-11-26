package korlibs.image.format

import korlibs.encoding.*
import korlibs.image.bitmap.*
import korlibs.io.concurrent.atomic.*
import korlibs.io.file.*
import korlibs.io.lang.*
import korlibs.io.lang.ASCII
import korlibs.io.stream.*
import kotlin.coroutines.cancellation.*

open class ImageFormats(formats: Iterable<ImageFormat>) : ImageFormat("") {
    constructor(vararg formats: ImageFormat) : this(formats.toList())

    @PublishedApi
    internal var _formats: Set<ImageFormat> by KorAtomicRef(formats.listFormats() - this)
	val formats: Set<ImageFormat> get() = _formats

    fun formatByExtOrNull(ext: String): ImageFormat? = formats.firstOrNull { ext in it.extensions }

    fun formatByExt(ext: String): ImageFormat {
        return formatByExtOrNull(ext)
            ?: throw UnsupportedOperationException("Don't know how to generate file for extension '$ext' (supported extensions ${formats.flatMap { it.extensions }})")
    }

    override fun toString(): String = "ImageFormats(${formats.size})$formats"

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
        if (formats.isEmpty()) return null
        //println("ImageFormats.decodeHeader:" + formats.size + ": " + formats)
		for (format in formats) return try {
            format.decodeHeader(s.sliceStart(), props) ?: continue
		} catch (e: Throwable) {
            //e.printStackTrace()
            if (e is CancellationException) throw e
			continue
		}
		return null
	}

    private inline fun <T> readImageTyped(s: SyncStream, props: ImageDecodingProps, block: (format: ImageFormat, s: SyncStream, props: ImageDecodingProps) -> T): T {
        //val format = formats.firstOrNull { it.check(s.sliceStart(), props) }
        //println("--------------")
        //println("FORMATS: $formats, props=$props")
        for (format in formats) {
            if (format.check(s.sliceStart(), props)) {
                //println("FORMAT CHECK: $format")
                return block(format, s.sliceStart(), props)
            }
        }
        //if (format != null) return format.readImage(s.sliceStart(), props)
        throw UnsupportedOperationException(
            "No suitable image format : MAGIC:" + s.sliceStart().readString(4, ASCII) +
                "(" + s.sliceStart().readBytes(4).hex + ") (" + s.sliceStart().readBytes(4).toString(ASCII) + ")"
        )
    }

	override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
        return readImageTyped(s, props) { format, s, props ->
            format.readImage(s.sliceStart(), props)
        }
	}

	override fun writeImage(image: ImageData, s: SyncStream, props: ImageEncodingProps) {
		//println("filename: $filename")
        formatByExt(PathInfo(props.filename).extensionLC).writeImage(image, s, props)
	}

    override suspend fun encodeSuspend(image: ImageDataContainer, props: ImageEncodingProps): ByteArray {
        return formatByExt(props.filename.pathInfo.extensionLC).encodeSuspend(image, props)
    }

    override fun readImageContainer(s: SyncStream, props: ImageDecodingProps): ImageDataContainer {
        return readImageTyped(s, props) { format, s, props ->
            format.readImageContainer(s.sliceStart(), props)
        }
    }

    override suspend fun decodeSuspend(data: ByteArray, props: ImageDecodingProps): Bitmap {
        return readImageTyped(data.openSync(), props) { format, s, props ->
            format.decodeSuspend(data, props)
        }
    }

    override suspend fun decode(file: VfsFile, props: ImageDecodingProps): Bitmap {
        return formatByExt(file.extensionLC).decode(file, props)
    }
}


fun Array<out ImageFormat>.listFormats(): Set<ImageFormat> = this.toList().listFormats()
fun Iterable<ImageFormat>.listFormats(): Set<ImageFormat> =
    flatMap { it.listFormats() }.toMutableSet()

fun ImageFormat.listFormats(): Set<ImageFormat> = when (this) {
    is ImageFormats -> this.formats
    else -> setOf(this)
}

operator fun ImageFormat.plus(format: ImageFormat): ImageFormat {
    if (this == format) return this
    if (format is ImageFormats && format.formats.isEmpty()) return this
    if (this is ImageFormats && this.formats.isEmpty()) return format
    return ImageFormats((this.listFormats() + format.listFormats()).distinct())
}
operator fun ImageFormat.plus(formats: List<ImageFormat>): ImageFormat {
    if (formats.isEmpty()) return this
    if (formats.size == 1) return this + formats.first()
    return this + ImageFormats(formats.listFormats())
}

@Suppress("unused")
suspend fun Bitmap.writeTo(
	file: VfsFile,
	formats: ImageFormat = RegisteredImageFormats,
	props: ImageEncodingProps = ImageEncodingProps()
) = file.writeBytes(formats.encode(this, props.withFile(file)))

@Suppress("unused")
suspend fun BmpSlice.writeTo(
    file: VfsFile,
    formats: ImageFormat = RegisteredImageFormats,
    props: ImageEncodingProps = ImageEncodingProps()
) = this.extract().writeTo(file, formats, props)

suspend fun Bitmap.encode(formats: ImageFormat = RegisteredImageFormats, props: ImageEncodingProps = ImageEncodingProps()) = formats.encode(this, props)
