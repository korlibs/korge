package com.soywiz.korim.vector.paint

import com.soywiz.kds.DoubleArrayList
import com.soywiz.kds.IntArrayList
import com.soywiz.kmem.clamp
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.*
import kotlin.apply
import kotlin.math.*

interface Paint {
    fun transformed(m: Matrix): Paint
}

object NonePaint : Paint {
    override fun transformed(m: Matrix) = this
}

open class ColorPaint(val color: RGBA) : Paint {
    override fun transformed(m: Matrix) = this
}

/**
 * Paints a default color. For BitmapFonts, draw the original Bitmap without tinting.
 */
object DefaultPaint : ColorPaint(Colors.BLACK)

interface TransformedPaint : Paint {
    val transform: Matrix
}

enum class GradientKind {
    LINEAR, RADIAL, SWEEP
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
    val units: GradientUnits = GradientUnits.OBJECT_BOUNDING_BOX
) : TransformedPaint {
    fun x0(m: Matrix) = m.transformX(x0, y0)
    fun y0(m: Matrix) = m.transformY(x0, y0)
    fun r0(m: Matrix) = m.transformX(r0, r0)

    fun x1(m: Matrix) = m.transformX(x1, y1)
    fun y1(m: Matrix) = m.transformY(x1, y1)
    fun r1(m: Matrix) = m.transformX(r1, r1)

    val numberOfStops get() = stops.size

    fun addColorStop(stop: Double, color: RGBA): GradientPaint = add(stop, color)
    inline fun addColorStop(stop: Number, color: RGBA): GradientPaint = add(stop.toDouble(), color)

    fun add(stop: Double, color: RGBA): GradientPaint = this.apply {
        stops += stop
        colors += color.value
        return this
    }

    val gradientMatrix = Matrix().apply {
        translate(-x0, -y0)
        scale(1.0 / Point.distance(x0, y0, x1, y1).clamp(1.0, 16000.0))
        rotate(-Angle.between(x0, y0, x1, y1))
        premultiply(transform)
    }

    val gradientMatrixInv = gradientMatrix.inverted()

    private val r0r1_2 = 2 * r0 * r1
    private val r0pow2 = r0.pow2
    private val r1pow2 = r1.pow2
    private val y0_y1 = y0 - y1
    private val r0_r1 = r0 - r1
    private val x0_x1 = x0 - x1
    private val radial_scale = 1.0 / ((r0 - r1).pow2 - (x0 - x1).pow2 - (y0 - y1).pow2)

    fun getRatioAt(x: Double, y: Double): Double = cycle.apply(when (kind) {
        GradientKind.SWEEP -> {
            Point.angle(x0, y0, x, y) / 360.degrees
        }
        GradientKind.RADIAL -> {
            //1.0 - (-r1 * (r0 - r1) + (x0 - x1) * (x1 - x) + (y0 - y1) * (y1 - y) - sqrt(r1.pow2 * ((x0 - x).pow2 + (y0 - y).pow2) - 2 * r0 * r1 * ((x0 - x) * (x1 - x) + (y0 - y) * (y1 - y)) + r0.pow2 * ((x1 - x).pow2 + (y1 - y).pow2) - (x1 * y0 - x * y0 - x0 * y1 + x * y1 + x0 * y - x1 * y).pow2)) / ((r0 - r1).pow2 - (x0 - x1).pow2 - (y0 - y1).pow2)
            1.0 - (-r1 * r0_r1 + x0_x1 * (x1 - x) + y0_y1 * (y1 - y) - sqrt(r1pow2 * ((x0 - x).pow2 + (y0 - y).pow2) - r0r1_2 * ((x0 - x) * (x1 - x) + (y0 - y) * (y1 - y)) + r0pow2 * ((x1 - x).pow2 + (y1 - y).pow2) - (x1 * y0 - x * y0 - x0 * y1 + x * y1 + x0 * y - x1 * y).pow2)) * radial_scale
        }
        else -> {
            gradientMatrix.transformX(x, y)
        }
    })

    val Double.pow2 get() = this * this

    fun getRatioAt(x: Double, y: Double, m: Matrix): Double = getRatioAt(m.transformX(x, y), m.transformY(x, y))

    fun applyMatrix(m: Matrix): GradientPaint = GradientPaint(
        kind,
        m.transformX(x0, y0),
        m.transformY(x0, y0),
        r0,
        m.transformX(x1, y1),
        m.transformY(x1, y1),
        r1,
        DoubleArrayList(stops),
        IntArrayList(colors),
        cycle,
        Matrix(),
        interpolationMethod,
        units
    )

    override fun transformed(m: Matrix) = applyMatrix(m)

    override fun toString(): String = when (kind) {
        GradientKind.LINEAR -> "LinearGradient($x0, $y0, $x1, $y1, $stops, $colors)"
        GradientKind.RADIAL -> "RadialGradient($x0, $y0, $r0, $x1, $y1, $r1, $stops, $colors)"
        GradientKind.SWEEP -> "SweepGradient($x0, $y0, $stops, $colors)"
    }
}

inline fun LinearGradientPaint(x0: Number, y0: Number, x1: Number, y1: Number, cycle: CycleMethod = CycleMethod.NO_CYCLE, transform: Matrix = Matrix(), block: GradientPaint.() -> Unit) = GradientPaint(GradientKind.LINEAR, x0.toDouble(), y0.toDouble(), 0.0, x1.toDouble(), y1.toDouble(), 0.0, cycle = cycle, transform = transform).also(block)
inline fun RadialGradientPaint(x0: Number, y0: Number, r0: Number, x1: Number, y1: Number, r1: Number, cycle: CycleMethod = CycleMethod.NO_CYCLE, transform: Matrix = Matrix(), block: GradientPaint.() -> Unit) = GradientPaint(GradientKind.RADIAL, x0.toDouble(), y0.toDouble(), r0.toDouble(), x1.toDouble(), y1.toDouble(), r1.toDouble(), cycle = cycle, transform = transform).also(block)
inline fun SweepGradientPaint(x0: Number, y0: Number, transform: Matrix = Matrix(), block: GradientPaint.() -> Unit) = GradientPaint(GradientKind.SWEEP, x0.toDouble(), y0.toDouble(), 0.0, 0.0, 0.0, 0.0, transform = transform).also(block)

inline fun LinearGradientPaint(x0: Number, y0: Number, x1: Number, y1: Number, cycle: CycleMethod = CycleMethod.NO_CYCLE, transform: Matrix = Matrix()) = GradientPaint(GradientKind.LINEAR, x0.toDouble(), y0.toDouble(), 0.0, x1.toDouble(), y1.toDouble(), 0.0, cycle = cycle, transform = transform)
inline fun RadialGradientPaint(x0: Number, y0: Number, r0: Number, x1: Number, y1: Number, r1: Number, cycle: CycleMethod = CycleMethod.NO_CYCLE, transform: Matrix = Matrix()) = GradientPaint(GradientKind.RADIAL, x0.toDouble(), y0.toDouble(), r0.toDouble(), x1.toDouble(), y1.toDouble(), r1.toDouble(), cycle = cycle, transform = transform)
inline fun SweepGradientPaint(x0: Number, y0: Number, transform: Matrix = Matrix()) = GradientPaint(GradientKind.SWEEP, x0.toDouble(), y0.toDouble(), 0.0, 0.0, 0.0, 0.0, transform = transform)

class BitmapPaint(
    val bitmap: Bitmap,
    override val transform: Matrix,
    val cycleX: CycleMethod = CycleMethod.NO_CYCLE,
    val cycleY: CycleMethod = CycleMethod.NO_CYCLE,
    val smooth: Boolean = true
) : TransformedPaint {
    val repeatX: Boolean get() = cycleX != CycleMethod.NO_CYCLE
    val repeatY: Boolean get() = cycleY != CycleMethod.NO_CYCLE
    val repeat: Boolean get() = repeatX || repeatY

    // Old constructor
    constructor(
        bitmap: Bitmap,
        transform: Matrix,
        repeat: Boolean = false,
        smooth: Boolean = true
    ) : this(bitmap, transform, if (repeat) CycleMethod.REPEAT else CycleMethod.NO_CYCLE, if (repeat) CycleMethod.REPEAT else CycleMethod.NO_CYCLE, smooth)

    val bmp32 = bitmap.toBMP32()
    //override fun transformed(m: Matrix) = BitmapPaint(bitmap, Matrix().multiply(m, this.transform))
    override fun transformed(m: Matrix) = BitmapPaint(bitmap, Matrix().multiply(this.transform, m))
}
