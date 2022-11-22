package com.soywiz.korge.animate

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korge.annotations.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.interpolation.*
import com.soywiz.korma.math.*
import com.soywiz.korma.math.convertRange
import kotlin.reflect.*

@KorgeExperimental
val View.animStateManager by Extra.PropertyThis { AnimatorStateManager(this) }

@KorgeExperimental
data class AnimState(val transitions: List<V2<*>>, val time: TimeSpan = 0.5.seconds, val easing: Easing = Easing.LINEAR) {
    constructor(vararg transitions: V2<*>, time: TimeSpan = 0.5.seconds, easing: Easing = Easing.LINEAR) : this(transitions.toList(), time = time, easing = easing)

    operator fun plus(other: AnimState): AnimState {
        return AnimState(
            (other.transitions.associateBy { it.key } + transitions.associateBy { it.key }).values.toList(),
            time = time,
            easing = easing
        )
    }
}

class AnimatorStateManager(val view: View) {
    data class Entry<T>(val v2: V2<T>, val value: T)
    val defaults = LinkedHashMap<KMutableProperty0<*>, Entry<*>>()

    private fun backup(state: AnimState) {
        // Backup all the values we don't have yet
        state.transitions?.fastForEach { trans ->
            defaults.getOrPut(trans.key) { Entry<Any?>(trans as V2<Any?>, trans.get()) }
        }
    }

    private fun defaultState(): AnimState {
        return AnimState(defaults.map { (_, v) ->
            (v.v2 as V2<Any?>).copy(includeStart = false, end = v.value)
        })
    }

    private var currentTime: TimeSpan = TimeSpan.ZERO
    private var currentState: AnimState = AnimState()

    fun add(vararg states: AnimState) {
        // Keeps other states running
        TODO()
    }

    fun set(vararg states: AnimState) {
        states.fastForEach { backup(it) }
        val default = defaultState()
        currentTime = TimeSpan.ZERO
        if (states.isEmpty()) {
            currentState = default
        } else {
            //val statesDefault = (states.toList() + default)
            //currentState = statesDefault.fold(statesDefault.first()) { a, b -> a + b }
            currentState = states.fold(states.first()) { a, b -> a + b }
        }
        ensureUpdater()
    }

    private var updater: Cancellable? = null

    fun ensureUpdater() {
        if (updater == null) {
            updater = view.addUpdater {
                update(it)
            }
        }
    }

    fun update(dt: TimeSpan) {
        val isStart = currentTime == TimeSpan.ZERO
        currentTime += dt
        var completedCount = 0
        currentState.transitions.fastForEach {
            val startTime = it.startTime.seconds
            val endTime = it.endTime(currentState.time).seconds
            val ratio = currentTime.seconds.convertRange(startTime, endTime, 0.0, 1.0)
            if (isStart) it.init()
            it.set(currentState.easing(ratio.clamp01()))
            if (ratio >= 1.0) {
                completedCount++
            }
        }
        if (completedCount >= currentState.transitions.size) {
            updater?.cancel()
            updater = null
        }
    }
}
