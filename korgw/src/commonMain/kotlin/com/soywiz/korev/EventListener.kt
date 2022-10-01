package com.soywiz.korev

import com.soywiz.kds.FastArrayList
import com.soywiz.kds.IntArrayList
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kds.iterators.fastForEachWithTemp
import com.soywiz.korio.lang.Closeable

/**
 * Supports registering for [Event] of [EventType] and dispatching events.
 */
interface EventListener {
    /**
     * Registers a [handler] block to be executed when an even of [type] is [dispatch]ed
     */
    fun <T : TEvent<T>> onEvent(type: EventType<T>, handler: (T) -> Unit): Closeable
    /**
     * Dispatched a [event] of [type] that will execute all the handlers registered with [onEvent]
     * in this object and its children.
     */
    fun <T : TEvent<T>> dispatch(type: EventType<T>, event: T, result: EventResult? = null)
}

fun <T : TEvent<T>> EventListener.dispatchSimple(event: T) = dispatch(event.type, event)
fun <T : TEvent<T>> EventListener.dispatchWithResult(event: T): EventResult =
    EventResult().also { dispatch(event.type, event, it) }

data class EventResult(
    var iterationCount: Int = 0,
    var resultCount: Int = 0,
) {
    fun reset() {
        iterationCount = 0
        resultCount = 0
    }
}

interface EventListenerChildren : EventListener {
    fun onEventsCount(): Map<EventType<*>, Int>?
    fun onEventCount(type: EventType<*>): Int
}

class EventListenerFastMap<K, V> {
    val keys: FastArrayList<K> = FastArrayList()
    val values: FastArrayList<V?> = FastArrayList()
    val counts: IntArrayList = IntArrayList()
    val size: Int get() = keys.size

    inline fun forEach(block: (key: K, value: V?, count: Int) -> Unit) {
        for (n in 0 until size) {
            block(keys[n], values[n], counts[n])
        }
    }

    fun set(key: K, value: V?, count: Int = -1): Int {
        val index = getKeyIndex(key)
        return if (index < 0) {
            keys.add(key)
            values.add(value)
            counts.add(if (count >= 0) count else 0)
            keys.size - 1
        } else {
            keys[index] = key
            if (value != null) values[index] = value
            if (count >= 0) counts[index] = count
            index
        }
    }

    // @TODO: Should we have a lookup map instead of indexOf?
    fun getKeyIndex(key: K): Int {
        return keys.indexOf(key)
    }

    fun getCount(key: K): Int {
        val index = getKeyIndex(key)
        return if (index >= 0) counts.getAt(index) else 0
    }

    fun setCount(key: K, count: Int) {
        if (getKeyIndex(key) < 0) {
            set(key, null, 0)
        }

        counts[getKeyIndex(key)] = count
    }

    fun getValue(key: K): V? {
        val index = getKeyIndex(key)
        if (index < 0) return null
        return values[index]
    }

    inline fun getOrPutValue(key: K, gen: () -> V): V {
        val index = getKeyIndex(key)
        if (index < 0 || values[index] == null) {
            val value = gen()
            set(key, value)
            return value
        }
        return values[index]!!
    }
}

open class BaseEventListener : EventListenerChildren {
    protected open val baseParent: BaseEventListener? get() = null
    /** @TODO: Critical. Consider two lists */
    private var __eventListeners: MutableMap<EventType<*>, ListenerNode>? = null
    /** @TODO: Critical. Consider a list and a [com.soywiz.kds.IntArrayList] */
    @PublishedApi
    internal var __eventListenerStats: MutableMap<EventType<*>, Int>? = null

    protected class ListenerNode {
        val listeners = FastArrayList<(Any) -> Unit>()
        val temp = FastArrayList<(Any) -> Unit>()
    }

    fun <T : TEvent<T>> clearEvents(type: EventType<T>) {
        val lists = __eventListeners?.getOrElse(type) { null }
        val listeners = lists?.listeners
        if (listeners != null) {
            __updateChildListenerCount(type, -listeners.size)
            __eventListeners?.clear()
        }
    }

    fun clearEvents() {
        val types = __eventListeners?.keys?.toList() ?: return
        types.fastForEach { clearEvents(it) }
    }

    final override fun <T : TEvent<T>> onEvent(type: EventType<T>, handler: (T) -> Unit): Closeable {
        handler as (Any) -> Unit
        if (__eventListeners == null) __eventListeners = mutableMapOf()
        val lists = __eventListeners!!.getOrPut(type) { ListenerNode() }
        lists.listeners.add(handler)
        __updateChildListenerCount(type, +1)
        return Closeable {
            if (lists.listeners.remove(handler)) {
                __updateChildListenerCount(type, -1)
            }
        }
    }

    // , result: EventResult?
    final override fun <T : TEvent<T>> dispatch(type: EventType<T>, event: T, result: EventResult?) {
        val eventListenerCount = onEventCount(type)
        if (eventListenerCount <= 0) return

        dispatchChildren(type, event, result)

        val listeners = __eventListeners?.get(type)
        listeners?.listeners?.fastForEachWithTemp(listeners.temp) {
            it(event)
        }
        result?.let { it.iterationCount++ }
    }

    protected open fun <T : TEvent<T>> dispatchChildren(type: EventType<T>, event: T, result: EventResult?) {
    }

    final override fun onEventCount(type: EventType<*>): Int {
        return __eventListenerStats?.getOrElse(type) { 0 } ?: 0
    }

    final override fun onEventsCount(): Map<EventType<*>, Int>? {
        return __eventListenerStats
    }

    //inline fun EventListenerChildren.Internal.__iterateListenerCount(block: (EventType<*>, Int) -> Unit) {
    private fun __iterateListenerCount(block: (EventType<*>, Int) -> Unit) {
        __eventListenerStats?.forEach {
            block(it.key, it.value)
        }
    }

    private fun __updateChildListenerCount(type: EventType<*>, delta: Int) {
        if (delta == 0) return
        if (__eventListenerStats == null) __eventListenerStats = mutableMapOf()
        __eventListenerStats?.put(type, __eventListenerStats!!.getOrElse(type) { 0 } + delta)
        baseParent?.__updateChildListenerCount(type, delta)
    }

    protected fun __updateChildListenerCount(child: BaseEventListener, add: Boolean) {
        //println("__updateChildListenerCount[$this]:view=$view,add=$add")
        child.__iterateListenerCount { eventType, i ->
            //println("   - $eventType: $i")
            __updateChildListenerCount(eventType, if (add) +i else -i)
        }
    }
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
