package com.soywiz.korge.gradle.targets.apple

import com.soywiz.korge.gradle.util.encodePNG
import com.soywiz.korge.gradle.util.toBufferedImage
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import javax.imageio.ImageIO

object IcnsBuilder {
	fun build(image: ByteArray): ByteArray {
		return build(ImageIO.read(image.inputStream()))
	}

	fun build(image: BufferedImage): ByteArray {
		val scaledImage = image.getScaledInstance(512, 512, Image.SCALE_SMOOTH).toBufferedImage()
		val ic09Bytes = scaledImage.encodePNG()
		return ByteArrayOutputStream().also { baos ->
			DataOutputStream(baos).apply {
				writeBytes("icns")
				writeInt(8 + 8 + ic09Bytes.size)
				writeBytes("ic09")
				writeInt(ic09Bytes.size)
				write(ic09Bytes)
				flush()
			}
		}.toByteArray()
	}

}

