package com.soywiz.korge

import com.soywiz.klock.milliseconds
import com.soywiz.korge.awt.*
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
import com.soywiz.korui.*
import com.soywiz.korui.native.*
import java.awt.Container

internal actual fun completeViews(views: Views) {
    val frame = (views.gameWindow.debugComponent as? Container?) ?: return
    val app = UiApplication(DEFAULT_UI_FACTORY)
    app.views = views
    val debugger = ViewsDebuggerComponent(views, app)
    views.gameWindow.onDebugChanged.add { debug ->
        views.renderContext.debugAnnotateView = if (debug) debugger.selectedView else null
    }
    frame.add(debugger)
    views.stage.timers.interval(500.milliseconds) {
        if (views.gameWindow.debug) {
            debugger.update()
        }
    }
}
