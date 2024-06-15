package korlibs.korge.animate

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.io.async.*
import korlibs.io.lang.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*
import korlibs.math.interpolation.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.math.*
import kotlin.reflect.*
import kotlin.time.*

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class AnimatorDslMarker

val View.simpleAnimator: Animator by Extra.PropertyThis {
    animator(parallel = true).also { it.autoInvalidateView = true }
}

fun View.animator(
    defaultTime: Duration = Animator.DEFAULT_TIME,
    defaultSpeed: Double = Animator.DEFAULT_SPEED,
    defaultEasing: Easing = Animator.DEFAULT_EASING,
    parallel: Boolean = false,
    looped: Boolean = false,
    startImmediately: Boolean = Animator.DEFAULT_START_IMMEDIATELY,
    block: @AnimatorDslMarker Animator.() -> Unit = {}
): Animator = Animator(this, defaultTime.fast, defaultSpeed, defaultEasing, parallel, looped, parent = null, startImmediately = startImmediately, level = 0).apply(block)

suspend fun View.animate(
    defaultTime: Duration = Animator.DEFAULT_TIME,
    defaultSpeed: Double = Animator.DEFAULT_SPEED,
    defaultEasing: Easing = Animator.DEFAULT_EASING,
    parallel: Boolean = false,
    looped: Boolean = false,
    completeOnCancel: Boolean = false,
    startImmediately: Boolean = Animator.DEFAULT_START_IMMEDIATELY,
    block: @AnimatorDslMarker Animator.() -> Unit = {}
): Animator = Animator(this, defaultTime.fast, defaultSpeed, defaultEasing, parallel, looped, parent = null, startImmediately = startImmediately, level = 0).apply(block).also { it.await(completeOnCancel = completeOnCancel) }

open class Animator @PublishedApi internal constructor(
    @PublishedApi internal val root: View,
    @PublishedApi internal val fastDefaultTime: FastDuration,
    @PublishedApi internal val defaultSpeed: Double,
    @PublishedApi internal val defaultEasing: Easing,
    private val parallel: Boolean = false,
    private val looped: Boolean = false,
    private val parent: Animator?,
    private var lazyInit: (Animator.() -> Unit)? = null,
    @PublishedApi internal val level: Int,
    @PublishedApi internal val startImmediately: Boolean,
) : CloseableCancellable {
    val defaultTime: Duration get() = fastDefaultTime.slow

    //private val indent: String get() = Indenter.INDENTS[level]

    companion object {
        val DEFAULT_TIME = 500.milliseconds
        val DEFAULT_SPEED = 128.0 // Points per second
        val DEFAULT_EASING = Easing.EASE
        val DEFAULT_START_IMMEDIATELY = true
        //val DEFAULT_START_IMMEDIATELY = false
    }

    val onComplete = Signal<Unit>()

    internal val nodes = Deque<NewAnimatorNode>()
    var speed: Double = 1.0
    val rootAnimator: Animator get() = parent?.rootAnimator ?: this

    internal fun removeProps(props: Set<KMutableProperty0<*>>) {
        for (node in nodes) {
            if (node is TweenNode) {
                node.computedVs.retainAll { it.key !in props }
            }
        }
    }

    @PublishedApi
    internal fun addNode(node: NewAnimatorNode) {
        nodes.add(node)
        ensure()
    }

    private var updater: Cancellable? = null
    var autoInvalidateView = false

    private fun ensure() {
        if (parent != null) return parent.ensure()

        //println("updateComponents=${updateComponents.size}, updateComponents.contains(updater)=${updateComponents.contains(updater)}, updater=$updater : $updateComponents")
        if (rootAnimator.updater != null) return
        //if (updater != null) return

        //println("!!!!!!!!!!!!! ADD NEW UPDATER : updater=$updater, this=$this, parent=$parent")

        rootAnimator.updater = this@Animator.root.addFastUpdater(first = rootAnimator.startImmediately) { dt ->
            if (this@Animator.autoInvalidateView) this@Animator.root.invalidateRender()
            //println("****")
            if (this@Animator.rootAnimationNode.update(dt) >= FastDuration.ZERO) {
                if (this@Animator.looped) {
                    this@Animator.onComplete()
                } else {
                    this@Animator.cancel()
                }
            }
        }
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
    fun cancel(): Animator {
        //println("---- CANCEL: looped=$looped, currentTime=$currentTime, totalTime=$totalTime")
        rootAnimationNode.reset()
        nodes.clear()
        rootAnimator.updater?.cancel()
        rootAnimator.updater = null
        parallelStarted = false
        onComplete()
        return this
    }

    val isActive: Boolean get() = rootAnimator.updater != null && !rootAnimationNode.isEmpty()

    /**
     * Finishes all the pending animations and sets all the properties to their final state.
     */
    fun complete(): Animator {
        rootAnimationNode.complete()
        return this
    }

    inline fun parallel(
        time: Duration = this.defaultTime,
        speed: Double = this.defaultSpeed,
        easing: Easing = this.defaultEasing,
        looped: Boolean = false,
        startImmediately: Boolean = this.startImmediately,
        callback: @AnimatorDslMarker Animator.() -> Unit
    ): Animator = Animator(root, time.fast, speed, easing, true, looped, level = level + 1, parent = this, startImmediately = startImmediately).also { callback(it) }.also { addNode(it.rootAnimationNode) }

    inline fun sequence(
        defaultTime: Duration = this.defaultTime,
        defaultSpeed: Double = this.defaultSpeed,
        easing: Easing = this.defaultEasing,
        looped: Boolean = false,
        startImmediately: Boolean = this.startImmediately,
        callback: @AnimatorDslMarker Animator.() -> Unit
    ): Animator = Animator(root, defaultTime.fast, defaultSpeed, easing, false, looped, level = level + 1, parent = this, startImmediately = startImmediately).also { callback(it) }.also { addNode(it.rootAnimationNode) }

    fun parallelLazy(
        time: Duration = this.defaultTime,
        speed: Double = this.defaultSpeed,
        easing: Easing = this.defaultEasing,
        looped: Boolean = false,
        startImmediately: Boolean = this.startImmediately,
        init: @AnimatorDslMarker Animator.() -> Unit
    ): Animator = Animator(root, time.fast, speed, easing, true, looped, lazyInit = init, level = level + 1, parent = this, startImmediately = startImmediately).also { addNode(it.rootAnimationNode) }

    fun sequenceLazy(
        time: Duration = this.defaultTime,
        speed: Double = this.defaultSpeed,
        easing: Easing = this.defaultEasing,
        looped: Boolean = false,
        startImmediately: Boolean = this.startImmediately,
        init: @AnimatorDslMarker Animator.() -> Unit
    ): Animator = Animator(root, time.fast, speed, easing, false, looped, lazyInit = init, level = level + 1, parent = this, startImmediately = startImmediately).also { addNode(it.rootAnimationNode) }

    @PublishedApi internal fun __tween(
        vararg vs: V2<*>,
        lazyVs: Array<out () -> V2<*>>? = null,
        time: FastDuration = this.fastDefaultTime,
        lazyTime: (() -> FastDuration)? = null,
        easing: Easing = this.defaultEasing,
        name: String?,
        replace: Boolean = true
    ) {
        //println("__tween=time=$time")
        if (replace && parallel) {
            removeProps(vs.map { it.key }.toSet())
        }
        addNode(TweenNode(*vs, lazyVs = lazyVs, time = time, lazyTime = lazyTime, easing = easing, name = name))
    }

    @PublishedApi internal fun __tween(
        vararg vs: () -> V2<*>,
        time: FastDuration = this.fastDefaultTime,
        lazyTime: (() -> FastDuration)? = null,
        easing: Easing = this.defaultEasing,
        name: String?
    ) {
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

        private val toRemove = fastArrayListOf<NewAnimatorNode>()

        override fun update(dt: FastDuration): FastDuration {
            var dt = if (speed != 1.0) dt * speed else dt

            //println("UPDATE!!: dt=$dt")
            ensureInit()

            if (parallel) {
                var completedTime = 0.fastSeconds
                toRemove.clear()
                nodes.forEachIndexed { index, it ->
                    val result = it.update(dt)
                    if (result >= 0.fastSeconds) {
                        toRemove.add(it)
                    }
                    completedTime = if (index == 0) result else min(completedTime.fastMilliseconds, result.fastMilliseconds).fastMilliseconds
                    //println(" - $result")
                }
                if (toRemove.isNotEmpty()) nodes.removeAll(toRemove)
                toRemove.clear()
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
                return if (nodes.isNotEmpty() || currentNode != null) (-1).fastSeconds else FastDuration.ZERO
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
        val time: FastDuration = 1000.fastMilliseconds,
        val lazyTime: (() -> FastDuration)? = null,
        val easing: Easing,
        val name: String? = null
    ) : NewAnimatorNode {
        val computedVs by lazy { (lazyVs?.map { it() } ?: vs.toList()).toMutableList() }
        private val totalTime: FastDuration by lazy { lazyTime?.invoke() ?: time }

        override fun toString(): String = "TweenNode(totalTime=$totalTime, name=$name, ${computedVs.toList()})"

        private var currentTime: FastDuration = FastDuration.ZERO
        override fun reset() {
            currentTime = FastDuration.ZERO
        }

        override fun update(dt: FastDuration): FastDuration {
            if (currentTime == FastDuration.ZERO) {
                computedVs.fastForEach {
                    it.init()
                }
            }
            currentTime += dt
            computedVs.fastForEach {
                val ratio = when {
                    totalTime == FastDuration.ZERO -> 1.0
                    else -> currentTime.fastSeconds.convertRange(it.fastStartTime.fastSeconds, it.endTime(totalTime).fastSeconds, 0.0, 1.0)
                }

                //println("dt=$dt, currentTime=$currentTime, totalTime=$totalTime, ratio=$ratio, it.startTime=${it.startTime}, it.endTime(totalTime)=${it.endTime(totalTime)}")

                if (ratio >= 0.0) {
                    it.set(easing.invoke(ratio.clamp01()).toRatio())
                }
            }
            return currentTime - totalTime
        }

        override fun complete() {
            computedVs.fastForEach { it.set(Ratio.ONE) }
        }
    }

    class BlockNode(val name: String? = null, val callback: () -> Unit) : NewAnimatorNode {
        override fun toString(): String = "BlockNode(name=$name)"

        var executed = false

        override fun reset() {
            executed = false
        }

        override fun update(dt: FastDuration): FastDuration {
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
    fun update(dt: FastDuration): FastDuration
    fun complete()
}

////////////////////////

fun Animator.tween(vararg vs: V2<*>, time: Duration = this.defaultTime, easing: Easing = this.defaultEasing, name: String? = null, replace: Boolean = true): Unit = __tween(*vs, time = time.fast, easing = easing, name = name, replace = replace)
fun Animator.tweenLazyTime(vararg vs: V2<*>, time: () -> Duration = { this.defaultTime }, easing: Easing = this.defaultEasing, name: String? = null, replace: Boolean = true) = __tween(*vs, lazyTime = { time().fast }, easing = easing, name = name, replace = replace)

fun Animator.tweenLazy(vararg vs: () -> V2<*>, time: Duration = this.defaultTime, easing: Easing = this.defaultEasing, name: String? = null) = __tween(*vs, time = time.fast, easing = easing, name = name)
fun Animator.tweenLazyLazyTime(vararg vs: () -> V2<*>, time: () -> Duration = { this.defaultTime }, easing: Easing = this.defaultEasing, name: String? = null) = __tween(*vs, lazyTime = { time().fast }, easing = easing, name = name)

fun Animator.wait(time: Duration = this.defaultTime) = __tween(time = time.fast, name = "wait")
fun Animator.waitLazy(time: () -> Duration) = __tween(lazyTime = { time().fast }, name = "wait")

fun Animator.block(name: String? = null, callback: () -> Unit) {
    addNode(Animator.BlockNode(name, callback))
}

fun Animator.removeFromParent(view: View) {
    block { view.removeFromParent() }
}

////////////////////////

fun Animator.scaleBy(view: View, scaleX: Double, scaleY: Double = scaleX, time: Duration = this.defaultTime, easing: Easing = this.defaultEasing) = __tween(view::scaleX.incr(scaleX), view::scaleY.incr(scaleY), time = time.fast, easing = easing, name = "scaleBy")
fun Animator.rotateBy(view: View, rotation: Angle, time: Duration = this.defaultTime, easing: Easing = this.defaultEasing) = __tween(view::rotation.incr(rotation), time = time.fast, easing = easing, name = "rotateBy")
fun Animator.moveBy(view: View, x: Double = 0.0, y: Double = 0.0, time: Duration = this.defaultTime, easing: Easing = this.defaultEasing) = __tween(view::x.incr(x), view::y.incr(y), time = time.fast, easing = easing, name = "moveBy")
fun Animator.moveByWithSpeed(view: View, x: Double = 0.0, y: Double = 0.0, speed: Double = this.defaultSpeed, easing: Easing = this.defaultEasing) = __tween(view::x.incr(x), view::y.incr(y), lazyTime = { (hypot(x, y) / speed.toDouble()).fastSeconds }, easing = easing, name = "moveByWithSpeed")

fun Animator.moveBy(view: View, x: Number = 0.0, y: Number = 0.0, time: Duration = this.defaultTime, easing: Easing = this.defaultEasing) = moveBy(view, x.toDouble(), y.toDouble(), time, easing)
fun Animator.moveByWithSpeed(view: View, x: Number = 0.0, y: Number = 0.0, speed: Double = this.defaultSpeed, easing: Easing = this.defaultEasing) = moveByWithSpeed(view, x.toDouble(), y.toDouble(), speed, easing)

fun Animator.scaleTo(
    view: View,
    scaleX: () -> Number,
    scaleY: () -> Number = scaleX,
    time: Duration = this.defaultTime,
    lazyTime: (() -> Duration)? = null,
    easing: Easing = this.defaultEasing
) = __tween({ view::scaleX[scaleX()] }, { view::scaleY[scaleY()] }, time = time.fast, lazyTime = lazyTime.fast, easing = easing, name = "scaleTo")
fun Animator.scaleTo(view: View, scaleX: Double, scaleY: Double = scaleX, time: Duration = this.defaultTime, easing: Easing = this.defaultEasing) = __tween(view::scaleX[scaleX], view::scaleY[scaleY], time = time.fast, easing = easing, name = "scaleTo")
fun Animator.scaleTo(view: View, scaleX: Float, scaleY: Float = scaleX, time: Duration = this.defaultTime, easing: Easing = this.defaultEasing) = scaleTo(view, scaleX.toDouble(), scaleY.toDouble(), time, easing)
fun Animator.scaleTo(view: View, scaleX: Int, scaleY: Int = scaleX, time: Duration = this.defaultTime, easing: Easing = this.defaultEasing) = scaleTo(view, scaleX.toDouble(), scaleY.toDouble(), time, easing)

private val (() -> Duration)?.fast: (() -> FastDuration)? get() {
    if (this == null) return null
    return { this().fast }
}

fun Animator.moveTo(
    view: View,
    x: () -> Number = { view.x },
    y: () -> Number = { view.y },
    time: Duration = this.defaultTime,
    lazyTime: (() -> Duration)? = null,
    easing: Easing = this.defaultEasing
) = __tween({ view::x[x()] }, { view::y[y()] }, time = time.fast, lazyTime = lazyTime.fast, easing = easing, name = "moveTo")
fun Animator.moveTo(view: View, x: Double, y: Double, time: Duration = this.defaultTime, easing: Easing = this.defaultEasing) = __tween(view::x[x], view::y[y], time = time.fast, easing = easing, name = "moveTo")
fun Animator.moveTo(view: View, x: Float, y: Float, time: Duration = this.defaultTime, easing: Easing = this.defaultEasing) = moveTo(view, x.toDouble(), y.toDouble(), time, easing)
fun Animator.moveTo(view: View, x: Int, y: Int, time: Duration = this.defaultTime, easing: Easing = this.defaultEasing) = moveTo(view, x.toDouble(), y.toDouble(), time, easing)

fun Animator.moveToWithSpeed(view: View, x: () -> Number = { view.x }, y: () -> Number = { view.y }, speed: () -> Number = { this.defaultSpeed }, easing: Easing = this.defaultEasing) = __tween({ view::x[x()] }, { view::y[y()] }, lazyTime = { (hypot(view.x - x().toDouble(), view.y - y().toDouble()) / speed().toDouble()).fastSeconds }, easing = easing, name = "moveToWithSpeed")
fun Animator.moveToWithSpeed(view: View, x: Double, y: Double, speed: Double = this.defaultSpeed, easing: Easing = this.defaultEasing) = __tween(view::x[x], view::y[y], lazyTime = { (hypot(view.x - x, view.y - y) / speed.toDouble()).fastSeconds }, easing = easing, name = "moveToWithSpeed")
fun Animator.moveToWithSpeed(view: View, x: Float, y: Float, speed: Number = this.defaultSpeed, easing: Easing = this.defaultEasing) = moveToWithSpeed(view, x.toDouble(), y.toDouble(), speed.toDouble(), easing)
fun Animator.moveToWithSpeed(view: View, x: Int, y: Int, speed: Number = this.defaultSpeed, easing: Easing = this.defaultEasing) = moveToWithSpeed(view, x.toDouble(), y.toDouble(), speed.toDouble(), easing)

fun Animator.moveInPath(
    view: View,
    path: VectorPath,
    includeLastPoint: Boolean = true,
    time: Duration = this.defaultTime,
    lazyTime: (() -> Duration)? = null,
    easing: Easing = this.defaultEasing
) = __tween({ view::pos.get(path, includeLastPoint = includeLastPoint) }, time = time.fast, lazyTime = lazyTime.fast, easing = easing, name = "moveInPath")
fun Animator.moveInPath(
    view: View,
    points: PointList,
    time: Duration = this.defaultTime,
    lazyTime: (() -> Duration)? = null,
    easing: Easing = this.defaultEasing
) = __tween({ view::pos[points] }, time = time.fast, lazyTime = lazyTime.fast, easing = easing, name = "moveInPath")

fun Animator.moveInPathWithSpeed(view: View, path: VectorPath, includeLastPoint: Boolean = true, speed: () -> Number = { this.defaultSpeed }, easing: Easing = this.defaultEasing) = __tween({ view::pos.get(path, includeLastPoint = includeLastPoint) }, lazyTime = { (path.length / speed().toDouble()).fastSeconds }, easing = easing, name = "moveInPathWithSpeed")
fun Animator.moveInPathWithSpeed(view: View, points: PointList, speed: () -> Number = { this.defaultSpeed }, easing: Easing = this.defaultEasing) = __tween({ view::pos[points] }, lazyTime = { (points.length / speed().toDouble()).fastSeconds }, easing = easing, name = "moveInPathWithSpeed")

fun Animator.alpha(view: View, alpha: Float, time: Duration = this.defaultTime, easing: Easing = this.defaultEasing) = __tween(view::alphaF[alpha], time = time.fast, easing = easing, name = "alpha")
fun Animator.alpha(view: View, alpha: Double, time: Duration = this.defaultTime, easing: Easing = this.defaultEasing) = alpha(view, alpha.toFloat(), time, easing)
fun Animator.alpha(view: View, alpha: Int, time: Duration = this.defaultTime, easing: Easing = this.defaultEasing) = alpha(view, alpha.toFloat(), time, easing)

fun Animator.rotateTo(view: View, angle: Angle, time: Duration = this.defaultTime, easing: Easing = this.defaultEasing) = __tween(view::rotation[angle], time = time.fast, easing = easing, name = "rotateTo")
fun Animator.rotateTo(
    view: View,
    rotation: () -> Angle,
    time: Duration = this.defaultTime,
    lazyTime: (() -> Duration)? = null,
    easing: Easing = this.defaultEasing
) = __tween({ view::rotation[rotation()] }, time = time.fast, lazyTime = lazyTime.fast, easing = easing, name = "rotateTo")

fun Animator.show(view: View, time: Duration = this.defaultTime, easing: Easing = this.defaultEasing) = alpha(view, 1.0, time, easing)
fun Animator.hide(view: View, time: Duration = this.defaultTime, easing: Easing = this.defaultEasing) = alpha(view, 0.0, time, easing)

private val VectorPath.length: Double get() = getCurves().length
private val PointList.length: Double get() {
    var sum = 0.0
    for (n in 1 until size) sum += Point.distance(get(n - 1), get(n))
    return sum
}

class AnimateCancellationException(val completeOnCancel: Boolean? = null) : CancellationException("AnimateCancellationException")
