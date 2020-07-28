package com.soywiz.korgw.awt

import com.soywiz.kgl.*
import com.soywiz.korgw.x11.*
import com.sun.jna.*
import com.sun.jna.platform.unix.*
import java.awt.*
import java.io.*

open class GLCanvas(val checkGl: Boolean = true) : Canvas(), Closeable {
    val ag: AwtAg = AwtAg(this, checkGl)
    val d: X11.Display = X.XOpenDisplay(null) ?: error("Can't open main display")
    val screen: Int = X.XDefaultScreen(d)
    var ctx: X11OpenglContext? = null
    val gl = ag.gl

    override fun update(g: Graphics) {
        paint(g)
    }

    override fun paint(g: Graphics) {
        val componentId = Native.getComponentID(this)
        if (ctx?.w != null && ctx?.w?.toLong() != componentId) {
            close()
        }
        var lost = false
        if (ctx == null) {
            ctx = X11OpenglContext(d, X11.Drawable(componentId), screen, doubleBuffered = true)
            lost = true
        }
        ctx?.makeCurrent()
        if (lost) {
            ag.contextLost()
        }
        render(gl, g)
        ctx?.swapBuffers()
        ctx?.releaseCurrent()
    }

    override fun close() {
        ctx?.dispose()
        ctx = null
    }

    var defaultRenderer: (gl: KmlGl, g: Graphics) -> Unit = { gl, g ->
        gl.clearColor(0f, 1f, 0f, 1f)
        gl.clear(X11KmlGl.COLOR_BUFFER_BIT)
    }

    open fun render(gl: KmlGl, g: Graphics) {
        defaultRenderer(gl, g)
    }
}
