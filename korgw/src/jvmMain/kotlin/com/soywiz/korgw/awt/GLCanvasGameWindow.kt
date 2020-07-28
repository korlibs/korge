package com.soywiz.korgw.awt

import com.soywiz.korgw.*
import com.soywiz.korgw.platform.*
import java.awt.*

class GLCanvasGameWindow(val canvas: GLCanvas) : BaseAwtGameWindow() {
    init {
        canvas.defaultRenderer = { gl, g ->
            framePaint(g)
        }
    }

    override val ctx: BaseOpenglContext get() = canvas.ctx
    override val ag: AwtAg get() = canvas.ag
    override val component: Component get() = canvas
    override val contentComponent: Component get() = canvas
}
