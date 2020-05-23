package com.soywiz.korge.input

import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korma.geom.Point

data class MouseDragInfo(
    var dx: Double = 0.0,
    var dy: Double = 0.0,
    var start: Boolean = false,
    var end: Boolean = false
) {
    fun set(dx: Double, dy: Double, start: Boolean, end: Boolean): MouseDragInfo {
        this.dx = dx
        this.dy = dy
        this.start = start
        this.end = end
        return this
    }
}

fun <T : View> T.onMouseDrag(callback: Views.(MouseDragInfo) -> Unit): T {
    var dragging = false
    var sx = 0.0
    var sy = 0.0
    var cx = 0.0
    var cy = 0.0
    val view = this

    val info = MouseDragInfo()
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
            callback(views(), info.set(0.0, 0.0, true, false))
        }
        onUpAnywhere {
            if (dragging) {
                updateMouse()
                dragging = false
                cx = mousePos.x
                cy = mousePos.y
                callback(views(), info.set(cx - sx, cy - sy, false, true))
            }
        }
        onMoveAnywhere {
            if (dragging) {
                updateMouse()
                cx = mousePos.x
                cy = mousePos.y
                callback(views(), info.set(cx - sx, cy - sy, false, false))
            }
        }
    }
    return this
}

fun <T : View> T.draggable(): T {
    val view = this
    var sx = 0.0
    var sy = 0.0
    onMouseDrag { info ->
        if (info.start) {
            sx = view.x
            sy = view.y
        }
        view.x = sx + info.dx
        view.y = sy + info.dy
        //println("DRAG: $dx, $dy, $start, $end")
    }
    return this
}
