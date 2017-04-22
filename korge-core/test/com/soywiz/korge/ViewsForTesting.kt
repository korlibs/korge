package com.soywiz.korge

import com.soywiz.korge.view.ViewsLog
import com.soywiz.korge.view.updateLoop
import com.soywiz.korio.async.syncTest
import org.junit.Before

open class ViewsForTesting {
	val viewsLog = ViewsLog()
	val injector = viewsLog.injector
	val ag = viewsLog.ag
	val input = viewsLog.input
	val views = viewsLog.views

	@Before
	fun initViews() = syncTest {
		viewsLog.init()
	}

	fun viewsTest(step: Int = 10, callback: suspend () -> Unit) = syncTest {
		views.updateLoop(step) {
			callback()
		}
	}

}
