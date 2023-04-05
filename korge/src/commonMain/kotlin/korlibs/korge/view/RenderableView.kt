package korlibs.korge.view

import korlibs.korge.annotations.*
import korlibs.korge.render.*
import korlibs.math.geom.*

@KorgeExperimental
inline fun Container.renderableView(size: Size = Size(128, 24), noinline viewRenderer: RenderableView.() -> Unit, ): RenderableView = RenderableView(size, viewRenderer).addTo(this)

@KorgeExperimental
inline fun Container.renderableView(size: Size = Size(128, 24), viewRenderer: ViewRenderer, ): RenderableView = RenderableView(size, viewRenderer).addTo(this)

class RenderableView(size: Size, var viewRenderer: ViewRenderer) : CustomContextRenderizableView(size) {
    var isFocused: Boolean = false
    var isOver: Boolean = false
    var isDown: Boolean = false

    override fun renderer(context: RenderContext2D, size: Size) {
        viewRenderer.apply { render() }
    }
}

fun interface ViewRenderer {
    fun RenderableView.render()
}
