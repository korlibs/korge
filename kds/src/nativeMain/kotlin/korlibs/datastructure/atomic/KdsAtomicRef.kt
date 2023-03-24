package korlibs.datastructure.atomic

import kotlin.native.concurrent.*

// @TODO: Use AtomicReference
actual class KdsAtomicRef<T> actual constructor(initial: T) {
    val ref = AtomicReference((initial.freeze()))
    actual var value: T
        get() = ref.value
        set(value) { ref.value = (value.freeze()) }
}
