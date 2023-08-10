@file:OptIn(ExperimentalStdlibApi::class)

package korlibs.event

import korlibs.datastructure.*
import korlibs.datastructure.iterators.fastForEach
import korlibs.datastructure.iterators.fastForEachWithTemp
import korlibs.io.lang.*

/**
 * Supports registering for [Event] of [EventType] and dispatching events.
 */
interface EventListener {
    /**
     * Registers a [handler] block to be executed when an event of [type] is [dispatch]ed
     */
    fun <T : BEvent> onEvent(type: EventType<T>, handler: (T) -> Unit): AutoCloseable

    fun <T : BEvent> onEvents(vararg etypes: EventType<out T>, handler: (T) -> Unit): AutoCloseable {
        if (etypes.isEmpty()) error("Must have at least one event type")
        val closeable = CancellableGroup()
        etypes.fastForEach { closeable += onEvent(it, handler) }
        return closeable
    }
    //fun clearEventListeners()

    /**
     * Dispatched a [event] of [type] that will execute all the handlers registered with [onEvents]
     * in this object and its children.
     */
    fun <T : BEvent> dispatch(type: EventType<T>, event: T, result: EventResult?, up: Boolean, down: Boolean): Boolean
    fun <T : BEvent> dispatch(type: EventType<T>, event: T, result: EventResult? = null): Boolean
        = dispatch(type, event, result, up = true, down = true)
    fun <T : BEvent> dispatchDown(type: EventType<T>, event: T, result: EventResult? = null): Boolean
        = dispatch(type, event, result, up = false, down = true)
    fun <T : BEvent> dispatchUp(type: EventType<T>, event: T, result: EventResult? = null): Boolean
        = dispatch(type, event, result, up = true, down = false)

    fun <T : BEvent> dispatch(event: T): Boolean = dispatch(event.fastCastTo<TEvent<T>>().type, event)

    fun <T : BEvent> dispatchWithResult(event: T, out: EventResult = EventResult()): EventResult {
        dispatch(event.fastCastTo<TEvent<T>>().type, event, out)
        return out
    }
}

fun <T : BEvent> EventListener.dispatchUp(event: T): Boolean = dispatchUp(event.fastCastTo<TEvent<T>>().type, event)
fun <T : BEvent> EventListener.dispatchDown(event: T): Boolean = dispatchDown(event.fastCastTo<TEvent<T>>().type, event)


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
    fun onEventsCount(): FastIdentityMap<EventType<*>, Int>?
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

open class BaseEventListener : EventListenerChildren, Extra {
    override var extra: ExtraType = null

    var eventListenerParent: BaseEventListener? = null
        private set

    protected fun changeEventListenerParent(other: BaseEventListener?) {
        this.eventListenerParent?.__updateChildListenerCount(this, add = false)
        this.eventListenerParent = other
        this.eventListenerParent?.__updateChildListenerCount(this, add = true)
    }

    /** @TODO: Critical. Consider two lists */
    //private var __eventListeners: MutableMap<EventType<*>, ListenerNode<*>>? = null
    private var __eventListeners: FastIdentityMap<EventType<*>, ListenerNode<*>>? = null
    /** @TODO: Critical. Consider a list and a [korlibs.datastructure.IntArrayList] */
    @PublishedApi
    internal var __eventListenerStats: FastIdentityMap<EventType<*>, Int>? = null

    protected class Listener<T: BEvent>(val func: (T) -> Unit, val node: ListenerNode<T>, val base: BaseEventListener) : CloseableCancellable {
        override fun close() {
            if (node.listeners.remove(this)) {
                base.__updateChildListenerCount(node.type, -1)
            }
        }

        fun attach() {
            node.listeners.add(this)
            base.__updateChildListenerCount(node.type, +1)
        }
    }

    protected class ListenerNode<T: BEvent>(val type: EventType<T>) {
        val listeners = FastArrayList<Listener<T>>()
        val temp = FastArrayList<Listener<T>>()
    }

    fun <T : BEvent> clearEvents(type: EventType<T>) {
        val lists = __eventListeners?.get(type)
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

    final override fun <T : BEvent> onEvent(type: EventType<T>, handler: (T) -> Unit): CloseableCancellable {
        if (__eventListeners == null) __eventListeners = FastIdentityMap()
        val lists: ListenerNode<T> = __eventListeners!!.getOrPut(type) { ListenerNode(type) } as ListenerNode<T>
        return Listener(handler, lists, this).also { it.attach() }
    }

    protected fun <T : BEvent> getListenersForType(type: EventType<T>): ListenerNode<T>? {
        return __eventListeners?.get(type) as? ListenerNode<T>
    }

    override fun <T : BEvent> dispatch(
        type: EventType<T>,
        event: T,
        result: EventResult?,
        up: Boolean,
        down: Boolean
    ): Boolean {
        //event._preventDefault = false
        val eventListenerCount = onEventCount(type)
        if (eventListenerCount <= 0) return false

        if (down) dispatchChildren(type, event, result)

        val listeners = getListenersForType(type)
        listeners?.listeners?.fastForEachWithTemp(listeners.temp) {
            it.func(event)
            result?.let { it.resultCount++ }
        }

        if (up) dispatchParent(type, event, result)

        result?.let { it.iterationCount++ }
        return event.defaultPrevented
    }
    // , result: EventResult?
    final override fun <T : BEvent> dispatch(type: EventType<T>, event: T, result: EventResult?): Boolean = super.dispatch(type, event, result)
    final override fun <T : BEvent> dispatch(event: T): Boolean = super.dispatch(event)
    final override fun <T : BEvent> dispatchWithResult(event: T, out: EventResult): EventResult = super.dispatchWithResult(event, out)

    open fun <T : BEvent> dispatchParent(type: EventType<T>, event: T, result: EventResult?) {
    }

    open fun <T : BEvent> dispatchChildren(type: EventType<T>, event: T, result: EventResult?) {
    }

    final override fun onEventCount(type: EventType<*>): Int {
        return __eventListenerStats?.getNull(type) ?: 0
    }

    fun getSelfEventCount(type: EventType<*>): Int {
        return getListenersForType(type)?.listeners?.size ?: 0
    }

    final override fun onEventsCount(): FastIdentityMap<EventType<*>, Int>? {
        return __eventListenerStats
    }

    //inline fun EventListenerChildren.Internal.__iterateListenerCount(block: (EventType<*>, Int) -> Unit) {

    private fun __updateChildListenerCount(type: EventType<*>, delta: Int) {
        if (delta == 0) return
        if (__eventListenerStats == null) __eventListenerStats = FastIdentityMap()
        __eventListenerStats?.set(type, (__eventListenerStats!![type] ?: 0) + delta)
        eventListenerParent?.__updateChildListenerCount(type, delta)
    }

    private fun __updateChildListenerCount(child: BaseEventListener, add: Boolean) {
        //println("__updateChildListenerCount[$this]:view=$view,add=$add")
        child.__eventListenerStats?.fastForEach { eventType, i ->
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

    /** @TODO: Critical. Consider a list and a [korlibs.datastructure.IntArrayList] */
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
