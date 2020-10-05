package com.soywiz.korui

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korui.light.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.AppKit.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.darwin.*
import kotlin.coroutines.*
import kotlin.reflect.*

@UseExperimental(InternalCoroutinesApi::class)
class MyNativeCoroutineDispatcher() : CoroutineDispatcher(), Delay, Closeable {
    override fun dispatchYield(context: CoroutineContext, block: Runnable): Unit = dispatch(context, block)

    class TimedTask(val ms: DateTime, val continuation: CancellableContinuation<Unit>)

    val tasks = Queue<Runnable>()
    val timedTasks = PriorityQueue<TimedTask>(Comparator<TimedTask> { a, b -> a.ms.compareTo(b.ms) })

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        tasks.enqueue(block)
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>): Unit {
        val task = TimedTask(DateTime.now() + timeMillis.milliseconds, continuation)
        continuation.invokeOnCancellation {
            timedTasks.remove(task)
        }
        timedTasks.add(task)
    }

    fun executeStep() {
        val now = DateTime.now()
        while (timedTasks.isNotEmpty() && now >= timedTasks.head.ms) {
            timedTasks.removeHead().continuation.resume(Unit)
        }

        while (tasks.isNotEmpty()) {
            val task = tasks.dequeue()
            task.run()
        }
    }

    override fun close() {

    }

    override fun toString(): String = "MyNativeCoroutineDispatcher"
}

@ThreadLocal
val myNativeCoroutineDispatcher: MyNativeCoroutineDispatcher = MyNativeCoroutineDispatcher()

actual val KoruiDispatcher: CoroutineDispatcher get() = myNativeCoroutineDispatcher

class NativeKoruiContext(
    val ag: AG,
    val light: LightComponents
    //, val app: NSApplication
) : KoruiContext()

class NativeLightComponents(val nkcAg: AG) : LightComponents() {
    val frameHandle = Any()

    override fun create(type: LightType, config: Any?): LightComponentInfo {
        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        val handle: Any = when (type) {
            LightType.FRAME -> frameHandle
            LightType.CONTAINER -> Any()
            LightType.BUTTON -> Any()
            LightType.IMAGE -> Any()
            LightType.PROGRESS -> Any()
            LightType.LABEL -> Any()
            LightType.TEXT_FIELD -> Any()
            LightType.TEXT_AREA -> Any()
            LightType.CHECK_BOX -> Any()
            LightType.SCROLL_PANE -> Any()
            LightType.AGCANVAS -> nkcAg.nativeComponent
            else -> throw UnsupportedOperationException("Type: $type")
        }
        return LightComponentInfo(handle).apply {
            this.ag = nkcAg
        }
    }

    val eds = arrayListOf<Pair<KClass<*>, EventDispatcher>>()

    fun <T : Event> dispatch(clazz: KClass<T>, e: T) {
        for ((eclazz, ed) in eds) {
            if (eclazz == clazz) {
                ed.dispatch(clazz, e)
            }
        }
    }

    inline fun <reified T : Event> dispatch(e: T) = dispatch(T::class, e)

    override fun <T : Event> registerEventKind(c: Any, clazz: KClass<T>, ed: EventDispatcher): Closeable {
        val pair = Pair(clazz, ed)

        if (c === frameHandle || c === nkcAg.nativeComponent) {
            eds += pair
            return Closeable { eds -= pair }
        }

        return DummyCloseable
    }

    override suspend fun dialogOpenFile(c: Any, filter: String): VfsFile {
        val openDlg: NSOpenPanel = NSOpenPanel().apply {
            setCanChooseFiles(true)
            setAllowsMultipleSelection(false)
            setCanChooseDirectories(false)
        }
        if (openDlg.runModalForDirectory(null, null).toInt() == NSOKButton.toInt()) {
            return com.soywiz.korio.file.std.localVfs(openDlg.filename())
        } else {
            throw com.soywiz.korio.lang.CancelException()
        }
    }
}

class WinController : NSObject() {
    @ObjCAction
    fun doTerminate() {
        NSApplication.sharedApplication.terminate(null)
    }
}

internal actual suspend fun KoruiWrap(entry: suspend (KoruiContext) -> Unit) = autoreleasepool {
    val app = NSApplication.sharedApplication()
    val controller = WinController()
    app.mainMenu = NSMenu().apply {
        //this.autoenablesItems = true
        addItem(NSMenuItem("Application", null, "").apply {
            this.submenu = NSMenu().apply {
                //this.autoenablesItems = true
                addItem(NSMenuItem("Quit", NSSelectorFromString(WinController::doTerminate.name), "q").apply {
                    target = controller
                    //enabled = true
                })
            }
            //enabled = true
        })
    }


    //val ctx = NativeKoruiContext(ag, app)
    val windowConfig = WindowConfig(640, 480, "Korui")

    val agNativeComponent = Any()
    val ag: AG = AGOpenglFactory.create(agNativeComponent).create(agNativeComponent, AGConfig())
    val light = NativeLightComponents(ag)

    app.delegate = MyAppDelegate(ag, windowConfig, object : MyAppHandler {
        override fun init(context: NSOpenGLContext?) {
            macTrace("init[a]")
            macTrace("init[b]")
            val ctx = NativeKoruiContext(ag, light)
            println("KoruiWrap.pentry[0]")
            //launch(KoruiDispatcher) { // Doesn't work!
            println("KoruiWrap.pentry[1]")
            println("KoruiWrap.entry[0]")
            GlobalScope.launch(KoruiDispatcher) {
                entry(ctx)
            }
            println("KoruiWrap.entry[1]")
            //}
            println("KoruiWrap.pentry[2]")
        }

        val mevent = MouseEvent()

        private fun mouseEvent(etype: MouseEvent.Type, ex: Int, ey: Int, ebutton: Int) {
            light.dispatch(mevent.apply {
                this.type = etype
                this.x = ex
                this.y = ey
                this.buttons = 1 shl ebutton
                this.isAltDown = false
                this.isCtrlDown = false
                this.isShiftDown = false
                this.isMetaDown = false
                //this.scaleCoords = false
            })
        }

        override fun mouseUp(x: Int, y: Int, button: Int) {
            mouseEvent(MouseEvent.Type.UP, x, y, button)
            mouseEvent(
                MouseEvent.Type.CLICK,
                x,
                y,
                button
            ) // @TODO: Conditionally depending on the down x,y & time
        }

        override fun mouseDown(x: Int, y: Int, button: Int) =
            mouseEvent(MouseEvent.Type.DOWN, x, y, button)

        override fun mouseMoved(x: Int, y: Int) = mouseEvent(MouseEvent.Type.MOVE, x, y, 0)

        val keyEvent = KeyEvent()

        override fun keyDownUp(char: Char, modifiers: Int, keyCode: Int, pressed: Boolean) {
            val key = KeyCodesToKeys[keyCode] ?: CharToKeys[char] ?: Key.UNKNOWN
            //println("keyDownUp: char=$char, modifiers=$modifiers, keyCode=${keyCode.toInt()}, key=$key, pressed=$pressed")
            light.dispatch(keyEvent.apply {
                this.type =
                    if (pressed) KeyEvent.Type.DOWN else KeyEvent.Type.UP
                this.id = 0
                this.key = key
                this.keyCode = keyCode
                this.character = char
            })
        }

        val resizedEvent = ReshapeEvent()
        override fun windowDidResize(width: Int, height: Int, context: NSOpenGLContext?) {
            //macTrace("windowDidResize")
            ag.resized(width, height)
            light.dispatch(resizedEvent.apply {
                this.width = width
                this.height = height
            })
            render(context)
        }

        override fun render(context: NSOpenGLContext?) {
            //macTrace("render")

            myNativeCoroutineDispatcher.executeStep()

            //context?.flushBuffer()
            context?.makeCurrentContext()
            ag.onRender(ag)
            context?.flushBuffer()
        }
    })
    app.setActivationPolicy(NSApplicationActivationPolicy.NSApplicationActivationPolicyRegular)
    app.activateIgnoringOtherApps(true)
    myNativeCoroutineDispatcher.executeStep()
    app.run()
}

/*
actual val KoruiDispatcher: CoroutineDispatcher get() = MyNativeDispatcher

@kotlin.native.ThreadLocal
object MyNativeDispatcher : CoroutineDispatcher(), Delay, DelayFrame {
	val ag: AG = AGOpenglFactory.create(Any()).create(Any())

	override fun dispatch(context: CoroutineContext, block: Runnable) {
		TODO()
	}

	override fun scheduleResumeAfterDelay(time: Long, unit: TimeUnit, continuation: CancellableContinuation<Unit>) {
		TODO()
	}

	override fun invokeOnTimeout(time: Long, unit: TimeUnit, block: Runnable): DisposableHandle {
		TODO()
	}

	override fun delayFrame(continuation: CancellableContinuation<Unit>) {
		TODO()
	}

	override fun toString() = "MyNativeDispatcher"
}
*/

/*
// @TOOD: kotlin-native if not ThreadLocal by lazy crashes. And If not by lazy, it crashes in depthFirstTraversal/FreezeSubgraph/initSharedInstance
actual object KoruiEventLoop {
	//actual fun create(): EventLoop = MacosNativeEventLoop()
	actual fun create(): EventLoop = MacosNativeEventLoop
}

actual val KoruiDispatcher: CoroutineDispatcher get() = MyNativeDispatcher

@ThreadLocal
//open class MacosNativeEventLoop : EventLoop() {
object MacosNativeEventLoop : EventLoop() {
	//var app: NSApplication? by atomicRef<NSApplication?>(null)

	override fun loop() {

	}
}
*/

// AGOpenglFactory.create(Any()).create(Any())

class WindowConfig(val width: Int, val height: Int, val title: String)

private class MyAppDelegate(val ag: AG, val windowConfig: WindowConfig, val handler: MyAppHandler) : NSObject(),
    NSApplicationDelegateProtocol {
    val mainDisplayRect = NSScreen.mainScreen()!!.frame
    val windowRect = mainDisplayRect.useContents<CGRect, CValue<CGRect>> {
        NSMakeRect(
            (size.width * 0.5 - windowConfig.width * 0.5),
            (size.height * 0.5 - windowConfig.height * 0.5),
            windowConfig.width.toDouble(),
            windowConfig.height.toDouble()
        )
    }

    val windowStyle = NSWindowStyleMaskTitled or NSWindowStyleMaskMiniaturizable or
        NSWindowStyleMaskClosable or NSWindowStyleMaskResizable

    val attrs = uintArrayOf(
        //NSOpenGLPFAOpenGLProfile,
        //NSOpenGLProfileVersion4_1Core,
        NSOpenGLPFAColorSize.convert(), 24.convert(),
        NSOpenGLPFAAlphaSize.convert(), 8.convert(),
        NSOpenGLPFADoubleBuffer.convert(),
        NSOpenGLPFADepthSize.convert(), 32.convert(),
        0.convert()
    )

    val pixelFormat = attrs.usePinned {
        NSOpenGLPixelFormat(it.addressOf(0).reinterpret<NSOpenGLPixelFormatAttributeVar>())
        //NSOpenGLPixelFormat.alloc()!!.initWithAttributes(it.addressOf(0).reinterpret())!!
    }

    private val openglView: NSOpenGLView = NSOpenGLView(NSMakeRect(0.0, 0.0, 16.0, 16.0), pixelFormat)
    private val appDelegate: AppDelegate = AppDelegate(handler, openglView, openglView?.openGLContext)

    private val window: NSWindow = NSWindow(windowRect, windowStyle, NSBackingStoreBuffered, false).apply {
        title = windowConfig.title
        opaque = true
        hasShadow = true
        preferredBackingLocation = NSWindowBackingLocationVideoMemory
        hidesOnDeactivate = false
        releasedWhenClosed = false

        openglView.setFrame(contentRectForFrameRect(frame))
        delegate = appDelegate

        setAcceptsMouseMovedEvents(true)
        setContentView(openglView)
        setContentMinSize(NSMakeSize(150.0, 100.0))
        //openglView.resignFirstResponder()
        openglView.setNextResponder(MyResponder(handler, openglView))
        //makeFirstResponder(MyResponder(handler, openglView))
        setNextResponder(MyResponder(handler, openglView))
    }
    //private val openglView: AppNSOpenGLView

    override fun applicationShouldTerminateAfterLastWindowClosed(app: NSApplication): Boolean {
        println("applicationShouldTerminateAfterLastWindowClosed")
        return true
    }

    override fun applicationWillFinishLaunching(notification: NSNotification) {
        println("applicationWillFinishLaunching")
        window.makeKeyAndOrderFront(this)
    }

    override fun applicationDidFinishLaunching(notification: NSNotification) {
        //val data = decodeImageData(readBytes("icon.jpg"))
        //println("${data.width}, ${data.height}")

        openglView.openGLContext?.makeCurrentContext()
        try {
            handler.init(openglView.openGLContext)
            handler.render(openglView.openGLContext)
            appDelegate.timer = NSTimer.scheduledTimerWithTimeInterval(1.0 / 60.0, true, ::timer)
        } catch (e: Throwable) {
            e.printStackTrace()
            window.close()
        }
    }


    private fun timer(timer: NSTimer?) {
        //println("TIMER")
        handler.render(openglView?.openGLContext)
    }

    override fun applicationWillTerminate(notification: NSNotification) {
        println("applicationWillTerminate")
        // Insert code here to tear down your application

    }
}

class MyResponder(val handler: MyAppHandler, val openGLView: NSOpenGLView) : NSResponder() {
    override fun acceptsFirstResponder(): Boolean {
        return true
    }

    fun getHeight(): Int = openGLView.bounds.useContents<CGRect, Int> { size.height.toInt() }

    override fun mouseUp(event: NSEvent) {
        super.mouseUp(event)
        event.locationInWindow.useContents<CGPoint, Unit> {
            val rx = x.toInt()
            val ry = getHeight() - y.toInt()
            //println("mouseUp($rx,$ry)")
            handler.mouseUp(rx, ry, event.buttonNumber.toInt())
        }
    }

    override fun mouseDown(event: NSEvent) {
        super.mouseDown(event)
        event.locationInWindow.useContents<CGPoint, Unit> {
            val rx = x.toInt()
            val ry = getHeight() - y.toInt()
            //println("mouseDown($rx,$ry)")
            handler.mouseDown(rx, ry, event.buttonNumber.toInt())
        }
    }

    override fun mouseMoved(event: NSEvent) {
        super.mouseMoved(event)
        event.locationInWindow.useContents<CGPoint, Unit> {
            val rx = x.toInt()
            val ry = getHeight() - y.toInt()
            //println("mouseMoved($rx,$ry)")
            handler.mouseMoved(rx, ry)
        }
    }

    override fun mouseDragged(event: NSEvent) {
        super.mouseDragged(event)
        event.locationInWindow.useContents<CGPoint, Unit> {
            val rx = x.toInt()
            val ry = getHeight() - y.toInt()
            //println("mouseDragged($rx,$ry)")
            handler.mouseMoved(rx, ry)
        }
    }

    fun keyDownUp(event: NSEvent, pressed: Boolean) {
        val str = event.charactersIgnoringModifiers ?: "\u0000"
        val c = str.getOrNull(0) ?: '\u0000'
        val cc = c.toInt().toChar()
        //println("keyDownUp")
        handler.keyDownUp(cc, event.modifierFlags.convert(), event.keyCode.toInt(), pressed)
    }

    override fun keyDown(event: NSEvent) {
        super.keyDown(event)
        keyDownUp(event, true)
    }

    override fun keyUp(event: NSEvent) {
        super.keyUp(event)
        keyDownUp(event, false)
    }
}

fun macTrace(str: String) {
    println(str)
}

interface MyAppHandler {
    fun init(context: NSOpenGLContext?)
    fun mouseUp(x: Int, y: Int, button: Int)
    fun mouseDown(x: Int, y: Int, button: Int)
    fun mouseMoved(x: Int, y: Int)
    fun keyDownUp(char: Char, modifiers: Int, keyCode: Int, pressed: Boolean)
    fun windowDidResize(width: Int, height: Int, context: NSOpenGLContext?)
    fun render(context: NSOpenGLContext?)
}

class AppDelegate(
    val handler: MyAppHandler,
    val openGLView: NSOpenGLView,
    var openGLContext: NSOpenGLContext? = null
) : NSObject(), NSWindowDelegateProtocol {
    var timer: NSTimer? = null

    override fun windowShouldClose(sender: NSWindow): Boolean {
        println("windowShouldClose")
        return true
    }

    override fun windowWillClose(notification: NSNotification) {
        println("windowWillClose")
        //openGLContext = null
//
        //timer?.invalidate()
        //timer = null
//
        //NSApplication.sharedApplication().stop(this)
    }

    override fun windowDidResize(notification: NSNotification) {
        println("windowDidResize")
        openGLView.bounds.useContents<CGRect, Unit> {
            val bounds = this
            handler.windowDidResize(bounds.size.width.toInt(), bounds.size.height.toInt(), openGLContext)
            Unit
        }
    }
}


val KeyCodesToKeys = mapOf(
    0x24 to Key.ENTER,
    0x4C to Key.ENTER,
    0x30 to Key.TAB,
    0x31 to Key.SPACE,
    0x33 to Key.DELETE,
    0x35 to Key.ESCAPE,
    0x37 to Key.META,
    0x38 to Key.LEFT_SHIFT,
    0x39 to Key.CAPS_LOCK,
    0x3A to Key.LEFT_ALT,
    0x3B to Key.LEFT_CONTROL,
    0x3C to Key.RIGHT_SHIFT,
    0x3D to Key.RIGHT_ALT,
    0x3E to Key.RIGHT_CONTROL,
    0x7B to Key.LEFT,
    0x7C to Key.RIGHT,
    0x7D to Key.DOWN,
    0x7E to Key.UP,
    0x48 to Key.VOLUME_UP,
    0x49 to Key.VOLUME_DOWN,
    0x4A to Key.MUTE,
    0x72 to Key.HELP,
    0x73 to Key.HOME,
    0x74 to Key.PAGE_UP,
    0x75 to Key.DELETE,
    0x77 to Key.END,
    0x79 to Key.PAGE_DOWN,
    0x3F to Key.FUNCTION,
    0x7A to Key.F1,
    0x78 to Key.F2,
    0x76 to Key.F4,
    0x60 to Key.F5,
    0x61 to Key.F6,
    0x62 to Key.F7,
    0x63 to Key.F3,
    0x64 to Key.F8,
    0x65 to Key.F9,
    0x6D to Key.F10,
    0x67 to Key.F11,
    0x6F to Key.F12,
    0x69 to Key.F13,
    0x6B to Key.F14,
    0x71 to Key.F15,
    0x6A to Key.F16,
    0x40 to Key.F17,
    0x4F to Key.F18,
    0x50 to Key.F19,
    0x5A to Key.F20
)


val CharToKeys = mapOf(
    'a' to Key.A, 'A' to Key.A,
    'b' to Key.B, 'B' to Key.B,
    'c' to Key.C, 'C' to Key.C,
    'd' to Key.D, 'D' to Key.D,
    'e' to Key.E, 'E' to Key.E,
    'f' to Key.F, 'F' to Key.F,
    'g' to Key.G, 'G' to Key.G,
    'h' to Key.H, 'H' to Key.H,
    'i' to Key.I, 'I' to Key.I,
    'j' to Key.J, 'J' to Key.J,
    'k' to Key.K, 'K' to Key.K,
    'l' to Key.L, 'L' to Key.L,
    'm' to Key.M, 'M' to Key.M,
    'n' to Key.N, 'N' to Key.N,
    'o' to Key.O, 'O' to Key.O,
    'p' to Key.P, 'P' to Key.P,
    'q' to Key.Q, 'Q' to Key.Q,
    'r' to Key.R, 'R' to Key.R,
    's' to Key.S, 'S' to Key.S,
    't' to Key.T, 'T' to Key.T,
    'u' to Key.U, 'U' to Key.U,
    'v' to Key.V, 'V' to Key.V,
    'w' to Key.W, 'W' to Key.W,
    'x' to Key.X, 'X' to Key.X,
    'y' to Key.Y, 'Y' to Key.Y,
    'z' to Key.Z, 'Z' to Key.Z,
    '0' to Key.N0, '1' to Key.N1, '2' to Key.N2, '3' to Key.N3, '4' to Key.N4,
    '5' to Key.N5, '6' to Key.N6, '7' to Key.N7, '8' to Key.N8, '9' to Key.N9
)
