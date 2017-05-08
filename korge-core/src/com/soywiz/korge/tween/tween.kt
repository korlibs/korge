@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.korge.tween

import com.soywiz.korge.component.Component
import com.soywiz.korge.time.TimeSpan
import com.soywiz.korge.view.View
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.async.CancellableContinuation
import com.soywiz.korio.async.suspendCancellableCoroutine
import com.soywiz.korio.util.clamp
import com.soywiz.korma.interpolation.interpolate
import com.soywiz.korma.interpolation.interpolateAny
import kotlin.reflect.KMutableProperty0

class TweenComponent(private val vs: List<V2<*>>, view: View, val time: Int? = null, val easing: Easing = Easing.LINEAR, val callback: (Double) -> Unit, val c: CancellableContinuation<Unit>) : Component(view) {
	var elapsed = 0
	val ctime = time ?: vs.map { it.endTime }.max() ?: 1000
	var cancelled = false

	init {
		c.onCancel {
			cancelled = true
		}
	}

	override fun update(dtMs: Int) {
		if (cancelled) {
			dettach()
			c.resume(Unit)
			return
		}
		elapsed += dtMs

		val ratio = (elapsed.toDouble() / ctime.toDouble()).clamp(0.0, 1.0)
		for (v in vs) {
			val durationInTween = (v.duration ?: (ctime - v.startTime))
			val elapsedInTween = (elapsed - v.startTime).clamp(0, durationInTween)
			val ratioInTween = if (durationInTween <= 0.0) 1.0 else elapsedInTween.toDouble() / durationInTween.toDouble()
			v.set(easing(ratioInTween))
		}
		callback(easing(ratio))

		if (ratio >= 1.0) {
			dettach()
			c.resume(Unit)
		}
	}
}

suspend fun View?.tween(vararg vs: V2<*>, time: TimeSpan, easing: Easing = Easing.LINEAR, callback: (Double) -> Unit = { }) = suspendCancellableCoroutine<Unit> { c ->
	val view = this@tween
	view?.addComponent(TweenComponent(vs.toList(), view, time.milliseconds, easing, callback, c))
}

@Suppress("UNCHECKED_CAST")
data class V2<V>(
	internal val key: KMutableProperty0<V>,
	internal val initial: V,
	internal val end: V,
	internal val interpolator: (V, V, Double) -> V,
	internal val startTime: Int = 0,
	internal val duration: Int? = null
) {
	val endTime = startTime + (duration ?: 0)

	@Deprecated("", replaceWith = ReplaceWith("key .. (initial...end)", "com.soywiz.korge.tween.rangeTo"))
	constructor(key: KMutableProperty0<V>, initial: V, end: V) : this(key, initial, end, ::interpolateAny)

	fun set(ratio: Double) = key.set(interpolator(initial, end, ratio))
}

operator fun <V> KMutableProperty0<V>.get(end: V) = V2(this, this.get(), end, ::interpolateAny)
operator fun <V> KMutableProperty0<V>.get(initial: V, end: V) = V2(this, initial, end, ::interpolateAny)

operator inline fun KMutableProperty0<Double>.get(end: Number) = V2(this, this.get(), end.toDouble(), ::interpolate)
operator inline fun KMutableProperty0<Double>.get(initial: Number, end: Number) = V2(this, initial.toDouble(), end.toDouble(), ::interpolate)

@Deprecated("Use get instead", level = DeprecationLevel.ERROR)
operator fun <V> V2<V>.rangeTo(that: V) = this.copy(initial = this.end, end = that)

@Deprecated("Use get instead", ReplaceWith("this[this.get()]"), DeprecationLevel.ERROR)
operator fun <V> KMutableProperty0<V>.rangeTo(that: V) = this[this.get()]

@Deprecated("Use get instead", ReplaceWith("this[that.start, that.endInclusive]"), DeprecationLevel.ERROR)
operator fun <V : Comparable<V>> KMutableProperty0<V>.rangeTo(that: ClosedRange<V>) = this[that.start, that.endInclusive]

@Deprecated("Use get instead", ReplaceWith("this[that.first, that.second]"), DeprecationLevel.ERROR)
operator fun <V> KMutableProperty0<V>.rangeTo(that: Pair<V, V>) = this[that.first, that.second]

fun <V> V2<V>.withEasing(easing: Easing): V2<V> = this.copy(interpolator = { a, b, ratio -> this.interpolator(a, b, easing(ratio)) })

fun V2<Int>.color(): V2<Int> = this.copy(interpolator = RGBA::blendRGBA)

fun <V> V2<V>.easing(easing: Easing): V2<V> = this.copy(interpolator = { a, b, ratio -> this.interpolator(a, b, easing(ratio)) })

inline fun <V> V2<V>.delay(startTime: TimeSpan) = this.copy(startTime = startTime.milliseconds)
inline fun <V> V2<V>.duration(duration: TimeSpan) = this.copy(duration = duration.milliseconds)

inline fun <V> V2<V>.linear() = this
inline fun <V> V2<V>.easeIn() = this.withEasing(Easings.EASE_IN)
inline fun <V> V2<V>.easeOut() = this.withEasing(Easings.EASE_OUT)
inline fun <V> V2<V>.easeInOut() = this.withEasing(Easings.EASE_IN_OUT)
inline fun <V> V2<V>.easeOutIn() = this.withEasing(Easings.EASE_OUT_IN)
inline fun <V> V2<V>.easeInBack() = this.withEasing(Easings.EASE_IN_BACK)
inline fun <V> V2<V>.easeOutBack() = this.withEasing(Easings.EASE_OUT_BACK)
inline fun <V> V2<V>.easeInOutBack() = this.withEasing(Easings.EASE_IN_OUT_BACK)
inline fun <V> V2<V>.easeOutInBack() = this.withEasing(Easings.EASE_OUT_IN_BACK)

inline fun <V> V2<V>.easeInElastic() = this.withEasing(Easings.EASE_IN_ELASTIC)
inline fun <V> V2<V>.easeOutElastic() = this.withEasing(Easings.EASE_OUT_ELASTIC)
inline fun <V> V2<V>.easeInOutElastic() = this.withEasing(Easings.EASE_IN_OUT_ELASTIC)
inline fun <V> V2<V>.easeOutInElastic() = this.withEasing(Easings.EASE_OUT_IN_ELASTIC)

inline fun <V> V2<V>.easeInBounce() = this.withEasing(Easings.EASE_IN_BOUNCE)
inline fun <V> V2<V>.easeOutBounce() = this.withEasing(Easings.EASE_OUT_BOUNCE)
inline fun <V> V2<V>.easeInOutBounce() = this.withEasing(Easings.EASE_IN_OUT_BOUNCE)
inline fun <V> V2<V>.easeOutInBounce() = this.withEasing(Easings.EASE_OUT_IN_BOUNCE)

inline fun <V> V2<V>.easeInQuad() = this.withEasing(Easings.EASE_IN_QUAD)
inline fun <V> V2<V>.easeOutQuad() = this.withEasing(Easings.EASE_OUT_QUAD)
inline fun <V> V2<V>.easeInOutQuad() = this.withEasing(Easings.EASE_IN_OUT_QUAD)

inline fun <V> V2<V>.easeSine() = this.withEasing(Easings.EASE_SINE)
