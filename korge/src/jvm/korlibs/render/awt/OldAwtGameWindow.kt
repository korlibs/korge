package korlibs.render.awt

/*
import korlibs.datastructure.*
import korlibs.event.*
import korlibs.graphics.*
import korlibs.graphics.gl.*
import korlibs.image.color.*
import korlibs.io.async.*
import korlibs.kgl.*
import korlibs.memory.*
import korlibs.platform.*
import korlibs.render.*
import korlibs.render.osx.*
import korlibs.render.platform.*
import korlibs.render.win32.*
import korlibs.render.x11.*
import korlibs.time.*
import kotlinx.coroutines.*
import java.awt.*
import java.awt.datatransfer.*
import java.awt.event.*
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.*
import kotlin.system.*

import korlibs.image.awt.*
import korlibs.image.bitmap.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.render.*
import korlibs.render.platform.*
import java.awt.*
import java.awt.event.*
import javax.swing.*


open class GLCanvas constructor(checkGl: Boolean = true, val logGl: Boolean = false, cacheGl: Boolean = false) : Canvas(), GameWindowConfig, Closeable {
    val ag: AGOpengl = AGOpenglAWT(checkGl, logGl, cacheGl)
    var ctx: BaseOpenglContext? = null
    val gl = ag.gl
    private var doContextLost = false

    override fun getGraphicsConfiguration(): GraphicsConfiguration? {
        return super.getGraphicsConfiguration()
    }

    override fun addNotify() {
        super.addNotify()
        close()
    }

    override fun removeNotify() {
        super.removeNotify()
        close()
    }

    override fun reshape(x: Int, y: Int, width: Int, height: Int) {
        super.reshape(x, y, width, height)
        repaint()
    }

    override fun update(g: Graphics) {
        paint(g)
    }

    override fun paint(g: Graphics) {
        //val componentId = Native.getComponentID(this)
        //if (ctxComponentId != componentId) {
        //    close()
        //}
        if (logGl) {
            //println("+++++++++++++++++++++++++++++")
        }
        if (ctx == null) {
            //println("--------------------------------------")
            //ctxComponentId = componentId
            ctx = glContextFromComponent(this, this)
            doContextLost = true
        }
        //println("--------------")
        ctx?.useContext(g, ag) { g, info ->
            if (doContextLost) {
                doContextLost = false
                ag.contextLost()
            }
            render(gl, g, info)
        }
    }


    override fun close() {
        ctx?.dispose()
        ctx = null
    }

    var defaultRendererAG: (ag: AG) -> Unit = {
    }

    var defaultRenderer: (gl: KmlGl, g: Graphics) -> Unit = { gl, g ->
        /*
        ctx?.useContext(g, ag) {
            gl.clearColor(0f, 0f, 0f, 1f)
            gl.clear(gl.COLOR_BUFFER_BIT)
        }
         */
    }

    val getCurrent: () -> Any? = { ctx?.getCurrent() }

    open fun render(gl: KmlGl, g: Graphics, info: BaseOpenglContext.ContextInfo) {
        //ctx?.makeCurrent()
        gl.info.current = getCurrent
        ag.startEndFrame {
            val viewport = info.viewport
            if (viewport != null) {
                gl.viewport(viewport.x, viewport.y, viewport.width, viewport.height)
                gl.scissor(viewport.x, viewport.y, viewport.width, viewport.height)
                gl.enable(KmlGl.SCISSOR_TEST)
                //println("viewport=$viewport")
            }
            defaultRenderer(gl, g)
            defaultRendererAG(ag)
        }
    }

    override var quality: GameWindowQuality = GameWindowQuality.AUTOMATIC
}

class AwtGameWindow(config: GameWindowCreationConfig = GameWindowCreationConfig.DEFAULT) : BaseAwtGameWindow(AGOpenglAWT(config)) {
    override var ctx: BaseOpenglContext? = null

    override fun ensureContext() {
        if (ctx == null) ctx = glContextFromComponent(frame, this)
    }

    val frame: JFrame = object : JFrame("Korgw") {
        val frame = this

        init {
            isVisible = false
            ignoreRepaint = true
            //background = Color.black
            setBounds(0, 0, 640, 480)
            frame.setLocationRelativeTo(null)
            frame.setKorgeDropTarget(this@AwtGameWindow)
            frame.setIconIncludingTaskbarFromResource("@appicon.png")
            this.initTools()

            addWindowListener(object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent?) {
                    debugFrame.isVisible = false
                    debugFrame.dispose()
                    running = false
                }
            })
            addComponentListener(object : ComponentAdapter() {
                override fun componentMoved(e: ComponentEvent?) {
                    synchronizeDebugFrameCoordinates()
                }

                override fun componentResized(e: ComponentEvent?) {
                    synchronizeDebugFrameCoordinates()
                }
            })
        }

        override fun paintComponents(g: Graphics?) = Unit

        override fun paint(g: Graphics) {
            try {
                framePaint(g)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    override var alwaysOnTop: Boolean by frame::_isAlwaysOnTop
    override var title: String by frame::title
    override var icon: Bitmap? = null
        set(value) {
            field = value
            frame.setIconIncludingTaskbarFromImage(value?.toAwt())
        }
    override var fullscreen: Boolean by frame::isFullScreen

    val debugFrame = JFrame("Debug").apply {
        try {
            this.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
            this.setSize(280, 256)
            this.type = Window.Type.UTILITY
            this.isAlwaysOnTop = true
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        val debugFrame = this
        this@AwtGameWindow.onDebugChanged.add {
            EventQueue.invokeLater {
                debugFrame.isVisible = it
                synchronizeDebugFrameCoordinates()
                if (debugFrame.isVisible) {
                    //frame.isVisible = false
                    frame.isVisible = true
                }
            }
        }
    }

    override val debugComponent: Any? = debugFrame

    private fun synchronizeDebugFrameCoordinates() {
        val displayMode = frame.getScreenDevice().displayMode
        //println("frame.location=${frame.location}, frame.size=${frame.size}, debugFrame.width=${debugFrame.width}, displayMode=${displayMode.width}x${displayMode.height}")
        val frameBounds = RectangleInt(frame.location.x, frame.location.y, frame.size.width, frame.size.height)
        debugFrame.setLocation(frameBounds.right.clamp(0, (displayMode.width - debugFrame.width * 1.0).toInt()), frameBounds.top)
        debugFrame.setSize(debugFrame.width.coerceAtLeast(64), frameBounds.height)
    }

    override fun setSize(width: Int, height: Int) {
        contentComponent.setSize(width, height)
        contentComponent.preferredSize = Dimension(width, height)
        frame.pack()
        val component = this.component
        if (component is Window) component.setLocationRelativeTo(null)
    }

    override val component: Component get() = frame
    override val contentComponent: Component get() = frame.contentPane

    override fun frameDispose() {
        frame.dispose()
    }
}
abstract class BaseAwtGameWindow(
    override val ag: AGOpengl
) : GameWindow(), ClipboardOwner {

    fun paintInContextInternal(g: Graphics, info: BaseOpenglContext.ContextInfo) {
        run {
            //run {
            ctx?.swapInterval(1)

            val g = g as Graphics2D
            val gl = ag.gl
            val factor = frameScaleFactor
            if (lastFactor != factor) {
                lastFactor = factor
                reshaped = true
            }

            //println("RENDER[1]")

            val viewport = info.viewport
            val scissor = info.scissors

            if (component is JFrame) {
                //println("component.width: ${contentComponent.width}x${contentComponent.height}")
                ag.mainFrameBuffer.setSize(
                    0, 0, (contentComponent.width * factor).toInt(), (contentComponent.height * factor).toInt(),
                )
            } else {
                ag.mainFrameBuffer.scissor(scissor)
                if (viewport != null) {
                    //val window = SwingUtilities.getWindowAncestor(contentComponent)
                    //println("window=${window.width}x${window.height} : factor=$factor")

                    val frameOrComponent = (window as? JFrame)?.contentPane ?: windowOrComponent

                    ag.mainFrameBuffer.setSize(
                        viewport.x, viewport.y, viewport.width, viewport.height,
                        (frameOrComponent.width * factor).toInt(),
                        (frameOrComponent.height * factor).toInt(),
                    )
                } else {
                    ag.mainFrameBuffer.setSize(
                        0, 0, (component.width * factor).toInt(), (component.height * factor).toInt(),
                    )
                }
            }

            //println(gl.getString(gl.VERSION))
            //println(gl.versionString)
            if (reshaped) {
                reshaped = false
                //println("RESHAPED!")
                dispatchReshapeEventEx(
                    ag.mainFrameBuffer.x,
                    ag.mainFrameBuffer.y,
                    ag.mainFrameBuffer.width,
                    ag.mainFrameBuffer.height,
                    ag.mainFrameBuffer.fullWidth,
                    ag.mainFrameBuffer.fullHeight,
                )
            }

            //gl.clearColor(1f, 1f, 1f, 1f)
            //gl.clear(gl.COLOR_BUFFER_BIT)
            var gamePadTime: TimeSpan = 0.milliseconds
            var frameTime: TimeSpan = 0.milliseconds
            var finishTime: TimeSpan = 0.milliseconds
            val totalTime = measureTime {
                frameTime = measureTime {
                    frame()
                }
                finishTime = measureTime {
                    gl.flush()
                    gl.finish()
                }
            }

            //println("totalTime=$totalTime, gamePadTime=$gamePadTime, finishTime=$finishTime, frameTime=$frameTime, timedTasksTime=${coroutineDispatcher.timedTasksTime}, tasksTime=${coroutineDispatcher.tasksTime}, renderTime=${renderTime}, updateTime=${updateTime}")
        }
    }


    val frameScaleFactor: Float get() = getDisplayScalingFactor(component)

    val nonScaledWidth get() = contentComponent.width.toDouble()
    val nonScaledHeight get() = contentComponent.height.toDouble()

    val scaledWidth get() = contentComponent.width * frameScaleFactor
    val scaledHeight get() = contentComponent.height * frameScaleFactor

    override val width: Int get() = (scaledWidth).toInt()
    override val height: Int get() = (scaledHeight).toInt()
    override var visible: Boolean by LazyDelegate { component::visible }
    override var quality: Quality = Quality.AUTOMATIC

    fun dispatchReshapeEvent() {
        val factor = frameScaleFactor
        dispatchReshapeEvent(
            component.x,
            component.y,
            (contentComponent.width * factor).toInt().coerceAtLeast(1),
            (contentComponent.height * factor).toInt().coerceAtLeast(1)
        )
    }

    var displayLinkLock: java.lang.Object? = null
    private val dl = OSXDisplayLink {
        displayLinkLock?.let { displayLock ->
            synchronized(displayLock) {
                displayLock.notify()
            }
        }
    }

    open fun frameDispose() {
    }

    var reshaped = false

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        launchImmediately(getCoroutineDispatcherWithCurrentContext()) {
            entry()
        }

//frame.setBounds(0, 0, width, height)

        //val timer= Timer(40, ActionListener {
        //})

        queue {
            dispatchReshapeEvent()
        }
        EventQueue.invokeLater {
            component.isVisible = true
            component.repaint()
            //fullscreen = true

            // keys.up(Key.ENTER) { if (it.alt) gameWindow.toggleFullScreen() }

            // @TODO: HACK so the windows grabs focus on Windows 10 when launching on gradle daemon
            val useRobotHack = Platform.isWindows

            if (useRobotHack) {
                (component as? Frame?)?.apply {
                    val frame = this
                    val insets = frame.insets
                    frame.isAlwaysOnTop = true
                    try {
                        val robot = Robot()
                        val pos = MouseInfo.getPointerInfo().location
                        val bounds = frame.bounds
                        bounds.setFrameFromDiagonal(bounds.minX + insets.left, bounds.minY + insets.top, bounds.maxX - insets.right, bounds.maxY - insets.bottom)

                        //println("frame.bounds: ${frame.bounds}")
                        //println("frame.bounds: ${bounds}")
                        //println("frame.insets: ${insets}")
                        //println(frame.contentPane.bounds)
                        //println("START ROBOT")
                        robot.mouseMove(bounds.centerX.toInt(), bounds.centerY.toInt())
                        robot.mousePress(InputEvent.BUTTON3_MASK)
                        robot.mouseRelease(InputEvent.BUTTON3_MASK)
                        robot.mouseMove(pos.x, pos.y)
                        //println("END ROBOT")
                    } catch (e: Throwable) {
                    }
                    frame.isAlwaysOnTop = false
                }
            }
        }
    }

}
*/
