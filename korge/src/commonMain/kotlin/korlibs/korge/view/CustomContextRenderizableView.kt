package korlibs.korge.view

import korlibs.korge.render.*

abstract class CustomContextRenderizableView(width: Float, height: Float) : RectBase() {
    override var width: Float = width; set(v) { field = v; dirtyVertices = true }
    override var height: Float = height; set(v) { field = v; dirtyVertices = true }

    lateinit var ctx: RenderContext
    lateinit var ctx2d: RenderContext2D

    final override fun renderInternal(ctx: RenderContext) {
        if (!visible) return
        this.ctx = ctx
        renderCtx2d(ctx) { ctx2d ->
            this.ctx2d = ctx2d
            renderer(ctx2d, width, height)
        }
    }

    abstract fun renderer(context: RenderContext2D, width: Float, height: Float)
}
