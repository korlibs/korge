@file:OptIn(ExperimentalStdlibApi::class)

package korlibs.korge.input

import korlibs.time.DateTime
import korlibs.time.TimeProvider
import korlibs.time.TimeSpan
import korlibs.korge.view.View
import korlibs.korge.view.Views
import korlibs.korge.view.xy
import korlibs.math.geom.*

open class MouseDragInfo(
    val view: View,
    var dx: Float = 0f,
    var dy: Float = 0f,
    var start: Boolean = false,
    var end: Boolean = false,
    var startTime: DateTime = DateTime.EPOCH,
    var time: DateTime = DateTime.EPOCH,
    var sx: Float = 0f,
    var sy: Float = 0f,
    var cx: Float = 0f,
    var cy: Float = 0f,
) {
    override fun toString(): String = "MouseDragInfo(start=$start, end=$end, sx=$sx, sy=$sy, cx=$cx, cy=$cy)"

    lateinit var mouseEvents: MouseEvents
    val elapsed: TimeSpan get() = time - startTime

    val localDXY: Point get() = localDXY(view)
    @Deprecated("") val localDX: Float get() = localDX(view)
    @Deprecated("") val localDY: Float get() = localDY(view)

    fun localDXY(view: View): Point = view.parent?.globalToLocalDelta(Point(0.0, 0.0), Point(dx, dy)) ?: Point(dx, dy)
    @Deprecated("") fun localDX(view: View): Float = localDXY(view).x
    @Deprecated("") fun localDY(view: View): Float = localDXY(view).y

    private var lastDx: Float = Float.NaN
    private var lastDy: Float = Float.NaN

    var deltaDx: Float = 0f
    var deltaDy: Float = 0f

    fun reset() {
        lastDx = Float.NaN
        lastDy = Float.NaN
        deltaDx = 0f
        deltaDy = 0f
        dx = 0f
        dy = 0f
        sx = 0f
        sy = 0f
        cx = 0f
        cy = 0f
    }

    fun set(dx: Float, dy: Float, start: Boolean, end: Boolean, time: DateTime, sx: Float, sy: Float, cx: Float, cy: Float): MouseDragInfo {
        this.dx = dx
        this.dy = dy
        this.sx = sx
        this.sy = sy
        this.cx = cx
        this.cy = cy
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

data class OnMouseDragAutoCloseable(
        val onDownAutoCloseable: AutoCloseable,
        val onUpAnywhereAutoCloseable: AutoCloseable,
        val onMoveAnywhereAutoCloseable: AutoCloseable
) : AutoCloseable {
    override fun close() {
        onDownAutoCloseable.close()
        onUpAnywhereAutoCloseable.close()
        onMoveAnywhereAutoCloseable.close()
    }
}

private fun <T : View> T.onMouseDragInternal(
    timeProvider: TimeProvider = TimeProvider, info:
    MouseDragInfo = MouseDragInfo(this), callback: Views.(MouseDragInfo) -> Unit
): Pair<T, OnMouseDragAutoCloseable> {
    var dragging = false
    var sx = 0f
    var sy = 0f
    var cx = 0f
    var cy = 0f
    val view = this

    var mousePos = Point()

    fun views() = view.stage!!.views

    fun updateMouse() {
        val views = views()
        //println("views.globalMouse=${views.globalMouseXY}, views.nativeMouse=${views.nativeMouseXY}")
        //mousePos.copyFrom(views.globalMouseXY)
        mousePos = views.globalMousePos
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
            else -> Unit
        }
        cx = mousePos.x
        cy = mousePos.y
        val dx = cx - sx
        val dy = cy - sy
        callback(views(), info.set(dx, dy, state.isStart, state.isEnd, timeProvider.now(), sx, sy, cx, cy))
    }

    lateinit var onDownAutoCloseable: AutoCloseable
    lateinit var onUpAnywhereAutoCloseable: AutoCloseable
    lateinit var onMoveAnywhereAutoCloseable: AutoCloseable
    this.mouse {
        onDownAutoCloseable = onDownCloseable { handle(it, MouseDragState.START) }
        onUpAnywhereAutoCloseable = onUpAnywhereCloseable { handle(it, MouseDragState.END) }
        onMoveAnywhereAutoCloseable = onMoveAnywhereCloseable { handle(it, MouseDragState.DRAG) }
    }
    return this to OnMouseDragAutoCloseable(
        onDownAutoCloseable,
        onUpAnywhereAutoCloseable,
        onMoveAnywhereAutoCloseable
    )
}

fun <T : View> T.onMouseDragCloseable(
    timeProvider: TimeProvider = TimeProvider, info:
    MouseDragInfo = MouseDragInfo(this), callback: Views.(MouseDragInfo) -> Unit
): OnMouseDragAutoCloseable = onMouseDragInternal(timeProvider, info, callback).second

fun <T : View> T.onMouseDrag(
    timeProvider: TimeProvider = TimeProvider,
    info: MouseDragInfo = MouseDragInfo(this),
    callback: Views.(MouseDragInfo) -> Unit
): T = onMouseDragInternal(timeProvider, info, callback).first

open class DraggableInfo(view: View) : MouseDragInfo(view) {
    var viewStartXY = Point()

    var viewStartX: Float
        get() = viewStartXY.x;
        set(value) {
            viewStartXY = viewStartXY.copy(x = value)
        }
    var viewStartY: Float
        get() = viewStartXY.y;
        set(value) {
            viewStartXY = viewStartXY.copy(y = value)
        }

    var viewPrevXY = Point()

    var viewPrevX: Float
        get() = viewPrevXY.x;
        set(value) {
            viewPrevXY = viewPrevXY.copy(x = value)
        }
    var viewPrevY: Float
        get() = viewPrevXY.y;
        set(value) {
            viewPrevXY = viewPrevXY.copy(y = value)
        }

    var viewNextXY = Point()

    var viewNextX: Float
        get() = viewNextXY.x;
        set(value) {
            viewNextXY = viewNextXY.copy(x = value)
        }
    var viewNextY: Float
        get() = viewNextXY.y;
        set(value) {
            viewNextXY = viewNextXY.copy(y = value)
        }

    var viewDeltaXY = Point.ZERO

    var viewDeltaX: Float
        get() = viewDeltaXY.x;
        set(value) {
            viewDeltaXY = viewDeltaXY.copy(x = value)
        }
    var viewDeltaY: Float
        get() = viewDeltaXY.y;
        set(value) {
            viewDeltaXY = viewDeltaXY.copy(y = value)
        }
}

data class DraggableAutoCloseable(
    val onMouseDragAutoCloseable: AutoCloseable
): AutoCloseable {
    override fun close() {
        onMouseDragAutoCloseable.close()
    }
}

private fun <T : View> T.draggableInternal(
    selector: View = this,
    autoMove: Boolean = true,
    onDrag: ((DraggableInfo) -> Unit)? = null
): Pair<T, DraggableAutoCloseable> {
    val view = this
    val info = DraggableInfo(view)
    val onMouseDragCloseable = selector.onMouseDragCloseable(info = info) {
        if (info.start) {
            info.viewStartXY = view.pos
        }
        //println("localDXY=${info.localDX(view)},${info.localDY(view)}")
        info.viewPrevXY = view.pos
        info.viewNextXY = Point(
            info.viewStartX + info.localDX(view),
            info.viewStartY + info.localDY(view)
        )
        info.viewDeltaXY = Point(info.viewNextX - info.viewPrevX, info.viewNextY - info.viewPrevY)
        if (autoMove) {
            view.xy(info.viewNextXY)
        }
        onDrag?.invoke(info)
        //println("DRAG: $dx, $dy, $start, $end")
    }
    return this to DraggableAutoCloseable(onMouseDragCloseable)
}

fun <T : View> T.draggableCloseable(
    selector: View = this,
    autoMove: Boolean = true,
    onDrag: ((DraggableInfo) -> Unit)? = null
): DraggableAutoCloseable = draggableInternal(selector, autoMove, onDrag).second

fun <T : View> T.draggable(
    selector: View = this,
    autoMove: Boolean = true,
    onDrag: ((DraggableInfo) -> Unit)? = null
): T = draggableInternal(selector, autoMove, onDrag).first
