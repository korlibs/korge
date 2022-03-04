package com.soywiz.korim.format

import com.soywiz.kds.*
import com.soywiz.korim.atlas.MutableAtlasUnit
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.file.VfsFile
import com.soywiz.korma.geom.ISizeInt

data class ImageOrientation(
    val rotation: Rotation = Rotation.R0,
    val flipX: Boolean = false,
    val flipY: Boolean = false,
) {
    enum class Rotation { R0, R90, R180, R270 }

    companion object {
        val ORIGINAL = ImageOrientation()
        val MIRROR_HORIZONTAL = ImageOrientation(flipX = true)
        val ROTATE_180 = ImageOrientation(Rotation.R180)
        val MIRROR_VERTICAL = ImageOrientation(flipY = true)
        val MIRROR_HORIZONTAL_ROTATE_270 = ImageOrientation(flipX = true, rotation = Rotation.R270)
        val ROTATE_90 = ImageOrientation(rotation = Rotation.R90)
        val MIRROR_HORIZONTAL_ROTATE_90 = ImageOrientation(flipX = true, rotation = Rotation.R90)
        val ROTATE_270 = ImageOrientation(rotation = Rotation.R270)
    }

    /*
    1 = Horizontal (normal)
    2 = Mirror horizontal
    3 = Rotate 180
    4 = Mirror vertical
    5 = Mirror horizontal and rotate 270 CW
    6 = Rotate 90 CW
    7 = Mirror horizontal and rotate 90 CW
    8 = Rotate 270 CW
    */
}

fun <T : ISizeInt> BmpCoordsWithT<T>.withImageOrientation(orientation: ImageOrientation): BmpCoordsWithT<T> {
    var result = this
    if (orientation.flipX) result = result.flippedX()
    if (orientation.flipY) result = result.flippedY()
    when (orientation.rotation) {
        ImageOrientation.Rotation.R0 -> result = result
        ImageOrientation.Rotation.R90 -> result = result.rotatedRight()
        ImageOrientation.Rotation.R180 -> result = result.rotatedRight().rotatedRight()
        ImageOrientation.Rotation.R270 -> result = result.rotatedRight().rotatedRight().rotatedRight()
    }
    return result
}

suspend fun VfsFile.readBitmapSliceWithOrientation(premultiplied: Boolean = true, name: String? = null, atlas: MutableAtlasUnit? = null): BitmapCoords {
    // @TODO: Support other formats providing orientation information in addition to EXIF?
    val result = kotlin.runCatching { EXIF.readExifFromJpeg(this) }
    val slice = readBitmapSlice(premultiplied, name, atlas)
    return if (result.isSuccess) {
        slice.withImageOrientation(result.getOrThrow().orientationSure)
    } else {
        slice
    }
}

var ImageInfo.orientation: ImageOrientation? by Extra.Property { null }
val ImageInfo?.orientationSure: ImageOrientation get() = this?.orientation ?: ImageOrientation.ORIGINAL

