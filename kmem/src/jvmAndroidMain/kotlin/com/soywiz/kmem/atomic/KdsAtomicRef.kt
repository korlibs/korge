package com.soywiz.kmem.atomic

actual class KmemAtomicRef<T> actual constructor(initial: T) {
    val ref = java.util.concurrent.atomic.AtomicReference<T>(initial)
    actual var value: T
        get() = ref.get()
        set(value) { ref.set(value) }
}
