package korlibs.render.awt

import korlibs.image.awt.*
import korlibs.image.bitmap.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.render.*
import korlibs.render.platform.*
import java.awt.*
import java.awt.event.*
import javax.imageio.*
import javax.swing.*

class AwtGameWindow(config: GameWindowCreationConfig = GameWindowCreationConfig.DEFAULT) : BaseAwtGameWindow(AGOpenglAWT(config)) {
    override var ctx: BaseOpenglContext? = null
    //val frame = Window(Frame("Korgw"))
    val classLoader = this.javaClass.classLoader

    override fun ensureContext() {
        if (ctx == null) ctx = glContextFromComponent(frame, this)
    }

    //private var currentInFullScreen = false

    val frame: JFrame = object : JFrame("Korgw") {
        val frame = this
        //val frame = object : Frame("Korgw") {
        init {
            isVisible = false
            ignoreRepaint = true
            //background = Color.black
            setBounds(0, 0, 640, 480)
            val frame = this
            frame.setLocationRelativeTo(null)
            frame.setKorgeDropTarget(this@AwtGameWindow)

            runCatching {
                val awtImageURL = AwtGameWindow::class.java.getResource("/@appicon.png")
                    ?: AwtGameWindow::class.java.getResource("@appicon.png")
                    ?: ClassLoader.getSystemResource("@appicon.png")
                setIconIncludingTaskbarFromImage(awtImageURL?.let { ImageIO.read(it) })
            }

            //val dim = getDefaultToolkit().screenSize
            //frame.setLocation(dim.width / 2 - frame.size.width / 2, dim.height / 2 - frame.size.height / 2)

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

        override fun paintComponents(g: Graphics?) {
        }

        // https://stackoverflow.com/questions/52108178/swing-animation-still-stutter-when-i-use-toolkit-getdefaulttoolkit-sync
        // https://www.oracle.com/java/technologies/painting.html
        // https://docs.oracle.com/javase/tutorial/extra/fullscreen/rendering.html
        // https://docs.oracle.com/javase/tutorial/extra/fullscreen/doublebuf.html
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
            //this.isUndecorated = true
            //this.opacity = 0.5f
            //focusableWindowState = false
        } catch (e: Throwable) {
            e.printStackTrace()
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

    init {
        onDebugChanged.add {
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

    override fun setSize(width: Int, height: Int) {
        contentComponent.setSize(width, height)
        contentComponent.preferredSize = Dimension(width, height)
        frame.pack()
        val component = this.component
        //val dim = Toolkit.getDefaultToolkit().screenSize
        //component.setLocation(dim.width / 2 - component.size.width / 2, dim.height / 2 - component.size.height / 2)
        if (component is Window) {
            component.setLocationRelativeTo(null)
        }
        //component.setlo
    }

    override val component: Component get() = frame
    override val contentComponent: Component get() = frame.contentPane

    override fun frameDispose() {
        frame.dispose()
    }
}
