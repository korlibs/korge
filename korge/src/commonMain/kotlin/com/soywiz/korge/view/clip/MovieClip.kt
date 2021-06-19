package com.soywiz.korge.view.clip

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.korge.annotations.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import kotlin.reflect.*

@KorgeExperimental
interface MovieClipTimeline {
    fun frame(time: TimeSpan, vararg props: V2<*>)
    fun update(time: TimeSpan)
}

@KorgeExperimental
open class MovieClip : Container(), MovieClipTimeline {
    @PublishedApi internal val states = FastArrayList<MovieClipState>()
    @PublishedApi internal val statesByName = LinkedHashMap<String?, MovieClipState>()

    fun registerState(it: MovieClipState) {
        statesByName[it.name] = it
        states.add(it)
    }

    inline fun state(name: String? = null, block: MovieClipState.() -> Unit = {}): MovieClipState {
        return MovieClipState(this, name).also(block).also { registerState(it) }
    }

    private val defaultState = state(null)
    private var elapsed: TimeSpan = 0.milliseconds
    private var currentStates: List<MovieClipState> = fastArrayListOf<MovieClipState>(defaultState)

    init {
        addUpdater {
            currentStates.fastForEach { currentState ->
                currentState.update(elapsed)
            }
            elapsed += it
        }
    }

    fun play(vararg states: MovieClipState) {
        currentStates = states.toFastList()
        elapsed = 0.seconds
    }

    fun play(vararg names: String?) {
        currentStates = names.mapNotNull { statesByName[it] }
        elapsed = 0.seconds
    }

    override fun frame(time: TimeSpan, vararg props: V2<*>) {
        defaultState.frame(time, *props)
    }

    override fun update(time: TimeSpan) {
        defaultState.update(time)
    }
}

@KorgeExperimental
open class MovieClipState(val def: MovieClip, val name: String?) : MovieClipTimeline {
    val timelines = LinkedHashMap<KMutableProperty0<*>, SortedMap<TimeSpan, V2<*>>>()

    fun play() = def.play(this)

    override fun frame(time: TimeSpan, vararg props: V2<*>) {
        props.fastForEach {
            val list = timelines.getOrPut(it.key) { SortedMap() }
            list[time] = it
        }
    }

    override fun update(time: TimeSpan) {
        for ((key, timeline) in timelines) {
            val key = key as KMutableProperty0<Any>
            val k0 = timeline.nearestLowExcludingExact(time)
            val k1 = timeline.nearestHigh(time)
            val v0 = k0?.let { timeline[k0] } as? V2<Any>?
            val v1 = k1?.let { timeline[k1] } as? V2<Any>?
            when {
                v0 == null -> {
                    v1!!.set(1.0)
                }
                v1 == null -> {
                    v0!!.set(1.0)
                }
                else -> {
                    val total = (k1!! - k0!!)
                    val elapsed = (time - k0)
                    val ratio = elapsed / total
                    key.set(v0.interpolator(ratio, v0.end, v1.end))
                }
            }
            //key.set(v0!!.interpolator())
            //println("$k0:$v0, $k1:$v1")
        }
    }
}
