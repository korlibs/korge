package com.soywiz.klogger.atomic

import kotlin.native.concurrent.*

internal actual class KloggerAtomicRef<T> actual constructor(initial: T) {
    private val ref = FreezableAtomicReference(initial.freeze())

    actual var value: T
        get() = ref.value
        set(value) {
            ref.value = value
        }

    actual inline fun update(block: (T) -> T) {
        //synchronized(ref) { ref.set(ref.get()) }
        do {
            val old = ref.value
            val new = block(old).freeze()
        } while (!ref.compareAndSet(old, new))
    }
}
