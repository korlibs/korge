package com.soywiz.korgw.awt

import com.soywiz.kgl.*
import com.soywiz.korgw.x11.*
import com.sun.jna.*
import com.sun.jna.platform.unix.*
import java.awt.*

open class GLCanvas : Canvas() {
    val d by lazy { X.XOpenDisplay(null) ?: error("Can't open main display") }
    val screen by lazy { X.XDefaultScreen(d) }
    val drawableId by lazy { Native.getComponentID(this) }
    val drawable by lazy { X11.Drawable(drawableId) }
    val ctx by lazy { X11OpenglContext(d, drawable, screen, doubleBuffered = true) }
    val gl = X11KmlGl

    override fun update(g: Graphics) {
        paint(g)
    }

    override fun paint(g: Graphics) {
        ctx.makeCurrent()
        render(gl)
        ctx.swapBuffers()
        ctx.releaseCurrent()
    }

    open fun render(gl: KmlGl) {
        gl.clearColor(0f, 1f, 0f, 1f)
        gl.clear(X11KmlGl.COLOR_BUFFER_BIT)
    }
}
