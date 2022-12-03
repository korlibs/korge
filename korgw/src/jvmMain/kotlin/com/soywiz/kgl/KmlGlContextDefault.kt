package com.soywiz.kgl

import com.soywiz.kmem.*
import com.soywiz.kmem.Platform
import com.soywiz.kmem.dyn.*
import com.soywiz.korgw.osx.*
import com.soywiz.korgw.x11.*
import com.sun.jna.*

actual fun KmlGlContextDefault(window: Any?, parent: KmlGlContext?): KmlGlContext = when {
    Platform.isMac -> MacKmlGlContext(window, parent)
    //Platform.isLinux -> LinuxKmlGlContext(window, parent)
    Platform.isLinux -> EGLKmlGlContext(window, parent)
    else -> error("Unsupported OS ${Platform.os}")
}

// http://renderingpipeline.com/2012/05/windowless-opengl/
open class X11KmlGlContext(window: Any? = null, parent: KmlGlContext? = null) : KmlGlContext(window, MacKmlGL(), parent) {
    init {
        val display = X.XOpenDisplay(null)
        //X.glXChooseFBConfig()
        /*
        val display = EGL.eglGetDisplay(0)
        val majorPtr = Memory(8).also { it.clear() }
        val minorPtr = Memory(8).also { it.clear() }
        val initialize = EGL.eglInitialize(display, majorPtr, minorPtr)
        if (!initialize) error("Can't initialize EGL")
        val major = majorPtr.getInt(0)
        val minor = minorPtr.getInt(0)
        println("display=$display, initialize=$initialize, major=${major}, minor=${minor}")

        val attrib_list = Memory()

        EGL.eglChooseConfig(display, attrib_list)
        //X.XBlackPixel(null, 0)
         */

        TODO()
    }
}

// https://forums.developer.nvidia.com/t/egl-without-x11/58733
open class EGLKmlGlContext(window: Any? = null, parent: KmlGlContext? = null) : KmlGlContext(window, MacKmlGL(), parent) {
    val display: Pointer? = run {
        EGL.eglGetDisplay(0).also {
            if (it == null) error("Can't get EGL main display. Try setting env DISPLAY=:0 ?")
        }
    }

    val eglCfg: Pointer? = run {
        val majorPtr = Memory(8).also { it.clear() }
        val minorPtr = Memory(8).also { it.clear() }
        val initialize = EGL.eglInitialize(display, majorPtr, minorPtr)
        if (!initialize) error("Can't initialize EGL")
        val major = majorPtr.getInt(0)
        val minor = minorPtr.getInt(0)
        //println("display=$display, initialize=$initialize, major=${major}, minor=${minor}")

        //val attrib_list = Memory()
        val eglCfgPtr = Memory(8)
        val numConfigsPtr = Memory(4)

        EGL.eglChooseConfig(display, Memory(intArrayOf(
            EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
            EGL_ALPHA_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_RED_SIZE, 8,
            EGL_DEPTH_SIZE, 16,
            EGL_STENCIL_SIZE, 8,
            EGL_RENDERABLE_TYPE, EGL_OPENGL_BIT,
            EGL_NONE
        )), eglCfgPtr, 1, numConfigsPtr)

        val numConfigs = numConfigsPtr.getInt(0)
        if (numConfigs != 1) error("Failed to choose exactly 1 config")

        eglCfgPtr.getPointer(0L)
    }

    val eglCtx: Pointer? = run {
        EGL.eglBindAPI(EGL_OPENGL_API)
        EGL.eglCreateContext(display, eglCfg, (parent as? EGLKmlGlContext?)?.eglCtx, null)
    }

    val eglSurface = EGL.eglCreatePbufferSurface(display, eglCfg, Memory(intArrayOf(
        EGL_WIDTH, 1024,
        EGL_HEIGHT, 1024,
        EGL_NONE,
    )))

    override fun set() {
        EGL.eglMakeCurrent(display, eglSurface, eglSurface, eglCtx)
    }

    override fun unset() {
        EGL.eglMakeCurrent(display, null, null, null)
    }

    override fun close() {
        EGL.eglDestroyContext(display, eglCtx);
        EGL.eglDestroySurface(display, eglSurface)
    }

    companion object {
        const val EGL_PBUFFER_BIT                   = 0x0001
        const val EGL_OPENGL_BIT                    = 0x0008

        const val EGL_ALPHA_SIZE                    = 0x3021
        const val EGL_BLUE_SIZE                     = 0x3022
        const val EGL_GREEN_SIZE                    = 0x3023
        const val EGL_RED_SIZE                      = 0x3024
        const val EGL_DEPTH_SIZE                    = 0x3025
        const val EGL_STENCIL_SIZE                  = 0x3026

        const val EGL_SURFACE_TYPE                  = 0x3033
        const val EGL_TRANSPARENT_TYPE              = 0x3034
        const val EGL_TRANSPARENT_BLUE_VALUE        = 0x3035
        const val EGL_TRANSPARENT_GREEN_VALUE       = 0x3036
        const val EGL_TRANSPARENT_RED_VALUE         = 0x3037
        const val EGL_NONE                          = 0x3038

        const val EGL_RENDERABLE_TYPE               = 0x3040

        const val EGL_TRANSPARENT_RGB               = 0x3052
        const val EGL_HEIGHT                        = 0x3056
        const val EGL_WIDTH                         = 0x3057

        const val EGL_OPENGL_API                    = 0x30A2

    }
}


// http://renderingpipeline.com/2012/05/windowless-opengl-on-macos-x/
open class MacKmlGlContext(window: Any? = null, parent: KmlGlContext? = null) : KmlGlContext(window, MacKmlGL(), parent) {
    init {
        MacGL.CGLEnable()
    }
    var ctx: com.sun.jna.Pointer? = run {
        val attributes = Memory(16L)
        attributes.setInt(0, kCGLPFAAccelerated)
        attributes.setInt(4, kCGLPFAOpenGLProfile)
        attributes.setInt(8, kCGLOGLPVersion_GL4_Core)
        attributes.setInt(12, 0)
        val num = Memory(4L)
        val ctx = Memory(8L) // void**
        val pix = Memory(8L) // void**
        checkError(MacGL.CGLChoosePixelFormat(attributes, pix, num))
        checkError(MacGL.CGLCreateContext(pix.getPointer(0L), (parent as? MacKmlGlContext)?.ctx, ctx))
        MacGL.CGLDestroyPixelFormat(pix.getPointer(0L))
        ctx.getPointer(0L)
    }

    private fun <T> checkError(value: T): T {
        return value
    }

    override fun set() {
        MacGL.CGLSetCurrentContext(ctx)
    }

    override fun unset() {
        MacGL.CGLSetCurrentContext(null)
    }

    override fun close() {
        if (ctx == null) return
        if (MacGL.CGLGetCurrentContext() == ctx) {
            MacGL.CGLSetCurrentContext(null)
        }
        MacGL.CGLDestroyContext(ctx)
        ctx = null
    }

    companion object {
        const val kCGLPFAAccelerated = 73
        const val kCGLPFAOpenGLProfile = 99
        const val kCGLOGLPVersion_GL4_Core = 16640
    }
}
