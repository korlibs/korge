package korlibs.korge.ui

import korlibs.image.color.*
import korlibs.io.async.*
import korlibs.korge.input.*
import korlibs.korge.style.*
import korlibs.korge.view.*
import korlibs.korge.view.property.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*
import korlibs.number.*

inline fun Container.uiSlider(
    value: Number = UISlider.DEFAULT_VALUE,
    min: Number = UISlider.DEFAULT_MIN,
    max: Number = UISlider.DEFAULT_MAX,
    step: Number = UISlider.DEFAULT_STEP,
    decimalPlaces: Int = UISlider.decimalPlacesFromStep(step.toDouble()),
    size: Size = UISlider.DEFAULT_SIZE,
    block: @ViewDslMarker UISlider.() -> Unit = {}
): UISlider = UISlider(value, min, max, step, decimalPlaces, size).addTo(this).apply(block)

@Suppress("OPT_IN_USAGE")
class UISlider(
    value: Number = DEFAULT_VALUE, min: Number = DEFAULT_MIN, max: Number = DEFAULT_MAX, step: Number = DEFAULT_STEP,
    decimalPlaces: Int = DEFAULT_DECIMAL_PLACES,
    size: Size = UISlider.DEFAULT_SIZE,
) : UIView(size)
// , StyleableView
{
    companion object {
        const val DEFAULT_VALUE = 0
        const val DEFAULT_MIN = 0
        const val DEFAULT_MAX = 100
        const val DEFAULT_STEP = 1f
        const val DEFAULT_DECIMAL_PLACES = 1
        val DEFAULT_SIZE = Size(128, 16)
        const val NO_STEP = 0f

        fun decimalPlacesFromStep(step: Double): Int = when {
            step >= 1.0 -> 0
            step > 0.01 -> 1
            else -> 2
        }
    }

    private val container = container().xy(0, size.height / 2)
    private val background = container.uiMaterialLayer(Size(100, 6)) {
        anchor(Anchor.MIDDLE_LEFT)
        this.shadowRadius = 0.0
        this.alpha = 0.34
        this.bgColor = styles.uiSelectedColor
        this.radiusRatio = RectCorners(1.0)
    }
    private val foreground = container.uiMaterialLayer(Size(0, 6)) {
        anchor(Anchor.MIDDLE_LEFT)
        this.shadowRadius = 0.0
        this.bgColor = styles.uiSelectedColor
        this.radiusRatio = RectCorners(1.0)
    }
    private val marksContainer = container.container()
    private val thumb = container.uiMaterialLayer(Size(16, 16)) {
        anchor(Anchor.CENTER)
        this.shadowColor = Colors.BLACK.withAd(0.15)
        this.shadowRadius = 4.0
        this.shadowOffset = Vector2D(-1.0, -1.0)
        this.bgColor = styles.uiSelectedColor
        this.radiusRatio = RectCorners(1.0)
        this.highlightColor = styles.uiSelectedColor.withAd(0.4)
        val hoverHighlights = arrayListOf<UIMaterialLayer.Highlight>()
        onOutOnOver({
            removeHighlights()
        }, {
            addHighlight(Point(0.5, 0.5), below = true, scale = 1.5, startRadius = 0.25)
        })
    }

    private fun updateThumbFromMouse(it: MouseEvents) {
        val cp = this@UISlider.globalToLocal(it.currentPosGlobal)
        val ratio = Ratio(cp.x, this@UISlider.width).clamped
        this@UISlider.ratio = ratio
        readjust()
    }

    init {
        draggable(autoMove = false) {
            if (it.start) {
                thumb.addHighlight(Point(0.5, 0.5), below = true, scale = 1.5, startRadius = 0.25)
            }
            updateThumbFromMouse(it.mouseEvents)
            if (it.end) {
                if (showTooltip != true) showTooltip(false)
            }
        }
    }

    val onChange: Signal<Double> = Signal()

    @ViewProperty
    var min: Double = min.toDouble()
        set(value) {
            if (field != value) {
                field = value
                readjust()
            }
        }

    @ViewProperty
    var max: Double = max.toDouble()
        set(value) {
            if (field != value) {
                field = value
                readjust()
            }
        }

    @ViewProperty
    var step: Double = step.toDouble()
        set(value) {
            if (field != value) {
                field = value
                readjust()
            }
        }

    @ViewProperty
    var value: Double = value.toDouble()
        set(value) {
            val rvalue = value.clamp(min, max).nearestAlignedTo(step)
            if (rvalue != field) {
                field = rvalue
                readjust()
                onChange(rvalue)
            }
        }

    @ViewProperty
    var marks: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updatedMarks()
            }
        }

    /**
     * null -> only while dragging
     * true -> always
     * false -> never
     */
    var showTooltip: Boolean? = null
        set(value) {
            if (field != value) {
                field = value
                showTooltip(value == true)
            }
        }

    private val nmarks1: Int get() = ((max - min) / step).toIntCeil()

    private var ratio: Ratio
        get() = value.convertRangeClamped(min, max, 0.0, 1.0).toRatio()
        set(value) {
            this.value = value.convertToRange(min, max)
        }

    private var tooltip: UITooltipContainerMedaitorNew.Tooltip? = null

    init {
        updatedMarks()
        readjust()
    }
    private fun showTooltip(show: Boolean) {
        if (show) updateTooltip() else updateTooltip(null)
    }

    private fun updateTooltip(value: String? = this.value.niceStr(decimalPlacesFromStep(step))) {
        if (value == null) {
            tooltip?.let { tooltipContainer?.hide(it) }
        } else {
            tooltip = tooltipContainer?.show(thumb, value)
        }
    }

    private fun readjust() {
        val ratio = this.ratio
        background.width = width
        foreground.width = width * ratio
        thumb.x = width * ratio
        for (n in 0..nmarks1) {
            val markRatio = Ratio(n, nmarks1)
            val include = (markRatio >= ratio)
            val child = marksContainer.getChildAtOrNull(n)
            (child as? UIMaterialLayer)?.bgColor = if (include) styles.uiSelectedColor else styles.uiBackgroundColor
        }
        if (showTooltip != false) {
            showTooltip(true)
        }
    }

    fun updatedStyles() {
        //println("!! onParentChanged")
        background.bgColor = styles.uiSelectedColor
        foreground.bgColor = styles.uiSelectedColor
        thumb.bgColor = styles.uiSelectedColor
        thumb.highlightColor = styles.uiSelectedColor.withAd(0.4)
        updatedMarks()
    }

    private fun updatedMarks() {
        marksContainer.removeChildren()
        if (!marks) return

        val viewWidth = width
        val nmarks1 = this.nmarks1
        for (n in 0 .. nmarks1) {
            val ratio = Ratio(n, nmarks1)
            marksContainer.uiMaterialLayer {
                anchor(Anchor.CENTER)
                size(2, 2)
                shadowRadius = 0.0
                bgColor = this@UISlider.styles.uiSelectedColor
                radiusRatio = RectCorners(1.0)
                xy(ratio.convertToRange(4.0, viewWidth - 4.0), 0)
            }
        }
    }

    override fun onSizeChanged() {
        super.onSizeChanged()
        updatedMarks()
        updatedStyles()
        readjust()
    }

    private var tooltipContainer: UITooltipContainerMedaitorNew? = null

    override fun onParentChanged() {
        super.onParentChanged()
        tooltipContainer?.close()
        updatedStyles()
        tooltipContainer = parent?.uiTooltipContainerMedaitorNew
        //println("tooltipContainer = $tooltipContainer")
    }
}
