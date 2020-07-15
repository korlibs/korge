package com.soywiz.kds.atomic

actual class KdsAtomicRef<T> actual constructor(initial: T) {
    val ref = java.util.concurrent.atomic.AtomicReference<T>(initial)
    actual var value: T
        get() = ref.get()
        set(value) = run { ref.set(value) }
}
