package com.soywiz.korge.compose

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.*
import com.soywiz.klock.*
import com.soywiz.korge.view.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

/*
suspend fun main() = Korge(width = 256, height = 64) {
    setComposeContent {
        var count by remember { mutableStateOf(0) }
        VStack {
            Text("$count")
            HStack {
                Button("-") { count-- }
                Button("+") { count++ }
            }
        }
    }
}
 */

// We would implement an Applier class like the following, which would teach compose how to
// manage a tree of Nodes.
// https://developer.android.com/reference/kotlin/androidx/compose/runtime/Applier
class NodeApplier(root: View) : AbstractApplier<View>(root) {
    override fun insertTopDown(index: Int, instance: View) {
        println("insertTopDown[$index]: $instance")
        val container = current as Container
        container.addChildAt(instance, container.numChildren - 1 - index)
        //current.children.add(index, instance)
    }

    override fun insertBottomUp(index: Int, instance: View) {
        println("insertBottomUp[$index]: $instance")
        (current as? Container?)?.addChildAt(instance, index)
        // Ignored as the tree is built top-down.
    }

    override fun remove(index: Int, count: Int) {
        println("remove[$index]..$count")
        (current as? Container?)?.removeChildAt(index, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        println("move: $from -> $to [$count]")
        repeat(count) { (current as? Container?)?.swapChildrenAt(from + it, to + it) }
    }

    override fun onClear() {
        println("onClear")
        (current as? Container?)?.removeChildren()
    }
}

class GlobalSnapshotManager(val dispatcher: CoroutineDispatcher) {
    private var started = false
    private var commitPending = false
    private var removeWriteObserver: (ObserverHandle)? = null

    private val scheduleScope = CoroutineScope(dispatcher + SupervisorJob())

    fun ensureStarted() {
        if (!started) {
            started = true
            removeWriteObserver = Snapshot.registerGlobalWriteObserver(globalWriteObserver)
        }
    }

    private val globalWriteObserver: (Any) -> Unit = {
        // Race, but we don't care too much if we end up with multiple calls scheduled.
        if (!commitPending) {
            commitPending = true
            schedule {
                commitPending = false
                Snapshot.sendApplyNotifications()
            }
        }
    }

    /**
     * List of deferred callbacks to run serially. Guarded by its own monitor lock.
     */
    private val scheduledCallbacks = mutableListOf<() -> Unit>()

    /**
     * Guarded by [scheduledCallbacks]'s monitor lock.
     */
    private var isSynchronizeScheduled = false

    /**
     * Synchronously executes any outstanding callbacks and brings snapshots into a
     * consistent, updated state.
     */
    private fun synchronize() {
        scheduledCallbacks.forEach { it.invoke() }
        scheduledCallbacks.clear()
        isSynchronizeScheduled = false
    }

    private fun schedule(block: () -> Unit) {
        scheduledCallbacks.add(block)
        if (!isSynchronizeScheduled) {
            isSynchronizeScheduled = true
            scheduleScope.launch { synchronize() }
        }
    }
}

class MonotonicClockImpl(val views: Views) : MonotonicFrameClock {
    override suspend fun <R> withFrameNanos(
        onFrame: (Long) -> R
    ): R {
        //println("MonotonicClockImpl.withFrameNanos")
        return suspendCoroutine { continuation ->
            val start = DateTime.now()
            views.stage.onNextFrame {
                val now = DateTime.now()
                continuation.resume(onFrame((now - start).nanoseconds.toLong()))
            }
        }
    }
}


@Suppress("NONREADONLY_CALL_IN_READONLY_COMPOSABLE")
@Composable
inline fun <T : View> ComposeKorgeView(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit,
    content: @Composable () -> Unit
) {
    ComposeNode<T, NodeApplier>(factory, update, content)
}

@Composable inline fun <T : View> ComposeKorgeView(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit
) {
    ComposeNode<T, NodeApplier>(factory, update)
}

/*
// TOOLS

fun Container.removeChildAt(index: Int): Boolean {
    return removeChild(getChildAtOrNull(index))
}

// @TODO: Optimize
fun Container.removeChildAt(index: Int, count: Int) {
    repeat(count) { removeChildAt(index) }
}

fun Container.swapChildrenAt(indexA: Int, indexB: Int) {
    val a = getChildAtOrNull(indexA) ?: return
    val b = getChildAtOrNull(indexB) ?: return
    swapChildren(a, b)
}
*/
