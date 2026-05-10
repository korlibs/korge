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
    val fastTime: FastDuration = FastDuration.NaN,
    val easing: Easing = DEFAULT_EASING,
    val callback: (Float) -> Unit,
    val c: CancellableContinuation<Unit>?,
    val fastWaitTime: FastDuration = FastDuration.NaN,
    val autoInvalidate: Boolean = true
) {
    constructor(
        view: BaseView,
        vs: List<V2<*>>,
        time: Duration,
        easing: Easing = DEFAULT_EASING,
        callback: (Float) -> Unit,
        c: CancellableContinuation<Unit>?,
        waitTime: Duration = Duration.NIL,
        autoInvalidate: Boolean = true
    ) : this(view, vs, time.fast, easing, callback, c, waitTime.fast, autoInvalidate)

    val waitTime get() = fastWaitTime.slow
    val time: Duration get() = fastTime.slow
	var elapsed = FastDuration.ZERO
	val hrtime = if (fastTime != FastDuration.NaN) fastTime else (vs.map { it.fastEndTime.nanoseconds }.maxOrNull() ?: 0.0).fastNanoseconds
	var cancelled = false
	var done = false
    var resumed = false

    var updater: AutoCloseable? = view.onEvent(UpdateEvent) { it -> _update(it.fastDeltaTime) }

	init {

		c?.invokeOnCancellation {
			cancelled = true
			//println("TWEEN CANCELLED[$this, $vs]: $elapsed")
		}
        _update(FastDuration.ZERO)
	}

    private fun _update(dt: FastDuration) {
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

    fun setTo(elapsed: Duration) = setTo(elapsed.fast)

	fun setTo(elapsed: FastDuration) {
        if (elapsed == FastDuration.ZERO) {
            vs.fastForEach { v ->
                v.init()
            }
        }
		vs.fastForEach { v ->
			val durationInTween = if (v.fastDuration == FastDuration.NaN) (hrtime - v.fastStartTime) else v.fastDuration
			val elapsedInTween = (elapsed - v.fastStartTime).fastMilliseconds.clamp(0.0, durationInTween.fastMilliseconds).fastMilliseconds
			val ratioInTween = if (durationInTween <= Duration.ZERO || elapsedInTween >= durationInTween) 1.0 else elapsedInTween / durationInTween
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
    waitTime: Duration = Duration.NIL,
    timeout: Boolean = false,
    autoInvalidate: Boolean = true,
    // @TODO: We should use Ratio here as callback at some point
    callback: (Float) -> Unit = { }
): Unit = tween(*vs, time = time.fast, easing = easing, waitTime = waitTime.fast, timeout = timeout, autoInvalidate = autoInvalidate, callback = callback)

fun BaseView?.tweenNoWait(
    vararg vs: V2<*>,
    time: Duration = DEFAULT_TIME,
    easing: Easing = DEFAULT_EASING,
    waitTime: Duration = TimeSpan.NIL,
    callback: (Float) -> Unit = { }
): TweenComponent? {
    if (this == null) return null
    return TweenComponent(this, vs.toList(), time.fast, easing, callback, null, waitTime.fast)
}

suspend fun QView.tween(
    vararg vs: V2<*>,
    time: Duration,
    easing: Easing = DEFAULT_EASING,
    waitTime: Duration = Duration.NIL,
    callback: (Float) -> Unit = { }
): Unit = tween(*vs, time = time.fast, easing = easing, waitTime = waitTime.fast, callback = callback)

suspend fun BaseView?.tweenAsync(
    vararg vs: V2<*>,
    time: Duration,
    easing: Easing = DEFAULT_EASING,
    waitTime: Duration = Duration.NIL,
    callback: (Float) -> Unit = {}
): Deferred<Unit> = tweenAsync(*vs, coroutineContext = coroutineContext, time = time.fast, easing = easing, waitTime = waitTime.fast, callback = callback)

fun BaseView?.tweenAsync(
    vararg vs: V2<*>,
    coroutineContext: CoroutineContext,
    time: Duration,
    easing: Easing = DEFAULT_EASING,
    waitTime: Duration = Duration.NIL,
    callback: (Float) -> Unit = {}
): Deferred<Unit> = tweenAsync(*vs, coroutineContext = coroutineContext, time = time.fast, easing = easing, waitTime = waitTime.fast, callback = callback)






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
    time: FastDuration = DEFAULT_FTIME,
    easing: Easing = DEFAULT_EASING,
    waitTime: FastDuration = FastDuration.NaN,
    timeout: Boolean = false,
    autoInvalidate: Boolean = true,
    // @TODO: We should use Ratio here as callback at some point
    callback: (Float) -> Unit = { }
) {
    if (this != null) {
        var tc: TweenComponent? = null
        try {
            withTimeoutNullable(if (timeout) time * 2 + 300.fastMilliseconds else FastDuration.NaN) {
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
    time: FastDuration = DEFAULT_FTIME,
    easing: Easing = DEFAULT_EASING,
    waitTime: FastDuration = FastDuration.NaN,
    callback: (Float) -> Unit = { }
): TweenComponent? {
    if (this == null) return null
    return TweenComponent(this, vs.toList(), time, easing, callback, null, waitTime)
}

suspend fun QView.tween(
    vararg vs: V2<*>,
    time: FastDuration = DEFAULT_FTIME,
    easing: Easing = DEFAULT_EASING,
    waitTime: FastDuration = FastDuration.NaN,
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

suspend fun BaseView?.tweenAsync(
    vararg vs: V2<*>,
    time: FastDuration = DEFAULT_FTIME,
    easing: Easing = DEFAULT_EASING,
    waitTime: FastDuration = FastDuration.NaN,
    callback: (Float) -> Unit = {}
): Deferred<Unit> = asyncImmediately(coroutineContext) { tween(*vs, time = time, easing = easing, waitTime = waitTime, callback = callback) }

fun BaseView?.tweenAsync(
    vararg vs: V2<*>,
    coroutineContext: CoroutineContext,
    time: FastDuration = DEFAULT_FTIME,
    easing: Easing = DEFAULT_EASING,
    waitTime: FastDuration = FastDuration.NaN,
    callback: (Float) -> Unit = {}
) = asyncImmediately(coroutineContext) { tween(*vs, time = time, easing = easing, waitTime = waitTime, callback = callback) }


@PublishedApi
internal val DEFAULT_EASING = Easing.EASE_IN_OUT_QUAD

@PublishedApi
internal val DEFAULT_TIME = 1.seconds

@PublishedApi
internal val DEFAULT_FTIME = 1.fastSeconds

private suspend fun <T> withTimeoutNullable(time: FastDuration?, block: suspend CoroutineScope.() -> T): T {
    return if (time == null || time == FastDuration.NaN) {
        block(CoroutineScope(coroutineContext))
    } else {
        withTimeout(time.fastMilliseconds.toLongRound(), block)
    }
}
