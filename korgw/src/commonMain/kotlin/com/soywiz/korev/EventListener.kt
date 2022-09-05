package com.soywiz.korev

import com.soywiz.korio.lang.Closeable

/**
 * Supports registering for [Event] of [EventType] and dispatching events.
 */
interface EventListener {
    /**
     * Registers a [handler] block to be executed when an even of [type] is [dispatch]ed
     */
    fun <T : TEvent<T>> addEventListener(type: EventType<T>, handler: (T) -> Unit): Closeable
    /**
     * Dispatched a [event] of [type] that will execute all the handlers registered with [addEventListener]
     * in this object and its children.
     */
    fun <T : TEvent<T>> dispatch(type: EventType<T>, event: T, result: EventResult? = null)
}

fun <T : TEvent<T>> EventListener.dispatchSimple(event: T) = dispatch(event.type, event)
fun <T : TEvent<T>> EventListener.dispatchWithResult(event: T): EventResult =
    EventResult().also { dispatch(event.type, event, it) }

class EventResult {
    var iterationCount: Int = 0
}

interface EventListenerChildren : EventListener {
    fun getEventListenerCounts(): Map<EventType<*>, Int>?
    fun getEventListenerCount(type: EventType<*>): Int
}

/*
interface BaseEventListenerContainer : EventListenerChildren {
    val __eventListenerParent: BaseEventListenerContainer?
    val __baseBaseEventListener: BaseEventListener
}

class BaseEventListener(val container: BaseEventListenerContainer) : EventListenerChildren {

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Event Listeners
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private class ListenerNode {
        val listeners = FastArrayList<(Any) -> Unit>()
        val temp = FastArrayList<(Any) -> Unit>()
    }
    /** @TODO: Critical. Consider two lists */
    private var __eventListeners: MutableMap<EventType<*>, ListenerNode>? = null

    override fun <T : Event> addEventListener(type: EventType<T>, handler: (T) -> Unit): Closeable {
        handler as (Any) -> Unit
        if (__eventListeners == null) __eventListeners = mutableMapOf()
        val lists = __eventListeners!!.getOrPut(type) { ListenerNode() }
        lists.listeners.add(handler)
        __updateChildListenerCount(type, +1)
        return Closeable {
            __updateChildListenerCount(type, -1)
            lists.listeners.remove(handler)
        }
    }

    override fun <T : Event> dispatch(type: EventType<T>, event: T) {
        val listeners = __eventListeners?.get(type)
        listeners?.listeners?.fastForEachWithTemp(listeners.temp) {
            it(event)
        }
    }

    /** @TODO: Critical. Consider a list and a [com.soywiz.kds.IntArrayList] */
    @PublishedApi
    internal var __eventListenerStats: MutableMap<EventType<*>, Int>? = null

    override fun getEventListenerCount(type: EventType<*>): Int {
        return __eventListenerStats?.getOrElse(type) { 0 } ?: 0
    }

    override fun getEventListenerCounts(): Map<EventType<*>, Int>? {
        return __eventListenerStats
    }

    //inline fun EventListenerChildren.Internal.__iterateListenerCount(block: (EventType<*>, Int) -> Unit) {
    fun __iterateListenerCount(block: (EventType<*>, Int) -> Unit) {
        __eventListenerStats?.forEach {
            block(it.key, it.value)
        }
    }

    val baseParent: BaseEventListenerContainer? get() = container.__eventListenerParent

    fun __updateChildListenerCount(type: EventType<*>, delta: Int) {
        if (delta == 0) return
        if (__eventListenerStats == null) __eventListenerStats = mutableMapOf<EventType<*>, Int>()
        __eventListenerStats?.put(type, __eventListenerStats!!.getOrElse(type) { 0 } + delta)
        baseParent?.__baseBaseEventListener?.__updateChildListenerCount(type, delta)
    }

    fun __updateChildListenerCount(view: BaseEventListenerContainer, add: Boolean) {
        //println("__updateChildListenerCount[$this]:view=$view,add=$add")
        view.__baseBaseEventListener.__iterateListenerCount { eventType, i ->
            //println("   - $eventType: $i")
            __updateChildListenerCount(eventType, if (add) +i else -i)
        }
    }
}
*/
