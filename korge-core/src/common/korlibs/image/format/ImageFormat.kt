package korlibs.image.format

import korlibs.datastructure.*
import korlibs.image.bitmap.*
import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.io.lang.*
import korlibs.io.stream.*
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

interface ImageFormatDecoder {
    suspend fun decode(file: VfsFile, props: ImageDecodingProps = ImageDecodingProps.DEFAULT): Bitmap
    suspend fun decodeSuspend(data: ByteArray, props: ImageDecodingProps = ImageDecodingProps.DEFAULT): Bitmap
}

interface ImageFormatEncoder {
    suspend fun encodeSuspend(
        image: ImageDataContainer,
        props: ImageEncodingProps = ImageEncodingProps("unknown"),
    ): ByteArray = throw UnsupportedOperationException()
}

suspend fun ImageFormatEncoder.encodeSuspend(
    bitmap: Bitmap,
    props: ImageEncodingProps = ImageEncodingProps("unknown"),
): ByteArray = encodeSuspend(ImageDataContainer(bitmap), props)


interface ImageFormatEncoderDecoder : ImageFormatEncoder, ImageFormatDecoder

abstract class ImageFormat(vararg exts: String) : BaseImageDecodingProps, ImageFormatEncoderDecoder {
    override val decodingProps: ImageDecodingProps by lazy { this.toProps() }
	val extensions = exts.map { it.toLowerCase().trim() }.toSet()
    open fun readImageContainer(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps.DEFAULT): ImageDataContainer = ImageDataContainer(listOf(readImage(s, props)))
	open fun readImage(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps.DEFAULT): ImageData = TODO()
	open fun writeImage(image: ImageData, s: SyncStream, props: ImageEncodingProps): Unit = throw UnsupportedOperationException()

    override suspend fun encodeSuspend(image: ImageDataContainer, props: ImageEncodingProps): ByteArray {
        val out = MemorySyncStream()
        writeImage(image.default, out, props)
        return out.toByteArray()
    }

    suspend fun decodeHeaderSuspend(file: VfsFile, props: ImageDecodingProps = ImageDecodingProps.DEFAULT): ImageInfo? {
        return file.openUse { decodeHeaderSuspend(this@openUse, props.withFile(file)) }
    }

    open suspend fun decodeHeaderSuspend(s: AsyncStream, props: ImageDecodingProps = ImageDecodingProps.DEFAULT): ImageInfo? {
        return decodeHeader(s.toSyncOrNull() ?: s.readAll().openSync(), props)
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
		readImage(s, ImageDecodingProps.DEFAULT.withFileName(filename)).mainBitmap

	suspend fun read(file: VfsFile) = this.read(file.readAsSyncStream(), file.baseName)
	//fun read(file: File) = this.read(file.openSync(), file.name)
	fun read(s: ByteArray, filename: String): Bitmap = read(s.openSync(), filename)

	fun read(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps.DEFAULT): Bitmap = readImage(s, props).mainBitmap
	//fun read(file: File, props: ImageDecodingProps = ImageDecodingProps()) = this.read(file.openSync(), props.copy(filename = file.name))
	fun read(s: ByteArray, props: ImageDecodingProps = ImageDecodingProps.DEFAULT): Bitmap = read(s.openSync(), props)

	fun check(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps.DEFAULT): Boolean =
        runIgnoringExceptions(show = true) { decodeHeader(s, props) != null } ?: false

	fun decode(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps.DEFAULT): Bitmap = this.read(s, props)
	//fun decode(file: File, props: ImageDecodingProps = ImageDecodingProps()) = this.read(file.openSync("r"), props.copy(filename = file.name))

    /** Decodes a given [data] byte array to a bitmap based on the image format with optional extra [prop] properties. */
    fun decode(data: ByteArray, props: ImageDecodingProps = ImageDecodingProps.DEFAULT): Bitmap = read(data.openSync(), props)

	override suspend fun decodeSuspend(data: ByteArray, props: ImageDecodingProps): Bitmap = decode(data, props)

	//fun decode(s: SyncStream, filename: String = "unknown") = this.read(s, filename)
	suspend fun decode(file: VfsFile) = this.read(file.readAsSyncStream(), file.baseName)
	//fun decode(file: File) = this.read(file.openSync("r"), file.name)
	//fun decode(s: ByteArray, filename: String = "unknown"): Bitmap = read(s.openSync(), filename)

    override suspend fun decode(file: VfsFile, props: ImageDecodingProps): Bitmap =
        this.read(file.readAsSyncStream(), props.withFile(file))

    suspend fun decode(s: AsyncStream, filename: String) = this.read(s.readAll(), ImageDecodingProps(filename))
    suspend fun decode(s: AsyncStream, props: ImageDecodingProps = ImageDecodingProps.DEFAULT) =
        this.read(s.readAll(), props)


	fun encode(image: ImageData, props: ImageEncodingProps = ImageEncodingProps("unknown")): ByteArray =
		MemorySyncStreamToByteArray(image.area * 4) { writeImage(image, this, props) }

    fun encode(frames: List<ImageFrame>, props: ImageEncodingProps = ImageEncodingProps("unknown")): ByteArray =
        encode(ImageData(frames), props)

    fun encode(bitmap: Bitmap, props: ImageEncodingProps = ImageEncodingProps("unknown")): ByteArray =
		encode(ImageData(bitmap), props)

	suspend fun read(file: VfsFile, props: ImageDecodingProps = ImageDecodingProps.DEFAULT): ImageData =
		this.readImage(file.readAll().openSync(), props.withFile(file))

	override fun toString(): String = "ImageFormat($extensions)"
}

class ImageDecoderNotFoundException : Exception("Can't read image using AWT. No available decoder for input")

interface BaseImageDecodingProps {
    val decodingProps: ImageDecodingProps
}

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
    /**
     * Provides an `out` parameter to reuse an existing Bitmap to reduce allocations.
     *
     * Note though that not all formats may use the bitmap provided by the `out` param.
     * In those cases, they will return a newly allocated Bitmap instead.
     */
    val out: Bitmap? = null,
    override var extra: ExtraType = null
) : BaseImageDecodingProps, Extra {

    override val decodingProps get() = this

    val premultipliedSure: Boolean get() = premultiplied ?: true
    val formatSure: ImageFormat get() = format ?: RegisteredImageFormats

    fun withFileName(filename: String): ImageDecodingProps = copy(filename = filename)
    fun withFile(file: VfsFile): ImageDecodingProps = withFileName(file.baseName)

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
    override var extra: ExtraType = null,
    val depremultiplyIfRequired: Boolean = true,
    val init: (ImageEncodingProps.() -> Unit)? = null
) : Extra {
    fun withFile(file: VfsFile): ImageEncodingProps = copy(filename = file.baseName)

    val extension: String get() = PathInfo(filename).extensionLC
    val mimeType: String get() = when (extension) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "avif" -> "image/avif"
        "heic" -> "image/heic"
        else -> "image/png"
    }

    init {
        init?.invoke(this)
    }
}
