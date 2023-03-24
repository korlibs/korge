package korlibs.korge

import korlibs.time.*
import korlibs.korge.awt.*
import korlibs.korge.time.*
import korlibs.korge.view.*
import kotlinx.coroutines.*
import java.awt.Container

internal actual fun completeViews(views: Views) {
    views.injector.mapSingleton<ViewsDebuggerComponent> {
        //UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())

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