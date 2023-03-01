package com.soywiz.korma.interpolation

import com.soywiz.kds.fastCastTo
import com.soywiz.korma.geom.*

interface Interpolable<T> {
    fun interpolateWith(ratio: Ratio, other: T): T
}

interface MutableInterpolable<T> {
    fun setToInterpolated(ratio: Ratio, l: T, r: T): T
}

@Deprecated("")
fun Double.interpolate(l: Point, r: Point): Point = Point(interpolate(l.x, r.x), interpolate(l.y, r.y))
@Deprecated("")
fun Double.interpolate(l: Float, r: Float): Float = (l + (r - l) * this).toFloat()
@Deprecated("")
fun Double.interpolate(l: Double, r: Double): Double = (l + (r - l) * this)
@Deprecated("")
fun Double.interpolate(l: Int, r: Int): Int = (l + (r - l) * this).toInt()
@Deprecated("")
fun Double.interpolate(l: Long, r: Long): Long = (l + (r - l) * this).toLong()
@Deprecated("")
fun <T> Double.interpolate(l: Interpolable<T>, r: Interpolable<T>): T = l.interpolateWith(this.toRatio(), r.fastCastTo<T>())
@Deprecated("")
fun <T : Interpolable<T>> Double.interpolate(l: T, r: T): T = l.interpolateWith(this.toRatio(), r)

fun Ratio.interpolate(l: Point, r: Point): Point = Point(interpolate(l.x, r.x), interpolate(l.y, r.y))
fun Ratio.interpolate(l: Float, r: Float): Float = (l + (r - l) * valueF)
fun Ratio.interpolate(l: Double, r: Double): Double = (l + (r - l) * valueF)
fun Ratio.interpolate(l: Int, r: Int): Int = (l + (r - l) * valueF).toInt()
fun Ratio.interpolate(l: Long, r: Long): Long = (l + (r - l) * valueF).toLong()
fun <T> Ratio.interpolate(l: Interpolable<T>, r: Interpolable<T>): T = l.interpolateWith(this, r.fastCastTo<T>())
fun <T : Interpolable<T>> Ratio.interpolate(l: T, r: T): T = l.interpolateWith(this, r)
