package com.soywiz.korge.component.docking

import com.soywiz.korge.component.Component
import com.soywiz.korge.event.addEventListener
import com.soywiz.korge.view.StageResizedEvent
import com.soywiz.korge.view.View
import com.soywiz.korma.geom.Anchor

class DockingComponent(view: View, var anchor: Anchor) : Component(view) {
	//private val bounds = Rectangle()

	init {
		detatchCancellables += view.addEventListener<StageResizedEvent> { e ->
			view.x = views.actualVirtualLeft.toDouble() + (views.actualVirtualWidth) * anchor.sx
			view.y = views.actualVirtualTop.toDouble() + (views.actualVirtualHeight) * anchor.sy
			view.invalidate()
			view.parent?.invalidate()
		}
	}
}

fun <T : View> T.dockedTo(anchor: Anchor) = this.apply { DockingComponent(this, anchor).attach() }
