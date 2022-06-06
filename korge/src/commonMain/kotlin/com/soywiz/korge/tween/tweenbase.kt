package com.soywiz.korge.tween

import com.soywiz.klock.TimeSpan
import com.soywiz.klock.coalesce
import com.soywiz.klock.milliseconds
import com.soywiz.klock.nanoseconds
import com.soywiz.kmem.clamp01
import com.soywiz.kmem.fract
import com.soywiz.kmem.toIntFloor
import com.soywiz.korim.color.ColorAdd
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.IPoint
import com.soywiz.korma.geom.IPointArrayList
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.PointArrayList
import com.soywiz.korma.geom.absoluteValue
import com.soywiz.korma.geom.bezier.getEquidistantPoints
import com.soywiz.korma.geom.bezier.getPoints
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.firstX
import com.soywiz.korma.geom.firstY
import com.soywiz.korma.geom.lastX
import com.soywiz.korma.geom.lastY
import com.soywiz.korma.geom.minus
import com.soywiz.korma.geom.normalized
import com.soywiz.korma.geom.plus
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korma.geom.vector.getCurves
import com.soywiz.korma.interpolation.Easing
import com.soywiz.korma.interpolation.Interpolable
import com.soywiz.korma.interpolation.interpolate
import com.soywiz.korma.math.isAlmostEquals
import kotlin.jvm.JvmName
import kotlin.reflect.KMutableProperty0

@Suppress("UNCHECKED_CAST")
data class V2<V>(
    val key: KMutableProperty0<V>,
    var initial: V,
    val end: V,
    val interpolator: (Double, V, V) -> V,
    val includeStart: Boolean,
    val startTime: TimeSpan = 0.nanoseconds,
    val duration: TimeSpan = TimeSpan.NIL
) {
    val endTime = startTime + duration.coalesce { 0.nanoseconds }

    fun init() {
        if (!includeStart) {
            initial = key.get()
        }
    }
    fun set(ratio: Double) = key.set(interpolator(ratio, initial, end))

    override fun toString(): String =
        "V2(key=${key.name}, range=[$initial-$end], startTime=$startTime, duration=$duration)"
}

@JvmName("getInt")
operator fun KMutableProperty0<Int>.get(end: Int) = V2(this, this.get(), end, ::_interpolateInt, includeStart = false)
@JvmName("getInt")
operator fun KMutableProperty0<Int>.get(initial: Int, end: Int) = V2(this, initial, end, ::_interpolateInt, includeStart = true)

@JvmName("getMutableProperty")
operator fun <V : Interpolable<V>> KMutableProperty0<V>.get(end: V) = V2(this, this.get(), end, ::_interpolateInterpolable, includeStart = false)
@JvmName("getMutableProperty")
operator fun <V : Interpolable<V>> KMutableProperty0<V>.get(initial: V, end: V) = V2(this, initial, end, ::_interpolateInterpolable, includeStart = true)

@PublishedApi
internal fun _interpolate(ratio: Double, l: Double, r: Double): Double = ratio.interpolate(l, r)

@PublishedApi
internal fun _interpolateInt(ratio: Double, l: Int, r: Int): Int = ratio.interpolate(l, r)

@PublishedApi
internal fun <V : Interpolable<V>> _interpolateInterpolable(ratio: Double, l: V, r: V): V = ratio.interpolate(l, r)

@PublishedApi
internal fun _interpolateFloat(ratio: Double, l: Float, r: Float): Float = ratio.interpolate(l, r)

@PublishedApi
internal fun _interpolateColor(ratio: Double, l: RGBA, r: RGBA): RGBA = RGBA.mixRgba(l, r, ratio)

@PublishedApi
internal fun _interpolateColorAdd(ratio: Double, l: ColorAdd, r: ColorAdd): ColorAdd = ColorAdd(
    ratio.interpolate(l.r, r.r),
    ratio.interpolate(l.g, r.g),
    ratio.interpolate(l.b, r.b),
    ratio.interpolate(l.a, r.a)
)

@PublishedApi
internal fun _interpolateAngle(ratio: Double, l: Angle, r: Angle): Angle = _interpolateAngleAny(ratio, l, r, minimizeAngle = true)

@PublishedApi
internal fun _interpolateAngleDenormalized(ratio: Double, l: Angle, r: Angle): Angle = _interpolateAngleAny(ratio, l, r, minimizeAngle = false)

internal fun _interpolateAngleAny(ratio: Double, l: Angle, r: Angle, minimizeAngle: Boolean = true): Angle {
    if (!minimizeAngle) return Angle.fromRatio(_interpolate(ratio, l.ratio, r.ratio))
    val ln = l.normalized
    val rn = r.normalized
    return when {
        (rn - ln).absoluteValue <= 180.degrees -> Angle.fromRadians(_interpolate(ratio, ln.radians, rn.radians))
        ln < rn -> Angle.fromRadians(_interpolate(ratio, (ln + 360.degrees).radians, rn.radians)).normalized
        else -> Angle.fromRadians(_interpolate(ratio, ln.radians, (rn + 360.degrees).radians)).normalized
    }
}

@PublishedApi
internal fun _interpolateTimeSpan(ratio: Double, l: TimeSpan, r: TimeSpan): TimeSpan = _interpolate(ratio, l.milliseconds, r.milliseconds).milliseconds

//inline operator fun KMutableProperty0<Float>.get(end: Number) = V2(this, this.get(), end.toFloat(), ::_interpolateFloat)
//inline operator fun KMutableProperty0<Float>.get(initial: Number, end: Number) =
//	V2(this, initial.toFloat(), end.toFloat(), ::_interpolateFloat)

inline operator fun KMutableProperty0<IPoint>.get(path: VectorPath, includeLastPoint: Boolean = path.isLastCommandClose, reversed: Boolean = false): V2<IPoint> = this[path.getCurves().getEquidistantPoints().also {
    //println("points.lastX=${points.lastX}, points.firstX=${points.firstX}")
    //println("points.lastY=${points.lastY}, points.firstY=${points.firstY}")
    if (!includeLastPoint && it.lastX.isAlmostEquals(it.firstX) && it.lastY.isAlmostEquals(it.firstY)) {
        (it as PointArrayList).removeAt(it.size - 1)
        //println("REMOVED LAST POINT!")
    }
    if (reversed) {
        (it as PointArrayList).reverse()
    }
}]

inline operator fun KMutableProperty0<IPoint>.get(range: IPointArrayList): V2<IPoint> {
    val temp = Point()
    return V2(
        this, temp, temp, { ratio, _, _ ->
            val ratioIndex = ratio * (range.size - 1)
            val index = ratioIndex.toIntFloor()
            val index1 = (index + 1).coerceAtMost(range.size)
            val sratio = fract(ratioIndex)
            temp.setTo(
                sratio.interpolate(range.getX(index), range.getX(index1)),
                sratio.interpolate(range.getY(index), range.getY(index1))
            )
        }, includeStart = false
    )
}

@JvmName("getFloat")
inline operator fun KMutableProperty0<Float>.get(end: Float) = V2(this, this.get(), end, ::_interpolateFloat, includeStart = false)
@JvmName("getFloat")
inline operator fun KMutableProperty0<Float>.get(initial: Float, end: Float) = V2(this, initial, end, ::_interpolateFloat, true)

inline operator fun KMutableProperty0<Double>.get(end: Double) = V2(this, this.get(), end, ::_interpolate, includeStart = false)
inline operator fun KMutableProperty0<Double>.get(initial: Double, end: Double) = V2(this, initial, end, ::_interpolate, true)

inline operator fun KMutableProperty0<Double>.get(end: Int) = get(end.toDouble())
inline operator fun KMutableProperty0<Double>.get(initial: Int, end: Int) = get(initial.toDouble(), end.toDouble())

inline operator fun KMutableProperty0<Double>.get(end: Float) = get(end.toDouble())
inline operator fun KMutableProperty0<Double>.get(initial: Float, end: Float) = get(initial.toDouble(), end.toDouble())

inline operator fun KMutableProperty0<Double>.get(end: Long) = get(end.toDouble())
inline operator fun KMutableProperty0<Double>.get(initial: Long, end: Float) = get(initial.toDouble(), end.toDouble())

inline operator fun KMutableProperty0<Double>.get(end: Number) = get(end.toDouble())
inline operator fun KMutableProperty0<Double>.get(initial: Number, end: Number) = get(initial.toDouble(), end.toDouble())

inline operator fun KMutableProperty0<RGBA>.get(end: RGBA) = V2(this, this.get(), end, ::_interpolateColor, includeStart = false)
inline operator fun KMutableProperty0<RGBA>.get(initial: RGBA, end: RGBA) = V2(this, initial, end, ::_interpolateColor, includeStart = true)

inline operator fun KMutableProperty0<ColorAdd>.get(end: ColorAdd) = V2(this, this.get(), end, ::_interpolateColorAdd, includeStart = false)
inline operator fun KMutableProperty0<ColorAdd>.get(initial: ColorAdd, end: ColorAdd) = V2(this, initial, end, ::_interpolateColorAdd, includeStart = true)

inline operator fun KMutableProperty0<Angle>.get(end: Angle) = V2(this, this.get(), end, ::_interpolateAngle, includeStart = false)
inline operator fun KMutableProperty0<Angle>.get(initial: Angle, end: Angle) = V2(this, initial, end, ::_interpolateAngle, includeStart = true)

fun V2<Angle>.denormalized(): V2<Angle> = this.copy(interpolator = ::_interpolateAngleDenormalized)

inline operator fun KMutableProperty0<TimeSpan>.get(end: TimeSpan) = V2(this, this.get(), end, ::_interpolateTimeSpan, includeStart = false)
inline operator fun KMutableProperty0<TimeSpan>.get(initial: TimeSpan, end: TimeSpan) = V2(this, initial, end, ::_interpolateTimeSpan, includeStart = true)

fun <V> V2<V>.clamped(): V2<V> = copy(interpolator = { ratio, l, r -> this.interpolator(ratio.clamp01(), l, r) })
fun <V> V2<V>.easing(easing: Easing): V2<V> = this.copy(interpolator = { ratio, a, b -> this.interpolator(easing(ratio), a, b) })

inline fun <V> V2<V>.delay(startTime: TimeSpan) = this.copy(startTime = startTime)
inline fun <V> V2<V>.duration(duration: TimeSpan) = this.copy(duration = duration)

inline fun <V> V2<V>.linear() = this
inline fun <V> V2<V>.smooth() = this.easing(Easing.SMOOTH)

inline fun <V> V2<V>.ease() = this.easing(Easing.EASE)
inline fun <V> V2<V>.easeIn() = this.easing(Easing.EASE_IN)
inline fun <V> V2<V>.easeOut() = this.easing(Easing.EASE_OUT)
inline fun <V> V2<V>.easeInOut() = this.easing(Easing.EASE_IN_OUT)

inline fun <V> V2<V>.easeInOld() = this.easing(Easing.EASE_IN_OLD)
inline fun <V> V2<V>.easeOutOld() = this.easing(Easing.EASE_OUT_OLD)
inline fun <V> V2<V>.easeInOutOld() = this.easing(Easing.EASE_IN_OUT_OLD)
inline fun <V> V2<V>.easeOutInOld() = this.easing(Easing.EASE_OUT_IN_OLD)

inline fun <V> V2<V>.easeInBack() = this.easing(Easing.EASE_IN_BACK)
inline fun <V> V2<V>.easeOutBack() = this.easing(Easing.EASE_OUT_BACK)
inline fun <V> V2<V>.easeInOutBack() = this.easing(Easing.EASE_IN_OUT_BACK)
inline fun <V> V2<V>.easeOutInBack() = this.easing(Easing.EASE_OUT_IN_BACK)

inline fun <V> V2<V>.easeInElastic() = this.easing(Easing.EASE_IN_ELASTIC)
inline fun <V> V2<V>.easeOutElastic() = this.easing(Easing.EASE_OUT_ELASTIC)
inline fun <V> V2<V>.easeInOutElastic() = this.easing(Easing.EASE_IN_OUT_ELASTIC)
inline fun <V> V2<V>.easeOutInElastic() = this.easing(Easing.EASE_OUT_IN_ELASTIC)

inline fun <V> V2<V>.easeInBounce() = this.easing(Easing.EASE_IN_BOUNCE)
inline fun <V> V2<V>.easeOutBounce() = this.easing(Easing.EASE_OUT_BOUNCE)
inline fun <V> V2<V>.easeInOutBounce() = this.easing(Easing.EASE_IN_OUT_BOUNCE)
inline fun <V> V2<V>.easeOutInBounce() = this.easing(Easing.EASE_OUT_IN_BOUNCE)

inline fun <V> V2<V>.easeInQuad() = this.easing(Easing.EASE_IN_QUAD)
inline fun <V> V2<V>.easeOutQuad() = this.easing(Easing.EASE_OUT_QUAD)
inline fun <V> V2<V>.easeInOutQuad() = this.easing(Easing.EASE_IN_OUT_QUAD)

inline fun <V> V2<V>.easeSine() = this.easing(Easing.EASE_SINE)

inline fun <V> V2<V>.easeClampStart() = this.easing(Easing.EASE_CLAMP_START)
inline fun <V> V2<V>.easeClampEnd() = this.easing(Easing.EASE_CLAMP_END)
inline fun <V> V2<V>.easeClampMiddle() = this.easing(Easing.EASE_CLAMP_MIDDLE)
