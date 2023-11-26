package korlibs.datastructure

import kotlin.reflect.*

inline fun <T, R> keep(mut: KMutableProperty0<T>, block: () -> R): R {
    val temp = mut.get()
    try {
        return block()
    } finally {
        mut.set(temp)
    }
}
