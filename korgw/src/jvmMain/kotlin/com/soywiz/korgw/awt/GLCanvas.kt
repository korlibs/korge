package com.soywiz.korgw.awt

import com.soywiz.kgl.*
import com.soywiz.korgw.platform.*
import com.sun.jna.*
import java.awt.*
import java.io.*

open class GLCanvas(val checkGl: Boolean = true) : Canvas(), Closeable {
    val ag: AwtAg = AwtAg(this, checkGl)
    private var ctxComponentId: Long = -1L
    var ctx: BaseOpenglContext? = null
    val gl = ag.gl

    override fun update(g: Graphics) {
        paint(g)
    }

    override fun paint(g: Graphics) {
        val componentId = Native.getComponentID(this)
        if (ctxComponentId != componentId) {
            close()
        }
        var lost = false
        if (ctx == null) {
            ctxComponentId = componentId
            ctx = glContextFromComponent(this)
            lost = true
        }
        ctx?.useContext {
            if (lost) {
                ag.contextLost()
            }
            render(gl, g)
        }
    }

    override fun close() {
        ctx?.dispose()
        ctx = null
    }

    var defaultRenderer: (gl: KmlGl, g: Graphics) -> Unit = { gl, g ->
        gl.clearColor(0f, 1f, 0f, 1f)
        gl.clear(gl.COLOR_BUFFER_BIT)
    }

    open fun render(gl: KmlGl, g: Graphics) {
        defaultRenderer(gl, g)
    }
}
