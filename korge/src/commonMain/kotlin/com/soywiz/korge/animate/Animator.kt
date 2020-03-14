package com.soywiz.korge.animate

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
}

enum class AnimatorNodeKind {
    Parallel, Sequence
}

open class AnimatorNode(
    val root: View,
    val defaultTime: TimeSpan = 0.5.seconds,
    val defaultSpeed: Double = 10.0,
    val defaultEasing: Easing = Easing.EASE_IN_OUT_QUAD,
    val kind: AnimatorNodeKind = AnimatorNodeKind.Sequence
) : BaseAnimatorNode {
    @PublishedApi
    internal val nodes = arrayListOf<BaseAnimatorNode>()

    override suspend fun execute() = when (kind) {
        AnimatorNodeKind.Sequence -> for (node in nodes) node.execute()
        AnimatorNodeKind.Parallel -> nodes.map { launchImmediately(coroutineContext) { it.execute() } }.joinAll()
    }

    inline fun parallel(
        defaultTime: TimeSpan = this.defaultTime,
        defaultSpeed: Double = this.defaultSpeed,
        defaultEasing: Easing = this.defaultEasing,
        callback: AnimatorNode.() -> Unit
    ) {
        nodes.add(AnimatorNode(root, defaultTime, defaultSpeed, defaultEasing, AnimatorNodeKind.Parallel).apply(callback))
    }

    inline fun sequence(
        defaultTime: TimeSpan = this.defaultTime,
        defaultSpeed: Double = this.defaultSpeed,
        defaultEasing: Easing = this.defaultEasing,
        callback: AnimatorNode.() -> Unit
    ) {
        nodes.add(AnimatorNode(root, defaultTime, defaultSpeed, defaultEasing, AnimatorNodeKind.Sequence).apply(callback))
    }

    class TweenNode(val view: View, val time: TimeSpan, val easing: Easing, vararg val vfs: V2<*>) : BaseAnimatorNode {
        override suspend fun execute() = view.tween(*vfs, time = time, easing = easing)
    }

    @PublishedApi
    internal fun __tween(vararg vfs: V2<*>, time: TimeSpan = defaultTime, easing: Easing = defaultEasing) {
        nodes.add(TweenNode(root, time, easing, *vfs))
    }

    fun tween(vararg vfs: V2<*>, time: TimeSpan = defaultTime, easing: Easing = defaultEasing) = __tween(*vfs, time = time, easing = easing)

    fun View.moveTo(x: Double, y: Double, time: TimeSpan = defaultTime, easing: Easing = defaultEasing) =
        __tween(this::x[x], this::y[y], time = time, easing = easing)

    fun View.moveToWithSpeed(x: Double, y: Double, speed: Number = defaultSpeed, easing: Easing = defaultEasing) =
        moveTo(x, y, (hypot(this.x - x, this.y - y) / speed.toDouble()).seconds, easing)

    fun View.show(time: TimeSpan = defaultTime, easing: Easing = defaultEasing) = alpha(1, time, easing)
    fun View.hide(time: TimeSpan = defaultTime, easing: Easing = defaultEasing) = alpha(0, time, easing)

    inline fun View.moveTo(x: Number, y: Number, time: TimeSpan = defaultTime, easing: Easing = defaultEasing) = moveTo(x.toDouble(), y.toDouble(), time, easing)
    inline fun View.moveToWithSpeed(x: Number, y: Number, speed: Number = defaultSpeed, easing: Easing = defaultEasing) = moveToWithSpeed(x.toDouble(), y.toDouble(), speed, easing)
    inline fun View.alpha(alpha: Number, time: TimeSpan = defaultTime, easing: Easing = defaultEasing) = __tween(this::alpha[alpha.toDouble()], time = time, easing = easing)

    inline fun wait(time: TimeSpan = defaultTime) = __tween(time = time)
}

class Animator(view: View) : AnimatorNode(view) {
}

suspend fun View.animate(block: Animator.() -> Unit = {}): Animator = Animator(this).apply(block).also { it.execute() }
