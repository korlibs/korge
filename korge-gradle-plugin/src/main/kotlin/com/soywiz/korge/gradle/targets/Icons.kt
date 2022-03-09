package com.soywiz.korge.gradle.targets

import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.util.encodePNG
import com.soywiz.korge.gradle.util.getScaledInstance
import com.soywiz.korge.gradle.util.toBufferedImage
import org.gradle.api.*
import java.io.*
import javax.imageio.ImageIO

val ICON_SIZES = listOf(20, 29, 40, 44, 48, 50, 55, 57, 58, 60, 72, 76, 80, 87, 88, 100, 114, 120, 144, 152, 167, 172, 180, 196, 1024)

fun tryGetResourceBytes(path: String): ByteArray? =
    KorgeGradlePlugin::class.java.getResource("/" + path.trim('/'))?.readBytes()

fun getResourceBytes(path: String): ByteArray = tryGetResourceBytes(path) ?: error("Can't find resource '$path'")
fun getResourceString(path: String): String = getResourceBytes(path).toString(Charsets.UTF_8)

fun KorgeExtension.getIconBytes(): ByteArray = KorgeIconProvider(this).getIconBytes()
fun KorgeExtension.getBannerBytes(): ByteArray = KorgeIconProvider(this).getBannerBytes()

fun KorgeExtension.getIconBytes(width: Int, height: Int = width): ByteArray = ImageIO.read(getIconBytes().inputStream()).getScaledInstance(width, height).toBufferedImage().encodePNG()
fun KorgeExtension.getBannerBytes(width: Int, height: Int = width): ByteArray = ImageIO.read(getBannerBytes().inputStream()).getScaledInstance(width, height).toBufferedImage().encodePNG()

class KorgeIconProvider(val icon: File? = null, val banner: File? = null) {
    constructor(korge: KorgeExtension) : this(korge.icon, korge.banner)
    constructor(project: Project) : this(project.korge)

    fun iconExists() = icon != null && icon!!.exists()
    fun bannerExists() = banner != null && banner!!.exists()

    fun getIconBytes(): ByteArray = when {
        iconExists() -> icon!!.readBytes()
        else -> getResourceBytes("/icons/korge.png")
    }

    fun getBannerBytes(): ByteArray = when {
        bannerExists() -> banner!!.readBytes()
        iconExists() -> icon!!.readBytes()
        else -> getResourceBytes("/banners/korge.png")
    }


    fun getIconBytes(width: Int, height: Int = width): ByteArray =  ImageIO.read(getIconBytes().inputStream()).getScaledInstance(width, height).toBufferedImage().encodePNG()
    fun getBannerBytes(width: Int, height: Int = width): ByteArray =  ImageIO.read(getBannerBytes().inputStream()).getScaledInstance(width, height).toBufferedImage().encodePNG()

}
