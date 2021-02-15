package com.soywiz.korge.input

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korev.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*

class TouchEvents(override val view: View) : TouchComponent {
    data class Info(
        var index: Int = -1,
        var id: Int = 0,
        var local: Point = Point(),
        var startLocal: Point = Point(),
        var global: Point = Point(),
        var startGlobal: Point = Point(),
    ) : Extra by Extra.Mixin() {
        val localX: Double get() = local.x
        val localY: Double get() = local.y
        val startLocalX: Double get() = startLocal.x
        val startLocalY: Double get() = startLocal.y

        val globalX: Double get() = global.x
        val globalY: Double get() = global.y
        val startGlobalX: Double get() = startGlobal.x
        val startGlobalY: Double get() = startGlobal.y

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
        view.globalToLocalXY(touch.x, touch.y, this.local)
    }

    fun Info.start() = this.apply {
        this.startLocal.copyFrom(this.local)
        this.startGlobal.copyFrom(this.global)
    }

    private val infoPool = Pool { Info(it) }
    private val infoById = FastIntMap<Info>()
    val infos = FastArrayList<Info>()

    override fun onTouchEvent(views: Views, e: TouchEvent) {
        val actionTouch = e.actionTouch ?: e.touches.firstOrNull() ?: Touch.dummy

        infos.clear()

        if (e.type == TouchEvent.Type.START) {
            val info = infoPool.alloc().copyFrom(actionTouch).start()
            infoById[info.id] = info
            start(info)
        }

        e.touches.fastForEach {
            val info = infoById[it.id]
            if (info != null) {
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
            val info = infoById[actionTouch.id]
            if (info != null) {
                end(info.copyFrom(actionTouch))
                endAll(this)
                infoById.remove(info.id)
                infoPool.free(info)
            }
        }

        updateAll(this)
    }
}

fun View.touch(block: TouchEvents.() -> Unit) {
    block(getOrCreateComponentTouch { TouchEvents(this) })
}
