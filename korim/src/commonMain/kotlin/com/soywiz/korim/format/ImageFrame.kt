package com.soywiz.korim.format

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*

open class ImageFrame @Deprecated("") constructor(
    val index: Int,
    @Deprecated("")
	val slice: BmpSlice,
	val time: TimeSpan = 0.seconds,
    @Deprecated("")
	val targetX: Int = 0,
    @Deprecated("")
	val targetY: Int = 0,
    @Deprecated("")
	val main: Boolean = true,
    @Deprecated("")
    val includeInAtlas: Boolean = true,
    val layerData: List<ImageFrameLayer> = emptyList()
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
            layerData: List<ImageFrameLayer> = emptyList(),
            index: Int = 0,
        ): ImageFrame {
            return ImageFrame(index, bitmap.slice(name = name), time, targetX, targetY, main, includeInAtlas, layerData)
        }

        operator fun invoke(index: Int, layerData: List<ImageFrameLayer>, time: TimeSpan): ImageFrame {
            val first = layerData.firstOrNull() ?: return ImageFrame(index, Bitmaps.transparent)
            return ImageFrame(index, first.slice, time, first.targetX, first.targetY, first.main, first.includeInAtlas, layerData)
        }
    }

    val duration get() = time
    val width get() = slice.width
    val height get() = slice.height
	val area: Int get() = slice.area
    val bitmap by lazy { slice.extract() }
    val name get() = slice.name

	override fun toString(): String = "ImageFrame($slice, time=$time, targetX=$targetX, targetY=$targetY, main=$main)"
}

val Iterable<ImageFrame>.area: Int get() = this.sumBy { it.area }
