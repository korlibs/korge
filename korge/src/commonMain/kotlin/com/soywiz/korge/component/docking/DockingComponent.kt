package com.soywiz.korge.component.docking

import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*

fun <T : View> T.dockedTo(anchor: Anchor, scaleMode: ScaleMode = ScaleMode.NO_SCALE, offset: MPoint = MPoint(), hook: (View) -> Unit = {}): T {
    DockingComponent(this, anchor, scaleMode, MPoint().copyFrom(offset), hook).attach()
    return this
}

class DockingComponent(
    override val view: View,
    var anchor: Anchor,
    var scaleMode: ScaleMode = ScaleMode.NO_SCALE,
    val offset: MPoint = MPoint(),
    val hook: (View) -> Unit
) :
    ResizeComponent {
    val initialViewSize = MSize(view.width, view.height)
    private val actualVirtualSize = MSize(0, 0)
    private val targetSize = MSize(0, 0)

    init {
        view.deferWithViews { views ->
            resized(views, views.actualVirtualWidth, views.actualVirtualHeight)
        }
    }

    override fun resized(views: Views, width: Int, height: Int) {
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
            actualVirtualSize.setTo(views.actualVirtualWidth, views.actualVirtualHeight)
            val size = scaleMode.invoke(initialViewSize, actualVirtualSize, targetSize)
            view.setSize(size.width, size.height)
        }
        view.invalidate()
        view.parent?.invalidate()
        hook(view)
    }
}
