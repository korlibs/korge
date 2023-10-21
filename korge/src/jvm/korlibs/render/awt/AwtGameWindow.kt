package korlibs.render.awt

import korlibs.datastructure.*
import korlibs.datastructure.thread.*
import korlibs.graphics.*
import korlibs.image.awt.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.render.*
import korlibs.render.MenuItem
import korlibs.time.*
import kotlinx.coroutines.*
import java.awt.*
import java.awt.datatransfer.*
import java.awt.event.*
import javax.swing.*
import kotlin.concurrent.*

val AwtAGOpenglCanvas.gameWindow: AwtCanvasGameWindow by Extra.PropertyThis { AwtCanvasGameWindow(this) }

open class AwtCanvasGameWindow(
    val canvas: AwtAGOpenglCanvas
) : GameWindow(), ClipboardOwner {
    override val devicePixelRatio: Double by TimedCache(ttl = .1.seconds) { canvas.devicePixelRatio }
    override val pixelsPerInch: Double by TimedCache(ttl = .1.seconds) { canvas.pixelsPerInch }
    override val pixelsPerLogicalInchRatio: Double by TimedCache(ttl = .1.seconds) { pixelsPerInch / AG.defaultPixelsPerInch }

    override val dialogInterface: DialogInterface = DialogInterfaceAwt { canvas }
    override val ag: AG get() = canvas.ag
    private var _window: Window? = null
    open val window: Window? get() {
        if (_window == null) {
            _window = SwingUtilities.getWindowAncestor(canvas)
        }
        return _window
    }

    val thread = nativeThread(name = "NewAwtGameWindow") {
        eventLoop.runTasksForever()
    }

    override val width: Int get() = canvas.width
    override val height: Int get() = canvas.height
    override var backgroundColor: RGBA
        get() = canvas.background.toRgba()
        set(value) { canvas.background = value.toAwt() }

    override var cursor: ICursor = Cursor.DEFAULT
        set(value) {
            if (field == value) return
            field = value
            canvas.cursor = value.jvmCursor
        }

    init {
        onUpdateEvent {
            DesktopGamepadUpdater.updateGamepads(this)
        }
        canvas.doRender = {
            //it.clear(it.mainFrameBuffer, Colors.RED)
            dispatchNewRenderEvent()
        }
        canvas.canvas.registerMouseEvents(this)
        canvas.canvas.registerKeyEvents(this)
        canvas.registerGestureListeners(this)
    }

    override fun showContextMenu(items: List<MenuItem>) {
        val popupMenu = JPopupMenu()
        for (item in items) {
            popupMenu.add(item.toJMenuItem())
        }
        //println("showContextMenu: $items")
        popupMenu.setLightWeightPopupEnabled(false)
        popupMenu.show(canvas, canvas.mousePosition.x, canvas.mousePosition.y)
    }

    override fun close(exitCode: Int) {
        super.close(exitCode)
        coroutineDispatcher.close()
    }

    override fun lostOwnership(clipboard: Clipboard?, contents: Transferable?) {
    }

    override val hapticFeedbackGenerateSupport: Boolean get() = true
    override fun hapticFeedbackGenerate(kind: HapticFeedbackKind) = canvas.hapticFeedbackGenerate(kind)

    val clipboard: Clipboard by lazy { Toolkit.getDefaultToolkit().systemClipboard }

    override suspend fun clipboardWrite(data: ClipboardData) {
        awtEventQueueLater {
            when (data) {
                is TextClipboardData -> {
                    clipboard.setContents(StringSelection(data.text), this)
                }
            }
        }
    }

    override suspend fun clipboardRead(): ClipboardData? {
        return awtEventQueueLater {
            val str = clipboard.getData(DataFlavor.stringFlavor) as? String?
            str?.let { TextClipboardData(it) }
        }
    }

    suspend fun <T> awtEventQueueLater(block: () -> T): T {
        val deferred = CompletableDeferred<T>()
        EventQueue.invokeLater {
            deferred.completeWith(runCatching(block))
        }
        return deferred.await()
    }

    override fun computeDisplayRefreshRate(): Int {
        return window?.getScreenDevice()?.cachedRefreshRate?.takeIf { it > 0 } ?: 60
    }
}

class AwtGameWindow(
    val config: GameWindowCreationConfig = GameWindowCreationConfig()
) : AwtCanvasGameWindow(run {
    System.setProperty("apple.laf.useScreenMenuBar", "true")
    System.setProperty("apple.awt.application.name", config.title)
    System.setProperty("com.apple.mrj.application.apple.menu.about.name", config.title)
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    AwtAGOpenglCanvas()
}) {
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
                    this@AwtGameWindow.close()
                }
            })
            initTools()
        }
        override fun paintComponents(g: Graphics?) {
        }
    }
    override val window: Window get() = frame

    override fun setMainMenu(items: List<MenuItem>) {
        //Dispatchers.Unconfined.launchUnscoped {
        thread {
            val bar = JMenuBar()
            for (item in items) {
                val mit = item.toJMenuItem()
                if (mit is JMenu) bar.add(mit)
            }
            frame.jMenuBar = bar
            frame.doLayout()
            frame.repaint()
            println("GameWindow.setMainMenu: component=$frame, bar=$bar")
        }
    }

    override var alwaysOnTop: Boolean by frame::_isAlwaysOnTop
    override var title: String
        get() = frame.title
        set(value) {
            frame.title = value
            System.setProperty("apple.awt.application.name", value)
        }
    override var visible: Boolean by frame::visible
    override var icon: Bitmap? = null
        set(value) {
            field = value
            frame.setIconIncludingTaskbarFromImage(value?.toAwt())
        }
    override var fullscreen: Boolean by frame::isFullScreen

    override var backgroundColor: RGBA
        get() = frame.background.toRgba()
        set(value) {
            frame.background = value.toAwt()
            canvas.background = value.toAwt()
            canvas.canvas.background = value.toAwt()
        }

    override fun close(exitCode: Int) {
        try {
            super.close(exitCode)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        println("exitProcessOnClose=$exitProcessOnClose")
        if (exitProcessOnClose) {
            System.exit(exitCode)
        }
    }
}
