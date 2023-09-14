package korlibs.math.geom

import korlibs.math.annotations.KormaMutableApi

@KormaMutableApi
@Deprecated("Use SizeInt instead")
inline class MSizeInt(val float: MSize) {
    companion object {
        operator fun invoke(): MSizeInt = MSizeInt(MSize(0, 0))
        operator fun invoke(width: Int, height: Int): MSizeInt = MSizeInt(MSize(width, height))
        operator fun invoke(that: SizeInt): MSizeInt = MSizeInt(MSize(that.width, that.height))
    }

    val immutable: SizeInt get() = SizeInt(width, height)
    fun clone(): MSizeInt = MSizeInt(float.clone())

    fun setTo(width: Int, height: Int) : MSizeInt {
        this.width = width
        this.height = height

        return this
    }

    fun setTo(that: SizeInt) = setTo(that.width, that.height)
    fun setTo(that: MSizeInt) = setTo(that.width, that.height)

    fun setToScaled(sx: Double, sy: Double) = setTo((this.width * sx).toInt(), (this.height * sy).toInt())
    fun setToScaled(sx: Int, sy: Int) = setToScaled(sx.toDouble(), sy.toDouble())
    fun setToScaled(sx: Float, sy: Float) = setToScaled(sx.toDouble(), sy.toDouble())

    fun anchoredIn(container: MRectangleInt, anchor: Anchor, out: MRectangleInt = MRectangleInt()): MRectangleInt {
        return out.setTo(
            ((container.width - this.width) * anchor.doubleX).toInt(),
            ((container.height - this.height) * anchor.doubleY).toInt(),
            width,
            height
        )
    }

    operator fun contains(v: MSizeInt): Boolean = (v.width <= width) && (v.height <= height)
    operator fun times(v: Double) = MSizeInt(MSize((width * v).toInt(), (height * v).toInt()))
    operator fun times(v: Int) = this * v.toDouble()
    operator fun times(v: Float) = this * v.toDouble()

    fun getAnchorPosition(anchor: Anchor, out: MPointInt = MPointInt(0, 0)): MPointInt =
        out.setTo((width * anchor.doubleX).toInt(), (height * anchor.doubleY).toInt())

    var width: Int
        set(value) { float.width = value.toDouble() }
        get() = float.width.toInt()
    var height: Int
        set(value) { float.height = value.toDouble() }
        get() = float.height.toInt()

    //override fun toString(): String = "SizeInt($width, $height)"
    override fun toString(): String = "SizeInt(width=$width, height=$height)"
}
