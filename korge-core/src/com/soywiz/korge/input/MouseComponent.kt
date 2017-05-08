package com.soywiz.korge.input

import com.soywiz.korge.bitmapfont.drawText
import com.soywiz.korge.component.Component
import com.soywiz.korge.event.addEventListener
import com.soywiz.korge.view.*
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.async.Signal
import com.soywiz.korio.async.addSuspend
import com.soywiz.korio.util.Extra
import com.soywiz.korio.util.Once
import com.soywiz.korio.util.extraProperty
import com.soywiz.korma.geom.Point2d

class MouseComponent(view: View) : Component(view) {
	val input = views.input
	val onClick = Signal<MouseComponent>()
	val onOver = Signal<MouseComponent>()
	val onOut = Signal<MouseComponent>()
	val onDown = Signal<MouseComponent>()
	val onUp = Signal<MouseComponent>()
	val onUpOutside = Signal<MouseComponent>()
	val onUpAnywhere = Signal<MouseComponent>()
	val onMove = Signal<MouseComponent>()

	var hitTestType = View.HitTestType.BOUNDING

	val startedPos = Point2d()
	val lastPos = Point2d()
	val currentPos = Point2d()
	var hitTest: View? = null; private set
	private var lastOver = false
	private var lastPressing = false

	val CLICK_THRESHOLD = 10

	var Input.mouseHitSearch by Extra.Property { false}
	var Input.mouseHitResult by Extra.Property<View?> { null }
	var Input.mouseHitResultUsed by Extra.Property<View?> { null }
	var Views.mouseDebugHandlerOnce by Extra.Property { Once() }

	fun getMouseHitResult() = input.mouseHitResult

	var downPos = Point2d()
	var upPos = Point2d()
	var clickedCount = 0

	private fun hitTest(): View? {
		if (!input.mouseHitSearch) {
			input.mouseHitSearch = true
			input.mouseHitResult = views.stage.hitTest(views.nativeMouseX, views.nativeMouseY, hitTestType)
			//if (frame.mouseHitResult != null) {
			//val hitResult = frame.mouseHitResult!!
			//println("BOUNDS: $hitResult : " + hitResult.getLocalBounds() + " : " + hitResult.getGlobalBounds())
			//hitResult.dump()
			//}
		}
		return input.mouseHitResult
	}

	val isOver: Boolean get() {
		return hitTest?.hasAncestor(view) ?: false
	}

	init {
		addEventListener<MouseClickEvent> { e ->
			if (isOver) {
				onClick(this)
			}
			/*
			upPos.copyFrom(input.mouse)
			if (upPos.distanceTo(downPos) < CLICK_THRESHOLD) {
				clickedCount++
				if (isOver) {
					onClick(this)
				}
			}
			*/
		}
		addEventListener<MouseUpEvent> { e ->
			upPos.copyFrom(input.mouse)
			if (upPos.distanceTo(downPos) < CLICK_THRESHOLD) {
				clickedCount++
				//if (isOver) {
				//	onClick(this)
				//}
			}
		}
		addEventListener<MouseDownEvent> { e ->
			downPos.copyFrom(input.mouse)
		}
		addEventListener<MouseMovedEvent> { e ->
			//println(e)
		}
	}

	override fun update(dtMs: Int) {
		views.mouseDebugHandlerOnce {
			views.debugHandlers += {
				val mouseHit = hitTest()
				if (mouseHit != null) {
					val bounds = mouseHit.getLocalBounds()
					renderContext.batch.drawQuad(
						views.whiteTexture, x = bounds.x.toFloat(), y = bounds.y.toFloat(), width = bounds.width.toFloat(), height = bounds.height.toFloat(), colorMul = RGBA(0xFF, 0, 0, 0x3F),
						m = mouseHit.globalMatrix
					)
					renderContext.batch.drawText(defaultFont, 16.0, mouseHit.toString(), x = 0, y = 0)
				}

				val mouseHitResultUsed = input.mouseHitResultUsed
				if (mouseHitResultUsed != null) {
					val bounds = mouseHitResultUsed.getLocalBounds()
					renderContext.batch.drawQuad(
						views.whiteTexture, x = bounds.x.toFloat(), y = bounds.y.toFloat(), width = bounds.width.toFloat(), height = bounds.height.toFloat(), colorMul = RGBA(0x00, 0, 0xFF, 0x3F),
						m = mouseHitResultUsed.globalMatrix
					)
					renderContext.batch.drawText(defaultFont, 16.0, mouseHitResultUsed.toString(), x = 0, y = 16)
				}
			}
		}

		//println("${frame.mouseHitResult}")

		hitTest = hitTest()
		val over = isOver
		if (over) input.mouseHitResultUsed = view
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
		if (pressingChanged && !pressing) {
			if (over) onUp(this) else onUpOutside(this)
			onUpAnywhere(this)
			//if ((currentPos - startedPos).length < CLICK_THRESHOLD) onClick(this)
		}
		if (over && clickedCount > 0) {
			//onClick(this)
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
inline fun <T : View?> T?.onUpOutside(noinline handler: suspend (MouseComponent) -> Unit) = this.apply { this?.mouse?.onUpOutside?.addSuspend(this.views.coroutineContext, handler) }
inline fun <T : View?> T?.onUpAnywhere(noinline handler: suspend (MouseComponent) -> Unit) = this.apply { this?.mouse?.onUpAnywhere?.addSuspend(this.views.coroutineContext, handler) }
inline fun <T : View?> T?.onMove(noinline handler: suspend (MouseComponent) -> Unit) = this.apply { this?.mouse?.onMove?.addSuspend(this.views.coroutineContext, handler) }
