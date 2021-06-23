package com.soywiz.korgw

import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.cg.toCgFloat
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import com.soywiz.korma.geom.*
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

class MyNSWindow(contentRect: kotlinx.cinterop.CValue<platform.Foundation.NSRect /* = platform.CoreGraphics.CGRect */>, styleMask: platform.AppKit.NSWindowStyleMask /* = kotlin.ULong */, backing: platform.AppKit.NSBackingStoreType /* = kotlin.ULong */, defer: kotlin.Boolean) : NSWindow(
    contentRect, styleMask, backing, defer
) {
}

class MyNSOpenGLView(
    val defaultGameWindow: MyDefaultGameWindow,
    frame: kotlinx.cinterop.CValue<platform.Foundation.NSRect /* = platform.CoreGraphics.CGRect */>,
    pixelFormat: platform.AppKit.NSOpenGLPixelFormat?
) : NSOpenGLView(frame, pixelFormat), NSTextInputProtocol {
    override fun acceptsFirstResponder(): Boolean = true
    override fun becomeFirstResponder(): Boolean = true

    //fun getHeight() = openglView.bounds.height
    fun getHeight() = bounds.height

    var lastModifierFlags: Int = 0

    fun dispatchFlagIfRequired(event: NSEvent, mask: Int, key: Key) {
        val old = (lastModifierFlags and mask) != 0
        val new = (event.modifierFlags.toInt() and mask) != 0
        if (old == new) return

        defaultGameWindow.dispatchKeyEventEx(
            type = if (new) KeyEvent.Type.DOWN else KeyEvent.Type.UP,
            id = 0,
            character = ' ',
            key = key,
            keyCode = key.ordinal,
            shift = event.shift,
            ctrl = event.ctrl,
            alt = event.alt,
            meta = event.meta
        )
    }

    override fun flagsChanged(event: NSEvent) {
        dispatchFlagIfRequired(event, NSShiftKeyMask.toInt(), Key.LEFT_SHIFT)
        dispatchFlagIfRequired(event, NSControlKeyMask.toInt(), Key.LEFT_CONTROL)
        dispatchFlagIfRequired(event, NSAlternateKeyMask.toInt(), Key.LEFT_ALT)
        dispatchFlagIfRequired(event, NSCommandKeyMask.toInt(), Key.META)
        dispatchFlagIfRequired(event, NSFunctionKeyMask.toInt(), Key.FUNCTION)
        dispatchFlagIfRequired(event, NSEventModifierFlagCapsLock.toInt(), Key.CAPS_LOCK)

        lastModifierFlags = event.modifierFlags.toInt()
    }

    override fun mouseUp(event: NSEvent) = mouseEvent(MouseEvent.Type.UP, event)
    override fun rightMouseUp(event: NSEvent) = mouseEvent(MouseEvent.Type.UP, event)
    override fun otherMouseUp(event: NSEvent) = mouseEvent(MouseEvent.Type.UP, event)

    override fun mouseDown(event: NSEvent) = mouseEvent(MouseEvent.Type.DOWN, event)
    override fun rightMouseDown(event: NSEvent) = mouseEvent(MouseEvent.Type.DOWN, event)
    override fun otherMouseDown(event: NSEvent) = mouseEvent(MouseEvent.Type.DOWN, event)

    override fun mouseDragged(event: NSEvent) = mouseEvent(MouseEvent.Type.DRAG, event)
    override fun rightMouseDragged(event: NSEvent) = mouseEvent(MouseEvent.Type.DRAG, event)
    override fun otherMouseDragged(event: NSEvent) = mouseEvent(MouseEvent.Type.DRAG, event)

    override fun scrollWheel(event: NSEvent) = mouseEvent(MouseEvent.Type.SCROLL, event)

    override fun mouseMoved(event: NSEvent) = mouseEvent(MouseEvent.Type.MOVE, event)

    private fun mouseEvent(etype: MouseEvent.Type, e: NSEvent) {
        val ex = e.locationInWindow.x.toInt()
        val ey = (getHeight() - e.locationInWindow.y).toInt()
        //println("mouseUp($rx,$ry)")
        val ebutton = e.buttonNumber.toInt()

        val factor = defaultGameWindow.backingScaleFactor
        val sx = ex * factor
        val sy = ey * factor

        defaultGameWindow.dispatchMouseEvent(
            id = 0,
            type = etype,
            x = sx.toInt(),
            y = sy.toInt(),
            button = when (etype) {
                MouseEvent.Type.SCROLL -> MouseButton.BUTTON_WHEEL
                else -> button(ebutton)
            },
            buttons = when (etype) {
                MouseEvent.Type.SCROLL -> 0
                else -> buttonMask(e.buttonMask.toInt())
            },
            scrollDeltaX = e.deltaX.toDouble(), scrollDeltaY = e.deltaY.toDouble(), scrollDeltaZ = e.deltaZ.toDouble(),
            isShiftDown = e.shift, isCtrlDown = e.ctrl, isAltDown = e.alt, isMetaDown = e.meta,
        )
    }

    fun buttonMask(mask: Int): Int {
        var out = 0
        for (n in 0 until 8) {
            if (mask.extractBool(n)) out = out or button(n).bits
        }
        return out
    }

    fun button(index: Int): MouseButton {
        return when (index) {
            0 -> MouseButton.LEFT
            1 -> MouseButton.RIGHT
            2 -> MouseButton.MIDDLE
            else -> MouseButton[index]
        }
    }

    var lastFn = false
    var lastShift = false
    var lastCtrl = false
    var lastAlt = false
    var lastMeta = false

    fun keyDownUp(event: NSEvent, pressed: Boolean, e: NSEvent) {
        val str = event.charactersIgnoringModifiers ?: "\u0000"
        val c = str.getOrNull(0) ?: '\u0000'
        val cc = c.toInt().toChar()
        //println("keyDownUp")
        val char = cc
        val keyCode = event.keyCode.toInt()

        val rawKey = KeyCodesToKeys[keyCode] ?: CharToKeys[char] ?: Key.UNKNOWN
        val key = when {
            (rawKey == Key.BACKSPACE || rawKey == Key.DELETE) -> if (event.fn) Key.DELETE else Key.BACKSPACE
            else -> rawKey
        }

        lastModifierFlags = event.modifierFlags.toInt()

        //println("keyDownUp: char=$char, keyCode=${keyCode.toInt()}, key=$key, pressed=$pressed, shift=${e.shift}, ctrl=${e.ctrl}, alt=${e.alt}, meta=${e.meta}, characters=${event.characters}, event.willBeHandledByComplexInputMethod()=${event.willBeHandledByComplexInputMethod()}")

        defaultGameWindow.dispatchKeyEventEx(
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
        lastFn = event.fn
        lastShift = event.shift
        lastCtrl = event.ctrl
        lastAlt = event.alt
        lastMeta = event.meta

        interpretKeyEvents(listOf(event))
        //val inputMethod = event.willBeHandledByComplexInputMethod()
        keyDownUp(event, true, event)
    }

    override fun keyUp(event: NSEvent) {
        //super.keyUp(event)
        keyDownUp(event, false, event)
    }

    override fun insertText(string: Any?) {
        //println("MyNSOpenGLView.insertText: '$string'")
        if (string == null) return
        for (char in string.toString()) {
            defaultGameWindow.dispatchKeyEventEx(
                type = KeyEvent.Type.TYPE,
                id = 0,
                character = char,
                key = Key.UNKNOWN,
                keyCode = char.code,
                shift = lastShift,
                ctrl = lastCtrl,
                alt = lastAlt,
                meta = lastMeta
            )
        }
    }

    var inputRect: Rectangle = Rectangle(0.0, 0.0, 0.0, 0.0)

    fun setInputRectangle(windowRect: Rectangle) {
        this.inputRect = windowRect
    }

    // @TODO: Used for example when partially typing japanese. We need to display partial text while typing somehow
    override fun setMarkedText(string: Any?, selectedRange: CValue<NSRange>) = Unit//.also { println("setMarkedText: '$string', $selectedRange") }
    // @TODO: We should set the rectangle of the text input so IME places completion box at the right place
    override fun firstRectForCharacterRange(range: CValue<NSRange>): CValue<NSRect> = NSMakeRect(
        0.0.toCgFloat(), 0.0.toCgFloat(), 0.0.toCgFloat(), 0.0.toCgFloat()
        //(this.bounds.left + inputRect.x).toCgFloat(),
        //(this.bounds.top + inputRect.y).toCgFloat(),
        //(inputRect.width).toCgFloat(),
        //(inputRect.height).toCgFloat()
    )//.also { println("firstRectForCharacterRange: $range") }
    override fun attributedSubstringFromRange(range: CValue<NSRange>): NSAttributedString? = null//.also { println("attributedSubstringFromRange: $range") }
    override fun characterIndexForPoint(point: CValue<NSPoint>): NSUInteger = 0u//.also { println("characterIndexForPoint: $point") }
    override fun conversationIdentifier(): NSInteger = 0//.also { println("conversationIdentifier") }
    override fun doCommandBySelector(selector: COpaquePointer?) = Unit//.also { println("doCommandBySelector: $selector") }
    override fun hasMarkedText(): Boolean = false//.also { println("hasMarkedText") }
    override fun markedRange(): CValue<NSRange> = NSMakeRange(0u, 0u)//.also { println("markedRange") }
    override fun selectedRange(): CValue<NSRange> = NSMakeRange(0u, 0u)//.also { println("selectedRange") }
    override fun unmarkText() = Unit//.also { println("unmarkText") }
    override fun validAttributesForMarkedText(): List<*>? = null//.also { println("validAttributesForMarkedText") }
}

class MyDefaultGameWindow : GameWindow(), DoRenderizable {
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
            NSOpenGLPFADoubleBuffer.convert(),
            NSOpenGLPFAColorSize.convert(), 24.convert(),
            NSOpenGLPFAAlphaSize.convert(), 8.convert(),
            NSOpenGLPFADepthSize.convert(), 24.convert(),
            NSOpenGLPFAStencilSize.convert(), 8.convert(),
            NSOpenGLPFAAccumSize.convert(), 0.convert(),
            0.convert()
        ).toUIntArray()
    }

    val pixelFormat by lazy {
        //println("NSOpenGLPFAStencilSize: $NSOpenGLPFAStencilSize")
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

    private val openglView: MyNSOpenGLView = MyNSOpenGLView(this@MyDefaultGameWindow, NSMakeRect(0.0, 0.0, 16.0, 16.0), pixelFormat)
    var timer: NSTimer? = null

    override fun setInputRectangle(windowRect: Rectangle) {
        openglView.setInputRectangle(windowRect)
    }

    private var responder: NSResponder

    private val window: NSWindow = MyNSWindow(windowRect, windowStyle, NSBackingStoreBuffered, false).apply {
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
            override fun becomeFirstResponder(): Boolean = true




            //external override fun performKeyEquivalent(event: NSEvent): Boolean {
            //    return true
            //}
        }
        makeFirstResponder(openglView)
        //openglView.setNextResponder(responder)
        //setNextResponder(responder)
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
    internal val backingScaleFactor: Double get() = window.backingScaleFactor.toDouble()
    internal var lastBackingScaleFactor = 0.0

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

    override suspend fun openFileDialog(filter: FileFilter?, write: Boolean, multi: Boolean, currentDir: VfsFile?): List<VfsFile> {
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

actual fun CreateDefaultGameWindow(): GameWindow = MyDefaultGameWindow()

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

val NSEvent.fn get() = (modifierFlags and NSFunctionKeyMask) != 0uL
val NSEvent.shift get() = (modifierFlags and NSShiftKeyMask) != 0uL
val NSEvent.ctrl get() = (modifierFlags and NSControlKeyMask) != 0uL
val NSEvent.alt get() = (modifierFlags and NSAlternateKeyMask) != 0uL
val NSEvent.meta get() = (modifierFlags and NSCommandKeyMask) != 0uL
