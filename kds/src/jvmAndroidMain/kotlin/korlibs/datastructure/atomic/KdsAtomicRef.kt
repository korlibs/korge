package korlibs.datastructure.atomic

actual class KdsAtomicRef<T> actual constructor(initial: T) {
    val ref = java.util.concurrent.atomic.AtomicReference<T>(initial)
    actual var value: T
        get() = ref.get()
        set(value) { ref.set(value) }
}