package korlibs.render.awt

import korlibs.datastructure.*
import korlibs.datastructure.event.*
import korlibs.datastructure.thread.*
import korlibs.graphics.*
import korlibs.image.awt.*
import korlibs.image.bitmap.*
import korlibs.render.*
import korlibs.time.*
import java.awt.*
import java.awt.event.*
import javax.swing.*

val AwtAGOpenglCanvas.gameWindow: NewAwtCanvasGameWindow by Extra.PropertyThis { NewAwtCanvasGameWindow(this) }

open class NewAwtCanvasGameWindow(val canvas: AwtAGOpenglCanvas) : GameWindow() {
    override val ag: AG get() = canvas.ag

    val thread = nativeThread(name = "NewAwtGameWindow") {
        eventLoop.runTasksForever()
    }

    init {
        canvas.doRender = {
            dispatchNewRenderEvent()
        }
        coroutineDispatcher.eventLoop.setInterval(60.hz) {
            dispatchUpdateEvent()
        }
    }

    override fun close(exitCode: Int) {
        super.close(exitCode)
        coroutineDispatcher.close()
    }
}

class NewAwtGameWindow(val config: GameWindowCreationConfig = GameWindowCreationConfig()) : NewAwtCanvasGameWindow(AwtAGOpenglCanvas()) {
    val frame = object : JFrame() {
        init {
            isVisible = false
            ignoreRepaint = true
            defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
            contentPane.layout = GridLayout(1, 1)
            contentPane.add(canvas)
            preferredSize = Dimension(640, 480)
            //setBounds(0, 0, 640, 480)
            pack()
            setLocationRelativeTo(null)
            addWindowListener(object : WindowAdapter() {
                override fun windowClosed(e: WindowEvent) {
                    this@NewAwtGameWindow.close()
                }
            })
            initTools()
        }
        override fun paintComponents(g: Graphics?) {
        }
    }

    override var alwaysOnTop: Boolean by frame::_isAlwaysOnTop
    override var title: String by frame::title
    override var visible: Boolean by frame::visible
    override var icon: Bitmap? = null
        set(value) {
            field = value
            frame.setIconIncludingTaskbarFromImage(value?.toAwt())
        }
    override var fullscreen: Boolean by frame::isFullScreen
}
