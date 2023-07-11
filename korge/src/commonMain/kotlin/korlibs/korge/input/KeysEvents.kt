package korlibs.korge.input

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.time.*
import korlibs.memory.*
import korlibs.event.*
import korlibs.korge.component.*
import korlibs.korge.time.*
import korlibs.korge.view.*
import korlibs.io.async.*
import korlibs.io.lang.*
import korlibs.math.interpolation.*
import kotlin.native.concurrent.*

class KeysEvents(val view: View) : Closeable {
    @PublishedApi
    internal lateinit var views: Views
    @PublishedApi
    internal val coroutineContext get() = views.coroutineContext

    private val onKeyDown = AsyncSignal<KeyEvent>()
    private val onKeyUp = AsyncSignal<KeyEvent>()
    private val onKeyTyped = AsyncSignal<KeyEvent>()

    fun KeyEvent.setFromKeys(key: Key, keys: InputKeys, dt: TimeSpan, type: KeyEvent.Type = KeyEvent.Type.DOWN): KeyEvent {
        this.type = type
        this.key = key
        this.keyCode = key.ordinal
        this.shift = keys.shift
        this.ctrl = keys.ctrl
        this.alt = keys.alt
        this.meta = keys.meta
        this.deltaTime = dt
        return this
    }

    /** Executes [callback] on each frame when [key] is being pressed. When [dt] is provided, the [callback] is executed at that [dt] steps. */
    fun downFrame(keys: List<Key>, dt: TimeSpan = TimeSpan.NIL, callback: (ke: KeyEvent) -> Unit): Cancellable {
        val ke = KeyEvent()
        return view.addOptFixedUpdater(dt) { dt ->
            if (::views.isInitialized) {
                val vkeys = views.keys
                keys.fastForEach { key ->
                    if (vkeys[key]) {
                        callback(ke.setFromKeys(key, vkeys, dt))
                    }
                }
            }
            //if (view.input)
        }
    }
    fun downFrame(vararg keys: Key, dt: TimeSpan = TimeSpan.NIL, callback: (ke: KeyEvent) -> Unit): Cancellable =
        downFrame(keys.toList(), dt, callback)
    fun downFrame(key: Key, dt: TimeSpan = TimeSpan.NIL, callback: (ke: KeyEvent) -> Unit): Cancellable =
        downFrame(listOf(key), dt, callback)

    fun justDown(keys: List<Key>, callback: (ke: KeyEvent) -> Unit): Cancellable {
        val ke = KeyEvent()
        return view.addUpdaterWithViews { views, dt ->
            val vkeys = views.keys
            keys.fastForEach { key ->
                if (vkeys.justPressed(key)) {
                    callback(ke.setFromKeys(key, vkeys, dt))
                }
            }
            //if (view.input)
        }
    }

    fun justDown(vararg keys: Key, callback: (ke: KeyEvent) -> Unit): Cancellable =
        justDown(keys.toList(), callback)
    fun justDown(key: Key, callback: (ke: KeyEvent) -> Unit): Cancellable =
        justDown(listOf(key), callback)

    fun downRepeating(keys: Set<Key>, maxDelay: TimeSpan = 500.milliseconds, minDelay: TimeSpan = 100.milliseconds, delaySteps: Int = 6, callback: suspend (ke: KeyEvent) -> Unit): Cancellable {
        val keys = keys.toList()
        val ke = KeyEvent()
        val currentStep = IntArray(keys.size)
        val remainingTime = Array(keys.size) { 0.milliseconds }
        return view.addUpdaterWithViews { views, dt ->
            val vkeys = views.keys
            keys.fastForEachWithIndex { index, key ->
                if (vkeys[key]) {
                    remainingTime[index] -= dt
                    if (remainingTime[index] < 0.milliseconds) {
                        val ratio = Ratio(currentStep[index], delaySteps).clamped
                        currentStep[index]++
                        remainingTime[index] += ratio.interpolate(maxDelay, minDelay)
                        launchImmediately(views.coroutineContext) {
                            callback(ke.setFromKeys(key, views.keys, dt))
                        }
                    }
                } else {
                    currentStep[index] = 0
                    remainingTime[index] = 0.milliseconds
                }
            }
        }
    }

    fun downRepeating(vararg keys: Key, maxDelay: TimeSpan = 500.milliseconds, minDelay: TimeSpan = 100.milliseconds, delaySteps: Int = 6, callback: suspend (ke: KeyEvent) -> Unit): Cancellable =
        downRepeating(keys.toSet(), maxDelay, minDelay, delaySteps, callback)
    fun downRepeating(key: Key, maxDelay: TimeSpan = 500.milliseconds, minDelay: TimeSpan = 100.milliseconds, delaySteps: Int = 6, callback: suspend (ke: KeyEvent) -> Unit): Cancellable =
        downRepeating(setOf(key), maxDelay, minDelay, delaySteps, callback)

    fun down(keys: Set<Key>, callback: suspend (key: KeyEvent) -> Unit): Closeable =
        onKeyDown { if (it.key in keys) callback(it) }
    fun down(callback: suspend (key: KeyEvent) -> Unit): Closeable = onKeyDown { callback(it) }
    fun down(key: Key, callback: suspend (key: KeyEvent) -> Unit): Closeable = onKeyDown { if (it.key == key) callback(it) }
    fun down(vararg keys: Key, callback: suspend (key: KeyEvent) -> Unit): Closeable = down(keys.toSet(), callback)

    fun downWithModifiers(keys: Set<Key>, ctrl: Boolean? = null, shift: Boolean? = null, alt: Boolean? = null, meta: Boolean? = null, callback: suspend (key: KeyEvent) -> Unit): Closeable = onKeyDown { e ->
        if (e.key in keys && match(ctrl, e.ctrl) && match(shift, e.shift) && match(alt, e.alt) && match(meta, e.meta)) callback(e)
    }
    fun downWithModifiers(key: Key, ctrl: Boolean? = null, shift: Boolean? = null, alt: Boolean? = null, meta: Boolean? = null, callback: suspend (key: KeyEvent) -> Unit): Closeable =
        downWithModifiers(setOf(key), ctrl, shift, alt, meta, callback)
    fun downWithModifiers(vararg keys: Key, ctrl: Boolean? = null, shift: Boolean? = null, alt: Boolean? = null, meta: Boolean? = null, callback: suspend (key: KeyEvent) -> Unit): Closeable =
        downWithModifiers(keys.toSet(), ctrl, shift, alt, meta, callback)

    private fun match(pattern: Boolean?, value: Boolean) = (pattern == null || value == pattern)

    fun up(keys: Set<Key>, callback: suspend (key: KeyEvent) -> Unit): Closeable = onKeyUp { e -> if (e.key in keys) callback(e) }
    fun up(callback: suspend (key: KeyEvent) -> Unit): Closeable = onKeyUp { e -> callback(e) }
    fun up(key: Key, callback: suspend (key: KeyEvent) -> Unit): Closeable = onKeyUp { e -> if (e.key == key) callback(e) }
    fun up(vararg keys: Key, callback: suspend (key: KeyEvent) -> Unit): Closeable = up(keys.toSet(), callback)

    fun typed(keys: Set<Key>, callback: suspend (key: KeyEvent) -> Unit): Closeable = onKeyTyped { if (it.key in keys) callback(it) }
    fun typed(callback: suspend (key: KeyEvent) -> Unit): Closeable = onKeyTyped { callback(it) }
    fun typed(key: Key, callback: suspend (key: KeyEvent) -> Unit): Closeable = onKeyTyped { if (it.key == key) callback(it) }
    fun typed(vararg keys: Key, callback: suspend (key: KeyEvent) -> Unit): Closeable = typed(keys.toSet(), callback)

    val closeable = CancellableGroup()

    init {
        view.onEvent(KeyEvent.Type.TYPE) { event -> views = event.target as Views; launchImmediately(views.coroutineContext) { onKeyTyped.invoke(event) } }
        view.onEvent(KeyEvent.Type.DOWN) { event -> views = event.target as Views; launchImmediately(views.coroutineContext) { onKeyDown.invoke(event) } }
        view.onEvent(KeyEvent.Type.UP) { event -> views = event.target as Views; launchImmediately(views.coroutineContext) { onKeyUp.invoke(event) } }
    }

    override fun close() {
        closeable.close()
    }
}

@ThreadLocal
val View.keys: KeysEvents by Extra.PropertyThis<View, KeysEvents> { KeysEvents(this) }

inline fun View.newKeys(callback: KeysEvents.() -> Unit): KeysEvents = KeysEvents(this).also {
    callback(it)
}

inline fun <T> View.keys(callback: KeysEvents.() -> T): T {
    return keys.run(callback)
}

suspend fun KeysEvents.waitUp(key: Key): KeyEvent = waitUp { it.key == key }
suspend fun KeysEvents.waitUp(filter: (key: KeyEvent) -> Boolean = { true }): KeyEvent =
    waitSubscriberCloseable { callback -> up { if (filter(it)) callback(it) } }
