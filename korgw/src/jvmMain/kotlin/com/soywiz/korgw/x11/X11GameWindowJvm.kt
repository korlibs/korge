package com.soywiz.korgw.x11

import com.soywiz.kgl.KmlGl
import com.soywiz.kgl.checkedIf
import com.soywiz.kmem.toInt
import com.soywiz.kmem.write32LE
import com.soywiz.korag.AGOpengl
import com.soywiz.korev.Key
import com.soywiz.korev.MouseButton
import com.soywiz.korev.MouseEvent
import com.soywiz.korgw.DialogInterface
import com.soywiz.korgw.EventLoopGameWindow
import com.soywiz.korgw.ZenityDialogs
import com.soywiz.korim.bitmap.Bitmap
import com.sun.jna.*
import com.sun.jna.platform.unix.X11.*

//class X11Ag(val window: X11GameWindow, override val gl: KmlGl = LogKmlGlProxy(X11KmlGl())) : AGOpengl() {
class X11Ag(val window: X11GameWindow, val checkGl: Boolean, override val gl: KmlGl = X11KmlGl.checkedIf(checkGl)) : AGOpengl() {
    override val gles: Boolean = true
    override val linux: Boolean = true
    override val nativeComponent: Any = window
}

class X11GameWindow(val checkGl: Boolean) : EventLoopGameWindow(), DialogInterface by ZenityDialogs() {
    override val ag: X11Ag by lazy { X11Ag(this, checkGl) }
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

    var d: Display? = null
    var root: Window? = null
    val NilWin: Window? = null
    var w: Window? = null
    var s: Int = 0

    fun realSetTitle(title: String): Unit = X.run {
        if (d == null || w == NilWin) return@run
        //X.XSetWMIconName(d, w, )
        X.XStoreName(d, w, title)
        X.XSetIconName(d, w, title)
    }

    fun realSetIcon(value: Bitmap?): Unit = X.run {
        if (d == null || w == NilWin || value == null) return@run
        val property = XInternAtom(d, "_NET_WM_ICON", false)
        val bmp = value.toBMP32()
        val VSIZE = NativeLong.SIZE
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
        val mem = Memory((bytes.size * 8).toLong())
        mem.write(0L, bytes, 0, bytes.size)
        XChangeProperty(
            d, w, property, XA_CARDINAL, 32, PropModeReplace,
            mem, bytes.size / NativeLong.SIZE
        )
    }

    // https://stackoverflow.com/questions/9065669/x11-glx-fullscreen-mode
    fun realSetFullscreen(value: Boolean): Unit = X.run {
        if (d == null || w == NilWin) return@run
    }
    fun realSetVisible(value: Boolean): Unit = X.run {
        if (d == null || w == NilWin) return@run
    }

    // https://github.com/AlexeyAB/SDL-OculusRift/blob/master/src/video/x11/SDL_x11opengl.c
    override fun doInitialize() {
        d = X.XOpenDisplay(null) ?: error("Can't open main display")
        s = X.XDefaultScreen(d)
        root = X.XDefaultRootWindow(d)

        val vi = X11OpenglContext.chooseVisuals(d, s)

        //val cmap = XCreateColormap(d, root, vi->visual, AllocNone);
        val screenWidth = X.XDisplayWidth(d, s)
        val screenHeight = X.XDisplayHeight(d, s)

        val gameWindow = this@X11GameWindow

        val winX = screenWidth / 2 - width / 2
        val winY = screenHeight / 2 - height / 2

        println("screenWidth: $screenWidth, screenHeight: $screenHeight, winX=$winX, winY=$winY, width=$width, height=$height")

        w = X.XCreateSimpleWindow(
            d, X.XRootWindow(d, s),
            winX, winY,
            width, height,
            1,
            X.XBlackPixel(d, s), X.XWhitePixel(d, s)
        )
        //val attr = XSetWindowAttributes().apply { autoWrite() }.apply { autoRead() }
        //XChangeWindowAttributes(d, w, NativeLong(0L), attr)

        val eventMask = NativeLong(
            (ExposureMask
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
                .toLong()
        )

        X.XSelectInput(d, w, eventMask)
        X.XMapWindow(d, w)
        realSetIcon(icon)
        realSetVisible(fullscreen)
        realSetVisible(visible)
        realSetTitle(title)

        ctx = X11OpenglContext(d, w, s, vi, doubleBuffered = doubleBuffered)
        ctx.makeCurrent()

        val wmDeleteMessage = X.XInternAtom(d, "WM_DELETE_WINDOW", false)
        if (wmDeleteMessage != null) {
            X.XSetWMProtocols(d, w, arrayOf(wmDeleteMessage), 1)
        }
    }

    val doubleBuffered = false
    //val doubleBuffered = true
    lateinit var ctx: X11OpenglContext

    override fun doSwapBuffers() {
        //println("doSwapBuffers")
        ctx.swapBuffers()
    }

    interface glXSwapIntervalEXTCallback : Callback {
        fun callback(dpy: Display?, draw: Pointer, value: Int)
    }

    private var glXSwapIntervalEXTSet: Boolean = false
    private var swapIntervalEXT: glXSwapIntervalEXTCallback? = null
    private var swapIntervalEXTPointer: Pointer? = null

    override fun doInitRender() {
        ctx.makeCurrent()

        if (!glXSwapIntervalEXTSet) {
            glXSwapIntervalEXTSet = true
            swapIntervalEXTPointer = X.glXGetProcAddress("glXSwapIntervalEXT")

            swapIntervalEXT = when {
                swapIntervalEXTPointer != Pointer.NULL -> CallbackReference.getCallback(glXSwapIntervalEXTCallback::class.java, swapIntervalEXTPointer) as? glXSwapIntervalEXTCallback?
                else -> null
            }
            println("swapIntervalEXT: $swapIntervalEXT")
        }

        val dpy = X.glXGetCurrentDisplay()
        val drawable = X.glXGetCurrentDrawable()
        swapIntervalEXT?.callback(dpy, drawable, vsync.toInt())
        //swapIntervalEXT?.callback(dpy, drawable, 0)
        //swapIntervalEXT?.callback(dpy, drawable, 1)
        //glXSwapIntervalEXT?.callback(dpy, drawable, 0)

        X.glViewport(0, 0, width, height)
        X.glClearColor(.3f, .6f, .3f, 1f)
        X.glClear(GL.GL_COLOR_BUFFER_BIT)
    }

    override fun doDestroy() {
        X.XDestroyWindow(d, w)
        X.XCloseDisplay(d)
    }

    val e = XEvent()
    override fun doHandleEvents() {
        loop@ while (running) {
            if (X.XPending(d) == 0) return
            X.XNextEvent(d, e)
            when (e.type) {
                Expose -> if (e.xexpose.count == 0) render(doUpdate = false)
                ClientMessage, DestroyNotify -> close()
                ConfigureNotify -> {
                    val conf = XConfigureEvent(e.pointer)
                    width = conf.width
                    height = conf.height
                    dispatchReshapeEvent(conf.x, conf.y, conf.width, conf.height)
                    if (!doubleBuffered) {
                        render(doUpdate = false)
                    }
                    //println("RESIZED! ${conf.width} ${conf.height}")
                }
                KeyPress, KeyRelease -> {
                    val pressing = e.type == KeyPress
                    val ev =
                        if (pressing) com.soywiz.korev.KeyEvent.Type.DOWN else com.soywiz.korev.KeyEvent.Type.UP
                    val keyCode = XKeyEvent(e.pointer).keycode.toInt()
                    val kkey = XK_KeyMap[X.XLookupKeysym(e, 0)] ?: Key.UNKNOWN
                    //println("KEY: $ev, ${keyCode.toChar()}, $kkey, $keyCode, keySym=$keySym")
                    dispatchKeyEvent(ev, 0, keyCode.toChar(), kkey, keyCode)
                    //break@loop
                }
                MotionNotify, ButtonPress, ButtonRelease -> {
                    val mot = MyXMotionEvent(e.pointer)
                    //val mot = e.xmotion
                    val but = e.xbutton

                    val isShiftDown = false
                    val isCtrlDown = false
                    val isAltDown = false
                    val isMetaDown = false
                    val evType = when (e.type) {
                        MotionNotify -> MouseEvent.Type.MOVE
                        ButtonPress -> MouseEvent.Type.DOWN
                        ButtonRelease -> MouseEvent.Type.UP
                        else -> MouseEvent.Type.MOVE
                    }

                    //in 4..7 -> MouseButton.LEFT // 4=WHEEL_UP, 5=WHEEL_DOWN, 6=WHEEL_LEFT, 7=WHEEL_RIGHT!
                    val scrollDeltaX = when (mot.button) {
                        6 -> -1.0
                        7 -> +1.0
                        else -> 0.0
                    }
                    val scrollDeltaY = when (mot.button) {
                        4 -> -1.0
                        5 -> +1.0
                        else -> 0.0
                    }
                    val scrollDeltaZ = 0.0

                    val button = when (mot.button) {
                        1 -> MouseButton.LEFT
                        2 -> MouseButton.MIDDLE
                        3 -> MouseButton.RIGHT
                        in 4..7 -> MouseButton.BUTTON_WHEEL
                        else -> MouseButton.BUTTON_UNKNOWN
                    }

                    val realEvType = when (button) {
                        MouseButton.BUTTON_WHEEL -> {
                            when (evType) {
                                MouseEvent.Type.DOWN -> MouseEvent.Type.SCROLL
                                else -> null
                            }
                        }
                        else -> evType
                    }

                    if (realEvType != null) {
                        dispatchMouseEvent(
                            realEvType, 0, mot.x, mot.y,
                            button, 0,
                            scrollDeltaX, scrollDeltaY, scrollDeltaZ, isShiftDown, isCtrlDown, isAltDown, isMetaDown, false,
                            simulateClickOnUp = true
                        )
                    }
                }
                else -> {
                    //println("OTHER EVENT ${e.type}")
                }
            }
        }
    }
}
