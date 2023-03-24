package korlibs.math.geom

import korlibs.math.annotations.KormaMutableApi
import korlibs.math.internal.niceStr
import korlibs.math.interpolation.Interpolable
import korlibs.math.interpolation.MutableInterpolable
import korlibs.math.interpolation.Ratio
import korlibs.math.interpolation.interpolate

@KormaMutableApi
@Deprecated("Use Size instead")
inline class MSize(val p: MPoint) : MutableInterpolable<MSize>, Interpolable<MSize>, Sizeable, MSizeable {
    companion object {
        operator fun invoke(): MSize = MSize(MPoint(0, 0))
        operator fun invoke(width: Double, height: Double): MSize = MSize(MPoint(width, height))
        operator fun invoke(width: Int, height: Int): MSize = MSize(MPoint(width, height))
        operator fun invoke(width: Float, height: Float): MSize = MSize(MPoint(width, height))
    }

    fun copy() = MSize(p.copy())

    override val size: Size get() = immutable
    override val mSize: MSize get() = this

    val area: Double get() = width * height
    val perimeter: Double get() = width * 2 + height * 2
    val min: Double get() = kotlin.math.min(width, height)
    val max: Double get() = kotlin.math.max(width, height)

    var width: Double
        set(value) { p.x = value }
        get() = p.x
    var height: Double
        set(value) { p.y = value }
        get() = p.y

    fun setTo(width: Double, height: Double): MSize {
        this.width = width
        this.height = height
        return this
    }
    fun setTo(width: Int, height: Int) = setTo(width.toDouble(), height.toDouble())
    fun setTo(width: Float, height: Float) = setTo(width.toDouble(), height.toDouble())
    fun setTo(that: MSize) = setTo(that.width, that.height)
    fun setTo(that: Size) = setTo(that.width, that.height)

    fun setToScaled(sx: Double, sy: Double) = setTo((this.width * sx), (this.height * sy))
    fun setToScaled(sx: Float, sy: Float) = setToScaled(sx.toDouble(), sy.toDouble())
    fun setToScaled(sx: Int, sy: Int) = setToScaled(sx.toDouble(), sy.toDouble())

    fun clone() = MSize(width, height)

    override fun interpolateWith(ratio: Ratio, other: MSize): MSize = MSize(0, 0).setToInterpolated(ratio, this, other)

    override fun setToInterpolated(ratio: Ratio, l: MSize, r: MSize): MSize = this.setTo(
        ratio.interpolate(l.width, r.width),
        ratio.interpolate(l.height, r.height)
    )

    override fun toString(): String = "Size(width=${width.niceStr}, height=${height.niceStr})"
}