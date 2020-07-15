package com.soywiz.kds.atomic

import kotlin.native.concurrent.AtomicReference

// @TODO: Use AtomicReference
actual class KdsAtomicRef<T> actual constructor(initial: T) {
    val ref = AtomicReference(initial.kdsFreeze())
    actual var value: T
        get() = ref.value
        set(value) = run { ref.value = value.kdsFreeze() }
}
