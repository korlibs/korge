package com.soywiz.korge.time

import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.klock.hr.*
import com.soywiz.korge.component.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.view.*
import com.soywiz.korio.lang.*
import kotlinx.coroutines.*
import kotlin.collections.*
import kotlin.coroutines.*

private typealias TimerCallback = (HRTimeSpan) -> Unit

inline class TimerRef(val ref: Double)

class TimerComponents(override val view: View) : UpdateComponent {
    private val _timers = arrayListOf<(HRTimeSpan) -> Unit>()

    override fun update(dt: HRTimeSpan) {
        _timers.fastForEach { it(dt) }
    }

    private fun addTimer(callback: (HRTimeSpan) -> Unit) {
        _timers += callback
    }

    private fun removeTimer(callback: (HRTimeSpan) -> Unit) {
        _timers -= callback
    }

    suspend fun wait(time: TimeSpan): Unit = wait(time.hr)
    suspend fun wait(time: HRTimeSpan): Unit = suspendCancellableCoroutine { c -> timeout(time) { c.resume(Unit) } }

    suspend fun waitFrame() = suspendCoroutine<Unit> { c ->
        lateinit var timer: TimerCallback
        timer = {
            removeTimer(timer)
            c.resume(Unit)
        }
        addTimer(timer)
    }

    suspend fun waitMilliseconds(time: Double): Unit = wait(time.hrMilliseconds)

    fun waitMilliseconds(time: Double, callback: () -> Unit = {}): Closeable = timeoutMs(time, callback)

    private fun _interval(time: HRTimeSpan, repeat: Boolean, callback: () -> Unit = {}): Closeable {
        lateinit var timer: TimerCallback
        var elapsed = 0.hrNanoseconds
        timer = { deltaMs ->
            elapsed += deltaMs
            while (elapsed >= time) {
                if (!repeat) removeTimer(timer)
                elapsed -= time
                callback()
                if (!repeat) break
            }
        }
        addTimer(timer)
        return Closeable { removeTimer(timer) }
    }

    fun timeout(time: HRTimeSpan, callback: () -> Unit = {}): Closeable = _interval(time, false, callback)
    fun interval(time: HRTimeSpan, callback: () -> Unit = {}): Closeable = _interval(time, true, callback)

    fun timeoutMs(time: Double, callback: () -> Unit = {}): Closeable = timeout(time.hrMilliseconds, callback)
    fun intervalMs(time: Double, callback: () -> Unit = {}): Closeable = interval(time.hrMilliseconds, callback)
}

val View.timers get() = this.getOrCreateComponentUpdate<TimerComponents> { TimerComponents(this) }

fun View.timeout(time: TimeSpan, callback: () -> Unit): Closeable = this.timers.timeoutMs(time.milliseconds, callback)
fun View.interval(time: TimeSpan, callback: () -> Unit): Closeable = this.timers.intervalMs(time.milliseconds, callback)
suspend fun View.delay(time: TimeSpan) = this.timers.wait(time)
suspend fun View.delayFrame() = this.timers.waitFrame()
