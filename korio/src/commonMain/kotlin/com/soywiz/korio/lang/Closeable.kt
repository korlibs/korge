package com.soywiz.korio.lang

import com.soywiz.kds.iterators.*
import kotlinx.coroutines.*

// @TODO: Merge [Closeable], [Disposable] and [Cancellable]

interface Disposable {
	fun dispose()

    companion object {
        operator fun invoke(callback: () -> Unit) = object : Disposable {
            override fun dispose() = callback()
        }
    }
}

interface Closeable {
	fun close(): Unit

	companion object {
		operator fun invoke(callback: () -> Unit) = object : Closeable {
			override fun close() = callback()
		}
	}
}

object DummyCloseable : Closeable {
	override fun close() {
	}
}

interface OptionalCloseable : Closeable {
	override fun close(): Unit = Unit
}

// Kotlin BUG : Exception in thread "AWT-EventQueue-0" java.lang.NoClassDefFoundError: com/soywiz/korio/lang/CloseableKt$Closeable$1
//fun Closeable(callback: () -> Unit) = object : Closeable {
//	override fun close() = callback()
//}

//java.lang.NoClassDefFoundError: com/soywiz/korio/lang/CloseableKt$Closeable$1 (wrong name: com/soywiz/korio/lang/CloseableKt$closeable$1)
//  at java.lang.ClassLoader.defineClass1(Native Method)
//fun Iterable<Closeable>.closeable(): Closeable = Closeable {
//	for (closeable in this@closeable) closeable.close()
//}

inline fun <TCloseable : Closeable, T : Any> TCloseable.use(callback: (TCloseable) -> T): T {
	try {
		return callback(this)
	} finally {
		this.close()
	}
}

fun interface Cancellable {
	fun cancel(e: Throwable): Unit

	interface Listener {
		fun onCancel(handler: (Throwable) -> Unit): Unit
	}

	companion object {
		operator fun invoke(callback: (Throwable) -> Unit) = Cancellable { e -> callback(e) }
        operator fun invoke(cancellables: List<Cancellable>) = Cancellable { e -> cancellables.fastForEach { it.cancel(e) } }
	}
}

fun Cancellable.cancel() = cancel(CancellationException(""))

fun Iterable<Cancellable>.cancel(e: Throwable = CancellationException("")): Unit =
	run { for (c in this) c.cancel(e) }

fun Iterable<Cancellable>.cancellable() = Cancellable { this.cancel() }

fun Iterable<Closeable>.close() = run { for (c in this) c.close() }
fun Iterable<Closeable>.closeable() = Closeable { this.close() }

fun Closeable.cancellable() = Cancellable { this.close() }
fun Closeable.disposable() = Disposable { this.close() }
fun Cancellable.closeable(e: () -> Throwable = { CancellationException("") }) =
	Closeable { this.cancel(e()) }
