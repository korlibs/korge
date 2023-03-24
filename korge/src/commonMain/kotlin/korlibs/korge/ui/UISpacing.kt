package korlibs.korge.ui

import korlibs.korge.view.Container
import korlibs.korge.view.ViewDslMarker
import korlibs.korge.view.ViewLeaf
import korlibs.korge.view.addTo

inline fun Container.uiSpacing(
    width: Double = UI_DEFAULT_WIDTH,
    height: Double = UI_DEFAULT_HEIGHT,
    block: @ViewDslMarker UISpacing.() -> Unit = {}
): UISpacing = UISpacing(width, height).addTo(this).apply(block)

open class UISpacing(
    width: Double = UI_DEFAULT_WIDTH,
    height: Double = UI_DEFAULT_HEIGHT,
) : UIView(width, height), ViewLeaf