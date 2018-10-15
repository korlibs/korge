package com.soywiz.korge.time

import com.soywiz.klock.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korio.lang.*
import kotlinx.coroutines.*
import kotlin.collections.arrayListOf
import kotlin.collections.minusAssign
import kotlin.collections.plusAssign
import kotlin.coroutines.*

class TimerComponents(override val view: View) : UpdateComponent {
	private val timers = arrayListOf<(Int) -> Unit>()
	private val timersIt = arrayListOf<(Int) -> Unit>()

	override fun update(ms: Double) {
		timersIt.clear()
		timersIt.addAll(timers)
		for (timer in timersIt) timer(ms.toInt())
	}

	suspend fun wait(time: TimeSpan) = waitMilliseconds(time.milliseconds)

	suspend fun waitFrame() = waitMilliseconds(1000.0 / 60.0)

	private var accumulated = 0.0

	fun takeAccumulated() = accumulated.also { accumulated = 0.0 }
	fun incrAccumulated(time: Double) = run { accumulated += time }

	suspend fun waitMilliseconds(time: Double): Unit = suspendCancellableCoroutine { c ->
		waitMilliseconds(time) { c.resume(Unit) }
	}

	fun waitMilliseconds(time: Double, callback: () -> Unit = {}): Closeable {
		var elapsedTime = takeAccumulated()
		var timer: ((Int) -> Unit)? = null
		timer = {
			elapsedTime += it
			//println("TIMER: $elapsedTime")
			if (elapsedTime >= time) {
				incrAccumulated(elapsedTime - time)
				timers -= timer!!
				//println("DONE!")
				callback()
			}
		}
		timers += timer
		return Closeable { timers -= timer }
	}
}

val View.timers get() = this.getOrCreateComponent { TimerComponents(this) }
suspend fun View.waitMs(time: Int) = this.timers.waitMilliseconds(time.toDouble())
suspend fun View.wait(time: TimeSpan) = this.timers.wait(time)
suspend fun View.waitFrame() = this.timers.waitFrame()

suspend fun View.sleepMs(time: Int) = this.timers.waitMilliseconds(time.toDouble())
suspend fun View.sleep(time: TimeSpan) = this.timers.wait(time)
suspend fun View.sleepFrame() = this.timers.waitFrame()

suspend fun View.delay(time: TimeSpan) = this.timers.wait(time)

fun View.timer(time: TimeSpan, callback: () -> Unit): Closeable = this.timers.waitMilliseconds(time.milliseconds, callback)
