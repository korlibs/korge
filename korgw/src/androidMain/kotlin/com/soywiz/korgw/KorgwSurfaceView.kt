package com.soywiz.korgw

import android.content.Context
import android.content.pm.FeatureInfo
import android.opengl.EGL14.EGL_ALPHA_SIZE
import android.opengl.EGL14.EGL_BIND_TO_TEXTURE_RGB
import android.opengl.EGL14.EGL_BIND_TO_TEXTURE_RGBA
import android.opengl.EGL14.EGL_BLUE_SIZE
import android.opengl.EGL14.EGL_CONFIG_CAVEAT
import android.opengl.EGL14.EGL_CONFIG_ID
import android.opengl.EGL14.EGL_CONTEXT_CLIENT_VERSION
import android.opengl.EGL14.EGL_DEPTH_SIZE
import android.opengl.EGL14.EGL_GREEN_SIZE
import android.opengl.EGL14.EGL_LEVEL
import android.opengl.EGL14.EGL_MAX_PBUFFER_HEIGHT
import android.opengl.EGL14.EGL_MAX_PBUFFER_WIDTH
import android.opengl.EGL14.EGL_MAX_SWAP_INTERVAL
import android.opengl.EGL14.EGL_MIN_SWAP_INTERVAL
import android.opengl.EGL14.EGL_RED_SIZE
import android.opengl.EGL14.EGL_RENDERABLE_TYPE
import android.opengl.EGL14.EGL_STENCIL_SIZE
import android.opengl.EGL14.EGL_SURFACE_TYPE
import android.opengl.EGL14.eglGetCurrentContext
import android.opengl.EGL14.eglGetCurrentDisplay
import android.opengl.EGL14.eglQueryContext
import android.opengl.GLSurfaceView
import android.os.Build
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.soywiz.kds.IntMap
import com.soywiz.kds.buildIntArray
import com.soywiz.kds.toIntArrayList
import com.soywiz.kds.toMap
import com.soywiz.klock.PerformanceCounter
import com.soywiz.klock.TimeSpan
import com.soywiz.kmem.hasBits
import com.soywiz.kmem.setBits
import com.soywiz.korev.GameButton
import com.soywiz.korev.GamePadConnectionEvent
import com.soywiz.korev.GamepadInfo
import com.soywiz.korev.Key
import com.soywiz.korev.StandardGamepadMapping
import com.soywiz.korev.Touch
import com.soywiz.korev.TouchEvent
import com.soywiz.korio.async.Signal
import com.soywiz.korma.geom.Point
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.opengles.GL10
import kotlin.math.absoluteValue

// https://github.com/aosp-mirror/platform_frameworks_base/blob/e4df5d375df945b0f53a9c7cca83d37970b7ce64/opengl/java/android/opengl/GLSurfaceView.java
open class KorgwSurfaceView constructor(
    val viewOrActivity: Any?,
    context: Context,
    val gameWindow: BaseAndroidGameWindow,
) : GLSurfaceView(context) {
    val view = this

    val onDraw = Signal<Unit>()
    val requestedClientVersion by lazy { getVersionFromPackageManager(context) }
    var clientVersion = -1

    init {
        println("KorgwActivity: Created GLSurfaceView $this for ${viewOrActivity}")

        println("OpenGL ES Version (requested): $requestedClientVersion")
        setEGLContextClientVersion(getVersionFromPackageManager(context))
        setEGLConfigChooser(AndroidConfigChooser(hdr = gameWindow.config.hdr))
        setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
                //GLES20.glClearColor(0.0f, 0.4f, 0.7f, 1.0f)
                gameWindow.handleContextLost()
                clientVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    val out = IntArray(1)
                    eglQueryContext(eglGetCurrentDisplay(), eglGetCurrentContext(), EGL_CONTEXT_CLIENT_VERSION, out, 0)
                    out[0]
                } else {
                    2
                }
                println("OpenGL ES Version (actual): $clientVersion")
            }

            override fun onDrawFrame(unused: GL10) {
                val frameStartTime = PerformanceCounter.reference
                gameWindow.handleInitEventIfRequired()
                gameWindow.handleReshapeEventIfRequired(0, 0, view.width, view.height)
                try {
                    val gamepads = InputDevice.getDeviceIds().map { InputDevice.getDevice(it) }
                        .filter { it.sources.hasBits(InputDevice.SOURCE_GAMEPAD) }.sortedBy { it.id }

                    if (gamepads.isNotEmpty() || activeGamepads.size != 0) {
                        val currentGamePadIds = gamepads.map { it.id }.toSet()
                        activeGamepads.toMap().forEach { (deviceId, value) ->
                            if (deviceId !in currentGamePadIds) {
                                activeGamepads.remove(deviceId)
                                //gameWindow.dispatchGamepadConnectionEvent(GamePadConnectionEvent.Type.DISCONNECTED, -1)
                            }
                        }
                        gameWindow.dispatchGamepadUpdateStart()
                        val l = Point()
                        val r = Point()
                        for ((index, gamepad) in gamepads.withIndex()) {
                            val info = getGamepadInfo(gamepad.id)
                            if (!info.connected) {
                                info.connected = true
                                gameWindow.dispatchGamepadConnectionEvent(GamePadConnectionEvent.Type.CONNECTED, index)
                            }
                            l.setTo(info.rawAxes[0], info.rawAxes[1])
                            r.setTo(info.rawAxes[2], info.rawAxes[3])
                            gameWindow.dispatchGamepadUpdateAdd(l, r, info.rawButtonsPressed, StandardGamepadMapping, gamepad.name, 1.0)
                            //println("gamepad=$gamepad")
                        }
                        gameWindow.dispatchGamepadUpdateEnd()
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                gameWindow.frame(frameStartTime = frameStartTime)
                onDraw(Unit)
            }

            override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
                println("---------------- GLSurfaceView.onSurfaceChanged($width, $height) --------------")
                //ag.contextVersion++
                //GLES20.glViewport(0, 0, width, height)
                //surfaceChanged = true
            }
        })
    }

    val activeGamepads = IntMap<GamepadInfo>()
    fun getGamepadInfo(deviceId: Int): GamepadInfo = activeGamepads.getOrPut(deviceId) { GamepadInfo() }

    private val touches = TouchEventHandler()
    private val coords = MotionEvent.PointerCoords()

    fun onKey(keyCode: Int, event: KeyEvent, type: com.soywiz.korev.KeyEvent.Type, long: Boolean): Boolean {
        val char = keyCode.toChar()
        val key = AndroidKeyMap.KEY_MAP[keyCode] ?: Key.UNKNOWN

        //if (event.source.hasBits(InputDevice.SOURCE_GAMEPAD)) {
        if (event.device?.sources?.hasBits(InputDevice.SOURCE_GAMEPAD) == true) {
            //println("GAMEPAD: $key")
            val info = getGamepadInfo(event.deviceId)
            val press = type == com.soywiz.korev.KeyEvent.Type.DOWN
            val button = when (key) {
                Key.LEFT -> GameButton.LEFT
                Key.RIGHT -> GameButton.RIGHT
                Key.UP -> GameButton.UP
                Key.DOWN -> GameButton.DOWN
                Key.XBUTTON_L1 -> GameButton.L1
                Key.XBUTTON_L2 -> GameButton.L2
                Key.XBUTTON_THUMBL -> GameButton.L3
                Key.XBUTTON_R1 -> GameButton.R1
                Key.XBUTTON_R2 -> GameButton.R2
                Key.XBUTTON_THUMBR -> GameButton.R3
                Key.XBUTTON_A -> GameButton.BUTTON0
                Key.XBUTTON_B -> GameButton.BUTTON1
                Key.XBUTTON_X -> GameButton.BUTTON2
                Key.XBUTTON_Y -> GameButton.BUTTON3
                Key.XBUTTON_SELECT -> GameButton.SELECT
                Key.XBUTTON_START -> GameButton.START
                Key.XBUTTON_MODE -> GameButton.SYSTEM
                Key.MEDIA_RECORD -> GameButton.RECORD
                else -> {
                    println(" - UNHANDLED GAMEPAD KEY: $key (keyCode=$keyCode)")
                    null
                }
            }
            if (button != null) {
                info.rawButtonsPressed = info.rawButtonsPressed.setBits(button.bitMask, press)
                return true
            }
        }

        //println("type=$type, keyCode=$keyCode, char=$char, key=$key, long=$long, unicodeChar=${event.unicodeChar}, event.keyCode=${event.keyCode}")
        //println("onKey[$type]: $event, keyboardType=${event.device.keyboardType}, sources=${event.device.sources}")


        //if (event.source.hasBits(InputDevice.SOURCE_GAMEPAD)) {
        //}
        //println(InputDevice.SOURCE)
        gameWindow.queue {
            gameWindow.dispatchKeyEventEx(
                type, 0, char, key, keyCode,
                shift = event.isShiftPressed,
                ctrl = event.isCtrlPressed,
                alt = event.isAltPressed,
                meta = event.isMetaPressed,
            )
        }
        return true
    }

    private fun Double.withoutDeadRange(): Double {
        // @TODO: Should we query for the right value?
        if (this.absoluteValue < 0.09) return 0.0
        return this
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.device.sources.hasBits(InputDevice.SOURCE_GAMEPAD)) {
            val info = getGamepadInfo(event.deviceId)
            info.rawAxes[0] = event.getAxisValue(MotionEvent.AXIS_X).toDouble().withoutDeadRange()
            info.rawAxes[1] = event.getAxisValue(MotionEvent.AXIS_Y).toDouble().withoutDeadRange()
            info.rawAxes[2] = event.getAxisValue(MotionEvent.AXIS_RX).toDouble().withoutDeadRange()
            info.rawAxes[3] = event.getAxisValue(MotionEvent.AXIS_RY).toDouble().withoutDeadRange()
            info.rawAxes[4] = event.getAxisValue(MotionEvent.AXIS_LTRIGGER).toDouble().withoutDeadRange()
            info.rawAxes[5] = event.getAxisValue(MotionEvent.AXIS_RTRIGGER).toDouble().withoutDeadRange()
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        //println("KorgwSurfaceView.onKeyDown")
        return onKey(keyCode, event, type = com.soywiz.korev.KeyEvent.Type.DOWN, long = false)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        //println("KorgwSurfaceView.onKeyLongPress")
        return onKey(keyCode, event, type = com.soywiz.korev.KeyEvent.Type.DOWN, long = true)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        //println("KorgwSurfaceView.onKeyUp")
        onKey(keyCode, event, type = com.soywiz.korev.KeyEvent.Type.UP, long = false)
        val unicodeChar = event.unicodeChar
        if (unicodeChar != 0) {
            onKey(unicodeChar, event, type = com.soywiz.korev.KeyEvent.Type.TYPE, long = false)
        }
        return true
    }

    override fun onKeyMultiple(keyCode: Int, repeatCount: Int, event: KeyEvent): Boolean {
        //println("Android.onKeyMultiple:$keyCode,$repeatCount,${event.unicodeChar},$event")
        for (char in event.characters) {
            onKey(char.toInt(), event, type = com.soywiz.korev.KeyEvent.Type.TYPE, long = false)
        }
        return true
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val gameWindow = gameWindow

        val actionMasked = ev.actionMasked
        val actionPointerIndex = ev.actionIndex

        if (ev.action != MotionEvent.ACTION_MOVE) {
            //println("[${DateTime.nowUnixLong()}]onTouchEvent: ${ev.action}, ${MotionEvent.actionToString(ev.action)}, actionMasked=$actionMasked, actionPointerId=$actionPointerId, ev.pointerCount=${ev.pointerCount}")
        }

        val type = when (actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> TouchEvent.Type.START
            MotionEvent.ACTION_HOVER_MOVE -> TouchEvent.Type.HOVER
            MotionEvent.ACTION_MOVE -> TouchEvent.Type.MOVE
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> TouchEvent.Type.END
            else -> TouchEvent.Type.UNKNOWN
        }

        touches.handleEvent(gameWindow, type) { currentTouchEvent ->
            for (n in 0 until ev.pointerCount) {
                ev.getPointerCoords(n, coords)
                val id = ev.getPointerId(n)
                val status = when {
                    type == TouchEvent.Type.START && actionPointerIndex == n -> Touch.Status.ADD
                    type == TouchEvent.Type.END && actionPointerIndex == n -> Touch.Status.REMOVE
                    else -> Touch.Status.KEEP
                }
                currentTouchEvent.touch(id, coords.x.toDouble(), coords.y.toDouble(), status)
            }
        }
        return true
    }
}

private fun getVersionFromPackageManager(context: Context): Int {
    var version = 1
    val packageManager = context.packageManager
    val featureInfos = packageManager.systemAvailableFeatures
    if (featureInfos.isNotEmpty())
        for (featureInfo in featureInfos)
        // Null feature name means this feature is the open gl es version feature.
            if (featureInfo.name == null && featureInfo.reqGlEsVersion != FeatureInfo.GL_ES_VERSION_UNDEFINED)
                version = (featureInfo.reqGlEsVersion ushr 16) and 0xFF

    return version
}

// https://kotlinlang.slack.com/archives/CJEF0LB6Y/p1630391858001400
class AndroidConfigChooser(
    val hdr: Boolean? = null
) : GLSurfaceView.EGLConfigChooser {
    /**
     * Gets called by the GLSurfaceView class to return the best config
     */
    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig? {
        //val configs = getAllConfigs(egl, display)
        for (attribs in sequence {
            if (hdr == true) {
                yield(createAttribList(red = 16, green = 16, blue = 16, depth = 24, stencil = 8, gles3 = true))
            }
            yield(createAttribList(red = 8, green = 8, blue = 8, alpha = 8, depth = 24, stencil = 8, gles3 = true))
            yield(createAttribList(red = 8, green = 8, blue = 8, alpha = 8, depth = 16, stencil = 8, gles3 = true))
            yield(createAttribList(red = 8, green = 8, blue = 8, alpha = 8, depth = 24, stencil = 8, gles2 = true))
            yield(createAttribList(red = 8, green = 8, blue = 8, alpha = 8, depth = 16, stencil = 8, gles2 = true))
            yield(createAttribList(stencil = 8, gles3 = true))
            yield(createAttribList(stencil = 8, gles2 = true))
            yield(createAttribList(gles3 = true))
            yield(createAttribList(gles2 = true))
            yield(createAttribList())
        }) {
            val configs = getConfigs(egl, display, attribs)
            if (configs.isNotEmpty()) {
                println("AndroidConfigChooser.chooseConfig=${configs.size} : attribs=${attribs.toIntArrayList()}")
                for ((index, config) in configs.withIndex()) {
                    val mark = if (index == 0) " [Chosen]" else ""
                    println(" - [$index] $config$mark")
                }
                return configs.first().config
            }
        }

        return null
    }

    private fun getConfigs(egl: EGL10, display: EGLDisplay, attribs: IntArray): List<EGLFullConfig> = try {
        val numConfigs = IntArray(1)
        egl.eglChooseConfig(display, attribs, null, 0, numConfigs)
        val configs = arrayOfNulls<EGLConfig>(numConfigs[0])
        egl.eglChooseConfig(display, attribs, configs, numConfigs[0], null)
        configs.filterNotNull().map { EGLFullConfig(egl, display, it) }
    } catch (e: Throwable) {
        e.printStackTrace()
        emptyList()
    }

    private fun createAttribList(
        red: Int? = null,
        green: Int? = null,
        blue: Int? = null,
        alpha: Int? = null,
        depth: Int? = null,
        stencil: Int? = null,
        gles2: Boolean? = null,
        gles3: Boolean? = null,
    ): IntArray = buildIntArray {
        when {
            gles3 != null -> add(EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT)
            gles2 != null -> add(EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT)
        }
        if (red != null) add(EGL10.EGL_RED_SIZE, red)
        if (green != null) add(EGL10.EGL_GREEN_SIZE, green)
        if (blue != null) add(EGL10.EGL_BLUE_SIZE, blue)
        if (alpha != null) add(EGL10.EGL_ALPHA_SIZE, alpha)
        if (depth != null) add(EGL10.EGL_DEPTH_SIZE, depth)
        if (stencil != null) add(EGL10.EGL_STENCIL_SIZE, stencil)
        add(EGL10.EGL_NONE)
    }

    private fun getAllConfigs(egl: EGL10, display: EGLDisplay): List<EGLFullConfig> {
        return getConfigs(egl, display, createAttribList())
        //val numConfigs = IntArray(1)
        //egl.eglGetConfigs(display, null, 0, numConfigs)
        //val configs = arrayOfNulls<EGLConfig>(numConfigs[0])
        //egl.eglGetConfigs(display, configs, numConfigs[0], null)
        //return configs.filterNotNull().map { EGLFullConfig(egl, display, it) }
    }

    class EGLFullConfig(
        val egl: EGL10,
        val display: EGLDisplay,
        val config: EGLConfig?,
    ) {
        val red: Int by lazy { eglGetConfigAttribSafe(EGL_RED_SIZE) }
        val green: Int by lazy { eglGetConfigAttribSafe(EGL_GREEN_SIZE) }
        val blue: Int by lazy { eglGetConfigAttribSafe(EGL_BLUE_SIZE) }
        val alpha: Int by lazy { eglGetConfigAttribSafe(EGL_ALPHA_SIZE) }
        val depth: Int by lazy { eglGetConfigAttribSafe(EGL_DEPTH_SIZE) }
        val stencil: Int by lazy { eglGetConfigAttribSafe(EGL_STENCIL_SIZE) }
        val renderableType: Int by lazy { eglGetConfigAttribSafe(EGL_RENDERABLE_TYPE) }
        val caveat: Int by lazy { eglGetConfigAttribSafe(EGL_CONFIG_CAVEAT) }
        val configId: Int by lazy { eglGetConfigAttribSafe(EGL_CONFIG_ID) }
        val maxWidth: Int by lazy { eglGetConfigAttribSafe(EGL_MAX_PBUFFER_WIDTH) }
        val maxHeight: Int by lazy { eglGetConfigAttribSafe(EGL_MAX_PBUFFER_HEIGHT) }
        val minSwapInterval: Int by lazy { eglGetConfigAttribSafe(EGL_MIN_SWAP_INTERVAL) }
        val maxSwapInterval: Int by lazy { eglGetConfigAttribSafe(EGL_MAX_SWAP_INTERVAL) }
        val bindRgb: Int by lazy { eglGetConfigAttribSafe(EGL_BIND_TO_TEXTURE_RGB) }
        val bindRgba: Int by lazy { eglGetConfigAttribSafe(EGL_BIND_TO_TEXTURE_RGBA) }
        val level: Int by lazy { eglGetConfigAttribSafe(EGL_LEVEL) }
        val surfaceType: Int by lazy { eglGetConfigAttribSafe(EGL_SURFACE_TYPE) }

        val gles2: Boolean by lazy { renderableType.hasBits(EGL_OPENGL_ES2_BIT) }
        val gles3: Boolean by lazy { renderableType.hasBits(EGL_OPENGL_ES3_BIT) }

        private fun eglGetConfigAttribSafe(attribute: Int): Int {
            val value = IntArray(1)
            if (!egl.eglGetConfigAttrib(display, config, attribute, value)) throw AssertionError()
            return value[0]
        }

        override fun toString(): String = "EGLFullConfig[$configId]($red, $green, $blue, $alpha, depth=$depth, stencil=$stencil, renderableType=$renderableType, gles2=$gles2, gles3=$gles3, caveat=$caveat, maxWidth=$maxWidth, maxHeight=$maxHeight, swapInternval=$minSwapInterval-$maxSwapInterval, level=$level, bindRgb/a=$bindRgb,$bindRgba, surfaceType=$surfaceType)"
    }

    companion object {
        private const val EGL_OPENGL_ES2_BIT = 0x04
        private const val EGL_OPENGL_ES3_BIT = 0x40
    }
}
