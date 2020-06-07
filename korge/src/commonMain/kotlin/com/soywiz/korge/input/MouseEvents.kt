package com.soywiz.korge.input

import com.soywiz.kds.*
import com.soywiz.korge.bitmapfont.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korev.*
import com.soywiz.korge.internal.*
import kotlin.js.*
import kotlin.reflect.*

@OptIn(KorgeInternal::class)
class MouseEvents(override val view: View) : MouseComponent, Extra by Extra.Mixin() {
    @PublishedApi
    internal lateinit var views: Views
    @PublishedApi
    internal val coroutineContext get() = views.coroutineContext

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
    val moveOutside = Signal<MouseEvents>()
    val exit = Signal<MouseEvents>()

    @Deprecated("", ReplaceWith("moveOutside"))
    val mouseOutside get() = moveOutside

    @Deprecated("Use function instead with suspend handler")
    @JsName("_onClick")
	val onClick = click

    @Deprecated("Use function instead with suspend handler")
    @JsName("_onOver")
	val onOver = over

    @Deprecated("Use function instead with suspend handler")
    @JsName("_onOut")
	val onOut = out

    @Deprecated("Use function instead with suspend handler")
    @JsName("_onDown")
	val onDown = down

    @Deprecated("Use function instead with suspend handler")
    @JsName("_onDownFromOutside")
	val onDownFromOutside = downFromOutside

    @Deprecated("Use function instead with suspend handler")
    @JsName("_onUp")
	val onUp = up

    @Deprecated("Use function instead with suspend handler")
    @JsName("_onUpOutside")
	val onUpOutside = upOutside

    @Deprecated("Use function instead with suspend handler")
    @JsName("_onUpAnywhere")
	val onUpAnywhere = upAnywhere

    @Deprecated("Use function instead with suspend handler")
    @JsName("_onMove")
	val onMove = move

    @Deprecated("Use function instead with suspend handler")
    @JsName("_onMoveAnywhere")
	val onMoveAnywhere = moveAnywhere

    @Deprecated("Use function instead with suspend handler")
    @JsName("_onMoveOutside")
	val onMoveOutside = mouseOutside

    @PublishedApi
    internal inline fun _mouseEvent(prop: KProperty1<MouseEvents, Signal<MouseEvents>>, noinline handler: suspend (MouseEvents) -> Unit): MouseEvents {
        prop.get(this).add { launchImmediately(this.coroutineContext) { handler(it) } }
        return this
    }

    inline fun onClick(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents = _mouseEvent(MouseEvents::click, handler)
    inline fun onOver(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents = _mouseEvent(MouseEvents::over, handler)
    inline fun onOut(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents = _mouseEvent(MouseEvents::out, handler)
    inline fun onDown(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents = _mouseEvent(MouseEvents::down, handler)
    inline fun onDownFromOutside(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents = _mouseEvent(MouseEvents::downFromOutside, handler)
    inline fun onUp(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents = _mouseEvent(MouseEvents::up, handler)
    inline fun onUpOutside(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents = _mouseEvent(MouseEvents::upOutside, handler)
    inline fun onUpAnywhere(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents = _mouseEvent(MouseEvents::upAnywhere, handler)
    inline fun onMove(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents = _mouseEvent(MouseEvents::move, handler)
    inline fun onMoveAnywhere(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents = _mouseEvent(MouseEvents::moveAnywhere, handler)
    inline fun onMoveOutside(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents = _mouseEvent(MouseEvents::moveOutside, handler)
    inline fun onExit(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents = _mouseEvent(MouseEvents::exit, handler)

    var hitTest: View? = null; private set
	private var lastOver = false
    private var lastInside = false
	private var lastPressing = false

	val CLICK_THRESHOLD = 16

	var Input.mouseHitSearch by Extra.Property { false }
	var Input.mouseHitResult by Extra.Property<View?> { null }
	var Input.mouseHitResultUsed by Extra.Property<View?> { null }
	var Views.mouseDebugHandlerOnce by Extra.Property { Once() }

    // Global variants (Not related to the STAGE! but to the window coordinates, so can't be translated directly use *Stage variants instead or directly Stage.mouseXY!)
    @KorgeInternal
	var downPosGlobal = Point()
    @KorgeInternal
    var upPosGlobal = Point()
    @KorgeInternal
    val startedPosGlobal = Point()
    @KorgeInternal
    val lastPosGlobal = Point()
    @KorgeInternal
    val currentPosGlobal = Point()

    // Local variants
    private val _downPosLocal: Point = Point()
    private val _upPosLocal: Point = Point()
    private val _startedPosLocal = Point()
    private val _lastPosLocal = Point()
    private val _currentPosLocal = Point()

    val startedPosLocal get() = view.globalToLocal(startedPosGlobal, _startedPosLocal)
    val lastPosLocal get() = view.globalToLocal(lastPosGlobal, _lastPosLocal)
    val currentPosLocal get() = view.globalToLocal(currentPosGlobal, _currentPosLocal)
    val downPosLocal get() = view.globalToLocal(downPosGlobal, _downPosLocal)
    val upPosLocal get() = view.globalToLocal(upPosGlobal, _upPosLocal)

    // Stage-based variants
    private val _downPosStage: Point = Point()
    private val _upPosStage: Point = Point()
    private val _startedPosStage = Point()
    private val _lastPosStage = Point()
    private val _currentPosStage = Point()

    val startedPosStage get() = views.stage.globalToLocal(startedPosGlobal, _startedPosStage)
    val lastPosStage get() = views.stage.globalToLocal(lastPosGlobal, _lastPosStage)
    val currentPosStage get() = views.stage.globalToLocal(currentPosGlobal, _currentPosStage)
    val downPosStage get() = views.stage.globalToLocal(downPosGlobal, _downPosStage)
    val upPosStage get() = views.stage.globalToLocal(upPosGlobal, _upPosStage)

    // Deprecated variants
    @Deprecated("Use startedPosLocal instead")
    val startedPos get() = startedPosLocal
    @Deprecated("Use lastPosLocal instead")
    val lastPos get() = lastPosLocal
    @Deprecated("Use currentPosLocal instead")
    val currentPos get() = currentPosLocal
    @Deprecated("Use downPosLocal or downPosGlobal instead")
    val downPos get() = downPosGlobal
    @Deprecated("Use upPosLocal or upPosGlobal instead")
    val upPos get() = upPosGlobal

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
    lateinit var lastEvent: MouseEvent
    var button: MouseButton = MouseButton.LEFT
    var buttons: Int = 0
    var scrollDeltaX: Double = 0.0
    var scrollDeltaY: Double = 0.0
    var scrollDeltaZ: Double = 0.0
    var isShiftDown: Boolean = false
    var isCtrlDown: Boolean = false
    var isAltDown: Boolean = false
    var isMetaDown: Boolean = false

	@Suppress("DuplicatedCode")
    override fun onMouseEvent(views: Views, event: MouseEvent) {
        this.views = views
        // Store event
        this.lastEvent = event
        this.button = event.button
        this.buttons = event.buttons
        this.scrollDeltaX = event.scrollDeltaX
        this.scrollDeltaY = event.scrollDeltaY
        this.scrollDeltaZ = event.scrollDeltaZ
        this.isShiftDown = event.isShiftDown
        this.isCtrlDown = event.isCtrlDown
        this.isAltDown = event.isAltDown
        this.isMetaDown = event.isMetaDown

        //println("MouseEvent.onMouseEvent($views, $event)")
		when (event.type) {
			MouseEvent.Type.UP -> {
				upPosGlobal.copyFrom(views.input.mouse)
				if (upPosGlobal.distanceTo(downPosGlobal) < CLICK_THRESHOLD) {
					clickedCount++
					//if (isOver) {
					//	onClick(this)
					//}
				}
			}
			MouseEvent.Type.DOWN -> {
				downPosGlobal.copyFrom(views.input.mouse)
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

    inner class MouseEventsUpdate(override val view: View) : UpdateComponentWithViews, Extra by Extra.Mixin() {
        override fun update(views: Views, ms: Double) {
            this@MouseEvents.update(views, ms)
        }
    }

    val updater = MouseEventsUpdate(view).attach()

    fun update(views: Views, ms: Double) {
		if (!view.mouseEnabled) return
        this.views = views

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
						colorMul = RGBA(0xFF, 0, 0, 0x3F),
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
						colorMul = RGBA(0x00, 0, 0xFF, 0x3F),
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
        val inside = views.input.mouseInside
		if (over) views.input.mouseHitResultUsed = view
		val pressing = views.input.mouseButtons != 0
		val overChanged = (lastOver != over)
        val insideChanged = (lastInside != inside)
		val pressingChanged = pressing != lastPressing
        currentPosGlobal.copyFrom(views.input.mouse)

		//println("$hitTest, ${input.mouse}, $over, $pressing, $overChanged, $pressingChanged")

		//println("MouseComponent: $hitTest, $over")

		if (!overChanged && over && currentPosGlobal != lastPosGlobal) move(this)
		if (!overChanged && !over && currentPosGlobal != lastPosGlobal) moveOutside(this)
		if (currentPosGlobal != lastPosGlobal) moveAnywhere(this)
		if (overChanged && over) over(this)
		if (overChanged && !over) out(this)
		if (over && pressingChanged && pressing) {
			startedPosGlobal.copyFrom(currentPosGlobal)
			down(this)
		}
		if (overChanged && pressing) {
			downFromOutside(this)
		}
		if (pressingChanged && !pressing) {
			if (over) up(this) else upOutside(this)
			upAnywhere(this)
			//if ((currentPos - startedPos).length < CLICK_THRESHOLD) onClick(this)
		}
		if (over && clickedCount > 0) {
			//onClick(this)
		}
        if (insideChanged && !inside) {
            moveOutside(this)
            out(this)
            upOutside(this)
            exit(this)
        }

		lastOver = over
        lastInside = inside
		lastPressing = pressing
		lastPosGlobal.copyFrom(currentPosGlobal)
		clickedCount = 0
	}
}

//var Input.Frame.mouseHitResult by Extra.Property<View?>("mouseHitResult") {
//    views.root.hitTest(input.mouse)
//}


//var View.mouseEnabled by Extra.Property { true }

val View.mouse by Extra.PropertyThis<View, MouseEvents> { this.getOrCreateComponentMouse<MouseEvents> { MouseEvents(this) } }
inline fun <T> View.mouse(callback: MouseEvents.() -> T): T = mouse.run(callback)

@PublishedApi
internal inline fun <T : View?> T?.doMouseEvent(prop: KProperty1<MouseEvents, Signal<MouseEvents>>, noinline handler: suspend (MouseEvents) -> Unit): T? {
    this?.mouse?.let { mouse -> prop.get(mouse).add { launchImmediately(mouse.coroutineContext) { handler(it) } } }
    return this
}

inline fun <T : View?> T.onClick(noinline handler: suspend (MouseEvents) -> Unit) = doMouseEvent(MouseEvents::click, handler)
inline fun <T : View?> T.onOver(noinline handler: suspend (MouseEvents) -> Unit) = doMouseEvent(MouseEvents::over, handler)
inline fun <T : View?> T.onOut(noinline handler: suspend (MouseEvents) -> Unit) = doMouseEvent(MouseEvents::out, handler)
inline fun <T : View?> T.onDown(noinline handler: suspend (MouseEvents) -> Unit) = doMouseEvent(MouseEvents::down, handler)
inline fun <T : View?> T.onDownFromOutside(noinline handler: suspend (MouseEvents) -> Unit) = doMouseEvent(MouseEvents::downFromOutside, handler)
inline fun <T : View?> T.onUp(noinline handler: suspend (MouseEvents) -> Unit) = doMouseEvent(MouseEvents::up, handler)
inline fun <T : View?> T.onUpOutside(noinline handler: suspend (MouseEvents) -> Unit) = doMouseEvent(MouseEvents::upOutside, handler)
inline fun <T : View?> T.onUpAnywhere(noinline handler: suspend (MouseEvents) -> Unit) = doMouseEvent(MouseEvents::upAnywhere, handler)
inline fun <T : View?> T.onMove(noinline handler: suspend (MouseEvents) -> Unit) = doMouseEvent(MouseEvents::move, handler)
