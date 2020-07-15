package com.soywiz.korgw.jogl

/*
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.*
import com.jogamp.opengl.awt.GLCanvas
import com.soywiz.kgl.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korgw.GameWindow
import com.soywiz.korim.awt.AwtNativeImage
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.dynamic.*
import com.soywiz.korio.util.*
import java.awt.*
import java.awt.event.*
import java.awt.event.KeyEvent
import javax.swing.*

class JoglGameWindow : GameWindow() {
    override val ag: AGAwt by lazy {
        AGAwt(AGConfig(antialiasHint = (quality != Quality.PERFORMANCE)))
    }

    val frame = object : JFrame() {
        init {
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            contentPane.add(ag.glcanvas)
            ag.glcanvas.requestFocusInWindow()
        }

        override fun createRootPane(): JRootPane = super.createRootPane().apply {
            putClientProperty("apple.awt.fullscreenable", true)
        }
    }

    override var title: String
        get() = frame.title
        set(value) {
            frame.title = value
        }

    override var width: Int = 0; private set
    override var height: Int = 0; private set

    override fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
        frame.contentPane.preferredSize = Dimension(width, height)
        //frame.setSize(width, height)
        frame.pack()
        frame.setLocationRelativeTo(null)

        //println(ag.glcanvas.size)

        //val screenSize = Toolkit.getDefaultToolkit().screenSize
        //frame.setPosition(
        //    (screenSize.width - value.width) / 2,
        //    (screenSize.height - value.height) / 2
        //)
        //window.setLocationRelativeTo(null)
    }

    override var icon: Bitmap?
        get() = super.icon
        set(value) {
        }
    override var fullscreen: Boolean
        //get() = (frame.extendedState and JFrame.MAXIMIZED_BOTH) != 0
        get() = if (OS.isMac) {
            val screenSize = Toolkit.getDefaultToolkit().screenSize
            val frameSize = frame.size
            //println("screenSize=$screenSize, frameSize=$frameSize")
            (screenSize == frameSize)
        } else {
            frame.graphicsConfiguration.device.fullScreenWindow == frame
        }
        set(value) {
            if (OS.isMac) {
                if (fullscreen != value) {
                    KDynamic {
                        global["com.apple.eawt.Application"].dynamicInvoke("getApplication")
                            .dynamicInvoke("requestToggleFullScreen", frame)
                    }
                }
            } else {
                frame.graphicsConfiguration.device.fullScreenWindow = if (value) frame else null
            }
            /*
            if (value) {
                frame.extendedState = frame.extendedState or JFrame.MAXIMIZED_BOTH
            } else {
                frame.extendedState = frame.extendedState and JFrame.MAXIMIZED_BOTH.inv()
            }
            */
        }
    override var visible: Boolean
        get() = frame.isVisible
        set(value) {
            frame.isVisible = value
        }

    private fun dispatchME(e: java.awt.event.MouseEvent, type: com.soywiz.korev.MouseEvent.Type) {
        dispatch(mouseEvent {
            this.type = type
            this.id = 0
            this.x = e.x
            this.y = e.y
            this.button = MouseButton[e.button]
        })
    }

    private fun dispatchKE(e: java.awt.event.KeyEvent, type: com.soywiz.korev.KeyEvent.Type) {
        dispatch(keyEvent {
            this.type = type
            this.id = 0
            this.keyCode = e.keyCode
            this.character = e.keyChar
            this.key = when (e.keyCode) {
                KeyEvent.VK_ENTER          -> Key.ENTER
                KeyEvent.VK_BACK_SPACE     -> Key.BACKSPACE
                KeyEvent.VK_TAB            -> Key.TAB
                KeyEvent.VK_CANCEL         -> Key.CANCEL
                KeyEvent.VK_CLEAR          -> Key.CLEAR
                KeyEvent.VK_SHIFT          -> Key.LEFT_SHIFT
                KeyEvent.VK_CONTROL        -> Key.LEFT_CONTROL
                KeyEvent.VK_ALT            -> Key.LEFT_ALT
                KeyEvent.VK_PAUSE          -> Key.PAUSE
                KeyEvent.VK_CAPS_LOCK      -> Key.CAPS_LOCK
                KeyEvent.VK_ESCAPE         -> Key.ESCAPE
                KeyEvent.VK_SPACE          -> Key.SPACE
                KeyEvent.VK_PAGE_UP        -> Key.PAGE_UP
                KeyEvent.VK_PAGE_DOWN      -> Key.PAGE_DOWN
                KeyEvent.VK_END            -> Key.END
                KeyEvent.VK_HOME           -> Key.HOME
                KeyEvent.VK_LEFT           -> Key.LEFT
                KeyEvent.VK_UP             -> Key.UP
                KeyEvent.VK_RIGHT          -> Key.RIGHT
                KeyEvent.VK_DOWN           -> Key.DOWN
                KeyEvent.VK_COMMA          -> Key.COMMA
                KeyEvent.VK_MINUS          -> Key.MINUS
                KeyEvent.VK_PERIOD         -> Key.PERIOD
                KeyEvent.VK_SLASH          -> Key.SLASH
                KeyEvent.VK_0              -> Key.N0
                KeyEvent.VK_1              -> Key.N1
                KeyEvent.VK_2              -> Key.N2
                KeyEvent.VK_3              -> Key.N3
                KeyEvent.VK_4              -> Key.N4
                KeyEvent.VK_5              -> Key.N5
                KeyEvent.VK_6              -> Key.N6
                KeyEvent.VK_7              -> Key.N7
                KeyEvent.VK_8              -> Key.N8
                KeyEvent.VK_9              -> Key.N9
                KeyEvent.VK_SEMICOLON      -> Key.SEMICOLON
                KeyEvent.VK_EQUALS         -> Key.EQUAL
                KeyEvent.VK_A              -> Key.A
                KeyEvent.VK_B              -> Key.B
                KeyEvent.VK_C              -> Key.C
                KeyEvent.VK_D              -> Key.D
                KeyEvent.VK_E              -> Key.E
                KeyEvent.VK_F              -> Key.F
                KeyEvent.VK_G              -> Key.G
                KeyEvent.VK_H              -> Key.H
                KeyEvent.VK_I              -> Key.I
                KeyEvent.VK_J              -> Key.J
                KeyEvent.VK_K              -> Key.K
                KeyEvent.VK_L              -> Key.L
                KeyEvent.VK_M              -> Key.M
                KeyEvent.VK_N              -> Key.N
                KeyEvent.VK_O              -> Key.O
                KeyEvent.VK_P              -> Key.P
                KeyEvent.VK_Q              -> Key.Q
                KeyEvent.VK_R              -> Key.R
                KeyEvent.VK_S              -> Key.S
                KeyEvent.VK_T              -> Key.T
                KeyEvent.VK_U              -> Key.U
                KeyEvent.VK_V              -> Key.V
                KeyEvent.VK_W              -> Key.W
                KeyEvent.VK_X              -> Key.X
                KeyEvent.VK_Y              -> Key.Y
                KeyEvent.VK_Z              -> Key.Z
                KeyEvent.VK_OPEN_BRACKET   -> Key.OPEN_BRACKET
                KeyEvent.VK_BACK_SLASH     -> Key.BACKSLASH
                KeyEvent.VK_CLOSE_BRACKET  -> Key.CLOSE_BRACKET
                KeyEvent.VK_NUMPAD0        -> Key.NUMPAD0
                KeyEvent.VK_NUMPAD1        -> Key.NUMPAD1
                KeyEvent.VK_NUMPAD2        -> Key.NUMPAD2
                KeyEvent.VK_NUMPAD3        -> Key.NUMPAD3
                KeyEvent.VK_NUMPAD4        -> Key.NUMPAD4
                KeyEvent.VK_NUMPAD5        -> Key.NUMPAD5
                KeyEvent.VK_NUMPAD6        -> Key.NUMPAD6
                KeyEvent.VK_NUMPAD7        -> Key.NUMPAD7
                KeyEvent.VK_NUMPAD8        -> Key.NUMPAD8
                KeyEvent.VK_NUMPAD9        -> Key.NUMPAD9
                KeyEvent.VK_MULTIPLY       -> Key.KP_MULTIPLY
                KeyEvent.VK_ADD            -> Key.KP_ADD
                KeyEvent.VK_SEPARATER      -> Key.KP_SEPARATOR
                //KeyEvent.VK_SEPARATOR      -> Key.KP_SEPARATOR
                KeyEvent.VK_SUBTRACT       -> Key.KP_SUBTRACT
                KeyEvent.VK_DECIMAL        -> Key.KP_DECIMAL
                KeyEvent.VK_DIVIDE         -> Key.KP_DIVIDE
                KeyEvent.VK_DELETE         -> Key.DELETE
                KeyEvent.VK_NUM_LOCK       -> Key.NUM_LOCK
                KeyEvent.VK_SCROLL_LOCK    -> Key.SCROLL_LOCK
                KeyEvent.VK_F1             -> Key.F1
                KeyEvent.VK_F2             -> Key.F2
                KeyEvent.VK_F3             -> Key.F3
                KeyEvent.VK_F4             -> Key.F4
                KeyEvent.VK_F5             -> Key.F5
                KeyEvent.VK_F6             -> Key.F6
                KeyEvent.VK_F7             -> Key.F7
                KeyEvent.VK_F8             -> Key.F8
                KeyEvent.VK_F9             -> Key.F9
                KeyEvent.VK_F10            -> Key.F10
                KeyEvent.VK_F11            -> Key.F11
                KeyEvent.VK_F12            -> Key.F12
                KeyEvent.VK_F13            -> Key.F13
                KeyEvent.VK_F14            -> Key.F14
                KeyEvent.VK_F15            -> Key.F15
                KeyEvent.VK_F16            -> Key.F16
                KeyEvent.VK_F17            -> Key.F17
                KeyEvent.VK_F18            -> Key.F18
                KeyEvent.VK_F19            -> Key.F19
                KeyEvent.VK_F20            -> Key.F20
                KeyEvent.VK_F21            -> Key.F21
                KeyEvent.VK_F22            -> Key.F22
                KeyEvent.VK_F23            -> Key.F23
                KeyEvent.VK_F24            -> Key.F24
                KeyEvent.VK_PRINTSCREEN    -> Key.PRINT_SCREEN
                KeyEvent.VK_INSERT         -> Key.INSERT
                KeyEvent.VK_HELP           -> Key.HELP
                KeyEvent.VK_META           -> Key.META
                KeyEvent.VK_BACK_QUOTE     -> Key.BACKQUOTE
                KeyEvent.VK_QUOTE          -> Key.QUOTE
                KeyEvent.VK_KP_UP          -> Key.KP_UP
                KeyEvent.VK_KP_DOWN        -> Key.KP_DOWN
                KeyEvent.VK_KP_LEFT        -> Key.KP_LEFT
                KeyEvent.VK_KP_RIGHT       -> Key.KP_RIGHT
                //KeyEvent.VK_DEAD_GRAVE               -> Key.DEAD_GRAVE
                //KeyEvent.VK_DEAD_ACUTE               -> Key.DEAD_ACUTE
                //KeyEvent.VK_DEAD_CIRCUMFLEX          -> Key.DEAD_CIRCUMFLEX
                //KeyEvent.VK_DEAD_TILDE               -> Key.DEAD_TILDE
                //KeyEvent.VK_DEAD_MACRON              -> Key.DEAD_MACRON
                //KeyEvent.VK_DEAD_BREVE               -> Key.DEAD_BREVE
                //KeyEvent.VK_DEAD_ABOVEDOT            -> Key.DEAD_ABOVEDOT
                //KeyEvent.VK_DEAD_DIAERESIS           -> Key.DEAD_DIAERESIS
                //KeyEvent.VK_DEAD_ABOVERING           -> Key.DEAD_ABOVERING
                //KeyEvent.VK_DEAD_DOUBLEACUTE         -> Key.DEAD_DOUBLEACUTE
                //KeyEvent.VK_DEAD_CARON               -> Key.DEAD_CARON
                //KeyEvent.VK_DEAD_CEDILLA             -> Key.DEAD_CEDILLA
                //KeyEvent.VK_DEAD_OGONEK              -> Key.DEAD_OGONEK
                //KeyEvent.VK_DEAD_IOTA                -> Key.DEAD_IOTA
                //KeyEvent.VK_DEAD_VOICED_SOUND        -> Key.DEAD_VOICED_SOUND
                //KeyEvent.VK_DEAD_SEMIVOICED_SOUND    -> Key.DEAD_SEMIVOICED_SOUND
                //KeyEvent.VK_AMPERSAND                -> Key.AMPERSAND
                //KeyEvent.VK_ASTERISK                 -> Key.ASTERISK
                //KeyEvent.VK_QUOTEDBL                 -> Key.QUOTEDBL
                //KeyEvent.VK_LESS                     -> Key.LESS
                //KeyEvent.VK_GREATER                  -> Key.GREATER
                //KeyEvent.VK_BRACELEFT                -> Key.BRACELEFT
                //KeyEvent.VK_BRACERIGHT               -> Key.BRACERIGHT
                //KeyEvent.VK_AT                       -> Key.AT
                //KeyEvent.VK_COLON                    -> Key.COLON
                //KeyEvent.VK_CIRCUMFLEX               -> Key.CIRCUMFLEX
                //KeyEvent.VK_DOLLAR                   -> Key.DOLLAR
                //KeyEvent.VK_EURO_SIGN                -> Key.EURO_SIGN
                //KeyEvent.VK_EXCLAMATION_MARK         -> Key.EXCLAMATION_MARK
                //KeyEvent.VK_INVERTED_EXCLAMATION_MARK -> Key.INVERTED_EXCLAMATION_MARK
                //KeyEvent.VK_LEFT_PARENTHESIS         -> Key.LEFT_PARENTHESIS
                //KeyEvent.VK_NUMBER_SIGN              -> Key.NUMBER_SIGN
                //KeyEvent.VK_PLUS                     -> Key.PLUS
                //KeyEvent.VK_RIGHT_PARENTHESIS        -> Key.RIGHT_PARENTHESIS
                //KeyEvent.VK_UNDERSCORE               -> Key.UNDERSCORE
                //KeyEvent.VK_WINDOWS                  -> Key.WINDOWS
                //KeyEvent.VK_CONTEXT_MENU             -> Key.CONTEXT_MENU
                //KeyEvent.VK_FINAL                    -> Key.FINAL
                //KeyEvent.VK_CONVERT                  -> Key.CONVERT
                //KeyEvent.VK_NONCONVERT               -> Key.NONCONVERT
                //KeyEvent.VK_ACCEPT                   -> Key.ACCEPT
                //KeyEvent.VK_MODECHANGE               -> Key.MODECHANGE
                //KeyEvent.VK_KANA                     -> Key.KANA
                //KeyEvent.VK_KANJI                    -> Key.KANJI
                //KeyEvent.VK_ALPHANUMERIC             -> Key.ALPHANUMERIC
                //KeyEvent.VK_KATAKANA                 -> Key.KATAKANA
                //KeyEvent.VK_HIRAGANA                 -> Key.HIRAGANA
                //KeyEvent.VK_FULL_WIDTH               -> Key.FULL_WIDTH
                //KeyEvent.VK_HALF_WIDTH               -> Key.HALF_WIDTH
                //KeyEvent.VK_ROMAN_CHARACTERS         -> Key.ROMAN_CHARACTERS
                //KeyEvent.VK_ALL_CANDIDATES           -> Key.ALL_CANDIDATES
                //KeyEvent.VK_PREVIOUS_CANDIDATE       -> Key.PREVIOUS_CANDIDATE
                //KeyEvent.VK_CODE_INPUT               -> Key.CODE_INPUT
                //KeyEvent.VK_JAPANESE_KATAKANA        -> Key.JAPANESE_KATAKANA
                //KeyEvent.VK_JAPANESE_HIRAGANA        -> Key.JAPANESE_HIRAGANA
                //KeyEvent.VK_JAPANESE_ROMAN           -> Key.JAPANESE_ROMAN
                //KeyEvent.VK_KANA_LOCK                -> Key.KANA_LOCK
                //KeyEvent.VK_INPUT_METHOD_ON_OFF      -> Key.INPUT_METHOD_ON_OFF
                //KeyEvent.VK_CUT                      -> Key.CUT
                //KeyEvent.VK_COPY                     -> Key.COPY
                //KeyEvent.VK_PASTE                    -> Key.PASTE
                //KeyEvent.VK_UNDO                     -> Key.UNDO
                //KeyEvent.VK_AGAIN                    -> Key.AGAIN
                //KeyEvent.VK_FIND                     -> Key.FIND
                //KeyEvent.VK_PROPS                    -> Key.PROPS
                //KeyEvent.VK_STOP                     -> Key.STOP
                //KeyEvent.VK_COMPOSE                  -> Key.COMPOSE
                //KeyEvent.VK_ALT_GRAPH                -> Key.ALT_GRAPH
                //KeyEvent.VK_BEGIN                    -> Key.BEGIN
                KeyEvent.VK_UNDEFINED      -> Key.UNDEFINED
                else -> Key.UNKNOWN
            }
        })
    }

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        ag.onRender {
            dispatch(renderEvent)
        }

        val motionListener = object : MouseMotionAdapter() {
            override fun mouseMoved(e: java.awt.event.MouseEvent) = dispatchME(e, com.soywiz.korev.MouseEvent.Type.MOVE)
            override fun mouseDragged(e: java.awt.event.MouseEvent) = dispatchME(e, com.soywiz.korev.MouseEvent.Type.DRAG)
        }
        val mouseListener = object : MouseAdapter() {
            override fun mousePressed(e: java.awt.event.MouseEvent) =
                dispatchME(e, com.soywiz.korev.MouseEvent.Type.DOWN)

            override fun mouseReleased(e: java.awt.event.MouseEvent) =
                dispatchME(e, com.soywiz.korev.MouseEvent.Type.UP)

            override fun mouseMoved(e: java.awt.event.MouseEvent) = dispatchME(e, com.soywiz.korev.MouseEvent.Type.MOVE)
            override fun mouseEntered(e: java.awt.event.MouseEvent) =
                dispatchME(e, com.soywiz.korev.MouseEvent.Type.ENTER)

            override fun mouseDragged(e: java.awt.event.MouseEvent) =
                dispatchME(e, com.soywiz.korev.MouseEvent.Type.DRAG)

            override fun mouseClicked(e: java.awt.event.MouseEvent) =
                dispatchME(e, com.soywiz.korev.MouseEvent.Type.CLICK)

            override fun mouseExited(e: java.awt.event.MouseEvent) =
                dispatchME(e, com.soywiz.korev.MouseEvent.Type.EXIT)

            override fun mouseWheelMoved(e: MouseWheelEvent) {
                dispatch(mouseEvent {
                    this.scrollDeltaX = e.preciseWheelRotation
                    this.scrollDeltaY = e.preciseWheelRotation
                    this.scrollDeltaZ = e.preciseWheelRotation
                })
            }
        }

        val keyListener = object : KeyListener {
            override fun keyTyped(e: KeyEvent) = dispatchKE(e, com.soywiz.korev.KeyEvent.Type.TYPE)
            override fun keyPressed(e: KeyEvent) = dispatchKE(e, com.soywiz.korev.KeyEvent.Type.DOWN)
            override fun keyReleased(e: KeyEvent) = dispatchKE(e, com.soywiz.korev.KeyEvent.Type.UP)
        }

        // In both components
        frame.addKeyListener(keyListener)
        ag.glcanvas.addKeyListener(keyListener)

        ag.glcanvas.addMouseMotionListener(motionListener)
        ag.glcanvas.addMouseListener(mouseListener)

        frame.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                dispatchReshapeEvent(0, 0, ag.glcanvas.width, ag.glcanvas.height)
            }
        })
        launchAsap(coroutineDispatcher) {
            entry()
        }
        var lastTime = PerformanceCounter.milliseconds
        while (true) {
            val currentTime = PerformanceCounter.milliseconds
            val elapsedTime = (currentTime - lastTime).milliseconds
            coroutineDispatcher.executePending()
            ag.glcanvas.repaint()
            delay((timePerFrame - elapsedTime).milliseconds.clamp(0.0, 32.0).milliseconds)
            lastTime = currentTime
        }

    }
}

object AGFactoryAwt : AGFactory {
    override val supportsNativeFrame: Boolean = true
    override fun create(nativeControl: Any?, config: AGConfig): AG =
        AGAwt(config)
    override fun createFastWindow(title: String, width: Int, height: Int): AGWindow {
        val glp = GLProfile.getDefault()
        val caps = GLCapabilities(glp)
        val window = GLWindow.create(caps)
        window.title = title
        window.setSize(width, height)
        window.isVisible = true

        window.addGLEventListener(object : GLEventListener {
            override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) = Unit
            override fun display(drawable: GLAutoDrawable) = Unit
            override fun init(drawable: GLAutoDrawable) = Unit
            override fun dispose(drawable: GLAutoDrawable) = Unit
        })

        return object : AGWindow {
            override fun repaint() = Unit
            override val ag: AG = AGAwtNative(window)
        }
    }
}

abstract class AGAwtBase(val config: AGConfig = AGConfig(), val glDecorator: (KmlGl) -> KmlGl = { it }) : AGOpengl() {
    var glprofile = GLProfile.getDefault()
    //val glprofile = GLProfile.get( GLProfile.GL2 )
    var glcapabilities = GLCapabilities(glprofile).apply {
        if (config.antialiasHint) {
            sampleBuffers = true
            numSamples = 4
        }
        stencilBits = 8
        depthBits = 24
    }
    lateinit var ad: GLAutoDrawable
    private var _gl: KmlGl = KmlGlDummy
    override val gl: KmlGl get() = _gl
    lateinit var glThread: Thread
    override var isGlAvailable: Boolean = false
    override var devicePixelRatio: Double = 1.0

    fun setAutoDrawable(d: GLAutoDrawable) {
        glThread = Thread.currentThread()
        ad = d
        if (_gl === KmlGlDummy) {
            //gl = KmlGlCached(JvmKmlGl(d.gl as GL2))
            _gl = glDecorator(JvmKmlGl(d.gl as GL2))
        }
        isGlAvailable = true
    }

    val awtBase = this

    //val queue = Deque<(gl: GL) -> Unit>()
}

class AGAwt(config: AGConfig, glDecorator: (KmlGl) -> KmlGl = { it }) : AGAwtBase(config, glDecorator), AGContainer {
    val glcanvas = GLCanvas(glcapabilities)
    override val nativeComponent = glcanvas

    override val ag: AG = this

    override fun offscreenRendering(callback: () -> Unit) {
        if (!glcanvas.context.isCurrent) {
            glcanvas.context.makeCurrent()
            try {
                callback()
            } finally {
                glcanvas.context.release()
            }
        } else {
            callback()
        }
    }

    override fun dispose() {
        glcanvas.disposeGLEventListener(glEventListener, true)
    }

    override fun repaint() {
        glcanvas.repaint()
        //if (initialized) {
        //	onRender(this)
        //}
    }

    private val tempFloat4 = FloatArray(4)

    override fun resized(width: Int, height: Int) {
        val (scaleX, scaleY) = glcanvas.getCurrentSurfaceScale(tempFloat4)
        devicePixelRatio = (scaleX + scaleY) / 2.0
        super.resized((width * scaleX).toInt(), (height * scaleY).toInt())
    }

    val glEventListener = object : GLEventListener {
        override fun reshape(d: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {
            setAutoDrawable(d)

            //if (isJvm) resized(width, height)
        }

        var onReadyOnce = Once()

        override fun display(d: GLAutoDrawable) {
            setAutoDrawable(d)

            //while (true) {
            //	val callback = synchronized(queue) { if (queue.isNotEmpty()) queue.remove() else null } ?: break
            //	callback(gl)
            //}

            onReadyOnce {
                //println(glcanvas.chosenGLCapabilities.depthBits)
                ready()
            }
            onRender(awtBase)
            gl.flush()

            //gl.glClearColor(1f, 1f, 0f, 1f)
            //gl.glClear(GL.GL_COLOR_BUFFER_BIT)
            //d.swapBuffers()
        }

        override fun init(d: GLAutoDrawable) {
            contextVersion++
            setAutoDrawable(d)
            //println("c")
        }

        override fun dispose(d: GLAutoDrawable) {
            setAutoDrawable(d)
            //println("d")
        }
    }

    init {
        //((glcanvas as JoglNewtAwtCanvas).getNativeWindow() as JAWTWindow).setSurfaceScale(new float[] {2, 2});
        //glcanvas.nativeSurface.
        //println(glcanvas.nativeSurface.convertToPixelUnits(intArrayOf(1000)).toList())

        glcanvas.addGLEventListener(glEventListener)
    }

    //override fun readColor(bitmap: Bitmap32): Unit {
    //	checkErrors {
    //		gl.readPixels(
    //			0, 0, bitmap.width, bitmap.height,
    //			GL.GL_RGBA, GL.GL_UNSIGNED_BYTE,
    //			IntBuffer.wrap(bitmap.data)
    //		)
    //	}
    //}

    //override fun readDepth(width: Int, height: Int, out: FloatArray): Unit {
    //	val GL_DEPTH_COMPONENT = 0x1902
    //	checkErrors { gl.readPixels(0, 0, width, height, GL_DEPTH_COMPONENT, GL.GL_FLOAT, FloatBuffer.wrap(out)) }
    //}
}

class AGAwtNative(override val nativeComponent: Any, config: AGConfig = AGConfig(), glDecorator: (KmlGl) -> KmlGl = { it }) : AGAwtBase(config, glDecorator) {

}

class JvmKmlGl(val gl: GL2) : KmlGl() {
    override fun activeTexture(texture: Int): Unit = gl.glActiveTexture(texture)
    override fun attachShader(program: Int, shader: Int): Unit = gl.glAttachShader(program, shader)
    override fun bindAttribLocation(program: Int, index: Int, name: String): Unit = gl.glBindAttribLocation(program, index, name)
    override fun bindBuffer(target: Int, buffer: Int): Unit = gl.glBindBuffer(target, buffer)
    override fun bindFramebuffer(target: Int, framebuffer: Int): Unit = gl.glBindFramebuffer(target, framebuffer)
    override fun bindRenderbuffer(target: Int, renderbuffer: Int): Unit = gl.glBindRenderbuffer(target, renderbuffer)
    override fun bindTexture(target: Int, texture: Int): Unit = gl.glBindTexture(target, texture)
    override fun blendColor(red: Float, green: Float, blue: Float, alpha: Float): Unit = gl.glBlendColor(red, green, blue, alpha)
    override fun blendEquation(mode: Int): Unit = gl.glBlendEquation(mode)
    override fun blendEquationSeparate(modeRGB: Int, modeAlpha: Int): Unit = gl.glBlendEquationSeparate(modeRGB, modeAlpha)
    override fun blendFunc(sfactor: Int, dfactor: Int): Unit = gl.glBlendFunc(sfactor, dfactor)
    override fun blendFuncSeparate(sfactorRGB: Int, dfactorRGB: Int, sfactorAlpha: Int, dfactorAlpha: Int): Unit = gl.glBlendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha)
    override fun bufferData(target: Int, size: Int, data: FBuffer, usage: Int): Unit = gl.glBufferData(target, size.toLong(), data.nioBuffer, usage)
    override fun bufferSubData(target: Int, offset: Int, size: Int, data: FBuffer): Unit = gl.glBufferSubData(target, offset.toLong(), size.toLong(), data.nioBuffer)
    override fun checkFramebufferStatus(target: Int): Int = gl.glCheckFramebufferStatus(target)
    override fun clear(mask: Int): Unit = gl.glClear(mask)
    override fun clearColor(red: Float, green: Float, blue: Float, alpha: Float): Unit = gl.glClearColor(red, green, blue, alpha)
    override fun clearDepthf(d: Float): Unit = gl.glClearDepth(d.toDouble())
    override fun clearStencil(s: Int): Unit = gl.glClearStencil(s)
    override fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit = gl.glColorMask(red, green, blue, alpha)
    override fun compileShader(shader: Int): Unit = gl.glCompileShader(shader)
    override fun compressedTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, imageSize: Int, data: FBuffer): Unit = gl.glCompressedTexImage2D(target, level, internalformat, width, height, border, imageSize, data.nioBuffer)
    override fun compressedTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, imageSize: Int, data: FBuffer): Unit = gl.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, imageSize, data.nioBuffer)
    override fun copyTexImage2D(target: Int, level: Int, internalformat: Int, x: Int, y: Int, width: Int, height: Int, border: Int): Unit = gl.glCopyTexImage2D(target, level, internalformat, x, y, width, height, border)
    override fun copyTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit = gl.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height)
    override fun createProgram(): Int = gl.glCreateProgram()
    override fun createShader(type: Int): Int = gl.glCreateShader(type)
    override fun cullFace(mode: Int): Unit = gl.glCullFace(mode)
    override fun deleteBuffers(n: Int, items: FBuffer): Unit = gl.glDeleteBuffers(n, items.nioIntBuffer)
    override fun deleteFramebuffers(n: Int, items: FBuffer): Unit = gl.glDeleteFramebuffers(n, items.nioIntBuffer)
    override fun deleteProgram(program: Int): Unit = gl.glDeleteProgram(program)
    override fun deleteRenderbuffers(n: Int, items: FBuffer): Unit = gl.glDeleteRenderbuffers(n, items.nioIntBuffer)
    override fun deleteShader(shader: Int): Unit = gl.glDeleteShader(shader)
    override fun deleteTextures(n: Int, items: FBuffer): Unit = gl.glDeleteTextures(n, items.nioIntBuffer)
    override fun depthFunc(func: Int): Unit = gl.glDepthFunc(func)
    override fun depthMask(flag: Boolean): Unit = gl.glDepthMask(flag)
    override fun depthRangef(n: Float, f: Float): Unit = gl.glDepthRange(n.toDouble(), f.toDouble())
    override fun detachShader(program: Int, shader: Int): Unit = gl.glDetachShader(program, shader)
    override fun disable(cap: Int): Unit = gl.glDisable(cap)
    override fun disableVertexAttribArray(index: Int): Unit = gl.glDisableVertexAttribArray(index)
    override fun drawArrays(mode: Int, first: Int, count: Int): Unit = gl.glDrawArrays(mode, first, count)
    override fun drawElements(mode: Int, count: Int, type: Int, indices: Int): Unit = gl.glDrawElements(mode, count, type, indices.toLong())
    override fun enable(cap: Int): Unit = gl.glEnable(cap)
    override fun enableVertexAttribArray(index: Int): Unit = gl.glEnableVertexAttribArray(index)
    override fun finish(): Unit = gl.glFinish()
    override fun flush(): Unit = gl.glFlush()
    override fun framebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int): Unit = gl.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer)
    override fun framebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int): Unit = gl.glFramebufferTexture2D(target, attachment, textarget, texture, level)
    override fun frontFace(mode: Int): Unit = gl.glFrontFace(mode)
    override fun genBuffers(n: Int, buffers: FBuffer): Unit = gl.glGenBuffers(n, buffers.nioIntBuffer)
    override fun generateMipmap(target: Int): Unit = gl.glGenerateMipmap(target)
    override fun genFramebuffers(n: Int, framebuffers: FBuffer): Unit = gl.glGenFramebuffers(n, framebuffers.nioIntBuffer)
    override fun genRenderbuffers(n: Int, renderbuffers: FBuffer): Unit = gl.glGenRenderbuffers(n, renderbuffers.nioIntBuffer)
    override fun genTextures(n: Int, textures: FBuffer): Unit = gl.glGenTextures(n, textures.nioIntBuffer)
    override fun getActiveAttrib(program: Int, index: Int, bufSize: Int, length: FBuffer, size: FBuffer, type: FBuffer, name: FBuffer): Unit = gl.glGetActiveAttrib(program, index, bufSize, length.nioIntBuffer, size.nioIntBuffer, type.nioIntBuffer, name.nioBuffer)
    override fun getActiveUniform(program: Int, index: Int, bufSize: Int, length: FBuffer, size: FBuffer, type: FBuffer, name: FBuffer): Unit = gl.glGetActiveUniform(program, index, bufSize, length.nioIntBuffer, size.nioIntBuffer, type.nioIntBuffer, name.nioBuffer)
    override fun getAttachedShaders(program: Int, maxCount: Int, count: FBuffer, shaders: FBuffer): Unit = gl.glGetAttachedShaders(program, maxCount, count.nioIntBuffer, shaders.nioIntBuffer)
    override fun getAttribLocation(program: Int, name: String): Int = gl.glGetAttribLocation(program, name)
    override fun getUniformLocation(program: Int, name: String): Int = gl.glGetUniformLocation(program, name)
    override fun getBooleanv(pname: Int, data: FBuffer): Unit = gl.glGetBooleanv(pname, data.nioBuffer)
    override fun getBufferParameteriv(target: Int, pname: Int, params: FBuffer): Unit = gl.glGetBufferParameteriv(target, pname, params.nioIntBuffer)
    override fun getError(): Int = gl.glGetError()
    override fun getFloatv(pname: Int, data: FBuffer): Unit = gl.glGetFloatv(pname, data.nioFloatBuffer)
    override fun getFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: FBuffer): Unit = gl.glGetFramebufferAttachmentParameteriv(target, attachment, pname, params.nioIntBuffer)
    override fun getIntegerv(pname: Int, data: FBuffer): Unit = gl.glGetIntegerv(pname, data.nioIntBuffer)
    override fun getProgramInfoLog(program: Int, bufSize: Int, length: FBuffer, infoLog: FBuffer): Unit = gl.glGetProgramInfoLog(program, bufSize, length.nioIntBuffer, infoLog.nioBuffer)
    override fun getRenderbufferParameteriv(target: Int, pname: Int, params: FBuffer): Unit = gl.glGetRenderbufferParameteriv(target, pname, params.nioIntBuffer)
    override fun getProgramiv(program: Int, pname: Int, params: FBuffer): Unit = gl.glGetProgramiv(program, pname, params.nioIntBuffer)
    override fun getShaderiv(shader: Int, pname: Int, params: FBuffer): Unit = gl.glGetShaderiv(shader, pname, params.nioIntBuffer)
    override fun getShaderInfoLog(shader: Int, bufSize: Int, length: FBuffer, infoLog: FBuffer): Unit = gl.glGetShaderInfoLog(shader, bufSize, length.nioIntBuffer, infoLog.nioBuffer)
    override fun getShaderPrecisionFormat(shadertype: Int, precisiontype: Int, range: FBuffer, precision: FBuffer): Unit = gl.glGetShaderPrecisionFormat(shadertype, precisiontype, range.nioIntBuffer, precision.nioIntBuffer)
    override fun getShaderSource(shader: Int, bufSize: Int, length: FBuffer, source: FBuffer): Unit = gl.glGetShaderSource(shader, bufSize, length.nioIntBuffer, source.nioBuffer)
    override fun getString(name: Int): String = gl.glGetString(name)
    override fun getTexParameterfv(target: Int, pname: Int, params: FBuffer): Unit = gl.glGetTexParameterfv(target, pname, params.nioFloatBuffer)
    override fun getTexParameteriv(target: Int, pname: Int, params: FBuffer): Unit = gl.glGetTexParameteriv(target, pname, params.nioIntBuffer)
    override fun getUniformfv(program: Int, location: Int, params: FBuffer): Unit = gl.glGetUniformfv(program, location, params.nioFloatBuffer)
    override fun getUniformiv(program: Int, location: Int, params: FBuffer): Unit = gl.glGetUniformiv(program, location, params.nioIntBuffer)
    override fun getVertexAttribfv(index: Int, pname: Int, params: FBuffer): Unit = gl.glGetVertexAttribfv(index, pname, params.nioFloatBuffer)
    override fun getVertexAttribiv(index: Int, pname: Int, params: FBuffer): Unit = gl.glGetVertexAttribiv(index, pname, params.nioIntBuffer)
    override fun getVertexAttribPointerv(index: Int, pname: Int, pointer: FBuffer): Unit = gl.glGetVertexAttribiv(index, pname, pointer.nioIntBuffer)
    override fun hint(target: Int, mode: Int): Unit = gl.glHint(target, mode)
    override fun isBuffer(buffer: Int): Boolean = gl.glIsBuffer(buffer)
    override fun isEnabled(cap: Int): Boolean = gl.glIsEnabled(cap)
    override fun isFramebuffer(framebuffer: Int): Boolean = gl.glIsFramebuffer(framebuffer)
    override fun isProgram(program: Int): Boolean = gl.glIsProgram(program)
    override fun isRenderbuffer(renderbuffer: Int): Boolean = gl.glIsRenderbuffer(renderbuffer)
    override fun isShader(shader: Int): Boolean = gl.glIsShader(shader)
    override fun isTexture(texture: Int): Boolean = gl.glIsTexture(texture)
    override fun lineWidth(width: Float): Unit = gl.glLineWidth(width)
    override fun linkProgram(program: Int): Unit = gl.glLinkProgram(program)
    override fun pixelStorei(pname: Int, param: Int): Unit = gl.glPixelStorei(pname, param)
    override fun polygonOffset(factor: Float, units: Float): Unit = gl.glPolygonOffset(factor, units)
    override fun readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: FBuffer): Unit = gl.glReadPixels(x, y, width, height, format, type, pixels.nioBuffer)
    override fun releaseShaderCompiler(): Unit = gl.glReleaseShaderCompiler()
    override fun renderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int): Unit = gl.glRenderbufferStorage(target, internalformat, width, height)
    override fun sampleCoverage(value: Float, invert: Boolean): Unit = gl.glSampleCoverage(value, invert)
    override fun scissor(x: Int, y: Int, width: Int, height: Int): Unit = gl.glScissor(x, y, width, height)
    override fun shaderBinary(count: Int, shaders: FBuffer, binaryformat: Int, binary: FBuffer, length: Int): Unit = gl.glShaderBinary(count, shaders.nioIntBuffer, binaryformat, binary.nioBuffer, length)
    override fun shaderSource(shader: Int, string: String): Unit = gl.glShaderSource(shader, 1, arrayOf(string), intArrayOf(string.length), 0)
    override fun stencilFunc(func: Int, ref: Int, mask: Int): Unit = gl.glStencilFunc(func, ref, mask)
    override fun stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int): Unit = gl.glStencilFuncSeparate(face, func, ref, mask)
    override fun stencilMask(mask: Int): Unit = gl.glStencilMask(mask)
    override fun stencilMaskSeparate(face: Int, mask: Int): Unit = gl.glStencilMaskSeparate(face, mask)
    override fun stencilOp(fail: Int, zfail: Int, zpass: Int): Unit = gl.glStencilOp(fail, zfail, zpass)
    override fun stencilOpSeparate(face: Int, sfail: Int, dpfail: Int, dppass: Int): Unit = gl.glStencilOpSeparate(face, sfail, dpfail, dppass)
    override fun texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: FBuffer?): Unit = gl.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels?.nioBuffer)
    override fun texImage2D(target: Int, level: Int, internalformat: Int, format: Int, type: Int, data: NativeImage): Unit = gl.glTexImage2D(target, level, internalformat, data.width, data.height, 0, format, type, (data as AwtNativeImage).buffer)
    override fun texParameterf(target: Int, pname: Int, param: Float): Unit = gl.glTexParameterf(target, pname, param)
    override fun texParameterfv(target: Int, pname: Int, params: FBuffer): Unit = gl.glTexParameterfv(target, pname, params.nioFloatBuffer)
    override fun texParameteri(target: Int, pname: Int, param: Int): Unit = gl.glTexParameteri(target, pname, param)
    override fun texParameteriv(target: Int, pname: Int, params: FBuffer): Unit = gl.glTexParameteriv(target, pname, params.nioIntBuffer)
    override fun texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, type: Int, pixels: FBuffer): Unit = gl.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels.nioBuffer)
    override fun uniform1f(location: Int, v0: Float): Unit = gl.glUniform1f(location, v0)
    override fun uniform1fv(location: Int, count: Int, value: FBuffer): Unit = gl.glUniform1fv(location, count, value.nioFloatBuffer)
    override fun uniform1i(location: Int, v0: Int): Unit = gl.glUniform1i(location, v0)
    override fun uniform1iv(location: Int, count: Int, value: FBuffer): Unit = gl.glUniform1iv(location, count, value.nioIntBuffer)
    override fun uniform2f(location: Int, v0: Float, v1: Float): Unit = gl.glUniform2f(location, v0, v1)
    override fun uniform2fv(location: Int, count: Int, value: FBuffer): Unit = gl.glUniform2fv(location, count, value.nioFloatBuffer)
    override fun uniform2i(location: Int, v0: Int, v1: Int): Unit = gl.glUniform2i(location, v0, v1)
    override fun uniform2iv(location: Int, count: Int, value: FBuffer): Unit = gl.glUniform2iv(location, count, value.nioIntBuffer)
    override fun uniform3f(location: Int, v0: Float, v1: Float, v2: Float): Unit = gl.glUniform3f(location, v0, v1, v2)
    override fun uniform3fv(location: Int, count: Int, value: FBuffer): Unit = gl.glUniform3fv(location, count, value.nioFloatBuffer)
    override fun uniform3i(location: Int, v0: Int, v1: Int, v2: Int): Unit = gl.glUniform3i(location, v0, v1, v2)
    override fun uniform3iv(location: Int, count: Int, value: FBuffer): Unit = gl.glUniform3iv(location, count, value.nioIntBuffer)
    override fun uniform4f(location: Int, v0: Float, v1: Float, v2: Float, v3: Float): Unit = gl.glUniform4f(location, v0, v1, v2, v3)
    override fun uniform4fv(location: Int, count: Int, value: FBuffer): Unit = gl.glUniform4fv(location, count, value.nioFloatBuffer)
    override fun uniform4i(location: Int, v0: Int, v1: Int, v2: Int, v3: Int): Unit = gl.glUniform4i(location, v0, v1, v2, v3)
    override fun uniform4iv(location: Int, count: Int, value: FBuffer): Unit = gl.glUniform4iv(location, count, value.nioIntBuffer)
    override fun uniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FBuffer): Unit = gl.glUniformMatrix2fv(location, count, transpose, value.nioFloatBuffer)
    override fun uniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FBuffer): Unit = gl.glUniformMatrix3fv(location, count, transpose, value.nioFloatBuffer)
    override fun uniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FBuffer): Unit = gl.glUniformMatrix4fv(location, count, transpose, value.nioFloatBuffer)
    override fun useProgram(program: Int): Unit = gl.glUseProgram(program)
    override fun validateProgram(program: Int): Unit = gl.glValidateProgram(program)
    override fun vertexAttrib1f(index: Int, x: Float): Unit = gl.glVertexAttrib1f(index, x)
    override fun vertexAttrib1fv(index: Int, v: FBuffer): Unit = gl.glVertexAttrib1fv(index, v.nioFloatBuffer)
    override fun vertexAttrib2f(index: Int, x: Float, y: Float): Unit = gl.glVertexAttrib2f(index, x, y)
    override fun vertexAttrib2fv(index: Int, v: FBuffer): Unit = gl.glVertexAttrib2fv(index, v.nioFloatBuffer)
    override fun vertexAttrib3f(index: Int, x: Float, y: Float, z: Float): Unit = gl.glVertexAttrib3f(index, x, y, z)
    override fun vertexAttrib3fv(index: Int, v: FBuffer): Unit = gl.glVertexAttrib3fv(index, v.nioFloatBuffer)
    override fun vertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float): Unit = gl.glVertexAttrib4f(index, x, y, z, w)
    override fun vertexAttrib4fv(index: Int, v: FBuffer): Unit = gl.glVertexAttrib4fv(index, v.nioFloatBuffer)
    override fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, pointer: Int): Unit = gl.glVertexAttribPointer(index, size, type, normalized, stride, pointer.toLong())
    override fun viewport(x: Int, y: Int, width: Int, height: Int): Unit = gl.glViewport(x, y, width, height)
}
*/
