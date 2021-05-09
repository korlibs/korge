package com.soywiz.korgw

//import X11.*
import GL.*
import com.soywiz.kds.IntMap
import com.soywiz.kgl.*
import com.soywiz.kmem.startAddressOf
import com.soywiz.kmem.write32LE
import com.soywiz.korag.AGOpengl
import com.soywiz.korev.Key
import com.soywiz.korev.MouseButton
import com.soywiz.korev.MouseEvent
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.lang.Charsets
import com.soywiz.korio.lang.toString
import com.soywiz.korio.stream.MemorySyncStream
import com.soywiz.korio.stream.toByteArray
import kotlinx.cinterop.*
import platform.posix.fread
import platform.posix.pclose
import platform.posix.popen

//typealias XVisualInfo = IntVar
//typealias GLXDrawable = Window
//typealias GLXContext = COpaquePointer?
//
//val glXChooseVisual by GLFunc<(CPointer<Display>?, Int, CPointer<IntVar>) -> CPointer<XVisualInfo>?>()
//val glXCreateContext by GLFunc<(CPointer<Display>?, CPointer<XVisualInfo>?, GLXContext, Int) -> GLXContext>()
//val glXMakeCurrent by GLFunc<(CPointer<Display>?, GLXDrawable, GLXContext) -> Int>()
//val glXSwapBuffers by GLFunc<(CPointer<Display>?, GLXDrawable) -> Unit>()
//val glXGetCurrentDisplay by GLFunc<() -> CPointer<Display>?>()
//val glXGetCurrentDrawable by GLFunc<() -> Window>()
//
//const val GLX_RGBA = 4
//const val GLX_DOUBLEBUFFER = 5
//const val GLX_DEPTH_SIZE = 12
//const val GLX_STENCIL_SIZE = 3

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

private fun escapeshellarg(str: String) = "'" + str.replace("'", "\\'") + "'"

// @TODO: Move this to Korio exec/execToString
class NativeZenityDialogs : ZenityDialogs() {
    override suspend fun exec(vararg args: String): String = memScoped {
        val command = "/bin/sh -c '" + args.joinToString(" ") { escapeshellarg(it) }.replace("'", "\"'\"") + "' 2>&1"
        println("COMMAND: $command")
        val fp = popen(command, "r")
            ?: error("Couldn't exec ${args.toList()}")

        val out = MemorySyncStream()
        val TMPSIZE = 1024
        val temp = allocArray<ByteVar>(TMPSIZE)

        do {
            val res = fread(temp, 1.convert(), TMPSIZE.convert(), fp)
            for (n in 0 until res.toInt()) {
                out.write(temp[n].toInt() and 0xFF)
            }
        } while (res.toInt() > 0)
        pclose(fp)
        return out.toByteArray().toString(Charsets.UTF8)
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

    fun realSetTitle(title: String): Unit = run {
        if (d == null || w == NilWin) return@run
        //X.XSetWMIconName(d, w, )
        XStoreName(d, w, title)
        XSetIconName(d, w, title)
    }

    fun realSetIcon(value: Bitmap?): Unit = run {
        if (d == null || w == NilWin || value == null) return@run
        memScoped {
            val property = XInternAtom(d, "_NET_WM_ICON", 0)
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
    }

    // https://stackoverflow.com/questions/9065669/x11-glx-fullscreen-mode
    fun realSetFullscreen(value: Boolean): Unit {
        println("realSetFullscreen. value=$value")
        if (d == null || w == NilWin) return
        val fullscreen = value
        memScoped {

            val _NET_WM_STATE = XInternAtom(d, "_NET_WM_STATE", 1)
            val _NET_WM_STATE_ADD = XInternAtom(d, "_NET_WM_STATE_ADD", 1)
            val _NET_WM_STATE_REMOVE = XInternAtom(d, "_NET_WM_STATE_REMOVE", 1)
            val _NET_WM_STATE_FULLSCREEN = XInternAtom(d, "_NET_WM_STATE_FULLSCREEN", 1)

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
    }

    fun realSetVisible(value: Boolean): Unit = run {
        if (d == null || w == NilWin) return@run
    }

    override fun doInitialize() {
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

        val wmDeleteMessage = XInternAtom(d, "WM_DELETE_WINDOW", 0)
        memScoped {
            val protocolsArray = allocArray<AtomVar>(1)
            protocolsArray[0] = wmDeleteMessage
            XSetWMProtocols(d, w, protocolsArray, 1)
        }
    }

    lateinit var ctx: X11OpenglContext

    val doubleBuffered = false
    //val doubleBuffered = true

    override fun doHandleEvents() = memScoped {
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
        ctx.makeCurrent()
        NativeBaseKmlGl.glViewportExt(0, 0, width, height)
        NativeBaseKmlGl.glClearColorExt(.3f, .6f, .3f, 1f)
        NativeBaseKmlGl.glClearExt(NativeBaseKmlGl.GL_COLOR_BUFFER_BIT)

        // https://github.com/spurious/SDL-mirror/blob/4c1c6d03ddaa3095b3c63c38ddd0a6cfad58b752/src/video/windows/SDL_windowsopengl.c#L439-L447
        val dpy = glXGetCurrentDisplay()
        val drawable = glXGetCurrentDrawable()
        swapIntervalEXT?.invoke(dpy, drawable, vsync.toInt())
    }

    override fun doSwapBuffers() {
        ctx.swapBuffers()
    }

    override fun doDestroy() {
        XDestroyWindow(d, w)
        XCloseDisplay(d)
    }
}

internal val XK_KeyMap: IntMap<Key> by lazy {
    IntMap<Key>().apply {
        this[XK_space] = Key.SPACE
        this[XK_exclam] = Key.UNKNOWN
        this[XK_quotedbl] = Key.UNKNOWN
        this[XK_numbersign] = Key.UNKNOWN
        this[XK_dollar] = Key.UNKNOWN
        this[XK_percent] = Key.UNKNOWN
        this[XK_ampersand] = Key.UNKNOWN
        this[XK_apostrophe] = Key.APOSTROPHE
        this[XK_quoteright] = Key.UNKNOWN
        this[XK_parenleft] = Key.UNKNOWN
        this[XK_parenright] = Key.UNKNOWN
        this[XK_asterisk] = Key.UNKNOWN
        this[XK_plus] = Key.KP_ADD
        this[XK_comma] = Key.COMMA
        this[XK_minus] = Key.MINUS
        this[XK_period] = Key.PERIOD
        this[XK_slash] = Key.SLASH
        this[XK_0] = Key.N0
        this[XK_1] = Key.N1
        this[XK_2] = Key.N2
        this[XK_3] = Key.N3
        this[XK_4] = Key.N4
        this[XK_5] = Key.N5
        this[XK_6] = Key.N6
        this[XK_7] = Key.N7
        this[XK_8] = Key.N8
        this[XK_9] = Key.N9
        this[XK_colon] = Key.UNKNOWN
        this[XK_semicolon] = Key.SEMICOLON
        this[XK_less] = Key.UNKNOWN
        this[XK_equal] = Key.EQUAL
        this[XK_greater] = Key.UNKNOWN
        this[XK_question] = Key.UNKNOWN
        this[XK_at] = Key.UNKNOWN
        this[XK_A] = Key.A
        this[XK_B] = Key.B
        this[XK_C] = Key.C
        this[XK_D] = Key.D
        this[XK_E] = Key.E
        this[XK_F] = Key.F
        this[XK_G] = Key.G
        this[XK_H] = Key.H
        this[XK_I] = Key.I
        this[XK_J] = Key.J
        this[XK_K] = Key.K
        this[XK_L] = Key.L
        this[XK_M] = Key.M
        this[XK_N] = Key.N
        this[XK_O] = Key.O
        this[XK_P] = Key.P
        this[XK_Q] = Key.Q
        this[XK_R] = Key.R
        this[XK_S] = Key.S
        this[XK_T] = Key.T
        this[XK_U] = Key.U
        this[XK_V] = Key.V
        this[XK_W] = Key.W
        this[XK_X] = Key.X
        this[XK_Y] = Key.Y
        this[XK_Z] = Key.Z
        this[XK_bracketleft] = Key.UNKNOWN
        this[XK_backslash] = Key.BACKSLASH
        this[XK_bracketright] = Key.UNKNOWN
        this[XK_asciicircum] = Key.UNKNOWN
        this[XK_underscore] = Key.UNKNOWN
        this[XK_grave] = Key.UNKNOWN
        this[XK_quoteleft] = Key.UNKNOWN
        this[XK_a] = Key.A
        this[XK_b] = Key.B
        this[XK_c] = Key.C
        this[XK_d] = Key.D
        this[XK_e] = Key.E
        this[XK_f] = Key.F
        this[XK_g] = Key.G
        this[XK_h] = Key.H
        this[XK_i] = Key.I
        this[XK_j] = Key.J
        this[XK_k] = Key.K
        this[XK_l] = Key.L
        this[XK_m] = Key.M
        this[XK_n] = Key.N
        this[XK_o] = Key.O
        this[XK_p] = Key.P
        this[XK_q] = Key.Q
        this[XK_r] = Key.R
        this[XK_s] = Key.S
        this[XK_t] = Key.T
        this[XK_u] = Key.U
        this[XK_v] = Key.V
        this[XK_w] = Key.W
        this[XK_x] = Key.X
        this[XK_y] = Key.Y
        this[XK_z] = Key.Z
        //this[XK_leftarrow] = Key.LEFT
        //this[XK_uparrow] = Key.UP
        //this[XK_rightarrow] = Key.RIGHT
        //this[XK_downarrow] = Key.DOWN
        this[XK_BackSpace] = Key.BACKSPACE
        this[XK_Tab] = Key.TAB
        this[XK_Linefeed] = Key.UNKNOWN
        this[XK_Clear] = Key.CLEAR
        this[XK_Return] = Key.RETURN
        this[XK_Pause] = Key.PAUSE
        this[XK_Scroll_Lock] = Key.SCROLL_LOCK
        this[XK_Sys_Req] = Key.UNKNOWN
        this[XK_Escape] = Key.ESCAPE
        this[XK_Delete] = Key.DELETE
        this[XK_Home] = Key.HOME
        this[XK_Left] = Key.LEFT
        this[XK_Up] = Key.UP
        this[XK_Right] = Key.RIGHT
        this[XK_Down] = Key.DOWN
        this[XK_Prior] = Key.UNKNOWN
        this[XK_Page_Up] = Key.PAGE_UP
        this[XK_Next] = Key.UNKNOWN
        this[XK_Page_Down] = Key.PAGE_DOWN
        this[XK_End] = Key.END
        this[XK_Begin] = Key.UNKNOWN
        this[XK_Select] = Key.UNKNOWN
        this[XK_Print] = Key.PRINT_SCREEN
        this[XK_Execute] = Key.UNKNOWN
        this[XK_Insert] = Key.INSERT
        this[XK_Undo] = Key.UNKNOWN
        this[XK_Redo] = Key.UNKNOWN
        this[XK_Menu] = Key.MENU
        this[XK_Find] = Key.UNKNOWN
        this[XK_Cancel] = Key.CANCEL
        this[XK_Help] = Key.HELP
        this[XK_Break] = Key.UNKNOWN
        this[XK_Mode_switch] = Key.UNKNOWN
        this[XK_script_switch] = Key.UNKNOWN
        this[XK_Num_Lock] = Key.NUM_LOCK
        this[XK_KP_Space] = Key.UNKNOWN
        this[XK_KP_Tab] = Key.UNKNOWN
        this[XK_KP_Enter] = Key.KP_ENTER
        this[XK_KP_F1] = Key.F1
        this[XK_KP_F2] = Key.F2
        this[XK_KP_F3] = Key.F3
        this[XK_KP_F4] = Key.F4
        this[XK_KP_Home] = Key.HOME
        this[XK_KP_Left] = Key.KP_LEFT
        this[XK_KP_Up] = Key.KP_UP
        this[XK_KP_Right] = Key.KP_RIGHT
        this[XK_KP_Down] = Key.KP_DOWN
        this[XK_KP_Prior] = Key.UNKNOWN
        this[XK_KP_Page_Up] = Key.UNKNOWN
        this[XK_KP_Next] = Key.UNKNOWN
        this[XK_KP_Page_Down] = Key.UNKNOWN
        this[XK_KP_End] = Key.END
        this[XK_KP_Begin] = Key.HOME
        this[XK_KP_Insert] = Key.INSERT
        this[XK_KP_Delete] = Key.DELETE
        this[XK_KP_Equal] = Key.KP_EQUAL
        this[XK_KP_Multiply] = Key.KP_MULTIPLY
        this[XK_KP_Add] = Key.KP_ADD
        this[XK_KP_Separator] = Key.KP_SEPARATOR
        this[XK_KP_Subtract] = Key.KP_SUBTRACT
        this[XK_KP_Decimal] = Key.KP_DECIMAL
        this[XK_KP_Divide] = Key.KP_DIVIDE
        this[XK_KP_0] = Key.KP_0
        this[XK_KP_1] = Key.KP_1
        this[XK_KP_2] = Key.KP_2
        this[XK_KP_3] = Key.KP_3
        this[XK_KP_4] = Key.KP_4
        this[XK_KP_5] = Key.KP_5
        this[XK_KP_6] = Key.KP_6
        this[XK_KP_7] = Key.KP_7
        this[XK_KP_8] = Key.KP_8
        this[XK_KP_9] = Key.KP_9
        this[XK_F1] = Key.F1
        this[XK_F2] = Key.F2
        this[XK_F3] = Key.F3
        this[XK_F4] = Key.F4
        this[XK_F5] = Key.F5
        this[XK_F6] = Key.F6
        this[XK_F7] = Key.F7
        this[XK_F8] = Key.F8
        this[XK_F9] = Key.F9
        this[XK_F10] = Key.F10
        this[XK_F11] = Key.F11
        this[XK_F12] = Key.F12
        this[XK_F13] = Key.F13
        this[XK_F14] = Key.F14
        this[XK_F15] = Key.F15
        this[XK_F16] = Key.F16
        this[XK_F17] = Key.F17
        this[XK_F18] = Key.F18
        this[XK_F19] = Key.F19
        this[XK_F20] = Key.F20
        this[XK_F21] = Key.F21
        this[XK_F22] = Key.F22
        this[XK_F23] = Key.F23
        this[XK_F24] = Key.F24
        this[XK_F25] = Key.F25
        this[XK_F26] = Key.UNKNOWN
        this[XK_F27] = Key.UNKNOWN
        this[XK_F28] = Key.UNKNOWN
        this[XK_F29] = Key.UNKNOWN
        this[XK_F30] = Key.UNKNOWN
        this[XK_F31] = Key.UNKNOWN
        this[XK_F32] = Key.UNKNOWN
        this[XK_F33] = Key.UNKNOWN
        this[XK_F34] = Key.UNKNOWN
        this[XK_F35] = Key.UNKNOWN

        this[XK_R1] = Key.UNKNOWN
        this[XK_R2] = Key.UNKNOWN
        this[XK_R3] = Key.UNKNOWN
        this[XK_R4] = Key.UNKNOWN
        this[XK_R5] = Key.UNKNOWN
        this[XK_R6] = Key.UNKNOWN
        this[XK_R7] = Key.UNKNOWN
        this[XK_R8] = Key.UNKNOWN
        this[XK_R9] = Key.UNKNOWN
        this[XK_R10] = Key.UNKNOWN
        this[XK_R11] = Key.UNKNOWN
        this[XK_R12] = Key.UNKNOWN
        this[XK_R13] = Key.UNKNOWN
        this[XK_R14] = Key.UNKNOWN
        this[XK_R15] = Key.UNKNOWN

        this[XK_L1] = Key.UNKNOWN
        this[XK_L2] = Key.UNKNOWN
        this[XK_L3] = Key.UNKNOWN
        this[XK_L4] = Key.UNKNOWN
        this[XK_L5] = Key.UNKNOWN
        this[XK_L6] = Key.UNKNOWN
        this[XK_L7] = Key.UNKNOWN
        this[XK_L8] = Key.UNKNOWN
        this[XK_L9] = Key.UNKNOWN
        this[XK_L10] = Key.UNKNOWN

        this[XK_Shift_L] = Key.LEFT_SHIFT
        this[XK_Shift_R] = Key.RIGHT_SHIFT
        this[XK_Control_L] = Key.LEFT_CONTROL
        this[XK_Control_R] = Key.RIGHT_CONTROL
        this[XK_Caps_Lock] = Key.CAPS_LOCK
        this[XK_Shift_Lock] = Key.CAPS_LOCK
        this[XK_Meta_L] = Key.LEFT_SUPER
        this[XK_Meta_R] = Key.RIGHT_SUPER
        this[XK_Alt_L] = Key.LEFT_ALT
        this[XK_Alt_R] = Key.RIGHT_ALT
        this[XK_Super_L] = Key.LEFT_SUPER
        this[XK_Super_R] = Key.RIGHT_SUPER
        this[XK_Hyper_L] = Key.LEFT_SUPER
        this[XK_Hyper_R] = Key.RIGHT_SUPER
    }
}
