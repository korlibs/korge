package com.soywiz.korgw.awt

import com.soywiz.kmem.*
import com.soywiz.korev.*
import com.soywiz.korgw.*
import com.soywiz.korgw.platform.*
import com.soywiz.korim.awt.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.dynamic.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import java.awt.*
import java.awt.datatransfer.*
import java.awt.dnd.*
import java.awt.event.*
import java.io.*
import javax.imageio.*
import javax.swing.*

class AwtGameWindow(config: GameWindowCreationConfig) : BaseAwtGameWindow(AGOpenglAWT(config)) {

    /*
    fun JFrame.isFullScreen(): Boolean {
        try {
            awtGetPeer()
        } catch (e: Throwable) {
            e.printStackTrace()
            return false
        }
    }
    */

    override var ctx: BaseOpenglContext? = null
    //val frame = Window(Frame("Korgw"))
    val classLoader = this.javaClass.classLoader

    override fun ensureContext() {
        if (ctx == null) {
            if (Platform.isMac) {
                try {
                    //ctx = ProxiedMacAWTOpenglContext(frame)
                    ctx = glContextFromComponent(frame, this)

                } catch (e: Throwable) {
                    e.printStackTrace()
                    ctx = glContextFromComponent(frame, this)
                }
            } else {
                ctx = glContextFromComponent(frame, this)
            }
        }
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
            frame.dropTarget = object : DropTarget() {
                init {
                    this.addDropTargetListener(object : DropTargetAdapter() {
                        override fun drop(dtde: DropTargetDropEvent) {
                            //println("drop")
                            dtde.acceptDrop(DnDConstants.ACTION_COPY)
                            dispatchDropfileEvent(DropFileEvent.Type.DROP, (dtde.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>).map { it.toVfs() })
                            dispatchDropfileEvent(DropFileEvent.Type.END, null)
                        }
                    })
                }

                override fun dragEnter(dtde: DropTargetDragEvent) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY)
                    //dispatchDropfileEvent(DropFileEvent.Type.ENTER, null)
                    dispatchDropfileEvent(DropFileEvent.Type.START, null)
                    //println("dragEnter")
                    super.dragEnter(dtde)
                }

                override fun dragOver(dtde: DropTargetDragEvent) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY)
                    super.dragOver(dtde)
                }

                override fun dragExit(dte: DropTargetEvent) {
                    //dispatchDropfileEvent(DropFileEvent.Type.EXIT, null)
                    dispatchDropfileEvent(DropFileEvent.Type.END, null)
                    super.dragExit(dte)
                }
            }

            run {
                val awtImageURL = AwtGameWindow::class.java.getResource("/@appicon.png")
                    ?: AwtGameWindow::class.java.getResource("@appicon.png")
                    ?: ClassLoader.getSystemResource("@appicon.png")
                if (awtImageURL != null) {
                    runCatching {
                        val awtImage = ImageIO.read(awtImageURL)
                        kotlin.runCatching {
                            Dyn.global["java.awt.Taskbar"].dynamicInvoke("getTaskbar").dynamicInvoke("setIconImage", awtImage)
                        }
                        frame.iconImage = awtImage.getScaledInstance(32, 32, Image.SCALE_SMOOTH)
                    }
                }
            }

            //val dim = getDefaultToolkit().screenSize
            //frame.setLocation(dim.width / 2 - frame.size.width / 2, dim.height / 2 - frame.size.height / 2)

            if (Platform.isMac) {
                //rootPane.putClientProperty("apple.awt.fullscreenable", true)

                /*
                val listener = Proxy.newProxyInstance(classLoader, arrayOf(Class.forName("com.apple.eawt.FullScreenListener"))) { proxy, method, args ->
                    //println("INVOKED: $proxy, $method")
                    when (method.name) {
                        "windowEnteredFullScreen" -> {
                            currentInFullScreen = true
                        }
                        "windowExitedFullScreen" -> {
                            currentInFullScreen = false
                        }
                    }
                    null
                }
                */
                try {
                    Dyn.global["com.apple.eawt.FullScreenUtilities"].dynamicInvoke("setWindowCanFullScreen", frame, true)
                    //Dyn.global["com.apple.eawt.FullScreenUtilities"].invoke("addFullScreenListenerTo", frame, listener)
                } catch (e: Throwable) {
                    if (e::class.qualifiedName != "java.lang.reflect.InaccessibleObjectException") {
                        e.printStackTrace()
                    }
                }

                // @TODO: This one owns the whole screen in a bad way
                //val env = GraphicsEnvironment.getLocalGraphicsEnvironment()
                //val device = env.defaultScreenDevice
                //device.fullScreenWindow = this
            }
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

    override var title: String
        get() = frame.title
        set(value) { frame.title = value }
    override var icon: Bitmap? = null
        set(value) {
            field = value
            val awtImage = value?.toAwt()
            if (awtImage != null) {
                kotlin.runCatching {
                    Dyn.global["java.awt.Taskbar"].dynamicInvoke("getTaskbar").dynamicInvoke("setIconImage", awtImage)
                }
                frame.iconImage = awtImage
            }
        }

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
        val frameBounds = MRectangleInt(frame.location.x, frame.location.y, frame.size.width, frame.size.height)
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

    override var fullscreen: Boolean
        get() = when {
            Platform.isMac -> frame.rootPane.bounds == frame.bounds
            else -> GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.fullScreenWindow == frame
        }
        set(value) {
            //println("fullscreen: $fullscreen -> $value")
            if (fullscreen != value) {
                when {
                    Platform.isMac -> {
                        //println("TOGGLING!")
                        if (fullscreen != value) {
                            queue {
                                try {
                                    //println("INVOKE!: ${getClass("com.apple.eawt.Application").invoke("getApplication")}")
                                    Dyn.global["com.apple.eawt.Application"]
                                        .dynamicInvoke("getApplication")
                                        .dynamicInvoke("requestToggleFullScreen", frame)
                                } catch (e: Throwable) {
                                    if (e::class.qualifiedName != "java.lang.reflect.InaccessibleObjectException") {
                                        e.printStackTrace()
                                    }
                                    GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.fullScreenWindow = if (value) frame else null
                                    frame.isVisible = true
                                }
                            }
                        }
                    }
                    else -> {
                        GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.fullScreenWindow = if (value) frame else null

                        //frame.extendedState = if (value) JFrame.MAXIMIZED_BOTH else JFrame.NORMAL
                        //frame.isUndecorated = value
                        frame.isVisible = true
                        //frame.isAlwaysOnTop = true
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

    override fun loopInitialization() {
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                debugFrame.isVisible = false
                debugFrame.dispose()
                running = false
            }
        })
        frame.addComponentListener(object : ComponentAdapter() {
            override fun componentMoved(e: ComponentEvent?) {
                synchronizeDebugFrameCoordinates()
            }

            override fun componentResized(e: ComponentEvent?) {
                synchronizeDebugFrameCoordinates()
            }
        })
    }

    override fun frameDispose() {
        frame.dispose()
    }
}

