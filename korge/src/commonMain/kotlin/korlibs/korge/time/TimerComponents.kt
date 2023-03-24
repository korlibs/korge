package korlibs.korge.time

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.time.*
import korlibs.memory.*
import korlibs.korge.view.*
import korlibs.io.lang.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

private typealias TimerCallback = (TimeSpan) -> Unit

inline class TimerRef(val index: Int)

class TimerComponents(val view: View) {
    private val _timers = arrayListOf<((TimeSpan) -> Unit)?>()
    private val _autoRemove = IntArrayList()
    private val _freeIndices = IntArrayList()

    private var updater: Closeable? = null

    private fun ensureUpdater() {
        if (updater != null) return
        updater = view.addUpdater { dt ->
            _timers.fastForEachWithIndex { index, it ->
                it?.invoke(dt)
                if (_autoRemove[index] != 0) {
                    removeTimer(TimerRef(index))
                }
            }
            if (_timers.isEmpty()) {
                updater?.close()
                updater = null
            }
        }
    }

    private fun addTimer(autoRemove: Boolean, callback: (TimeSpan) -> Unit): TimerRef {
        if (_freeIndices.isNotEmpty()) {
            val index = _freeIndices.removeAt(_freeIndices.size - 1)
            _timers[index] = callback
            _autoRemove[index] = autoRemove.toInt()
            ensureUpdater()
            return TimerRef(index)
        }
        _timers += callback
        _autoRemove.add(autoRemove.toInt())
        ensureUpdater()
        return TimerRef(_timers.size - 1)
    }

    private fun removeTimer(ref: TimerRef) {
        _timers[ref.index] = null
        _autoRemove[ref.index] = 0
        _freeIndices.add(ref.index)
    }

    suspend fun wait(time: TimeSpan): Unit = suspendCancellableCoroutine { c -> timeout(time) { c.resume(Unit) } }
    suspend fun waitFrame() = suspendCoroutine<Unit> { c -> addTimer(true) { c.resume(Unit) } }

    private fun _interval(time: TimeSpan, repeat: Boolean, callback: () -> Unit = {}): Closeable {
        var elapsed = 0.milliseconds
        var ref = TimerRef(-1)
        ref = addTimer(false) { dt ->
            elapsed += dt
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
    fun intervalAndNow(time: TimeSpan, callback: () -> Unit): Closeable {
        callback()
        return interval(time, callback)
    }
}

val View.timers: TimerComponents by Extra.PropertyThis("__ViewTimerComponents") { TimerComponents(this) }

fun View.timeout(time: TimeSpan, callback: () -> Unit): Closeable = this.timers.timeout(time, callback)
fun View.interval(time: TimeSpan, callback: () -> Unit): Closeable = this.timers.interval(time, callback)
fun View.intervalAndNow(time: TimeSpan, callback: () -> Unit): Closeable = this.timers.intervalAndNow(time, callback)

suspend fun View.delay(time: TimeSpan) = this.timers.wait(time)
suspend fun View.delayFrame() = this.timers.waitFrame()
