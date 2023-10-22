package korlibs.korge

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.korge.awt.*
import korlibs.korge.time.*
import korlibs.korge.view.*
import korlibs.korge.view.Ellipse
import korlibs.korge.view.Image
import korlibs.math.geom.*
import korlibs.time.*
import kotlinx.coroutines.*
import java.awt.Container
import java.util.*

interface ViewsCompleter {
    fun completeViews(views: Views)
}

class StandardViewsCompleter : ViewsCompleter {
    override fun completeViews(views: Views) {
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

        views.viewFactories.addAll(listOf(
            ViewFactory("Image") { Image(Bitmaps.white).apply { size(100f, 100f) } },
            ViewFactory("VectorImage") { VectorImage.createDefault().apply { size(100f, 100f) } },
            ViewFactory("SolidRect") { SolidRect(100, 100, Colors.WHITE) },
            ViewFactory("Ellipse") { Ellipse(Size(50f, 50f), Colors.WHITE).center() },
            ViewFactory("Container") { korlibs.korge.view.Container() },
            ViewFactory("9-Patch") { NinePatch(NinePatchBmpSlice(Bitmap32(62, 62, premultiplied = true))) },
        ))
    }
}

internal actual fun completeViews(views: Views) {
    for (completer in ServiceLoader.load(ViewsCompleter::class.java).toList()) {
        completer.completeViews(views)
    }
}
