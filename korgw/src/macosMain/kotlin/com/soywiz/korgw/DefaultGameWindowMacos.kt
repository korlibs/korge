package com.soywiz.korgw

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.gl.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korim.format.ns.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.MSize
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.AppKit.*
import platform.CoreGraphics.*
import platform.CoreVideo.*
import platform.Foundation.*
import platform.JavaRuntimeSupport.*
import platform.darwin.*
import kotlin.native.SharedImmutable
import kotlin.native.concurrent.*

private fun ByteArray.toNsData(): NSData {
    val array = this
    return memScoped {
        array.usePinned { arrayPin ->
            NSData.dataWithBytes(arrayPin.startAddressOf, array.size.convert())
        }
    }
}

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

    //var customCursor: NSCursor? = null

    // @TODO: Broken in Kotlin 1.8.0: https://youtrack.jetbrains.com/issue/KT-55653/Since-Kotlin-1.8.0-NSView.resetCursorRects-doesnt-exist-anymore-and-cannot-override-it
    //override fun resetCursorRects() {
    //    val cursor = defaultGameWindow.cursor
    //    val nsCursor = cursor.nsCursor
    //    addCursorRect(bounds, nsCursor)
    //    println("MyNSOpenGLView.resetCursorRects: bounds=${bounds.toRectangle()}, cursor=$cursor, nsCursor=$nsCursor")
    //}

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

    private val gestureEvent = GestureEvent()
    override fun magnifyWithEvent(event: NSEvent) {
        defaultGameWindow.dispatch(gestureEvent.also {
            it.type = GestureEvent.Type.MAGNIFY
            it.id = 0
            it.amount = event.magnification()
        })
        //println("magnifyWithEvent:event=$event")
        super.magnifyWithEvent(event)
    }

    // https://developer.apple.com/documentation/appkit/nsevent
    override fun rotateWithEvent(event: NSEvent) {
        defaultGameWindow.dispatch(gestureEvent.also {
            it.type = GestureEvent.Type.ROTATE
            it.id = 0
            it.amount = event.rotation.toDouble()
        })

        super.rotateWithEvent(event)
    }

    override fun swipeWithEvent(event: NSEvent) {
        defaultGameWindow.dispatch(gestureEvent.also {
            it.type = GestureEvent.Type.SWIPE
            it.id = 0
            it.amountX = event.deltaX.toDouble()
            it.amountY = event.deltaY.toDouble()
        })
        //println("swipeWithEvent:event=$event")
        super.swipeWithEvent(event)
    }

    override fun smartMagnifyWithEvent(event: NSEvent) {
        defaultGameWindow.dispatch(gestureEvent.also {
            it.type = GestureEvent.Type.SMART_MAGNIFY
            it.id = 0
            it.amount = 1.0
        })
        //println("smartMagnifyWithEvent:event=$event")
        super.smartMagnifyWithEvent(event)
    }

    override fun scrollWheel(event: NSEvent): Unit {
        //println("scrollWheel:event=$event")
        mouseEvent(MouseEvent.Type.SCROLL, event)
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
            scrollDeltaX = -e.deltaX, scrollDeltaY = -e.deltaY, scrollDeltaZ = -e.deltaZ,
            isShiftDown = e.shift, isCtrlDown = e.ctrl, isAltDown = e.alt, isMetaDown = e.meta,
            scrollDeltaMode = MouseEvent.ScrollDeltaMode.PIXEL
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

    var inputRect: MRectangle = MRectangle(0.0, 0.0, 0.0, 0.0)

    fun setInputRectangle(windowRect: MRectangle) {
        this.inputRect = windowRect
    }

    // @TODO: Used for example when partially typing japanese. We need to display partial text while typing somehow
    override fun setMarkedText(string: Any?, selectedRange: CValue<NSRange>) = Unit//.also { println("setMarkedText: '$string', $selectedRange") }
    // @TODO: We should set the rectangle of the text input so IME places completion box at the right place
    override fun firstRectForCharacterRange(range: CValue<NSRange>): CValue<NSRect> = NSMakeRect(
        0.0.cg, 0.0.cg, 0.0.cg, 0.0.cg
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

class MyDefaultGameWindow : GameWindow() {
    val gameWindow = this
    val gameWindowStableRef = StableRef.create(gameWindow)
    val app = NSApplication.sharedApplication()
    val controller = WinController()
    override val dialogInterface: DialogInterfaceMacos = DialogInterfaceMacos { this }

    val windowStyle = NSWindowStyleMaskTitled or NSWindowStyleMaskMiniaturizable or
        NSWindowStyleMaskClosable or NSWindowStyleMaskResizable

    @Suppress("OPT_IN_USAGE")
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
        ).asUIntArray()
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

    override fun setInputRectangle(windowRect: MRectangle) {
        openglView.setInputRectangle(windowRect)
    }

    private var responder: NSResponder

    internal val window: NSWindow = MyNSWindow(windowRect, windowStyle, NSBackingStoreBuffered, false).apply {
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

    // https://developer.apple.com/documentation/appkit/nscursor
    override var cursor: ICursor = Cursor.DEFAULT
        set(value) {
            if (field == value) return
            field = value

            if (true) {
                field.nsCursor.set()
            } else {
                // @TODO: Broken in Kotlin 1.8.0: https://youtrack.jetbrains.com/issue/KT-55653/Since-Kotlin-1.8.0-NSView.resetCursorRects-doesnt-exist-anymore-and-cannot-override-it
                //window.contentView?.let { window.invalidateCursorRectsForView(it) }
            }
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
        doRender(update = false)
    }

    @Suppress("RemoveRedundantCallsOfConversionMethods")
    internal val backingScaleFactor: Double get() = window.backingScaleFactor.toDouble()
    internal var lastBackingScaleFactor = 0.0

    val darwinGamePad = DarwinGameControllerNative()

    fun doRender(update: Boolean) {
        //println("doRender[0]")
        val frameStartTime = PerformanceCounter.reference
        //macTrace("render")

        //println("doRender[1]")

        if (lastBackingScaleFactor != backingScaleFactor) {
            lastBackingScaleFactor = backingScaleFactor
            doWindowDidResize()
            return
        }

        //println("doRender[2]")

        //context?.flushBuffer()

        var doRender = !update
        if (update) {
            darwinGamePad.updateGamepads(gameWindow)
            frame(doUpdate = true, doRender = false, frameStartTime = frameStartTime)
            if (mustTriggerRender) {
                doRender = true
            }
        }

        if (doRender) {
            val context = openglView.openGLContext
            context?.makeCurrentContext()

            //println("doRender[3] : $context")
            //ag.clear(Colors.BLACK)
            //ag.onRender(ag)
            //dispatch(renderEvent)
            frame(frameStartTime = frameStartTime)
            context?.flushBuffer()
        }

        //println("doRender[3]")
        //println("doRender[4]")
    }

    override val ag: AG = AGNative()

    override val devicePixelRatio: Double
        get() {
            //return NSScreen.mainScreen?.backingScaleFactor?.toDouble() ?: field
            return window.backingScaleFactor
        }

    override val pixelsPerInch: Double
        get() {
            val screen = window.screen ?: return 96.0
            val screenSizeInPixels = screen.visibleFrame.useContents { MSize(size.width, size.height) }
            val screenSizeInMillimeters = CGDisplayScreenSize(((screen.deviceDescription["NSScreenNumber"]) as NSNumber).unsignedIntValue).useContents { MSize(width, height) }

            val dpmm = screenSizeInPixels.width / screenSizeInMillimeters.width
            val dpi = dpmm / 0.0393701

            //println("screenSizeInPixels=$screenSizeInPixels")
            //println("screenSizeInMillimeters=$screenSizeInMillimeters")
            //println("dpmm=$dpmm")
            //println("dpi=$dpi")

            return dpi // 1 millimeter -> 0.0393701 inches
        }

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
        window.setContentSize(NSMakeSize(width.cg, height.cg))
        window.center()
        //window.setFrameTopLeftPoint()
    }

    override fun close(exitCode: Int) {
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

                    doRender(update = true)
                    val useDisplayLink = Environment["MACOS_USE_DISPLAY_LINK"] != "false"
                    if (useDisplayLink && createDisplayLink()) {
                        //timer = NSTimer.scheduledTimerWithTimeInterval(1.0 / 480.0, true, ::timerDisplayLink)
                    } else {
                        timer = NSTimer.scheduledTimerWithTimeInterval(1.0 / 60.0, true, ::timer)
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                    window.close()
                }
            }

            val arena = Arena()
            val displayLink = arena.alloc<CVDisplayLinkRefVar>()

            private fun checkDisplayLink(code: Int) {
                if (code != kCVReturnSuccess) internalException(code)
            }

            fun createDisplayLink(): Boolean {
                //println("createDisplayLink[1]")
                return try {
                    checkDisplayLink(CVDisplayLinkCreateWithCGDisplay(CGMainDisplayID(), displayLink.ptr))
                    checkDisplayLink(CVDisplayLinkSetOutputCallback(displayLink.value, staticCFunction(::displayCallback), gameWindowStableRef.asCPointer()))
                    checkDisplayLink(CVDisplayLinkStart(displayLink.value))
                    true
                } catch (e: InternalException) {
                    if (displayLink.value != null) {
                        CVDisplayLinkRelease(displayLink.value)
                    }
                    e.printStackTrace()
                    false
                }
            }

            private fun timer(timer: NSTimer?) {
                //println("TIMER")
                doRender(update = true)
            }

            override fun applicationWillTerminate(notification: NSNotification) {
                //println("applicationWillTerminate")
                // Insert code here to tear down your application
            }
        }

        coroutineDispatcher.executePending(1.seconds)
        app.run()
    }

    override suspend fun clipboardWrite(data: ClipboardData) {
        val pasteboard = NSPasteboard.generalPasteboard

        pasteboard.declareTypes(listOf(NSPasteboardTypeString), null)
        when (data) {
            is TextClipboardData -> pasteboard.setString(data.text, NSPasteboardTypeString)
        }
    }

    override suspend fun clipboardRead(): ClipboardData? {
        val pasteboard = NSPasteboard.generalPasteboard
        val items = pasteboard.pasteboardItems as? List<NSPasteboardItem>?
        if (items.isNullOrEmpty()) return null
        return items.last().stringForType(NSPasteboardTypeString)?.let { TextClipboardData(it) }
    }

    override val hapticFeedbackGenerateSupport: Boolean get() = true
    override fun hapticFeedbackGenerate(kind: HapticFeedbackKind) {
        NSHapticFeedbackManager.defaultPerformer.performFeedbackPattern(
            when (kind) {
                HapticFeedbackKind.GENERIC -> NSHapticFeedbackPatternGeneric
                HapticFeedbackKind.ALIGNMENT -> NSHapticFeedbackPatternAlignment
                HapticFeedbackKind.LEVEL_CHANGE -> NSHapticFeedbackPatternLevelChange
            },
            NSHapticFeedbackPerformanceTimeNow
        )
    }
}

actual fun CreateDefaultGameWindow(config: GameWindowCreationConfig): GameWindow = MyDefaultGameWindow()

private val atomicDisplayLinkContext = AtomicReference<COpaquePointer?>(null)

@SharedImmutable
val doDisplayCallbackRender: () -> Unit = {
    try {
        initRuntimeIfNeeded()
        val gameWindow = atomicDisplayLinkContext.value?.asStableRef<MyDefaultGameWindow>()
        gameWindow?.get()?.doRender(update = true)
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}

@Suppress("UNUSED_PARAMETER")
fun displayCallback(
    displayLink: CVDisplayLinkRef?,
    inNow: CPointer<CVTimeStamp>?,
    inOutputTime: CPointer<CVTimeStamp>?,
    flagsIn: CVOptionFlags,
    flagsOut: CPointer<CVOptionFlagsVar>?,
    displayLinkContext: COpaquePointer?
): CVReturn {
    initRuntimeIfNeeded()
    atomicDisplayLinkContext.value = displayLinkContext
    // Wait for this in the case we take more time than the frame time to not collapse this
    NSOperationQueue.mainQueue.addOperations(
        listOf(NSBlockOperation().also { it.addExecutionBlock(doDisplayCallbackRender) }),
        //waitUntilFinished = true
        waitUntilFinished = false // ISSUE: https://github.com/korlibs/korge/issues/1078
    )
    //NSOperationQueue.mainQueue.addOperationWithBlock(doDisplayCallbackRender)
    return kCVReturnSuccess
}

class WinController : NSObject() {
    @ObjCAction
    fun doTerminate() {
        NSApplication.sharedApplication.terminate(null)
    }
}

@kotlin.native.concurrent.ThreadLocal
val doMacTrace by lazy { Environment["MAC_TRACE"] == "true" }

fun macTrace(str: String) {
    if (doMacTrace) println(str)
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

inline val Int.cg: CGFloat get() = this.toDouble()
inline val Double.cg: CGFloat get() = this.toDouble()


val GameWindow.Cursor.nsCursor: NSCursor get() = when (this) {
    GameWindow.Cursor.DEFAULT -> NSCursor.arrowCursor
    GameWindow.Cursor.CROSSHAIR -> NSCursor.crosshairCursor
    GameWindow.Cursor.TEXT -> NSCursor.IBeamCursor
    GameWindow.Cursor.HAND -> NSCursor.pointingHandCursor
    GameWindow.Cursor.MOVE -> NSCursor.closedHandCursor
    GameWindow.Cursor.WAIT -> NSCursor.dragCopyCursor
    GameWindow.Cursor.RESIZE_EAST -> NSCursor.resizeRightCursor
    GameWindow.Cursor.RESIZE_SOUTH -> NSCursor.resizeDownCursor
    GameWindow.Cursor.RESIZE_WEST -> NSCursor.resizeLeftCursor
    GameWindow.Cursor.RESIZE_NORTH -> NSCursor.resizeUpCursor
    GameWindow.Cursor.RESIZE_NORTH_EAST -> NSCursor.javaResizeNECursor() ?: NSCursor.arrowCursor
    GameWindow.Cursor.RESIZE_NORTH_WEST -> NSCursor.javaResizeNWCursor() ?: NSCursor.arrowCursor
    GameWindow.Cursor.RESIZE_SOUTH_EAST -> NSCursor.javaResizeSECursor() ?: NSCursor.arrowCursor
    GameWindow.Cursor.RESIZE_SOUTH_WEST -> NSCursor.javaResizeSWCursor() ?: NSCursor.arrowCursor
    else -> NSCursor.arrowCursor
}

val GameWindow.CustomCursor.nsCursor: NSCursor by extraPropertyThis {
    val result = createBitmap()
    val image = result.bitmap.toBMP32IfRequired().toNSImage()
    NSCursor(image, result.hotspot.toNSPoint())
}

val GameWindow.ICursor.nsCursor: NSCursor get() = when (this) {
    is GameWindow.Cursor -> this.nsCursor
    is GameWindow.CustomCursor -> this.nsCursor
    else -> NSCursor.arrowCursor
}
