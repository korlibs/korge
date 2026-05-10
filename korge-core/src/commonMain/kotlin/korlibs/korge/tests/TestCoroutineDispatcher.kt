package korlibs.korge.tests

import korlibs.datastructure.*
import korlibs.datastructure.lock.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.time.*

@OptIn(InternalCoroutinesApi::class)
@Deprecated("")
class TestCoroutineDispatcher(val frameTime: Duration = 16.milliseconds) :
	//CoroutineDispatcher(), ContinuationInterceptor, Delay, DelayFrame {
	CoroutineDispatcher(), ContinuationInterceptor, Delay {
	var time = 0L; private set

	class TimedTask(val time: Long, val callback: suspend () -> Unit) {
		override fun toString(): String = "TimedTask(time=$time)"
	}

	private val tasks = PriorityQueue<TimedTask>(Comparator { a, b -> a.time.compareTo(b.time) })
    private val lock = NonRecursiveLock()

	//override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
	//	return object : Continuation<T> {
	//		override val context: CoroutineContext = continuation.context
	//		override fun resumeWith(result: Result<T>) {
	//			val exception = result.exceptionOrNull()
	//			if (exception != null) {
	//				continuation.resumeWithException(exception)
	//			} else {
	//				continuation.resume(result.getOrThrow())
	//			}
	//		}
	//	}
	//}

	private fun scheduleAfter(time: Int, callback: suspend () -> Unit) {
        lock {
            tasks += TimedTask(this.time + time) {
                callback()
            }
        }
	}

	override fun dispatch(context: CoroutineContext, block: Runnable) {
		scheduleAfter(0) { block.run() }
	}

	override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
		scheduleAfter(timeMillis.toInt()) { continuation.resume(Unit) }
	}

	//override fun delayFrame(continuation: CancellableContinuation<Unit>) {
	//	scheduleAfter(frameTime.millisecondsInt) { continuation.resume(Unit) }
	//}

	var exception: Throwable? = null
	fun loop() {
		//println("doStep: currentThreadId=$currentThreadId")
		if (exception != null) throw exception ?: error("error")
		//println("TASKS: ${tasks.size}")
		while (true) {
			val task = lock { if (tasks.isNotEmpty()) tasks.removeHead() else null } ?: break
			this.time = task.time
			//println("RUN: $task")

            // @TODO: This is probably wrong
			task.callback.startCoroutine(object : Continuation<Unit> {
				override val context: CoroutineContext = this@TestCoroutineDispatcher

				override fun resumeWith(result: Result<Unit>) {
					val exception = result.exceptionOrNull()
					exception?.printStackTrace()
					this@TestCoroutineDispatcher.exception = exception
				}
			})
		}
	}

    @Deprecated("Use loop instead if possible")
    suspend fun step(time: Duration) {
        this.time += time.millisecondsLong
        while (true) {
            val task = lock { if (tasks.isNotEmpty() && this.time >= tasks.head.time) tasks.removeHead() else null } ?: break
            task.callback()
        }
    }

	fun loop(entry: suspend () -> Unit) {
		entry.startCoroutine(object : Continuation<Unit> {
			override val context: CoroutineContext = this@TestCoroutineDispatcher

			override fun resumeWith(result: Result<Unit>) {
				val exception = result.exceptionOrNull()
				exception?.printStackTrace()
				this@TestCoroutineDispatcher.exception = exception
			}
		})
		loop()
	}
}
