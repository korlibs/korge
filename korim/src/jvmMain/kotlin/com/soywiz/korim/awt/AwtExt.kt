package com.soywiz.korim.awt

import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.ScaleMode
import com.soywiz.korma.geom.Size
import kotlinx.coroutines.*
import java.awt.*
import java.awt.event.*
import java.awt.image.*
import java.io.*
import javax.imageio.*
import javax.swing.*
import kotlin.coroutines.*

fun Bitmap32.toAwt(
	out: BufferedImage = BufferedImage(
		width.coerceAtLeast(1),
		height.coerceAtLeast(1),
		if (this.premultiplied) BufferedImage.TYPE_INT_ARGB_PRE else BufferedImage.TYPE_INT_ARGB
	)
): BufferedImage {
	transferTo(out)
	return out
}

fun Bitmap.toAwt(
	out: BufferedImage = BufferedImage(
		width.coerceAtLeast(1),
		height.coerceAtLeast(1),
		if (this.premultiplied) BufferedImage.TYPE_INT_ARGB_PRE else BufferedImage.TYPE_INT_ARGB
	)
): BufferedImage = this.toBMP32().toAwt(out)

suspend fun awtShowImageAndWait(image: Bitmap): Unit = awtShowImageAndWait(image.toBMP32().toAwt())

suspend fun awtShowImageAndWait(image: BufferedImage): Unit = suspendCancellableCoroutine { c ->
	awtShowImage(image).addWindowListener(object : WindowAdapter() {
		override fun windowClosing(e: WindowEvent) {
			c.resume(Unit)
		}
	})
}

fun awtShowImage(image: BufferedImage): JFrame {
	//println("Showing: $image")
	val frame = object : JFrame("Image (${image.width}x${image.height})") {
        override fun paint(g: Graphics) {
            //super.paint(g)
            paintComponents(g)
        }
    }
    frame.contentPane = object : Container() {
        override fun paint(g: Graphics) {
            (g as? Graphics2D)?.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            (g as? Graphics2D)?.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            (g as? Graphics2D)?.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            val scaleMode = ScaleMode.SHOW_ALL
            val imageSize = Size(image.width, image.height)
            val containerSize = g.clipBounds.toKorma()
            val out = containerSize.place(imageSize, Anchor.MIDDLE_CENTER, scaleMode).toInt()
            g.drawImage(image, out.x, out.y, out.width, out.height, null)
        }
    }
	//val label = JLabel()

    //label.icon = ImageIcon(image)
	//label.setSize(image.width, image.height)
	//frame.add(label, BorderLayout.CENTER)

	//frame.setSize(bitmap.width, bitmap.height)

	frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
    frame.contentPane.minimumSize = Dimension(128, 128)
    frame.contentPane.preferredSize = Dimension(image.width, image.height)
	frame.pack()
	frame.setLocationRelativeTo(null)
	frame.isVisible = true
	return frame
}

// @TODO: Move to KorMA
private fun java.awt.Rectangle.toKorma(): Rectangle = Rectangle(this.x, this.y, this.width, this.height)

// @TODO: Move to KorMA
private fun Rectangle.place(item: Size, anchor: Anchor, scale: ScaleMode, out: Rectangle = Rectangle()): Rectangle {
    val outSize = scale(item, this.size)
    val x = (this.width - outSize.width) * anchor.sx
    val y = (this.height - outSize.height) * anchor.sy
    return out.setTo(x, y, outSize.width, outSize.height)
}

fun awtShowImage(bitmap: Bitmap) = awtShowImage(bitmap.toBMP32().toAwt())

fun awtConvertImage(image: BufferedImage): BufferedImage {
	val out = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB_PRE)
	out.graphics.drawImage(image, 0, 0, null)
	return out
}

fun awtConvertImageIfRequired(image: BufferedImage): BufferedImage =
	if ((image.type == BufferedImage.TYPE_INT_ARGB_PRE) || (image.type == BufferedImage.TYPE_INT_ARGB)) image else awtConvertImage(image)

fun Bitmap32.transferTo(out: BufferedImage): BufferedImage {
	val ints = (out.raster.dataBuffer as DataBufferInt).data
	arraycopy(this.data.ints, 0, ints, 0, this.width * this.height)
	for (n in 0 until area) ints[n] = BGRA.rgbaToBgra(ints[n])
	out.flush()
	return out
}

val BufferedImage.premultiplied: Boolean get() = this.isAlphaPremultiplied

fun BufferedImage.toBMP32(): Bitmap32 = AwtNativeImage(this).toBMP32()

fun ImageIOReadFormat(s: InputStream, type: Int = AWT_INTERNAL_IMAGE_TYPE_PRE): BufferedImage {
	return ImageIO.read(s)?.clone(type = type) ?: error("Can't read image using AWT")
	//return ImageIO.createImageInputStream(s).use { i ->
	//	// Get the reader
	//	val readers = ImageIO.getImageReaders(i)
//
	//	if (!readers.hasNext()) {
	//		throw IllegalArgumentException("No reader for: " + s) // Or simply return null
	//	}
//
	//	val reader = readers.next()
//
	//	try {
	//		// Set input
	//		reader.input = i
//
	//		// Configure the param to use the destination type you want
	//		val param = reader.defaultReadParam
	//		//param.destinationType = ImageTypeSpecifier.createFromBufferedImageType(type)
//
	//		// Finally read the image, using settings from param
	//		reader.read(0, param).clone(type = type)
	//	} finally {
	//		// Dispose reader in finally block to avoid memory leaks
	//		reader.dispose()
	//	}
	//}
}

fun awtReadImage(data: ByteArray): BufferedImage = ImageIOReadFormat(ByteArrayInputStream(data))
suspend fun awtReadImageInWorker(data: ByteArray, premultiplied: Boolean): BufferedImage =
    executeInWorkerJVM {  ImageIOReadFormat(ByteArrayInputStream(data), if (premultiplied) AWT_INTERNAL_IMAGE_TYPE_PRE else AWT_INTERNAL_IMAGE_TYPE) }

suspend fun awtReadImageInWorker(file: File, premultiplied: Boolean): BufferedImage =
    executeInWorkerJVM { FileInputStream(file).use { ImageIOReadFormat(it, if (premultiplied) AWT_INTERNAL_IMAGE_TYPE_PRE else AWT_INTERNAL_IMAGE_TYPE) } }

//var image = ImageIO.read(File("/Users/al/some-picture.jpg"))
