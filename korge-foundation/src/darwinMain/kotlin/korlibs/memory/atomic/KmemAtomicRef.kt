package korlibs.memory.atomic

// @TODO: Use AtomicReference
actual class KmemAtomicRef<T> actual constructor(initial: T) {
    val ref = kotlin.concurrent.AtomicReference(initial)
    actual var value: T
        get() = ref.value
        set(value) { ref.value = value }
}
