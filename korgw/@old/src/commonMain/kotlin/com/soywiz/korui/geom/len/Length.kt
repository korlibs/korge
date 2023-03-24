package com.soywiz.korui.geom.len

import korlibs.memory.*
import korlibs.io.util.*
import korlibs.math.geom.*
import kotlin.math.*

//sealed class Length : Comparable<Length> {

// http://www.w3schools.com/cssref/css_units.asp

sealed class Length {
	abstract class Fixed() : Length()
	abstract class Variable() : Length()

	class Context {
		var fontSize: Double = 16.0
		var viewportWidth: Double = 640.0
		var viewportHeight: Double = 480.0
		var size: Int = 100
		var pixelsPerInch: Double = 96.0

		fun setSize(v: Int) = this.apply {
			this.size = v
		}

		val viewportWidth1pc: Double get() = viewportWidth * 0.01
		val viewportHeight1pc: Double get() = viewportHeight * 0.01
		val pixelRatio: Double get() = pixelsPerInch / 96.0

		inline fun keep(callback: Context.() -> Unit) {
			val oldFontSize = fontSize
			val oldViewportWidth = viewportWidth
			val oldViewportHeight = viewportHeight
			val oldSize = size
			val oldPixelsPerInch = pixelsPerInch
			try {
				callback()
			} finally {
				fontSize = oldFontSize
				viewportWidth = oldViewportWidth
				viewportHeight = oldViewportHeight
				size = oldSize
				pixelsPerInch = oldPixelsPerInch
			}
		}
	}

	data class MM(val v: Double) : Fixed() {
		override fun calc(ctx: Context): Int = (v * ctx.pixelsPerInch * 0.0393701).toInt()
		override fun toString() = "${v}mm"
	}

	data class CM(val v: Double) : Fixed() {
		override fun calc(ctx: Context): Int = (v * ctx.pixelsPerInch * 0.393701).toInt()
		override fun toString() = "${v}cm"
	}

	data class INCH(val v: Double) : Fixed() {
		override fun calc(ctx: Context): Int = (v * ctx.pixelsPerInch).toInt()
		override fun toString() = "${v}inch"
	}

	//data class PX(val v: Double) : Fixed() {
	//	override fun calc(ctx: Context): Int = v
	//	override fun toString() = "${v}px"
	//}

	data class PT(val v: Double) : Fixed() {
		override fun calc(ctx: Context): Int = (v * ctx.pixelRatio).toInt()
		override fun toString() = "${v}pt"
	}

	data class EM(val v: Double) : Fixed() {
		override fun calc(ctx: Context): Int = (v * ctx.fontSize).toInt()
		override fun toString() = "${v}em"
	}

	data class VW(val v: Double) : Fixed() {
		override fun calc(ctx: Context): Int = (v * ctx.viewportWidth1pc).toInt()
		override fun toString() = "${v}em"
	}

	data class VH(val v: Double) : Fixed() {
		override fun calc(ctx: Context): Int = (v * ctx.viewportHeight1pc).toInt()
		override fun toString() = "${v}em"
	}

	data class VMIN(val v: Double) : Fixed() {
		override fun calc(ctx: Context): Int = (v * min(ctx.viewportWidth1pc, ctx.viewportHeight1pc)).toInt()
		override fun toString() = "${v}em"
	}

	data class VMAX(val v: Double) : Fixed() {
		override fun calc(ctx: Context): Int = (v * max(ctx.viewportWidth1pc, ctx.viewportHeight1pc)).toInt()
		override fun toString() = "${v}em"
	}

	data class Ratio(val ratio: Double) : Variable() {
		override fun calc(ctx: Context): Int = (ratio * ctx.size).toInt()
		override fun toString() = "${ratio * 100}%"
	}

	data class Binop(val a: Length, val b: Length, val op: String, val act: (Int, Int) -> Int) : Length() {
		override fun calc(ctx: Context): Int = act(a.calc(ctx), b.calc(ctx))
		override fun toString() = "($a $op $b)"
	}

	data class Scale(val a: Length?, val scale: Double) : Length() {
		override fun calc(ctx: Context): Int = (a.calcMax(ctx) * scale).toInt()
		override fun toString() = "($a * $scale)"
	}

	abstract fun calc(ctx: Context): Int

	companion object {
		val ZERO = PT(0.0)

		fun calc(
			ctx: Context,
			default: Length,
			size: Length?,
			min: Length? = null,
			max: Length? = null,
			ignoreBounds: Boolean = false
		): Int {
			val sizeCalc = (size ?: default).calc(ctx)
			val minCalc = min.calcMin(ctx, if (ignoreBounds) Int.MIN_VALUE else 0)
			val maxCalc = max.calcMax(ctx, if (ignoreBounds) Int.MAX_VALUE else ctx.size)
			return sizeCalc.clamp(minCalc, maxCalc)
		}
	}

	operator fun plus(that: Length): Length = Length.Binop(this, that, "+") { a, b -> a + b }
	operator fun minus(that: Length): Length = Length.Binop(this, that, "-") { a, b -> a - b }
	operator fun times(that: Double): Length = Length.Scale(this, that)
	operator fun times(that: Int): Length = Length.Scale(this, that.toDouble())
}

object MathEx {
	fun <T : Comparable<T>> min(a: T, b: T): T = if (a.compareTo(b) < 0) a else b
	fun <T : Comparable<T>> max(a: T, b: T): T = if (a.compareTo(b) > 0) a else b
}

//fun Length?.calc(size: Int, default: Int): Int = this?.calc(size) ?: default

fun Length?.calcMin(ctx: Length.Context, default: Int = 0): Int = this?.calc(ctx) ?: default
fun Length?.calcMax(ctx: Length.Context, default: Int = ctx.size): Int = this?.calc(ctx) ?: default

//operator fun Length?.plus(that: Length?): Length? = Length.Binop(this, that, "+") { a, b -> a + b }
//operator fun Length?.minus(that: Length?): Length? = Length.Binop(this, that, "-") { a, b -> a - b }
operator fun Length?.times(that: Double): Length? = Length.Scale(this, that)

fun RectangleInt.setNewTo(
	ctx: Length.Context,
	bounds: RectangleInt,
	x: Length?,
	y: Length?,
	width: Length?,
	height: Length?
) = this.setTo(
	x?.calc(ctx.setSize(bounds.width)) ?: bounds.x,
	y?.calc(ctx.setSize(bounds.height)) ?: bounds.y,
	width?.calc(ctx.setSize(bounds.width)) ?: bounds.width,
	height?.calc(ctx.setSize(bounds.height)) ?: bounds.height
)

fun RectangleInt.setNewBoundsTo(
	ctx: Length.Context,
	bounds: RectangleInt,
	left: Length?,
	top: Length?,
	right: Length?,
	bottom: Length?
) = this.setBoundsTo(
	left?.calc(ctx.setSize(bounds.width)) ?: bounds.left,
	top?.calc(ctx.setSize(bounds.height)) ?: bounds.top,
	right?.calc(ctx.setSize(bounds.width)) ?: bounds.right,
	bottom?.calc(ctx.setSize(bounds.height)) ?: bounds.bottom
)

//val Int.px: Length get() = Length.PX(this.toDouble())
val Int.mm: Length get() = Length.MM(this.toDouble())
val Int.cm: Length get() = Length.CM(this.toDouble())
val Int.inch: Length get() = Length.INCH(this.toDouble())
val Int.pt: Length get() = Length.PT(this.toDouble())
val Int.em: Length get() = Length.EM(this.toDouble())
val Int.vw: Length get() = Length.VW(this.toDouble())
val Int.vh: Length get() = Length.VH(this.toDouble())
val Int.vmin: Length get() = Length.VMIN(this.toDouble())
val Int.vmax: Length get() = Length.VMAX(this.toDouble())
val Int.percent: Length get() = Length.Ratio(this.toDouble() / 100.0)

//val Int.px: Length get() = Length.PX(this)
val Double.mm: Length get() = Length.MM(this)
val Double.cm: Length get() = Length.CM(this)
val Double.inch: Length get() = Length.INCH(this)
val Double.pt: Length get() = Length.PT(this)
val Double.em: Length get() = Length.EM(this)
val Double.vw: Length get() = Length.VW(this)
val Double.vh: Length get() = Length.VH(this)
val Double.vmin: Length get() = Length.VMIN(this)
val Double.vmax: Length get() = Length.VMAX(this)
val Double.percent: Length get() = Length.Ratio(this.toDouble() / 100.0)

val Double.ratio: Length get() = Length.Ratio(this)