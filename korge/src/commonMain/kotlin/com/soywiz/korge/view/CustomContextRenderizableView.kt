package com.soywiz.korge.view

import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.RenderContext2D

abstract class CustomContextRenderizableView(width: Double, height: Double) : RectBase() {
    override var width: Double = width; set(v) { field = v; dirtyVertices = true }
    override var height: Double = height; set(v) { field = v; dirtyVertices = true }

    lateinit var ctx: RenderContext
    lateinit var ctx2d: RenderContext2D

    final override fun renderInternal(ctx: RenderContext) {
        if (!visible) return
        ctx.useCtx2d { context ->
            this.ctx = ctx
            ctx2d = context
            context.keep {
                context.blendFactors = blendMode.factors
                context.setMatrix(globalMatrix)
                renderer(context, width, height)
            }
        }
    }

    abstract fun renderer(context: RenderContext2D, width: Double, height: Double)
}
