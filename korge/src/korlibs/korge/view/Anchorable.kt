package korlibs.korge.view

import korlibs.korge.view.property.*
import korlibs.math.geom.*

interface Anchorable {
    @ViewProperty(name = "anchor")
    var anchor: Anchor

    @ViewProperty
    val anchorActions: ViewActionList get() = ViewActionList(
        ViewAction("TL") { view ->
            views.undoable("Change anchor", view as View) {
                (view as Anchorable).anchor(Anchor.TOP_LEFT)
            }
        },
        ViewAction("Center") { view ->
            views.undoable("Change anchor", view as View) {
                (view as Anchorable).anchor(Anchor.CENTER)
            }
        },
    )
}

fun <T : Anchorable> T.anchor(anchor: Anchor): T {
    this.anchor = anchor
    return this
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T : Anchorable> T.anchor(ax: Number, ay: Number): T = anchor(Anchor(ax.toDouble(), ay.toDouble()))

fun <T : Anchorable> T.center(): T = anchor(0.5, 0.5)
val <T : Anchorable> T.centered: T get() = center()

interface PixelAnchorable {
    @ViewProperty(name = "anchorPixel")
    var anchorPixel: Point
}

fun <T : PixelAnchorable> T.anchorPixel(point: Point): T {
    this.anchorPixel = point
    return this
}

