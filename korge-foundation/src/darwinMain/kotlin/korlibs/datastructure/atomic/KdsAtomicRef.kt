package korlibs.datastructure.atomic

import kotlin.concurrent.*

// @TODO: Use AtomicReference
actual class KdsAtomicRef<T> actual constructor(initial: T) {
    val ref = AtomicReference(initial)
    actual var value: T
        get() = ref.value
        set(value) { ref.value = value }
}
