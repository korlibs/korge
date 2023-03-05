package com.soywiz.korma.geom

import com.soywiz.korma.annotations.*
import com.soywiz.korma.interpolation.*


@KormaMutableApi
sealed interface IPointInt {
    val x: Int
    val y: Int

    val point: PointInt get() = PointInt(x, y)

    companion object {
        operator fun invoke(x: Int, y: Int): IPointInt = MPointInt(x, y)
    }
}

@KormaMutableApi
inline class MPointInt(val p: MPoint) : IPointInt, Comparable<IPointInt>, MutableInterpolable<MPointInt> {
    override fun compareTo(other: IPointInt): Int = compare(this.x, this.y, other.x, other.y)

    companion object {
        operator fun invoke(): MPointInt = MPointInt(0, 0)
        operator fun invoke(x: Int, y: Int): MPointInt = MPointInt(MPoint(x, y))
        operator fun invoke(that: IPointInt): MPointInt = MPointInt(MPoint(that.x, that.y))

        fun compare(lx: Int, ly: Int, rx: Int, ry: Int): Int {
            val ret = ly.compareTo(ry)
            return if (ret == 0) lx.compareTo(rx) else ret
        }
    }
    override var x: Int ; set(value) { p.x = value.toDouble() } get() = p.x.toInt()
    override var y: Int ; set(value) { p.y = value.toDouble() } get() = p.y.toInt()
    fun setTo(x: Int, y: Int) : MPointInt {
        this.x = x
        this.y = y
        return this
    }
    fun setTo(that: IPointInt) = this.setTo(that.x, that.y)

    override fun setToInterpolated(ratio: Ratio, l: MPointInt, r: MPointInt): MPointInt =
        setTo(ratio.interpolate(l.x, r.x), ratio.interpolate(l.y, r.y))

    override fun toString(): String = "($x, $y)"
}

fun MPoint.asInt(): MPointInt = MPointInt(this)
fun MPointInt.asDouble(): MPoint = this.p
