@file:Suppress("NOTHING_TO_INLINE")

package korlibs.korge.tween

import korlibs.datastructure.iterators.*
import korlibs.time.*
import korlibs.memory.*
import korlibs.korge.view.*
import korlibs.io.async.*
import korlibs.io.lang.*
import korlibs.math.interpolation.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

class TweenComponent(
    val view: BaseView,
    private val vs: List<V2<*>>,
    val time: TimeSpan = TimeSpan.NIL,
    val easing: Easing = DEFAULT_EASING,
    val callback: (Double) -> Unit,
    val c: CancellableContinuation<Unit>?,
    val waitTime: TimeSpan = TimeSpan.NIL,
    val autoInvalidate: Boolean = true
) {
	var elapsed = 0.0.milliseconds
	val hrtime = if (time != TimeSpan.NIL) time else (vs.map { it.endTime.nanoseconds }.maxOrNull() ?: 0.0).nanoseconds
	var cancelled = false
	var done = false
    var resumed = false

    var updater: Closeable? = view.onEvent(UpdateEvent) { it -> _update(it.deltaTime) }

	init {

		c?.invokeOnCancellation {
			cancelled = true
			//println("TWEEN CANCELLED[$this, $vs]: $elapsed")
		}
        _update(TimeSpan.ZERO)
	}

    private fun _update(dt: TimeSpan) {
        if (autoInvalidate) {
            view.invalidateRender()
        }

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

    fun resumeOnce() {
        if (resumed) return
        resumed = true
        c?.resume(Unit)
    }

	fun completeOnce() {
        if (done) return
        done = true
        updater?.close()
        updater = null
        resumeOnce()
        //println("TWEEN COMPLETED[$this, $vs]: $elapsed")
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
			v.set(easedRatioInTween.toRatio())
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
    autoInvalidate: Boolean = true,
    callback: (Double) -> Unit = { }
) {
	if (this != null) {
		var tc: TweenComponent? = null
		try {
			withTimeout(if (timeout) time * 2 + 300.milliseconds else TimeSpan.NIL) {
				suspendCancellableCoroutine<Unit> { c ->
					val view = this@tween
					//println("STARTED TWEEN at thread $currentThreadId")
					tc = TweenComponent(view, vs.toList(), time, easing, callback, c, waitTime, autoInvalidate)
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
    return TweenComponent(this, vs.toList(), time, easing, callback, null, waitTime)
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
): Deferred<Unit> = asyncImmediately(coroutineContext) { tween(*vs, time = time, easing = easing, waitTime = waitTime, callback = callback) }

fun BaseView?.tweenAsync(
	vararg vs: V2<*>,
	coroutineContext: CoroutineContext,
	time: TimeSpan = DEFAULT_TIME,
	easing: Easing = DEFAULT_EASING,
    waitTime: TimeSpan = TimeSpan.NIL,
    callback: (Double) -> Unit = {}
) = asyncImmediately(coroutineContext) { tween(*vs, time = time, easing = easing, waitTime = waitTime, callback = callback) }
