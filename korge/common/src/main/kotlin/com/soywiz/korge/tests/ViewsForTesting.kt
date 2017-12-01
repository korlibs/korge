package com.soywiz.korge.tests

import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.korge.view.ViewsLog
import com.soywiz.korge.view.updateLoop
import com.soywiz.korio.async.EventLoopTest
import com.soywiz.korio.async.sync

open class ViewsForTesting {
	val elt = EventLoopTest()
	val viewsLog = ViewsLog(elt).apply {
		syncTest { init() }
	}
	val injector = viewsLog.injector
	val ag = viewsLog.ag
	val input = viewsLog.input
	val views = viewsLog.views

	fun syncTest(block: suspend EventLoopTest.() -> Unit): Unit {
		sync(el = elt, step = 10, block = block)
	}

	fun viewsTest(step: TimeSpan = 10.milliseconds, callback: suspend EventLoopTest.() -> Unit) = syncTest {
		views.updateLoop(this@syncTest, step.milliseconds) {
			callback(this@syncTest)
		}
	}
}
