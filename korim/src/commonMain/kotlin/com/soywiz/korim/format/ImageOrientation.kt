package com.soywiz.korim.format

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korim.atlas.MutableAtlasUnit
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.file.VfsFile
import com.soywiz.korma.geom.ISizeInt
import kotlin.native.concurrent.ThreadLocal

// TL, TR, BR, BL
inline class ImageOrientation(val data: Int) {
    val rotation: Rotation get() = Rotation[data.extract2(0)]
    val flipX: Boolean get() = data.extractBool(2)
    val flipY: Boolean get() = data.extractBool(3)
    constructor(rotation: Rotation = Rotation.R0, flipX: Boolean = false, flipY: Boolean = false) : this(
        0.insert2(rotation.ordinal, 0).insert(flipX, 2).insert(flipY, 3)
    )

    fun flippedX(): ImageOrientation = ImageOrientation(rotation, !flipX, flipY)
    fun flippedY(): ImageOrientation = ImageOrientation(rotation, flipX, !flipY)
    fun rotatedLeft(): ImageOrientation = ImageOrientation(rotation.rotatedLeft(), flipX, flipY)
    fun rotatedRight(): ImageOrientation = ImageOrientation(rotation.rotatedRight(), flipX, flipY)

    val indices: IntArray get() = INDICES[data.extract4(0)]

    enum class Rotation {
        R0, R90, R180, R270;

        fun rotatedLeft(): Rotation = Rotation[(ordinal - 1) umod 4]
        fun rotatedRight(): Rotation = Rotation[(ordinal + 1) umod 4]

        companion object {
            val VALUES = values()
            operator fun get(index: Int): Rotation = VALUES[index umod VALUES.size]
        }
    }

    object Indices {
        const val TL = 0
        const val TR = 1
        const val BR = 2
        const val BL = 3
    }

    companion object {
        private val INDICES = Array(16) {
            val orientation = ImageOrientation(it)
            val out = intArrayOf(0, 1, 2, 3)
            val rotation = orientation.rotation
            val flipX: Boolean = orientation.flipX
            val flipY: Boolean = orientation.flipY
            if (flipX) {
                out.swap(Indices.TL, Indices.TR)
                out.swap(Indices.BL, Indices.BR)
            }
            if (flipY) {
                out.swap(Indices.TL, Indices.BL)
                out.swap(Indices.TR, Indices.BR)
            }
            out.rotateRight(rotation.ordinal)
            return@Array out
        }

        val ORIGINAL = ImageOrientation(Rotation.R0)
        val MIRROR_HORIZONTAL = ImageOrientation(flipX = true)
        val ROTATE_180 = ImageOrientation(Rotation.R180)
        val MIRROR_VERTICAL = ImageOrientation(flipY = true)
        val MIRROR_HORIZONTAL_ROTATE_270 = ImageOrientation(flipX = true, rotation = Rotation.R270)
        val ROTATE_90 = ImageOrientation(rotation = Rotation.R90)
        val MIRROR_HORIZONTAL_ROTATE_90 = ImageOrientation(flipX = true, rotation = Rotation.R90)
        val ROTATE_270 = ImageOrientation(rotation = Rotation.R270)
    }

    val isRotatedDeg90CwOrCcw: Boolean get() = data.extractBool(1) // equivalent to (rotation == Rotation.R90 || rotation == Rotation.R270)

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
    return when (orientation.rotation) {
        ImageOrientation.Rotation.R0 -> result
        ImageOrientation.Rotation.R90 -> result.rotatedRight()
        ImageOrientation.Rotation.R180 -> result.rotatedRight().rotatedRight()
        ImageOrientation.Rotation.R270 -> result.rotatedRight().rotatedRight().rotatedRight()
    }
}

suspend fun VfsFile.readBitmapSliceWithOrientation(props: ImageDecodingProps = ImageDecodingProps.DEFAULT, name: String? = null, atlas: MutableAtlasUnit? = null): BitmapCoords {
    // @TODO: Support other formats providing orientation information in addition to EXIF?
    val result = kotlin.runCatching { EXIF.readExifFromJpeg(this) }
    val slice = readBitmapSlice(name, atlas, props)
    return if (result.isSuccess) {
        slice.withImageOrientation(result.getOrThrow().orientationSure)
    } else {
        slice
    }
}

@ThreadLocal
var ImageInfo.orientation: ImageOrientation? by Extra.Property { null }
val ImageInfo?.orientationSure: ImageOrientation get() = this?.orientation ?: ImageOrientation.ORIGINAL

