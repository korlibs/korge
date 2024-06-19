package korlibs.korge

import korlibs.event.*
import korlibs.graphics.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.korge.awt.*
import korlibs.korge.ipc.*
import korlibs.korge.render.*
import korlibs.korge.time.*
import korlibs.korge.view.*
import korlibs.korge.view.Ellipse
import korlibs.korge.view.Image
import korlibs.math.geom.*
import korlibs.memory.*
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

class IPCViewsCompleter : ViewsCompleter {
    override fun completeViews(views: Views) {
        val korgeIPC = System.getenv("KORGE_IPC")
        if (korgeIPC != null) {
            val ipc = KorgeIPC(korgeIPC)

            views.onBeforeRender {
                while (ipc.availableEvents > 0) {
                    val e = ipc.readEvent() ?: break
                    if (e.timestamp < System.currentTimeMillis() - 100) continue

                    when (e.type) {
                        IPCEvent.KEY_DOWN, IPCEvent.KEY_UP -> {
                            views.dispatch(
                                KeyEvent(when (e.type) {
                                IPCEvent.KEY_DOWN -> KeyEvent.Type.DOWN
                                IPCEvent.KEY_UP -> KeyEvent.Type.UP
                                else -> KeyEvent.Type.DOWN
                            }, key = awtKeyCodeToKey(e.p0)
                                )
                            )
                        }
                        else -> {
                            println(e)
                        }
                    }
                }
            }

            var fbMem = Buffer(0, direct = true)

            views.onAfterRender {
                val fb = it.currentFrameBufferOrMain
                val nbytes = fb.width * fb.height * 4
                if (fbMem.size < nbytes) {
                    fbMem = Buffer(nbytes, direct = true)
                }
                it.ag.readToMemory(fb.base, fb.info, 0, 0, fb.width, fb.height, fbMem, AGReadKind.COLOR)
                //val bmp = it.ag.readColor(it.currentFrameBuffer)
                //channel.trySend(bmp)
                ipc.setFrame(IPCFrame(System.currentTimeMillis().toInt(), fb.width, fb.height, IntArray(0), fbMem.sliceWithSize(0, nbytes).nioIntBuffer))
            }
        }
    }
}

internal actual fun completeViews(views: Views) {
    for (completer in ServiceLoader.load(ViewsCompleter::class.java).toList()) {
        completer.completeViews(views)
    }
}
