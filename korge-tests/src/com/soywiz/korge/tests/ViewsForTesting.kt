package com.soywiz.korge.tests

import com.soywiz.korge.view.ViewsLog
import com.soywiz.korge.view.updateLoop
import com.soywiz.korio.async.EventLoopTest
import com.soywiz.korio.async.sync
import org.junit.Before

open class ViewsForTesting {
	val elt = EventLoopTest()
	val viewsLog = ViewsLog(elt)
	val injector = viewsLog.injector
	val ag = viewsLog.ag
	val input = viewsLog.input
	val views = viewsLog.views

	fun syncTest(block: suspend EventLoopTest.() -> Unit): Unit {
		sync(el = elt, step = 10, block = block)
	}

	@Before
	fun initViews() = syncTest {
		viewsLog.init()
	}

	fun viewsTest(step: Int = 10, callback: suspend EventLoopTest.() -> Unit) = syncTest {
		views.updateLoop(this@syncTest, step) {
			callback(this@syncTest)
		}
	}

}
