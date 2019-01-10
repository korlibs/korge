@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.korge.tween

import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.reflect.*

class TweenComponent(
	override val view: View,
	private val vs: List<V2<*>>,
	val time: Long? = null,
	val easing: Easing = Easing.LINEAR,
	val callback: (Double) -> Unit,
	val c: CancellableContinuation<Unit>
) : UpdateComponent {
	var elapsed = 0
	val ctime : Long = time ?: vs.map { it.endTime }.max()?.toLong() ?: 1000L
	var cancelled = false
	var done = false

	init {
		c.invokeOnCancellation {
			cancelled = true
			//println("TWEEN CANCELLED[$this, $vs]: $elapsed")
		}
		update(0.0)
	}

	fun completeOnce() {
		if (!done) {
			done = true
			detach()
			c.resume(Unit)
			//println("TWEEN COMPLETED[$this, $vs]: $elapsed")
		}
	}

	override fun update(ms: Double) {
		val dtMs = ms.toInt()
		//println("TWEEN UPDATE[$this, $vs]: $elapsed + $dtMs")
		if (cancelled) {
			//println(" --> cancelled")
			return completeOnce()
		}
		elapsed += dtMs

		val ratio = (elapsed.toDouble() / ctime.toDouble()).clamp(0.0, 1.0)
		for (v in vs) {
			val durationInTween = (v.duration ?: (ctime - v.startTime))
			val elapsedInTween = (elapsed - v.startTime).clamp(0L, durationInTween)
			val ratioInTween =
				if (durationInTween <= 0.0) 1.0 else elapsedInTween.toDouble() / durationInTween.toDouble()
			v.set(easing(ratioInTween))
		}
		callback(easing(ratio))

		if (ratio >= 1.0) {
			//println(" --> completed")
			return completeOnce()
		}
	}

	override fun toString(): String = "TweenComponent($view)"
}

private val emptyCallback: (Double) -> Unit = {}

suspend fun View?.tween(
	vararg vs: V2<*>,
	time: TimeSpan,
	easing: Easing = Easing.LINEAR,
	callback: (Double) -> Unit = emptyCallback
): Unit {
	if (this != null) {
		withTimeout(300 + time.millisecondsLong * 2) {
			suspendCancellableCoroutine<Unit> { c ->
				val view = this@tween
				//println("STARTED TWEEN at thread $currentThreadId")
				TweenComponent(view, vs.toList(), time.millisecondsLong, easing, callback, c).attach()
			}
		}
	}
}

suspend fun View.show(time: TimeSpan, easing: Easing = Easing.LINEAR) =
	tween(this::alpha[1.0], time = time, easing = easing) { this.visible = true }

suspend fun View.hide(time: TimeSpan, easing: Easing = Easing.LINEAR) =
	tween(this::alpha[0.0], time = time, easing = easing)

suspend inline fun View.moveTo(x: Number, y: Number, time: TimeSpan, easing: Easing = Easing.LINEAR) =
	tween(this::x[x.toDouble()], this::y[y.toDouble()], time = time, easing = easing)

suspend inline fun View.moveBy(dx: Number, dy: Number, time: TimeSpan, easing: Easing = Easing.LINEAR) =
	tween(this::x[this.x + dx.toDouble()], this::y[this.y + dy.toDouble()], time = time, easing = easing)

suspend inline fun View.scaleTo(sx: Number, sy: Number, time: TimeSpan, easing: Easing = Easing.LINEAR) =
	tween(this::scaleX[sx.toDouble()], this::scaleY[sy.toDouble()], time = time, easing = easing)

suspend inline fun View.rotateTo(deg: Angle, time: TimeSpan, easing: Easing = Easing.LINEAR) =
	tween(this::rotationRadians[deg.radians], time = time, easing = easing)

suspend inline fun View.rotateBy(ddeg: Angle, time: TimeSpan, easing: Easing = Easing.LINEAR) =
	tween(this::rotationRadians[this.rotationRadians + ddeg.radians], time = time, easing = easing)

@Suppress("UNCHECKED_CAST")
data class V2<V>(
	internal val key: KMutableProperty0<V>,
	internal val initial: V,
	internal val end: V,
	internal val interpolator: (Double, V, V) -> V,
	internal val startTime: Long = 0,
	internal val duration: Long? = null
) {
	val endTime = startTime + (duration ?: 0)

	@Deprecated("", replaceWith = ReplaceWith("key .. (initial...end)", "com.soywiz.korge.tween.rangeTo"))
	constructor(key: KMutableProperty0<V>, initial: V, end: V) : this(key, initial, end, ::_interpolateAny)

	fun set(ratio: Double) = key.set(interpolator(ratio, initial, end))

	override fun toString(): String =
		"V2(key=${key.name}, range=[$initial-$end], startTime=$startTime, duration=$duration)"
}

operator fun <V> KMutableProperty0<V>.get(end: V) = V2(this, this.get(), end, ::_interpolateAny)
operator fun <V> KMutableProperty0<V>.get(initial: V, end: V) = V2(this, initial, end, ::_interpolateAny)

@PublishedApi
internal fun _interpolate(ratio: Double, l: Double, r: Double) = ratio.interpolate(l, r)

@PublishedApi
internal fun _interpolateFloat(ratio: Double, l: Float, r: Float) = ratio.interpolate(l, r)

@PublishedApi
internal fun <V> _interpolateAny(ratio: Double, l: V, r: V) = ratio.interpolateAny(l, r)

@PublishedApi
internal fun _interpolateColor(ratio: Double, l: Int, r: Int): Int = RGBA.blendRGBAInt(l, r, ratio)

//inline operator fun KMutableProperty0<Float>.get(end: Number) = V2(this, this.get(), end.toFloat(), ::_interpolateFloat)
//inline operator fun KMutableProperty0<Float>.get(initial: Number, end: Number) =
//	V2(this, initial.toFloat(), end.toFloat(), ::_interpolateFloat)

inline operator fun KMutableProperty0<Double>.get(end: Number) = V2(this, this.get(), end.toDouble(), ::_interpolate)
inline operator fun KMutableProperty0<Double>.get(initial: Number, end: Number) =
	V2(this, initial.toDouble(), end.toDouble(), ::_interpolate)

fun V2<Int>.color(): V2<Int> = this.copy(interpolator = ::_interpolateColor)

fun <V> V2<V>.easing(easing: Easing): V2<V> =
	this.copy(interpolator = { ratio, a, b -> this.interpolator(easing(ratio), a, b) })

inline fun <V> V2<V>.delay(startTime: TimeSpan) = this.copy(startTime = startTime.millisecondsLong)
inline fun <V> V2<V>.duration(duration: TimeSpan) = this.copy(duration = duration.millisecondsLong)

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
