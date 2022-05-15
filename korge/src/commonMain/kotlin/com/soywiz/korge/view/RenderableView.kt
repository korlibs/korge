package com.soywiz.korge.view

import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korge.render.RenderContext2D

@KorgeExperimental
inline fun Container.renderableView(
    width: Double = 128.0,
    height: Double = 24.0,
    noinline viewRenderer: RenderableView.() -> Unit,
): RenderableView = RenderableView(width, height, viewRenderer).addTo(this)

@KorgeExperimental
inline fun Container.renderableView(
    width: Double = 128.0,
    height: Double = 24.0,
    viewRenderer: ViewRenderer,
): RenderableView = RenderableView(width, height, viewRenderer).addTo(this)

class RenderableView(width: Double, height: Double, var viewRenderer: ViewRenderer) : CustomContextRenderizableView(width, height) {
    var isFocused: Boolean = false
    var isOver: Boolean = false
    var isDown: Boolean = false

    override fun renderer(context: RenderContext2D, width: Double, height: Double) {
        viewRenderer.apply { render() }
    }
}

fun interface ViewRenderer {
    fun RenderableView.render()
}
