package com.soywiz.korge.time

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korio.lang.*
import kotlinx.coroutines.*
import kotlin.collections.*
import kotlin.coroutines.*

private typealias TimerCallback = (TimeSpan) -> Unit

inline class TimerRef(val index: Int)

class TimerComponents(override val view: View) : UpdateComponent {
    private val _timers = arrayListOf<((TimeSpan) -> Unit)?>()
    private val _autoRemove = IntArrayList()
    private val _freeIndices = IntArrayList()

    override fun update(dt: TimeSpan) {
        _timers.fastForEachWithIndex { index, it ->
            it?.invoke(dt)
            if (_autoRemove[index] != 0) {
                removeTimer(TimerRef(index))
            }
        }
    }

    private fun addTimer(autoRemove: Boolean, callback: (TimeSpan) -> Unit): TimerRef {
        if (_freeIndices.isNotEmpty()) {
            val index = _freeIndices.removeAt(_freeIndices.size - 1)
            _timers[index] = callback
            _autoRemove[index] = autoRemove.toInt()
            return TimerRef(index)
        }
        _timers += callback
        _autoRemove.add(autoRemove.toInt())
        return TimerRef(_timers.size - 1)
    }

    private fun removeTimer(ref: TimerRef) {
        _timers[ref.index] = null
        _autoRemove[ref.index] = 0
        _freeIndices.add(ref.index)
    }

    suspend fun wait(time: TimeSpan): Unit = suspendCancellableCoroutine { c -> timeout(time) { c.resume(Unit) } }

    suspend fun waitFrame() = suspendCoroutine<Unit> { c ->
        addTimer(true) { c.resume(Unit) }
    }

    private fun _interval(time: TimeSpan, repeat: Boolean, callback: () -> Unit = {}): Closeable {
        var elapsed = 0.milliseconds
        var ref = TimerRef(-1)
        ref = addTimer(false) { deltaMs ->
            elapsed += deltaMs
            while (elapsed >= time) {
                if (!repeat) removeTimer(ref)
                elapsed -= time
                callback()
                if (!repeat) break
            }
        }
        return Closeable { removeTimer(ref) }
    }

    fun timeout(time: TimeSpan, callback: () -> Unit = {}): Closeable = _interval(time, false, callback)
    fun interval(time: TimeSpan, callback: () -> Unit = {}): Closeable = _interval(time, true, callback)
}

val View.timers get() = this.getOrCreateComponentUpdate<TimerComponents> { TimerComponents(this) }

fun View.timeout(time: TimeSpan, callback: () -> Unit): Closeable = this.timers.timeout(time, callback)
fun View.interval(time: TimeSpan, callback: () -> Unit): Closeable = this.timers.interval(time, callback)
suspend fun View.delay(time: TimeSpan) = this.timers.wait(time)
suspend fun View.delayFrame() = this.timers.waitFrame()
