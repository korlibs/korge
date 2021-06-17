package com.soywiz.korge.view

import com.soywiz.korge.annotations.*
import com.soywiz.korge.render.*

@KorgeExperimental
inline fun Container.renderableView(
    width: Double = 128.0,
    height: Double = 24.0,
    noinline skinRenderer: RenderableView.() -> Unit,
): RenderableView = RenderableView(width, height, skinRenderer).addTo(this)

@KorgeExperimental
inline fun Container.renderableView(
    width: Double = 128.0,
    height: Double = 24.0,
    skinRenderer: ViewRenderer,
): RenderableView = RenderableView(width, height, skinRenderer).addTo(this)

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
