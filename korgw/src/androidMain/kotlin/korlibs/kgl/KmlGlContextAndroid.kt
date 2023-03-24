package korlibs.kgl

import android.content.*
import android.opengl.*
import android.opengl.EGL14.*
import android.opengl.EGLExt.*
import android.os.*
import android.view.*

actual fun KmlGlContextDefault(window: Any?, parent: KmlGlContext?): KmlGlContext {
    return AndroidKmlGlContext(window, parent)
}

fun getAndroidTestContext(): Context {
    return Class.forName("android.app.Instrumentation").getMethod("getContext").invoke(
        Class.forName("androidx.test.platform.app.InstrumentationRegistry").getMethod("getInstrumentation").invoke(null)
    ) as Context
}

class AndroidKmlGlContext(window: Any?, parent: KmlGlContext?) : KmlGlContext(window, KmlGlAndroid({ 2 }), parent) {
    val setup by lazy { Setup().also { it.eglSetup() } }
    val display get() = setup.mEGLDisplay
    val eglSurface get() = setup.mEGLSurface
    val context get() = setup.mEGLContext

    /*
    val androidContext by lazy { (window as? View?)?.context ?: getAndroidTestContext() }
    val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        eglGetDisplay(0)
    } else {
        TODO("VERSION.SDK_INT < JELLY_BEAN_MR1")
    }
    val majorPtr = intArrayOf(0)
    val minorPtr = intArrayOf(0)
    val initialize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        eglInitialize(display, majorPtr, 0, minorPtr, 0)
    } else {
        TODO("VERSION.SDK_INT < JELLY_BEAN_MR1")
    }
    val major = majorPtr[0]
    val minor = minorPtr[0]
    init {
        if (!initialize) error("Can't initialize EGL major=$major, minor=$minor")
    }
    val configs = arrayOfNulls<EGLConfig>(1)
    val numConfigs = intArrayOf(0)
    val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        eglChooseConfig(display, intArrayOf(
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, // request OpenGL ES 2.0
            EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
            EGL_ALPHA_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_RED_SIZE, 8,
            EGL_DEPTH_SIZE, 16,
            EGL_STENCIL_SIZE, 8,
            EGL_NONE
        ), 0, configs, 0, 1, numConfigs, 0)
    } else {
        TODO("VERSION.SDK_INT < JELLY_BEAN_MR1")
    }
    init {
        if (configs[0] == null) error("Can't find config result=$result, display=$display")
        if (numConfigs[0] < 1) error("Can't select config result=$result, display=$display")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            eglBindAPI(EGL_OPENGL_API)
        }
    }
    val context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        eglCreateContext(display, configs[0], null, intArrayOf(
            EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL_NONE
        ), 0)
    } else {
        TODO("VERSION.SDK_INT < JELLY_BEAN_MR1")
    }
    //val surfaceView = SurfaceView(getAndroidTestContext())
    //val eglSurface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
    //    eglCreateWindowSurface(display, configs[0], surfaceView, intArrayOf(), 0)
    //} else {
    //    TODO("VERSION.SDK_INT < JELLY_BEAN_MR1")
    //}
    val eglSurface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        eglCreatePbufferSurface(display, configs[0], intArrayOf(EGL_NONE), 0)
    } else {
        TODO("VERSION.SDK_INT < JELLY_BEAN_MR1")
    }
    */

    override fun set() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            eglMakeCurrent(display, eglSurface, eglSurface, context)
        } else {
            TODO("VERSION.SDK_INT < JELLY_BEAN_MR1")
        }
    }

    override fun unset() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            eglMakeCurrent(display, eglSurface, eglSurface, EGL_NO_CONTEXT)
        } else {
            TODO("VERSION.SDK_INT < JELLY_BEAN_MR1")
        }
    }

    override fun swap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            eglSwapBuffers(display, eglSurface)
        } else {
            TODO("VERSION.SDK_INT < JELLY_BEAN_MR1")
        }
    }

    override fun close() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            eglDestroyContext(display, context)
            eglTerminate(display)
        } else {
            TODO("VERSION.SDK_INT < JELLY_BEAN_MR1")
        }
    }

    class Setup {
        var mEGLDisplay: EGLDisplay? = null
        var mEGLContext: EGLContext? = null
        var mEGLSurface: EGLSurface? = null

        fun eglSetup() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                mEGLDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY)
                if (mEGLDisplay === EGL_NO_DISPLAY) {
                    throw RuntimeException("unable to get EGL14 display")
                }
                val version = IntArray(2)
                if (!eglInitialize(mEGLDisplay, version, 0, version, 1)) {
                    mEGLDisplay = null
                    throw RuntimeException("unable to initialize EGL14")
                }
                val attribList = intArrayOf(
                    EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, // request OpenGL ES 2.0
                    //EGL_RECORDABLE_ANDROID, 1,
                    EGL_RED_SIZE, 8,
                    EGL_GREEN_SIZE, 8,
                    EGL_BLUE_SIZE, 8,
                    EGL_ALPHA_SIZE, 8,
                    EGL_DEPTH_SIZE, 16,
                    EGL_STENCIL_SIZE, 8,
                    EGL_NONE
                )
                val configs: Array<EGLConfig?> = arrayOfNulls<EGLConfig>(1)
                val numConfigs = IntArray(1)
                if (!eglChooseConfig(
                        mEGLDisplay, attribList, 0, configs, 0, configs.size,
                        numConfigs, 0
                    )
                ) {
                    throw RuntimeException("unable to find RGB888+recordable ES2 EGL config")
                }
                val attrib_list = intArrayOf(
                    EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL_NONE
                )
                mEGLContext = eglCreateContext(
                    mEGLDisplay, configs[0], eglGetCurrentContext(),
                    attrib_list, 0
                )
                //GlUtil.checkEglError("eglCreateContext")
                if (mEGLContext == null) {
                    throw RuntimeException("null context")
                }
                val surfaceAttribs = intArrayOf(
                    EGL_NONE
                )
                //mEGLSurface = eglCreateWindowSurface(
                //    mEGLDisplay, configs[0], mSurface,
                //    surfaceAttribs, 0
                //)
                mEGLSurface = eglCreatePbufferSurface(mEGLDisplay, configs[0], intArrayOf(EGL_NONE), 0)

                //GlUtil.checkEglError("eglCreateWindowSurface")
                if (mEGLSurface == null) {
                    throw RuntimeException("surface was null")
                }
            }

        }
    }

}