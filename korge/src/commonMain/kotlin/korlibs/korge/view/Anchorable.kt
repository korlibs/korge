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

fun <T : Anchorable> T.anchor(ax: Float, ay: Float): T = anchor(Anchor(ax, ay))
fun <T : Anchorable> T.anchor(ax: Double, ay: Double): T = anchor(Anchor(ax, ay))
fun <T : Anchorable> T.anchor(ax: Int, ay: Int): T = anchor(Anchor(ax, ay))

fun <T : Anchorable> T.center(): T = anchor(0.5f, 0.5f)
val <T : Anchorable> T.centered: T get() = anchor(0.5f, 0.5f)

interface PixelAnchorable {
    @ViewProperty(name = "anchorPixel")
    var anchorPixel: Point
}

fun <T : PixelAnchorable> T.anchorPixel(point: Point): T {
    this.anchorPixel = point
    return this
}

