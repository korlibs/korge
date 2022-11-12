package com.soywiz.korge.input

import com.soywiz.kds.Extra
import com.soywiz.kds.extraProperty
import com.soywiz.klock.DateTime
import com.soywiz.klock.PerformanceCounter
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.seconds
import com.soywiz.korev.MouseButton
import com.soywiz.korev.MouseEvent
import com.soywiz.korev.preventDefault
import com.soywiz.korge.bitmapfont.drawText
import com.soywiz.korge.component.MouseComponent
import com.soywiz.korge.component.UpdateComponentWithViews
import com.soywiz.korge.component.attach
import com.soywiz.korge.component.detach
import com.soywiz.korge.internal.KorgeInternal
import com.soywiz.korge.view.BlendMode
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.ViewsContainer
import com.soywiz.korge.view.hasAncestor
import com.soywiz.korge.view.onNextFrame
import com.soywiz.korgw.GameWindow
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.async.Signal
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.util.Once
import com.soywiz.korma.geom.Point
import kotlin.math.max
import kotlin.native.concurrent.ThreadLocal
import kotlin.reflect.KProperty1

@OptIn(KorgeInternal::class)
class MouseEvents(override val view: View) : MouseComponent, Extra by Extra.Mixin(), Closeable {
    init {
        view.mouseEnabled = true
    }

    companion object {
        var Input.mouseHitSearch by Extra.Property { false }
        var Input.mouseHitResult by Extra.Property<View?> { null }
        var Input.mouseHitResultUsed by Extra.Property<View?> { null }
        var Views.mouseDebugHandlerOnce by Extra.Property { Once() }

        private fun mouseHitTest(views: Views): View? {
            if (!views.input.mouseHitSearch) {
                views.input.mouseHitSearch = true
                views.input.mouseHitResult =
                    views.stage.mouseHitTest(views.globalMouseX, views.globalMouseY)

                var view: View? = views.input.mouseHitResult
                while (view != null) {
                    if (view.cursor != null) {
                        break
                    }
                    view = view.parent
                }

                val newCursor = view?.cursor ?: GameWindow.Cursor.DEFAULT
                if (views.gameWindow.cursor != newCursor) {
                    views.gameWindow.cursor = newCursor
                }
                //if (frame.mouseHitResult != null) {
                //val hitResult = frame.mouseHitResult!!
                //println("BOUNDS: $hitResult : " + hitResult.getLocalBounds() + " : " + hitResult.getGlobalBounds())
                //hitResult.dump()
                //}
            }
            return views.input.mouseHitResult
        }

        fun installDebugExtensionOnce(views: Views) {
            views.mouseDebugHandlerOnce {
                views.debugHandlers += { ctx ->
                    val scale = ctx.debugOverlayScale
                    //val scale = 2.0

                    val space = max(1 * scale, 2.0)
                    val matrix = views.windowToGlobalMatrix
                    //println(scale)


                    var yy = 60.toDouble() * scale
                    val lineHeight = 8.toDouble() * scale
                    val mouseHit = mouseHitTest(views)
                    if (mouseHit != null) {
                        val bounds = mouseHit.getLocalBoundsOptimizedAnchored()
                        renderContext.useBatcher { batch ->
                            batch.drawQuad(
                                ctx.getTex(Bitmaps.white),
                                x = bounds.x.toFloat(),
                                y = bounds.y.toFloat(),
                                width = bounds.width.toFloat(),
                                height = bounds.height.toFloat(),
                                colorMul = RGBA(0xFF, 0, 0, 0x3F),
                                m = mouseHit.globalMatrix,
                                premultiplied = Bitmaps.white.premultiplied,
                                wrap = false,
                            )
                            renderContext.drawText(
                                debugBmpFont,
                                lineHeight,
                                "$mouseHit : ${views.globalMouseX},${views.globalMouseY}",
                                x = 0,
                                y = yy.toInt(),
                                filtering = false,
                                colMul = ctx.debugExtraFontColor,
                                blendMode = BlendMode.INVERT,
                                m = matrix
                            )
                        }
                        yy += lineHeight
                        yy += space
                    }

                    val mouseHitResultUsed = input.mouseHitResultUsed
                    if (mouseHitResultUsed != null) {
                        val bounds = mouseHitResultUsed.getLocalBoundsOptimizedAnchored()
                        renderContext.useBatcher { batch ->
                            batch.drawQuad(
                                ctx.getTex(Bitmaps.white),
                                x = bounds.x.toFloat(),
                                y = bounds.y.toFloat(),
                                width = bounds.width.toFloat(),
                                height = bounds.height.toFloat(),
                                colorMul = RGBA(0x00, 0, 0xFF, 0x3F),
                                m = mouseHitResultUsed.globalMatrix,
                                premultiplied = Bitmaps.white.premultiplied, wrap = false,
                            )
                            var vview = mouseHitResultUsed
                            while (vview != null) {
                                renderContext.drawText(
                                    debugBmpFont,
                                    lineHeight,
                                    vview.toString(),
                                    x = 0,
                                    y = yy.toInt(),
                                    filtering = false,
                                    colMul = ctx.debugExtraFontColor,
                                    blendMode = BlendMode.INVERT,
                                    m = matrix
                                )
                                vview = vview.parent
                                yy += lineHeight
                                yy += space
                            }
                        }
                    }
                }
            }
        }
    }

    lateinit var views: Views
        internal set

    @PublishedApi
    internal val coroutineContext get() = views.coroutineContext

    val input get() = views.input

    //var cursor: GameWindow.ICursor? = null
    val downImmediate = Signal<MouseEvents>()
    val click = Signal<MouseEvents>()
    val over = Signal<MouseEvents>()
    val out = Signal<MouseEvents>()
    val down = Signal<MouseEvents>()
    val downOutside = Signal<MouseEvents>()
    val downFromOutside = Signal<MouseEvents>()
    val up = Signal<MouseEvents>()
    val upOutside = Signal<MouseEvents>()
    val upOutsideExit = Signal<MouseEvents>()
    val upOutsideAny = Signal<MouseEvents>()
    val upAnywhere = Signal<MouseEvents>()
    val move = Signal<MouseEvents>()
    val moveAnywhere = Signal<MouseEvents>()
    val moveOutside = Signal<MouseEvents>()
    val exit = Signal<MouseEvents>()
    val scroll = Signal<MouseEvents>()
    val scrollAnywhere = Signal<MouseEvents>()
    val scrollOutside = Signal<MouseEvents>()

    @PublishedApi
    internal inline fun _mouseEvent(
        prop: KProperty1<MouseEvents, Signal<MouseEvents>>,
        noinline handler: suspend (MouseEvents) -> Unit
    ): MouseEvents {
        prop.get(this).add { launchImmediately(this.coroutineContext) { handler(it) } }
        return this
    }

    inline fun onClick(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents =
        _mouseEvent(MouseEvents::click, handler)

    inline fun onOver(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents =
        _mouseEvent(MouseEvents::over, handler)

    inline fun onOut(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents =
        _mouseEvent(MouseEvents::out, handler)

    inline fun onDown(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents =
        _mouseEvent(MouseEvents::down, handler)

    inline fun onDownFromOutside(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents =
        _mouseEvent(MouseEvents::downFromOutside, handler)

    inline fun onUp(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents =
        _mouseEvent(MouseEvents::up, handler)

    inline fun onUpOutside(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents =
        _mouseEvent(MouseEvents::upOutside, handler)

    inline fun onUpAnywhere(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents =
        _mouseEvent(MouseEvents::upAnywhere, handler)

    inline fun onMove(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents =
        _mouseEvent(MouseEvents::move, handler)

    inline fun onMoveAnywhere(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents =
        _mouseEvent(MouseEvents::moveAnywhere, handler)

    inline fun onMoveOutside(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents =
        _mouseEvent(MouseEvents::moveOutside, handler)

    inline fun onExit(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents =
        _mouseEvent(MouseEvents::exit, handler)

    inline fun onScroll(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents =
        _mouseEvent(MouseEvents::scroll, handler)

    inline fun onScrollAnywhere(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents =
        _mouseEvent(MouseEvents::scrollAnywhere, handler)

    inline fun onScrollOutside(noinline handler: suspend (MouseEvents) -> Unit): MouseEvents =
        _mouseEvent(MouseEvents::scrollOutside, handler)


    // Returns the Closeable after adding the handler to the corresponding Signal
    // in the MouseEvents.
    @PublishedApi
    internal inline fun _mouseEventCloseable(
        prop: KProperty1<MouseEvents, Signal<MouseEvents>>,
        noinline handler: suspend (MouseEvents) -> Unit
    ): Closeable {
        return prop.get(this)
            .add { launchImmediately(this.coroutineContext) { handler(it) } }
    }

    inline fun onClickCloseable(noinline handler: suspend (MouseEvents) -> Unit) =
        _mouseEventCloseable(MouseEvents::click, handler)

    inline fun onOverCloseable(noinline handler: suspend (MouseEvents) -> Unit) =
        _mouseEventCloseable(MouseEvents::over, handler)

    inline fun onOutCloseable(noinline handler: suspend (MouseEvents) -> Unit) =
        _mouseEventCloseable(MouseEvents::out, handler)

    inline fun onDownCloseable(noinline handler: suspend (MouseEvents) -> Unit) =
        _mouseEventCloseable(MouseEvents::down, handler)

    inline fun onDownFromOutsideCloseable(noinline handler: suspend (MouseEvents) -> Unit) =
        _mouseEventCloseable(MouseEvents::downFromOutside, handler)

    inline fun onUpCloseable(noinline handler: suspend (MouseEvents) -> Unit) =
        _mouseEventCloseable(MouseEvents::up, handler)

    inline fun onUpOutsideCloseable(noinline handler: suspend (MouseEvents) -> Unit) =
        _mouseEventCloseable(MouseEvents::upOutside, handler)

    inline fun onUpAnywhereCloseable(noinline handler: suspend (MouseEvents) -> Unit) =
        _mouseEventCloseable(MouseEvents::upAnywhere, handler)

    inline fun onMoveCloseable(noinline handler: suspend (MouseEvents) -> Unit) =
        _mouseEventCloseable(MouseEvents::move, handler)

    inline fun onMoveAnywhereCloseable(noinline handler: suspend (MouseEvents) -> Unit) =
        _mouseEventCloseable(MouseEvents::moveAnywhere, handler)

    inline fun onMoveOutsideCloseable(noinline handler: suspend (MouseEvents) -> Unit) =
        _mouseEventCloseable(MouseEvents::moveOutside, handler)

    inline fun onExitCloseable(noinline handler: suspend (MouseEvents) -> Unit) =
        _mouseEventCloseable(MouseEvents::exit, handler)

    inline fun onScrollCloseable(noinline handler: suspend (MouseEvents) -> Unit) =
        _mouseEventCloseable(MouseEvents::scroll, handler)

    inline fun onScrollAnywhereCloseable(noinline handler: suspend (MouseEvents) -> Unit) =
        _mouseEventCloseable(MouseEvents::scrollAnywhere, handler)

    inline fun onScrollOutsideCloseable(noinline handler: suspend (MouseEvents) -> Unit) =
        _mouseEventCloseable(MouseEvents::scrollOutside, handler)

    var hitTest: View? = null; private set
    private var lastOver = false
    private var lastInside = false
    private var lastPressing = false

    var upPosTime = PerformanceCounter.reference
    var downPosTime = PerformanceCounter.reference

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

    var clickedCount = 0

    fun stopPropagation() {
        currentEvent?.stopPropagation()
    }

    val isOver: Boolean get() = hitTest?.hasAncestor(view) ?: false
    var lastEventSet = false
    var currentEvent: MouseEvent? = null
    var lastEvent: MouseEvent = MouseEvent()
    var lastEmulated: Boolean = false
    var lastEventUp: MouseEvent = MouseEvent()

    //var lastEventDown: MouseEvent? = null
    //var lastEventScroll: MouseEvent? = null
    //var lastEventMove: MouseEvent? = null
    //var lastEventDrag: MouseEvent? = null
    //var lastEventClick: MouseEvent? = null
    //var lastEventEnter: MouseEvent? = null
    //var lastEventExit: MouseEvent? = null
    val button: MouseButton get() = lastEvent.button
    val buttons: Int get() = lastEvent.buttons

    @Deprecated("Use other variants")
    val scrollDeltaX: Double get() = lastEvent.scrollDeltaX

    @Deprecated("Use other variants")
    val scrollDeltaY: Double get() = lastEvent.scrollDeltaY

    @Deprecated("Use other variants")
    val scrollDeltaZ: Double get() = lastEvent.scrollDeltaZ

    val scrollDeltaXPixels: Double get() = lastEvent.scrollDeltaXPixels
    val scrollDeltaYPixels: Double get() = lastEvent.scrollDeltaYPixels
    val scrollDeltaZPixels: Double get() = lastEvent.scrollDeltaZPixels

    val scrollDeltaXLines: Double get() = lastEvent.scrollDeltaXLines
    val scrollDeltaYLines: Double get() = lastEvent.scrollDeltaYLines
    val scrollDeltaZLines: Double get() = lastEvent.scrollDeltaZLines

    val scrollDeltaXPages: Double get() = lastEvent.scrollDeltaXPages
    val scrollDeltaYPages: Double get() = lastEvent.scrollDeltaYPages
    val scrollDeltaZPages: Double get() = lastEvent.scrollDeltaZPages

    val isShiftDown: Boolean get() = lastEvent.isShiftDown
    val isCtrlDown: Boolean get() = lastEvent.isCtrlDown
    val isAltDown: Boolean get() = lastEvent.isAltDown
    val isMetaDown: Boolean get() = lastEvent.isMetaDown
    val pressing get() = views.input.mouseButtons != 0

    override fun toString(): String = lastEvent.toString()

    @Suppress("DuplicatedCode")
    override fun onMouseEvent(views: Views, event: MouseEvent) {
        if (!view.mouseEnabled) return
        this.views = views
        // Store event
        this.currentEvent = event
        this.lastEvent.copyFrom(event)
        this.lastEmulated = event.emulated
        lastEventSet = true

        //println("MouseEvent.onMouseEvent($views, $event)")
        when (event.type) {
            MouseEvent.Type.UP -> {
                lastEventUp.copyFrom(event)
                upPosGlobal.copyFrom(views.input.mouse)
                upPosTime = PerformanceCounter.reference
                val elapsedTime = upPosTime - downPosTime
                if (
                    upPosGlobal.distanceTo(downPosGlobal) < views.input.clickDistance &&
                    elapsedTime < views.input.clickTime
                ) {
                    clickedCount++
                    //if (isOver) {
                    //	onClick(this)
                    //}
                    if (isOver) {
                        click(this@MouseEvents)
                        if (click.listenerCount > 0) {
                            preventDefault(view)
                        }
                    }
                }
            }
            MouseEvent.Type.DOWN -> {
                //this.lastEventDown = event
                downPosTime = PerformanceCounter.reference
                downPosGlobal.copyFrom(views.input.mouse)
                if (downImmediate.hasListeners) {
                    if (isOver) {
                        downImmediate(this@MouseEvents)
                    }
                }
            }
            MouseEvent.Type.SCROLL -> {
                //this.lastEventScroll = event
                if (isOver) {
                    scroll(this@MouseEvents)
                } else {
                    scrollOutside(this@MouseEvents)
                }
                scrollAnywhere(this@MouseEvents)
            }
            //MouseEvent.Type.MOVE -> this.lastEventMove = event
            //MouseEvent.Type.DRAG -> this.lastEventDrag = event
            //MouseEvent.Type.CLICK -> this.lastEventClick = event
            //MouseEvent.Type.ENTER -> this.lastEventEnter = event
            //MouseEvent.Type.EXIT -> this.lastEventExit = event
            else -> Unit
        }
    }

    inner class MouseEventsUpdate(override val view: View) : UpdateComponentWithViews,
        Extra by Extra.Mixin() {
        override fun update(views: Views, dt: TimeSpan) {
            this@MouseEvents.update(views, dt)
        }
    }

    val updater = MouseEventsUpdate(view).attach()

    private fun <T> temporalLastEvent(lastEventNew: MouseEvent?, block: () -> T): T {
        val old = lastEvent
        lastEvent = lastEventNew ?: lastEvent
        try {
            return block()
        } finally {
            lastEvent = old
        }
    }

    fun update(views: Views, dt: TimeSpan) {
        if (!view.mouseEnabled) return
        this.views = views

        installDebugExtensionOnce(views)

        //println("${frame.mouseHitResult}")

        hitTest = mouseHitTest(views)
        val over = isOver
        val inside = views.input.mouseInside
        if (over) views.input.mouseHitResultUsed = view
        val overChanged = (lastOver != over)
        val insideChanged = (lastInside != inside)
        val pressingChanged = pressing != lastPressing
        currentPosGlobal.copyFrom(views.input.mouse)

        //println("$hitTest, ${input.mouse}, $over, $pressing, $overChanged, $pressingChanged")

        //println("MouseComponent: $hitTest, $over")

        if (!overChanged && over && currentPosGlobal != lastPosGlobal) move(this)
        if (!overChanged && !over && currentPosGlobal != lastPosGlobal) moveOutside(this)
        if (currentPosGlobal != lastPosGlobal) moveAnywhere(this)
        if (overChanged && over) {
            //if (cursor != null) views.gameWindow.cursor = cursor!!
            over(this)
        }
        if (overChanged && !over) {
            //if (cursor != null) views.gameWindow.cursor = GameWindow.Cursor.DEFAULT
            out(this)
        }
        if (pressingChanged && pressing) {
            startedPosGlobal.copyFrom(currentPosGlobal)
            if (over) {
                down(this)
            }
            if (!over) {
                downOutside(this)
            }
        }
        if (overChanged && pressing) {
            downFromOutside(this)
        }
        if (pressingChanged && !pressing) {
            temporalLastEvent(lastEventUp) {
                if (over) {
                    up(this)
                } else {
                    upOutside(this)
                    upOutsideAny(this)
                }
                upAnywhere(this)
            }
            //if ((currentPos - startedPos).length < CLICK_THRESHOLD) onClick(this)
        }
        if (over && clickedCount > 0) {
            //onClick(this)
        }
        if (insideChanged && !inside) {
            moveOutside(this)
            out(this)
            upOutsideExit(this)
            upOutsideAny(this)
            exit(this)
        }

        lastOver = over
        lastInside = inside
        lastPressing = pressing
        lastPosGlobal.copyFrom(currentPosGlobal)
        clickedCount = 0
    }

    override fun close() {
        this.detach()
    }
}

//var Input.Frame.mouseHitResult by Extra.Property<View?>("mouseHitResult") {
//    views.root.hitTest(input.mouse)
//}


//var View.mouseEnabled by Extra.Property { true }

@ThreadLocal // @TODO: Is this required?
val View.mouse by Extra.PropertyThis<View, MouseEvents> {
    this.getOrCreateComponentMouse<MouseEvents> { MouseEvents(this) }
}

inline fun View.newMouse(callback: MouseEvents.() -> Unit): MouseEvents {
    return MouseEvents(this).also {
        this.addComponent(it)
        callback(it)
    }
}

inline fun <T> View.mouse(callback: MouseEvents.() -> T): T = mouse.run(callback)

@PublishedApi
internal inline fun <T : View?> T?.doMouseEvent(
    prop: KProperty1<MouseEvents, Signal<MouseEvents>>,
    noinline handler: suspend (MouseEvents) -> Unit
): T? {
    this?.mouse?.let { mouse ->
        prop.get(mouse).add { launchImmediately(mouse.coroutineContext) { handler(it) } }
    }
    return this
}

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class EventsDslMarker

// @TODO: onOut should happen before onOver (circumvented via onNextFrame)

inline fun <T : View> T.onOutOnOver(
    noinline out: @EventsDslMarker (MouseEvents) -> Unit,
    noinline over: @EventsDslMarker (MouseEvents) -> Unit
): T {
    var component: UpdateComponentWithViews? = null
    onOut { events ->
        component?.detach()
        component = null
        out(events)
    }
    onOver { events -> component = onNextFrame { over(events) } }
    return this
}

inline fun <T : View?> T.onClick(noinline handler: @EventsDslMarker suspend (MouseEvents) -> Unit) =
    doMouseEvent(MouseEvents::click, handler)

inline fun <T : View?> T.onOver(noinline handler: @EventsDslMarker suspend (MouseEvents) -> Unit) =
    doMouseEvent(MouseEvents::over, handler)

inline fun <T : View?> T.onOut(noinline handler: @EventsDslMarker suspend (MouseEvents) -> Unit) =
    doMouseEvent(MouseEvents::out, handler)

inline fun <T : View?> T.onDown(noinline handler: @EventsDslMarker suspend (MouseEvents) -> Unit) =
    doMouseEvent(MouseEvents::down, handler)

inline fun <T : View?> T.onDownFromOutside(noinline handler: @EventsDslMarker suspend (MouseEvents) -> Unit) =
    doMouseEvent(MouseEvents::downFromOutside, handler)

inline fun <T : View?> T.onUp(noinline handler: @EventsDslMarker suspend (MouseEvents) -> Unit) =
    doMouseEvent(MouseEvents::up, handler)

inline fun <T : View?> T.onUpOutside(noinline handler: @EventsDslMarker suspend (MouseEvents) -> Unit) =
    doMouseEvent(MouseEvents::upOutside, handler)

inline fun <T : View?> T.onUpAnywhere(noinline handler: @EventsDslMarker suspend (MouseEvents) -> Unit) =
    doMouseEvent(MouseEvents::upAnywhere, handler)

inline fun <T : View?> T.onMove(noinline handler: @EventsDslMarker suspend (MouseEvents) -> Unit) =
    doMouseEvent(MouseEvents::move, handler)

inline fun <T : View?> T.onScroll(noinline handler: @EventsDslMarker suspend (MouseEvents) -> Unit) =
    doMouseEvent(MouseEvents::scroll, handler)

fun ViewsContainer.installMouseDebugExtensionOnce() = MouseEvents.installDebugExtensionOnce(views)

fun MouseEvents.doubleClick(callback: (MouseEvents) -> Unit): Closeable = multiClick(2, callback)

fun MouseEvents.multiClick(count: Int, callback: (MouseEvents) -> Unit): Closeable {
    var clickCount = 0
    var lastClickTime = DateTime.EPOCH
    return this.click {
        val currentClickTime = DateTime.now()
        if (currentClickTime - lastClickTime > 0.3.seconds) {
            clickCount = 0
        }
        lastClickTime = currentClickTime
        clickCount++
        it.clickedCount = clickCount
        if (clickCount == count) {
            callback(it)
        }
    }
}

@ThreadLocal // @TODO: Is this required?
var View.cursor: GameWindow.ICursor? by extraProperty { null }

//    get() = mouse.cursor
//    set(value) { mouse.cursor = value }

//var View.cursor: GameWindow.ICursor?
//    get() = mouse.cursor
//    set(value) { mouse.cursor = value }
