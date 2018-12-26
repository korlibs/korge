package com.soywiz.korge.input

import com.soywiz.kds.*
import com.soywiz.korge.async.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import com.soywiz.korui.event.*

val View.drag by Extra.PropertyThis<View, DragComponent> { this.getOrCreateComponent { DragComponent(this) } }

inline fun <T : View?> T?.onDragStart(noinline handler: suspend (DragComponent.Info) -> Unit) =
	this.apply { this?.drag?.onDragStart?.addSuspend(KorgeDispatcher, handler) }

inline fun <T : View?> T?.onDragEnd(noinline handler: suspend (DragComponent.Info) -> Unit) =
	this.apply { this?.drag?.onDragEnd?.addSuspend(KorgeDispatcher, handler) }

inline fun <T : View?> T?.onDragMove(noinline handler: suspend (DragComponent.Info) -> Unit) =
	this.apply { this?.drag?.onDragMove?.addSuspend(KorgeDispatcher, handler) }

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
		val touch = e.touch
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
