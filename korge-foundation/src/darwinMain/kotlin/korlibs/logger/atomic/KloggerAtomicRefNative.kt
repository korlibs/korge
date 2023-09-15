package korlibs.logger.atomic

import kotlin.concurrent.*

internal actual class KloggerAtomicRef<T> actual constructor(initial: T) {
    private val ref = AtomicReference(initial)

    actual var value: T
        get() = ref.value
        set(value) {
            ref.value = value
        }

    actual inline fun update(block: (T) -> T) {
        //synchronized(ref) { ref.set(ref.get()) }
        do {
            val old = ref.value
            val new = block(old)
        } while (!ref.compareAndSet(old, new))
    }
}
