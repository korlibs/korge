package com.soywiz.korge.ui

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.render.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.property.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*

inline fun Container.uiMaterialLayer(
    width: Double = UI_DEFAULT_WIDTH,
    height: Double = UI_DEFAULT_HEIGHT,
    block: @ViewDslMarker UIMaterialLayer.() -> Unit = {}
): UIMaterialLayer = UIMaterialLayer(width, height).addTo(this).apply(block)

class MaterialLayerHighlights(val view: View) {
    class Highlight(var pos: IPoint, var radiusRatio: Double, var alpha: Double)

    @PublishedApi internal val highlights = fastArrayListOf<Highlight>()
    private val highlightsActive = fastArrayListOf<Highlight>()

    val size: Int get() = highlights.size

    inline fun fastForEach(block: (Highlight) -> Unit) {
        highlights.fastForEach(block)
    }

    fun addHighlight(pos: IPoint) {
        removeHighlights()
        val highlight = Highlight(pos, 0.0, 1.0)
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
    width: Double = 100.0,
    height: Double = 100.0
) : UIView(width, height), ViewLeaf {
    @ViewProperty
    var bgColor: RGBA = Colors.WHITE; set(value) { field = value; invalidateRender() }
    @ViewProperty
    var radius: IRectCorners = IRectCorners.EMPTY; set(value) { field = value; invalidateRender() }

    @ViewProperty
    var borderColor: RGBA = Colors.BLACK; set(value) { field = value; invalidateRender() }
    @ViewProperty
    var borderSize: Double = 0.0; set(value) { field = value; invalidateRender() }

    //var highlightPos = Point(0.5, 0.5); set(value) { field = value; invalidateRender() }
    //var highlightRadius = 0.0; set(value) { field = value; invalidateRender() }
    //var highlightColor = Colors.WHITE; set(value) { field = value; invalidateRender() }

    @ViewProperty
    var shadowColor: RGBA = Colors.BLACK.withAd(0.3); set(value) { field = value; invalidateRender() }
    @ViewProperty
    var shadowRadius: Double = 10.0; set(value) { field = value; invalidateRender() }
    @ViewProperty
    var shadowOffset: IPoint = IPoint.ZERO; set(value) { field = value; invalidateRender() }

    private val highlights = MaterialLayerHighlights(this)

    override fun renderInternal(ctx: RenderContext) {
        if (!visible) return

        renderCtx2d(ctx) { ctx2d ->
            //println("context.multiplyColor=${ctx2d.multiplyColor}")
            ctx2d.materialRoundRect(
                x = 0.0,
                y = 0.0,
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
                    x = 0.0,
                    y = 0.0,
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
        addHighlight(MPoint(0.5, 0.5))
    }

    fun addHighlight(pos: IPoint) {
        highlights.addHighlight(pos)
    }

    @ViewProperty
    fun removeHighlights() {
        highlights.removeHighlights()
    }
}
