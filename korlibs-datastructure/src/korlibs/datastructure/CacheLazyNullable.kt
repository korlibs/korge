package korlibs.datastructure

import kotlin.reflect.*

inline fun <T> cacheLazyNullable(field: KMutableProperty0<T?>, gen: () -> T): T {
    var result = field.get()
    if (result == null) {
        result = gen()
        field.set(result)
    }
    return result!!
}
