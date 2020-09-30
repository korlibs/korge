package com.soywiz.korge.gradle.util

import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

fun BufferedImage.encodePNG(): ByteArray =
	ByteArrayOutputStream().also { ImageIO.write(this, "png", it) }.toByteArray()

fun Image.toBufferedImage(): BufferedImage {
	if (this is BufferedImage && this.type == BufferedImage.TYPE_INT_ARGB) return this
	val bimage = BufferedImage(this.getWidth(null), this.getHeight(null), BufferedImage.TYPE_INT_ARGB)
	val bGr = bimage.createGraphics()
	bGr.drawImage(this, 0, 0, null)
	bGr.dispose()
	return bimage
}

fun Image.getScaledInstance(width: Int, height: Int) = getScaledInstance(width, height, Image.SCALE_SMOOTH)

fun ByteArray.decodeImage() = ImageIO.read(this.inputStream()).toBufferedImage()

val BufferedImage.area get() = width * height
