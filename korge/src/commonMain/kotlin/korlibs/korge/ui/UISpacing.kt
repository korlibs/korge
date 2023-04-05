package korlibs.korge.ui

import korlibs.korge.view.*

inline fun Container.uiSpacing(
    width: Float = UI_DEFAULT_WIDTH,
    height: Float = UI_DEFAULT_HEIGHT,
    block: @ViewDslMarker UISpacing.() -> Unit = {}
): UISpacing = UISpacing(width, height).addTo(this).apply(block)

open class UISpacing(
    width: Float = UI_DEFAULT_WIDTH,
    height: Float = UI_DEFAULT_HEIGHT,
) : UIView(width, height), ViewLeaf
