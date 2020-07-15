package com.soywiz.korvi.internal.experimental

import android.opengl.EGL14
import android.opengl.GLUtils
import javax.microedition.khronos.egl.*

class OffscreenSurface(val width: Int, val height: Int) {
    var mEgl: EGL10? = null
    var mEglDisplay: EGLDisplay? = null
    var mEglContext: EGLContext? = null
    var mEglSurface: EGLSurface? = null

    init {
        init()
    }

    inline fun makeCurrentTemporarily(block: () -> Unit) {
        val oldDisplay = mEgl?.eglGetCurrentDisplay()
        val oldSurface = mEgl?.eglGetCurrentSurface(1)
        val oldContext = mEgl?.eglGetCurrentContext()
        makeCurrent()
        try {
            block()
        } finally {
            mEgl?.eglMakeCurrent(oldDisplay, oldSurface, oldSurface, oldContext)
        }
    }

    fun makeCurrent() {
        mEgl?.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)
    }

    fun init() {
        val mEgl = javax.microedition.khronos.egl.EGLContext.getEGL() as EGL10
        mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)

        if (mEglDisplay === EGL10.EGL_NO_DISPLAY) throw RuntimeException(
            "Error: eglGetDisplay() Failed " + GLUtils.getEGLErrorString(
                mEgl.eglGetError()
            )
        )

        val version = IntArray(2)

        if (!mEgl.eglInitialize(
                mEglDisplay,
                version
            )
        ) throw RuntimeException("Error: eglInitialize() Failed " + GLUtils.getEGLErrorString(mEgl.eglGetError()))

        val maEGLconfigs = arrayOfNulls<EGLConfig>(1)

        val configsCount = IntArray(1)
        val configSpec = intArrayOf(
            EGL10.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_DEPTH_SIZE, 0,
            EGL10.EGL_STENCIL_SIZE, 0,
            EGL10.EGL_NONE
        )

        require(
            !(!mEgl.eglChooseConfig(
                mEglDisplay,
                configSpec,
                maEGLconfigs,
                1,
                configsCount
            ) || configsCount[0] === 0)
        ) { "Error: eglChooseConfig() Failed " + GLUtils.getEGLErrorString(mEgl.eglGetError()) }

        if (maEGLconfigs[0] == null) throw java.lang.RuntimeException("Error: eglConfig() not Initialized")

        val attrib_list = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE)

        mEglContext = mEgl.eglCreateContext(mEglDisplay, maEGLconfigs[0], EGL10.EGL_NO_CONTEXT, attrib_list)

        mEglSurface = mEgl.eglCreatePbufferSurface(mEglDisplay, maEGLconfigs[0], intArrayOf(EGL10.EGL_WIDTH, width, EGL10.EGL_HEIGHT, height))

        if (mEglSurface == null || mEglSurface === EGL10.EGL_NO_SURFACE) {
            val error = mEgl.eglGetError()
            if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                return
            }
            throw java.lang.RuntimeException("Error: createWindowSurface() Failed " + GLUtils.getEGLErrorString(error))
        }
        /*
        if (!mEgl.eglMakeCurrent(
                mEglDisplay,
                mEglSurface,
                mEglSurface,
                mEglContext
            )
        ) throw java.lang.RuntimeException("Error: eglMakeCurrent() Failed " + GLUtils.getEGLErrorString(mEgl.eglGetError()))

        val widthResult = IntArray(1)
        val heightResult = IntArray(1)

        mEgl.eglQuerySurface(mEglDisplay, mEglSurface, EGL10.EGL_WIDTH, widthResult)
        mEgl.eglQuerySurface(mEglDisplay, mEglSurface, EGL10.EGL_HEIGHT, heightResult)

        println("${widthResult.toList()}, ${heightResult.toList()}")
         */
    }
}
