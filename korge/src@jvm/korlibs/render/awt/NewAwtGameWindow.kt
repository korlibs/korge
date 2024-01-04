package korlibs.render.awt

import korlibs.datastructure.thread.*
import korlibs.event.*
import korlibs.graphics.*
import korlibs.image.awt.*
import korlibs.image.bitmap.*
import korlibs.io.async.*
import korlibs.render.*
import korlibs.time.*
import java.awt.*
import java.awt.event.*
import javax.swing.*

class NewAwtGameWindow : GameWindow() {
    val canvas = AwtAGOpenglCanvas()
    override val ag: AG get() = canvas.ag
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

    val thread = nativeThread(name = "NewAwtGameWindow") {
        myCoroutineDispatcher.loopForever()
    }

    init {
        canvas.doRender = {
            dispatchNewRenderEvent()
        }
        myCoroutineDispatcher.eventLoop.setInterval(60.hz) {
            dispatchUpdateEvent()
        }
    }

    override var alwaysOnTop: Boolean
        get() = frame.isAlwaysOnTop
        set(value) { frame.isAlwaysOnTop = value }
    override var title: String by frame::title
    override var visible: Boolean
        get() = frame.isVisible
        set(value) { frame.isVisible = value }
    override var icon: Bitmap? = null
        set(value) {
            field = value
            frame.setIconIncludingTaskbarFromImage(value?.toAwt())
        }
    override var fullscreen: Boolean by frame::isFullScreen

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        launchUnscoped { entry() }
    }

    override fun close(exitCode: Int) {
        super.close(exitCode)
        myCoroutineDispatcher.close()
    }
}
