@file:Suppress("PackageDirectoryMismatch")

package korlibs.template.util

import korlibs.template.internal.*
import kotlin.coroutines.*

interface KorteAsyncTextWriterContainer {
    suspend fun write(writer: suspend (String) -> Unit)
}

class KorteDeferred<T> {
    private val lock = Lock()
    private var result: Result<T>? = null
    private val continuations = arrayListOf<Continuation<T>>()

    fun completeWith(result: Result<T>) {
        //println("completeWith: $result")
        lock {
            this.result = result
        }
        resolveIfRequired()
    }

    fun completeExceptionally(t: Throwable) = completeWith(Result.failure(t))
    fun complete(value: T) = completeWith(Result.success(value))

    // @TODO: Cancellable?
    suspend fun await(): T = suspendCoroutine { c ->
        lock {
            continuations += c
        }
        //println("await:$c")
        resolveIfRequired()
    }

    private fun resolveIfRequired() {
        val result = lock { result }
        if (result != null) {
            for (v in lock {
                if (continuations.isEmpty()) emptyList() else continuations.toList().also { continuations.clear() }
            }) {
                //println("resume:$v")
                v.resumeWith(result)
            }
        }
    }

    fun toContinuation(coroutineContext: CoroutineContext) = object : Continuation<T> {
        override val context: CoroutineContext = coroutineContext
        override fun resumeWith(result: Result<T>) = completeWith(result)
    }

    companion object {
        fun <T> asyncImmediately(coroutineContext: CoroutineContext, callback: suspend () -> T): KorteDeferred<T> =
            KorteDeferred<T>().also { deferred ->
                callback.startCoroutine(object : Continuation<T> {
                    override val context: CoroutineContext = coroutineContext
                    override fun resumeWith(result: Result<T>) = deferred.completeWith(result)
                })
            }
    }
}

class KorteListReader<T> constructor(val list: List<T>, val ctx: T? = null) {
    class OutOfBoundsException(val list: KorteListReader<*>, val pos: Int) : RuntimeException()

    var position = 0
    val size: Int get() = list.size
    val eof: Boolean get() = position >= list.size
    val hasMore: Boolean get() = position < list.size
    fun peekOrNull(): T? = list.getOrNull(position)
    fun peek(): T = list.getOrNull(position) ?: throw OutOfBoundsException(this, position)
    fun tryPeek(ahead: Int): T? = list.getOrNull(position + ahead)
    fun skip(count: Int = 1) = this.apply { this.position += count }
    fun read(): T = peek().apply { skip(1) }
    fun tryPrev(): T? = list.getOrNull(position - 1)
    fun prev(): T = tryPrev() ?: throw OutOfBoundsException(this, position - 1)
    fun tryRead(): T? = if (hasMore) read() else null
    fun prevOrContext(): T = tryPrev() ?: ctx ?: throw TODO("Context not defined")
    override fun toString(): String = "ListReader($list)"
}
