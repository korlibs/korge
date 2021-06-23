package com.soywiz.korgw.osx

import com.soywiz.kgl.KmlGl
import com.soywiz.kgl.checkedIf
import com.soywiz.kgl.logIf
import com.soywiz.klock.PerformanceCounter
import com.soywiz.korag.AGOpengl
import com.soywiz.korev.Key
import com.soywiz.korev.KeyEvent
import com.soywiz.korev.MouseButton
import com.soywiz.korev.MouseEvent
import com.soywiz.korgw.*
import com.soywiz.korgw.platform.BaseOpenglContext
import com.soywiz.korgw.platform.INativeGL
import com.soywiz.korgw.platform.NativeKgl
import com.soywiz.korgw.platform.NativeLoad
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.format.PNG
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.net.URL
import com.soywiz.korio.util.OS
import com.soywiz.korio.util.Once
import com.sun.jna.Callback
import com.sun.jna.Library
import java.nio.ByteBuffer
import kotlin.coroutines.*

class MacAG(val window: Long, val checkGl: Boolean, val logGl:Boolean) : AGOpengl() {
    override val gles: Boolean = true
    //override val glSlVersion = 140
    //override val glSlVersion = 100
    override val gl: KmlGl = MacKmlGL().checkedIf(checkGl).logIf(logGl)
    override val nativeComponent: Any = window
}

open class MacKmlGL : NativeKgl(MacGL)

interface MacGL : INativeGL, Library {
    fun CGLSetParameter(vararg args: Any?)
    fun CGLEnable(vararg args: Any?)

    companion object : MacGL by NativeLoad("OpenGL")
}

private fun ByteArray.toNSData(): Long = NSClass("NSData").alloc().msgSend("initWithBytes:length:", ByteBuffer.wrap(this), this.size)

class MacosGLContext(var contentView: Long, val window: Long, val quality: GameWindow.Quality) : BaseOpenglContext {
    companion object {
        const val NSOpenGLPFAMultisample = 59
        const val NSOpenGLPFASampleBuffers = 55
        const val NSOpenGLPFASamples = 56
        const val NSOpenGLPFADoubleBuffer = 5
        const val NSOpenGLPFAColorSize = 8
        const val NSOpenGLPFAAlphaSize = 11
        const val NSOpenGLPFADepthSize = 12
        const val NSOpenGLPFAStencilSize = 13
        const val NSOpenGLPFAAccumSize = 14
    }

    val attrs: IntArray by lazy {
        val antialias = (this.quality != GameWindow.Quality.PERFORMANCE)
        val antialiasArray = if (antialias) intArrayOf(
            NSOpenGLPFAMultisample,
            NSOpenGLPFASampleBuffers, 1,
            NSOpenGLPFASamples, 4
        ) else intArrayOf()
        intArrayOf(
            *antialiasArray,
            //NSOpenGLPFAOpenGLProfile,
            //NSOpenGLProfileVersion4_1Core,
            NSOpenGLPFADoubleBuffer,
            NSOpenGLPFAColorSize, 24,
            NSOpenGLPFAAlphaSize, 8,
            NSOpenGLPFADepthSize, 24,
            NSOpenGLPFAStencilSize, 8,
            NSOpenGLPFAAccumSize, 0,
            0
        )
    }

    val pixelFormat = NSClass("NSOpenGLPixelFormat").alloc().msgSend("initWithAttributes:", attrs)
    val openGLContext = NSClass("NSOpenGLContext").alloc().msgSend("initWithFormat:shareContext:", pixelFormat, null)

    init {
        //println("pixelFormat: $pixelFormat")
        //println("openGLContext: $openGLContext")
        setView(contentView)
    }

    override val scaleFactor: Double get() = if (window != 0L) window.msgSendCGFloat("backingScaleFactor").toDouble() else 1.0

    override fun makeCurrent() {
        openGLContext.msgSend("makeCurrentContext")
    }

    override fun swapBuffers() {
        GL.glFlush()
        openGLContext.msgSend("flushBuffer")
    }

    fun clearDrawable() {
        openGLContext.msgSend("clearDrawable")
    }

    fun setView(contentView: Long) {
        openGLContext.msgSend("setView:", contentView)
    }

    fun setParameters() {
        val dims = intArrayOf(720, 480)
        GL.CGLSetParameter(openGLContext, 304, dims)
        GL.CGLEnable(openGLContext, 304)
    }
}

internal val isOSXMainThread get() = OS.isMac && (NSClass("NSThread").msgSend("isMainThread") != 0L)

private val _initializeMacOnce = Once()
fun initializeMacOnce() = _initializeMacOnce {
    if (OS.isMac) {
        // https://indiestack.com/2016/12/touch-bar-crash-protection/
        //[[NSUserDefaults standardUserDefaults] registerDefaults:[NSDictionary dictionaryWithObject:[NSNumber numberWithBool:NO] forKey:@"NSFunctionBarAPIEnabled"]];
        NSClass("NSUserDefaults").msgSend("standardUserDefaults").msgSend(
            "registerDefaults:",
            NSClass("NSDictionary").msgSend(
                "dictionaryWithObject:forKey:",
                NSClass("NSNumber").msgSend("numberWithBool:", 0),
                NSString("NSFunctionBarAPIEnabled")
            )
        )
    }
}

class MacGameWindow(val checkGl: Boolean, val logGl: Boolean) : GameWindow() {
    init {
        initializeMacOnce()
    }

    val autoreleasePool = NSClass("NSAutoreleasePool").alloc().msgSend("init")

    companion object {
        val isMainThread get() = isOSXMainThread
    }

    val initialWidth = 128
    val initialHeight = 128
    var contentView: Long = 0L

    val app = NSClass("NSApplication").msgSend("sharedApplication")
    val sharedApp = app


    val windowWillClose: WindowWillCloseCallback = object : WindowWillCloseCallback {
        override fun invoke(self: Long, _sel: Long, sender: Long): Long {
            running = false
            System.exit(0)
            return 0L
        }
    }

    val applicationShouldTerminate: ApplicationShouldTerminateCallback = object : ApplicationShouldTerminateCallback {
        override fun invoke(self: Long, _sel: Long, sender: Long): Long {
            //println("applicationShouldTerminateCallback")
            running = false
            System.exit(0)
            return 0L
        }
    }

    val mouseEventHandler: ObjcCallbackVoid = ObjcCallbackVoid { self, _sel, sender ->
        val type = sender.msgSend("type")
        val point = sender.msgSendNSPoint("locationInWindow")
        val buttonNumber = sender.msgSend("buttonNumber")
        val clickCount = sender.msgSend("clickCount")

        val rect = MyNSRect()
        contentView.msgSend_stret(rect, "frame")

        val rect2 = MyNSRect()
        window.msgSend_stret(rect2, "frame")

        val rect3 = MyNSRect()
        window.msgSend_stret(rect3, "contentRectForFrameRect:", rect2)

        glCtx?.setParameters()

        val point2 = NSPoint(point.x, rect.height - point.y)
        val x = point2.x.toDouble()
        val y = point2.y.toDouble()
        val button = MouseButton[buttonNumber.toInt()]

        //val res = NSClass("NSEvent").id.msgSend_stret(data, "mouseLocation")

        val selName = ObjectiveC.sel_getName(_sel)
        val ev = when (selName) {
            "mouseMoved:" -> MouseEvent.Type.MOVE
            "mouseDown:", "leftMouseDown:", "rightMouseDown:", "otherMouseDown:" -> MouseEvent.Type.DOWN
            "mouseUp:", "leftMouseUp:", "rightMouseUp:", "otherMouseUp:" -> MouseEvent.Type.UP
            else -> MouseEvent.Type.MOVE
        }

        val factor = backingScaleFactor

        val sx = x * factor
        val sy = y * factor

        dispatchSimpleMouseEvent(ev, 0, sx.toInt(), sy.toInt(), button, simulateClickOnUp = false)
        //println("MOUSE EVENT ($type) ($selName) from NSWindow! $point2 : $buttonNumber : $clickCount, $rect, $rect2, $rect3")
    }

    val keyEventHandler: ObjcCallbackVoid = ObjcCallbackVoid { self, _sel, sender ->
        val characters = NSString(sender.msgSend("characters")).toString()
        val charactersIgnoringModifiers = NSString(sender.msgSend("charactersIgnoringModifiers")).toString()
        val char = charactersIgnoringModifiers.getOrNull(0) ?: '\u0000'
        val keyCode = sender.msgSend("keyCode").toInt()

        val key = KeyCodesToKeys[keyCode] ?: CharToKeys[char] ?: Key.UNKNOWN

        val selName = ObjectiveC.sel_getName(_sel)
        val ev = when (selName) {
            "keyDown:" -> KeyEvent.Type.DOWN
            "keyUp:" -> KeyEvent.Type.UP
            "keyPress:" -> KeyEvent.Type.TYPE
            else -> KeyEvent.Type.TYPE
        }

        //println("keyDown: $selName : $characters : ${char.toInt()} : $charactersIgnoringModifiers : $keyCode : $key")
        dispatchKeyEvent(ev, 0, char, key, keyCode)
    }


    val windowTimer: ObjcCallbackVoid = ObjcCallbackVoid { self, _sel, sender ->
        renderOpengl(update = true)
    }

    fun windowDidResize() {
        //println("windowDidResize:")
        val frect = MyNSRect()
        window.msgSend_stret(frect, "frame")

        val rect = frect

        val factor = backingScaleFactor

        val barSize = 21.0
        rect.y += barSize
        rect.height -= barSize

        val width = rect.width
        val height = rect.height
        val scaledWidth = rect.width * factor
        val scaledHeight = rect.height * factor

        //val rect = MyNativeNSRect()
        //window.msgSend_stret(rect, "contentRectForFrameRect:", frect)

        //val rect = MyNativeNSRect()
        //window.msgSend_stret(rect, "frame")

        //println("factor=$factor, width=$width, height=$height, scaledWidth=$scaledWidth, scaledHeight=$scaledHeight")

        glCtx?.clearDrawable()
        this@MacGameWindow.width = (scaledWidth).toInt()
        this@MacGameWindow.height = (scaledHeight).toInt()
        contentView.msgSend("setBoundsSize:", MyNativeNSPoint.ByValue(scaledWidth, scaledHeight))
        glCtx?.setView(contentView)
        dispatchReshapeEvent(rect.x.toInt(), rect.y.toInt(), scaledWidth.toInt(), scaledHeight.toInt())
        renderOpengl()
    }

    private var lastBackingScaleFactor = 0.0

    fun renderOpengl(update: Boolean = false) {
        val startTime = PerformanceCounter.reference
        // This allows to detect a change in the scale factor of the window without having to resize (changing resolution)
        if (lastBackingScaleFactor != backingScaleFactor) {
            lastBackingScaleFactor = backingScaleFactor
            windowDidResize()
            return
        }
        //println(backingScaleFactor)

        glCtx?.makeCurrent()
        GL.glViewport(0, 0, width, height)
        //GL.glClearColor(.3f, .7f, 1f, 1f)
        GL.glClearColor(.3f, .3f, .3f, 1f)
        GL.glClear(GL.GL_COLOR_BUFFER_BIT)

        //println("RENDER")

        frame(update, startTime)

        glCtx?.swapBuffers()
    }

    val windowDidResize = ObjcCallbackVoid { self, _sel, notification ->
        windowDidResize()
    }

    val rect = NSRect(0, 0, initialWidth, initialHeight)
    val window = AllocateClassAndRegister("MyNSWindow", "NSWindow") {
        // Mouse
        addMethod("mouseEntered:", mouseEventHandler, "v@:@")
        addMethod("mouseExited:", mouseEventHandler, "v@:@")
        addMethod("mouseDragged:", mouseEventHandler, "v@:@")
        addMethod("mouseMoved:", mouseEventHandler, "v@:@")
        addMethod("mouseDown:", mouseEventHandler, "v@:@")
        addMethod("mouseUp:", mouseEventHandler, "v@:@")
        addMethod("rightMouseDragged:", mouseEventHandler, "v@:@")
        addMethod("rightMouseMoved:", mouseEventHandler, "v@:@")
        addMethod("rightMouseDown:", mouseEventHandler, "v@:@")
        addMethod("rightMouseUp:", mouseEventHandler, "v@:@")
        addMethod("otherMouseDragged:", mouseEventHandler, "v@:@")
        addMethod("otherMouseMoved:", mouseEventHandler, "v@:@")
        addMethod("otherMouseDown:", mouseEventHandler, "v@:@")
        addMethod("otherMouseUp:", mouseEventHandler, "v@:@")

        // Keyboard
        addMethod("keyDown:", keyEventHandler, "v@:@")
        addMethod("keyUp:", keyEventHandler, "v@:@")
        addMethod("keyPress:", keyEventHandler, "v@:@")
        addMethod("flagsChanged:", ObjcCallbackVoid { self, _sel, sender ->
            val modifierFlags = sender.msgSend("modifierFlags")
            //println("flags changed! : $modifierFlags")
        }, "v@:@")

        // Other
        addMethod("windowTimer:", windowTimer, "v@:@")
        addMethod("windowDidResize:", windowDidResize, "v@:@")
    }.alloc().msgSend(
        "initWithContentRect:styleMask:backing:defer:",
        rect,
        NSWindowStyleMaskTitled or NSWindowStyleMaskClosable or NSWindowStyleMaskMiniaturizable or NSWindowStyleMaskResizable or NSWindowStyleMaskResizable,
        NSBackingStoreBuffered,
        false
    ).also {
        it.msgSend("setTitle:", NSString("Korgw"))
        it.msgSend("setCollectionBehavior:", NSWindowCollectionBehaviorFullScreenPrimary)
        //[self.window setCollectionBehavior:NSWindowCollectionBehaviorFullScreenPrimary];
    }

    override val key: CoroutineContext.Key<*>
        get() = super.key
    override val ag: MacAG = MacAG(window, checkGl, logGl)
    override val coroutineDispatcher: GameWindowCoroutineDispatcher
        get() = super.coroutineDispatcher
    override var title: String
        get() = NSString(window.msgSend("title")).toString()
        set(value) {
            window.msgSend("setTitle:", NSString(value))
        }
    override var width: Int = initialWidth
        protected set
    override var height: Int = initialHeight
        protected set
    override var icon: Bitmap? = null
        set(value) {
            field = value
            //println("MacosGameWindow. Setting icon: $value")
            if (value != null) {
                //val size = image.msgSendNSPoint("size")
                app.msgSend("setApplicationIconImage:", NSClass("NSImage").alloc().msgSend("initWithData:", PNG.encode(value).toNSData()))
            } else {
                app.msgSend("setApplicationIconImage:", 0L)
            }
        }
    override var fullscreen: Boolean
        get() = (window.msgSend("styleMask") and NSWindowStyleMaskFullScreen.toLong()) == NSWindowStyleMaskFullScreen.toLong()
        set(value) {
            if (this.fullscreen != value) {
                println("toggleFullScreen: $window : $value")
                window.msgSend("toggleFullScreen:", app)
            }
        }
    override var visible: Boolean
        get() = super.visible
        set(value) {}
    override var quality: Quality
        get() = super.quality
        set(value) {}

    override fun setSize(width: Int, height: Int) {
        window.msgSend("setContentSize:", MyNativeNSPoint.ByValue(width, height))
        this.width = width
        this.height = height

    }

    override suspend fun browse(url: URL) {
        super.browse(url)
    }

    override suspend fun alert(message: String) {
        super.alert(message)
    }

    override suspend fun confirm(message: String): Boolean {
        return super.confirm(message)
    }

    override suspend fun prompt(message: String, default: String): String {
        return super.prompt(message, default)
    }

    override suspend fun openFileDialog(filter: FileFilter?, write: Boolean, multi: Boolean, currentDir: VfsFile?): List<VfsFile> {
        return super.openFileDialog(filter, write, multi, currentDir)
    }

    override fun close() {
        super.close()
        autoreleasePool.msgSend("drain")
        // @TODO: Close window gracefully?
        window.msgSend("close")
        //exitProcess(0)
    }

    //var running = true

    var glCtx: MacosGLContext? = null

    val backingScaleFactor get() = if (window != 0L) window.msgSendCGFloat("backingScaleFactor").toDouble() else 1.0

    val timerCallback = object : Callback {
        fun callback() {
            println("TIMER!")
        }
    }

    val timerCallback2 = ObjcCallbackVoid { self, _sel, notification ->
        println("frame!")
    }

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        launchImmediately(getCoroutineDispatcherWithCurrentContext()) {
            entry()
        }

        app.msgSend("setDelegate:", AllocateClassAndRegister("AppDelegate", "NSObject", "NSApplicationDelegate") {
            addMethod(("applicationShouldTerminate:"), applicationShouldTerminate, "@:@")
            addMethod(("applicationDidBecomeActive:"), ObjcCallbackVoidEmpty {
                //println("applicationDidBecomeActive")
                renderOpengl()
            }, "@:@")
            addMethod("applicationWillFinishLaunching:", ObjcCallbackVoidEmpty {
                //println("applicationWillFinishLaunching")
            }, "@:@")
            addMethod("applicationDidFinishLaunching:", ObjcCallbackVoidEmpty {
                val processName = NSString(NSClass("NSProcessInfo").msgSend("processInfo").msgSend("processName"))

                app.msgSend("setMainMenu:", NSMenu {
                    addItem(NSMenuItem {
                        setSubmenu(NSMenu {
                            //addItem(NSMenuItem("About $processName", "onAboutTouched:", ""))
                            addItem(NSMenuItem("Quit $processName", "terminate:", "q"))
                        })
                    })
                }.id)

                app.msgSend("setActivationPolicy:", 0)
                app.msgSend("activateIgnoringOtherApps:", true)

                //println("applicationDidFinishLaunching")
            }, "@:@")
        }.alloc().msgSend("init"))
        app.msgSend("finishLaunching")

        window.msgSend("setReleasedWhenClosed:", 0L)
        window.msgSend("cascadeTopLeftFromPoint:", NSPoint(20, 20))

        contentView = window.msgSend("contentView")
        glCtx = MacosGLContext(contentView, window, quality)
        //println("contentView: $contentView")
        contentView.msgSend("setWantsBestResolutionOpenGLSurface:", true)

        //val openglView = NSClass("NSOpenGLView").alloc().msgSend("initWithFrame:pixelFormat:", MyNativeNSRect.ByValue(0, 0, 512, 512), pixelFormat)
        //val openGLContext = openglView.msgSend("openGLContext")
        //window.msgSend("contentView", openglView)
        //val contentView = window.msgSend("contentView")


        window.msgSend("setAcceptsMouseMovedEvents:", true)
        window.msgSend("setBackgroundColor:", NSClass("NSColor").msgSend("blackColor"))
        window.msgSend("makeKeyAndOrderFront:", app)
        window.msgSend("center")

        app.msgSend("activateIgnoringOtherApps:", true)

        window.msgSend("makeKeyWindow")
        window.msgSend("setIsVisible:", true)

        window.msgSend("setAcceptsMouseMovedEvents:", true)
        window.msgSend("setDelegate:", AllocateClassAndRegister("WindowDelegate", "NSObject", "NSWindowDelegate") {
            addMethod("windowWillClose:", windowWillClose, "v@:@")
            addMethod("windowDidExpose:", ObjcCallbackVoid { self, _sel, notification -> renderOpengl() }, "v@:@")
            addMethod("windowDidUpdate:", ObjcCallbackVoid { self, _sel, notification -> renderOpengl() }, "v@:@")
            addMethod("windowDidResize:", windowDidResize, "v@:@")
        }.alloc().msgSend("init"))

        windowDidResize()
        dispatchInitEvent()


        //contentView.msgSend("fullScreenEnable")

        //val NSApp = Foundation.NATIVE.getGlobalVariableAddress("NSApp")
        val NSDefaultRunLoopMode = Foundation.NATIVE.getGlobalVariableAddress("NSDefaultRunLoopMode")
        //val NSDefaultRunLoopMode = Foundation.NATIVE.getGlobalVariableAddress("NSDefaultRunLoopMode")
        //println("NSDefaultRunLoopMode: $NSDefaultRunLoopMode")

        //NSClass("NSTimer").msgSend("scheduledTimerWithTimeInterval:repeats:block:", (1.0 / 60.0), true, timerCallback)
        NSClass("NSTimer").msgSend(
            "scheduledTimerWithTimeInterval:target:selector:userInfo:repeats:",
            (1.0 / 60.0),
            window,
            sel("windowTimer:"),
            null,
            true
        )


        app.msgSend("run")

        autoreleasePool.msgSend("drain")
    }
}

val NSWindowStyleMaskTitled = 1 shl 0
val NSWindowStyleMaskClosable = 1 shl 1
val NSWindowStyleMaskMiniaturizable = 1 shl 2
val NSWindowStyleMaskResizable = 1 shl 3
val NSWindowStyleMaskFullScreen = 1 shl 14
val NSWindowStyleMaskFullSizeContentView = 1 shl 15

val NSBackingStoreBuffered = 2

private val NSWindowCollectionBehaviorFullScreenPrimary = (1 shl 7).toLong()


internal val KeyCodesToKeys = mapOf(
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

internal val CharToKeys = mapOf(
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
