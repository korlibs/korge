package com.soywiz.korge.time

import com.soywiz.klock.TimeSpan
import com.soywiz.korge.component.Component
import com.soywiz.korge.view.View
import com.soywiz.korio.coroutine.korioSuspendCoroutine

class TimerComponents(view: View) : Component(view) {
	private val timers = arrayListOf<(Int) -> Unit>()
	private val timersIt = arrayListOf<(Int) -> Unit>()

	override fun update(dtMs: Int) {
		timersIt.clear()
		timersIt.addAll(timers)
		for (timer in timersIt) timer(dtMs)
	}

	suspend fun wait(time: TimeSpan) = waitMilliseconds(time.ms)

	suspend fun waitFrame() = waitMilliseconds(0)

	suspend fun waitMilliseconds(time: Int): Unit = korioSuspendCoroutine<Unit> { c ->
		var timer: ((Int) -> Unit)? = null
		var elapsedTime = 0
		timer = {
			elapsedTime += it
			if (elapsedTime >= time) {
				timers -= timer!!
				c.resume(Unit)
			}
		}
		timers += timer
	}
}

val View.timers get() = this.getOrCreateComponent { TimerComponents(this) }
suspend fun View.waitMs(ms: Int) = this.timers.waitMilliseconds(ms)
suspend fun View.wait(time: TimeSpan) = this.timers.wait(time)
suspend fun View.waitFrame() = this.timers.waitFrame()

suspend fun View.sleepMs(ms: Int) = this.timers.waitMilliseconds(ms)
suspend fun View.sleep(time: TimeSpan) = this.timers.wait(time)
suspend fun View.sleepFrame() = this.timers.waitFrame()
