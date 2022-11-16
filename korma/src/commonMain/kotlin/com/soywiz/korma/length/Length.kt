package com.soywiz.korma.length

import com.soywiz.kmem.*
import kotlin.math.*

//sealed class Length : Comparable<Length> {

// http://www.w3schools.com/cssref/css_units.asp

sealed class Length {
    abstract class Fixed() : Length()
    abstract class Variable() : Length()

    interface LengthContext {
        val fontSize: Double get() = 16.0
        val viewportWidth: Double get() = 640.0
        val viewportHeight: Double get() = 480.0
        val size: Int get() = 100
        val pixelsPerInch: Double get() = 96.0

        val viewportWidth1pc: Double get() = viewportWidth * 0.01
        val viewportHeight1pc: Double get() = viewportHeight * 0.01
        val pixelRatio: Double get() = pixelsPerInch / 96.0
    }

    open class Context : LengthContext, LengthExtensions {
        override var fontSize: Double = 16.0
        override var viewportWidth: Double = 640.0
        override var viewportHeight: Double = 480.0
        override var size: Int = 100
        override var pixelsPerInch: Double = 96.0

        fun setSize(v: Int) = this.apply {
            this.size = v
        }

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
        override fun calc(ctx: LengthContext): Int = (v * ctx.pixelsPerInch * 0.0393701).toInt()
        override fun toString() = "${v}mm"
    }

    data class CM(val v: Double) : Fixed() {
        override fun calc(ctx: LengthContext): Int = (v * ctx.pixelsPerInch * 0.393701).toInt()
        override fun toString() = "${v}cm"
    }

    data class INCH(val v: Double) : Fixed() {
        override fun calc(ctx: LengthContext): Int = (v * ctx.pixelsPerInch).toInt()
        override fun toString() = "${v}inch"
    }

    //data class PX(val v: Double) : Fixed() {
    //	override fun calc(ctx: Context): Int = v
    //	override fun toString() = "${v}px"
    //}

    data class PT(val v: Double) : Fixed() {
        override fun calc(ctx: LengthContext): Int = (v * ctx.pixelRatio).toInt()
        override fun toString() = "${v}pt"
    }

    data class EM(val v: Double) : Fixed() {
        override fun calc(ctx: LengthContext): Int = (v * ctx.fontSize).toInt()
        override fun toString() = "${v}em"
    }

    data class VW(val v: Double) : Fixed() {
        override fun calc(ctx: LengthContext): Int = (v * ctx.viewportWidth1pc).toInt()
        override fun toString() = "${v}em"
    }

    data class VH(val v: Double) : Fixed() {
        override fun calc(ctx: LengthContext): Int = (v * ctx.viewportHeight1pc).toInt()
        override fun toString() = "${v}em"
    }

    data class VMIN(val v: Double) : Fixed() {
        override fun calc(ctx: LengthContext): Int = (v * min(ctx.viewportWidth1pc, ctx.viewportHeight1pc)).toInt()
        override fun toString() = "${v}em"
    }

    data class VMAX(val v: Double) : Fixed() {
        override fun calc(ctx: LengthContext): Int = (v * max(ctx.viewportWidth1pc, ctx.viewportHeight1pc)).toInt()
        override fun toString() = "${v}em"
    }

    data class Ratio(val ratio: Double) : Variable() {
        override fun calc(ctx: LengthContext): Int = (ratio * ctx.size).toInt()
        override fun toString() = "${ratio * 100}%"
    }

    data class Binop(val a: Length, val b: Length, val op: String, val act: (Int, Int) -> Int) : Length() {
        override fun calc(ctx: LengthContext): Int = act(a.calc(ctx), b.calc(ctx))
        override fun toString() = "($a $op $b)"
    }

    data class Scale(val a: Length?, val scale: Double) : Length() {
        override fun calc(ctx: LengthContext): Int = (a.calcMax(ctx) * scale).toInt()
        override fun toString() = "($a * $scale)"
    }

    data class Max(val a: Length, val b: Length) : Length() {
        override fun calc(ctx: LengthContext): Int = max(a.calc(ctx), b.calc(ctx))
    }

    data class Min(val a: Length, val b: Length) : Length() {
        override fun calc(ctx: LengthContext): Int = min(a.calc(ctx), b.calc(ctx))
    }

    abstract fun calc(ctx: LengthContext): Int

    companion object {
        val ZERO = PT(0.0)

        fun calc(
            ctx: LengthContext,
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
    operator fun div(that: Double): Length = Length.Scale(this, 1.0 / that)
    operator fun div(that: Int): Length = Length.Scale(this, 1.0 / that.toDouble())
}


//fun Length?.calc(size: Int, default: Int): Int = this?.calc(size) ?: default

fun Length?.calcMin(ctx: Length.LengthContext, default: Int = 0): Int = this?.calc(ctx) ?: default
fun Length?.calcMax(ctx: Length.LengthContext, default: Int = ctx.size): Int = this?.calc(ctx) ?: default

//operator fun Length?.plus(that: Length?): Length? = Length.Binop(this, that, "+") { a, b -> a + b }
//operator fun Length?.minus(that: Length?): Length? = Length.Binop(this, that, "-") { a, b -> a - b }
operator fun Length?.times(that: Double): Length? = Length.Scale(this, that)
operator fun Length?.div(that: Double): Length? = Length.Scale(this, 1.0 / that)

interface LengthExtensions {
    companion object : LengthExtensions

    fun max(a: Length, b: Length): Length = Length.Max(a, b)
    fun min(a: Length, b: Length): Length = Length.Min(a, b)

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
}

data class PaddingLength(var top: Length? = Length.ZERO, var right: Length? = Length.ZERO, var bottom: Length? = Length.ZERO, var left: Length? = Length.ZERO) {
    constructor(vertical: Length?, horizontal: Length?) : this(vertical, horizontal, vertical, horizontal)
    constructor(pad: Length?) : this(pad, pad, pad, pad)

    fun setTo(top: Length?, right: Length?, bottom: Length?, left: Length?) = this.apply {
        this.top = top
        this.right = right
        this.bottom = bottom
        this.left = left
    }

    fun setTo(vertical: Length?, horizontal: Length?) = setTo(vertical, horizontal, vertical, horizontal)
    fun setTo(pad: Length?) = setTo(pad, pad, pad, pad)
    fun setTo(pad: PaddingLength) = setTo(pad.top, pad.right, pad.bottom, pad.left)
}

data class PositionLength(var x: Length?, var y: Length?)

data class SizeLength(var width: Length?, var height: Length?) {
    constructor(side: Length?) : this(side, side)

    fun copyFrom(other: SizeLength) = setTo(other.width, other.height)

    fun setTo(width: Length?, height: Length?) = this.apply {
        this.width = width
        this.height = height
    }

    fun setToScale(sX: Double, sY: Double = sX) = this.apply {
        this.setTo(this.width * sX, this.height * sY)
    }
}
