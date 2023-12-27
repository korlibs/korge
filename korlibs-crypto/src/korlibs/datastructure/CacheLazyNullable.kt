package korlibs.datastructure

import kotlin.reflect.KMutableProperty0

inline fun <T> cacheLazyNullable(field: KMutableProperty0<T?>, gen: () -> T): T {
    var result = field.get()
    if (result == null) {
        result = gen()
        field.set(result)
    }
    return result!!
}
