package com.soywiz.korim.format

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*

open class ImageFrame(
	val bitmap: Bitmap,
	val time: TimeSpan = 0.seconds,
	val targetX: Int = 0,
	val targetY: Int = 0,
	val main: Boolean = true,
    var name: String? = null,
    val includeInAtlas: Boolean = true,
) : Extra by Extra.Mixin() {
    val duration get() = time
    val width get() = bitmap.width
    val height get() = bitmap.height
	val area: Int get() = bitmap.area
    val slice = bitmap.slice(name = name, bounds = RectangleInt(targetX, targetY, bitmap.width, bitmap.height))

	override fun toString(): String = "ImageFrame(name=$name, $bitmap, time=$time, targetX=$targetX, targetY=$targetY, main=$main)"
}

val Iterable<ImageFrame>.area: Int get() = this.sumBy { it.area }
