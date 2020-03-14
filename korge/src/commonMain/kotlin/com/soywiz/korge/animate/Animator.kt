package com.soywiz.korge.animate

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korma.interpolation.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.math.*

interface BaseAnimatorNode {
    suspend fun execute()
    fun executeImmediately()
}

open class Animator(
    val root: View,
    val time: TimeSpan = DEFAULT_TIME,
    val speed: Double = DEFAULT_SPEED,
    val easing: Easing = DEFAULT_EASING,
    val completeOnCancel: Boolean = DEFAULT_COMPLETE_ON_CANCEL,
    val kind: NodeKind = NodeKind.Sequence
) : BaseAnimatorNode {
    companion object {
        val DEFAULT_TIME = 0.5.seconds
        val DEFAULT_SPEED = 128.0 // Points per second
        val DEFAULT_EASING = Easing.EASE_IN_OUT_QUAD
        val DEFAULT_COMPLETE_ON_CANCEL = true
        //val DEFAULT_CANCEL = OnCancel.JustCancel
    }

    enum class NodeKind {
        Parallel, Sequence
    }

    enum class OnCancel {
        JustCancel, Complete
    }

    @PublishedApi
    internal val nodes = Deque<BaseAnimatorNode>()

    override suspend fun execute() {
        when (kind) {
            NodeKind.Sequence -> {
                try {
                    while (nodes.isNotEmpty()) nodes.removeFirst().execute()
                } catch (e: CancellationException) {
                    println("CancellationException")
                    if (completeOnCancel) {
                        while (nodes.isNotEmpty()) nodes.removeFirst().executeImmediately()
                    }
                }
            }
            NodeKind.Parallel -> {
                val jobs = arrayListOf<Job>()
                while (nodes.isNotEmpty()) {
                    val node = nodes.removeFirst()
                    jobs += launchImmediately(coroutineContext) { node.execute() }
                }
                jobs.joinAll()
            }
        }
    }

    override fun executeImmediately() {
        while (nodes.isNotEmpty()) nodes.removeFirst().executeImmediately()
    }

    inline fun parallel(
        time: TimeSpan = this.time,
        speed: Double = this.speed,
        easing: Easing = this.easing,
        completeOnCancel: Boolean = this.completeOnCancel,
        callback: Animator.() -> Unit
    ) = Animator(root, time, speed, easing, completeOnCancel, NodeKind.Parallel).apply(callback).also { nodes.add(it) }

    inline fun sequence(
        time: TimeSpan = this.time,
        speed: Double = this.speed,
        easing: Easing = this.easing,
        completeOnCancel: Boolean = this.completeOnCancel,
        callback: Animator.() -> Unit
    ) = Animator(root, time, speed, easing, completeOnCancel, NodeKind.Sequence).apply(callback).also { nodes.add(it) }

    inner class TweenNode(val view: View, vararg val vfs: V2<*>, val time: TimeSpan = 1.seconds, val lazyTime: (() -> TimeSpan)? = null, val easing: Easing) : BaseAnimatorNode {
        override suspend fun execute() {
            try {
                view.tween(*vfs, time = if (lazyTime != null) lazyTime!!() else time, easing = easing)
            } catch (e: CancellationException) {
                //println("TweenNode: $e")
                if (completeOnCancel) {
                    executeImmediately()
                }
            }
        }
        override fun executeImmediately() = vfs.fastForEach { it.set(1.0) }
    }

    @PublishedApi
    internal fun __tween(vararg vfs: V2<*>, time: TimeSpan = this.time, lazyTime: (() -> TimeSpan)? = null, easing: Easing = this.easing) {
        nodes.add(TweenNode(root, *vfs, time = time, lazyTime = lazyTime, easing = easing))
    }

    fun tween(vararg vfs: V2<*>, time: TimeSpan = this.time, easing: Easing = this.easing) = __tween(*vfs, time = time, easing = easing)
    fun tween(vararg vfs: V2<*>, time: () -> TimeSpan = { this.time }, easing: Easing = this.easing) = __tween(*vfs, lazyTime = time, easing = easing)

    fun View.moveTo(x: Double, y: Double, time: TimeSpan = this@Animator.time, easing: Easing = this@Animator.easing) = __tween(this::x[x], this::y[y], time = time, easing = easing)
    fun View.moveToWithSpeed(x: Double, y: Double, speed: Number = this@Animator.speed, easing: Easing = this@Animator.easing) =
        __tween(this::x[x], this::y[y], lazyTime = { (hypot(this.x - x, this.y - y) / speed.toDouble()).seconds }, easing = easing)

    fun View.show(time: TimeSpan = this@Animator.time, easing: Easing = this@Animator.easing) = alpha(1, time, easing)
    fun View.hide(time: TimeSpan = this@Animator.time, easing: Easing = this@Animator.easing) = alpha(0, time, easing)

    inline fun View.moveTo(x: Number, y: Number, time: TimeSpan = this@Animator.time, easing: Easing = this@Animator.easing) = moveTo(x.toDouble(), y.toDouble(), time, easing)
    inline fun View.moveToWithSpeed(x: Number, y: Number, speed: Number = this@Animator.speed, easing: Easing = this@Animator.easing) = moveToWithSpeed(x.toDouble(), y.toDouble(), speed, easing)
    inline fun View.alpha(alpha: Number, time: TimeSpan = this@Animator.time, easing: Easing = this@Animator.easing) = __tween(this::alpha[alpha.toDouble()], time = time, easing = easing)

    fun wait(time: TimeSpan = this.time) = __tween(time = time)
    fun wait(time: (() -> TimeSpan)? = null) = __tween(lazyTime = time)

    fun block(callback: () -> Unit) {
        nodes.add(object : BaseAnimatorNode {
            override suspend fun execute() = callback()
            override fun executeImmediately() = callback()
        })
    }
}

fun View.animator(
    time: TimeSpan = Animator.DEFAULT_TIME,
    speed: Double = Animator.DEFAULT_SPEED,
    easing: Easing = Animator.DEFAULT_EASING,
    completeOnCancel: Boolean = Animator.DEFAULT_COMPLETE_ON_CANCEL,
    kind: Animator.NodeKind = Animator.NodeKind.Sequence,
    block: Animator.() -> Unit = {}
): Animator = Animator(this, time, speed, easing, completeOnCancel, kind).apply(block)

suspend fun View.animate(
    time: TimeSpan = Animator.DEFAULT_TIME,
    speed: Double = Animator.DEFAULT_SPEED,
    easing: Easing = Animator.DEFAULT_EASING,
    completeOnCancel: Boolean = Animator.DEFAULT_COMPLETE_ON_CANCEL,
    kind: Animator.NodeKind = Animator.NodeKind.Sequence,
    block: Animator.() -> Unit = {}
): Animator = Animator(this, time, speed, easing, completeOnCancel, kind).apply(block).also { it.execute() }

suspend fun View.animateSequence(
    time: TimeSpan = Animator.DEFAULT_TIME,
    speed: Double = Animator.DEFAULT_SPEED,
    easing: Easing = Animator.DEFAULT_EASING,
    completeOnCancel: Boolean = Animator.DEFAULT_COMPLETE_ON_CANCEL,
    block: Animator.() -> Unit = {}
): Animator = animate(time, speed, easing, completeOnCancel, Animator.NodeKind.Sequence, block)

suspend fun View.animateParallel(
    time: TimeSpan = Animator.DEFAULT_TIME,
    speed: Double = Animator.DEFAULT_SPEED,
    easing: Easing = Animator.DEFAULT_EASING,
    completeOnCancel: Boolean = Animator.DEFAULT_COMPLETE_ON_CANCEL,
    block: Animator.() -> Unit = {}
): Animator = animate(time, speed, easing, completeOnCancel, Animator.NodeKind.Parallel, block)
