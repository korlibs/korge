package com.soywiz.korge

import com.soywiz.korge.view.ViewsLog

open class ViewsForTesting {
	val viewsLog = ViewsLog()
	val injector = viewsLog.injector
	val ag = viewsLog.ag
	val input = viewsLog.input
	val views = viewsLog.views
}
