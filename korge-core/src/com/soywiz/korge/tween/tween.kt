package com.soywiz.korge.tween

import com.soywiz.korge.component.Component
import com.soywiz.korge.view.View
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.async.CancellableContinuation
import com.soywiz.korio.async.suspendCancellableCoroutine
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.util.clamp
import kotlin.reflect.KMutableProperty1

class TweenComponent(vs: List<VX<Any, *>>, val tweenObj: Any, view: View, val time: Int, val easing: Easing = Easing.LINEAR, val callback: (Double) -> Unit, val c: CancellableContinuation<Unit>) : Component(view) {
	val vs2 = vs.map { it.v2(tweenObj) }
	var elapsed = 0

	override fun update(dtMs: Int) {
		elapsed += dtMs
		val ratio = (elapsed.toDouble() / time.toDouble()).clamp(0.0, 1.0)
		val fratio = easing(ratio)
		for (v in vs2) {
			v.set(tweenObj, fratio)
		}
		callback(fratio)

		if (ratio >= 1.0) {
			dettach()
			c.resume(Unit)
		}
	}
}

suspend fun <T : View> T.tween(vararg vs: VX<T, *>, time: Int, easing: Easing = Easing.LINEAR, callback: (Double) -> Unit = { }) = suspendCancellableCoroutine<Unit> { c ->
	val view = this@tween
	view.removeComponents(TweenComponent::class.java)
	view.addComponent(TweenComponent(vs.toList() as List<VX<Any, *>>, view, view, time, easing, callback, c))
}

suspend fun <T : Any> View.tween(item: T, vararg vs: VX<T, *>, time: Int, easing: Easing = Easing.LINEAR, callback: (Double) -> Unit = { }) = suspendCancellableCoroutine<Unit> { c ->
	val view = this@tween
	view.removeComponents(TweenComponent::class.java)
	view.addComponent(TweenComponent(vs.toList() as List<VX<Any, *>>, item, view, time, easing, callback, c))
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

interface VX<T, V> {
	fun v2(obj: T): V2<T, V>
}

class V1<T, V>(val key: KMutableProperty1<T, V>, val value: V, val interpolator: (V, V, Double) -> V) : VX<T, V> {
	@Deprecated("", replaceWith = ReplaceWith("key .. value", "com.soywiz.korge.tween.rangeTo"))
	constructor(key: KMutableProperty1<T, V>, value: V) : this(key, value, ::interpolate)

	override fun v2(obj: T): V2<T, V> = V2(key, key.get(obj), value, interpolator)
	operator fun rangeTo(that: V) = V2(key, value, that, interpolator)
}

@Suppress("UNCHECKED_CAST")
class V2<T, V>(val key: KMutableProperty1<T, V>, val initial: V, val end: V, val interpolator: (V, V, Double) -> V) : VX<T, V> {
	@Deprecated("", replaceWith = ReplaceWith("key .. (initial...end)", "com.soywiz.korge.tween.rangeTo"))
	constructor(key: KMutableProperty1<T, V>, initial: V, end: V) : this(key, initial, end, ::interpolate)

	fun set(obj: T, ratio: Double) = key.set(obj, interpolator(initial, end, ratio))
	override fun v2(obj: T): V2<T, V> = this
}

operator fun <T, V> KMutableProperty1<T, V>.rangeTo(that: V) = V1(this, that, ::interpolate)
operator fun <T, V : Comparable<V>> KMutableProperty1<T, V>.rangeTo(that: ClosedRange<V>) = V2(this, that.start, that.endInclusive, ::interpolate)
operator fun <T, V> KMutableProperty1<T, V>.rangeTo(that: Pair<V, V>) = V2(this, that.first, that.second, ::interpolate)

fun <T, V> V1<T, V>.withEasing(easing: Easing): V1<T, V> = V1(this.key, this.value) { a, b, ratio -> this.interpolator(a, b, easing(ratio)) }
fun <T, V> V2<T, V>.withEasing(easing: Easing): V2<T, V> = V2(this.key, this.initial, this.end) { a, b, ratio -> this.interpolator(a, b, easing(ratio)) }

fun <T> V1<T, Int>.color(): V1<T, Int> = V1(this.key, this.value, RGBA::blendRGBA)
fun <T> V2<T, Int>.color(): V2<T, Int> = V2(this.key, this.initial, this.end, RGBA::blendRGBA)
