package com.soywiz.korge.input

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korge.baseview.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import kotlin.reflect.*

class GestureEvents(val view: BaseView) {
    val lastEvent = GestureEvent()
    /** Trackpad pinch zooming. Only implemented on MacOS for now */
    val magnify = Signal<GestureEvents>()
    /** Trackpad pinch rotation. Only implemented on MacOS for now */
    val rotate = Signal<GestureEvents>()
    val swipe = Signal<GestureEvents>()
    val smartZoom = Signal<GestureEvents>()
    val id: Int get() = lastEvent.id
    val amount: Double get() = lastEvent.amount

    lateinit var views: Views
        private set

    init {
        view.onEvent(*GestureEvent.Type.ALL) { event ->
            this.views = event.target as Views
            lastEvent.copyFrom(event)
            when (event.type) {
                GestureEvent.Type.MAGNIFY -> this.magnify(this)
                GestureEvent.Type.ROTATE -> this.magnify(this)
                GestureEvent.Type.SWIPE -> this.swipe(this)
                GestureEvent.Type.SMART_MAGNIFY -> this.smartZoom(this)
            }
            //println("onGestureEvent: $event")
        }
    }
}

val View.gestures by Extra.PropertyThis { GestureEvents(this) }

inline fun <T> View.gestures(callback: GestureEvents.() -> T): T = gestures.run(callback)

/** Trackpad pinch zooming. Only implemented on MacOS for now */
inline fun <T : View?> T.onMagnify(noinline handler: @EventsDslMarker suspend (GestureEvents) -> Unit) =
    doGestureEvent(GestureEvents::magnify, handler)

inline fun <T : View?> T.onSwipe(noinline handler: @EventsDslMarker suspend (GestureEvents) -> Unit) =
    doGestureEvent(GestureEvents::swipe, handler)

/** Trackpad pinch rotation. Only implemented on MacOS for now */
inline fun <T : View?> T.onRotate(noinline handler: @EventsDslMarker suspend (GestureEvents) -> Unit) =
    doGestureEvent(GestureEvents::rotate, handler)

inline fun <T : View?> T.onSmartZoom(noinline handler: @EventsDslMarker suspend (GestureEvents) -> Unit) =
    doGestureEvent(GestureEvents::smartZoom, handler)

@PublishedApi
internal inline fun <T : View?> T?.doGestureEvent(
    prop: KProperty1<GestureEvents, Signal<GestureEvents>>,
    noinline handler: suspend (GestureEvents) -> Unit
): T? {
    this?.gestures?.let { gestures ->
        prop.get(gestures).add { launchImmediately(gestures.views.coroutineContext) { handler(it) } }
    }
    return this
}
