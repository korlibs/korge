@file:OptIn(ExperimentalStdlibApi::class)

package korlibs.io.lang

import korlibs.datastructure.iterators.fastForEach
import kotlinx.coroutines.CancellationException

// @TODO: Merge [Closeable], [Disposable] and [Cancellable]

interface Disposable {
    fun dispose()

    companion object {
        operator fun invoke(callback: () -> Unit) = object : Disposable {
            override fun dispose() = callback()
        }
    }
}

object DummyAutoCloseable : AutoCloseable {
    override fun close() = Unit
}

interface OptionalAutoCloseable : AutoCloseable {
    override fun close(): Unit = Unit
}

fun interface Cancellable {
    fun cancel(e: Throwable)

    companion object {
        operator fun invoke(callback: (Throwable) -> Unit) = Cancellable { e -> callback(e) }
        operator fun invoke(cancellables: List<Cancellable>) = Cancellable { e -> cancellables.fastForEach { it.cancel(e) } }
    }
}

fun Cancellable.cancel() = cancel(CancellationException(""))

fun Iterable<Cancellable>.cancel(e: Throwable = CancellationException("")): Unit =
    run { for (c in this) c.cancel(e) }

fun Iterable<Cancellable>.cancellable() = Cancellable { this.cancel() }

fun Iterable<AutoCloseable>.close() {
    for (c in this) c.close()
}

fun Iterable<AutoCloseable>.closeable() = object : AutoCloseable {
    override fun close() {
        close()
    }
}

fun AutoCloseable.cancellable() = Cancellable { this.close() }
fun AutoCloseable.disposable() = Disposable { this.close() }
fun Cancellable.closeable(e: () -> Throwable = { CancellationException("") }) = object : AutoCloseable {
    override fun close() {
        cancel(e())
    }
}

interface CloseableCancellable : AutoCloseable, Cancellable {
    override fun cancel(e: Throwable) {
        close()
    }
}

fun CloseableCancellable(callback: (Throwable?) -> Unit): CloseableCancellable = object : CloseableCancellable {
    override fun close() = callback(null)
    override fun cancel(e: Throwable) = callback(e)
}

class CancellableGroup() : CloseableCancellable {
    private val cancellables = arrayListOf<Cancellable>()

    constructor(vararg items: Cancellable) : this() {
        items.fastForEach { this += it }
    }

    constructor(items: Iterable<Cancellable>) : this() {
        for (it in items) this += it
    }

    operator fun plusAssign(c: CloseableCancellable) {
        cancellables += c
    }

    operator fun plusAssign(c: Cancellable) {
        cancellables += c
    }

    operator fun plusAssign(c: AutoCloseable) {
        cancellables += c.cancellable()
    }

    fun addCancellable(c: Cancellable) {
        cancellables += c
    }

    fun addCloseable(c: AutoCloseable) {
        cancellables += c.cancellable()
    }

    override fun close() {
        cancel(kotlin.coroutines.cancellation.CancellationException())
    }

    override fun cancel(e: Throwable) {
        cancellables.cancel(e)
    }
}

