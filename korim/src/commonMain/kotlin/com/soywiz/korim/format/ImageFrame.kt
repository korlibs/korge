package com.soywiz.korim.format

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korim.bitmap.*

open class ImageFrame(
    val index: Int,
    val time: TimeSpan = 0.seconds,
    val layerData: List<ImageFrameLayer> = emptyList(),
) : Extra by Extra.Mixin() {
    companion object {
        @Deprecated("")
        operator fun invoke(
            bitmap: Bitmap,
            time: TimeSpan = 0.seconds,
            targetX: Int = 0,
            targetY: Int = 0,
            main: Boolean = true,
            includeInAtlas: Boolean = true,
            name: String? = null,
            index: Int = 0,
        ): ImageFrame = ImageFrame(index, time, fastArrayListOf(ImageFrameLayer(
            ImageLayer(0, null),
            bitmap.slice(name = name),
            targetX,
            targetY,
            main,
            includeInAtlas,
        )))
    }

    val first = layerData.firstOrNull()

    val slice: BmpSlice get() = first?.slice ?: Bitmaps.transparent
    val targetX: Int get() = first?.targetX ?: 0
    val targetY: Int get() = first?.targetY ?: 0
    val main: Boolean get() = first?.main ?: false
    val includeInAtlas: Boolean get() = first?.includeInAtlas ?: true

    val duration get() = time
    val width get() = slice.width
    val height get() = slice.height
	val area: Int get() = slice.area
    val bitmap get() = first?.bitmap ?: Bitmaps.transparent.bmp
    val name get() = slice.name

	override fun toString(): String = "ImageFrame($slice, time=$time, targetX=$targetX, targetY=$targetY, main=$main)"
}

val Iterable<ImageFrame>.area: Int get() = this.sumBy { it.area }
