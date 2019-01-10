package com.soywiz.korge.component.docking

import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korma.geom.*
import com.soywiz.korev.*

class DockingComponent(override val view: View, var anchor: Anchor) : ResizeComponent {
	//private val bounds = Rectangle()

	override fun resized(views: Views, width: Int, height: Int) {
		view.x = views.actualVirtualLeft.toDouble() + (views.actualVirtualWidth) * anchor.sx
		view.y = views.actualVirtualTop.toDouble() + (views.actualVirtualHeight) * anchor.sy
		view.invalidate()
		view.parent?.invalidate()
	}
}

fun <T : View> T.dockedTo(anchor: Anchor) = DockingComponent(this, anchor).attach()
