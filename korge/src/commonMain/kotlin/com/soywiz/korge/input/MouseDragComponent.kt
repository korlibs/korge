package com.soywiz.korge.input

import com.soywiz.klock.*
import com.soywiz.korge.view.*
import com.soywiz.korma.geom.Point

data class MouseDragInfo(
    val view: View,
    var dx: Double = 0.0,
    var dy: Double = 0.0,
    var start: Boolean = false,
    var end: Boolean = false,
    var startTime: DateTime = DateTime.EPOCH,
    var time: DateTime = DateTime.EPOCH,
) {
    val elapsed: TimeSpan get() = time - startTime

    val localDX get() = view.parent?.globalToLocalDX(0.0, 0.0, dx, dy) ?: dx
    val localDY get() = view.parent?.globalToLocalDY(0.0, 0.0, dx, dy) ?: dy

    fun set(dx: Double, dy: Double, start: Boolean, end: Boolean, time: DateTime): MouseDragInfo {
        this.dx = dx
        this.dy = dy
        this.start = start
        this.end = end
        if (start) this.startTime = time
        this.time = time
        return this
    }
}

fun <T : View> T.onMouseDrag(timeProvider: TimeProvider = TimeProvider, callback: Views.(MouseDragInfo) -> Unit): T {
    var dragging = false
    var sx = 0.0
    var sy = 0.0
    var cx = 0.0
    var cy = 0.0
    val view = this

    val info = MouseDragInfo(view)
    val mousePos = Point()

    fun views() = view.stage!!.views

    fun updateMouse() {
        val views = views()
        mousePos.setTo(
            views.nativeMouseX,
            views.nativeMouseY
        )
    }

    this.mouse {
        onDown {
            updateMouse()
            dragging = true
            sx = mousePos.x
            sy = mousePos.y
            info.dx = 0.0
            info.dy = 0.0
            callback(views(), info.set(0.0, 0.0, true, false, timeProvider.now()))
        }
        onUpAnywhere {
            if (dragging) {
                updateMouse()
                dragging = false
                cx = mousePos.x
                cy = mousePos.y
                callback(views(), info.set(cx - sx, cy - sy, false, true, timeProvider.now()))
            }
        }
        onMoveAnywhere {
            if (dragging) {
                updateMouse()
                cx = mousePos.x
                cy = mousePos.y
                callback(views(), info.set(cx - sx, cy - sy, false, false, timeProvider.now()))
            }
        }
    }
    return this
}

fun <T : View> T.draggable(selector: View = this, onDrag: ((MouseDragInfo) -> Unit)? = null): T {
    val view = this
    var sx = 0.0
    var sy = 0.0
    selector.onMouseDrag { info ->
        if (info.start) {
            sx = view.x
            sy = view.y
        }
        view.x = sx + info.localDX
        view.y = sy + info.localDY
        onDrag?.invoke(info)
        //println("DRAG: $dx, $dy, $start, $end")
    }
    return this
}
