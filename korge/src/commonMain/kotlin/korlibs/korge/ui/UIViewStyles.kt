package korlibs.korge.ui

import korlibs.image.color.*
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.render.*
import korlibs.korge.style.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*

typealias UIRenderer<T> = RenderContext2D.(progressBar: T) -> Unit

var ViewStyles.uiBackgroundColor: RGBA by ViewStyle(MaterialColors.BLUE_50)
var ViewStyles.uiSelectedColor: RGBA by ViewStyle(MaterialColors.BLUE_600)
var ViewStyles.uiUnselectedColor: RGBA by ViewStyle(MaterialColors.GRAY_700)
fun ViewStyles.uiSelectedColor(selected: Boolean): RGBA = if (selected) uiSelectedColor else uiUnselectedColor
var ViewStyles.uiProgressBarRenderer: UIRenderer<UIProgressBar> by ViewStyle {
    materialRoundRect(0f, 0f, width, height, radius = RectCorners(3.0), color = it.styles.uiBackgroundColor)
    materialRoundRect(0f, 0f, width * it.ratio, height, radius = RectCorners(3.0), color = it.styles.uiSelectedColor)
}
@OptIn(KorgeExperimental::class)
var ViewStyles.uiCheckboxButtonRenderer: UIRenderer<UIBaseCheckBox<*>> by ViewStyle {
    val extraPad = -0f
    val extraPad2 = extraPad * 2
    val styles = it.styles
    materialRoundRect(
        0f + extraPad, 0f + extraPad, height - extraPad2, height - extraPad2, radius = RectCorners((height - extraPad2) * 0.5),
        color = (kotlin.math.max(it.overRatio, it.focusRatio)).interpolate(Colors.TRANSPARENT, styles.uiSelectedColor(it.checkedRatio > 0.5).withAd(0.3))
    )
    it.highlights.fastForEach {
        materialRoundRect(
            0f + extraPad, 0f + extraPad, height - extraPad2, height - extraPad2, radius = RectCorners(height * 0.5),
            color = Colors.TRANSPARENT,
            highlightPos = it.pos,
            highlightRadius = it.radiusRatio,
            highlightColor = styles.uiSelectedColor.withAd(it.alpha * 0.7),
            //colorMul = renderColorMul,
        )
    }
    when (it.kind) {
        UISwitch, UICheckBox -> {
            val padding = 6f
            val padding2 = padding * 2
            materialRoundRect(
                0f + padding, 0f + padding, height - padding2, height - padding2, radius = RectCorners(4.0),
                color = Colors.TRANSPARENT,
                borderColor = it.checkedRatio.interpolate(styles.uiUnselectedColor, styles.uiSelectedColor),
                borderSize = 2f,
            )
            run {
                val padding = it.checkedRatio.toRatio().interpolate(height, 10f)
                val padding2 = padding * 2
                materialRoundRect(
                    0f + padding, 0f + padding, height - padding2, height - padding2, radius = RectCorners(1.0),
                    color = it.checkedRatio.interpolate(Colors.TRANSPARENT, styles.uiSelectedColor),
                )
            }
        }
        else -> {
            val padding = 6f
            val padding2 = padding * 2
            materialRoundRect(
                0f + padding,
                0f + padding,
                height - padding2,
                height - padding2,
                radius = RectCorners((height - padding2) * 0.5),
                borderSize = 2f,
                borderColor = it.checkedRatio.interpolate(styles.uiUnselectedColor, styles.uiSelectedColor),
                color = Colors.TRANSPARENT,
                highlightRadius = it.checkedRatio.toRatio().interpolate(0f, 0.2f),
                highlightPos = Point(0.5, 0.5),
                highlightColor = styles.uiSelectedColor,
            )
        }
    }
}
