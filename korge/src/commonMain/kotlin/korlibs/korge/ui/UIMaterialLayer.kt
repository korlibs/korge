package korlibs.korge.ui

import korlibs.datastructure.*
import korlibs.image.color.*
import korlibs.korge.animate.*
import korlibs.korge.render.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.korge.view.property.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*
import korlibs.time.*

inline fun Container.uiMaterialLayer(
    size: Size = UI_DEFAULT_SIZE,
    block: @ViewDslMarker UIMaterialLayer.() -> Unit = {}
): UIMaterialLayer = UIMaterialLayer(size).addTo(this).apply(block)

class MaterialLayerHighlights(val view: View) {
    class Highlight(var pos: Point, var radiusRatio: Float, var alpha: Float)

    @PublishedApi internal val highlights = fastArrayListOf<Highlight>()
    private val highlightsActive = fastArrayListOf<Highlight>()

    val size: Int get() = highlights.size

    inline fun fastForEach(block: (Highlight) -> Unit) {
        highlights.fastForEach(block)
    }

    fun addHighlight(pos: Point) {
        removeHighlights()
        val highlight = Highlight(pos, 0f, 1f)
        highlights += highlight
        highlightsActive += highlight
        view.simpleAnimator.tween(highlight::radiusRatio[1.0], V2Callback { view.invalidateRender() }, time = 0.5.seconds, easing = Easing.EASE_IN)
    }

    @ViewProperty
    fun removeHighlights() {
        highlightsActive.fastForEach {
            view.simpleAnimator.sequence {
                tween(it::alpha[0.0].delay(0.1.seconds), V2Callback { view.invalidateRender() }, time = 0.3.seconds, easing = Easing.EASE_IN)
                block { highlights.remove(it) }
            }
        }
        highlightsActive.clear()
    }
}

class UIMaterialLayer(
    size: Size = Size(100, 100),
) : UIView(size), ViewLeaf {
    @ViewProperty
    var bgColor: RGBA = Colors.WHITE; set(value) { field = value; invalidateRender() }
    @ViewProperty
    var radius: RectCorners = RectCorners.EMPTY; set(value) { field = value; invalidateRender() }

    @ViewProperty
    var borderColor: RGBA = Colors.BLACK; set(value) { field = value; invalidateRender() }
    @ViewProperty
    var borderSize: Float = 0f; set(value) { field = value; invalidateRender() }

    //var highlightPos = Point(0.5, 0.5); set(value) { field = value; invalidateRender() }
    //var highlightRadius = 0.0; set(value) { field = value; invalidateRender() }
    //var highlightColor = Colors.WHITE; set(value) { field = value; invalidateRender() }

    @ViewProperty
    var shadowColor: RGBA = Colors.BLACK.withAf(0.3f); set(value) { field = value; invalidateRender() }
    @ViewProperty
    var shadowRadius: Float = 10f; set(value) { field = value; invalidateRender() }
    @ViewProperty
    var shadowOffset: Point = Point.ZERO; set(value) { field = value; invalidateRender() }

    private val highlights = MaterialLayerHighlights(this)

    override fun renderInternal(ctx: RenderContext) {
        if (!visible) return

        renderCtx2d(ctx) { ctx2d ->
            //println("context.multiplyColor=${ctx2d.multiplyColor}")
            ctx2d.materialRoundRect(
                x = 0f,
                y = 0f,
                width = width,
                height = height,
                color = bgColor,
                radius = radius,
                shadowOffset = shadowOffset,
                shadowColor = shadowColor,
                shadowRadius = shadowRadius,
                //highlightPos = highlightPos,
                //highlightRadius = highlightRadius,
                //highlightColor = highlightColor,
                borderSize = borderSize,
                borderColor = borderColor,
                //colorMul = renderColorMul,
            )
            highlights.fastForEach {
                ctx2d.materialRoundRect(
                    x = 0f,
                    y = 0f,
                    width = width,
                    height = height,
                    color = Colors.TRANSPARENT,
                    radius = radius,
                    highlightPos = it.pos,
                    highlightRadius = it.radiusRatio,
                    highlightColor = Colors.WHITE.withAd(it.alpha * 0.4),
                    //colorMul = renderColorMul,
                )
            }
        }
    }

    @ViewProperty
    private fun addHighlightAction() {
        addHighlight(Point(0.5, 0.5))
    }

    fun addHighlight(pos: Point) {
        highlights.addHighlight(pos)
    }

    @ViewProperty
    fun removeHighlights() {
        highlights.removeHighlights()
    }
}
