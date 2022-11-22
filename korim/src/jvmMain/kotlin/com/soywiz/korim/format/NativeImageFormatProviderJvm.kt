package com.soywiz.korim.format

import com.soywiz.korim.awt.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import java.awt.image.*
import java.io.*
import javax.imageio.ImageIO.*
import kotlin.math.*

actual val nativeImageFormatProvider: NativeImageFormatProvider = AwtNativeImageFormatProvider

object AwtNativeImageFormatProvider : NativeImageFormatProvider() {
	init {
		// Try to detect junit and run then in headless mode
		if (Thread.currentThread().stackTrace.contentDeepToString().contains("org.junit")) {
			System.setProperty("java.awt.headless", "true")
		}
	}

    override suspend fun decodeInternal(data: ByteArray, props: ImageDecodingProps): NativeImageResult {
        return AwtNativeImage(awtReadImageInWorker(data, props)).result(props)
    }

    override suspend fun decodeInternal(vfs: Vfs, path: String, props: ImageDecodingProps): NativeImageResult = when (vfs) {
        is LocalVfs -> AwtNativeImage(awtReadImageInWorker(File(path), props))
        else -> AwtNativeImage(awtReadImageInWorker(vfs[path].readAll(), props))
    }.result(props)

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
