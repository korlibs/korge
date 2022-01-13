package com.soywiz.klock.internal

import kotlin.native.concurrent.AtomicReference

internal actual fun <T> klockAtomicLazy(initializer: () -> T): Lazy<T> = kotlin.native.concurrent.atomicLazy(initializer)
internal actual fun <T> klockLazyOrGet(initializer: () -> T): Lazy<T> = object : Lazy<T> {
    override val value: T get() = initializer()
    override fun isInitialized(): Boolean = true
}

internal actual class KlockAtomicRef<T> actual constructor(initial: T) {
    val ref = AtomicReference(initial)
    actual var value: T
        get() = ref.value
        set(value) { ref.value = value }
}

internal actual class KlockLock {
    // @TODO: Proper do this on native
    actual inline operator fun <T> invoke(callback: () -> T): T = callback()
}

internal actual val klockIsKotlinNative: Boolean = true
