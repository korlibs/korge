package com.soywiz.korge.gradle.targets

import com.soywiz.korge.gradle.KorgeExtension
import com.soywiz.korge.gradle.KorgeGradlePlugin
import com.soywiz.korge.gradle.util.encodePNG
import com.soywiz.korge.gradle.util.getScaledInstance
import com.soywiz.korge.gradle.util.toBufferedImage
import com.soywiz.korio.lang.*
import javax.imageio.ImageIO

val ICON_SIZES = listOf(20, 29, 40, 44, 48, 50, 55, 57, 58, 60, 72, 76, 80, 87, 88, 100, 114, 120, 144, 152, 167, 172, 180, 196, 1024)

fun tryGetResourceBytes(path: String): ByteArray? {
	return KorgeGradlePlugin::class.java.getResource("/" + path.trim('/'))?.readBytes()
}

fun getResourceBytes(path: String): ByteArray = tryGetResourceBytes(path) ?: error("Can't find resource '$path'")
fun getResourceString(path: String): String = getResourceBytes(path).toString(UTF8)

fun KorgeExtension.iconExists() = icon != null && icon!!.exists()

fun KorgeExtension.getIconBytes(): ByteArray {
	return when {
		iconExists() -> icon!!.readBytes()
		else -> getResourceBytes("/icons/korge.png")
	}
}

fun KorgeExtension.getIconBytes(size: Int): ByteArray {
	return ImageIO.read(getIconBytes().inputStream()).getScaledInstance(size, size).toBufferedImage().encodePNG()
}

