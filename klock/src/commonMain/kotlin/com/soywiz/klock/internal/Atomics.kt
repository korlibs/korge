package com.soywiz.klock.internal

internal expect fun <T> klockAtomicLazy(initializer: () -> T): Lazy<T>

// @TODO: Can't get lazy or atomic working on Kotlin/Native, thus in K/N for now I'm going to make some lazy stuff as a getter
internal expect fun <T> klockLazyOrGet(initializer: () -> T): Lazy<T>

internal expect class KlockAtomicRef<T> constructor(initial: T) {
    var value: T
}

internal expect class KlockLock() {
    inline operator fun <T> invoke(callback: () -> T): T
}

internal expect val klockIsKotlinNative: Boolean
