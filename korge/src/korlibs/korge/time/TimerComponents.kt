package korlibs.korge.time

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.time.*
import korlibs.korge.view.*
import korlibs.io.lang.*
import korlibs.math.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.time.*

private typealias TimerCallback = (Duration) -> Unit

inline class TimerRef(val index: Int)

class TimerComponents(val view: View) {
    private val _timers = arrayListOf<((Duration) -> Unit)?>()
    private val _autoRemove = IntArrayList()
    private val _freeIndices = IntArrayList()

    private var updater: AutoCloseable? = null

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

    private fun addTimer(autoRemove: Boolean, callback: (Duration) -> Unit): TimerRef {
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

    suspend fun wait(time: Duration): Unit = suspendCancellableCoroutine { c -> timeout(time) { c.resume(Unit) } }
    suspend fun waitFrame() = suspendCoroutine<Unit> { c -> addTimer(true) { c.resume(Unit) } }

    private fun _interval(time: Duration, repeat: Boolean, callback: () -> Unit = {}): AutoCloseable {
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

    fun timeout(time: Duration, callback: () -> Unit = {}): AutoCloseable = _interval(time, false, callback)
    fun interval(time: Duration, callback: () -> Unit = {}): AutoCloseable = _interval(time, true, callback)
    fun intervalAndNow(time: Duration, callback: () -> Unit): AutoCloseable {
        callback()
        return interval(time, callback)
    }
}

val View.timers: TimerComponents by Extra.PropertyThis("__ViewTimerComponents") { TimerComponents(this) }

fun View.timeout(time: Duration, callback: () -> Unit): AutoCloseable = this.timers.timeout(time, callback)
fun View.interval(time: Duration, callback: () -> Unit): AutoCloseable = this.timers.interval(time, callback)
fun View.intervalAndNow(time: Duration, callback: () -> Unit): AutoCloseable = this.timers.intervalAndNow(time, callback)

suspend fun View.delay(time: Duration) = this.timers.wait(time)
suspend fun View.delayFrame() = this.timers.waitFrame()
