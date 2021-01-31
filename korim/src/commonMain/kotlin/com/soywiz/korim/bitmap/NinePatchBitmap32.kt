package com.soywiz.korim.bitmap

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korim.internal.*
import com.soywiz.korim.internal.max2
import com.soywiz.korio.file.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.math.*

class NinePatchInfo(
	val xranges: List<Pair<Boolean, IntRange>>,
	val yranges: List<Pair<Boolean, IntRange>>,
	val width: Int,
	val height: Int
) {
	constructor(
		width: Int, height: Int,
		left: Int, top: Int, right: Int, bottom: Int
	) : this(
		listOf(false to (0 until left), true to (left until right), false to (right until width)),
		listOf(false to (0 until top), true to (top until bottom), false to (bottom until height)),
		width, height
	)

	class AxisSegment(val scaled: Boolean, val range: IntRange) {
		val fixed get() = !scaled
		val length get() = range.length

		fun computedLength(axis: AxisInfo, boundsLength: Int): Double {
			val scale = (boundsLength.toDouble() / axis.totalLen.toDouble()).clamp(0.0, 1.0)
			return if (fixed) {
				length.toDouble() * scale
			} else {
				val variableSize = (boundsLength - (axis.fixedLen * scale))
				variableSize.toDouble() * (length.toDouble() / axis.scaledLen.toDouble())
			}
		}
	}

	class AxisInfo(ranges: List<Pair<Boolean, IntRange>>, val totalLen: Int) {
		val segments = ranges.map { AxisSegment(it.first, it.second) }
		val fixedLen = max2(1, segments.filter { it.fixed }.map { it.length }.sum())
		val scaledLen = max2(1, segments.filter { it.scaled }.map { it.length }.sum())
	}

	val xaxis = AxisInfo(xranges, width)
	val yaxis = AxisInfo(yranges, height)

	val xsegments = xaxis.segments
	val ysegments = yaxis.segments

	val fixedWidth = xaxis.fixedLen
	val fixedHeight = yaxis.fixedLen

	val scaledWidth = xaxis.scaledLen
	val scaledHeight = yaxis.scaledLen

	class Segment(val rect: RectangleInt, val x: AxisSegment, val y: AxisSegment) : Extra by Extra.Mixin() {
		val scaleX: Boolean = x.scaled
		val scaleY: Boolean = y.scaled
	}

	val segments = ysegments.map { y ->
		xsegments.map { x ->
			Segment(
				RectangleInt.fromBounds(x.range.start, y.range.start, x.range.endExclusive, y.range.endExclusive),
				x, y
			)
		}
	}

	// Can be reused for textures using AG
	fun computeScale(
		bounds: RectangleInt,
		callback: (segment: Segment, x: Int, y: Int, width: Int, height: Int) -> Unit
	) {
		//println("scaleFixed=($scaleFixedX,$scaleFixedY)")
		var ry = 0
		for ((yindex, y) in ysegments.withIndex()) {
			val segHeight = y.computedLength(this.yaxis, bounds.height).toInt()
			var rx = 0
			for ((xindex, x) in xsegments.withIndex()) {
				val segWidth = x.computedLength(this.xaxis, bounds.width).toInt()

				val seg = segments[yindex][xindex]
				val segLeft = (rx + bounds.left).toInt()
				val segTop = (ry + bounds.top).toInt()

				//println("($x,$y):($segWidth,$segHeight)")
				callback(seg, segLeft, segTop, segWidth.toInt(), segHeight.toInt())

				rx += segWidth
			}
			ry += segHeight
		}
	}

}

class NinePatchBitmap32(val bmp: Bitmap32) {
	val width get() = bmp.width
	val height get() = bmp.height
	val dwidth get() = width.toDouble()
	val dheight get() = height.toDouble()
	val content = bmp.sliceWithBounds(1, 1, bmp.width - 1, bmp.height - 1)

	val info = NinePatchInfo(
		(1 until bmp.width - 1).computeRle { bmp[it, 0].a != 0 },
		(1 until bmp.height - 1).computeRle { bmp[0, it].a != 0 },
		content.width, content.height
	)

	val NinePatchInfo.Segment.bmp by Extra.PropertyThis<NinePatchInfo.Segment, Bitmap32> {
		this@NinePatchBitmap32.content.slice(this.rect).extract()
	}

	fun <T : Bitmap> drawTo(
		other: T,
		bounds: RectangleInt,
		antialiased: Boolean = true,
		drawRegions: Boolean = false
	): T {
		other.context2d(antialiased) {
			info.computeScale(bounds) { seg, segLeft, segTop, segWidth, segHeight ->
				drawImage(seg.bmp, segLeft, segTop, segWidth.toInt(), segHeight.toInt())
				if (drawRegions) {
					stroke(Colors.RED) { rect(segLeft, segTop, segWidth, segHeight) }
				}
			}
		}
		return other
	}

	fun rendered(width: Int, height: Int, antialiased: Boolean = true, drawRegions: Boolean = false): Bitmap32 {
		return drawTo(
			NativeImage(width, height),
			//Bitmap32(width, height),
			RectangleInt(0, 0, width, height),
			antialiased = antialiased,
			drawRegions = drawRegions
		).toBMP32()
	}
}

fun Bitmap.asNinePatch() = NinePatchBitmap32(this.toBMP32())
suspend fun VfsFile.readNinePatch(format: ImageFormat = RegisteredImageFormats) = NinePatchBitmap32(readBitmap(format).toBMP32())

private inline fun <T, R : Any> Iterable<T>.computeRle(callback: (T) -> R): List<Pair<R, IntRange>> {
    var first = true
    var pos = 0
    var startpos = 0
    lateinit var lastRes: R
    val out = arrayListOf<Pair<R, IntRange>>()
    for (it in this) {
        val current = callback(it)
        if (!first) {
            if (current != lastRes) {
                out += lastRes to (startpos until pos)
                startpos = pos
            }
        }
        lastRes = current
        first = false
        pos++
    }
    if (startpos != pos) {
        out += lastRes to (startpos until pos)
    }
    return out
}
