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
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.interpolation.*
import kotlin.math.*

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class NewAnimatorDslMarker

fun View.newAnimator(
    time: TimeSpan = NewAnimator.DEFAULT_TIME,
    speed: Double = NewAnimator.DEFAULT_SPEED,
    easing: Easing = NewAnimator.DEFAULT_EASING,
    parallel: Boolean = false,
    looped: Boolean = false,
    block: @NewAnimatorDslMarker NewAnimator.() -> Unit = {}
): NewAnimator = NewAnimator(this, time, speed, easing, parallel, looped, parent = null).apply(block)

open class NewAnimator(
    @PublishedApi internal val root: View,
    @PublishedApi internal val time: TimeSpan = DEFAULT_TIME,
    @PublishedApi internal val speed: Double = DEFAULT_SPEED,
    @PublishedApi internal val easing: Easing = DEFAULT_EASING,
    private val parallel: Boolean = false,
    private val looped: Boolean = false,
    private val parent: NewAnimator? = null,
    private var lazyInit: (NewAnimator.() -> Unit)? = null
) : CloseableCancellable {
    companion object {
        val DEFAULT_TIME = 500.milliseconds
        val DEFAULT_SPEED = 128.0 // Points per second
        val DEFAULT_EASING = Easing.EASE
    }

    val onComplete = Signal<Unit>()

    private val nodes = Deque<NewAnimatorNode>()

    @PublishedApi
    internal fun addNode(node: NewAnimatorNode) {
        nodes.add(node)
        ensure()
    }

    private var currentTime: TimeSpan = 0.seconds
    private var updater: UpdateComponent? = null

    private fun ensure() {
        if (parent != null) return parent.ensure()
        if (!(root.getComponentsOfType(UpdateComponent) ?: emptyList()).contains(updater)) {
            updater = root.addUpdater2 {
                currentTime += it
                rootAnimationNode.update(currentTime)
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

    private var currentNode: NewAnimatorNode? = null

    private fun ensureInit() {
        val init = lazyInit ?: return
        lazyInit = null
        init(this)
    }

    private var parallelStarted = false

    @PublishedApi internal val rootAnimationNode = RootAnimationNode()

    /** Suspends until this animation has been completed */
    suspend fun awaitComplete() {
        if (updater == null) return
        if (currentNode == null && nodes.isEmpty()) return
        onComplete.waitOne()
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
        currentTime = 0.seconds
        currentNode = null
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
        time: TimeSpan = this.time, speed: Double = this.speed, easing: Easing = this.easing, looped: Boolean = false,
        callback: @NewAnimatorDslMarker NewAnimator.() -> Unit
    ): NewAnimator = NewAnimator(root, time, speed, easing, true, looped).also(callback).also { addNode(it.rootAnimationNode) }

    inline fun sequence(
        time: TimeSpan = this.time, speed: Double = this.speed, easing: Easing = this.easing, looped: Boolean = false,
        callback: @NewAnimatorDslMarker NewAnimator.() -> Unit
    ): NewAnimator = NewAnimator(root, time, speed, easing, false, looped).also(callback).also { addNode(it.rootAnimationNode) }

    fun parallelLazy(
        time: TimeSpan = this.time, speed: Double = this.speed, easing: Easing = this.easing, looped: Boolean = false,
        init: @NewAnimatorDslMarker NewAnimator.() -> Unit
    ): NewAnimator = NewAnimator(root, time, speed, easing, true, looped, lazyInit = init).also { addNode(it.rootAnimationNode) }

    fun sequenceLazy(
        time: TimeSpan = this.time, speed: Double = this.speed, easing: Easing = this.easing, looped: Boolean = false,
        init: @NewAnimatorDslMarker NewAnimator.() -> Unit
    ): NewAnimator = NewAnimator(root, time, speed, easing, false, looped, lazyInit = init).also { addNode(it.rootAnimationNode) }

    private fun __tween(vararg vs: V2<*>, lazyVs: Array<out () -> V2<*>>? = null, time: TimeSpan = this.time, lazyTime: (() -> TimeSpan)? = null, easing: Easing = this.easing, name: String? = null) {
        //println("__tween=time=$time")
        addNode(TweenNode(*vs, lazyVs = lazyVs, time = time, lazyTime = lazyTime, easing = easing, name = name))
    }

    private fun __tween(vararg vs: () -> V2<*>, time: TimeSpan = this.time, lazyTime: (() -> TimeSpan)? = null, easing: Easing = this.easing, name: String? = null) {
        addNode(TweenNode(lazyVs = vs, time = time, lazyTime = lazyTime, easing = easing, name = name))
    }

    fun tween(vararg vs: V2<*>, time: TimeSpan = this.time, easing: Easing = this.easing, name: String? = null): Unit = __tween(*vs, time = time, easing = easing, name = name)
    fun tweenLazyTime(vararg vs: V2<*>, time: () -> TimeSpan = { this.time }, easing: Easing = this.easing, name: String? = null) = __tween(*vs, lazyTime = time, easing = easing, name = name)

    fun tweenLazy(vararg vs: () -> V2<*>, time: TimeSpan = this.time, easing: Easing = this.easing, name: String? = null) = __tween(*vs, time = time, easing = easing, name = name)
    fun tweenLazyLazyTime(vararg vs: () -> V2<*>, time: () -> TimeSpan = { this.time }, easing: Easing = this.easing, name: String? = null) = __tween(*vs, lazyTime = time, easing = easing, name = name)

    fun wait(time: TimeSpan = this.time) = __tween(time = time, name = "wait")
    fun waitLazy(time: () -> TimeSpan) = __tween(lazyTime = time, name = "wait")

    fun block(name: String? = null, callback: () -> Unit) {
        addNode(BlockNode(name, callback))
    }

    ////////////////////////

    fun scaleBy(view: View, scaleX: Double, scaleY: Double = scaleX, time: TimeSpan = this.time, easing: Easing = this.easing) = __tween({ view::scaleX[view.scaleX + scaleX] }, { view::scaleY[view.scaleY + scaleY] }, time = time, easing = easing)
    fun rotateBy(view: View, rotation: Angle, time: TimeSpan = this.time, easing: Easing = this.easing) = __tween({ view::rotation[view.rotation + rotation] }, time = time, easing = easing)
    fun moveBy(view: View, x: Double = 0.0, y: Double = 0.0, time: TimeSpan = this.time, easing: Easing = this.easing) = __tween({ view::x[view.x + x] }, { view::y[view.y + y] }, time = time, easing = easing)
    fun moveByWithSpeed(view: View, x: Double = 0.0, y: Double = 0.0, speed: Double = this.speed, easing: Easing = this.easing) = __tween({ view::x[view.x + x] }, { view::y[view.y + y] }, lazyTime = { (hypot(x, y) / speed.toDouble()).seconds }, easing = easing)

    fun moveBy(view: View, x: Number = 0.0, y: Number = 0.0, time: TimeSpan = this.time, easing: Easing = this.easing) = moveBy(view, x.toDouble(), y.toDouble(), time, easing)
    fun moveByWithSpeed(view: View, x: Number = 0.0, y: Number = 0.0, speed: Double = this.speed, easing: Easing = this.easing) = moveByWithSpeed(view, x.toDouble(), y.toDouble(), speed, easing)

    fun scaleTo(view: View, scaleX: () -> Number, scaleY: () -> Number = scaleX, time: TimeSpan = this.time, lazyTime: (() -> TimeSpan)? = null, easing: Easing = this.easing) = __tween({ view::scaleX[scaleX()] }, { view::scaleY[scaleY()] }, time = time, lazyTime = lazyTime, easing = easing)
    fun scaleTo(view: View, scaleX: Double, scaleY: Double = scaleX, time: TimeSpan = this.time, easing: Easing = this.easing) = __tween(view::scaleX[scaleX], view::scaleY[scaleY], time = time, easing = easing)
    fun scaleTo(view: View, scaleX: Float, scaleY: Float = scaleX, time: TimeSpan = this.time, easing: Easing = this.easing) = scaleTo(view, scaleX.toDouble(), scaleY.toDouble(), time, easing)
    fun scaleTo(view: View, scaleX: Int, scaleY: Int = scaleX, time: TimeSpan = this.time, easing: Easing = this.easing) = scaleTo(view, scaleX.toDouble(), scaleY.toDouble(), time, easing)

    fun moveTo(view: View, x: () -> Number = { view.x }, y: () -> Number = { view.y }, time: TimeSpan = this.time, lazyTime: (() -> TimeSpan)? = null, easing: Easing = this.easing) = __tween({ view::x[x()] }, { view::y[y()] }, time = time, lazyTime = lazyTime, easing = easing)
    fun moveTo(view: View, x: Double, y: Double, time: TimeSpan = this.time, easing: Easing = this.easing) = __tween(view::x[x], view::y[y], time = time, easing = easing)
    fun moveTo(view: View, x: Float, y: Float, time: TimeSpan = this.time, easing: Easing = this.easing) = moveTo(view, x.toDouble(), y.toDouble(), time, easing)
    fun moveTo(view: View, x: Int, y: Int, time: TimeSpan = this.time, easing: Easing = this.easing) = moveTo(view, x.toDouble(), y.toDouble(), time, easing)

    fun moveToWithSpeed(view: View, x: () -> Number = { view.x }, y: () -> Number = { view.y }, speed: () -> Number = { this.speed }, easing: Easing = this.easing) = __tween({ view::x[x()] }, { view::y[y()] }, lazyTime = { (hypot(view.x - x().toDouble(), view.y - y().toDouble()) / speed().toDouble()).seconds }, easing = easing)
    fun moveToWithSpeed(view: View, x: Double, y: Double, speed: Double = this.speed, easing: Easing = this.easing) = __tween(view::x[x], view::y[y], lazyTime = { (hypot(view.x - x, view.y - y) / speed.toDouble()).seconds }, easing = easing)
    fun moveToWithSpeed(view: View, x: Float, y: Float, speed: Number = this.speed, easing: Easing = this.easing) = moveToWithSpeed(view, x.toDouble(), y.toDouble(), speed.toDouble(), easing)
    fun moveToWithSpeed(view: View, x: Int, y: Int, speed: Number = this.speed, easing: Easing = this.easing) = moveToWithSpeed(view, x.toDouble(), y.toDouble(), speed.toDouble(), easing)

    fun moveInPath(view: View, path: VectorPath, includeLastPoint: Boolean = true, time: TimeSpan = this.time, lazyTime: (() -> TimeSpan)? = null, easing: Easing = this.easing) = __tween({ view::pos.get(path, includeLastPoint = includeLastPoint) }, time = time, lazyTime = lazyTime, easing = easing)
    fun moveInPath(view: View, points: IPointArrayList, time: TimeSpan = this.time, lazyTime: (() -> TimeSpan)? = null, easing: Easing = this.easing) = __tween({ view::pos[points] }, time = time, lazyTime = lazyTime, easing = easing)

    fun moveInPathWithSpeed(view: View, path: VectorPath, includeLastPoint: Boolean = true, speed: () -> Number = { this.speed }, easing: Easing = this.easing) = __tween({ view::pos.get(path, includeLastPoint = includeLastPoint) }, lazyTime = { (path.length / speed().toDouble()).seconds }, easing = easing)
    fun moveInPathWithSpeed(view: View, points: IPointArrayList, speed: () -> Number = { this.speed }, easing: Easing = this.easing) = __tween({ view::pos[points] }, lazyTime = { (points.length / speed().toDouble()).seconds }, easing = easing)

    fun alpha(view: View, alpha: Double, time: TimeSpan = this.time, easing: Easing = this.easing) = __tween(view::alpha[alpha], time = time, easing = easing)
    fun alpha(view: View, alpha: Float, time: TimeSpan = this.time, easing: Easing = this.easing) = alpha(view, alpha.toDouble(), time, easing)
    fun alpha(view: View, alpha: Int, time: TimeSpan = this.time, easing: Easing = this.easing) = alpha(view, alpha.toDouble(), time, easing)

    fun rotateTo(view: View, angle: Angle, time: TimeSpan = this.time, easing: Easing = this.easing) = __tween(view::rotation[angle], time = time, easing = easing)
    fun rotateTo(view: View, rotation: () -> Angle, time: TimeSpan = this.time, lazyTime: (() -> TimeSpan)? = null, easing: Easing = this.easing) = __tween({ view::rotation[rotation()] }, time = time, lazyTime = lazyTime, easing = easing)

    fun show(view: View, time: TimeSpan = this.time, easing: Easing = this.easing) = alpha(view, 1.0, time, easing)
    fun hide(view: View, time: TimeSpan = this.time, easing: Easing = this.easing) = alpha(view, 0.0, time, easing)

    private val VectorPath.length: Double get() = getCurves().length
    private val IPointArrayList.length: Double get() {
        var sum = 0.0
        for (n in 1 until size) sum += Point.distance(getX(n - 1), getY(n - 1), getX(n), getY(n))
        return sum
    }

    ////////////////////////

    inner class RootAnimationNode : NewAnimatorNode {

        /**
         * Note that getting this field, won't return time for lazy nodes.
         */
        override val totalTime: TimeSpan get() = when {
            parallel -> if (nodes.isNotEmpty()) nodes.maxOf { it.totalTime } else 0.seconds
            else -> {
                var sum = 0.seconds
                currentNode?.let { sum += it.totalTime - currentTime }
                for (node in nodes) sum += node.totalTime
                sum
            }
        }

        override fun update(time: TimeSpan) {
            ensureInit()
            var rtime = time
            var completed = false
            when (parallel) {
                false -> {
                    if (currentNode == null) {
                        currentNode = if (nodes.isNotEmpty()) nodes.removeFirst() else null
                        if (looped) currentNode?.let { nodes.addLast(it) }
                        rtime = 0.seconds
                        //println("UPDATE: $time, size=${nodes.size}, time=$time, currentNode=$currentNode")
                    }
                    currentNode?.let { node ->
                        val nodeTotalTime = node.totalTime
                        val nodeTime = (rtime).clamp(0.seconds, nodeTotalTime)
                        node.update(nodeTime)
                        val newNodeTotalTime = node.totalTime // Might be updated if node is lazy!
                        //println("## time=$time, nodeTime=$nodeTime, currentTime=$currentTime, node.totalTime=${node.totalTime}")
                        if (nodeTime >= newNodeTotalTime) {
                            currentTime -= nodeTotalTime
                            //totalTime -= node.totalTime
                            currentNode = null
                            if (nodes.isEmpty()) completed = true
                        }
                    }
                }
                true -> {
                    val maxTotalTime = totalTime
                    val ptime = if (!parallelStarted) 0.seconds else rtime
                    nodes.forEach {
                        val pptime = rtime.clamp(0.seconds, it.totalTime)
                        it.update(pptime)
                    }
                    parallelStarted = true
                    completed = rtime >= maxTotalTime
                }
            }
            //if (rtime >= totalTime && ((currentNode == null && nodes.isEmpty()) || parallel || looped)) {
            if (completed) {
                //println("---- COMPLETED: looped=$looped, rtime=$rtime, totalTime=$totalTime, nodes=${nodes.size}, currentNode=$currentNode")
                if (looped) {
                    currentTime = 0.seconds
                    onComplete()
                } else {
                    cancel()
                }
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

    }

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
                it.set(easing.invoke(if (totalTime == TimeSpan.ZERO) 1.0 else time / this.totalTime))
            }
        }

        override fun complete() {
            computedVs.fastForEach { it.set(1.0) }
        }
    }

    class BlockNode(val name: String? = null, val callback: () -> Unit) : NewAnimatorNode {
        override fun toString(): String = "BaseAnimatorNode.Block(name=$name)"

        var executed = false
        override val totalTime: TimeSpan get() = 0.seconds

        override fun update(time: TimeSpan) = complete()
        override fun complete() {
            if (executed) return
            executed = true
            callback()
        }
    }
}
interface NewAnimatorNode {
    //fun reset()
    val totalTime: TimeSpan
    fun update(time: TimeSpan)
    fun complete()
}
