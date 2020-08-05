package com.soywiz.korgw.awt

import com.soywiz.korgw.platform.*
import java.awt.*

open class GLCanvasGameWindow(val canvas: GLCanvas) : BaseAwtGameWindow() {
    init {
        canvas.ag.isGlAvailable = false
        canvas.defaultRenderer = { gl, g ->
            framePaint(g)
        }
    }

    override val ctx: BaseOpenglContext get() = canvas.ctx!!
    override val ag: AwtAg get() = canvas.ag
    override val component: Component get() = canvas
    override val contentComponent: Component get() = canvas
}
