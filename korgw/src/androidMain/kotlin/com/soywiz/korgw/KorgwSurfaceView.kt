package com.soywiz.korgw

import android.content.Context
import android.content.pm.FeatureInfo
import android.opengl.EGL14.*
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.soywiz.korev.Touch
import com.soywiz.korev.TouchEvent
import com.soywiz.korio.async.Signal
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.opengles.GL10

// https://github.com/aosp-mirror/platform_frameworks_base/blob/e4df5d375df945b0f53a9c7cca83d37970b7ce64/opengl/java/android/opengl/GLSurfaceView.java
class KorgwSurfaceView(val viewOrActivity: Any?, context: Context, val gameWindow: BaseAndroidGameWindow) : GLSurfaceView(context) {
    val view = this

    val onDraw = Signal<Unit>()
    val requestedClientVersion by lazy { getVersionFromPackageManager(context) }
    var clientVersion = -1

    init {
        println("KorgwActivity: Created GLSurfaceView $this for ${viewOrActivity}")

        println("OpenGL ES Version (requested): $requestedClientVersion")
        setEGLContextClientVersion(getVersionFromPackageManager(context))
        setEGLConfigChooser(AndroidConfigChooser(EGLConfigSettings()))
        setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
                //GLES20.glClearColor(0.0f, 0.4f, 0.7f, 1.0f)
                gameWindow.handleContextLost()
                val out = IntArray(1)
                eglQueryContext(eglGetCurrentDisplay(), eglGetCurrentContext(), EGL_CONTEXT_CLIENT_VERSION, out, 0)
                clientVersion = out[0]
                println("OpenGL ES Version (actual): $clientVersion")
            }

            override fun onDrawFrame(unused: GL10) {
                gameWindow.handleInitEventIfRequired()
                gameWindow.handleReshapeEventIfRequired(0, 0, view.width, view.height)
                gameWindow.frame()
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

    private val touches = TouchEventHandler()
    private val coords = MotionEvent.PointerCoords()

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
        touches.handleEvent(gameWindow, gameWindow.coroutineContext!!, type) { currentTouchEvent ->
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
    val packageManager = context.packageManager
    val featureInfos = packageManager.systemAvailableFeatures
    if (featureInfos != null && featureInfos.isNotEmpty()) {
        for (featureInfo in featureInfos) {
            // Null feature name means this feature is the open gl es version feature.
            if (featureInfo.name == null) {
                return when {
                    featureInfo.reqGlEsVersion != FeatureInfo.GL_ES_VERSION_UNDEFINED -> {
                        (featureInfo.reqGlEsVersion ushr 16) and 0xFF
                    }
                    else -> {
                        1 // Lack of property means OpenGL ES version 1
                    }
                }
            }
        }
    }
    return 1
}

// https://kotlinlang.slack.com/archives/CJEF0LB6Y/p1630391858001400
class AndroidConfigChooser(settings: EGLConfigSettings) : GLSurfaceView.EGLConfigChooser {
    protected var settings: EGLConfigSettings

    /**
     * Gets called by the GLSurfaceView class to return the best config
     */
    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig? {
        var requestedConfig = requestedConfig
        val configs = getConfigs(egl, display)

        // First try to find an exact match, but allowing a higher stencil
        var choosenConfig =
            chooseConfig(egl, display, configs, requestedConfig, false, false, false, true)
        if (choosenConfig == null && requestedConfig.d > 16) {
            requestedConfig.d = 16
            choosenConfig =
                chooseConfig(egl, display, configs, requestedConfig, false, false, false, true)
        }
        if (choosenConfig == null) {
            choosenConfig =
                chooseConfig(egl, display, configs, requestedConfig, true, false, false, true)
        }
        if (choosenConfig == null && requestedConfig.a > 0) {
            choosenConfig =
                chooseConfig(egl, display, configs, requestedConfig, true, true, false, true)
        }
        if (choosenConfig == null && requestedConfig.a > 0) {
            requestedConfig.a = 1
            choosenConfig =
                chooseConfig(egl, display, configs, requestedConfig, true, true, false, true)
        }
        if (choosenConfig == null && requestedConfig.bitsPerPixel > 16) {
            requestedConfig.r = 5
            requestedConfig.g = 6
            requestedConfig.b = 5
            choosenConfig =
                chooseConfig(egl, display, configs, requestedConfig, true, false, false, true)
            if (choosenConfig == null) {
                choosenConfig =
                    chooseConfig(egl, display, configs, requestedConfig, true, true, false, true)
            }
        }

        if (choosenConfig == null) {
            //failsafe, should pick best config with at least 16 depth
            requestedConfig = Config(0, 0, 0, 0, 16, 0)
            choosenConfig =
                chooseConfig(egl, display, configs, requestedConfig, true, false, false, true)
        }

        return choosenConfig
    }

    private val requestedConfig: Config
        get() {
            return Config(
                settings.red,
                settings.green,
                settings.blue,
                settings.alpha,
                settings.depth,
                settings.stencil
            )
        }

    /**
     * Query egl for the available configs
     *
     * @param egl
     * @param display
     * @return
     */
    private fun getConfigs(egl: EGL10, display: EGLDisplay): Array<EGLConfig?> {
        val num_config = IntArray(1)
        val configSpec = intArrayOf(
            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
            EGL10.EGL_NONE
        )
        var gles3 = true

        // Try openGL ES 3
        try {
            if (!egl.eglChooseConfig(display, configSpec, null, 0, num_config)) {
                gles3 = false
            }
        } catch (re: RuntimeException) {
            // it's just the device not supporting GLES3. Fallback to GLES2
            gles3 = false
        }
        if (!gles3) {
            // Get back to openGL ES 2
            configSpec[1] = EGL_OPENGL_ES2_BIT
            if (!egl.eglChooseConfig(display, configSpec, null, 0, num_config)) {
                throw AssertionError()
            }
        }
        val numConfigs = num_config[0]
        val configs = arrayOfNulls<EGLConfig>(numConfigs)
        if (!egl.eglChooseConfig(display, configSpec, configs, numConfigs, num_config)) {
            throw AssertionError()
        }

        return configs
    }

    private fun chooseConfig(
        egl: EGL10, display: EGLDisplay, configs: Array<EGLConfig?>, requestedConfig: Config,
        higherRGB: Boolean, higherAlpha: Boolean,
        higherSamples: Boolean, higherStencil: Boolean
    ): EGLConfig? {
        var keptConfig: EGLConfig? = null
        var kr = 0
        var kg = 0
        var kb = 0
        var ka = 0
        var kd = 0
        var kst = 0


        // first pass through config list.  Try to find an exact match.
        for (config in configs) {
            val r = eglGetConfigAttribSafe(
                egl, display, config,
                EGL10.EGL_RED_SIZE
            )
            val g = eglGetConfigAttribSafe(
                egl, display, config,
                EGL10.EGL_GREEN_SIZE
            )
            val b = eglGetConfigAttribSafe(
                egl, display, config,
                EGL10.EGL_BLUE_SIZE
            )
            val a = eglGetConfigAttribSafe(
                egl, display, config,
                EGL10.EGL_ALPHA_SIZE
            )
            val d = eglGetConfigAttribSafe(
                egl, display, config,
                EGL10.EGL_DEPTH_SIZE
            )
            val st = eglGetConfigAttribSafe(
                egl, display, config,
                EGL10.EGL_STENCIL_SIZE
            )
            if (higherRGB && r < requestedConfig.r) {
                continue
            }
            if (!higherRGB && r != requestedConfig.r) {
                continue
            }
            if (higherRGB && g < requestedConfig.g) {
                continue
            }
            if (!higherRGB && g != requestedConfig.g) {
                continue
            }
            if (higherRGB && b < requestedConfig.b) {
                continue
            }
            if (!higherRGB && b != requestedConfig.b) {
                continue
            }
            if (higherAlpha && a < requestedConfig.a) {
                continue
            }
            if (!higherAlpha && a != requestedConfig.a) {
                continue
            }
            if (d < requestedConfig.d) {
                continue
            } // always allow higher depth
            if (higherStencil && st < requestedConfig.st) {
                continue
            }
            if (!higherStencil && !inRange(st, 0, requestedConfig.st)) {
                continue
            }

            //we keep the config if it is better
            if (r >= kr || g >= kg || b >= kb || a >= ka || d >= kd || st >= kst) {
                kr = r
                kg = g
                kb = b
                ka = a
                kd = d
                kst = st
                keptConfig = config
            }
        }
        if (keptConfig != null) {
            return keptConfig
        }

        //no match found
        return null
    }

    private fun inRange(`val`: Int, min: Int, max: Int): Boolean {
        return `val` in min..max
    }

    private inner class Config constructor(
        /**
         * red, green, blue, alpha, depth, stencil
         */
        var r: Int, var g: Int, var b: Int, var a: Int, var d: Int, var st: Int
    ) {
        val bitsPerPixel: Int
            get() = r + g + b
    }

    companion object {
        private const val EGL_OPENGL_ES2_BIT = 4
        private const val EGL_OPENGL_ES3_BIT = 0x40
        private fun eglGetConfigAttribSafe(
            egl: EGL10,
            display: EGLDisplay,
            config: EGLConfig?,
            attribute: Int
        ): Int {
            val value = IntArray(1)
            if (!egl.eglGetConfigAttrib(display, config, attribute, value)) {
                throw AssertionError()
            }
            return value[0]
        }
    }

    init {
        this.settings = settings
    }
}

data class EGLConfigSettings(
    val red: Int = 8,
    val blue: Int = 8,
    val green: Int = 8,
    val alpha: Int = 0,
    val depth: Int = 16,
    val stencil: Int = 8
)
