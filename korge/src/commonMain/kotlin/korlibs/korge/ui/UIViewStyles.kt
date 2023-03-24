package korlibs.korge.ui

import korlibs.korge.render.*
import korlibs.korge.style.*
import korlibs.image.color.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*

typealias UIRenderer<T> = RenderContext2D.(progressBar: T) -> Unit

var ViewStyles.uiBackgroundColor: RGBA by ViewStyle(MaterialColors.BLUE_50)
var ViewStyles.uiSelectedColor: RGBA by ViewStyle(MaterialColors.BLUE_600)
var ViewStyles.uiUnselectedColor: RGBA by ViewStyle(MaterialColors.GRAY_700)
fun ViewStyles.uiSelectedColor(selected: Boolean): RGBA = if (selected) uiSelectedColor else uiUnselectedColor
var ViewStyles.uiProgressBarRenderer: UIRenderer<UIProgressBar> by ViewStyle {
    materialRoundRect(0.0, 0.0, width, height, radius = RectCorners(3.0), color = it.styles.uiBackgroundColor)
    materialRoundRect(0.0, 0.0, width * it.ratio, height, radius = RectCorners(3.0), color = it.styles.uiSelectedColor)
}
var ViewStyles.uiCheckboxButtonRenderer: UIRenderer<UIBaseCheckBox<*>> by ViewStyle {
    val extraPad = -0.0
    val extraPad2 = extraPad * 2
    val styles = it.styles
    materialRoundRect(
        0.0 + extraPad, 0.0 + extraPad, height - extraPad2, height - extraPad2, radius = RectCorners((height - extraPad2) * 0.5),
        color = (kotlin.math.max(it.overRatio, it.focusRatio)).interpolate(Colors.TRANSPARENT, styles.uiSelectedColor(it.checkedRatio > 0.5).withAd(0.3))
    )
    it.highlights.fastForEach {
        materialRoundRect(
            0.0 + extraPad, 0.0 + extraPad, height - extraPad2, height - extraPad2, radius = RectCorners(height * 0.5),
            color = Colors.TRANSPARENT,
            highlightPos = it.pos,
            highlightRadius = it.radiusRatio,
            highlightColor = styles.uiSelectedColor.withAd(it.alpha * 0.7),
            //colorMul = renderColorMul,
        )
    }
    when (it.kind) {
        UISwitch, UICheckBox -> {
            val padding = 6.0
            val padding2 = padding * 2
            materialRoundRect(
                0.0 + padding, 0.0 + padding, height - padding2, height - padding2, radius = RectCorners(4.0),
                color = Colors.TRANSPARENT,
                borderColor = it.checkedRatio.interpolate(styles.uiUnselectedColor, styles.uiSelectedColor),
                borderSize = 2.0,
            )
            run {
                val padding = it.checkedRatio.toRatio().interpolate(height, 10.0)
                val padding2 = padding * 2
                materialRoundRect(
                    0.0 + padding, 0.0 + padding, height - padding2, height - padding2, radius = RectCorners(1.0),
                    color = it.checkedRatio.interpolate(Colors.TRANSPARENT, styles.uiSelectedColor),
                )
            }
        }
        else -> {
            val padding = 6.0
            val padding2 = padding * 2
            materialRoundRect(
                0.0 + padding,
                0.0 + padding,
                height - padding2,
                height - padding2,
                radius = RectCorners((height - padding2) * 0.5),
                borderSize = 2.0,
                borderColor = it.checkedRatio.interpolate(styles.uiUnselectedColor, styles.uiSelectedColor),
                color = Colors.TRANSPARENT,
                highlightRadius = it.checkedRatio.toRatio().interpolate(0.0, 0.2),
                highlightPos = Point(0.5, 0.5),
                highlightColor = styles.uiSelectedColor,
            )
        }
    }
}