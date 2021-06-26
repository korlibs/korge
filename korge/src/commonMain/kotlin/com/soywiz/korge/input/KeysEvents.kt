package com.soywiz.korge.input

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korev.*
import com.soywiz.korge.component.*
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import kotlinx.coroutines.*
import kotlin.reflect.*

class KeysEvents(override val view: View) : KeyComponent {
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
    fun downFrame(key: Key, dt: TimeSpan = TimeSpan.NIL, callback: (ke: KeyEvent) -> Unit): Cancellable {
        val ke = KeyEvent()
        return view.addOptFixedUpdater(dt) { dt ->
            if (::views.isInitialized) {
                val keys = views.keys
                if (keys[key]) {
                    callback(ke.setFromKeys(key, keys, dt))
                }
            }
            //if (view.input)
        }
    }

    fun justDown(key: Key, callback: (ke: KeyEvent) -> Unit): Cancellable {
        val ke = KeyEvent()
        return view.addUpdaterWithViews { views, dt ->
            val keys = views.keys
            if (keys.justPressed(key)) {
                callback(ke.setFromKeys(key, keys, dt))
            }
            //if (view.input)
        }
    }

    fun downRepeating(key: Key, maxDelay: TimeSpan = 500.milliseconds, minDelay: TimeSpan = 100.milliseconds, delaySteps: Int = 6, callback: suspend (ke: KeyEvent) -> Unit): Cancellable {
        val ke = KeyEvent()
        var currentStep = 0
        var remainingTime = 0.milliseconds
        return view.addUpdaterWithViews { views, dt ->
            val keys = views.keys
            if (keys[key]) {
                remainingTime -= dt
                if (remainingTime < 0.milliseconds) {
                    val ratio = (currentStep.toDouble() / delaySteps.toDouble()).clamp01()
                    currentStep++
                    remainingTime += ratio.interpolate(maxDelay, minDelay)
                    launchImmediately(views.coroutineContext) {
                        callback(ke.setFromKeys(key, views.keys, dt))
                    }
                }
            } else {
                currentStep = 0
                remainingTime = 0.milliseconds
            }
        }
    }

    fun down(callback: suspend (key: KeyEvent) -> Unit): Closeable = onKeyDown { e -> callback(e) }
    fun down(key: Key, callback: suspend (key: KeyEvent) -> Unit): Closeable = onKeyDown { e -> if (e.key == key) callback(e) }

    fun downWithModifiers(key: Key, ctrl: Boolean? = null, shift: Boolean? = null, alt: Boolean? = null, meta: Boolean? = null, callback: suspend (key: KeyEvent) -> Unit): Closeable = onKeyDown { e ->
        if (e.key == key && match(ctrl, e.ctrl) && match(shift, e.shift) && match(alt, e.alt) && match(meta, e.meta)) callback(e)
    }

    private fun match(pattern: Boolean?, value: Boolean) = (pattern == null || value == pattern)

    fun up(callback: suspend (key: KeyEvent) -> Unit): Closeable = onKeyUp { e -> callback(e) }
    fun up(key: Key, callback: suspend (key: KeyEvent) -> Unit): Closeable = onKeyUp { e -> if (e.key == key) callback(e) }

    fun typed(callback: suspend (key: KeyEvent) -> Unit): Closeable = onKeyTyped { e -> callback(e) }
    fun typed(key: Key, callback: suspend (key: KeyEvent) -> Unit): Closeable = onKeyTyped { e -> if (e.key == key) callback(e) }

    override fun Views.onKeyEvent(event: KeyEvent) {
        this@KeysEvents.views = this@onKeyEvent
		when (event.type) {
			KeyEvent.Type.TYPE -> launchImmediately(views.coroutineContext) { onKeyTyped.invoke(event) }
			KeyEvent.Type.DOWN -> launchImmediately(views.coroutineContext) { onKeyDown.invoke(event) }
			KeyEvent.Type.UP -> launchImmediately(views.coroutineContext) { onKeyUp.invoke(event) }
		}
	}
}

val View.keys by Extra.PropertyThis<View, KeysEvents> { this.getOrCreateComponentKey<KeysEvents> { KeysEvents(this) } }
inline fun <T> View.keys(callback: KeysEvents.() -> T): T = keys.run(callback)

suspend fun KeysEvents.waitUp(key: Key): KeyEvent = waitUp { it.key == key }
suspend fun KeysEvents.waitUp(filter: (key: KeyEvent) -> Boolean = { true }): KeyEvent =
    waitSubscriberCloseable { callback -> up { if (filter(it)) callback(it) } }
