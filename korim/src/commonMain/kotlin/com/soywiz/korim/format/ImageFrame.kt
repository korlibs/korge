package com.soywiz.korim.format

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korim.bitmap.*

open class ImageFrame(
	val bitmap: Bitmap,
	val time: TimeSpan = 0.seconds,
	val targetX: Int = 0,
	val targetY: Int = 0,
	val main: Boolean = true
) : Extra by Extra.Mixin() {
    val duration get() = time
    val width get() = bitmap.width
    val height get() = bitmap.height
	val area: Int get() = bitmap.area

	override fun toString(): String = "ImageFrame($bitmap, time=$time, targetX=$targetX, targetY=$targetY, main=$main)"
}

val Iterable<ImageFrame>.area: Int get() = this.sumBy { it.area }
