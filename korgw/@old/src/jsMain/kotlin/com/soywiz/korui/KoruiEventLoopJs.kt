package com.soywiz.korui

import com.soywiz.korio.*
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import com.soywiz.korui.async.*
import kotlinx.coroutines.*
import kotlinx.browser.*
import kotlin.coroutines.*

actual val KoruiDispatcher: CoroutineDispatcher get() = if (OS.isJsNodeJs) NodeDispatcher else HtmlDispatcher

private external fun setTimeout(handler: dynamic, timeout: Int = definedExternally): Int
private external fun clearTimeout(handle: Int = definedExternally)

@UseExperimental(InternalCoroutinesApi::class)
object NodeDispatcher : CoroutineDispatcher(), Delay, DelayFrame {
	override fun dispatch(context: CoroutineContext, block: Runnable) {
		setTimeout({ block.run() }, 0)
	}

    @InternalCoroutinesApi
	override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>): Unit {
		val timeout = setTimeout({ with(continuation) { resumeUndispatched(Unit) } }, timeMillis.toInt())
		// Actually on cancellation, but clearTimeout is idempotent
		continuation.invokeOnCancellation {
			clearTimeout(timeout)
		}
	}

	@InternalCoroutinesApi
    override fun invokeOnTimeout(timeMillis: Long, block: Runnable): DisposableHandle {
		val timeout = setTimeout({ block.run() }, timeMillis.toInt())
		return object : DisposableHandle {
			override fun dispose() {
				clearTimeout(timeout)
			}
		}
	}
}

@UseExperimental(InternalCoroutinesApi::class)
object HtmlDispatcher : CoroutineDispatcher(), Delay, DelayFrame {
	private const val messageName = "dispatchCoroutine"

	private val queue = object : MessageQueue() {
		override fun schedule() {
            if (OS.isJsBrowser) {
                global.postMessage(messageName, "*")
            } else {
                setTimeout({ process() }, 0)
            }
		}
	}

	init {
        if (OS.isJsBrowser) {
            global.addEventListener("message", { event: dynamic ->
                if (event.source == global && event.data === messageName) {
                    event.stopPropagation()
                    queue.process()
                }
            }, true)
        }
	}

	override fun dispatch(context: CoroutineContext, block: Runnable) {
		queue.enqueue(block)
	}

	override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>): Unit {
        global.setTimeout({ with(continuation) { resumeUndispatched(Unit) } }, timeMillis.toInt())
		//window.setTimeout({ with(continuation) { resume(Unit) } }, unit.toMillis(time).toInt())
	}

	override fun invokeOnTimeout(timeMillis: Long, block: Runnable): DisposableHandle {
		val handle = global.setTimeout({ block.run() }, timeMillis.toInt())
		return object : DisposableHandle {
			override fun dispose() {
                global.clearTimeout(handle)
			}
		}
	}

	override fun delayFrame(continuation: CancellableContinuation<Unit>) {
        global.requestAnimationFrame { with(continuation) { resumeUndispatched(Unit) } }
		//window.requestAnimationFrame { with(continuation) { resume(Unit) } }
	}

	override fun toString() = "HtmlDispatcher"
}

internal open class Queue<T : Any> {
	private var queue = arrayOfNulls<Any?>(8)
	private var head = 0
	private var tail = 0

	val isEmpty get() = head == tail

	fun poll(): T? {
		if (isEmpty) return null
		val result = queue[head]!!
		queue[head] = null
		head = head.next()
		@Suppress("UNCHECKED_CAST")
		return result as T
	}

	tailrec fun add(element: T) {
		val newTail = tail.next()
		if (newTail == head) {
			resize()
			add(element) // retry with larger size
			return
		}
		queue[tail] = element
		tail = newTail
	}

	private fun resize() {
		var i = head
		var j = 0
		val a = arrayOfNulls<Any?>(queue.size * 2)
		while (i != tail) {
			a[j++] = queue[i]
			i = i.next()
		}
		queue = a
		head = 0
		tail = j
	}

	private fun Int.next(): Int {
		val j = this + 1
		return if (j == queue.size) 0 else j
	}
}

internal abstract class MessageQueue : Queue<Runnable>() {
	val yieldEvery = 16 // yield to JS event loop after this many processed messages

	private var scheduled = false

	abstract fun schedule()

	fun enqueue(element: Runnable) {
		add(element)
		if (!scheduled) {
			scheduled = true
			schedule()
		}
	}

	fun process() {
		try {
			// limit number of processed messages
			repeat(yieldEvery) {
				val element = poll() ?: return@process
				element.run()
			}
		} finally {
			if (isEmpty) {
				scheduled = false
			} else {
				schedule()
			}
		}
	}
}

internal actual suspend fun KoruiWrap(entry: suspend (KoruiContext) -> Unit) {
	entry(KoruiContext())
}
