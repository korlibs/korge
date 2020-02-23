package com.soywiz.korge.component.docking

import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korma.geom.*

fun <T : View> T.dockedTo(anchor: Anchor, scaleMode: ScaleMode = ScaleMode.NO_SCALE): T = this.apply {
    DockingComponent(this, anchor, scaleMode).attach()
}

class DockingComponent(override val view: View, var anchor: Anchor, var scaleMode: ScaleMode = ScaleMode.NO_SCALE) : ResizeComponent {
    val initialViewSize = Size(view.width, view.height)
    private val actualVirtualSize = Size(0, 0)
    private val targetSize = Size(0, 0)

    init {
        view.deferWithViews { views ->
            resized(views, views.actualWidth, views.actualHeight)
        }
    }

    override fun resized(views: Views, width: Int, height: Int) {
        view.position(
            views.actualVirtualLeft.toDouble() + (views.actualVirtualWidth) * anchor.sx,
            views.actualVirtualTop.toDouble() + (views.actualVirtualHeight) * anchor.sy
        )
        if (scaleMode != ScaleMode.NO_SCALE) {
            actualVirtualSize.setTo(views.actualVirtualWidth, views.actualVirtualHeight)
            val size = scaleMode.invoke(initialViewSize, actualVirtualSize, targetSize)
            view.setSize(size.width, size.height)
        }
        view.invalidate()
        view.parent?.invalidate()
    }
}
