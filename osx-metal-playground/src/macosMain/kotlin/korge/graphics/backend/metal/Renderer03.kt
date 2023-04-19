package korge.graphics.backend.metal

import korlibs.image.color.*
import korlibs.korge.render.*
import korlibs.math.geom.*
import korlibs.metal.*
import platform.MetalKit.*

class Renderer03(view: MTKView) : Renderer() {

    private val ag = AGMetal(view)
    private val agBitmapTextureManager = AgBitmapTextureManager(ag)
    private var renderContext = RenderContext(ag)
    private var renderContext2D = RenderContext2D(renderContext!!.batch, agBitmapTextureManager)

    override fun drawOnView(view: MTKView) {
        renderContext2D.rect(Rectangle(0f, 0f, 100f, 100f), Colors.RED)
        renderContext.flush()
    }
}
