package korge.graphics.backend.metal

import korlibs.image.color.*
import korlibs.korge.render.*
import korlibs.math.geom.*
import korlibs.metal.*
import platform.MetalKit.*

class Renderer03 : Renderer() {

    private var renderContext : RenderContext? = null
    private var renderContext2D : RenderContext2D? = null

    override fun drawOnView(view: MTKView) {
        if (renderContext == null) {
            val ag = AGMetal(view)
            val agBitmapTextureManager = AgBitmapTextureManager(ag)
            renderContext = RenderContext(ag)
            renderContext2D = RenderContext2D(renderContext!!.batch, agBitmapTextureManager)
        }

        renderContext2D?.rect(Rectangle(0f, 0f, 100f, 100f), Colors.RED)
        renderContext?.flush()
    }
}
