package korlibs.image.format

import korlibs.image.bitmap.*
import korlibs.io.concurrent.atomic.*
import korlibs.io.file.*
import korlibs.io.lang.*
import korlibs.io.lang.ASCII
import korlibs.io.stream.*
import korlibs.crypto.encoding.*
import kotlin.coroutines.cancellation.*

open class ImageFormats(formats: Iterable<ImageFormat>) : ImageFormat("") {
    constructor(vararg formats: ImageFormat) : this(formats.toList())

    @PublishedApi
    internal var _formats: Set<ImageFormat> by KorAtomicRef(formats.listFormats() - this)
	val formats: Set<ImageFormat> get() = _formats

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
) = file.writeBytes(formats.encode(this, props.copy(filename = file.baseName)))

@Suppress("unused")
suspend fun BmpSlice.writeTo(
    file: VfsFile,
    formats: ImageFormat = RegisteredImageFormats,
    props: ImageEncodingProps = ImageEncodingProps()
) = this.extract().writeTo(file, formats, props)

suspend fun Bitmap.encode(formats: ImageFormat = RegisteredImageFormats, props: ImageEncodingProps = ImageEncodingProps()) = formats.encode(this, props)
