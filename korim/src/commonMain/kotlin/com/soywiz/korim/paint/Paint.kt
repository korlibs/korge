package com.soywiz.korim.paint

import com.soywiz.kds.DoubleArrayList
import com.soywiz.kds.IDoubleArrayList
import com.soywiz.kds.IntArrayList
import com.soywiz.kmem.clamp
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.RgbaArray
import com.soywiz.korim.color.RgbaPremultipliedArray
import com.soywiz.korim.vector.CycleMethod
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.div
import com.soywiz.korma.geom.unaryMinus
import com.soywiz.korma.geom.vector.LineCap
import com.soywiz.korma.geom.vector.LineJoin
import com.soywiz.korma.geom.vector.LineScaleMode
import com.soywiz.korma.geom.vector.StrokeInfo
import com.soywiz.korma.geom.vector.VectorPath
import kotlin.math.sqrt

interface Paint {
    fun clone(): Paint
}

data class Stroke(val paint: Paint, val info: StrokeInfo) {
    constructor(
        paint: Paint,
        thickness: Double = 1.0,
        pixelHinting: Boolean = false,
        scaleMode: LineScaleMode = LineScaleMode.NORMAL,
        startCap: LineCap = LineCap.BUTT,
        endCap: LineCap = LineCap.BUTT,
        join: LineJoin = LineJoin.MITER,
        miterLimit: Double = 20.0,
        dash: IDoubleArrayList? = null,
        dashOffset: Double = 0.0
    ) : this(paint, StrokeInfo(
        thickness = thickness,
        pixelHinting = pixelHinting,
        scaleMode = scaleMode,
        startCap = startCap,
        endCap = endCap,
        join = join,
        miterLimit = miterLimit,
        dash = dash,
        dashOffset = dashOffset,
    ))
}
fun StrokeInfo.withPaint(paint: Paint): Stroke = Stroke(paint, this)

object NonePaint : Paint {
    override fun clone(): Paint = this
}

typealias ColorPaint = RGBA

/**
 * Paints a default color. For BitmapFonts, draw the original Bitmap without tinting.
 */
val DefaultPaint get() = Colors.BLACK

interface TransformedPaint : Paint {
    val transform: Matrix
    val units: GradientUnits
    fun applyMatrix(m: Matrix): TransformedPaint = replaceMatrix(this.transform * m)
    fun replaceMatrix(m: Matrix): TransformedPaint
}

enum class GradientKind {
    LINEAR, RADIAL,
    @Deprecated("Not available in some targets")
    SWEEP;
    companion object {
        @Deprecated("Not available in some targets")
        val CONIC: GradientKind get() = SWEEP
    }
}

enum class GradientUnits {
    USER_SPACE_ON_USE, OBJECT_BOUNDING_BOX
}

enum class GradientInterpolationMethod {
    LINEAR, NORMAL
}

data class GradientPaint(
    val kind: GradientKind,
    val x0: Double,
    val y0: Double,
    val r0: Double,
    val x1: Double,
    val y1: Double,
    val r1: Double,
    val stops: DoubleArrayList = DoubleArrayList(),
    val colors: IntArrayList = IntArrayList(),
    val cycle: CycleMethod = CycleMethod.NO_CYCLE,
    override val transform: Matrix = Matrix(),
    val interpolationMethod: GradientInterpolationMethod = GradientInterpolationMethod.NORMAL,
    override val units: GradientUnits = GradientUnits.OBJECT_BOUNDING_BOX,
    val startAngle: Angle = Angle.ZERO,
) : TransformedPaint {
    val isLinear: Boolean get() = kind == GradientKind.LINEAR
    val isRadial: Boolean get() = kind == GradientKind.RADIAL
    val isSweep: Boolean get() = kind == GradientKind.SWEEP

    @Deprecated("")
    fun x0(m: Matrix) = m.transformX(x0, y0)
    @Deprecated("")
    fun y0(m: Matrix) = m.transformY(x0, y0)
    @Deprecated("")
    fun r0(m: Matrix) = m.transformX(r0, r0)

    @Deprecated("")
    fun x1(m: Matrix) = m.transformX(x1, y1)
    @Deprecated("")
    fun y1(m: Matrix) = m.transformY(x1, y1)
    @Deprecated("")
    fun r1(m: Matrix) = m.transformX(r1, r1)

    val numberOfStops get() = stops.size

    companion object {
        fun identity(kind: GradientKind) = GradientPaint(kind, 0.0, 0.0, 0.0, if (kind == GradientKind.RADIAL) 0.0 else 1.0, 0.0, 1.0, transform = Matrix())

        fun gradientBoxMatrix(width: Double, height: Double, rotation: Angle, tx: Double, ty: Double, out: Matrix = Matrix()): Matrix {
            out.identity()
            out.pretranslate(tx + width / 2, ty + height / 2)
            out.prescale(width / 2, height / 2)
            out.prerotate(rotation)
            return out
        }

        fun fromGradientBox(kind: GradientKind, width: Double, height: Double, rotation: Angle, tx: Double, ty: Double): GradientPaint {
            return identity(kind).copy(transform = gradientBoxMatrix(width, height, rotation, tx, ty))
        }

        fun fillColors(out: RgbaPremultipliedArray, stops: DoubleArrayList, colors: IntArrayList) {
            _fillColors(out.ints, stops, colors, premultiplied = true)
        }

        private fun RGBA.colorInt(premultiplied: Boolean): Int = if (premultiplied) this.premultiplied.value else value

        private fun _fillColors(out: IntArray, stops: DoubleArrayList, colors: IntArrayList, premultiplied: Boolean) {
            val numberOfStops = stops.size
            val NCOLORS = out.size
            fun stopN(n: Int): Int = (stops[n] * NCOLORS).toInt()

            when (numberOfStops) {
                0, 1 -> {
                    val color = if (numberOfStops == 0) Colors.FUCHSIA else RGBA(colors.first())
                    val pcolor: Int = color.colorInt(premultiplied)
                    for (n in 0 until NCOLORS) out[n] = pcolor
                }
                else -> {
                    for (n in 0 until stopN(0)) out[n] = RGBA(colors.first()).colorInt(premultiplied)
                    for (n in 0 until numberOfStops - 1) {
                        val stop0 = stopN(n + 0)
                        val stop1 = stopN(n + 1)
                        val color0 = RGBA(colors.getAt(n + 0))
                        val color1 = RGBA(colors.getAt(n + 1))
                        for (s in stop0 until stop1) {
                            val ratio = (s - stop0).toDouble() / (stop1 - stop0).toDouble()
                            out[s] = RGBA.interpolate(color0, color1, ratio).colorInt(premultiplied)
                        }
                    }
                    for (n in stopN(numberOfStops - 1) until NCOLORS) out[n] = RGBA(colors.last()).colorInt(premultiplied)
                }
            }
        }
    }

    fun fillColors(out: RgbaArray): Unit = _fillColors(out.ints, stops, colors, premultiplied = false)
    fun fillColors(out: RgbaPremultipliedArray): Unit = _fillColors(out.ints, stops, colors, premultiplied = true)

    fun addColorStop(stop: Double, color: RGBA): GradientPaint = add(stop, color)
    inline fun addColorStop(stop: Number, color: RGBA): GradientPaint = add(stop.toDouble(), color)

    fun add(stop: Double, color: RGBA): GradientPaint = this.apply {
        stops += stop
        colors += color.value
        return this
    }

    val untransformedGradientMatrix = Matrix().apply {
        translate(-x0, -y0)
        val scale = 1.0 / Point.distance(x0, y0, x1, y1).clamp(1.0, 16000.0)
        scale(scale, scale)
        rotate(-Angle.between(x0, y0, x1, y1))
    }

    //val gradientMatrixInv = gradientMatrix.inverted()
    val transformInv = transform.inverted()

    val gradientMatrix = Matrix().apply {
        identity()
        premultiply(untransformedGradientMatrix)
        premultiply(transformInv)
    }

    private val r0r1_2 = 2 * r0 * r1
    private val r0pow2 = r0.pow2
    private val r1pow2 = r1.pow2
    private val y0_y1 = y0 - y1
    private val r0_r1 = r0 - r1
    private val x0_x1 = x0 - x1
    private val radial_scale = 1.0 / ((r0 - r1).pow2 - (x0 - x1).pow2 - (y0 - y1).pow2)

    fun getRatioAt(px: Double, py: Double): Double {
        //val x = px
        //val y = py
        return cycle.apply(when (kind) {
            GradientKind.SWEEP -> {
                val x = transformInv.transformX(px, py)
                val y = transformInv.transformY(px, py)
                Point.angle(x0, y0, x, y) / 360.degrees
            }
            GradientKind.RADIAL -> {
                val x = transformInv.transformX(px, py)
                val y = transformInv.transformY(px, py)
                1.0 - (-r1 * r0_r1 + x0_x1 * (x1 - x) + y0_y1 * (y1 - y) - sqrt(r1pow2 * ((x0 - x).pow2 + (y0 - y).pow2) - r0r1_2 * ((x0 - x) * (x1 - x) + (y0 - y) * (y1 - y)) + r0pow2 * ((x1 - x).pow2 + (y1 - y).pow2) - (x1 * y0 - x * y0 - x0 * y1 + x * y1 + x0 * y - x1 * y).pow2)) * radial_scale
            }
            else -> {
                //println("gradientMatrix.transformX($x, $y): ${gradientMatrix.transformX(x, y)}")
                gradientMatrix.transformX(px, py)
            }
        })
    }

    val Float.pow2: Float get() = this * this
    val Double.pow2: Double get() = this * this

    fun getRatioAt(x: Double, y: Double, m: Matrix): Double {
        //val tx = gradientMatrix.transformX(x, y)
        //val ty = gradientMatrix.transformY(x, y)
        //return m.transformX(tx, ty)
        return getRatioAt(m.transformX(x, y), m.transformY(x, y))
        //return getRatioAt(x, y)
    }

    //override fun applyMatrix(m: Matrix): GradientPaint = copy(transform = transform * m)
    override fun replaceMatrix(m: Matrix): GradientPaint = copy(transform = m)

    override fun clone(): Paint = copy(transform = transform.clone())

    private fun colorsToString(colors: RgbaArray): String = "[" + colors.joinToString(", ") { it.hexString } + "]"
    private fun colorsToString(colors: IntArrayList): String = colorsToString(RgbaArray(colors.toIntArray()))

    override fun toString(): String {
        return buildString {
            append(when (kind) {
                GradientKind.LINEAR -> "LinearGradient([$x0, $y0], [$x1, $y1]"
                GradientKind.RADIAL -> "RadialGradient([$x0, $y0, $r0], [$x1, $y1, $r1]"
                GradientKind.SWEEP -> "ConicGradient([$x0, $y0]"
            })
            append(", stops=$stops, colors=${colorsToString(colors)}, cycle=$cycle, transform=$transform, interpolationMethod=$interpolationMethod, units=$units)")
        }
    }
}

inline fun LinearGradientPaint(x0: Number, y0: Number, x1: Number, y1: Number, cycle: CycleMethod = CycleMethod.NO_CYCLE, transform: Matrix = Matrix(), block: GradientPaint.() -> Unit = {}) = GradientPaint(GradientKind.LINEAR, x0.toDouble(), y0.toDouble(), 0.0, x1.toDouble(), y1.toDouble(), 0.0, cycle = cycle, transform = transform).also(block)
inline fun RadialGradientPaint(x0: Number, y0: Number, r0: Number, x1: Number, y1: Number, r1: Number, cycle: CycleMethod = CycleMethod.NO_CYCLE, transform: Matrix = Matrix(), block: GradientPaint.() -> Unit = {}) = GradientPaint(GradientKind.RADIAL, x0.toDouble(), y0.toDouble(), r0.toDouble(), x1.toDouble(), y1.toDouble(), r1.toDouble(), cycle = cycle, transform = transform).also(block)
@Deprecated("Only available on Android or Bitmap32")
inline fun SweepGradientPaint(x0: Number, y0: Number, transform: Matrix = Matrix(), block: GradientPaint.() -> Unit = {}) = GradientPaint(GradientKind.SWEEP, x0.toDouble(), y0.toDouble(), 0.0, 0.0, 0.0, 0.0, transform = transform).also(block)
inline fun ConicGradientPaint(startAngle: Angle, x0: Number, y0: Number, transform: Matrix = Matrix(), block: GradientPaint.() -> Unit = {}) = GradientPaint(GradientKind.CONIC, x0.toDouble(), y0.toDouble(), 0.0, 0.0, 0.0, 0.0, startAngle = startAngle, transform = transform).also(block)

/** Adds color stops to the gradient in the [pairs] list being the left of the pair the ratio between 0.0 and 1.0, and the right of the pair the [Color] */
fun GradientPaint.add(vararg pairs: Pair<Double, RGBA>): GradientPaint {
    for ((ratio, color) in pairs) add(ratio, color)
    return this
}

/** Adds colors [c0] and [c1] to the gradient equidistantly */
fun GradientPaint.add(c0: RGBA, c1: RGBA) = add(0.0, c0).add(1.0, c1)
/** Adds colors [c0], [c1] and [c2] to the gradient equidistantly */
fun GradientPaint.add(c0: RGBA, c1: RGBA, c2: RGBA) = add(0.0, c0).add(0.5, c1).add(1.0, c2)
/** Adds colors [c0], [c1], [c2] and [c3] to the gradient equidistantly */
fun GradientPaint.add(c0: RGBA, c1: RGBA, c2: RGBA, c3: RGBA) = add(0.0, c0).add(1.0 / 3.0, c1).add(2.0 / 3.0, c2).add(1.0, c3)
/** Adds [colors] to the gradient equidistantly */
inline fun <T : RGBA> GradientPaint.add(vararg colors: T): GradientPaint = add(RgbaArray(colors.size) { colors[it] })
/** Adds [colors] to the gradient equidistantly */
fun GradientPaint.add(colors: RgbaArray): GradientPaint {
    val size = (colors.size - 1).coerceAtLeast(1).toDouble()
    for (n in 0 until colors.size) {
        val ratio = n.toDouble() / size
        add(ratio, colors[n])
    }
    return this
}

fun Bitmap.toPaint(
    transform: Matrix = Matrix(),
    cycleX: CycleMethod = CycleMethod.NO_CYCLE,
    cycleY: CycleMethod = CycleMethod.NO_CYCLE,
    smooth: Boolean = true,
    units: GradientUnits = GradientUnits.OBJECT_BOUNDING_BOX
): BitmapPaint = BitmapPaint(this, transform, cycleX, cycleY, smooth, units)

data class BitmapPaint(
    val bitmap: Bitmap,
    override val transform: Matrix = Matrix(),
    val cycleX: CycleMethod = CycleMethod.NO_CYCLE,
    val cycleY: CycleMethod = CycleMethod.NO_CYCLE,
    val smooth: Boolean = true,
    override val units: GradientUnits = GradientUnits.OBJECT_BOUNDING_BOX
) : TransformedPaint {
    val repeatX: Boolean get() = cycleX.repeating
    val repeatY: Boolean get() = cycleY.repeating
    val repeat: Boolean get() = repeatX || repeatY

    val bmp32 = bitmap.toBMP32()
    override fun clone(): Paint = copy(transform = transform.clone())

    //override fun applyMatrix(m: Matrix): BitmapPaint = copy(transform = transform * m)
    override fun replaceMatrix(m: Matrix): BitmapPaint = copy(transform = m)

    //override fun transformed(m: Matrix) = BitmapPaint(bitmap, Matrix().multiply(this.transform, m))
    override fun toString(): String = "BitmapPaint($bitmap, cycle=($cycleX, $cycleY), smooth=$smooth, transform=$transform)"
}

/*
const canvasLinear = document.getElementById('canvasLinear');
const ctx = canvasLinear.getContext('2d');
const fillStyle = ctx.createRadialGradient(150,150,30, 130,180,70);
fillStyle.addColorStop(0, 'red');
fillStyle.addColorStop(0.5, 'green');
fillStyle.addColorStop(1, 'blue');
ctx.fillStyle = fillStyle;
ctx.translate(100, 20)
ctx.scale(2, 2)
ctx.fillRect(100, 100, 100, 100);
*/

fun Paint.getPaintWithUnits(transform: Matrix, path: VectorPath): Paint {
    val paint = this
    if (paint !is TransformedPaint) return this
    if (paint.units == GradientUnits.USER_SPACE_ON_USE) return this
    //val points = path.clone().applyTransform(transform).getPoints2()
    //val points = path.getPoints2()
    //val bounds = BoundsBuilder().add(path.getPoints2()).getBounds()
    val bounds = path.getBounds()
    val m = Matrix()
    m.pretranslate(bounds.x, bounds.y)
    m.prescale(bounds.width, bounds.height)
    return paint.replaceMatrix(paint.transform * m * transform.inverted())
}
