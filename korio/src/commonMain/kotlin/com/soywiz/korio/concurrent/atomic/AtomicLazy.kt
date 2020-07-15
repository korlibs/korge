package com.soywiz.korio.concurrent.atomic

internal class AtomicLazyImpl<out T>(initializer: () -> T) : Lazy<T> {
	private object UNINITIALIZED
	private object INITIALIZING

	private val initializer_ = korAtomic<Function0<T>?>(initializer)
	private val value_ = korAtomic<Any?>(UNINITIALIZED)

	override val value: T
		get() {
			if (value_.compareAndSet(UNINITIALIZED, INITIALIZING)) {
				// We execute exclusively here.
				val ctor = initializer_.value
				if (ctor != null && initializer_.compareAndSet(ctor, null)) {
					value_.compareAndSet(INITIALIZING, ctor())
				} else {
					// Something wrong.
					check(false)
				}
			}
			var result: Any?
			do {
				result = value_.value
			} while (result === INITIALIZING)

			check(result !== UNINITIALIZED && result != INITIALIZING)
			@Suppress("UNCHECKED_CAST")
			return result as T
		}

	// Racy!
	override fun isInitialized(): Boolean = value_.value !== UNINITIALIZED

	override fun toString(): String = if (isInitialized())
		value_.value.toString() else "Lazy value not initialized yet."
}

// Until in Konan. Temporarily From: https://github.com/JetBrains/kotlin-native/pull/1769
fun <T> atomicLazy(initializer: () -> T): Lazy<T> = AtomicLazyImpl(initializer)
