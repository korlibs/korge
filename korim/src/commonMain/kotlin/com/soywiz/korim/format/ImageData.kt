package com.soywiz.korim.format

import com.soywiz.kds.*
import com.soywiz.korim.atlas.*
import com.soywiz.korim.bitmap.*

open class ImageData(
    val frames: List<ImageFrame>,
    val loopCount: Int = 0,
    val width: Int = frames.firstOrNull()?.width ?: 1,
    val height: Int = frames.firstOrNull()?.height ?: 1
) : Extra by Extra.Mixin() {
    val area: Int get() = frames.area
    val framesByName = frames.associateBy { it.name }
    val framesSortedByProority = frames.sortedByDescending {
        if (it.main) {
            Int.MAX_VALUE
        } else {
            it.bitmap.width * it.bitmap.height * (it.bitmap.bpp * it.bitmap.bpp)
        }
    }

    val mainBitmap: Bitmap get() = framesSortedByProority.firstOrNull()?.bitmap
        ?: throw IllegalArgumentException("No bitmap found")

    override fun toString(): String = "ImageData($frames)"
}

fun ImageData.toAtlas(): AtlasPacker.Result<BmpSlice> {
    val slices = frames.filter { it.includeInAtlas }.map { it.slice }
    return AtlasPacker.pack(slices)
}
