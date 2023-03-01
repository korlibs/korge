package com.soywiz.korma.interpolation

import com.soywiz.kds.fastCastTo
import com.soywiz.korma.geom.*

interface Interpolable<T> {
    fun interpolateWith(ratio: Ratio, other: T): T
}

interface MutableInterpolable<T> {
    fun setToInterpolated(ratio: Ratio, l: T, r: T): T
}

@Deprecated("") fun Double.interpolate(l: Point, r: Point): Point = this.toRatio().interpolate(l, r)
@Deprecated("") fun Double.interpolate(l: Float, r: Float): Float = this.toRatio().interpolate(l, r)
@Deprecated("") fun Double.interpolate(l: Double, r: Double): Double = this.toRatio().interpolate(l, r)
@Deprecated("") fun Double.interpolate(l: Int, r: Int): Int = this.toRatio().interpolate(l, r)
@Deprecated("") fun Double.interpolate(l: Long, r: Long): Long = this.toRatio().interpolate(l, r)
@Deprecated("") fun <T> Double.interpolate(l: Interpolable<T>, r: Interpolable<T>): T = this.toRatio().interpolate(l, r)
@Deprecated("") fun <T : Interpolable<T>> Double.interpolate(l: T, r: T): T = this.toRatio().interpolate(l, r)

fun Ratio.interpolate(l: Point, r: Point): Point = Point(interpolate(l.x, r.x), interpolate(l.y, r.y))
fun Ratio.interpolate(l: Float, r: Float): Float = (l + (r - l) * valueF)
fun Ratio.interpolate(l: Double, r: Double): Double = (l + (r - l) * valueD)
fun Ratio.interpolate(l: Int, r: Int): Int = (l + (r - l) * valueD).toInt()
fun Ratio.interpolate(l: Long, r: Long): Long = (l + (r - l) * valueD).toLong()
fun <T> Ratio.interpolate(l: Interpolable<T>, r: Interpolable<T>): T = l.interpolateWith(this, r.fastCastTo<T>())
fun <T : Interpolable<T>> Ratio.interpolate(l: T, r: T): T = l.interpolateWith(this, r)
