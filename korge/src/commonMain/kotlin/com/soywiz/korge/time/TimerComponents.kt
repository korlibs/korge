package com.soywiz.korge.time

import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.korge.component.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.view.*
import com.soywiz.korio.lang.*
import kotlinx.coroutines.*
import kotlin.collections.arrayListOf
import kotlin.collections.minusAssign
import kotlin.collections.plusAssign
import kotlin.coroutines.*

private typealias TimerCallback = (Int) -> Unit

inline class TimerRef(val ref: Double)

class TimerComponents(override val view: View) : UpdateComponent {
	private val _timers = arrayListOf<(Int) -> Unit>()

	override fun update(ms: Double) {
		_timers.fastForEach { it(ms.toInt()) }
	}

	private fun addTimer(callback: (Int) -> Unit) {
		_timers += callback
	}

	private fun removeTimer(callback: (Int) -> Unit) {
		_timers -= callback
	}

	suspend fun wait(time: TimeSpan) = waitMilliseconds(time.milliseconds)

	suspend fun waitFrame() = suspendCoroutine<Unit> { c ->
		lateinit var timer: TimerCallback
		timer = {
			removeTimer(timer)
			c.resume(Unit)
		}
		addTimer(timer)
	}

	suspend fun waitMilliseconds(time: Double): Unit = suspendCancellableCoroutine { c ->
		waitMilliseconds(time) { c.resume(Unit) }
	}

	fun waitMilliseconds(time: Double, callback: () -> Unit = {}): Closeable = timeoutMs(time, callback)

	fun timeoutMs(time: Double, callback: () -> Unit = {}): Closeable {
		var elapsedTime = 0.0
		lateinit var timer: ((Int) -> Unit)
		timer = {
			elapsedTime += it
			//println("TIMER: $elapsedTime")
			if (elapsedTime >= time) {
				removeTimer(timer)
				//println("DONE!")
				callback()
			}
		}
		addTimer(timer)
		return Closeable { removeTimer(timer) }
	}

	fun intervalMs(time: Double, callback: () -> Unit = {}): Closeable {
		lateinit var timer: TimerCallback
		var elapsed = 0.0
		timer = { deltaMs ->
			elapsed += deltaMs
			while (elapsed >= time) {
				elapsed -= time
				callback()
			}
		}
		addTimer(timer)
		return Closeable { removeTimer(timer) }
	}
}

val View.timers get() = this.getOrCreateComponentUpdate<TimerComponents> { TimerComponents(this) }

@Deprecated("", ReplaceWith("this.delay(time.milliseconds)", "com.soywiz.klock.milliseconds"))
suspend fun View.waitMs(time: Int) = this.delay(time.ms)
@Deprecated("", ReplaceWith("this.delay(time)"))
suspend fun View.wait(time: TimeSpan) = this.delay(time)
@Deprecated("", ReplaceWith("this.delayFrame()"))
suspend fun View.waitFrame() = this.delayFrame()

@Deprecated("", ReplaceWith("this.delay(time.milliseconds)", "com.soywiz.klock.milliseconds"))
suspend fun View.sleepMs(time: Int) = this.delay(time.ms)
@Deprecated("", ReplaceWith("this.delay(time)"))
suspend fun View.sleep(time: TimeSpan) = this.delay(time)
@Deprecated("", ReplaceWith("this.delayFrame()"))
suspend fun View.sleepFrame() = this.delayFrame()

@Deprecated("", ReplaceWith("this.timeout(time, callback)"))
fun View.timer(time: TimeSpan, callback: () -> Unit): Closeable = this.timeout(time, callback)

fun View.timeout(time: TimeSpan, callback: () -> Unit): Closeable = this.timers.timeoutMs(time.milliseconds, callback)
fun View.interval(time: TimeSpan, callback: () -> Unit): Closeable = this.timers.intervalMs(time.milliseconds, callback)
suspend fun View.delay(time: TimeSpan) = this.timers.wait(time)
suspend fun View.delayFrame() = this.timers.waitFrame()
