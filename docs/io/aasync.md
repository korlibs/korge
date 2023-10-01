---
permalink: /io/async/
group: io
layout: default
title: Async Tools
title_prefix: KorIO
description: "Signals, Once, Atomic, ThreadLocal, AsyncCache, AsyncCloseable, AsyncByteArrayDeque, delay with Klock TimeSpan, runBlockingNoSuspensions..."
fa-icon: fa-sync-alt
priority: 3
---

KorIO has extra asynchronous and threading utilities.



## AsyncByteArrayDeque

```kotlin
class AsyncByteArrayDeque(bufferSize: Int = 1024) : AsyncOutputStream, AsyncInputStream {
    override suspend fun write(buffer: ByteArray, offset: Int, len: Int)
    override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int
    override suspend fun close()
}
```

## AsyncCache

```kotlin
class AsyncCache {
    suspend operator fun <T> invoke(key: String, gen: suspend () -> T): T
}

class AsyncCacheGen<T>(gen: suspend (key: String) -> T) {
    suspend operator fun invoke(key: String): T
}
```

## AsyncCloseable

```kotlin
interface AsyncCloseable {
	suspend fun close()
}
suspend inline fun <T : AsyncCloseable, TR> T.use(callback: T.() -> TR): TR

val AsyncCloseable.Companion.DUMMY: AsyncCloseable
```

## Klock integration

```kotlin
suspend fun delay(time: TimeSpan)
suspend fun CoroutineContext.delay(time: TimeSpan)
suspend fun <T> withTimeout(time: TimeSpan, block: suspend CoroutineScope.() -> T): T
```

## runBlockingNoSuspensions

When we want to execute a suspend function in all the targets including JavaScript
in the cases we know for sure that no suspensions will happen:

```kotlin
fun <T : Any> runBlockingNoSuspensions(callback: suspend () -> T): T
```

## Signals

```kotlin
class Signal<T>(onRegister: () -> Unit = {}) {
    // Dispatching
	operator fun invoke(value: T): Unit

    // Subscription manipulation
    val listenerCount: Int
    fun clear()

    // Registering/Subscribing/Waiting
	fun once(handler: (T) -> Unit): Closeable
	fun add(handler: (T) -> Unit): Closeable
	operator fun invoke(handler: (T) -> Unit): Closeable
	suspend fun waitOne(): T
    suspend fun listen(): ReceiveChannel<T>
}

fun <TI, TO> Signal<TI>.mapSignal(transform: (TI) -> TO): Signal<TO>
suspend fun Iterable<Signal<*>>.waitOne(): Any?
fun <T> Signal<T>.waitOneAsync(): Deferred<T>
suspend fun <T> Signal<T>.addSuspend(handler: suspend (T) -> Unit): Closeable
fun <T> Signal<T>.addSuspend(context: CoroutineContext, handler: suspend (T) -> Unit): Closeable
suspend fun <T> Signal<T>.waitOne(timeout: TimeSpan): T?
suspend fun <T> Signal<T>.waitOneOpt(timeout: TimeSpan?): T?
suspend inline fun <T> Map<Signal<Unit>, T>.executeAndWaitAnySignal(callback: () -> Unit): T
suspend inline fun <T> Iterable<Signal<T>>.executeAndWaitAnySignal(callback: () -> Unit): Pair<Signal<T>, T>
suspend inline fun <T> Signal<T>.executeAndWaitSignal(callback: () -> Unit): T
```


## Once and AsyncOnce

```kotlin
class Once {
	inline operator fun invoke(callback: () -> Unit)
}

class AsyncOnce<T> {
	suspend operator fun invoke(callback: suspend () -> T): T
}
```

## Atomic

Korio includes atomic tools (they are less efficient that atomicfu, but do not require compiler plugins):

```kotlin
fun <T> korAtomic(initial: T): KorAtomicRef<T>
fun korAtomic(initial: Boolean): KorAtomicBoolean
fun korAtomic(initial: Int): KorAtomicInt
fun korAtomic(initial: Long): KorAtomicLong
```

## ThreadLocal

```kotlin
class threadLocal<T>(val gen: () -> T) {
	inline operator fun getValue(thisRef: Any?, property: KProperty<*>): T
	inline operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
}

abstract class NativeThreadLocal<T>() {
    abstract fun initialValue(): T
	fun get(): T
	fun set(value: T)
}
```
