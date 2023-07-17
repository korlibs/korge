package korlibs.korge.view

import korlibs.image.*
import korlibs.image.text.*
import korlibs.korge.view.property.*
import korlibs.math.geom.*

object TextAlignmentProvider : ViewPropertyProvider.ItemsImpl<TextAlignment>() {
    override val ITEMS: List<TextAlignment> get() = TextAlignment.ALL
}

object HorizontalAlignProvider : ViewPropertyProvider.ItemsImpl<HorizontalAlign>() {
    override val ITEMS: List<HorizontalAlign> get() = HorizontalAlign.ALL
}

object VerticalAlignProvider : ViewPropertyProvider.ItemsImpl<VerticalAlign>() {
    override val ITEMS: List<VerticalAlign> get() = VerticalAlign.ALL
}

object ScaleModeProvider : ViewPropertyProvider.ItemsImpl<ScaleMode>() {
    override val ITEMS = listOf(ScaleMode.COVER, ScaleMode.SHOW_ALL, ScaleMode.EXACT, ScaleMode.NO_SCALE)
}

@Suppress("unused")
object BlendModeProvider : ViewPropertyProvider.ItemsImpl<BlendMode>() {
    override val ITEMS get() = BlendMode.STANDARD_LIST
}

object QualityProvider : ViewPropertyProvider.ItemsImpl<Quality>() {
    override val ITEMS get() = Quality.LIST
}
