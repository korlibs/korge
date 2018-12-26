package com.soywiz.korge.input

import com.soywiz.kds.*
import com.soywiz.korge.async.*
import com.soywiz.korge.bitmapfont.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.event.*

class MouseEvents(override val view: View) : MouseComponent, UpdateComponentWithViews {
	val click = Signal<MouseEvents>()
	val over = Signal<MouseEvents>()
	val out = Signal<MouseEvents>()
	val down = Signal<MouseEvents>()
	val downFromOutside = Signal<MouseEvents>()
	val up = Signal<MouseEvents>()
	val upOutside = Signal<MouseEvents>()
	val upAnywhere = Signal<MouseEvents>()
	val move = Signal<MouseEvents>()
	val moveAnywhere = Signal<MouseEvents>()
	val mouseOutside = Signal<MouseEvents>()

	val onClick = click
	val onOver = over
	val onOut = out
	val onDown = down
	val onDownFromOutside = downFromOutside
	val onUp = up
	val onUpOutside = upOutside
	val onUpAnywhere = upAnywhere
	val onMove = move
	val onMoveAnywhere = moveAnywhere
	val onMoveOutside = mouseOutside

	val startedPos = Point()
	val lastPos = Point()
	val currentPos = Point()
	var hitTest: View? = null; private set
	private var lastOver = false
	private var lastPressing = false

	val CLICK_THRESHOLD = 16

	var Input.mouseHitSearch by Extra.Property { false }
	var Input.mouseHitResult by Extra.Property<View?> { null }
	var Input.mouseHitResultUsed by Extra.Property<View?> { null }
	var Views.mouseDebugHandlerOnce by Extra.Property { Once() }

	var downPos = Point()
	var upPos = Point()
	var clickedCount = 0

	private fun hitTest(views: Views): View? {
		if (!views.input.mouseHitSearch) {
			views.input.mouseHitSearch = true
			views.input.mouseHitResult = views.stage.hitTest(views.nativeMouseX, views.nativeMouseY)
			//if (frame.mouseHitResult != null) {
			//val hitResult = frame.mouseHitResult!!
			//println("BOUNDS: $hitResult : " + hitResult.getLocalBounds() + " : " + hitResult.getGlobalBounds())
			//hitResult.dump()
			//}
		}
		return views.input.mouseHitResult
	}

	val isOver: Boolean get() = hitTest?.hasAncestor(view) ?: false

	override fun onMouseEvent(views: Views, event: MouseEvent) {
		//println("MouseEvent.onMouseEvent($views, $event)")
		when (event.type) {
			MouseEvent.Type.UP -> {
				upPos.copyFrom(views.input.mouse)
				if (upPos.distanceTo(downPos) < CLICK_THRESHOLD) {
					clickedCount++
					//if (isOver) {
					//	onClick(this)
					//}
				}
			}
			MouseEvent.Type.DOWN -> {
				downPos.copyFrom(views.input.mouse)
			}
			MouseEvent.Type.CLICK -> {
				if (isOver) {
					onClick(this@MouseEvents)
					if (onClick.listenerCount > 0) {
						preventDefault(view)
					}
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
			else -> {
			}
		}
	}

	override fun update(views: Views, ms: Double) {
		val dtMs = ms.toInt()
		if (!view.mouseEnabled) return

		views.mouseDebugHandlerOnce {
			views.debugHandlers += { ctx ->
				val mouseHit = hitTest(views)
				if (mouseHit != null) {
					val bounds = mouseHit.getLocalBounds()
					renderContext.batch.drawQuad(
						ctx.getTex(Bitmaps.white),
						x = bounds.x.toFloat(),
						y = bounds.y.toFloat(),
						width = bounds.width.toFloat(),
						height = bounds.height.toFloat(),
						colorMulInt = RGBAInt(0xFF, 0, 0, 0x3F),
						m = mouseHit.globalMatrix
					)
					renderContext.drawText(
						Fonts.defaultFont,
						16.0,
						mouseHit.toString() + " : " + views.nativeMouseX + "," + views.nativeMouseY,
						x = 0,
						y = 0
					)
				}

				val mouseHitResultUsed = input.mouseHitResultUsed
				if (mouseHitResultUsed != null) {
					val bounds = mouseHitResultUsed.getLocalBounds()
					renderContext.batch.drawQuad(
						ctx.getTex(Bitmaps.white),
						x = bounds.x.toFloat(),
						y = bounds.y.toFloat(),
						width = bounds.width.toFloat(),
						height = bounds.height.toFloat(),
						colorMulInt = RGBAInt(0x00, 0, 0xFF, 0x3F),
						m = mouseHitResultUsed.globalMatrix
					)
					var vview = mouseHitResultUsed
					var yy = 16
					while (vview != null) {
						renderContext.drawText(Fonts.defaultFont, 16.0, vview.toString(), x = 0, y = yy)
						vview = vview?.parent
						yy += 16
					}
				}
			}
		}

		//println("${frame.mouseHitResult}")

		hitTest = hitTest(views)
		val over = isOver
		if (over) views.input.mouseHitResultUsed = view
		val pressing = views.input.mouseButtons != 0
		val overChanged = (lastOver != over)
		val pressingChanged = pressing != lastPressing
		view.globalToLocal(views.input.mouse, currentPos)

		//println("$hitTest, ${input.mouse}, $over, $pressing, $overChanged, $pressingChanged")

		//println("MouseComponent: $hitTest, $over")

		if (!overChanged && over && currentPos != lastPos) onMove(this)
		if (!overChanged && !over && currentPos != lastPos) onMoveOutside(this)
		if (currentPos != lastPos) onMoveAnywhere(this)
		if (overChanged && over) onOver(this)
		if (overChanged && !over) onOut(this)
		if (over && pressingChanged && pressing) {
			startedPos.copyFrom(currentPos)
			onDown(this)
		}
		if (overChanged && pressing) {
			onDownFromOutside(this)
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

val View.mouse by Extra.PropertyThis<View, MouseEvents> { this.getOrCreateComponent { MouseEvents(this) } }
inline fun <T> View.mouse(callback: MouseEvents.() -> T): T = mouse.run(callback)

inline fun <T : View?> T?.onClick(noinline handler: suspend (MouseEvents) -> Unit) =
	this.apply { this?.mouse?.onClick?.addSuspend(KorgeDispatcher, handler) }

inline fun <T : View?> T?.onOver(noinline handler: suspend (MouseEvents) -> Unit) =
	this.apply { this?.mouse?.onOver?.addSuspend(KorgeDispatcher, handler) }

inline fun <T : View?> T?.onOut(noinline handler: suspend (MouseEvents) -> Unit) =
	this.apply { this?.mouse?.onOut?.addSuspend(KorgeDispatcher, handler) }

inline fun <T : View?> T?.onDown(noinline handler: suspend (MouseEvents) -> Unit) =
	this.apply { this?.mouse?.onDown?.addSuspend(KorgeDispatcher, handler) }

inline fun <T : View?> T?.onDownFromOutside(noinline handler: suspend (MouseEvents) -> Unit) =
	this.apply { this?.mouse?.onDownFromOutside?.addSuspend(KorgeDispatcher, handler) }

inline fun <T : View?> T?.onUp(noinline handler: suspend (MouseEvents) -> Unit) =
	this.apply { this?.mouse?.onUp?.addSuspend(KorgeDispatcher, handler) }

inline fun <T : View?> T?.onUpOutside(noinline handler: suspend (MouseEvents) -> Unit) =
	this.apply { this?.mouse?.onUpOutside?.addSuspend(KorgeDispatcher, handler) }

inline fun <T : View?> T?.onUpAnywhere(noinline handler: suspend (MouseEvents) -> Unit) =
	this.apply { this?.mouse?.onUpAnywhere?.addSuspend(KorgeDispatcher, handler) }

inline fun <T : View?> T?.onMove(noinline handler: suspend (MouseEvents) -> Unit) =
	this.apply { this?.mouse?.onMove?.addSuspend(KorgeDispatcher, handler) }
