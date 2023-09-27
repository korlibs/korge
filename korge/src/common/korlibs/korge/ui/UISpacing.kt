package korlibs.korge.ui

import korlibs.korge.view.*
import korlibs.math.geom.*

inline fun Container.uiSpacing(
    size: Size = UI_DEFAULT_SIZE,
    block: @ViewDslMarker UISpacing.() -> Unit = {}
): UISpacing = UISpacing(size).addTo(this).apply(block)

open class UISpacing(
    size: Size = UI_DEFAULT_SIZE,
) : UIView(size), ViewLeaf
