package com.soywiz.kmem.atomic

import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze

// @TODO: Use AtomicReference
actual class KmemAtomicRef<T> actual constructor(initial: T) {
    val ref = AtomicReference(initial.freeze())
    actual var value: T
        get() = ref.value
        set(value) { ref.value = value.freeze() }
}
