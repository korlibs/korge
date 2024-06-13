@file:Suppress("NOTHING_TO_INLINE")

package korlibs.korge.tween

import korlibs.datastructure.iterators.*
import korlibs.io.async.*
import korlibs.io.lang.*
import korlibs.korge.view.*
import korlibs.math.*
import korlibs.math.interpolation.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.time.*

class TweenComponent(
    val view: BaseView,
    private val vs: List<V2<*>>,
    val time: Duration = TimeSpan.NIL,
    val easing: Easing = DEFAULT_EASING,
    val callback: (Float) -> Unit,
    val c: CancellableContinuation<Unit>?,
    val waitTime: Duration = TimeSpan.NIL,
    val autoInvalidate: Boolean = true
) {
	var elapsed = 0.0.milliseconds
	val hrtime = if (time != TimeSpan.NIL) time else (vs.map { it.endTime.nanoseconds }.maxOrNull() ?: 0.0).nanoseconds
	var cancelled = false
	var done = false
    var resumed = false

    var updater: AutoCloseable? = view.onEvent(UpdateEvent) { it -> _update(it.deltaTime) }

	init {

		c?.invokeOnCancellation {
			cancelled = true
			//println("TWEEN CANCELLED[$this, $vs]: $elapsed")
		}
        _update(TimeSpan.ZERO)
	}

    private fun _update(dt: Duration) {
        if (autoInvalidate) {
            view.invalidateRender()
        }

        if (cancelled) {
            //println(" --> cancelled")
            return completeOnce()
        }
        //println("TWEEN UPDATE[$this, $vs]: $elapsed + $dt")
        elapsed += dt

        val ratio: Float = (elapsed / hrtime).toFloat().clamp(0f, 1f)
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

	fun setTo(elapsed: Duration) {
        if (elapsed == 0.milliseconds) {
            vs.fastForEach { v ->
                v.init()
            }
        }
		vs.fastForEach { v ->
			val durationInTween = v.duration.coalesce { (hrtime - v.startTime) }
			val elapsedInTween = (elapsed - v.startTime).clamp(0.0.milliseconds, durationInTween)
			val ratioInTween = if (durationInTween <= TimeSpan.ZERO || elapsedInTween >= durationInTween) 1.0 else elapsedInTween / durationInTween
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
    time: Duration = DEFAULT_TIME,
    easing: Easing = DEFAULT_EASING,
    waitTime: Duration = TimeSpan.NIL,
    timeout: Boolean = false,
    autoInvalidate: Boolean = true,
    // @TODO: We should use Ratio here as callback at some point
    callback: (Float) -> Unit = { }
) {
	if (this != null) {
		var tc: TweenComponent? = null
		try {
			withTimeoutNullable(if (timeout) time * 2 + 300.milliseconds else TimeSpan.NIL) {
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
    time: Duration = DEFAULT_TIME,
    easing: Easing = DEFAULT_EASING,
    waitTime: Duration = TimeSpan.NIL,
    callback: (Float) -> Unit = { }
): TweenComponent? {
    if (this == null) return null
    return TweenComponent(this, vs.toList(), time, easing, callback, null, waitTime)
}

suspend fun QView.tween(
    vararg vs: V2<*>,
    time: Duration = DEFAULT_TIME,
    easing: Easing = DEFAULT_EASING,
    waitTime: Duration = TimeSpan.NIL,
    callback: (Float) -> Unit = { }
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
    time: Duration = DEFAULT_TIME,
    easing: Easing = DEFAULT_EASING,
    waitTime: Duration = TimeSpan.NIL,
    callback: (Float) -> Unit = {}
): Deferred<Unit> = asyncImmediately(coroutineContext) { tween(*vs, time = time, easing = easing, waitTime = waitTime, callback = callback) }

fun BaseView?.tweenAsync(
    vararg vs: V2<*>,
    coroutineContext: CoroutineContext,
    time: Duration = DEFAULT_TIME,
    easing: Easing = DEFAULT_EASING,
    waitTime: Duration = TimeSpan.NIL,
    callback: (Float) -> Unit = {}
) = asyncImmediately(coroutineContext) { tween(*vs, time = time, easing = easing, waitTime = waitTime, callback = callback) }
