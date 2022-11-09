package com.soywiz.korge.animate

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korge.component.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.interpolation.*
import kotlinx.coroutines.*
import kotlin.math.*

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class AnimatorDslMarker

fun View.animator(
    defaultTime: TimeSpan = Animator.DEFAULT_TIME,
    defaultSpeed: Double = Animator.DEFAULT_SPEED,
    defaultEasing: Easing = Animator.DEFAULT_EASING,
    parallel: Boolean = false,
    looped: Boolean = false,
    block: @AnimatorDslMarker Animator.() -> Unit = {}
): Animator = Animator(this, defaultTime, defaultSpeed, defaultEasing, parallel, looped, parent = null).apply(block)

suspend fun View.animate(
    defaultTime: TimeSpan = Animator.DEFAULT_TIME,
    defaultSpeed: Double = Animator.DEFAULT_SPEED,
    defaultEasing: Easing = Animator.DEFAULT_EASING,
    parallel: Boolean = false,
    looped: Boolean = false,
    completeOnCancel: Boolean = false,
    block: @AnimatorDslMarker Animator.() -> Unit = {}
): Animator = Animator(this, defaultTime, defaultSpeed, defaultEasing, parallel, looped, parent = null).apply(block).also { it.await(completeOnCancel = completeOnCancel) }

open class Animator(
    @PublishedApi internal val root: View,
    @PublishedApi internal val defaultTime: TimeSpan = DEFAULT_TIME,
    @PublishedApi internal val defaultSpeed: Double = DEFAULT_SPEED,
    @PublishedApi internal val defaultEasing: Easing = DEFAULT_EASING,
    private val parallel: Boolean = false,
    private val looped: Boolean = false,
    private val parent: Animator?,
    private var lazyInit: (Animator.() -> Unit)? = null,
    @PublishedApi internal val level: Int = 0,
) : CloseableCancellable {
    //private val indent: String get() = Indenter.INDENTS[level]

    companion object {
        val DEFAULT_TIME = 500.milliseconds
        val DEFAULT_SPEED = 128.0 // Points per second
        val DEFAULT_EASING = Easing.EASE
    }

    val onComplete = Signal<Unit>()

    private val nodes = Deque<NewAnimatorNode>()
    var speed: Double = 1.0

    @PublishedApi
    internal fun addNode(node: NewAnimatorNode) {
        nodes.add(node)
        ensure()
    }

    private var updater: UpdateComponent? = null
    var autoInvalidateView = false

    private fun ensure() {
        if (parent != null) return parent.ensure()
        if (!(root.getComponentsOfType(UpdateComponent) ?: emptyList()).contains(updater)) {
        //if (updater == null) {
            //println("!!!!!!!!!!!!! ADD NEW UPDATER : updater=$updater, this=$this, parent=$parent")
            updater = root.addUpdater2(first = false) {
                if (autoInvalidateView) root.invalidateRender()
                //println("****")
                if (rootAnimationNode.update(it) >= 0.seconds) {
                    if (looped) {
                        onComplete()
                    } else {
                        cancel()
                    }
                }
            }
        }
    }

    private fun <T : View> T.addUpdater2(first: Boolean = true, updatable: T.(dt: TimeSpan) -> Unit): UpdateComponent {
        val component = object : UpdateComponent {
            override val view: View get() = this@addUpdater2
            override fun update(dt: TimeSpan) {
                updatable(this@addUpdater2, dt)
            }
        }.attach()
        if (first) component.update(TimeSpan.ZERO)
        return component
    }

    private fun ensureInit() {
        val linit = lazyInit ?: return
        lazyInit = null
        linit(this)
    }

    private var parallelStarted = false

    @PublishedApi internal val rootAnimationNode = RootAnimationNode()

    /** Suspends until this animation has been completed */
    suspend fun awaitComplete(completeOnCancel: Boolean = false) {
        try {
            if (updater == null) return

            if (rootAnimationNode.isEmpty()) return
            onComplete.waitOne()
        } catch (e: CancellationException) {
            when {
                (e as? AnimateCancellationException?)?.completeOnCancel ?: completeOnCancel -> complete()
                else -> cancel()
            }
        }
    }

    /** Suspends until this animation has been completed */
    suspend fun await(completeOnCancel: Boolean = false) {
        awaitComplete(completeOnCancel)
    }

    override fun close() {
        cancel()
    }

    override fun cancel(e: Throwable) {
        cancel()
    }

    /**
     * Cancels and clears all the pending animations keeping the properties as they are when executing this function.
     */
    fun cancel() {
        //println("---- CANCEL: looped=$looped, currentTime=$currentTime, totalTime=$totalTime")
        rootAnimationNode.reset()
        nodes.clear()
        updater?.close()
        updater = null
        parallelStarted = false
        onComplete()
    }

    /**
     * Finishes all the pending animations and sets all the properties to their final state.
     */
    fun complete() {
        rootAnimationNode.complete()
    }

    inline fun parallel(
        time: TimeSpan = this.defaultTime, speed: Double = this.defaultSpeed, easing: Easing = this.defaultEasing, looped: Boolean = false,
        callback: @AnimatorDslMarker Animator.() -> Unit
    ): Animator = Animator(root, time, speed, easing, true, looped, level = level + 1, parent = this).also { callback(it) }.also { addNode(it.rootAnimationNode) }

    inline fun sequence(
        defaultTime: TimeSpan = this.defaultTime, defaultSpeed: Double = this.defaultSpeed, easing: Easing = this.defaultEasing, looped: Boolean = false,
        callback: @AnimatorDslMarker Animator.() -> Unit
    ): Animator = Animator(root, defaultTime, defaultSpeed, easing, false, looped, level = level + 1, parent = this).also { callback(it) }.also { addNode(it.rootAnimationNode) }

    fun parallelLazy(
        time: TimeSpan = this.defaultTime, speed: Double = this.defaultSpeed, easing: Easing = this.defaultEasing, looped: Boolean = false,
        init: @AnimatorDslMarker Animator.() -> Unit
    ): Animator = Animator(root, time, speed, easing, true, looped, lazyInit = init, level = level + 1, parent = this).also { addNode(it.rootAnimationNode) }

    fun sequenceLazy(
        time: TimeSpan = this.defaultTime, speed: Double = this.defaultSpeed, easing: Easing = this.defaultEasing, looped: Boolean = false,
        init: @AnimatorDslMarker Animator.() -> Unit
    ): Animator = Animator(root, time, speed, easing, false, looped, lazyInit = init, level = level + 1, parent = this).also { addNode(it.rootAnimationNode) }

    @PublishedApi internal fun __tween(vararg vs: V2<*>, lazyVs: Array<out () -> V2<*>>? = null, time: TimeSpan = this.defaultTime, lazyTime: (() -> TimeSpan)? = null, easing: Easing = this.defaultEasing, name: String?) {
        //println("__tween=time=$time")
        addNode(TweenNode(*vs, lazyVs = lazyVs, time = time, lazyTime = lazyTime, easing = easing, name = name))
    }

    @PublishedApi internal fun __tween(vararg vs: () -> V2<*>, time: TimeSpan = this.defaultTime, lazyTime: (() -> TimeSpan)? = null, easing: Easing = this.defaultEasing, name: String?) {
        addNode(TweenNode(lazyVs = vs, time = time, lazyTime = lazyTime, easing = easing, name = name))
    }

    inner class RootAnimationNode : NewAnimatorNode {
        /**
         * Note that getting this field, won't return time for lazy nodes.
         */
        //override val totalTime: TimeSpan get() = when {
        //    parallel -> if (nodes.isNotEmpty()) nodes.maxOf { it.totalTime } else 0.seconds
        //    else -> {
        //        var sum = 0.seconds
        //        currentNode?.let { sum += it.totalTime - currentTime }
        //        for (node in nodes) sum += node.totalTime
        //        sum
        //    }
        //}

        private var currentNode: NewAnimatorNode? = null
        override fun reset() {
            currentNode = null
        }

        override fun update(dt: TimeSpan): TimeSpan {
            var dt = dt * speed

            //println("${indent}|UPDATE!!")
            ensureInit()

            if (parallel) {
                var completedTime = 0.seconds
                nodes.forEachIndexed { index, it ->
                    val result = it.update(dt)
                    completedTime = if (index == 0) result else min(completedTime, result)
                    //println(" - $result")
                }
                parallelStarted = true
                return completedTime
            }

            while (true) {
                if (currentNode == null) {
                    currentNode = if (nodes.isNotEmpty()) nodes.removeFirst() else null
                    currentNode?.reset()
                    //println("${indent}|LOADNODE!! ${currentNode}")
                    if (looped) {
                        currentNode?.let { nodes.addLast(it) }
                    }
                }
                if (currentNode != null) {
                    val node = currentNode!!
                    //println("${indent}UPDATE[dt=$dt]: $node")
                    val extraTime = node.update(dt)
                    if (extraTime >= TimeSpan.ZERO) {
                        //println("${indent}|COMPLETENODE!! ${currentNode}")
                        currentNode = null
                        dt = extraTime
                    }
                    // Continue running other nodes
                    if (extraTime > TimeSpan.ZERO) {
                        continue
                    }
                }
                //println("${indent}->completed")
                return if (nodes.isNotEmpty() || currentNode != null) (-1).seconds else TimeSpan.ZERO
            }
        }

        override fun complete() {
            ensureInit()
            currentNode?.also {
                currentNode = null
                it.complete()
            }
            while (nodes.isNotEmpty()) nodes.removeFirst().complete()
            cancel()
        }

        override fun toString(): String = "RootAnimationNode(${this@Animator})"
        fun isEmpty(): Boolean {
            return currentNode == null && nodes.isEmpty()
        }
    }

    class TweenNode(
        vararg val vs: V2<*>,
        val lazyVs: Array<out () -> V2<*>>? = null,
        val time: TimeSpan = 1000.milliseconds,
        val lazyTime: (() -> TimeSpan)? = null,
        val easing: Easing,
        val name: String? = null
    ) : NewAnimatorNode {
        val computedVs by lazy { if (lazyVs != null) Array(lazyVs.size) { lazyVs[it]() } else vs }
        private val totalTime: TimeSpan by lazy { lazyTime?.invoke() ?: time }

        override fun toString(): String = "TweenNode(totalTime=$totalTime, name=$name, ${computedVs.toList()})"

        private var currentTime: TimeSpan = 0.seconds
        override fun reset() {
            currentTime = 0.seconds
        }

        override fun update(dt: TimeSpan): TimeSpan {
            if (currentTime == 0.seconds) {
                computedVs.fastForEach {
                    it.init()
                }
            }
            currentTime += dt
            computedVs.fastForEach {
                val ratio = when {
                    totalTime == TimeSpan.ZERO -> 1.0
                    else -> currentTime.seconds.convertRange(it.startTime.seconds, it.endTime(totalTime).seconds, 0.0, 1.0)
                }

                if (ratio >= 0.0) {
                    it.set(easing.invoke(ratio.clamp01()))
                }
            }
            return currentTime - totalTime
        }

        override fun complete() {
            computedVs.fastForEach { it.set(1.0) }
        }
    }

    class BlockNode(val name: String? = null, val callback: () -> Unit) : NewAnimatorNode {
        override fun toString(): String = "BlockNode(name=$name)"

        var executed = false

        override fun reset() = Unit
        override fun update(dt: TimeSpan): TimeSpan {
            complete()
            return dt
        }
        override fun complete() {
            if (executed) return
            executed = true
            callback()
        }
    }
}
interface NewAnimatorNode {
    //fun reset()
    //val totalTime: TimeSpan
    fun reset()
    /** Sets this node to the specified [dt] (Delta Time), returns the exceeded time of completion, 0 if completed, less than zero if not completed. */
    fun update(dt: TimeSpan): TimeSpan
    fun complete()
}

////////////////////////

fun Animator.tween(vararg vs: V2<*>, time: TimeSpan = this.defaultTime, easing: Easing = this.defaultEasing, name: String? = null): Unit = __tween(*vs, time = time, easing = easing, name = name)
fun Animator.tweenLazyTime(vararg vs: V2<*>, time: () -> TimeSpan = { this.defaultTime }, easing: Easing = this.defaultEasing, name: String? = null) = __tween(*vs, lazyTime = time, easing = easing, name = name)

fun Animator.tweenLazy(vararg vs: () -> V2<*>, time: TimeSpan = this.defaultTime, easing: Easing = this.defaultEasing, name: String? = null) = __tween(*vs, time = time, easing = easing, name = name)
fun Animator.tweenLazyLazyTime(vararg vs: () -> V2<*>, time: () -> TimeSpan = { this.defaultTime }, easing: Easing = this.defaultEasing, name: String? = null) = __tween(*vs, lazyTime = time, easing = easing, name = name)

fun Animator.wait(time: TimeSpan = this.defaultTime) = __tween(time = time, name = "wait")
fun Animator.waitLazy(time: () -> TimeSpan) = __tween(lazyTime = time, name = "wait")

fun Animator.block(name: String? = null, callback: () -> Unit) {
    addNode(Animator.BlockNode(name, callback))
}

////////////////////////

fun Animator.scaleBy(view: View, scaleX: Double, scaleY: Double = scaleX, time: TimeSpan = this.defaultTime, easing: Easing = this.defaultEasing) = __tween(view::scaleX.incr(scaleX), view::scaleY.incr(scaleY), time = time, easing = easing, name = "scaleBy")
fun Animator.rotateBy(view: View, rotation: Angle, time: TimeSpan = this.defaultTime, easing: Easing = this.defaultEasing) = __tween(view::rotation.incr(rotation), time = time, easing = easing, name = "rotateBy")
fun Animator.moveBy(view: View, x: Double = 0.0, y: Double = 0.0, time: TimeSpan = this.defaultTime, easing: Easing = this.defaultEasing) = __tween(view::x.incr(x), view::y.incr(y), time = time, easing = easing, name = "moveBy")
fun Animator.moveByWithSpeed(view: View, x: Double = 0.0, y: Double = 0.0, speed: Double = this.defaultSpeed, easing: Easing = this.defaultEasing) = __tween(view::x.incr(x), view::y.incr(y), lazyTime = { (hypot(x, y) / speed.toDouble()).seconds }, easing = easing, name = "moveByWithSpeed")

fun Animator.moveBy(view: View, x: Number = 0.0, y: Number = 0.0, time: TimeSpan = this.defaultTime, easing: Easing = this.defaultEasing) = moveBy(view, x.toDouble(), y.toDouble(), time, easing)
fun Animator.moveByWithSpeed(view: View, x: Number = 0.0, y: Number = 0.0, speed: Double = this.defaultSpeed, easing: Easing = this.defaultEasing) = moveByWithSpeed(view, x.toDouble(), y.toDouble(), speed, easing)

fun Animator.scaleTo(view: View, scaleX: () -> Number, scaleY: () -> Number = scaleX, time: TimeSpan = this.defaultTime, lazyTime: (() -> TimeSpan)? = null, easing: Easing = this.defaultEasing) = __tween({ view::scaleX[scaleX()] }, { view::scaleY[scaleY()] }, time = time, lazyTime = lazyTime, easing = easing, name = "scaleTo")
fun Animator.scaleTo(view: View, scaleX: Double, scaleY: Double = scaleX, time: TimeSpan = this.defaultTime, easing: Easing = this.defaultEasing) = __tween(view::scaleX[scaleX], view::scaleY[scaleY], time = time, easing = easing, name = "scaleTo")
fun Animator.scaleTo(view: View, scaleX: Float, scaleY: Float = scaleX, time: TimeSpan = this.defaultTime, easing: Easing = this.defaultEasing) = scaleTo(view, scaleX.toDouble(), scaleY.toDouble(), time, easing)
fun Animator.scaleTo(view: View, scaleX: Int, scaleY: Int = scaleX, time: TimeSpan = this.defaultTime, easing: Easing = this.defaultEasing) = scaleTo(view, scaleX.toDouble(), scaleY.toDouble(), time, easing)

fun Animator.moveTo(view: View, x: () -> Number = { view.x }, y: () -> Number = { view.y }, time: TimeSpan = this.defaultTime, lazyTime: (() -> TimeSpan)? = null, easing: Easing = this.defaultEasing) = __tween({ view::x[x()] }, { view::y[y()] }, time = time, lazyTime = lazyTime, easing = easing, name = "moveTo")
fun Animator.moveTo(view: View, x: Double, y: Double, time: TimeSpan = this.defaultTime, easing: Easing = this.defaultEasing) = __tween(view::x[x], view::y[y], time = time, easing = easing, name = "moveTo")
fun Animator.moveTo(view: View, x: Float, y: Float, time: TimeSpan = this.defaultTime, easing: Easing = this.defaultEasing) = moveTo(view, x.toDouble(), y.toDouble(), time, easing)
fun Animator.moveTo(view: View, x: Int, y: Int, time: TimeSpan = this.defaultTime, easing: Easing = this.defaultEasing) = moveTo(view, x.toDouble(), y.toDouble(), time, easing)

fun Animator.moveToWithSpeed(view: View, x: () -> Number = { view.x }, y: () -> Number = { view.y }, speed: () -> Number = { this.defaultSpeed }, easing: Easing = this.defaultEasing) = __tween({ view::x[x()] }, { view::y[y()] }, lazyTime = { (hypot(view.x - x().toDouble(), view.y - y().toDouble()) / speed().toDouble()).seconds }, easing = easing, name = "moveToWithSpeed")
fun Animator.moveToWithSpeed(view: View, x: Double, y: Double, speed: Double = this.defaultSpeed, easing: Easing = this.defaultEasing) = __tween(view::x[x], view::y[y], lazyTime = { (hypot(view.x - x, view.y - y) / speed.toDouble()).seconds }, easing = easing, name = "moveToWithSpeed")
fun Animator.moveToWithSpeed(view: View, x: Float, y: Float, speed: Number = this.defaultSpeed, easing: Easing = this.defaultEasing) = moveToWithSpeed(view, x.toDouble(), y.toDouble(), speed.toDouble(), easing)
fun Animator.moveToWithSpeed(view: View, x: Int, y: Int, speed: Number = this.defaultSpeed, easing: Easing = this.defaultEasing) = moveToWithSpeed(view, x.toDouble(), y.toDouble(), speed.toDouble(), easing)

fun Animator.moveInPath(view: View, path: VectorPath, includeLastPoint: Boolean = true, time: TimeSpan = this.defaultTime, lazyTime: (() -> TimeSpan)? = null, easing: Easing = this.defaultEasing) = __tween({ view::pos.get(path, includeLastPoint = includeLastPoint) }, time = time, lazyTime = lazyTime, easing = easing, name = "moveInPath")
fun Animator.moveInPath(view: View, points: IPointArrayList, time: TimeSpan = this.defaultTime, lazyTime: (() -> TimeSpan)? = null, easing: Easing = this.defaultEasing) = __tween({ view::pos[points] }, time = time, lazyTime = lazyTime, easing = easing, name = "moveInPath")

fun Animator.moveInPathWithSpeed(view: View, path: VectorPath, includeLastPoint: Boolean = true, speed: () -> Number = { this.defaultSpeed }, easing: Easing = this.defaultEasing) = __tween({ view::pos.get(path, includeLastPoint = includeLastPoint) }, lazyTime = { (path.length / speed().toDouble()).seconds }, easing = easing, name = "moveInPathWithSpeed")
fun Animator.moveInPathWithSpeed(view: View, points: IPointArrayList, speed: () -> Number = { this.defaultSpeed }, easing: Easing = this.defaultEasing) = __tween({ view::pos[points] }, lazyTime = { (points.length / speed().toDouble()).seconds }, easing = easing, name = "moveInPathWithSpeed")

fun Animator.alpha(view: View, alpha: Double, time: TimeSpan = this.defaultTime, easing: Easing = this.defaultEasing) = __tween(view::alpha[alpha], time = time, easing = easing, name = "alpha")
fun Animator.alpha(view: View, alpha: Float, time: TimeSpan = this.defaultTime, easing: Easing = this.defaultEasing) = alpha(view, alpha.toDouble(), time, easing)
fun Animator.alpha(view: View, alpha: Int, time: TimeSpan = this.defaultTime, easing: Easing = this.defaultEasing) = alpha(view, alpha.toDouble(), time, easing)

fun Animator.rotateTo(view: View, angle: Angle, time: TimeSpan = this.defaultTime, easing: Easing = this.defaultEasing) = __tween(view::rotation[angle], time = time, easing = easing, name = "rotateTo")
fun Animator.rotateTo(view: View, rotation: () -> Angle, time: TimeSpan = this.defaultTime, lazyTime: (() -> TimeSpan)? = null, easing: Easing = this.defaultEasing) = __tween({ view::rotation[rotation()] }, time = time, lazyTime = lazyTime, easing = easing, name = "rotateTo")

fun Animator.show(view: View, time: TimeSpan = this.defaultTime, easing: Easing = this.defaultEasing) = alpha(view, 1.0, time, easing)
fun Animator.hide(view: View, time: TimeSpan = this.defaultTime, easing: Easing = this.defaultEasing) = alpha(view, 0.0, time, easing)

private val VectorPath.length: Double get() = getCurves().length
private val IPointArrayList.length: Double get() {
    var sum = 0.0
    for (n in 1 until size) sum += Point.distance(getX(n - 1), getY(n - 1), getX(n), getY(n))
    return sum
}

class AnimateCancellationException(val completeOnCancel: Boolean? = null) : CancellationException(null)

