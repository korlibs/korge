package com.soywiz.korge.time

import com.soywiz.korge.component.Component
import com.soywiz.korge.view.View
import com.soywiz.korio.async.Signal
import com.soywiz.korio.coroutine.korioSuspendCoroutine

class TimerComponents(view: View) : Component(view) {
	private val timers = arrayListOf<Signal<Int>>()

	override fun update(dtMs: Int) {
		for (timer in timers) timer(dtMs)
	}

	suspend fun wait(time: TimeSpan) = waitMilliseconds(time.ms)

	suspend fun waitMilliseconds(time: Int): Unit = korioSuspendCoroutine<Unit> { c ->
		val timer = Signal<Int>()
		var elapsedTime = 0
		timer {
			elapsedTime += it
			if (elapsedTime >= time) {
				timers -= timer
				c.resume(Unit)
			}
		}
		timers += timer
	}
}

val View.timers get() = this.getOrCreateComponent { TimerComponents(this) }
