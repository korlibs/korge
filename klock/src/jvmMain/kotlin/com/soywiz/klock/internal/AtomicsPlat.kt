package com.soywiz.klock.internal

import java.util.concurrent.atomic.AtomicReference

internal actual fun <T> klockAtomicLazy(initializer: () -> T): Lazy<T> = lazy(initializer)
internal actual fun <T> klockLazyOrGet(initializer: () -> T): Lazy<T> = lazy(initializer)

internal actual class KlockAtomicRef<T> actual constructor(initial: T) {
    val ref = AtomicReference(initial)
    actual var value: T
        get() = ref.get()
        set(value) { ref.set(value) }
}

internal actual class KlockLock {
    actual inline operator fun <T> invoke(callback: () -> T): T = synchronized(this) { callback() }
}

internal actual val klockIsKotlinNative: Boolean = false
