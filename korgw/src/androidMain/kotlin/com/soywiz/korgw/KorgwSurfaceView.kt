package com.soywiz.korgw

import android.content.Context
import android.content.pm.FeatureInfo
import android.opengl.EGL14.*
import android.opengl.EGL14.eglGetCurrentContext
import android.opengl.EGL14.eglGetCurrentDisplay
import android.opengl.EGL14.eglQueryContext
import android.opengl.GLSurfaceView
import android.os.Build
import android.view.Display
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import com.soywiz.kds.IntMap
import com.soywiz.kds.buildIntArray
import com.soywiz.kds.lock.NonRecursiveLock
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
import java.util.*
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.opengles.GL10
import kotlin.concurrent.thread
import kotlin.math.absoluteValue

// https://github.com/aosp-mirror/platform_frameworks_base/blob/e4df5d375df945b0f53a9c7cca83d37970b7ce64/opengl/java/android/opengl/GLSurfaceView.java
open class KorgwSurfaceView constructor(
    val viewOrActivity: Any?,
    context: Context,
    val gameWindow: BaseAndroidGameWindow,
    val config: GameWindowCreationConfig = gameWindow.config,
) : GLSurfaceView(context), GLSurfaceView.Renderer {
    val view = this

    val onDraw = Signal<Unit>()
    val requestedClientVersion by lazy { getVersionFromPackageManager(context) }
    var clientVersion = -1
    var continuousRenderMode: Boolean
        get() = renderMode == RENDERMODE_CONTINUOUSLY
        set(value) {
            renderMode = if (value) RENDERMODE_CONTINUOUSLY else RENDERMODE_WHEN_DIRTY
        }

    init {
        println("KorgwActivity: Created GLSurfaceView $this for ${viewOrActivity}")

        println("OpenGL ES Version (requested): $requestedClientVersion, config=$config")
        setEGLContextClientVersion(getVersionFromPackageManager(context))
        setEGLConfigChooser(AndroidConfigChooser(config))
        setRenderer(this)
        //renderMode = RENDERMODE_WHEN_DIRTY
    }

    var firstRender = false
    private val renderLock = NonRecursiveLock()

    var updateTimerThread: Thread? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val display: Display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).getDefaultDisplay()
        val refreshRating: Float = display.refreshRate

        updateTimerThread = thread(start = true, isDaemon = true, name = "korgw-updater") {
            try {
                while (true) {
                    val startTime = System.currentTimeMillis()
                    try {
                        if (!firstRender) {
                            requestRender()
                        } else {
                            //queueEvent {
                            renderLock {
                                //println("onAttachedToWindow.timer: continuousRenderMode=$continuousRenderMode")
                                if (!continuousRenderMode) {
                                    val frameStartTime = runPreFrame()
                                    try {
                                        gameWindow.frame(
                                            frameStartTime = frameStartTime,
                                            doUpdate = true,
                                            doRender = false
                                        )
                                        //println("     --> gameWindow.mustTriggerRender=${gameWindow.mustTriggerRender}")
                                        if (gameWindow.mustTriggerRender) {
                                            requestRender()
                                        }
                                    } catch (e: Throwable) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                        //}
                    } finally {
                        val endTime = System.currentTimeMillis()
                        val elapsedTime = (endTime - startTime).toInt()
                        // @TODO: Ideally this shouldn't be a timer, but a vsync-based callback, or at least use screen's hz)
                        Thread.sleep(maxOf(4L, (1000L / refreshRating).toLong() - elapsedTime))
                    }
                }
            } catch (e: InterruptedException) {
                // Do nothing, just finish the loop
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        updateTimerThread?.interrupt()
        updateTimerThread = null
    }

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

    private fun runPreFrame(): TimeSpan {
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
        return frameStartTime
    }

    override fun onDrawFrame(unused: GL10) {
        renderLock {
            try {
                val frameStartTime = runPreFrame()
                try {
                    if (!continuousRenderMode) {
                        gameWindow.updatedSinceFrame++
                    }
                    gameWindow.frame(frameStartTime = frameStartTime, doUpdate = continuousRenderMode, doRender = true)
                    onDraw(Unit)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            } finally {
                firstRender = true
            }
        }
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        println("---------------- GLSurfaceView.onSurfaceChanged($width, $height) --------------")
        //ag.contextVersion++
        //GLES20.glViewport(0, 0, width, height)
        //surfaceChanged = true
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

        when (keyCode) {
            // Not handled. Let the device turn up/down the volume
            KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_MUTE -> {
                return false
            }
            // Mark as not handled, just in case the OS wants to do something with this
            else -> {
                //return true
                return false
            }
        }
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
    val config: GameWindowCreationConfig,
) : GLSurfaceView.EGLConfigChooser {
    /**
     * Gets called by the GLSurfaceView class to return the best config
     */
    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig? {
        //val configs = getAllConfigs(egl, display)
        for (rconfig in sequence {
            if (config.hdr == true) {
                yield(GLRequestConfig(red = 16, green = 16, blue = 16, depth = 24, stencil = 8, gles3 = true, msaa = config.msaa))
                yield(GLRequestConfig(red = 16, green = 16, blue = 16, depth = 24, stencil = 8, gles3 = true, msaa = null))
            }
            for (msaa in listOf(config.msaa, null)) {
                for (gles3 in listOf(true, true)) {
                    for (depth in listOf(24, 16)) {
                        yield(GLRequestConfig(red = 8, green = 8, blue = 8, alpha = 8, depth = depth, stencil = 8, gles3 = gles3, gles2 = !gles3, msaa = msaa))
                    }
                }
            }
            yield(GLRequestConfig(stencil = 8, gles3 = true))
            yield(GLRequestConfig(stencil = 8, gles2 = true))
            yield(GLRequestConfig(gles3 = true))
            yield(GLRequestConfig(gles2 = true))
            yield(GLRequestConfig())
        }) {
            val attribs = rconfig.createAttribList()
            val configsWithScore = getConfigs(egl, display, attribs).map { it to rconfig.matchScore(it) }
            if (configsWithScore.isNotEmpty()) {
                println("AndroidConfigChooser.chooseConfig=${configsWithScore.size} : config.msaa=${config.msaa}, config=$rconfig")
                for ((index, configWithScore) in configsWithScore.withIndex()) {
                    val config = configWithScore.first
                    val score = configWithScore.second
                    val mark = if (index == 0) " [Chosen]" else ""
                    println(" - [$index] [score=$score] $config$mark")
                }
                return configsWithScore.first().first.config
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

    data class GLRequestConfig(
        val red: Int? = null,
        val green: Int? = null,
        val blue: Int? = null,
        val alpha: Int? = null,
        val depth: Int? = null,
        val stencil: Int? = null,
        val gles2: Boolean? = null,
        val gles3: Boolean? = null,
        val msaa: Int? = null,
    ) {
        fun matchScore(config: EGLFullConfig): Double {
            var score = 0.0
            if (config.depth == depth) score += 10.0
            if (config.stencil == stencil) score += 5.0
            if (config.samples == msaa) score += 3.0
            return score
        }

        fun createAttribList(): IntArray = buildIntArray {
            add(EGL10.EGL_LEVEL, 0)
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
            if (msaa != null) {
                add(EGL10.EGL_COLOR_BUFFER_TYPE, EGL10.EGL_RGB_BUFFER)
                add(EGL10.EGL_SAMPLE_BUFFERS, 1)
                add(EGL10.EGL_SAMPLES, msaa)
            }
            add(EGL10.EGL_NONE)
        }
    }

    private fun getAllConfigs(egl: EGL10, display: EGLDisplay): List<EGLFullConfig> {
        return getConfigs(egl, display, GLRequestConfig().createAttribList())
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
        val samples: Int by lazy { eglGetConfigAttribSafe(EGL_SAMPLES) }

        val gles2: Boolean by lazy { renderableType.hasBits(EGL_OPENGL_ES2_BIT) }
        val gles3: Boolean by lazy { renderableType.hasBits(EGL_OPENGL_ES3_BIT) }

        val surfaceTypeMultisampleResolveBoxBit get() = surfaceType.hasBits(EGL_MULTISAMPLE_RESOLVE_BOX_BIT)
        val surfaceTypePBufferBit get() = surfaceType.hasBits(EGL_PBUFFER_BIT)
        val surfaceTypePixmapBit get() = surfaceType.hasBits(EGL_PIXMAP_BIT)
        val surfaceTypeSwapBehaviourPreservedBit get() = surfaceType.hasBits(EGL_SWAP_BEHAVIOR_PRESERVED_BIT)
        val surfaceTypeVgAlphaFormatPre8Bit get() = surfaceType.hasBits(EGL_VG_ALPHA_FORMAT_PRE_BIT)
        val surfaceTypeVgColorSpaceLinearBit get() = surfaceType.hasBits(EGL_VG_COLORSPACE_LINEAR_BIT)
        val surfaceTypeWindowBit get() = surfaceType.hasBits(EGL_WINDOW_BIT)

        fun surfaceTypeString(): String = buildList {
            if (surfaceTypeMultisampleResolveBoxBit) add("MULTISAMPLE")
            if (surfaceTypePBufferBit) add("PBUFFER")
            if (surfaceTypePixmapBit) add("PIXMAP")
            if (surfaceTypeSwapBehaviourPreservedBit) add("SWAP")
            if (surfaceTypeVgAlphaFormatPre8Bit) add("VGALPHA")
            if (surfaceTypeVgColorSpaceLinearBit) add("VGCOLOR")
            if (surfaceTypeWindowBit) add("WINDOW")
        }.joinToString("-")

        private fun eglGetConfigAttribSafe(attribute: Int): Int {

            val value = IntArray(1)
            if (!egl.eglGetConfigAttrib(display, config, attribute, value)) throw AssertionError()
            return value[0]
        }

        override fun toString(): String = "EGLFullConfig[$configId]($red, $green, $blue, $alpha, depth=$depth, stencil=$stencil, renderableType=$renderableType, gles2=$gles2, gles3=$gles3, caveat=$caveat, maxWidth=$maxWidth, maxHeight=$maxHeight, swapInterval=$minSwapInterval-$maxSwapInterval, level=$level, bindRgb/a=$bindRgb,$bindRgba, surfaceType=$surfaceType(${surfaceTypeString()}), samples=$samples)"
    }

    companion object {
        private const val EGL_OPENGL_ES2_BIT = 0x04
        private const val EGL_OPENGL_ES3_BIT = 0x40
    }
}
