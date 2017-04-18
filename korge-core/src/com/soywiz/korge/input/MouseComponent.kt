package com.soywiz.korge.input

import com.soywiz.korge.component.Component
import com.soywiz.korge.view.View
import com.soywiz.korge.view.hasAncestor
import com.soywiz.korio.async.Signal
import com.soywiz.korio.util.Extra
import com.soywiz.korio.util.extraProperty
import com.soywiz.korma.geom.Point2d

class MouseComponent(view: View) : Component(view) {
	val input = views.input
	val frame = input.frame
	val onClick = Signal<MouseComponent>()
	val onOver = Signal<MouseComponent>()
	val onOut = Signal<MouseComponent>()
	val onDown = Signal<MouseComponent>()
	val onUp = Signal<MouseComponent>()
	val onMove = Signal<MouseComponent>()

	var hitTestType = View.HitTestType.BOUNDING

	val startedPos = Point2d()
	val lastPos = Point2d()
	val currentPos = Point2d()
	var hitTest: View? = null; private set
	private var lastOver = false
	private var lastPressing = false

	val CLICK_THRESHOLD = 10

	var Input.Frame.mouseHitSearch by extraProperty("mouseHitSearch", false)
	var Input.Frame.mouseHitResult by extraProperty<View?>("mouseHitResult", null)

	override fun update(dtMs: Int) {
		//println("${frame.mouseHitResult}")
		if (!frame.mouseHitSearch) {
			frame.mouseHitSearch = true
			frame.mouseHitResult = views.stage.hitTest(views.nativeMouseX, views.nativeMouseY, hitTestType)
		}
		hitTest = input.frame.mouseHitResult
		val over = hitTest?.hasAncestor(view) ?: false
		val pressing = input.mouseButtons != 0
		val overChanged = (lastOver != over)
		val pressingChanged = pressing != lastPressing
		view.globalToLocal(input.mouse, currentPos)

		//println("$hitTest, ${input.mouse}, $over, $pressing, $overChanged, $pressingChanged")

		//println("MouseComponent: $hitTest, $over")

		if (!overChanged && over && currentPos != lastPos) onMove(this)
		if (overChanged && over) onOver(this)
		if (overChanged && !over) onOut(this)
		if (over && pressingChanged && pressing) {
			startedPos.copyFrom(currentPos)
			onDown(this)
		}
		if (over && pressingChanged && !pressing) {
			onUp(this)
			if ((currentPos - startedPos).length < CLICK_THRESHOLD) onClick(this)
		};

		lastOver = over
		lastPressing = pressing
		lastPos.copyFrom(currentPos)
	}
}

//var Input.Frame.mouseHitResult by Extra.Property<View?>("mouseHitResult") {
//    views.root.hitTest(input.mouse)
//}


//var View.mouseEnabled by Extra.Property { true }

val View.mouse get() = this.getOrCreateComponent { MouseComponent(this) }

val View.onClick get() = mouse.onClick
val View.onOver get() = mouse.onOver
val View.onOut get() = mouse.onOut
val View.onDown get() = mouse.onDown
val View.onUp get() = mouse.onUp
val View.onMove get() = mouse.onMove
