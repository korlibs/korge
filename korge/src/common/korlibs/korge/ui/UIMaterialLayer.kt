package korlibs.korge.ui

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.image.color.*
import korlibs.korge.animate.*
import korlibs.korge.annotations.*
import korlibs.korge.internal.*
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

typealias MaterialLayerHighlights = UIMaterialLayer.Highlights

@OptIn(KorgeInternal::class, KorgeExperimental::class)
class UIMaterialLayer(
    size: Size = Size(100, 100),
) : UIView(size), ViewLeaf, Anchorable {
    class Highlight(var pos: Point, var radiusRatio: Double, var alpha: Double, var below: Boolean = false, var scale: Double = 1.0)

    class Highlights(val view: View) {
        @PublishedApi internal val highlights = fastArrayListOf<Highlight>()
        private val highlightsActive = fastArrayListOf<Highlight>()

        val size: Int get() = highlights.size

        inline fun fastForEach(block: (Highlight) -> Unit) {
            highlights.fastForEach(block)
        }

        fun addHighlight(pos: Point, below: Boolean = false, scale: Double = 1.0, startRadius: Double = 0.0): Highlight {
            removeHighlights()
            val highlight = Highlight(pos, startRadius, 1.0, below, scale)
            highlights += highlight
            highlightsActive += highlight
            view.simpleAnimator.tween(highlight::radiusRatio[1.0], V2Callback { view.invalidateRender() }, time = 0.5.seconds, easing = Easing.EASE_IN)
            return highlight
        }

        fun removeHighlight(highlight: Highlight) {
            view.simpleAnimator.sequence {
                tween(
                    if (highlight.below) highlight::radiusRatio[0.0] else highlight::alpha[0.0],
                    V2Callback { view.invalidateRender() }, time = 0.3.seconds, easing = Easing.EASE_IN)
                block { highlights.remove(highlight) }
            }
            highlightsActive.remove(highlight)
        }

        fun removeHighlights(highlights: List<Highlight>) {
            highlights.toList().fastForEach { removeHighlight(it) }
            //highlightsActive.clear()
        }

        @ViewProperty
        fun removeHighlights() {
            removeHighlights(highlightsActive)
        }
    }


    @ViewProperty
    var bgColor: RGBA = Colors.WHITE; set(value) { field = value; invalidateRender() }
    @ViewProperty
    var radius: RectCorners? = null; set(value) { field = value; invalidateRender() }
    @ViewProperty
    var radiusRatio: RectCorners? = null; set(value) { field = value; invalidateRender() }

    @ViewProperty
    var borderColor: RGBA = Colors.BLACK; set(value) { field = value; invalidateRender() }
    @ViewProperty
    var borderSize: Double = 0.0; set(value) { field = value; invalidateRender() }

    //var highlightPos = Point(0.5, 0.5); set(value) { field = value; invalidateRender() }
    //var highlightRadius = 0.0; set(value) { field = value; invalidateRender() }
    //var highlightColor = Colors.WHITE; set(value) { field = value; invalidateRender() }

    @ViewProperty
    var shadowColor: RGBA = Colors.BLACK.withAf(0.3f); set(value) { field = value; invalidateRender() }
    @ViewProperty
    var shadowRadius: Double = 10.0; set(value) { field = value; invalidateRender() }
    @ViewProperty
    var shadowOffset: Point = Point.ZERO; set(value) { field = value; invalidateRender() }

    @ViewProperty
    var highlightColor: RGBA = Colors.WHITE.withAd(0.4)

    private val highlights = Highlights(this)

    override var anchor: Anchor = Anchor.TOP_LEFT
        set(value) {
            if (field != value) {
                field = value
                invalidateLocalBounds()
            }
        }

    @KorgeInternal override val anchorDispX: Float get() = (width * anchor.sx).toFloat()
    @KorgeInternal override val anchorDispY: Float get() = (height * anchor.sy).toFloat()

    override fun getLocalBoundsInternal(): Rectangle = Rectangle(-anchorDispX.toDouble(), -anchorDispY.toDouble(), width, height)

    private val computedRadius: RectCorners get() {
        return radiusRatio?.times(minOf(width, height) / 2.0) ?: radius ?: RectCorners.EMPTY
    }

    override fun renderInternal(ctx: RenderContext) {
        if (!visible) return

        renderCtx2d(ctx) { ctx2d ->
            //println("context.multiplyColor=${ctx2d.multiplyColor}")
            renderHighlights(ctx2d, below = true)
            ctx2d.materialRoundRect(
                x = -anchorDispX.toDouble(),
                y = -anchorDispY.toDouble(),
                width = width,
                height = height,
                color = bgColor,
                radius = computedRadius,
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
            renderHighlights(ctx2d, below = false)
        }
    }

    fun renderHighlights(ctx2d: RenderContext2D, below: Boolean) {
        highlights.fastForEach {
            if (below == it.below) {
                ctx2d.materialRoundRect(
                    x = -anchorDispX.toDouble() * it.scale,
                    y = -anchorDispY.toDouble() * it.scale,
                    width = width * it.scale,
                    height = height * it.scale,
                    color = Colors.TRANSPARENT,
                    radius = computedRadius * it.scale,
                    highlightPos = it.pos,
                    highlightRadius = it.radiusRatio * scale,
                    highlightColor = highlightColor.withAd(highlightColor.ad * it.alpha),
                    //colorMul = renderColorMul,
                )
            }
        }
    }

    @ViewProperty
    private fun addHighlightAction() {
        addHighlight(Point(0.5, 0.5))
    }

    @ViewProperty
    private fun addHighlightActionBelow() {
        addHighlight(Point(0.5, 0.5), below = true)
    }

    fun addHighlight(pos: Point, below: Boolean = false, scale: Double = 1.0, startRadius: Double = 0.0): Highlight {
        return highlights.addHighlight(pos, below, scale, startRadius)
    }

    fun removeHighlight(highlight: Highlight) {
        highlights.removeHighlight(highlight)
    }

    fun removeHighlights(highlights: List<Highlight>) {
        this.highlights.removeHighlights(highlights)
    }

    @ViewProperty
    fun removeHighlights() {
        highlights.removeHighlights()
    }
}
