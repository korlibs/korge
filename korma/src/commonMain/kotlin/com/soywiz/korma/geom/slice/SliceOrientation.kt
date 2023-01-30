package com.soywiz.korma.geom.slice

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korma.geom.*

enum class SliceRotation {
    R0, R90, R180, R270;

    val angle: Int = ordinal * 90

    fun rotatedLeft(offset: Int = 1): SliceRotation = SliceRotation[(ordinal - offset) umod 4]
    fun rotatedRight(offset: Int = 1): SliceRotation = SliceRotation[(ordinal + offset) umod 4]
    fun complementary(): SliceRotation = SliceRotation[-ordinal umod 4]
    internal fun _comp2(): SliceRotation = SliceRotation[(-ordinal + 2) umod 4]

    companion object {
        val VALUES = values()
        operator fun get(index: Int): SliceRotation = VALUES[index umod 4]
    }
}

/**
 * Represents an orientation where:
 * [flipX] and [flipY] is applied first, and then [rotation].
 */
inline class SliceOrientation(
    val raw: Int,
) {
    val rotation: SliceRotation get() = SliceRotation[raw.extract2(0)]
    val flipX: Boolean get() = raw.extractBool(2)

    constructor(rotation: SliceRotation = SliceRotation.R0, flipX: Boolean = false) : this(0.insert2(rotation.ordinal, 0).insert(flipX, 2))

    /** Indices represent TL, TR, BR, BL */
    val indices: IntArray get() = INDICES[raw.extract3(0)]

    val isRotatedDeg90CwOrCcw: Boolean get() = rotation == SliceRotation.R90 || rotation == SliceRotation.R270

    // @TODO: Check this
    fun inverted(): SliceOrientation {
        if (flipX && rotation.ordinal % 2 == 1) return SliceOrientation(rotation._comp2(), flipX)
        return SliceOrientation(rotation.complementary(), flipX)
    }

    fun flippedX(): SliceOrientation = SliceOrientation(flipX = !flipX, rotation = rotation.complementary())
    fun flippedY(): SliceOrientation = SliceOrientation(flipX = !flipX, rotation = if (isRotatedDeg90CwOrCcw) rotation else rotation.rotatedRight(2))
    fun rotatedLeft(offset: Int = 1): SliceOrientation = SliceOrientation(rotation.rotatedLeft(offset), flipX)
    fun rotatedRight(offset: Int = 1): SliceOrientation = SliceOrientation(rotation.rotatedRight(offset), flipX)

    fun transformed(orientation: SliceOrientation): SliceOrientation {
        var out = this
        if (orientation.flipX) out = out.flippedX()
        out = out.rotatedRight(orientation.rotation.ordinal)
        return out
    }
    override fun toString(): String = NAMES[raw.extract3(0)]

    fun getX(width: Int, height: Int, x: Int, y: Int): Int {
        val w1 = width - 1
        val h1 = height - 1
        val x = if (flipX) w1 - x else x
        return when (rotation) {
            SliceRotation.R0 -> x
            SliceRotation.R90 -> h1 - y
            SliceRotation.R180 -> w1 - x
            SliceRotation.R270 -> y
        }
    }
    fun getY(width: Int, height: Int, x: Int, y: Int): Int {
        val w1 = width - 1
        val h1 = height - 1
        val x = if (flipX) w1 - x else x
        return when (rotation) {
            SliceRotation.R0 -> y // done
            SliceRotation.R90 -> x
            SliceRotation.R180 -> (h1 - y)
            SliceRotation.R270 -> (w1 - x) // done
        }
    }
    fun getXY(width: Int, height: Int, x: Int, y: Int, out: PointInt = PointInt()): IPointInt =
        out.setTo(getX(width, height, x, y), getY(width, height, x, y))

    object Indices {
        const val TL = 0
        const val TR = 1
        const val BR = 2
        const val BL = 3
    }

    companion object {
        private val INDICES = Array(8) { index ->
            intArrayOf(0, 1, 2, 3).also { out ->
                val orientation = SliceOrientation(index)
                if (orientation.flipX) {
                    out.swap(Indices.TL, Indices.TR)
                    out.swap(Indices.BL, Indices.BR)
                }
                out.rotateRight(orientation.rotation.ordinal)
            }
        }
        private val NAMES = Array(8) { index ->
            val orientation = SliceOrientation(index)

            buildString {
                append(if (orientation.flipX) "MIRROR_HORIZONTAL_ROTATE_" else "ROTATE_")
                append(orientation.rotation.angle)
            }
        }

        val ROTATE_0: SliceOrientation = SliceOrientation(flipX = false, rotation = SliceRotation.R0)
        val ROTATE_90: SliceOrientation = SliceOrientation(flipX = false, rotation = SliceRotation.R90)
        val ROTATE_180: SliceOrientation = SliceOrientation(flipX = false, rotation = SliceRotation.R180)
        val ROTATE_270: SliceOrientation = SliceOrientation(flipX = false, rotation = SliceRotation.R270)
        val MIRROR_HORIZONTAL_ROTATE_0: SliceOrientation = SliceOrientation(flipX = true)
        val MIRROR_HORIZONTAL_ROTATE_90: SliceOrientation = SliceOrientation(flipX = true, rotation = SliceRotation.R90)
        val MIRROR_HORIZONTAL_ROTATE_180: SliceOrientation = SliceOrientation(flipX = true, rotation = SliceRotation.R180)
        val MIRROR_HORIZONTAL_ROTATE_270: SliceOrientation = SliceOrientation(flipX = true, rotation = SliceRotation.R270)

        // Aliases
        inline val NORMAL: SliceOrientation get() = ROTATE_0
        inline val ORIGINAL: SliceOrientation get() = ROTATE_0
        inline val MIRROR_HORIZONTAL: SliceOrientation get() = MIRROR_HORIZONTAL_ROTATE_0
        inline val MIRROR_VERTICAL: SliceOrientation get() = MIRROR_HORIZONTAL_ROTATE_180

        val VALUES: List<SliceOrientation> = listOf(
            ROTATE_0, ROTATE_90, ROTATE_180, ROTATE_270,
            MIRROR_HORIZONTAL_ROTATE_0, MIRROR_HORIZONTAL_ROTATE_90, MIRROR_HORIZONTAL_ROTATE_180, MIRROR_HORIZONTAL_ROTATE_270,
        )
    }
}
