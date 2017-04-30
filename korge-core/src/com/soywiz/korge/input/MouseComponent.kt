package com.soywiz.korge.input

import com.soywiz.korge.component.Component
import com.soywiz.korge.event.addEventListener
import com.soywiz.korge.view.*
import com.soywiz.korio.async.Signal
import com.soywiz.korio.async.addSuspend
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

	var downPos = Point2d()
	var upPos = Point2d()
	var clickedCount = 0

	init {
		detatchCancellables += view.addEventListener<MouseUpEvent> { e ->
			upPos.copyFrom(input.mouse)
			if (upPos.distanceTo(downPos) < CLICK_THRESHOLD) {
				clickedCount++
			}
		}
		detatchCancellables += view.addEventListener<MouseDownEvent> { e ->
			downPos.copyFrom(input.mouse)
		}
		detatchCancellables += view.addEventListener<MouseMovedEvent> { e ->
			//println(e)
		}
	}

	override fun update(dtMs: Int) {
		//println("${frame.mouseHitResult}")
		if (!frame.mouseHitSearch) {
			frame.mouseHitSearch = true
			frame.mouseHitResult = views.stage.hitTest(views.nativeMouseX, views.nativeMouseY, hitTestType)
			//if (frame.mouseHitResult != null) {
			//val hitResult = frame.mouseHitResult!!
			//println("BOUNDS: $hitResult : " + hitResult.getLocalBounds() + " : " + hitResult.getGlobalBounds())
			//hitResult.dump()
			//}
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
			//if ((currentPos - startedPos).length < CLICK_THRESHOLD) onClick(this)
		}
		if (over && clickedCount > 0) {
			onClick(this)
		}

		lastOver = over
		lastPressing = pressing
		lastPos.copyFrom(currentPos)
		clickedCount = 0
	}
}

//var Input.Frame.mouseHitResult by Extra.Property<View?>("mouseHitResult") {
//    views.root.hitTest(input.mouse)
//}


//var View.mouseEnabled by Extra.Property { true }

val View.mouse by Extra.PropertyThis<View, MouseComponent> { this.getOrCreateComponent { MouseComponent(this) } }

inline fun <T : View?> T?.onClick(noinline handler: suspend (MouseComponent) -> Unit) = this.apply { this?.mouse?.onClick?.addSuspend(this.views.coroutineContext, handler) }
inline fun <T : View?> T?.onOver(noinline handler: suspend (MouseComponent) -> Unit) = this.apply { this?.mouse?.onOver?.addSuspend(this.views.coroutineContext, handler) }
inline fun <T : View?> T?.onOut(noinline handler: suspend (MouseComponent) -> Unit) = this.apply { this?.mouse?.onOut?.addSuspend(this.views.coroutineContext, handler) }
inline fun <T : View?> T?.onDown(noinline handler: suspend (MouseComponent) -> Unit) = this.apply { this?.mouse?.onDown?.addSuspend(this.views.coroutineContext, handler) }
inline fun <T : View?> T?.onUp(noinline handler: suspend (MouseComponent) -> Unit) = this.apply { this?.mouse?.onUp?.addSuspend(this.views.coroutineContext, handler) }
inline fun <T : View?> T?.onMove(noinline handler: suspend (MouseComponent) -> Unit) = this.apply { this?.mouse?.onMove?.addSuspend(this.views.coroutineContext, handler) }
