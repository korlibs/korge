package com.soywiz.korge.view

import com.soywiz.korge.render.RenderContext

interface Renderable {
    fun render(ctx: RenderContext): Unit
}