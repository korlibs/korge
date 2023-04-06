package korlibs.korge.view

import korlibs.korge.render.*
import korlibs.math.geom.*

abstract class CustomContextRenderizableView(size: Size) : RectBase() {
    override var unscaledSize: Size = size
        set(value) {
            if (field != value) {
                field = value
                dirtyVertices = true
            }
        }

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
