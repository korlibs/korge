package korlibs.korge.animate

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.time.*
import korlibs.korge.annotations.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.io.lang.*
import korlibs.math.*
import korlibs.math.interpolation.*
import kotlin.reflect.*
import kotlin.time.*

@KorgeExperimental
val View.animStateManager by Extra.PropertyThis { AnimatorStateManager(this) }

@KorgeExperimental
data class AnimState(val transitions: List<V2<*>>, val fastTime: FastDuration = 0.5.fastSeconds, val easing: Easing = Easing.LINEAR) {
    constructor(vararg transitions: V2<*>, fastTime: FastDuration = 0.5.fastSeconds, easing: Easing = Easing.LINEAR) : this(transitions.toList(), fastTime = fastTime, easing = easing)

    constructor(transitions: List<V2<*>>, time: Duration, easing: Easing) : this(transitions, time.fast, easing)
    constructor(vararg transitions: V2<*>, time: Duration, easing: Easing = Easing.LINEAR) : this(transitions.toList(), time = time, easing = easing)

    val time: Duration get() = fastTime.slow

    operator fun plus(other: AnimState): AnimState = AnimState(
        (other.transitions.associateBy { it.key } + transitions.associateBy { it.key }).values.toList(),
        fastTime = fastTime,
        easing = easing
    )
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

    private var currentTime: FastDuration = FastDuration.ZERO
    private var currentState: AnimState = AnimState()

    fun add(vararg states: AnimState) {
        // Keeps other states running
        TODO()
    }

    fun set(vararg states: AnimState) {
        states.fastForEach { backup(it) }
        val default = defaultState()
        currentTime = FastDuration.ZERO
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
            updater = view.addFastUpdater {
                update(it)
            }
        }
    }

    fun update(dt: FastDuration) {
        val isStart = currentTime == FastDuration.ZERO
        currentTime += dt
        var completedCount = 0
        currentState.transitions.fastForEach {
            val startTime = it.startTime.seconds
            val endTime = it.endTime(currentState.time).seconds
            val ratio = currentTime.seconds.convertRange(startTime, endTime, 0.0, 1.0)
            if (isStart) it.init()
            it.set(currentState.easing(ratio.clamp01()).toRatio())
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
