package korlibs.image.text

import korlibs.image.font.*
import korlibs.io.lang.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*

data class TextAlignment(
    val horizontal: HorizontalAlign,
    val vertical: VerticalAlign,
) : EnumLike<TextAlignment> {
    val justified get() = horizontal == HorizontalAlign.JUSTIFY
    val anchor: Anchor = Anchor(horizontal.ratioFake, vertical.ratioFake)

    fun withHorizontal(horizontal: HorizontalAlign) = fromAlign(horizontal, vertical)
    fun withVertical(vertical: VerticalAlign) = fromAlign(horizontal, vertical)

    companion object {
        private val horizontals = listOf(HorizontalAlign.LEFT, HorizontalAlign.CENTER, HorizontalAlign.RIGHT, HorizontalAlign.JUSTIFY)

        private val TOP: List<TextAlignment> = horizontals.map { TextAlignment(it, VerticalAlign.TOP) }
        private val BASELINE = horizontals.map { TextAlignment(it, VerticalAlign.BASELINE) }
        private val MIDDLE = horizontals.map { TextAlignment(it, VerticalAlign.MIDDLE) }
        private val BOTTOM = horizontals.map { TextAlignment(it, VerticalAlign.BOTTOM) }

        val ALL: List<TextAlignment> = TOP + BASELINE + MIDDLE + BOTTOM

        val TOP_LEFT = TOP[0]
        val TOP_CENTER = TOP[1]
        val TOP_RIGHT = TOP[2]
        val TOP_JUSTIFIED = TOP[3]

        val BASELINE_LEFT = BASELINE[0]
        val BASELINE_CENTER = BASELINE[1]
        val BASELINE_RIGHT = BASELINE[2]
        val BASELINE_JUSTIFIED = BASELINE[3]

        val LEFT = TOP[0]
        val CENTER = TOP[1]
        val RIGHT = TOP[2]
        val JUSTIFIED = TOP[3]

        val MIDDLE_LEFT = MIDDLE[0]
        val MIDDLE_CENTER = MIDDLE[1]
        val MIDDLE_RIGHT = MIDDLE[2]
        val MIDDLE_JUSTIFIED = MIDDLE[3]

        val BOTTOM_LEFT = BOTTOM[0]
        val BOTTOM_CENTER = BOTTOM[1]
        val BOTTOM_RIGHT = BOTTOM[2]
        val BOTTOM_JUSTIFIED = BOTTOM[3]

        fun fromAlign(horizontal: HorizontalAlign, vertical: VerticalAlign): TextAlignment {
            val horizontalIndex = when (horizontal) {
                HorizontalAlign.LEFT -> 0
                HorizontalAlign.CENTER -> 1
                HorizontalAlign.RIGHT -> 2
                HorizontalAlign.JUSTIFY -> 3
                else -> return TextAlignment(horizontal, vertical)
            }
            return when (vertical) {
                VerticalAlign.TOP -> TOP[horizontalIndex]
                VerticalAlign.BASELINE -> BASELINE[horizontalIndex]
                VerticalAlign.MIDDLE -> MIDDLE[horizontalIndex]
                VerticalAlign.BOTTOM -> BOTTOM[horizontalIndex]
                else -> TextAlignment(horizontal, vertical)
            }
        }
    }

    override fun EnumLike.Scope.getValues(): List<TextAlignment> = ALL

    override fun toString(): String = "${vertical}_$horizontal"
}

inline class VerticalAlign(val ratio: Float) : EnumLike<VerticalAlign> {
    val ratioFake get() = if (this == BASELINE) 1f else ratio
    val ratioFake0 get() = if (this == BASELINE) 0f else ratio

    object Provider {
        val ITEMS: List<VerticalAlign> get() = ALL
    }

    companion object {
        val TOP = VerticalAlign(0f)
        val MIDDLE = VerticalAlign(0.5f)
        val BOTTOM = VerticalAlign(1f)
        val BASELINE = VerticalAlign(Float.POSITIVE_INFINITY) // Special
        private val values = arrayOf(TOP, MIDDLE, BASELINE, BOTTOM)

        val CENTER get() = MIDDLE
        val ALL = values.toList()

        fun values() = values

        operator fun invoke(str: String): VerticalAlign = when (str.uppercase()) {
            "TOP" -> TOP
            "CENTER", "MIDDLE" -> MIDDLE
            "BOTTOM" -> BOTTOM
            "BASELINE" -> BASELINE
            else -> VerticalAlign(str.substringAfter('(').substringBefore(')').toFloatOrNull() ?: 0f)
        }
    }

    fun getOffsetY(height: Float, baseline: Float): Float = when (this) {
        BASELINE -> baseline
        else -> -height * ratio
    }

    fun getOffsetYRespectBaseline(glyph: GlyphMetrics, font: FontMetrics): Float = when (this) {
        BASELINE -> 0f
        else -> ratio.toRatio().interpolate(font.top, font.bottom)
    }

    fun getOffsetYRespectBaseline(font: FontMetrics, totalHeight: Float): Float = when (this) {
        BASELINE -> 0f
        else -> ratio.toRatio().interpolate(font.top, font.top - totalHeight)
    }

    override fun EnumLike.Scope.getValues(): List<VerticalAlign> = ALL

    override fun toString(): String = when (this) {
        TOP -> "TOP"
        MIDDLE -> "MIDDLE"
        BOTTOM -> "BOTTOM"
        BASELINE -> "BASELINE"
        else -> "VerticalAlign($ratio)"
    }
}

inline class HorizontalAlign(val ratio: Float) : EnumLike<HorizontalAlign> {
    val ratioFake get() = if (this == JUSTIFY) 0f else ratio

    companion object {
        val JUSTIFY = HorizontalAlign(-1f / 2048f)
        val LEFT = HorizontalAlign(0.0f)
        val CENTER = HorizontalAlign(0.5f)
        val RIGHT = HorizontalAlign(1.0f)

        private val values = arrayOf(LEFT, CENTER, RIGHT, JUSTIFY)
        val ALL = values.toList()
        fun values() = values

        operator fun invoke(str: String): HorizontalAlign = when (str.uppercase()) {
            "LEFT" -> LEFT
            "CENTER" -> CENTER
            "RIGHT" -> RIGHT
            "JUSTIFY" -> JUSTIFY
            else -> HorizontalAlign(str.substringAfter('(').substringBefore(')').toFloatOrNull() ?: 0f)
        }
    }

    fun getOffsetX(min: Float, max: Float): Float = getOffsetX(max - min) + min

    fun getOffsetX(width: Float): Float = when (this) {
        JUSTIFY -> 0f
        else -> width * ratio
    }

    override fun EnumLike.Scope.getValues(): List<HorizontalAlign> = ALL

    override fun toString(): String = when (this) {
        LEFT -> "LEFT"
        CENTER -> "CENTER"
        RIGHT -> "RIGHT"
        JUSTIFY -> "JUSTIFY"
        else -> "HorizontalAlign($ratio)"
    }
}
