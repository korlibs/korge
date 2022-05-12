package com.soywiz.korge.input

import com.soywiz.klock.DateTime
import com.soywiz.klock.TimeProvider
import com.soywiz.klock.TimeSpan
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.xy
import com.soywiz.korio.lang.Closeable
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

data class OnMouseDragCloseable(
    val onDownCloseable: Closeable,
    val onUpAnywhereCloseable: Closeable,
    val onMoveAnywhereCloseable: Closeable
) : Closeable {
    override fun close() {
        onDownCloseable.close()
        onUpAnywhereCloseable.close()
        onMoveAnywhereCloseable.close()
    }
}

private fun <T : View> T.onMouseDragInternal(
    timeProvider: TimeProvider = TimeProvider, info:
    MouseDragInfo = MouseDragInfo(this), callback: Views.(MouseDragInfo) -> Unit
): Pair<T, OnMouseDragCloseable> {
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
        mousePos.copyFrom(views.globalMouseXY)
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

    lateinit var onDownCloseable: Closeable
    lateinit var onUpAnywhereCloseable: Closeable
    lateinit var onMoveAnywhereCloseable: Closeable
    this.mouse {
        onDownCloseable = onDownCloseable { handle(it, MouseDragState.START) }
        onUpAnywhereCloseable = onUpAnywhereCloseable { handle(it, MouseDragState.END) }
        onMoveAnywhereCloseable = onMoveAnywhereCloseable { handle(it, MouseDragState.DRAG) }
    }
    return this to OnMouseDragCloseable(
        onDownCloseable,
        onUpAnywhereCloseable,
        onMoveAnywhereCloseable
    )
}

fun <T : View> T.onMouseDragCloseable(
    timeProvider: TimeProvider = TimeProvider, info:
    MouseDragInfo = MouseDragInfo(this), callback: Views.(MouseDragInfo) -> Unit
): OnMouseDragCloseable = onMouseDragInternal(timeProvider, info, callback).second

fun <T : View> T.onMouseDrag(
    timeProvider: TimeProvider = TimeProvider,
    info: MouseDragInfo = MouseDragInfo(this),
    callback: Views.(MouseDragInfo) -> Unit
): T = onMouseDragInternal(timeProvider, info, callback).first

open class DraggableInfo(view: View) : MouseDragInfo(view) {
    val viewStartXY = Point()

    var viewStartX: Double
        get() = viewStartXY.x;
        set(value) {
            viewStartXY.x = value
        }
    var viewStartY: Double
        get() = viewStartXY.y;
        set(value) {
            viewStartXY.y = value
        }

    val viewPrevXY = Point()

    var viewPrevX: Double
        get() = viewPrevXY.x;
        set(value) {
            viewPrevXY.x = value
        }
    var viewPrevY: Double
        get() = viewPrevXY.y;
        set(value) {
            viewPrevXY.y = value
        }

    val viewNextXY = Point()

    var viewNextX: Double
        get() = viewNextXY.x;
        set(value) {
            viewNextXY.x = value
        }
    var viewNextY: Double
        get() = viewNextXY.y;
        set(value) {
            viewNextXY.y = value
        }

    val viewDeltaXY = Point()

    var viewDeltaX: Double
        get() = viewDeltaXY.x;
        set(value) {
            viewDeltaXY.x = value
        }
    var viewDeltaY: Double
        get() = viewDeltaXY.y;
        set(value) {
            viewDeltaXY.y = value
        }
}

data class DraggableCloseable(
    val onMouseDragCloseable: Closeable
): Closeable {
    override fun close() {
        onMouseDragCloseable.close()
    }
}

private fun <T : View> T.draggableInternal(
    selector: View = this,
    autoMove: Boolean = true,
    onDrag: ((DraggableInfo) -> Unit)? = null
): Pair<T, DraggableCloseable> {
    val view = this
    val info = DraggableInfo(view)
    val onMouseDragCloseable = selector.onMouseDragCloseable(info = info) {
        if (info.start) {
            info.viewStartXY.copyFrom(view.pos)
        }
        //println("localDXY=${info.localDX(view)},${info.localDY(view)}")
        info.viewPrevXY.copyFrom(view.pos)
        info.viewNextXY.setTo(
            info.viewStartX + info.localDX(view),
            info.viewStartY + info.localDY(view)
        )
        info.viewDeltaXY.setTo(info.viewNextX - info.viewPrevX, info.viewNextY - info.viewPrevY)
        if (autoMove) {
            view.xy(info.viewNextXY)
        }
        onDrag?.invoke(info)
        //println("DRAG: $dx, $dy, $start, $end")
    }
    return this to DraggableCloseable(onMouseDragCloseable)
}

fun <T : View> T.draggableCloseable(
    selector: View = this,
    autoMove: Boolean = true,
    onDrag: ((DraggableInfo) -> Unit)? = null
): DraggableCloseable = draggableInternal(selector, autoMove, onDrag).second

fun <T : View> T.draggable(
    selector: View = this,
    autoMove: Boolean = true,
    onDrag: ((DraggableInfo) -> Unit)? = null
): T = draggableInternal(selector, autoMove, onDrag).first
