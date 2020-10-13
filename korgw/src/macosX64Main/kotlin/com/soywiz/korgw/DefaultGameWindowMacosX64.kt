package com.soywiz.korgw

import com.soywiz.klock.*
import com.soywiz.klock.hr.*
import com.soywiz.kmem.startAddressOf
import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.cg.toCgFloat
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.AppKit.*
import platform.CoreGraphics.*
import platform.CoreVideo.*
import platform.Foundation.*
import platform.darwin.*
import kotlin.native.concurrent.AtomicInt

private fun ByteArray.toNsData(): NSData {
    val array = this
    return memScoped {
        array.usePinned { arrayPin ->
            NSData.dataWithBytes(arrayPin.startAddressOf, array.size.convert())
        }
    }
}

val frameRequestNumber = AtomicInt(0)

actual fun CreateDefaultGameWindow(): GameWindow = object : GameWindow(), DoRenderizable {
    val gameWindow = this
    val gameWindowStableRef = StableRef.create(gameWindow)
    val app = NSApplication.sharedApplication()
    val controller = WinController()

    val windowStyle = NSWindowStyleMaskTitled or NSWindowStyleMaskMiniaturizable or
        NSWindowStyleMaskClosable or NSWindowStyleMaskResizable

    val attrs: UIntArray by lazy {
        val antialias = (this.quality != GameWindow.Quality.PERFORMANCE)
        val antialiasArray = if (antialias) intArrayOf(
            NSOpenGLPFAMultisample.convert(),
            NSOpenGLPFASampleBuffers.convert(), 1.convert(),
            NSOpenGLPFASamples.convert(), 4.convert()
        ) else intArrayOf()
        intArrayOf(
            *antialiasArray,
            //NSOpenGLPFAOpenGLProfile,
            //NSOpenGLProfileVersion4_1Core,
            NSOpenGLPFAColorSize.convert(), 24.convert(),
            NSOpenGLPFAAlphaSize.convert(), 8.convert(),
            NSOpenGLPFADoubleBuffer.convert(),
            NSOpenGLPFADepthSize.convert(), 32.convert(),
            0.convert()
        ).toUIntArray()
    }

    val pixelFormat by lazy {
        attrs.usePinned {
            NSOpenGLPixelFormat(it.addressOf(0).reinterpret<NSOpenGLPixelFormatAttributeVar>())
            //NSOpenGLPixelFormat.alloc()!!.initWithAttributes(it.addressOf(0).reinterpret())!!
        }
    }

    val windowConfigWidth = 640
    val windowConfigHeight = 480
    val windowConfigTitle = ""

    val windowRect: CValue<NSRect> = run {
        val frame = NSScreen.mainScreen()!!.frame
        NSMakeRect(
            (frame.width * 0.5 - windowConfigWidth * 0.5),
            (frame.height * 0.5 - windowConfigHeight * 0.5),
            windowConfigWidth.toDouble(),
            windowConfigHeight.toDouble()
        )
    }

    private val openglView: NSOpenGLView = NSOpenGLView(NSMakeRect(0.0, 0.0, 16.0, 16.0), pixelFormat)
    var timer: NSTimer? = null

    private var responder: NSResponder

    private val window: NSWindow = NSWindow(windowRect, windowStyle, NSBackingStoreBuffered, false).apply {
        setIsVisible(false)
        title = windowConfigTitle
        opaque = true
        hasShadow = true
        preferredBackingLocation = NSWindowBackingLocationVideoMemory
        hidesOnDeactivate = false
        releasedWhenClosed = false

        openglView.setFrame(contentRectForFrameRect(frame))
        delegate = object : NSObject(), NSWindowDelegateProtocol {
            override fun windowShouldClose(sender: NSWindow): Boolean {
                //println("windowShouldClose")
                return true
            }

            override fun windowWillClose(notification: NSNotification) {
                //println("windowWillClose")
            }

            override fun windowDidResize(notification: NSNotification) {
                doWindowDidResize()
            }
        }

        setAcceptsMouseMovedEvents(true)
        setContentView(openglView)
        setContentMinSize(NSMakeSize(150.0, 100.0))
        responder = object : NSResponder() {
            override fun acceptsFirstResponder(): Boolean = true

            fun getHeight() = openglView.bounds.height

            override fun mouseUp(event: NSEvent) {
                //super.mouseUp(event)
                val rx = event.locationInWindow.x.toInt()
                val ry = (getHeight() - event.locationInWindow.y).toInt()
                //println("mouseUp($rx,$ry)")
                val x = rx
                val y = ry
                val button = event.buttonNumber.toInt()

                mouseEvent(MouseEvent.Type.UP, x, y, button, event)
                mouseEvent(MouseEvent.Type.CLICK, x, y, button, event) // @TODO: Conditionally depending on the down x,y & time
            }

            override fun mouseDown(event: NSEvent) {
                //super.mouseDown(event)
                val rx = event.locationInWindow.x.toInt()
                val ry = (getHeight() - event.locationInWindow.y).toInt()
                //println("mouseDown($rx,$ry)")
                mouseEvent(MouseEvent.Type.DOWN, rx, ry, event.buttonNumber.toInt(), event)
            }

            override fun mouseMoved(event: NSEvent) {
                //super.mouseMoved(event)
                val rx = event.locationInWindow.x.toInt()
                val ry = (getHeight() - event.locationInWindow.y).toInt()
                //println("mouseMoved($rx,$ry)")
                mouseEvent(MouseEvent.Type.MOVE, rx, ry, 0, event)
            }

            private fun mouseEvent(etype: MouseEvent.Type, ex: Int, ey: Int, ebutton: Int, e: NSEvent) {
                val factor = backingScaleFactor
                val sx = ex * factor
                val sy = ey * factor

                dispatchMouseEvent(
                    id = 0,
                    type = etype,
                    x = sx.toInt(),
                    y = sy.toInt(),
                    button = MouseButton[ebutton],
                    buttons = e.buttonMask.toInt(),
                    isShiftDown = e.shift, isCtrlDown = e.ctrl, isAltDown = e.alt, isMetaDown = e.meta
                )
            }

            override fun mouseDragged(event: NSEvent) {
                super.mouseDragged(event)
                val rx = event.locationInWindow.x.toInt()
                val ry = (getHeight() - event.locationInWindow.y).toInt()
                //println("mouseDragged($rx,$ry)")
                mouseEvent(MouseEvent.Type.DRAG, rx, ry, 0, event)
            }

            fun keyDownUp(event: NSEvent, pressed: Boolean, e: NSEvent) {
                val str = event.charactersIgnoringModifiers ?: "\u0000"
                val c = str.getOrNull(0) ?: '\u0000'
                val cc = c.toInt().toChar()
                //println("keyDownUp")
                val char = cc
                val keyCode = event.keyCode.toInt()

                val key = KeyCodesToKeys[keyCode] ?: CharToKeys[char] ?: Key.UNKNOWN
                //println("keyDownUp: char=$char, keyCode=${keyCode.toInt()}, key=$key, pressed=$pressed, shift=${e.shift}, ctrl=${e.ctrl}, alt=${e.alt}, meta=${e.meta}")
                dispatchKeyEventEx(
                    type = if (pressed) KeyEvent.Type.DOWN else KeyEvent.Type.UP,
                    id = 0,
                    character = char,
                    key = key,
                    keyCode = keyCode,
                    shift = e.shift,
                    ctrl = e.ctrl,
                    alt = e.alt,
                    meta = e.meta
                )
            }

            override fun keyDown(event: NSEvent) {
                //super.keyDown(event)
                keyDownUp(event, true, event)
            }

            override fun keyUp(event: NSEvent) {
                //super.keyUp(event)
                keyDownUp(event, false, event)
            }

            //external override fun performKeyEquivalent(event: NSEvent): Boolean {
            //    return true
            //}
        }
        openglView.setNextResponder(responder)
        setNextResponder(responder)
        setIsVisible(false)
    }

    private fun doWindowDidResize() {
        //println("windowDidResize")

        val factor = backingScaleFactor
        val width = openglView.bounds.width
        val height = openglView.bounds.height
        val scaledWidth = width * factor
        val scaledHeight = height * factor
        //macTrace("windowDidResize")
        dispatchReshapeEvent(0, 0, scaledWidth.toInt(), scaledHeight.toInt())
        doRender()
    }

    @Suppress("RemoveRedundantCallsOfConversionMethods")
    private val backingScaleFactor: Double get() = window.backingScaleFactor.toDouble()
    private var lastBackingScaleFactor = 0.0

    override fun doRenderRequest() {
        //dispatch_async(dispatch_get_main_queue(), ::doRender)
        frameRequestNumber.increment()
    }

    fun doRender() {
        //println("doRender[0]")
        val startTime = PerformanceCounter.reference
        //macTrace("render")
        val context = openglView.openGLContext

        //println("doRender[1]")

        if (lastBackingScaleFactor != backingScaleFactor) {
            lastBackingScaleFactor = backingScaleFactor
            doWindowDidResize()
            return
        }

        //println("doRender[2]")

        //context?.flushBuffer()
        context?.makeCurrentContext()

        //println("doRender[3] : $context")
        //ag.clear(Colors.BLACK)
        //ag.onRender(ag)
        //dispatch(renderEvent)
        frame()
        context?.flushBuffer()

        //println("doRender[3]")
        val elapsed = PerformanceCounter.reference - startTime
        val available = counterTimePerFrame - elapsed
        coroutineDispatcher.executePending(available)
        //println("doRender[4]")
    }

    override val ag: AG = AGNative()

    //override val width: Int get() = window.frame.width.toInt()
    //override val height: Int get() = window.frame.height.toInt()
    override val width: Int get() = openglView.bounds.width.toInt()
    override val height: Int get() = openglView.bounds.height.toInt()

    override var title: String = ""
        set(value) {
            field = value
            window.title = value
        }

    override var icon: Bitmap? = null
        set(value) {
            field = value
            if (value != null) {
                app.setApplicationIconImage(NSImage(data = PNG.encode(value).toNsData()))
            }
        }
    override var fullscreen: Boolean
        get() = (window.styleMask and NSFullScreenWindowMask) == NSFullScreenWindowMask
        set(value) {
            if (fullscreen != value) {
                window.toggleFullScreen(window)
            }
        }
    override var visible: Boolean
        get() = window.visible
        set(value) {
            window.setIsVisible(value)
            if (value) {
                window.makeKeyAndOrderFront(this)
            }
            //if (value) {
            //    window.makeKeyAndOrderFront(this)
            //    app.activateIgnoringOtherApps(true)
            //} else {
            //    window.orderOut(this)
            //}
        }

    override fun setSize(width: Int, height: Int) {
        //val frame = NSScreen.mainScreen()!!.frame
        //val rect = NSMakeRect(
        //    ((frame.width - width) * 0.5), ((frame.height - height) * 0.5),
        //    width.toDouble(), height.toDouble()
        //)

        //window.setFrame(rect, true, false)
        window.setContentSize(NSMakeSize(width.toCgFloat(), height.toCgFloat()))
        window.center()
        //window.setFrameTopLeftPoint()
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

    override suspend fun openFileDialog(filter: String?, write: Boolean, multi: Boolean): List<VfsFile> {
        val openDlg: NSOpenPanel = NSOpenPanel().apply {
            setCanChooseFiles(true)
            setAllowsMultipleSelection(false)
            setCanChooseDirectories(false)
        }
        if (openDlg.runModalForDirectory(null, null).toInt() == NSOKButton.toInt()) {
            return openDlg.filenames().filterIsInstance<String>().map { localVfs(it) }
        } else {
            throw CancelException()
        }
    }

    override fun close() {
        super.close()
        window.close()
    }

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) = autoreleasepool {
        val agNativeComponent = Any()
        val ag: AG = AGOpenglFactory.create(agNativeComponent).create(agNativeComponent, AGConfig())

        val ccontext = kotlin.coroutines.coroutineContext
        app.delegate = object : NSObject(), NSApplicationDelegateProtocol {

            //private val openglView: AppNSOpenGLView

            override fun applicationShouldTerminateAfterLastWindowClosed(app: NSApplication): Boolean {
                //println("applicationShouldTerminateAfterLastWindowClosed")
                return true
            }

            override fun applicationWillFinishLaunching(notification: NSNotification) {
                //println("applicationWillFinishLaunching")
                //window.makeKeyAndOrderFront(this)
            }

            override fun applicationDidFinishLaunching(notification: NSNotification) {
                //val data = decodeImageData(readBytes("icon.jpg"))
                //println("${data.width}, ${data.height}")
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

                app.setActivationPolicy(NSApplicationActivationPolicy.NSApplicationActivationPolicyRegular)
                app.activateIgnoringOtherApps(true)

                openglView.openGLContext?.makeCurrentContext()
                try {
                    macTrace("init[a] -- bb")
                    macTrace("init[b]")
                    //println("KoruiWrap.pentry[0]")
                    //launch(KoruiDispatcher) { // Doesn't work!
                    //println("KoruiWrap.pentry[1]")
                    //println("KoruiWrap.entry[0]")
                    kotlinx.coroutines.GlobalScope.launch(getCoroutineDispatcherWithCurrentContext(ccontext)) {
                        entry()
                    }
                    //println("KoruiWrap.entry[1]")
                    //}
                    //println("KoruiWrap.pentry[2]")

                    doRender()
                    val useDisplayLink = Environment["MACOS_USE_DISPLAY_LINK"] != "false"
                    when {
                        useDisplayLink -> {
                            createDisplayLink()
                            timer = NSTimer.scheduledTimerWithTimeInterval(1.0 / 480.0, true, ::timerDisplayLink)
                        }
                        else -> {
                            timer = NSTimer.scheduledTimerWithTimeInterval(1.0 / 60.0, true, ::timer)
                        }
                    }

                } catch (e: Throwable) {
                    e.printStackTrace()
                    window.close()
                }
            }

            val arena = Arena()
            val displayLink = arena.alloc<CVDisplayLinkRefVar>()

            fun createDisplayLink() {
                //println("createDisplayLink[1]")
                val displayID = CGMainDisplayID()
                val error = CVDisplayLinkCreateWithCGDisplay(displayID, displayLink.ptr)
                //println("createDisplayLink[2]")
                if (error == kCVReturnSuccess) {
                    //println("createDisplayLink[3]")

                    CVDisplayLinkSetOutputCallback(displayLink.value, staticCFunction(::displayCallback), gameWindowStableRef.asCPointer())
                    CVDisplayLinkStart(displayLink.value)
                    //println("createDisplayLink[4]")
                }
                //println("createDisplayLink[5]")
            }

            var displayedFrame = -1
            fun timerDisplayLink(timer: NSTimer?) {
                val frameRequest = frameRequestNumber.value
                if (displayedFrame != frameRequest) {
                    displayedFrame = frameRequest
                    doRender()
                }
            }

            // public typealias CVDisplayLinkOutputCallback = CPointer<CFunction<(CVDisplayLinkRef?, CPointer<CVTimeStamp>?, CPointer<CVTimeStamp>?, CVOptionFlags, CPointer<CVOptionFlagsVar>?, COpaquePointer?) -> platform.CoreVideo.CVReturn>>

            private fun timer(timer: NSTimer?) {
                //println("TIMER")
                doRender()
            }

            override fun applicationWillTerminate(notification: NSNotification) {
                //println("applicationWillTerminate")
                // Insert code here to tear down your application
            }
        }

        coroutineDispatcher.executePending(1.seconds)
        app.run()
    }
}

interface DoRenderizable {
    fun doRenderRequest()
}

fun displayCallback(displayLink: CVDisplayLinkRef?, inNow: CPointer<CVTimeStamp>?, inOutputTime: CPointer<CVTimeStamp>?, flagsIn: CVOptionFlags, flagsOut: CPointer<CVOptionFlagsVar>?, displayLinkContext: COpaquePointer?): CVReturn {
    initRuntimeIfNeeded()
    frameRequestNumber.increment()
    /*
    //frameRequestNumber.increment()
    //val doRenderizable = displayLinkContext!!.asStableRef<DoRenderizable>().get()
    autoreleasepool {
        val doRenderizable = displayLinkContext!!.asStableRef<DoRenderizable>().get()
        doRenderizable.doRenderRequest()
        //println("displayCallback[0]")
        //doRenderizable.doRenderRequest()
        //println("displayCallback[1]")
    }
     */
    return kCVReturnSuccess
}

class WinController : NSObject() {
    @ObjCAction
    fun doTerminate() {
        NSApplication.sharedApplication.terminate(null)
    }
}


fun macTrace(str: String) {
    println(str)
}

val CValue<NSPoint>.x get() = this.useContents { x }
val CValue<NSPoint>.y get() = this.useContents { y }

val CValue<NSRect>.left get() = this.useContents { origin.x }
val CValue<NSRect>.top get() = this.useContents { origin.y }
val CValue<NSRect>.width get() = this.useContents { size.width }
val CValue<NSRect>.height get() = this.useContents { size.height }

val NSEvent.shift get() = (modifierFlags and NSShiftKeyMask) != 0uL
val NSEvent.ctrl get() = (modifierFlags and NSControlKeyMask) != 0uL
val NSEvent.alt get() = (modifierFlags and NSAlternateKeyMask) != 0uL
val NSEvent.meta get() = (modifierFlags and NSCommandKeyMask) != 0uL
