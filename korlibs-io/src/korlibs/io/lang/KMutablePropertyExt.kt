package korlibs.io.lang

import kotlin.reflect.KMutableProperty0

/**
 * @TODO: Is this optimized in Kotlin?
 */
inline fun <reified V, T> KMutableProperty0<V>.keep(block: () -> T): T {
    val old = this.get()
    try {
        return block()
    } finally {
        this.set(old)
    }
}
