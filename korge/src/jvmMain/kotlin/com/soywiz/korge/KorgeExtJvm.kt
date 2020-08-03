package com.soywiz.korge

import com.soywiz.klock.hr.*
import com.soywiz.korge.awt.*
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
import javax.swing.*

internal actual fun completeViews(views: Views) {
    val frame = (views.gameWindow.debugComponent as? JFrame?) ?: return
    val debugger = ViewsDebuggerComponent(views.stage, views.coroutineContext, views)
    frame.add(debugger)
    views.stage.timers.interval(500.hrMilliseconds) {
        debugger.update()
    }
}
