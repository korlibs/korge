package com.soywiz.korgw.awt

import com.soywiz.kgl.*
import com.soywiz.korgw.x11.*
import com.sun.jna.*
import com.sun.jna.platform.unix.*
import java.awt.*

open class GLCanvas(val checkGl: Boolean = false) : Canvas() {
    val ag: AwtAg = AwtAg(this, checkGl)
    val d by lazy { X.XOpenDisplay(null) ?: error("Can't open main display") }
    val screen by lazy { X.XDefaultScreen(d) }
    val drawableId by lazy { Native.getComponentID(this) }
    val drawable by lazy { X11.Drawable(drawableId) }
    val ctx by lazy { X11OpenglContext(d, drawable, screen, doubleBuffered = true) }
    val gl = ag.gl

    override fun update(g: Graphics) {
        paint(g)
    }

    override fun paint(g: Graphics) {
        ctx.makeCurrent()
        render(gl, g)
        ctx.swapBuffers()
        ctx.releaseCurrent()
    }

    var defaultRenderer: (gl: KmlGl, g: Graphics) -> Unit = { gl, g ->
        gl.clearColor(0f, 1f, 0f, 1f)
        gl.clear(X11KmlGl.COLOR_BUFFER_BIT)
    }

    open fun render(gl: KmlGl, g: Graphics) {
        defaultRenderer(gl, g)
    }
}
