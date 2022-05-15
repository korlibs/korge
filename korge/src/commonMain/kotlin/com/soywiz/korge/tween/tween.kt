@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.korge.tween

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.clamp
import com.soywiz.klock.coalesce
import com.soywiz.klock.milliseconds
import com.soywiz.klock.nanoseconds
import com.soywiz.klock.seconds
import com.soywiz.kmem.clamp
import com.soywiz.korge.baseview.BaseView
import com.soywiz.korge.component.UpdateComponent
import com.soywiz.korge.component.attach
import com.soywiz.korge.component.detach
import com.soywiz.korge.view.QView
import com.soywiz.korio.async.asyncImmediately
import com.soywiz.korio.async.delay
import com.soywiz.korio.async.withTimeout
import com.soywiz.korma.interpolation.Easing
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

class TweenComponent(
    override val view: BaseView,
    private val vs: List<V2<*>>,
    val time: TimeSpan = TimeSpan.NIL,
    val easing: Easing = DEFAULT_EASING,
    val callback: (Double) -> Unit,
    val c: CancellableContinuation<Unit>?,
    val waitTime: TimeSpan = TimeSpan.NIL
) : UpdateComponent {
	var elapsed = 0.0.milliseconds
	val hrtime = if (time != TimeSpan.NIL) time else (vs.map { it.endTime.nanoseconds }.maxOrNull() ?: 0.0).nanoseconds
	var cancelled = false
	var done = false
    var resumed = false

	init {
		c?.invokeOnCancellation {
			cancelled = true
			//println("TWEEN CANCELLED[$this, $vs]: $elapsed")
		}
		update(0.0.milliseconds)
	}

    fun resumeOnce() {
        if (resumed) return
        resumed = true
        c?.resume(Unit)
    }

	fun completeOnce() {
        if (done) return
        done = true
        detach()
        resumeOnce()
        //println("TWEEN COMPLETED[$this, $vs]: $elapsed")
    }

	override fun update(dt: TimeSpan) {
        if (cancelled) {
            //println(" --> cancelled")
            return completeOnce()
        }
		//println("TWEEN UPDATE[$this, $vs]: $elapsed + $dt")
		elapsed += dt

		val ratio = (elapsed / hrtime).clamp(0.0, 1.0)
        //println("$elapsed/$hrtime : $ratio")
		setTo(elapsed)
		callback(easing(ratio))

        if (waitTime != TimeSpan.NIL && elapsed >= waitTime) {
            resumeOnce()
        }

        //println("UPDATE! : dt=${dt.timeSpan} : ratio=$ratio")

        if (ratio >= 1.0) {
			//println(" --> completed")
			return completeOnce()
		}
	}

	fun setTo(elapsed: TimeSpan) {
        if (elapsed == 0.milliseconds) {
            vs.fastForEach { v ->
                v.init()
            }
        }
		vs.fastForEach { v ->
			val durationInTween = v.duration.coalesce { (hrtime - v.startTime) }
			val elapsedInTween = (elapsed - v.startTime).clamp(0.0.milliseconds, durationInTween)
			val ratioInTween = if (durationInTween <= 0.0.milliseconds || elapsedInTween >= durationInTween) 1.0 else elapsedInTween / durationInTween
            val easedRatioInTween = easing(ratioInTween)
            //println("easedRatioInTween: $easedRatioInTween, ratioInTween: $ratioInTween, durationInTween: $durationInTween, elapsedInTween: $elapsedInTween, elapsed: $elapsed")
			v.set(easedRatioInTween)
		}
	}

	override fun toString(): String = "TweenComponent($view)"
}

/**
 * Creates a tween that will take a specified [time] to execute,
 * with an optional [easing].
 *
 * If [waitTime] is specified, the suspending function will wait as much as [time] or [waitTime] even if it is
 * still executing.
 *
 * Once completed [callback] will be executed.
 */
suspend fun BaseView?.tween(
    vararg vs: V2<*>,
    time: TimeSpan = DEFAULT_TIME,
    easing: Easing = DEFAULT_EASING,
    waitTime: TimeSpan = TimeSpan.NIL,
    timeout: Boolean = false,
    callback: (Double) -> Unit = { }
) {
	if (this != null) {
		var tc: TweenComponent? = null
		try {
			withTimeout(if (timeout) time * 2 + 300.milliseconds else TimeSpan.NIL) {
				suspendCancellableCoroutine<Unit> { c ->
					val view = this@tween
					//println("STARTED TWEEN at thread $currentThreadId")
					tc = TweenComponent(view, vs.toList(), time, easing, callback, c, waitTime).also { it.attach() }
				}
			}
		} catch (e: TimeoutCancellationException) {
			tc?.setTo(time)
		}
	}
}

fun BaseView?.tweenNoWait(
    vararg vs: V2<*>,
    time: TimeSpan = DEFAULT_TIME,
    easing: Easing = DEFAULT_EASING,
    waitTime: TimeSpan = TimeSpan.NIL,
    callback: (Double) -> Unit = { }
): TweenComponent? {
    if (this == null) return null
    return TweenComponent(this, vs.toList(), time, easing, callback, null, waitTime).also { it.attach() }
}

suspend fun QView.tween(
    vararg vs: V2<*>,
    time: TimeSpan = DEFAULT_TIME,
    easing: Easing = DEFAULT_EASING,
    waitTime: TimeSpan = TimeSpan.NIL,
    callback: (Double) -> Unit = { }
) {
    if (isEmpty()) {
        // @TODO: Do this?
        delay(time)
    } else {
        fastForEach {
            it.tween(*vs, time = time, easing = easing, waitTime = waitTime, callback = callback)
        }
    }
}

@PublishedApi
internal val DEFAULT_EASING = Easing.EASE_IN_OUT_QUAD

@PublishedApi
internal val DEFAULT_TIME = 1.seconds

suspend fun BaseView?.tweenAsync(
	vararg vs: V2<*>,
	time: TimeSpan = DEFAULT_TIME,
	easing: Easing = DEFAULT_EASING,
    waitTime: TimeSpan = TimeSpan.NIL,
	callback: (Double) -> Unit = {}
) = asyncImmediately(coroutineContext) { tween(*vs, time = time, easing = easing, waitTime = waitTime, callback = callback) }

fun BaseView?.tweenAsync(
	vararg vs: V2<*>,
	coroutineContext: CoroutineContext,
	time: TimeSpan = DEFAULT_TIME,
	easing: Easing = DEFAULT_EASING,
    waitTime: TimeSpan = TimeSpan.NIL,
    callback: (Double) -> Unit = {}
) = asyncImmediately(coroutineContext) { tween(*vs, time = time, easing = easing, waitTime = waitTime, callback = callback) }
