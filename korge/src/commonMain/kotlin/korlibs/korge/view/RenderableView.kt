package korlibs.korge.view

import korlibs.korge.annotations.*
import korlibs.korge.render.*

@KorgeExperimental
inline fun Container.renderableView(
    width: Float = 128f,
    height: Float = 24f,
    noinline viewRenderer: RenderableView.() -> Unit,
): RenderableView = RenderableView(width, height, viewRenderer).addTo(this)

@KorgeExperimental
inline fun Container.renderableView(
    width: Float = 128f,
    height: Float = 24f,
    viewRenderer: ViewRenderer,
): RenderableView = RenderableView(width, height, viewRenderer).addTo(this)

class RenderableView(width: Float, height: Float, var viewRenderer: ViewRenderer) : CustomContextRenderizableView(width, height) {
    var isFocused: Boolean = false
    var isOver: Boolean = false
    var isDown: Boolean = false

    override fun renderer(context: RenderContext2D, width: Float, height: Float) {
        viewRenderer.apply { render() }
    }
}

fun interface ViewRenderer {
    fun RenderableView.render()
}
