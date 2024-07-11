package korlibs.korge.input

import korlibs.io.lang.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.time.*
import kotlin.time.*

open class MouseDragInfo(
    val view: View,
    var dx: Double = 0.0,
    var dy: Double = 0.0,
    var start: Boolean = false,
    var end: Boolean = false,
    var startTime: DateTime = DateTime.EPOCH,
    var time: DateTime = DateTime.EPOCH,
    var sx: Double = 0.0,
    var sy: Double = 0.0,
    var cx: Double = 0.0,
    var cy: Double = 0.0,
) {
    override fun toString(): String = "MouseDragInfo(start=$start, end=$end, sx=$sx, sy=$sy, cx=$cx, cy=$cy)"

    lateinit var mouseEvents: MouseEvents
    val elapsed: Duration get() = time - startTime

    val localDXY: Point get() = localDXY(view)
    @Deprecated("") val localDX: Double get() = localDX(view)
    @Deprecated("") val localDY: Double get() = localDY(view)

    fun localDXY(view: View): Point = view.parent?.globalToLocalDelta(Point(0.0, 0.0), Point(dx, dy)) ?: Point(dx, dy)
    @Deprecated("")
    fun localDX(view: View): Double = localDXY(view).x
    @Deprecated("")
    fun localDY(view: View): Double = localDXY(view).y

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
        sx = 0.0
        sy = 0.0
        cx = 0.0
        cy = 0.0
    }

    fun set(dx: Double, dy: Double, start: Boolean, end: Boolean, time: DateTime, sx: Double, sy: Double, cx: Double, cy: Double): MouseDragInfo {
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

data class OnMouseDragCloseable(
    val onDownCloseable: AutoCloseable,
    val onUpAnywhereCloseable: AutoCloseable,
    val onMoveAnywhereCloseable: AutoCloseable
) : AutoCloseable {
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

    lateinit var onDownCloseable: AutoCloseable
    lateinit var onUpAnywhereCloseable: AutoCloseable
    lateinit var onMoveAnywhereCloseable: AutoCloseable
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
    var viewStartXY = Point()

    var viewStartX: Double
        get() = viewStartXY.x;
        set(value) {
            viewStartXY = viewStartXY.copy(x = value)
        }
    var viewStartY: Double
        get() = viewStartXY.y;
        set(value) {
            viewStartXY = viewStartXY.copy(y = value)
        }

    var viewPrevXY = Point()

    var viewPrevX: Double
        get() = viewPrevXY.x;
        set(value) {
            viewPrevXY = viewPrevXY.copy(x = value)
        }
    var viewPrevY: Double
        get() = viewPrevXY.y;
        set(value) {
            viewPrevXY = viewPrevXY.copy(y = value)
        }

    var viewNextXY = Point()

    var viewNextX: Double
        get() = viewNextXY.x;
        set(value) {
            viewNextXY = viewNextXY.copy(x = value)
        }
    var viewNextY: Double
        get() = viewNextXY.y;
        set(value) {
            viewNextXY = viewNextXY.copy(y = value)
        }

    var viewDeltaXY: Vector2D = Point.ZERO

    var viewDeltaX: Double
        get() = viewDeltaXY.x;
        set(value) {
            viewDeltaXY = viewDeltaXY.copy(x = value)
        }
    var viewDeltaY: Double
        get() = viewDeltaXY.y;
        set(value) {
            viewDeltaXY = viewDeltaXY.copy(y = value)
        }
}

data class DraggableCloseable(
    val onMouseDragCloseable: AutoCloseable
): AutoCloseable {
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
