package com.soywiz.korge.input

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.korev.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*

class TouchEvents(val view: View) {
    data class Info(
        var index: Int = -1,
        var id: Int = 0,
        var local: MPoint = MPoint(),
        var startLocal: MPoint = MPoint(),
        var startTime: DateTime = DateTime.EPOCH,
        var global: MPoint = MPoint(),
        var startGlobal: MPoint = MPoint(),
        var time: DateTime = DateTime.EPOCH,
    ) : Extra by Extra.Mixin() {
        val elapsedTime get() = time - startTime

        lateinit var views: Views

        @Deprecated("") val localX: Double get() = local.x
        @Deprecated("") val localY: Double get() = local.y
        @Deprecated("") val startLocalX: Double get() = startLocal.x
        @Deprecated("") val startLocalY: Double get() = startLocal.y

        @Deprecated("") val globalX: Double get() = global.x
        @Deprecated("") val globalY: Double get() = global.y
        @Deprecated("") val startGlobalX: Double get() = startGlobal.x
        @Deprecated("") val startGlobalY: Double get() = startGlobal.y

        override fun toString(): String = "Touch[$id](${localX.toInt()}, ${localY.toInt()})"
    }

    val start = Signal<Info>()
    val move = Signal<Info>()
    val startAll = Signal<TouchEvents>()
    val moveAll = Signal<TouchEvents>()
    val endAll = Signal<TouchEvents>()
    val updateAll = Signal<TouchEvents>()
    val end = Signal<Info>()

    fun Info.copyFrom(touch: Touch) = this.apply {
        this.id = touch.id
        this.global.setTo(touch.x, touch.y)
        this.local.copyFrom(view.globalToLocal(Point(touch.x, touch.y)))
    }

    fun Info.start() = this.apply {
        this.startLocal.copyFrom(this.local)
        this.startGlobal.copyFrom(this.global)
    }

    private val infoPool = Pool { Info(it) }
    private val infoById = FastIntMap<Info>()
    val infos = FastArrayList<Info>()

    fun simulateTapAt(views: Views, globalXY: MPoint) {
        val ev = TouchEvent(TouchEvent.Type.START)
        ev.startFrame(TouchEvent.Type.START)
        ev.touch(0, globalXY.x, globalXY.y, Touch.Status.ADD)
        ev.endFrame()
        onTouchEvent(views, ev)
        ev.startFrame(TouchEvent.Type.END)
        ev.touch(0, globalXY.x, globalXY.y, Touch.Status.REMOVE)
        ev.endFrame()
        onTouchEvent(views, ev)

        view.mouse.click(view.mouse)
    }

    init {
        view.onEvents(*TouchEvent.Type.ALL) { e ->
            onTouchEvent(e.target as Views, e)
        }
    }

    private fun onTouchEvent(views: Views, e: TouchEvent) {
        infos.clear()

        //println("onTouchEvents: $e")

        val now = views.timeProvider.now()

        if (e.type == TouchEvent.Type.START) {
            e.touches.fastForEach {
                if (it.status != Touch.Status.KEEP) {
                    val info = infoPool.alloc().copyFrom(it).start()
                    info.startTime = now
                    info.views = views
                    infoById[info.id] = info
                    start(info)
                }
            }
        }

        e.touches.fastForEach {
            val info = infoById[it.id]
            if (info != null) {
                info.time = now
                infos.add(info.copyFrom(it))
            }
        }

        if (e.type == TouchEvent.Type.START) {
            startAll(this)
        }

        if (e.type == TouchEvent.Type.MOVE) {
            infos.fastForEach {
                move(it)
            }
            moveAll(this)
        }

        if (e.type == TouchEvent.Type.END) {
            e.touches.fastForEach {
                if (it.status != Touch.Status.KEEP) {
                    val info = infoById[it.id]
                    if (info != null) {
                        end(info.copyFrom(it))
                        endAll(this)
                        infoById.remove(info.id)
                        infoPool.free(info)
                    }
                }
            }
        }

        updateAll(this)
    }
}

val View.touch: TouchEvents by Extra.PropertyThis { TouchEvents(this) }
fun View.touch(block: TouchEvents.() -> Unit) = block(touch)

// @TODO: Handle several views covering other views (like MouseEvents)
/**
 * @param block This block is executed once for every different touch
 */
fun View.singleTouch(removeTouch: Boolean = false, supportStartAnywhere: Boolean = false, block: SingleTouchHandler.(id: Int) -> Unit) {
    data class SingleTouchInfo(val handler: SingleTouchHandler = SingleTouchHandler(), var startedInside: Boolean = false)

    val ids = FastIntMap<SingleTouchInfo>()
    fun getById(id: Int): SingleTouchInfo = ids.getOrPut(id) { SingleTouchInfo().also { it.handler.block(id) } }

    touch {
        start {
            val info = getById(it.id)
            //println("TOUCH START: info=$info")
            val handler = info.handler
            info.startedInside = this@singleTouch.hitTest(it.global.point) != null
            if (handler.start.hasListeners && info.startedInside) {
                handler.start(it)
            }
            if (supportStartAnywhere) {
                handler.startAnywhere(it)
            }
        }
        move {
            val info = getById(it.id)
            //println("TOUCH MOVE: info=$info")
            if (!supportStartAnywhere && !info.startedInside) return@move

            val handler = info.handler
            if (handler.move.hasListeners && this@singleTouch.hitTest(it.global.point) != null) {
                handler.move(it)
            }
            handler.moveAnywhere(it)
        }
        end {
            val info = getById(it.id)
            if (!supportStartAnywhere && !info.startedInside) return@end

            val handler = info.handler

            val hitTest = if (handler.end.hasListeners || handler.tap.hasListeners) this@singleTouch.hitTest(it.global.point) != null else false
            //println("TOUCH END: info=$info, hitTest=$hitTest")

            if (hitTest) {
                handler.end(it)
            }
            handler.endAnywhere(it)
            if ((MPoint.distance(it.startGlobal, it.global) <= it.views.input.clickDistance) && (it.elapsedTime <= it.views.input.clickTime)) {
                if (info.startedInside && hitTest) {
                    //println("TOUCH END: TAP!")
                    handler.tap(it)
                }
                handler.tapAnywhere(it)
            }

            if (removeTouch) {
                ids.remove(it.id)
            }
        }
    }
}

open class SingleTouchHandler {
    val start = Signal<TouchEvents.Info>()
    val startAnywhere = Signal<TouchEvents.Info>()
    val move = Signal<TouchEvents.Info>()
    val moveAnywhere = Signal<TouchEvents.Info>()
    val end = Signal<TouchEvents.Info>()
    val endAnywhere = Signal<TouchEvents.Info>()
    val tap = Signal<TouchEvents.Info>()
    val tapAnywhere = Signal<TouchEvents.Info>()
}
