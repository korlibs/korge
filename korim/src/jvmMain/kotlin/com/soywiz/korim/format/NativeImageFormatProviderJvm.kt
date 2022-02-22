package com.soywiz.korim.format

import com.soywiz.korim.awt.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.font.Font
import com.soywiz.korim.font.FontMetrics
import com.soywiz.korim.font.GlyphMetrics
import com.soywiz.korim.font.SystemFont
import com.soywiz.korim.internal.*
import com.soywiz.korim.vector.GraphicsPath
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.file.std.*
import java.awt.*
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.awt.geom.PathIterator
import java.awt.image.*
import java.io.*
import kotlin.math.*

actual val nativeImageFormatProvider: NativeImageFormatProvider = AwtNativeImageFormatProvider

object AwtNativeImageFormatProvider : NativeImageFormatProvider() {
	init {
		// Try to detect junit and run then in headless mode
		if (Thread.currentThread()!!.stackTrace!!.contentDeepToString().contains("org.junit")) {
			System.setProperty("java.awt.headless", "true")
		}
	}

    override suspend fun decodeInternal(data: ByteArray, props: ImageDecodingProps): NativeImageResult {
        return AwtNativeImage(awtReadImageInWorker(data, props.premultiplied)).result()
    }

    override suspend fun decodeInternal(vfs: Vfs, path: String, props: ImageDecodingProps): NativeImageResult = when (vfs) {
        is LocalVfs -> AwtNativeImage(awtReadImageInWorker(File(path), props.premultiplied))
        else -> AwtNativeImage(awtReadImageInWorker(vfs[path].readAll(), props.premultiplied))
    }.result()

	override fun create(width: Int, height: Int, premultiplied: Boolean?): NativeImage =
		AwtNativeImage(BufferedImage(max(width, 1), max(height, 1), if (premultiplied == false) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_ARGB_PRE))

	override fun copy(bmp: Bitmap): NativeImage = AwtNativeImage(bmp.toAwt())
	override suspend fun display(bitmap: Bitmap, kind: Int): Unit = awtShowImageAndWait(bitmap)

    //override fun mipmap(bmp: Bitmap, levels: Int): NativeImage = (bmp.ensureNative() as AwtNativeImage).awtImage.getScaledInstance(bmp.width / (1 shl levels), bmp.height / (1 shl levels), Image.SCALE_SMOOTH).toBufferedImage(false).toAwtNativeImage()
}
