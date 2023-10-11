package korlibs.korge.ui

import korlibs.datastructure.*
import korlibs.image.font.*
import korlibs.image.text.*
import korlibs.io.lang.*
import korlibs.korge.animate.*
import korlibs.korge.annotations.*
import korlibs.korge.style.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.time.*

private val uiTooltipContainerMediatorNewKey: String = "uiTooltipContainerMediatorNew"

@KorgeExperimental
val Container.uiTooltipContainerMediatorNew: UITooltipContainerMediatorNew by Extra.PropertyThis(uiTooltipContainerMediatorNewKey) {
    UITooltipContainerMediatorNew(this)
}

@KorgeExperimental
val Container.closestUITooltipContainerMediatorNew: UITooltipContainerMediatorNew get() {
    if (this.extra?.contains(uiTooltipContainerMediatorNewKey) == true) {
        return this.uiTooltipContainerMediatorNew
    } else {
        return parent?.closestUITooltipContainerMediatorNew ?: uiTooltipContainerMediatorNew
    }
}

@KorgeExperimental
class UITooltipContainerMediatorNew(val container: Container) : Closeable {
    class Tooltip(track: View, textData: RichTextData) : UIView(size = computeSize(textData)) {
        companion object {
            fun computeSize(textData: RichTextData): Size {
                return textData.bounds().size + Size(8, 4)
            }
        }
        var track: View = track
            private set
        val backgroundView = uiMaterialLayer(size = size) {
            bgColor = styles.uiUnselectedColor
            radiusRatio = RectCorners(0.25)
        }
        val textView = textBlock(textData).apply {
            //xy((this@Tooltip.size * 0.5).toPoint())
            this.size = this@Tooltip.size
            this.align = TextAlignment.MIDDLE_CENTER
            //this.alignment = TextAlignment.MIDDLE_CENTER
        }
        var textDataForFit: RichTextData = textData
            set(value) {
                field = value
                textView.text = textData
            }
        var textData: RichTextData = textData
            set(value) {
                field = value
                textView.text = value
            }
        init {
            zIndex = 100000.0
        }
        fun reposition(track: View = this.track) {
            this.track = track
            val bounds = track.getGlobalBounds()
            val tooltipBounds = this.getGlobalBounds()
            this.globalPos = bounds.getAnchoredPoint(Anchor.TOP_CENTER) - Vector2D(tooltipBounds.width * 0.5, tooltipBounds.height + 4.0)
        }
        fun resize(textData: RichTextData) {
            val size = computeSize(textData)
            //println("resize=$size")
            this.size = size
            textView.size = size
            backgroundView.size = size
        }

        override fun onParentChanged() {
            reposition()
        }
    }

    private val tooltips = arrayListOf<Tooltip>()

    fun show(track: View, text: String, maxTextSize: String = text, update: Boolean = true, immediate: Boolean = false): Tooltip {
        val textStyle = RichTextData.Style(DefaultTtfFontAsBitmap, textSize = 12.0)
        val textData = textStyle.withText(text)
        val _tooltip = if (update) tooltips.firstOrNull { it.track == track } else null
        val tooltip = _tooltip ?: Tooltip(track, textData).also { tooltips += it.addTo(container) }
        if (!immediate && _tooltip == null) {
            tooltip.alpha = 0.0
            tooltip.simpleAnimator.tween(tooltip::alpha[1.0], time = .3.seconds)
        }
        tooltip.resize(textStyle.withText(maxTextSize))
        tooltip.textData = textData
        tooltip.reposition()
        //println("maxTextSize=$maxTextSize")
        return tooltip
    }

    fun hide(tooltip: Tooltip, immediate: Boolean = false) {
        if (immediate) {
            tooltip.removeFromParent()
        } else {
            tooltip.simpleAnimator.sequence {
                tween(tooltip::alpha[0.0], time = .3.seconds)
                removeFromParent(tooltip)
            }
        }
        tooltips -= tooltip
    }
    fun hideAll(immediate: Boolean = true): UITooltipContainerMediatorNew {
        while (tooltips.isNotEmpty()) {
            hide(tooltips.last())
        }
        return this
    }

    override fun close() {
        hideAll()
    }
}
