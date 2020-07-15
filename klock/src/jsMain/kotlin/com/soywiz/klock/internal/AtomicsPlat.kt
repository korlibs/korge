package com.soywiz.klock.internal

internal actual fun <T> klockAtomicLazy(initializer: () -> T): Lazy<T> = lazy(initializer)
internal actual fun <T> klockLazyOrGet(initializer: () -> T): Lazy<T> = lazy(initializer)

internal actual class KlockAtomicRef<T> actual constructor(initial: T) {
    actual var value: T = initial
}

internal actual class KlockLock {
    actual inline operator fun <T> invoke(callback: () -> T): T = callback()
}

internal actual val klockIsKotlinNative: Boolean = false
