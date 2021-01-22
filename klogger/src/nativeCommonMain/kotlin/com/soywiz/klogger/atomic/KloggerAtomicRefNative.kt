package com.soywiz.klogger.atomic

import kotlin.native.concurrent.*

actual class KloggerAtomicRef<T> actual constructor(initial: T) {
    @PublishedApi
    internal val ref = FreezableAtomicReference<T>(initial.freeze())

    actual val value: T get() = ref.value
    actual inline fun update(block: (T) -> T) {
        //synchronized(ref) { ref.set(ref.get()) }
        do {
            val old = ref.value
            val new = block(old).freeze()
        } while (!ref.compareAndSet(old, new))
    }
}
