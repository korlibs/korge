package com.soywiz.korge

import com.soywiz.klock.*
import com.soywiz.korge.awt.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
import com.soywiz.korui.*
import com.soywiz.korui.native.*
import kotlinx.coroutines.*
import java.awt.Container

internal actual fun completeViews(views: Views) {
    views.injector.mapSingleton<ViewsDebuggerComponent> {
        val app by lazy { UiApplication(DEFAULT_UI_FACTORY) }
        val debugger = ViewsDebuggerComponent(views, app)
        val frame = (views.gameWindow.debugComponent as? Container?) ?: return@mapSingleton debugger
        app.views = views
        views.gameWindow.onDebugChanged.add { debug ->
            views.renderContext.debugAnnotateView = if (debug) debugger.selectedView else null
        }
        frame.add(debugger)
        views.stage.timers.interval(500.milliseconds) {
            if (views.gameWindow.debug) {
                debugger.updateTimer()
            }
        }
        debugger
    }

    views.gameWindow.onDebugEnabled.once {
        runBlocking {
            views.injector.get<ViewsDebuggerComponent>()
        }
    }
}
