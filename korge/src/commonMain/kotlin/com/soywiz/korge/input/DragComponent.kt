package com.soywiz.korge.input

import com.soywiz.kds.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import com.soywiz.korev.*
import kotlin.coroutines.*

@Deprecated("Use onMouseDrag() instead")
val View.drag by Extra.PropertyThis<View, DragComponent> { this.getOrCreateComponent { DragComponent(this) } }

@Deprecated("Use onMouseDrag() instead", ReplaceWith(
    "onMouseDrag {\nif (it.start) handler\n}",
    "com.soywiz.korge.input.onMouseDrag"
))
suspend inline fun <T : View?> T?.onDragStart(noinline handler: suspend (DragComponent.Info) -> Unit): T? {
    this?.drag?.onDragStart?.addSuspend(coroutineContext, handler)
    return this
}

@Deprecated("Use onMouseDrag() instead", ReplaceWith(
    "onMouseDrag {\nif (it.end) handler\n}",
    "com.soywiz.korge.input.onMouseDrag"
))
suspend inline fun <T : View?> T?.onDragEnd(noinline handler: suspend (DragComponent.Info) -> Unit): T? {
    this?.drag?.onDragEnd?.addSuspend(coroutineContext, handler)
    return this
}

@Deprecated("Use onMouseDrag() instead", ReplaceWith(
    "onMouseDrag(handler)",
    "com.soywiz.korge.input.onMouseDrag"
))
suspend inline fun <T : View?> T?.onDragMove(noinline handler: suspend (DragComponent.Info) -> Unit): T? {
    this?.drag?.onDragMove?.addSuspend(coroutineContext, handler)
    return this
}

@Deprecated("Use onMouseDrag() instead")
class DragComponent(override val view: View) : TouchComponent {
	data class Info(
		var touch: Touch = Touch.dummy,
		var gstart: Point = Point(),
		var gend: Point = Point(),
		var delta: Point = Point()
	) {
		val id get() = touch.id
	}

	var Touch.dragging by extraProperty("DragComponent.dragging") { false }

	val info = Info()
	val onDragStart = Signal<Info>()
	val onDragMove = Signal<Info>()
	val onDragEnd = Signal<Info>()

	private fun updateStartEndPos(touch: Touch) {
		info.gstart.copyFrom(touch.current)
		info.gend.copyFrom(touch.current)
		info.delta.setToSub(info.gend, info.gstart)
	}

	private fun updateEndPos(touch: Touch) {
		info.gend.copyFrom(touch.current)
		info.delta.setToSub(info.gend, info.gstart)
	}

	override fun onTouchEvent(views: Views, e: TouchEvent) {
		val touch = e.touches.first()
		info.touch = touch
		when (e.type) {
			TouchEvent.Type.START -> {
				if (view.hitTest(touch.current.x, touch.current.y) != null) {
					touch.dragging = true
					updateStartEndPos(touch)
					onDragStart(info)
				}
			}
			TouchEvent.Type.END -> {
				if (touch.dragging) {
					touch.dragging = false
					updateEndPos(touch)
					onDragEnd(info)
				}
			}
			else -> {
				if (touch.dragging) {
					updateEndPos(touch)
					onDragMove(info)
				}
			}
		}
	}
}
