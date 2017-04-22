package com.soywiz.korge.tween

import com.soywiz.korge.component.Component
import com.soywiz.korge.view.View
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.async.CancellableContinuation
import com.soywiz.korio.async.suspendCancellableCoroutine
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.util.clamp
import kotlin.reflect.KMutableProperty0

class TweenComponent(private val vs: List<V2<*>>, view: View, val time: Int, val easing: Easing = Easing.LINEAR, val callback: (Double) -> Unit, val c: CancellableContinuation<Unit>) : Component(view) {
	var elapsed = 0

	override fun update(dtMs: Int) {
		elapsed += dtMs
		val ratio = (elapsed.toDouble() / time.toDouble()).clamp(0.0, 1.0)
		val fratio = easing(ratio)
		for (v in vs) {
			v.set(fratio)
		}
		callback(fratio)

		if (ratio >= 1.0) {
			dettach()
			c.resume(Unit)
		}
	}
}

suspend fun View?.tween(vararg vs: V2<*>, time: Int, easing: Easing = Easing.LINEAR, callback: (Double) -> Unit = { }) = suspendCancellableCoroutine<Unit> { c ->
	val view = this@tween
	view?.removeComponents(TweenComponent::class.java)
	view?.addComponent(TweenComponent(vs.toList(), view, time, easing, callback, c))
}

fun interpolate(v0: Int, v1: Int, step: Double): Int = (v0 * (1 - step) + v1 * step).toInt()
fun interpolate(v0: Long, v1: Long, step: Double): Long = (v0 * (1 - step) + v1 * step).toLong()
fun interpolate(v0: Double, v1: Double, step: Double): Double = v0 * (1 - step) + v1 * step

fun <T> interpolate(min: T, max: T, ratio: Double): T = when (min) {
	is Int -> interpolate(min, max as Int, ratio) as T
	is Long -> interpolate(min, max as Long, ratio) as T
	is Double -> interpolate(min, max as Double, ratio) as T
	else -> invalidOp
}

@Suppress("UNCHECKED_CAST")
class V2<V>(val key: KMutableProperty0<V>, val initial: V, val end: V, val interpolator: (V, V, Double) -> V) {
	@Deprecated("", replaceWith = ReplaceWith("key .. (initial...end)", "com.soywiz.korge.tween.rangeTo"))
	constructor(key: KMutableProperty0<V>, initial: V, end: V) : this(key, initial, end, ::interpolate)

	fun set(ratio: Double) = key.set(interpolator(initial, end, ratio))
}

operator fun <V> V2<V>.rangeTo(that: V) = V2(this.key, this.end, that, this.interpolator)

operator fun <V> KMutableProperty0<V>.rangeTo(that: V) = V2(this, this.get(), that, ::interpolate)
operator fun <V : Comparable<V>> KMutableProperty0<V>.rangeTo(that: ClosedRange<V>) = V2(this, that.start, that.endInclusive, ::interpolate)
operator fun <V> KMutableProperty0<V>.rangeTo(that: Pair<V, V>) = V2(this, that.first, that.second, ::interpolate)

fun <V> V2<V>.withEasing(easing: Easing): V2<V> = V2(this.key, this.initial, this.end) { a, b, ratio -> this.interpolator(a, b, easing(ratio)) }

fun V2<Int>.color(): V2<Int> = V2(this.key, this.initial, this.end, RGBA::blendRGBA)

fun <V> V2<V>.easing(easing: Easing): V2<V> = V2(this.key, this.initial, this.end) { a, b, ratio -> this.interpolator(a, b, easing(ratio)) }

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
