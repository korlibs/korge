package korlibs.korge.view

import korlibs.korge.render.*
import korlibs.math.geom.*

abstract class CustomContextRenderizableView(size: Size) : RectBase() {
    override var width: Float = size.width; set(v) { field = v; dirtyVertices = true }
    override var height: Float = size.height; set(v) { field = v; dirtyVertices = true }

    lateinit var ctx: RenderContext
    lateinit var ctx2d: RenderContext2D

    final override fun renderInternal(ctx: RenderContext) {
        if (!visible) return
        this.ctx = ctx
        renderCtx2d(ctx) { ctx2d ->
            this.ctx2d = ctx2d
            renderer(ctx2d, Size(width, height))
        }
    }

    abstract fun renderer(context: RenderContext2D, size: Size)
}
