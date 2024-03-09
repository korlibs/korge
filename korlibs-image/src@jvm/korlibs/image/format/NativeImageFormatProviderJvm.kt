package korlibs.image.format

import korlibs.image.awt.*
import korlibs.image.bitmap.*
import korlibs.io.file.*
import korlibs.io.file.std.*
import korlibs.memory.*
import korlibs.platform.*
import java.awt.*
import java.awt.image.*
import java.io.*
import javax.imageio.*
import javax.imageio.ImageIO.*
import kotlin.math.*


actual val nativeImageFormatProvider: NativeImageFormatProvider by lazy {
    when {
        Platform.isMac -> CoreGraphicsImageFormatProvider // In MacOS native decoder is 2x~5x faster than the one in the JVM
        else -> AwtNativeImageFormatProvider
    }
}

open class AwtNativeImageFormatProvider : NativeImageFormatProvider() {
    companion object : AwtNativeImageFormatProvider()

	init {
		// Try to detect junit and run then in headless mode
		if (Thread.currentThread().stackTrace.contentDeepToString().contains("org.junit") && System.getenv("HEADLESS_TESTS") == "true") {
			System.setProperty("java.awt.headless", "true")
		}
	}

    override suspend fun decodeHeaderInternal(data: ByteArray): ImageInfo {
        return ImageIO.createImageInputStream(data.inputStream()).use { `in` ->
            val readers: Iterator<ImageReader> = getImageReaders(`in`)
            if (readers.hasNext()) {
                val reader: ImageReader = readers.next()
                return try {
                    reader.input = `in`
                    ImageInfo {
                        width = reader.getWidth(0)
                        height = reader.getHeight(0)
                    }
                } finally {
                    reader.dispose()
                }
            }
            error("Can't decode image")
        }
    }

    override suspend fun decodeInternal(data: ByteArray, props: ImageDecodingProps): NativeImageResult {
        return AwtNativeImage(awtReadImageInWorker(data, props)).result(props)
    }

    override suspend fun decodeInternal(vfs: Vfs, path: String, props: ImageDecodingProps): NativeImageResult {
        return when (vfs) {
            is LocalVfs -> AwtNativeImage(awtReadImageInWorker(File(path), props))
            else -> {
                val bytes = vfs[path].readAll()
                val bufferedImage = awtReadImageInWorker(bytes, props)
                AwtNativeImage(bufferedImage)
            }
        }.result(props)
    }

    override suspend fun encodeSuspend(image: ImageDataContainer, props: ImageEncodingProps): ByteArray {
        val imageWriter = getImageWritersByMIMEType(props.mimeType).next()
        return ByteArrayOutputStream().use { bao ->
            createImageOutputStream(bao).use { ios ->
                imageWriter.output = ios
                imageWriter.write(image.default.mainBitmap.toAwt())
            }
            bao.toByteArray()
        }
    }

	override fun create(width: Int, height: Int, premultiplied: Boolean?): NativeImage =
		AwtNativeImage(BufferedImage(max(width, 1), max(height, 1), if (premultiplied == false) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_ARGB_PRE))

	override fun copy(bmp: Bitmap): NativeImage = AwtNativeImage(bmp.toAwt())
	override suspend fun display(bitmap: Bitmap, kind: Int): Unit = awtShowImageAndWait(bitmap)

    //override fun mipmap(bmp: Bitmap, levels: Int): NativeImage = (bmp.ensureNative() as AwtNativeImage).awtImage.getScaledInstance(bmp.width / (1 shl levels), bmp.height / (1 shl levels), Image.SCALE_SMOOTH).toBufferedImage(false).toAwtNativeImage()
}
