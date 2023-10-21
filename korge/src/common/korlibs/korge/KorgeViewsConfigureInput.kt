package korlibs.korge

import korlibs.audio.sound.*
import korlibs.datastructure.iterators.*
import korlibs.event.*
import korlibs.image.color.*
import korlibs.inject.*
import korlibs.io.async.*
import korlibs.korge.internal.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.time.*
import kotlinx.coroutines.*

@KorgeInternal
internal fun Views.prepareViewsBase(
    eventDispatcher: EventListener,
    clearEachFrame: Boolean = true,
    bgcolor: RGBA = Colors.TRANSPARENT,
    fixedSizeStep: TimeSpan = TimeSpan.NIL,
    forceRenderEveryFrame: Boolean = true,
    configInjector: Injector.() -> Unit = {},
): CompletableDeferred<Unit> {
    val views = this

    configInjector(views.injector)

    val input = views.input
    val ag = views.ag
    var downPos = Point.ZERO
    var upPos = Point.ZERO
    var downTime = DateTime.EPOCH
    var moveTime = DateTime.EPOCH
    var upTime = DateTime.EPOCH
    var moveMouseOutsideInNextFrame = false
    val mouseTouchId = -1
    views.forceRenderEveryFrame = forceRenderEveryFrame

    // devicePixelRatio might change at runtime by changing the resolution or changing the screen of the window
    fun getRealXY(x: Double, y: Double, scaleCoords: Boolean): Point {
        return views.windowToGlobalCoords(Point(x, y))
    }

    fun mouseDown(type: String, p: Point, button: MouseButton) {
        input.toggleButton(button, true)
        input.setMouseGlobalPos(p, down = false)
        input.setMouseGlobalPos(p, down = true)
        views.mouseUpdated()
        downPos = input.mousePos
        downTime = DateTime.now()
        input.mouseInside = true
    }

    fun mouseUp(type: String, p: Point, button: MouseButton) {
        //Console.log("mouseUp: $name")
        input.toggleButton(button, false)
        input.setMouseGlobalPos(p, down = false)
        views.mouseUpdated()
        upPos = views.input.mousePos
    }

    fun mouseMove(type: String, p: Point, inside: Boolean) {
        views.input.setMouseGlobalPos(p, down = false)
        views.input.mouseInside = inside
        if (!inside) {
            moveMouseOutsideInNextFrame = true
        }
        views.mouseUpdated()
        moveTime = DateTime.now()
    }

    fun mouseDrag(type: String, p: Point) {
        views.input.setMouseGlobalPos(p, down = false)
        views.mouseUpdated()
        moveTime = DateTime.now()
    }

    val mouseTouchEvent = TouchEvent()

    fun dispatchSimulatedTouchEvent(
        p: Point,
        button: MouseButton,
        type: TouchEvent.Type,
        status: Touch.Status
    ) {
        mouseTouchEvent.screen = 0
        mouseTouchEvent.emulated = true
        mouseTouchEvent.currentTime = DateTime.now()
        mouseTouchEvent.scaleCoords = false
        mouseTouchEvent.startFrame(type)
        mouseTouchEvent.touch(button.id, p, status, kind = Touch.Kind.MOUSE, button = button)
        mouseTouchEvent.endFrame()
        views.dispatch(mouseTouchEvent)
    }

    eventDispatcher.onEvents(*MouseEvent.Type.ALL) { e ->
        //println("MOUSE: $e")
        Korge.logger.trace { "eventDispatcher.addEventListener<MouseEvent>:$e" }
        val p = getRealXY(e.x.toDouble(), e.y.toDouble(), e.scaleCoords)
        when (e.type) {
            MouseEvent.Type.DOWN -> {
                mouseDown("mouseDown", p, e.button)
                //updateTouch(mouseTouchId, x, y, start = true, end = false)
                dispatchSimulatedTouchEvent(p, e.button, TouchEvent.Type.START, Touch.Status.ADD)
            }

            MouseEvent.Type.UP -> {
                mouseUp("mouseUp", p, e.button)
                //updateTouch(mouseTouchId, x, y, start = false, end = true)
                dispatchSimulatedTouchEvent(p, e.button, TouchEvent.Type.END, Touch.Status.REMOVE)
            }

            MouseEvent.Type.DRAG -> {
                mouseDrag("onMouseDrag", p)
                //updateTouch(mouseTouchId, x, y, start = false, end = false)
                dispatchSimulatedTouchEvent(p, e.button, TouchEvent.Type.MOVE, Touch.Status.KEEP)
            }

            MouseEvent.Type.MOVE -> mouseMove("mouseMove", p, inside = true)
            MouseEvent.Type.CLICK -> Unit
            MouseEvent.Type.ENTER -> mouseMove("mouseEnter", p, inside = true)
            MouseEvent.Type.EXIT -> mouseMove("mouseExit", p, inside = false)
            MouseEvent.Type.SCROLL -> Unit
        }
        views.dispatch(e)
    }

    eventDispatcher.onEvents(*KeyEvent.Type.ALL) { e ->
        Korge.logger.trace { "eventDispatcher.addEventListener<KeyEvent>:$e" }
        views.dispatch(e)
    }
    eventDispatcher.onEvents(*GestureEvent.Type.ALL) { e ->
        Korge.logger.trace { "eventDispatcher.addEventListener<GestureEvent>:$e" }
        views.dispatch(e)
    }

    eventDispatcher.onEvents(*DropFileEvent.Type.ALL) { e -> views.dispatch(e) }
    eventDispatcher.onEvent(ResumeEvent) { e ->
        views.dispatch(e)
        nativeSoundProvider.paused = false
    }
    eventDispatcher.onEvent(PauseEvent) { e ->
        views.dispatch(e)
        nativeSoundProvider.paused = true
    }
    eventDispatcher.onEvent(StopEvent) { e -> views.dispatch(e) }
    eventDispatcher.onEvent(DestroyEvent) { e ->
        try {
            views.dispatch(e)
        } finally {
            views.launchImmediately {
                views.close()
            }
        }
    }

    val touchMouseEvent = MouseEvent()
    eventDispatcher.onEvents(*TouchEvent.Type.ALL) { e ->
        Korge.logger.trace { "eventDispatcher.addEventListener<TouchEvent>:$e" }

        input.updateTouches(e)
        val ee = input.touch
        for (t in ee.touches) {
            t.p = getRealXY(t.x.toDouble(), t.y.toDouble(), e.scaleCoords)
        }
        views.dispatch(ee)

        // Touch to mouse events
        if (ee.numTouches == 1) {
            val start = ee.isStart
            val end = ee.isEnd
            val t = ee.touches.first()
            val p = t.p
            val x = t.x
            val y = t.y
            val button = MouseButton.LEFT

            //updateTouch(t.id, x, y, start, end)
            when {
                start -> mouseDown("onTouchStart", p, button)
                end -> mouseUp("onTouchEnd", p, button)
                else -> mouseMove("onTouchMove", p, inside = true)
            }
            views.dispatch(touchMouseEvent.also {
                it.id = 0
                it.button = button
                it.buttons = if (end) 0 else 1 shl button.id
                it.x = x.toInt()
                it.y = y.toInt()
                it.scaleCoords = false
                it.emulated = true
                it.type = when {
                    start -> MouseEvent.Type.DOWN
                    end -> MouseEvent.Type.UP
                    else -> MouseEvent.Type.DRAG
                }
            })
            if (end) {
                moveMouseOutsideInNextFrame = true
            }
        }

    }

    fun gamepadUpdated(e: GamePadUpdateEvent) {
        e.gamepads.fastForEach { gamepad ->
            input.gamepads[gamepad.index].copyFrom(gamepad)
        }
        input.updateConnectedGamepads()
    }

    eventDispatcher.onEvents(*GamePadConnectionEvent.Type.ALL) { e ->
        Korge.logger.trace { "eventDispatcher.addEventListener<GamePadConnectionEvent>:$e" }
        views.dispatch(e)
    }

    eventDispatcher.onEvent(GamePadUpdateEvent) { e ->
        gamepadUpdated(e)
        views.dispatch(e)
    }

    eventDispatcher.onEvent(ReshapeEvent) { e ->
        //try { throw Exception() } catch (e: Throwable) { e.printStackTrace() }
        //println("eventDispatcher.addEventListener<ReshapeEvent>: ${ag.backWidth}x${ag.backHeight} : ${e.width}x${e.height}")
        //println("resized. ${ag.backWidth}, ${ag.backHeight}")
        views.resized(ag.mainFrameBuffer.width, ag.mainFrameBuffer.height)
    }

    //println("eventDispatcher.dispatch(ReshapeEvent(0, 0, views.nativeWidth, views.nativeHeight)) : ${views.nativeWidth}x${views.nativeHeight}")
    eventDispatcher.dispatch(ReshapeEvent(0, 0, views.nativeWidth, views.nativeHeight))

    eventDispatcher.onEvent(ReloadEvent) { views.dispatch(it) }

    var renderShown = false
    views.clearEachFrame = clearEachFrame
    views.clearColor = bgcolor
    val firstRenderDeferred = CompletableDeferred<Unit>()

    fun renderBlock(event: RenderEvent) {
        //println("renderBlock: $event")
        try {
            views.frameUpdateAndRender(
                fixedSizeStep = fixedSizeStep,
                forceRender = views.forceRenderEveryFrame,
                doUpdate = event.update,
                doRender = event.render,
            )

            views.input.mouseOutside = false
            if (moveMouseOutsideInNextFrame) {
                moveMouseOutsideInNextFrame = false
                views.input.mouseOutside = true
                views.input.mouseInside = false
                views.mouseUpdated()
            }
        } catch (e: Throwable) {
            Korge.logger.error { "views.gameWindow.onRenderEvent:" }
            e.printStackTrace()
            if (views.rethrowRenderError) throw e
        }
    }

    views.gameWindow.onRenderEvent { event ->
        //println("RenderEvent: $event")
        if (!event.render) {
            renderBlock(event)
        } else {
            views.renderContext.doRender {
                if (!renderShown) {
                    //println("!!!!!!!!!!!!! views.gameWindow.addEventListener<RenderEvent>")
                    renderShown = true
                    firstRenderDeferred.complete(Unit)
                }
                renderBlock(event)
            }
        }
    }

    return firstRenderDeferred
}
