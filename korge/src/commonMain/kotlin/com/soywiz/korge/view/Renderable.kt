package com.soywiz.korge.view

import com.soywiz.korge.render.RenderContext

/**
 * Represents something that can be rendered. Usually a [View].
 */
interface Renderable {
    /**
     * Called when the render needs to be done. The object should use the [ctx] [RenderContext] to perform the drawings.
     */
	fun render(ctx: RenderContext): Unit
}
