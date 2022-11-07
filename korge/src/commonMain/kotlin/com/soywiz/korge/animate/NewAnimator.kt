package com.soywiz.korge.animate

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.korge.component.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*
import kotlin.math.*

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class NewAnimatorDslMarker

interface NewAnimatorNode {
    //fun reset()
    val totalTime: TimeSpan
    fun update(time: TimeSpan)
    fun complete()
}

open class NewAnimator(
    val view: View,
    val time: TimeSpan = DEFAULT_TIME,
    val speed: Double = DEFAULT_SPEED,
    val easing: Easing = DEFAULT_EASING,
    val completeOnCancel: Boolean = DEFAULT_COMPLETE_ON_CANCEL,
    val parallel: Boolean = false,
    val looped: Boolean = false,
    val parent: NewAnimator? = null
) : NewAnimatorNode, CloseableCancellable {
    companion object {
        val DEFAULT_TIME = 500.milliseconds
        val DEFAULT_SPEED = 128.0 // Points per second
        val DEFAULT_EASING = Easing.EASE_IN_OUT_QUAD
        val DEFAULT_COMPLETE_ON_CANCEL = true
    }

    private var _onCancel: () -> Unit = {}

    val onComplete = Signal<Unit>()

    fun onCancel(action: () -> Unit) {
        _onCancel = action
    }

    private val nodes = Deque<NewAnimatorNode>()

    fun addNode(node: NewAnimatorNode) {
        nodes.add(node)
        totalTime += node.totalTime
        ensure()
    }

    override var totalTime: TimeSpan = 0.seconds
    private var currentTime: TimeSpan = 0.seconds
    private var updater: UpdateComponent? = null

    fun ensure() {
        if (parent != null) return parent.ensure()
        if (updater == null || view.hasComponent(updater!!)) {
            updater = view.addUpdater {
                currentTime += it
                update(currentTime)
            }
        }
    }

    private var currentNode: NewAnimatorNode? = null

    override fun update(time: TimeSpan) {
        var time = time
        val rtime = time.clamp(0.seconds, totalTime)
        when (parallel) {
            false -> {
                if (currentNode == null) {
                    currentNode = if (nodes.isNotEmpty()) nodes.removeFirst() else null
                    if (looped) currentNode?.let { nodes.addLast(it) }
                    time = 0.seconds
                    //println("UPDATE: $time, size=${nodes.size}, time=$time, totalTime=$totalTime, currentNode=$currentNode")
                }
                currentNode?.let { node ->
                    val nodeTime = (time).clamp(0.seconds, node.totalTime)
                    node.update(nodeTime)
                    //println("## time=$time, nodeTime=$nodeTime, currentTime=$currentTime, node.totalTime=${node.totalTime}")
                    if (nodeTime >= node.totalTime) {
                        currentTime -= node.totalTime
                        totalTime -= node.totalTime
                        currentNode = null
                    }
                }
            }
            true -> {
                nodes.forEach {
                    it.update(rtime)
                }
            }
        }
        //if (rtime >= totalTime && ((currentNode == null && nodes.isEmpty()) || parallel || looped)) {
        if (currentTime >= totalTime) {
            //println("---- COMPLETED: looped=$looped, rtime=$rtime, totalTime=$totalTime, nodes=${nodes.size}, currentNode=$currentNode")
            if (looped) {
                currentTime = 0.seconds
                onComplete()
            } else {
                cancel()
            }
        }
    }

    /** Suspends until this animation has been completed */
    suspend fun awaitComplete() {
        onComplete.waitOne()
    }

    override fun close() {
        cancel()
    }

    fun cancel() {
        //println("---- CANCEL: looped=$looped, currentTime=$currentTime, totalTime=$totalTime")

        totalTime = 0.seconds
        currentTime = 0.seconds
        currentNode = null
        nodes.clear()
        updater?.close()
        updater = null
        onComplete()
    }

    override fun complete() {
        currentNode?.also {
            currentNode = null
            it.complete()
        }
        while (nodes.isNotEmpty()) nodes.removeFirst().complete()
        cancel()
    }

    inline fun parallel(
        time: TimeSpan = this.totalTime,
        speed: Double = this.speed,
        easing: Easing = this.easing,
        completeOnCancel: Boolean = this.completeOnCancel,
        looped: Boolean = false,
        callback: @NewAnimatorDslMarker NewAnimator.() -> Unit
    ) = NewAnimator(view, time, speed, easing, completeOnCancel, true, looped).also(callback).also { addNode(it) }

    inline fun sequence(
        time: TimeSpan = this.totalTime,
        speed: Double = this.speed,
        easing: Easing = this.easing,
        completeOnCancel: Boolean = this.completeOnCancel,
        looped: Boolean = false,
        callback: @NewAnimatorDslMarker NewAnimator.() -> Unit
    ) = NewAnimator(view, time, speed, easing, completeOnCancel, false, looped).also(callback).also { addNode(it) }

    fun parallelLazy(
        time: TimeSpan = this.totalTime,
        speed: Double = this.speed,
        easing: Easing = this.easing,
        completeOnCancel: Boolean = this.completeOnCancel,
        looped: Boolean = false,
        init: @NewAnimatorDslMarker NewAnimator.() -> Unit
    ) = NewAnimator(view, time, speed, easing, completeOnCancel, true, looped).also(init).also { addNode(it) }

    fun sequenceLazy(
        time: TimeSpan = this.totalTime,
        speed: Double = this.speed,
        easing: Easing = this.easing,
        completeOnCancel: Boolean = this.completeOnCancel,
        looped: Boolean = false,
        init: @NewAnimatorDslMarker NewAnimator.() -> Unit
    ) = NewAnimator(view, time, speed, easing, completeOnCancel, false, looped).also(init).also { addNode(it) }

    class TweenNode(
        vararg val vs: V2<*>,
        val lazyVs: Array<out () -> V2<*>>? = null,
        val time: TimeSpan = 1000.milliseconds,
        val lazyTime: (() -> TimeSpan)? = null,
        val easing: Easing,
        val name: String? = null
    ) : NewAnimatorNode {
        val computedVs by lazy {
            if (lazyVs != null) Array(lazyVs.size) { lazyVs[it]() } else vs
        }

        override val totalTime: TimeSpan by lazy {
            lazyTime?.invoke() ?: time
        }

        override fun toString(): String = "TweenNode(totalTime=$totalTime, name=$name)"

        override fun update(time: TimeSpan) {
            computedVs.fastForEach {
                if (time == 0.seconds) it.init()
                it.set(easing.invoke(time / this.totalTime))
            }
        }

        override fun complete() {
            computedVs.fastForEach { it.set(1.0) }
        }
    }

    @PublishedApi
    internal fun __tween(vararg vs: V2<*>, lazyVs: Array<out () -> V2<*>>? = null, time: TimeSpan = this.time, lazyTime: (() -> TimeSpan)? = null, easing: Easing = this.easing, name: String? = null) {
        addNode(TweenNode(*vs, lazyVs = lazyVs, time = time, lazyTime = lazyTime, easing = easing, name = name))
    }

    @PublishedApi
    internal fun __tween(vararg vs: () -> V2<*>, time: TimeSpan = this.time, lazyTime: (() -> TimeSpan)? = null, easing: Easing = this.easing, name: String? = null) {
        addNode(TweenNode(lazyVs = vs, time = time, lazyTime = lazyTime, easing = easing, name = name))
    }

    fun tween(vararg vs: V2<*>, time: TimeSpan = this.time, easing: Easing = this.easing, name: String? = null) = __tween(*vs, time = time, easing = easing, name = name)
    fun tween(vararg vs: V2<*>, time: () -> TimeSpan = { this.time }, easing: Easing = this.easing, name: String? = null) = __tween(*vs, lazyTime = time, easing = easing, name = name)

    fun tween(vararg vs: () -> V2<*>, time: TimeSpan = this.time, easing: Easing = this.easing, name: String? = null) = __tween(*vs, time = time, easing = easing, name = name)
    fun tween(vararg vs: () -> V2<*>, time: () -> TimeSpan = { this.time }, easing: Easing = this.easing, name: String? = null) = __tween(*vs, lazyTime = time, easing = easing, name = name)

    fun View.scaleBy(scaleX: Double, scaleY: Double = scaleX, time: TimeSpan = this@NewAnimator.time, easing: Easing = this@NewAnimator.easing) = __tween({ this::scaleX[this.scaleX + scaleX] }, { this::scaleY[this.scaleY + scaleY] }, time = time, easing = easing, name = "scaleBy")
    fun View.rotateBy(rotation: Angle, time: TimeSpan = this@NewAnimator.time, easing: Easing = this@NewAnimator.easing) = __tween({ this::rotation[this.rotation + rotation] }, time = time, easing = easing, name = "rotateBy")
    fun View.moveBy(x: Double = 0.0, y: Double = 0.0, time: TimeSpan = this@NewAnimator.time, easing: Easing = this@NewAnimator.easing) = __tween({ this::x[this.x + x] }, { this::y[this.y + y] }, time = time, easing = easing, name = "moveBy")
    fun View.moveByWithSpeed(x: Double = 0.0, y: Double = 0.0, speed: Double = this@NewAnimator.speed, easing: Easing = this@NewAnimator.easing) = __tween({ this::x[this.x + x] }, { this::y[this.y + y] }, lazyTime = { (hypot(x, y) / speed.toDouble()).seconds }, easing = easing, name = "moveByWithSpeed")

    fun View.scaleTo(scaleX: () -> Number, scaleY: () -> Number = scaleX, time: TimeSpan = this@NewAnimator.time, lazyTime: (() -> TimeSpan)? = null, easing: Easing = this@NewAnimator.easing) = __tween({ this::scaleX[scaleX()] }, { this::scaleY[scaleY()] }, time = time, lazyTime = lazyTime, easing = easing, name = "scaleTo")
    fun View.scaleTo(scaleX: Double, scaleY: Double = scaleX, time: TimeSpan = this@NewAnimator.time, easing: Easing = this@NewAnimator.easing) = __tween(this::scaleX[scaleX], this::scaleY[scaleY], time = time, easing = easing, name = "scaleTo")
    fun View.scaleTo(scaleX: Float, scaleY: Float = scaleX, time: TimeSpan = this@NewAnimator.time, easing: Easing = this@NewAnimator.easing) = scaleTo(scaleX.toDouble(), scaleY.toDouble(), time, easing)
    fun View.scaleTo(scaleX: Int, scaleY: Int = scaleX, time: TimeSpan = this@NewAnimator.time, easing: Easing = this@NewAnimator.easing) = scaleTo(scaleX.toDouble(), scaleY.toDouble(), time, easing)

    fun View.moveTo(x: () -> Number = { this.x }, y: () -> Number = { this.y }, time: TimeSpan = this@NewAnimator.time, lazyTime: (() -> TimeSpan)? = null, easing: Easing = this@NewAnimator.easing) = __tween({ this::x[x()] }, { this::y[y()] }, time = time, lazyTime = lazyTime, easing = easing, name = "moveTo")
    fun View.moveTo(x: Double, y: Double, time: TimeSpan = this@NewAnimator.time, easing: Easing = this@NewAnimator.easing) = __tween(this::x[x], this::y[y], time = time, easing = easing, name = "moveTo")
    fun View.moveTo(x: Float, y: Float, time: TimeSpan = this@NewAnimator.time, easing: Easing = this@NewAnimator.easing) = moveTo(x.toDouble(), y.toDouble(), time, easing)
    fun View.moveTo(x: Int, y: Int, time: TimeSpan = this@NewAnimator.time, easing: Easing = this@NewAnimator.easing) = moveTo(x.toDouble(), y.toDouble(), time, easing)

    fun View.moveToWithSpeed(x: () -> Number = { this.x }, y: () -> Number = { this.y }, speed: () -> Number = { this@NewAnimator.speed }, easing: Easing = this@NewAnimator.easing) = __tween({ this::x[x()] }, { this::y[y()] }, lazyTime = { (hypot(this.x - x().toDouble(), this.y - y().toDouble()) / speed().toDouble()).seconds }, easing = easing, name = "moveToWithSpeed")
    fun View.moveToWithSpeed(x: Double, y: Double, speed: Double = this@NewAnimator.speed, easing: Easing = this@NewAnimator.easing) = __tween(this::x[x], this::y[y], lazyTime = { (hypot(this.x - x, this.y - y) / speed.toDouble()).seconds }, easing = easing, name = "moveToWithSpeed")
    fun View.moveToWithSpeed(x: Float, y: Float, speed: Number = this@NewAnimator.speed, easing: Easing = this@NewAnimator.easing) = moveToWithSpeed(x.toDouble(), y.toDouble(), speed.toDouble(), easing)
    fun View.moveToWithSpeed(x: Int, y: Int, speed: Number = this@NewAnimator.speed, easing: Easing = this@NewAnimator.easing) = moveToWithSpeed(x.toDouble(), y.toDouble(), speed.toDouble(), easing)

    fun View.alpha(alpha: Double, time: TimeSpan = this@NewAnimator.time, easing: Easing = this@NewAnimator.easing, name: String? = "alpha") = __tween(this::alpha[alpha], time = time, easing = easing, name = name)
    fun View.alpha(alpha: Float, time: TimeSpan = this@NewAnimator.time, easing: Easing = this@NewAnimator.easing, name: String? = "alpha") = alpha(alpha.toDouble(), time, easing, name = name)
    fun View.alpha(alpha: Int, time: TimeSpan = this@NewAnimator.time, easing: Easing = this@NewAnimator.easing, name: String? = "alpha") = alpha(alpha.toDouble(), time, easing, name = name)

    fun View.rotateTo(angle: Angle, time: TimeSpan = this@NewAnimator.time, easing: Easing = this@NewAnimator.easing) = __tween(this::rotation[angle], time = time, easing = easing, name = "rotateTo")
    fun View.rotateTo(rotation: () -> Angle, time: TimeSpan = this@NewAnimator.time, lazyTime: (() -> TimeSpan)? = null, easing: Easing = this@NewAnimator.easing) = __tween({ this::rotation[rotation()] }, time = time, lazyTime = lazyTime, easing = easing, name = "rotateTo")

    fun View.show(time: TimeSpan = this@NewAnimator.time, easing: Easing = this@NewAnimator.easing) = alpha(1.0, time, easing, name = "show")
    fun View.hide(time: TimeSpan = this@NewAnimator.time, easing: Easing = this@NewAnimator.easing) = alpha(0.0, time, easing, name = "hide")

    fun wait(time: TimeSpan = this.time) = __tween(time = time, name = "wait")
    fun wait(time: () -> TimeSpan) = __tween(lazyTime = time, name = "wait")

    fun block(name: String? = null, callback: () -> Unit) {
        addNode(object : NewAnimatorNode {
            override fun toString(): String = "BaseAnimatorNode.Block(name=$name)"

            var executed = false
            override val totalTime: TimeSpan get() = 0.seconds

            override fun update(time: TimeSpan) = complete()
            override fun complete() {
                if (executed) return
                executed = true
                callback()
            }
        })
    }
}

fun View.newAnimator(
    time: TimeSpan = NewAnimator.DEFAULT_TIME,
    speed: Double = NewAnimator.DEFAULT_SPEED,
    easing: Easing = NewAnimator.DEFAULT_EASING,
    completeOnCancel: Boolean = NewAnimator.DEFAULT_COMPLETE_ON_CANCEL,
    parallel: Boolean = false,
    looped: Boolean = false,
    block: @NewAnimatorDslMarker NewAnimator.() -> Unit = {}
): NewAnimator = NewAnimator(this, time, speed, easing, completeOnCancel, parallel, looped, parent = null).apply(block)
