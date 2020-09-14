@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.korge.tween

import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.klock.hr.*
import com.soywiz.kmem.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.reflect.*

class TweenComponent(
        override val view: View,
        private val vs: List<V2<*>>,
        val time: HRTimeSpan = HRTimeSpan.NIL,
        val easing: Easing = DEFAULT_EASING,
        val callback: (Double) -> Unit,
        val c: CancellableContinuation<Unit>
) : UpdateComponentV2 {
	var elapsed = 0.hrNanoseconds
	val hrtime = if (time != HRTimeSpan.NIL) time else (vs.map { it.endTime.nanosecondsDouble }.max() ?: 0.0).hrNanoseconds
	var cancelled = false
	var done = false

	init {
		c.invokeOnCancellation {
			cancelled = true
			//println("TWEEN CANCELLED[$this, $vs]: $elapsed")
		}
		update(0.hrNanoseconds)
	}

	fun completeOnce() {
		if (!done) {
			done = true
			detach()
			c.resume(Unit)
			//println("TWEEN COMPLETED[$this, $vs]: $elapsed")
		}
	}

	override fun update(dt: HRTimeSpan) {
        if (cancelled) {
            //println(" --> cancelled")
            return completeOnce()
        }
		//println("TWEEN UPDATE[$this, $vs]: $elapsed + $dtMs")
		elapsed += dt

		val ratio = (elapsed / hrtime).clamp(0.0, 1.0)
        //println("$elapsed/$hrtime : $ratio")
		setTo(elapsed)
		callback(easing(ratio))

        //println("UPDATE! : dt=${dt.timeSpan} : ratio=$ratio")

        if (ratio >= 1.0) {
			//println(" --> completed")
			return completeOnce()
		}
	}

    @Deprecated("")
    fun setToMs(elapsed: Int) = setTo(elapsed.hrMilliseconds)

	fun setTo(elapsed: HRTimeSpan) {
        if (elapsed == 0.hrNanoseconds) {
            vs.fastForEach { v ->
                v.init()
            }
        }
		vs.fastForEach { v ->
			val durationInTween = v.duration.coalesce { (hrtime - v.startTime) }
			val elapsedInTween = (elapsed - v.startTime).clamp(0.hrNanoseconds, durationInTween)
			val ratioInTween = if (durationInTween <= 0.hrNanoseconds || elapsedInTween >= durationInTween) 1.0 else elapsedInTween / durationInTween
            val easedRatioInTween = easing(ratioInTween)
            //println("easedRatioInTween: $easedRatioInTween")
			v.set(easedRatioInTween)
		}
	}

	override fun toString(): String = "TweenComponent($view)"
}

private val emptyCallback: (Double) -> Unit = {}

suspend fun View?.tween(
	vararg vs: V2<*>,
	time: TimeSpan = DEFAULT_TIME,
	easing: Easing = DEFAULT_EASING,
	callback: (Double) -> Unit = emptyCallback
): Unit {
	if (this != null) {
		var tc: TweenComponent? = null
		try {
			withTimeout(300 + time.millisecondsLong * 2) {
				suspendCancellableCoroutine<Unit> { c ->
					val view = this@tween
					//println("STARTED TWEEN at thread $currentThreadId")
					tc = TweenComponent(view, vs.toList(), time.hr, easing, callback, c).also { it.attach() }
				}
			}
		} catch (e: TimeoutCancellationException) {
			tc?.setTo(time.hr)
		}
	}
}

@PublishedApi
internal val DEFAULT_EASING = Easing.EASE_IN_OUT_QUAD

@PublishedApi
internal val DEFAULT_TIME = 1.seconds

suspend fun View?.tweenAsync(
	vararg vs: V2<*>,
	time: TimeSpan = DEFAULT_TIME,
	easing: Easing = DEFAULT_EASING,
	callback: (Double) -> Unit = emptyCallback
) = asyncImmediately(coroutineContext) { tween(*vs, time = time, easing = easing, callback = callback) }

fun View?.tweenAsync(
	vararg vs: V2<*>,
	coroutineContext: CoroutineContext,
	time: TimeSpan = DEFAULT_TIME,
	easing: Easing = DEFAULT_EASING,
	callback: (Double) -> Unit = emptyCallback
) = asyncImmediately(coroutineContext) { tween(*vs, time = time, easing = easing, callback = callback) }

suspend fun View.show(time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) =
	tween(this::alpha[1.0], time = time, easing = easing) { this.visible = true }

suspend fun View.hide(time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) =
	tween(this::alpha[0.0], time = time, easing = easing)

suspend inline fun View.moveTo(x: Double, y: Double, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) = tween(this::x[x], this::y[y], time = time, easing = easing)
suspend inline fun View.moveBy(dx: Double, dy: Double, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) = tween(this::x[this.x + dx], this::y[this.y + dy], time = time, easing = easing)
suspend inline fun View.scaleTo(sx: Double, sy: Double, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) = tween(this::scaleX[sx], this::scaleY[sy], time = time, easing = easing)

@Deprecated("Kotlin/Native boxes inline+Number")
suspend inline fun View.moveTo(x: Number, y: Number, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) = moveTo(x.toDouble(), y.toDouble(), time, easing)
@Deprecated("Kotlin/Native boxes inline+Number")
suspend inline fun View.moveBy(dx: Number, dy: Number, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) = moveBy(dx.toDouble(), dy.toDouble(), time, easing)
@Deprecated("Kotlin/Native boxes inline+Number")
suspend inline fun View.scaleTo(sx: Number, sy: Number, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) = scaleTo(sx.toDouble(), sy.toDouble(), time, easing)

suspend inline fun View.rotateTo(deg: Angle, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) =
	tween(this::rotationRadians[deg.radians], time = time, easing = easing)

suspend inline fun View.rotateBy(ddeg: Angle, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) =
	tween(this::rotationRadians[this.rotationRadians + ddeg.radians], time = time, easing = easing)

@Suppress("UNCHECKED_CAST")
data class V2<V>(
	val key: KMutableProperty0<V>,
	var initial: V,
	val end: V,
	val interpolator: (Double, V, V) -> V,
    val includeStart: Boolean,
	val startTime: HRTimeSpan = 0.hrNanoseconds,
	val duration: HRTimeSpan = HRTimeSpan.NIL
) {
	val endTime = startTime + duration.coalesce { 0.hrNanoseconds }

    fun init() {
        if (!includeStart) {
            initial = key.get()
        }
    }
	fun set(ratio: Double) = key.set(interpolator(ratio, initial, end))

	override fun toString(): String =
		"V2(key=${key.name}, range=[$initial-$end], startTime=$startTime, duration=$duration)"
}

operator fun KMutableProperty0<Int>.get(end: Int) = V2(this, this.get(), end, ::_interpolateInt, includeStart = false)
operator fun KMutableProperty0<Int>.get(initial: Int, end: Int) = V2(this, initial, end, ::_interpolateInt, includeStart = true)

operator fun <V : Interpolable<V>> KMutableProperty0<V>.get(end: V) = V2(this, this.get(), end, ::_interpolateInterpolable, includeStart = false)
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
internal fun <V> _interpolateAny(ratio: Double, l: V, r: V) = ratio.interpolateAny(l, r)

@PublishedApi
internal fun _interpolateColor(ratio: Double, l: RGBA, r: RGBA): RGBA = RGBA.mixRgba(l, r, ratio)

@PublishedApi
internal fun _interpolateAngle(ratio: Double, l: Angle, r: Angle): Angle= _interpolateAngleAny(ratio, l, r, minimizeAngle = true)

@PublishedApi
internal fun _interpolateAngleDenormalized(ratio: Double, l: Angle, r: Angle): Angle= _interpolateAngleAny(ratio, l, r, minimizeAngle = false)

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

@Deprecated("Kotlin/Native boxes inline+Number")
inline operator fun KMutableProperty0<Double>.get(end: Number) = get(end.toDouble())
@Deprecated("Kotlin/Native boxes inline+Number")
inline operator fun KMutableProperty0<Double>.get(initial: Number, end: Number) = get(initial.toDouble(), end.toDouble())

inline operator fun KMutableProperty0<RGBA>.get(end: RGBA) = V2(this, this.get(), end, ::_interpolateColor, includeStart = false)
inline operator fun KMutableProperty0<RGBA>.get(initial: RGBA, end: RGBA) =
	V2(this, initial, end, ::_interpolateColor, includeStart = true)

inline operator fun KMutableProperty0<Angle>.get(end: Angle) = V2(this, this.get(), end, ::_interpolateAngle, includeStart = false)
inline operator fun KMutableProperty0<Angle>.get(initial: Angle, end: Angle) =
	V2(this, initial, end, ::_interpolateAngle, includeStart = true)

fun V2<Angle>.denormalized(): V2<Angle> = this.copy(interpolator = ::_interpolateAngleDenormalized)

inline operator fun KMutableProperty0<TimeSpan>.get(end: TimeSpan) = V2(this, this.get(), end, ::_interpolateTimeSpan, includeStart = false)
inline operator fun KMutableProperty0<TimeSpan>.get(initial: TimeSpan, end: TimeSpan) =
    V2(this, initial, end, ::_interpolateTimeSpan, includeStart = true)

fun <V> V2<V>.easing(easing: Easing): V2<V> =
	this.copy(interpolator = { ratio, a, b -> this.interpolator(easing(ratio), a, b) })

inline fun <V> V2<V>.delay(startTime: TimeSpan) = this.copy(startTime = startTime.hr)
inline fun <V> V2<V>.duration(duration: TimeSpan) = this.copy(duration = duration.hr)

inline fun <V> V2<V>.delay(startTime: HRTimeSpan) = this.copy(startTime = startTime)
inline fun <V> V2<V>.duration(duration: HRTimeSpan) = this.copy(duration = duration)

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
