package com.soywiz.korge

import com.soywiz.klock.hr.*
import com.soywiz.korge.awt.*
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
import com.soywiz.korui.*
import com.soywiz.korui.native.*
import javax.swing.*

internal actual fun completeViews(views: Views) {
    val frame = (views.gameWindow.debugComponent as? JFrame?) ?: return
    val debugger = ViewsDebuggerComponent(views, UiApplication(DEFAULT_UI_FACTORY))
    views.gameWindow.onDebugChanged.add { debug ->
        views.renderContext.debugAnnotateView = if (debug) debugger.selectedView else null
    }
    frame.add(debugger)
    views.stage.timers.interval(500.hrMilliseconds) {
        debugger.update()
    }
}
