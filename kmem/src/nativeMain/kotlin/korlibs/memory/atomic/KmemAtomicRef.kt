package korlibs.memory.atomic

import kotlin.native.concurrent.AtomicReference

// @TODO: Use AtomicReference
actual class KmemAtomicRef<T> actual constructor(initial: T) {
    val ref = AtomicReference(initial)
    actual var value: T
        get() = ref.value
        set(value) { ref.value = value }
}
