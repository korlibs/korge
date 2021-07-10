package com.soywiz.korge.input

import com.soywiz.klock.*
import com.soywiz.korge.view.*
import com.soywiz.korma.geom.Point

open class MouseDragInfo(
    val view: View,
    var dx: Double = 0.0,
    var dy: Double = 0.0,
    var start: Boolean = false,
    var end: Boolean = false,
    var startTime: DateTime = DateTime.EPOCH,
    var time: DateTime = DateTime.EPOCH,
) {
    lateinit var mouseEvents: MouseEvents
    val elapsed: TimeSpan get() = time - startTime

    val localDX get() = localDX(view)
    val localDY get() = localDY(view)

    fun localDX(view: View) = view.parent?.globalToLocalDX(0.0, 0.0, dx, dy) ?: dx
    fun localDY(view: View) = view.parent?.globalToLocalDY(0.0, 0.0, dx, dy) ?: dy

    private var lastDx: Double = Double.NaN
    private var lastDy: Double = Double.NaN

    var deltaDx: Double = 0.0
    var deltaDy: Double = 0.0

    fun reset() {
        lastDx = Double.NaN
        lastDy = Double.NaN
        deltaDx = 0.0
        deltaDy = 0.0
        dx = 0.0
        dy = 0.0
    }

    fun set(dx: Double, dy: Double, start: Boolean, end: Boolean, time: DateTime): MouseDragInfo {
        this.dx = dx
        this.dy = dy
        if (!lastDx.isNaN() && !lastDy.isNaN()) {
            this.deltaDx = lastDx - dx
            this.deltaDy = lastDy - dy
        }
        this.lastDx = dx
        this.lastDy = dy
        this.start = start
        this.end = end
        if (start) this.startTime = time
        this.time = time
        return this
    }
}

enum class MouseDragState {
    START, DRAG, END;

    val isDrag get() = this == DRAG
    val isStart get() = this == START
    val isEnd get() = this == END
}

fun <T : View> T.onMouseDrag(timeProvider: TimeProvider = TimeProvider, info: MouseDragInfo = MouseDragInfo(this), callback: Views.(MouseDragInfo) -> Unit): T {
    var dragging = false
    var sx = 0.0
    var sy = 0.0
    var cx = 0.0
    var cy = 0.0
    val view = this

    val mousePos = Point()

    fun views() = view.stage!!.views

    fun updateMouse() {
        val views = views()
        //println("views.globalMouse=${views.globalMouseXY}, views.nativeMouse=${views.nativeMouseXY}")
        //mousePos.copyFrom(views.globalMouseXY)
        mousePos.copyFrom(views.nativeMouseXY)
    }

    fun handle(it: MouseEvents, state: MouseDragState) {
        if (state != MouseDragState.START && !dragging) return
        updateMouse()
        info.mouseEvents = it
        val px = mousePos.x
        val py = mousePos.y
        when (state) {
            MouseDragState.START -> {
                dragging = true
                sx = px
                sy = py
                info.reset()
            }
            MouseDragState.END -> {
                dragging = false
            }
        }
        cx = mousePos.x
        cy = mousePos.y
        val dx = cx - sx
        val dy = cy - sy
        callback(views(), info.set(dx, dy, state.isStart, state.isEnd, timeProvider.now()))
    }

    this.mouse {
        onDown { handle(it, MouseDragState.START) }
        onUpAnywhere { handle(it, MouseDragState.END) }
        onMoveAnywhere { handle(it, MouseDragState.DRAG) }
    }
    return this
}

open class DraggableInfo(view: View) : MouseDragInfo(view) {
    var viewStartX: Double = 0.0
    var viewStartY: Double = 0.0
}

fun <T : View> T.draggable(selector: View = this, onDrag: ((DraggableInfo) -> Unit)? = null): T {
    val view = this
    val info = DraggableInfo(view)
    selector.onMouseDrag(info = info) {
        if (info.start) {
            info.viewStartX = view.x
            info.viewStartY = view.y
        }
        //println("localDXY=${info.localDX(view)},${info.localDY(view)}")
        view.x = info.viewStartX + info.localDX(view)
        view.y = info.viewStartY + info.localDY(view)
        onDrag?.invoke(info)
        //println("DRAG: $dx, $dy, $start, $end")
    }
    return this
}
