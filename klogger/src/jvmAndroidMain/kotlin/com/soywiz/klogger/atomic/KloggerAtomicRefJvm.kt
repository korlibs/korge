package com.soywiz.klogger.atomic

import java.util.concurrent.atomic.AtomicReference

actual class KloggerAtomicRef<T> actual constructor(initial: T) {
    @PublishedApi
    internal val ref = AtomicReference<T>(initial)

    actual val value: T get() = ref.get()
    actual inline fun update(block: (T) -> T) {
        //synchronized(ref) { ref.set(ref.get()) }
        do {
            val old = ref.get()
            val new = block(old)
        } while (!ref.compareAndSet(old, new))
    }
}
