package korlibs.korge.component.docking

import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*

fun <T : View> T.dockedTo(anchor: Anchor, scaleMode: ScaleMode = ScaleMode.NO_SCALE, offset: Point = Point(), hook: (View) -> Unit = {}): T {
    DockingComponent(this, anchor, scaleMode, offset, hook)
    return this
}

class DockingComponent(
    val view: View,
    var anchor: Anchor,
    var scaleMode: ScaleMode = ScaleMode.NO_SCALE,
    val offset: Point = Point(),
    val hook: (View) -> Unit
) {
    val initialViewSize = Size(view.widthD, view.heightD)
    private var actualVirtualSize = Size(0, 0)
    private var targetSize = Size(0, 0)

    init {
        view.onStageResized { width, height ->
            //println(views.actualVirtualWidth)
            view.position(
                anchor.ratioX.interpolate(views.virtualLeft, views.virtualRight) + offset.x,
                anchor.ratioY.interpolate(views.virtualTop, views.virtualBottom) + offset.y,
                //views.actualVirtualBounds.getAnchoredPosition(anchor) + offset
            )
            // @TODO: This is not working? why?
            //view.alignX(views.stage, anchor.sx, true)
            //view.alignY(views.stage, anchor.sy, true)
            if (scaleMode != ScaleMode.NO_SCALE) {
                actualVirtualSize = Size(views.actualVirtualWidth, views.actualVirtualHeight)
                val size = scaleMode.invoke(initialViewSize, actualVirtualSize)
                targetSize = size
                view.size(size)
            }
            view.invalidate()
            view.parent?.invalidate()
            hook(view)
        }
    }
}
