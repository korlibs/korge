package korlibs.logger.atomic

internal actual class KloggerAtomicRef<T> actual constructor(initial: T) {
    private var ref = initial

    actual var value: T
        get() = ref
        set(value) {
            ref = value
        }

    actual inline fun update(block: (T) -> T) {
        ref = block(ref)
    }
}
