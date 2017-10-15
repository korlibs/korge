package com.soywiz.korge.view

import com.soywiz.korge.render.RenderContext
import com.soywiz.korma.Matrix2d

interface Renderable {
    fun render(ctx: RenderContext, m: Matrix2d): Unit
}
