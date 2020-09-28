package com.soywiz.korge.time

import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korio.lang.*
import kotlinx.coroutines.*
import kotlin.collections.*
import kotlin.coroutines.*

private typealias TimerCallback = (TimeSpan) -> Unit

inline class TimerRef(val ref: Double)

class TimerComponents(override val view: View) : UpdateComponent {
    private val _timers = arrayListOf<(TimeSpan) -> Unit>()

    override fun update(dt: TimeSpan) {
        _timers.fastForEach { it(dt) }
    }

    private fun addTimer(callback: (TimeSpan) -> Unit) {
        _timers += callback
    }

    private fun removeTimer(callback: (TimeSpan) -> Unit) {
        _timers -= callback
    }

    suspend fun wait(time: TimeSpan): Unit = suspendCancellableCoroutine { c -> timeout(time) { c.resume(Unit) } }

    suspend fun waitFrame() = suspendCoroutine<Unit> { c ->
        lateinit var timer: TimerCallback
        timer = {
            removeTimer(timer)
            c.resume(Unit)
        }
        addTimer(timer)
    }

    private fun _interval(time: TimeSpan, repeat: Boolean, callback: () -> Unit = {}): Closeable {
        lateinit var timer: TimerCallback
        var elapsed = 0.milliseconds
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

    fun timeout(time: TimeSpan, callback: () -> Unit = {}): Closeable = _interval(time, false, callback)
    fun interval(time: TimeSpan, callback: () -> Unit = {}): Closeable = _interval(time, true, callback)
}

val View.timers get() = this.getOrCreateComponentUpdate<TimerComponents> { TimerComponents(this) }

fun View.timeout(time: TimeSpan, callback: () -> Unit): Closeable = this.timers.timeout(time, callback)
fun View.interval(time: TimeSpan, callback: () -> Unit): Closeable = this.timers.interval(time, callback)
suspend fun View.delay(time: TimeSpan) = this.timers.wait(time)
suspend fun View.delayFrame() = this.timers.waitFrame()
