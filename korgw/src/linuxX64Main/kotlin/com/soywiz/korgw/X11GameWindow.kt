package com.soywiz.korgw

import X11Embed.*
import com.soywiz.kgl.*
import com.soywiz.kmem.DynamicLibrary
import com.soywiz.kmem.startAddressOf
import com.soywiz.kmem.write32LE
import com.soywiz.korag.AGOpengl
import com.soywiz.korev.Key
import com.soywiz.korev.MouseButton
import com.soywiz.korev.MouseEvent
import com.soywiz.korim.bitmap.Bitmap
import kotlinx.cinterop.*

typealias XVisualInfo = ULongVar // Only used as pointer
typealias GLXDrawable = COpaquePointer
typealias GLXContext = COpaquePointer
typealias KeySym = Int
typealias CString = CPointer<ByteVar>
typealias CDisplayPointer = CPointer<Display>?

@SharedImmutable val x11lib = DynamicLibrary("libX11.so")
@SharedImmutable val XDefaultScreen by x11lib.func<(d: CDisplayPointer) -> Int>()
@SharedImmutable val XRootWindow by x11lib.func<(d: CDisplayPointer, scr: Int) -> Window>()
@SharedImmutable val XBlackPixel by x11lib.func<(d: CDisplayPointer, scr: Int) -> Int>()
@SharedImmutable val XWhitePixel by x11lib.func<(d: CDisplayPointer, scr: Int) -> Int>()
@SharedImmutable val XStoreName by x11lib.func<(d: CDisplayPointer, w: Window, title: CString) -> Unit>()
@SharedImmutable val XSetIconName by x11lib.func<(d: CDisplayPointer, w: Window, title: CString) -> Unit>()
@SharedImmutable val XDestroyWindow by x11lib.func<(d: CDisplayPointer, w: Window) -> Unit>()
@SharedImmutable val XCloseDisplay by x11lib.func<(d: CDisplayPointer) -> Unit>()
@SharedImmutable val XFlush by x11lib.func<(d: CDisplayPointer) -> Unit>()
@SharedImmutable val XInternAtom by x11lib.func<(d: CDisplayPointer, name: CString, p3: Int) -> Atom>()
@SharedImmutable val XOpenDisplay by x11lib.func<(name: CString?) -> CDisplayPointer>()
@SharedImmutable val XDefaultRootWindow by x11lib.func<(d: CDisplayPointer) -> Window>()
@SharedImmutable val XDisplayWidth by x11lib.func<(d: CDisplayPointer, scr: Int) -> Int>()
@SharedImmutable val XDisplayHeight by x11lib.func<(d: CDisplayPointer, scr: Int) -> Int>()
@SharedImmutable val XSelectInput by x11lib.func<(d: CDisplayPointer, w: Window, mask: Int) -> Unit>()
@SharedImmutable val XMapWindow by x11lib.func<(d: CDisplayPointer, w: Window) -> Unit>()
@SharedImmutable val XSetWMProtocols by x11lib.func<(d: CDisplayPointer, w: Window, array: CPointer<AtomVar>, count: Int) -> Unit>()
@SharedImmutable val XPending by x11lib.func<(d: CDisplayPointer) -> Int>()
@SharedImmutable val XCreateSimpleWindow by x11lib.func<(d: CDisplayPointer, parent: Window, x: Int, y: Int, width: UInt, height: UInt, border_width: UInt, border: Int, background: Int) -> Window>()
@SharedImmutable val XNextEvent by x11lib.func<(d: CDisplayPointer, event: CPointer<XEvent>) -> Unit>()
@SharedImmutable val XSendEvent by x11lib.func<(d: CDisplayPointer, w: Window, propagate: Int, event_mask: Int, event_send: CPointer<XEvent>) -> Status>()
@SharedImmutable val XLookupKeysym by x11lib.func<(event: CPointer<XKeyEvent>, index: Int) -> KeySym>()
@SharedImmutable val XChangeProperty by x11lib.func<(display: CDisplayPointer, w: Window, property: Atom, type: Atom, format: Int, mode: Int, data: CPointer<ByteVar>, nelements: Int) -> Unit>()
@SharedImmutable val XDeleteProperty by x11lib.func<(display: CDisplayPointer, w: Window, property: Atom) -> Unit>()
@SharedImmutable val glXChooseVisual by gllib.func<(d: CDisplayPointer, scr: Int, ptr: CPointer<IntVar>?) -> CPointer<XVisualInfo>?>()
@SharedImmutable val glXGetCurrentDisplay by gllib.func<() -> CDisplayPointer>()
@SharedImmutable val glXGetCurrentDrawable by gllib.func<() -> GLXDrawable>()
@SharedImmutable val glXSwapBuffers by gllib.func<(d: CDisplayPointer, w: Window) -> Unit>()
@SharedImmutable val glXMakeCurrent by gllib.func<(d: CDisplayPointer, w: Window, glc: GLXContext) -> Unit>()
@SharedImmutable val glXCreateContext by gllib.func<(d: CDisplayPointer, vi: CPointer<XVisualInfo>?, shareList: GLXContext?, direct: Int) -> GLXContext>()

//actual fun CreateDefaultGameWindow(): GameWindow = glutGameWindow
actual fun CreateDefaultGameWindow(): GameWindow {
    val engine = com.soywiz.korio.lang.Environment["KORGW_NATIVE_ENGINE"]
        ?: "default"
    println("Engine: $engine")
    return when (engine) {
        "sdl" -> SdlGameWindowNative()
        else -> X11GameWindow()
    }
}

// https://www.khronos.org/registry/OpenGL/extensions/EXT/EXT_swap_control.txt
private val swapIntervalEXT by GLFuncNull<(CPointer<Display>?, GLXDrawable, Int) -> Unit>("swapIntervalEXT")

//class X11Ag(val window: X11GameWindow, override val gl: KmlGl = LogKmlGlProxy(X11KmlGl())) : AGOpengl() {
class X11Ag(val window: X11GameWindow, override val gl: KmlGl = com.soywiz.kgl.KmlGlNative()) : AGOpengl() {
    override val gles: Boolean = true
    override val linux: Boolean = true
    override val nativeComponent: Any = window
}

// https://www.khronos.org/opengl/wiki/Tutorial:_OpenGL_3.0_Context_Creation_(GLX)
class X11OpenglContext(val d: CPointer<Display>?, val w: Window, val doubleBuffered: Boolean = true) {
    companion object {
        fun chooseVisuals(d: CPointer<Display>?, scr: Int = XDefaultScreen(d)): CPointer<XVisualInfo>? {
            val GLX_END = 0
            val attrsList = listOf(
                intArrayOf(GLX_RGBA, GLX_DOUBLEBUFFER, GLX_DEPTH_SIZE, 24, GLX_STENCIL_SIZE, 8, GLX_END),
                intArrayOf(GLX_RGBA, GLX_DEPTH_SIZE, 24, GLX_STENCIL_SIZE, 8, GLX_END),
                intArrayOf(GLX_RGBA, GLX_DOUBLEBUFFER, GLX_DEPTH_SIZE, 16, GLX_STENCIL_SIZE, 8, GLX_END),
                intArrayOf(GLX_RGBA, GLX_DEPTH_SIZE, 16, GLX_STENCIL_SIZE, 8, GLX_END),
                intArrayOf(GLX_RGBA, GLX_DOUBLEBUFFER, GLX_END),
                intArrayOf(GLX_RGBA, GLX_END),
                intArrayOf(GLX_END)
            )
            for (attrs in attrsList) {
                attrs.usePinned {
                    val res = glXChooseVisual(d, scr, it.addressOf(0))
                    if (res != null) return res
                }
            }
            println("VI: null")
            return null
        }
    }
    init {
        println("Creating OpenGL context")
    }
    val vi = chooseVisuals(d, 0)
    init {
        println("VI: $vi")
    }
    val glc = glXCreateContext(d, vi, null, 1)

    init {
        println("VI: $vi, d: $d, w: $w, glc: $glc")
        makeCurrent()
        println("GL_VENDOR: " + NativeBaseKmlGl.glGetStringExt(NativeBaseKmlGl.GL_VENDOR)?.toKString())
        println("GL_VERSION: " + NativeBaseKmlGl.glGetStringExt(NativeBaseKmlGl.GL_VERSION)?.toKString())
    }

    fun makeCurrent() {
        glXMakeCurrent(d, w, glc)
    }

    fun swapBuffers() {
        glXSwapBuffers(d, w)
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
class X11GameWindow : EventLoopGameWindow(), DialogInterface by NativeZenityDialogs() {
    //init { println("X11GameWindow") }
    override val ag: X11Ag by lazy { X11Ag(this) }
    override var width: Int = 200; private set
    override var height: Int = 200; private set
    override var title: String = "Korgw"
        set(value) {
            field = value
            realSetTitle(value)
        }
    override var icon: Bitmap? = null
        set(value) {
            field = value
            realSetIcon(value)
        }
    override var fullscreen: Boolean = false
        set(value) {
            field = value
            realSetFullscreen(value)
        }
    override var visible: Boolean = true
        set(value) {
            field = value
            realSetVisible(value)
        }
    override var quality: Quality = Quality.AUTOMATIC

    override fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    var d: CPointer<Display>? = null
    val NilWin: Window = 0UL
    var root: Window = 0UL
    var w: Window = 0UL
    var s: Int = 0

    fun realSetTitle(title: String): Unit {
        if (d == null || w == NilWin) return
        try {
            memScoped {
                //X.XSetWMIconName(d, w, )
                XStoreName(d, w, cstr(title))
                XSetIconName(d, w, cstr(title))
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }
    }

    fun realSetIcon(value: Bitmap?): Unit {
        if (d == null || w == NilWin || value == null) return
        try {
            memScoped {
                val property = XInternAtom(d, cstr("_NET_WM_ICON"), 0)
                val bmp = value.toBMP32()
                val VSIZE = Platform.cpuArchitecture.bitness / 8
                val bytes = ByteArray((bmp.area + 2) * VSIZE)
                bytes.write32LE(0, bmp.width)
                bytes.write32LE(VSIZE, bmp.height)
                for (n in 0 until bmp.area) {
                    val pos = VSIZE * (2 + n)
                    val c = bmp.data[n]
                    bytes[pos + 0] = c.r.toByte()
                    bytes[pos + 1] = c.g.toByte()
                    bytes[pos + 2] = c.b.toByte()
                    bytes[pos + 3] = c.a.toByte()
                }
                bytes.usePinned { pin ->
                    val XA_CARDINAL: Atom = 6.convert()
                    XChangeProperty(
                        d, w, property, XA_CARDINAL, 32, PropModeReplace,
                        pin.startAddressOf.reinterpret(), bytes.size / VSIZE
                    )
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }
    }

    private fun ArenaBase.cstr(str: String) = str.cstr.placeTo(this)

    // https://stackoverflow.com/questions/9065669/x11-glx-fullscreen-mode
    fun realSetFullscreen(value: Boolean): Unit {
        println("realSetFullscreen. value=$value")
        if (d == null || w == NilWin) return
        try {
            val fullscreen = value
            memScoped {
                val _NET_WM_STATE = XInternAtom(d, cstr("_NET_WM_STATE"), 1)
                val _NET_WM_STATE_ADD = XInternAtom(d, cstr("_NET_WM_STATE_ADD"), 1)
                val _NET_WM_STATE_REMOVE = XInternAtom(d, cstr("_NET_WM_STATE_REMOVE"), 1)
                val _NET_WM_STATE_FULLSCREEN = XInternAtom(d, cstr("_NET_WM_STATE_FULLSCREEN"), 1)

                //println("realSetFullscreen: wm_state=$_NET_WM_STATE, wm_fullscreen=$_NET_WM_STATE_FULLSCREEN")

                //val attributes = alloc<XSetWindowAttributes>()
                //val CWOverrideRedirect = (1L shl 9)
                //attributes.override_redirect = if (value) 1 else 0
                //XChangeWindowAttributes(d, w, CWOverrideRedirect.convert(), attributes.ptr)

                val isWindowMapped = true
                if (isWindowMapped) {
                    val e = alloc<XEvent>()
                    e.xany.type = ClientMessage
                    e.xclient.message_type = _NET_WM_STATE
                    e.xclient.format = 32
                    e.xclient.window = w
                    e.xclient.data.l[0] = (if (fullscreen) _NET_WM_STATE_ADD else _NET_WM_STATE_REMOVE).convert()
                    e.xclient.data.l[1] = _NET_WM_STATE_FULLSCREEN.convert()
                    e.xclient.data.l[3] = 0.convert()

                    XSendEvent(
                        d,
                        XDefaultRootWindow(d),
                        0.convert(),
                        SubstructureNotifyMask or SubstructureRedirectMask,
                        e.ptr
                    )

                } else {
                    val atoms = allocArray<AtomVar>(3)
                    var count = 0
                    if (fullscreen) {
                        atoms[count++] = _NET_WM_STATE_FULLSCREEN
                    }
                    val XA_ATOM: Atom = 4.convert()
                    if (count > 0) {
                        XChangeProperty(d, w, _NET_WM_STATE, XA_ATOM, 32, PropModeReplace, atoms.getPointer(this).reinterpret(), count)
                    } else {
                        XDeleteProperty(d, w, _NET_WM_STATE);
                    }
                }
                XFlush(d)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }
    }

    fun realSetVisible(value: Boolean): Unit {
        if (d == null || w == NilWin) return
    }

    override fun doInitialize() {
        println("doInitialize")

        try {
            memScoped {
                d = XOpenDisplay(null) ?: error("Can't open main display")
                s = XDefaultScreen(d)
                root = XDefaultRootWindow(d)

                //val cmap = XCreateColormap(d, root, vi->visual, AllocNone);
                val screenWidth = XDisplayWidth(d, s)
                val screenHeight = XDisplayHeight(d, s)

                val gameWindow = this@X11GameWindow

                val winX = screenWidth / 2 - width / 2
                val winY = screenHeight / 2 - height / 2

                println("screenWidth: $screenWidth, screenHeight: $screenHeight, winX=$winX, winY=$winY, width=$width, height=$height")

                w = XCreateSimpleWindow(
                    d, XRootWindow(d, s),
                    winX, winY,
                    width.convert(), height.convert(),
                    1.convert(),
                    XBlackPixel(d, s), XWhitePixel(d, s)
                )

                println("XCreateSimpleWindow=$w, d=$d, s=$s")

                val eventMask = (ExposureMask
                    or StructureNotifyMask
                    or EnterWindowMask
                    or LeaveWindowMask
                    or KeyPressMask
                    or KeyReleaseMask
                    or PointerMotionMask
                    or ButtonPressMask
                    or ButtonReleaseMask
                    or ButtonMotionMask
                    )

                XSelectInput(d, w, eventMask)
                XMapWindow(d, w)
                realSetIcon(icon)
                realSetVisible(fullscreen)
                realSetVisible(visible)
                realSetTitle(title)

                ctx = X11OpenglContext(d, w, doubleBuffered = doubleBuffered)
                ctx.makeCurrent()

                val wmDeleteMessage = XInternAtom(d, cstr("WM_DELETE_WINDOW"), 0)
                memScoped {
                    val protocolsArray = allocArray<AtomVar>(1)
                    protocolsArray[0] = wmDeleteMessage
                    XSetWMProtocols(d, w, protocolsArray, 1)
                }

                //println("/doInitialize")
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }
    }

    lateinit var ctx: X11OpenglContext

    val doubleBuffered = false
    //val doubleBuffered = true

    override fun close(exitCode: Int) {
        //println("close: $exitCode")
        super<EventLoopGameWindow>.close(exitCode)
    }

    override fun doHandleEvents() = memScoped {
        //println("doHandleEvents")
        val e = alloc<XEvent>()
        loop@ while (running) {
            //println("---")
            if (XPending(d) == 0) return
            XNextEvent(d, e.ptr)
            //println("EVENT: ${e.type}")
            when (e.type) {
                Expose -> if (e.xexpose.count == 0) render(doUpdate = false)
                ClientMessage, DestroyNotify -> close()
                ConfigureNotify -> {
                    val conf = e.xconfigure
                    width = conf.width
                    height = conf.height
                    //dispatchReshapeEvent(conf.x, conf.y, conf.width, conf.height)
                    dispatchReshapeEvent(0, 0, conf.width, conf.height)
                    if (!doubleBuffered) {
                        render(doUpdate = false)
                    }
                    //println("RESIZED! ${conf.width} ${conf.height}")
                }
                KeyPress, KeyRelease -> {
                    val pressing = e.type == KeyPress
                    val ev =
                        if (pressing) com.soywiz.korev.KeyEvent.Type.DOWN else com.soywiz.korev.KeyEvent.Type.UP
                    val keyCode = e.xkey.keycode.toInt()
                    val kkey = XK_KeyMap[XLookupKeysym(e.xkey.ptr, 0).toInt()] ?: Key.UNKNOWN
                    //println("KEY: $ev, ${keyCode.toChar()}, $kkey, $keyCode, keySym=$kkey")
                    dispatchKeyEvent(ev, 0, keyCode.toInt().toChar(), kkey, keyCode.toInt().convert())
                    //break@loop
                }
                MotionNotify, ButtonPress, ButtonRelease -> {
                    val mot = e.xmotion
                    val but = e.xbutton
                    val ev = when (e.type) {
                        MotionNotify -> MouseEvent.Type.MOVE
                        ButtonPress -> MouseEvent.Type.DOWN
                        ButtonRelease -> MouseEvent.Type.UP
                        else -> MouseEvent.Type.MOVE
                    }
                    val button = when (but.button.toInt()) {
                        1 -> MouseButton.LEFT
                        2 -> MouseButton.MIDDLE
                        3 -> MouseButton.RIGHT
                        // http://who-t.blogspot.com/2011/09/whats-new-in-xi-21-smooth-scrolling.html
                        4 -> MouseButton.BUTTON4 // WHEEL_UP!
                        5 -> MouseButton.BUTTON5 // WHEEL_DOWN!
                        6 -> MouseButton.BUTTON6 // WHEEL_LEFT!
                        7 -> MouseButton.BUTTON7 // WHEEL_RIGHT!
                        else -> MouseButton.BUTTON_UNKNOWN
                    }
                    //println(XMotionEvent().size())
                    //println(mot.size)
                    //println("MOUSE ${ev} ${mot.x} ${mot.y} ${mot.button}")

                    dispatchSimpleMouseEvent(ev, 0, mot.x, mot.y, button, simulateClickOnUp = false)
                }
                else -> {
                    //println("OTHER EVENT ${e.type}")
                }
            }
        }
    }

    override fun doInitRender() {
        try {
            ctx.makeCurrent()
            NativeBaseKmlGl.glViewportExt(0, 0, width, height)
            NativeBaseKmlGl.glClearColorExt(.3f, .6f, .3f, 1f)
            NativeBaseKmlGl.glClearExt(NativeBaseKmlGl.GL_COLOR_BUFFER_BIT)

            // https://github.com/spurious/SDL-mirror/blob/4c1c6d03ddaa3095b3c63c38ddd0a6cfad58b752/src/video/windows/SDL_windowsopengl.c#L439-L447
            val dpy = glXGetCurrentDisplay()
            val drawable = glXGetCurrentDrawable()
            swapIntervalEXT?.invoke(dpy, drawable, vsync.toInt())
        } catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }
    }

    override fun doSwapBuffers() {
        ctx.swapBuffers()
    }

    override fun doDestroy() {
        XDestroyWindow(d, w)
        XCloseDisplay(d)
    }
}
