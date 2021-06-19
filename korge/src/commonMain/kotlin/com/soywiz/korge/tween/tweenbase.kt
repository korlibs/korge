package com.soywiz.korge.tween

import com.soywiz.klock.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*
import kotlin.jvm.*
import kotlin.reflect.*

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
internal fun _interpolate(ratio: Double, l: Double, r: Double): Double = when {
    ratio < 0.0 -> l
    ratio >= 1.0 -> r
    else -> ratio.interpolate(l, r)
}

@PublishedApi
internal fun _interpolateInt(ratio: Double, l: Int, r: Int): Int = when {
    ratio < 0.0 -> l
    ratio >= 1.0 -> r
    else -> ratio.interpolate(l, r)
}

@PublishedApi
internal fun <V : Interpolable<V>> _interpolateInterpolable(ratio: Double, l: V, r: V): V = when {
    ratio < 0.0 -> l
    ratio >= 1.0 -> r
    else -> ratio.interpolate(l, r)
}

@PublishedApi
internal fun _interpolateFloat(ratio: Double, l: Float, r: Float): Float = when {
    ratio < 0.0 -> l
    ratio >= 1.0 -> r
    else -> ratio.interpolate(l, r)
}

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
    if (!minimizeAngle) return Angle(_interpolate(ratio, l.radians, r.radians))
    val ln = l.normalized
    val rn = r.normalized
    return when {
        (rn - ln).absoluteValue <= 180.degrees -> Angle(_interpolate(ratio, ln.radians, rn.radians))
        ln < rn -> Angle(_interpolate(ratio, (ln + 360.degrees).radians, rn.radians)).normalized
        else -> Angle(_interpolate(ratio, ln.radians, (rn + 360.degrees).radians)).normalized
    }
}

@PublishedApi
internal fun _interpolateTimeSpan(ratio: Double, l: TimeSpan, r: TimeSpan): TimeSpan = _interpolate(ratio, l.milliseconds, r.milliseconds).milliseconds

//inline operator fun KMutableProperty0<Float>.get(end: Number) = V2(this, this.get(), end.toFloat(), ::_interpolateFloat)
//inline operator fun KMutableProperty0<Float>.get(initial: Number, end: Number) =
//	V2(this, initial.toFloat(), end.toFloat(), ::_interpolateFloat)

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

fun <V> V2<V>.easing(easing: Easing): V2<V> = this.copy(interpolator = { ratio, a, b -> this.interpolator(easing(ratio), a, b) })

inline fun <V> V2<V>.delay(startTime: TimeSpan) = this.copy(startTime = startTime)
inline fun <V> V2<V>.duration(duration: TimeSpan) = this.copy(duration = duration)

inline fun <V> V2<V>.linear() = this
inline fun <V> V2<V>.smooth() = this.easing(Easing.SMOOTH)
inline fun <V> V2<V>.easeIn() = this.easing(Easing.EASE_IN)
inline fun <V> V2<V>.easeOut() = this.easing(Easing.EASE_OUT)
inline fun <V> V2<V>.easeInOut() = this.easing(Easing.EASE_IN_OUT)
inline fun <V> V2<V>.easeOutIn() = this.easing(Easing.EASE_OUT_IN)
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
