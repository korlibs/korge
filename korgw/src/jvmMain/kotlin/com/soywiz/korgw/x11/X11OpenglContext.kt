package com.soywiz.korgw.x11

import com.soywiz.korgw.platform.*
import com.soywiz.korio.lang.*
import com.sun.jna.CallbackReference
import com.sun.jna.Pointer
import com.sun.jna.platform.unix.*

// https://www.khronos.org/opengl/wiki/Tutorial:_OpenGL_3.0_Context_Creation_(GLX)
class X11OpenglContext(val d: X11.Display?, val w: X11.Drawable?, val scr: Int, val vi: XVisualInfo? = chooseVisuals(d, scr), val doubleBuffered: Boolean = true) : BaseOpenglContext {
    companion object {
        fun chooseVisuals(d: X11.Display?, scr: Int = X.XDefaultScreen(d)): XVisualInfo? {
            val attrsList = listOf(
                intArrayOf(GLX_RGBA, GLX_DOUBLEBUFFER, GLX_DEPTH_SIZE, 24, X11.None),
                intArrayOf(GLX_RGBA, GLX_DEPTH_SIZE, 24, X11.None),
                intArrayOf(GLX_RGBA, GLX_DOUBLEBUFFER, GLX_DEPTH_SIZE, 16, X11.None),
                intArrayOf(GLX_RGBA, GLX_DEPTH_SIZE, 16, X11.None),
                intArrayOf(GLX_RGBA, GLX_DOUBLEBUFFER, X11.None),
                //intArrayOf(GLX_RENDER_TYPE, GLX.GLX_RGBA_TYPE), // default
                intArrayOf(GLX_RGBA, X11.None),
                intArrayOf(X11.None)
            )
            for (attrs in attrsList) {
                val vi = X.glXChooseVisual(d, scr, attrs)
                if (vi != null) {
                    println("VI: $vi")
                    return vi
                }
            }
            println("VI: null")
            return null
        }
    }
    init {
        println("Preparing OpenGL context. Screen: $scr")
    }
    init {
        println("VI: $vi")
    }
    val glc = if (vi != null) X.glXCreateContext(d, vi, null, true) else null

    init {
        if (vi == null || glc == null) {
            println("WARNING! Visuals or GLC are NULL! This will probably cause a white window")
        }
        println("d: $d, w: $w, s: $scr, VI: $vi, glc: $glc")
        makeCurrent()
        println("GL_RENDERER: '" + X.glGetString(GL.GL_RENDERER) + "'")
        println("GL_VENDOR: '" + X.glGetString(GL.GL_VENDOR) + "'")
        println("GL_VERSION: '" + X.glGetString(GL.GL_VERSION) + "'")
    }

    val extensions = (X.glGetString(GL.GL_EXTENSIONS) ?: "").split(" ").toSet()

    init {
        println("GL_EXTENSIONS: " + extensions.size)
        if (Environment["GL_DUMP_EXTENSIONS"] == "true") {
            println("GL_EXTENSIONS: '$extensions'")
        }
    }

    override fun makeCurrent() {
        val result = X.glXMakeCurrent(d, w, glc)
        //X.glXMakeContextCurrent(d, w, w, glc)
        //glXMakeContextCurrent()
        //println("makeCurrent: $result")
    }

    override fun swapBuffers() {
        val result = X.glXSwapBuffers(d, w)
        //println("swapBuffers: $result")
    }

    private var glXSwapIntervalEXTSet: Boolean = false
    private var swapIntervalEXT: X11GameWindow.glXSwapIntervalEXTCallback? = null
    private var swapIntervalEXTPointer: Pointer? = null

    private fun getSwapInterval(): X11GameWindow.glXSwapIntervalEXTCallback? {
        if (!glXSwapIntervalEXTSet) {
            glXSwapIntervalEXTSet = true
            swapIntervalEXTPointer = X.glXGetProcAddress("glXSwapIntervalEXT")

            swapIntervalEXT = when {
                swapIntervalEXTPointer != Pointer.NULL -> CallbackReference.getCallback(X11GameWindow.glXSwapIntervalEXTCallback::class.java, swapIntervalEXTPointer) as? X11GameWindow.glXSwapIntervalEXTCallback?
                else -> null
            }
            println("swapIntervalEXT: $swapIntervalEXT")
        }
        return swapIntervalEXT
    }

    override fun supportsSwapInterval(): Boolean {
        return getSwapInterval() != null
    }

    override fun swapInterval(value: Int) {
        val dpy = X.glXGetCurrentDisplay()
        val drawable = X.glXGetCurrentDrawable()
        getSwapInterval()?.callback(dpy, drawable, value)
    }
}
