package korlibs.datastructure

inline fun <T1> hashCode(v1: T1): Int {
    var hash = 7
    hash = 31 * hash + v1.hashCode()
    return hash
}
inline fun <T1, T2> hashCode(v1: T1, v2: T2): Int {
    var hash = 7
    hash = 31 * hash + v1.hashCode()
    hash = 31 * hash + v2.hashCode()
    return hash
}
inline fun <T1, T2, T3> hashCode(v1: T1, v2: T2, v3: T3): Int {
    var hash = 7
    hash = 31 * hash + v1.hashCode()
    hash = 31 * hash + v2.hashCode()
    hash = 31 * hash + v3.hashCode()
    return hash
}
inline fun <T1, T2, T3, T4> hashCode(v1: T1, v2: T2, v3: T3, v4: T4): Int {
    var hash = 7
    hash = 31 * hash + v1.hashCode()
    hash = 31 * hash + v2.hashCode()
    hash = 31 * hash + v3.hashCode()
    hash = 31 * hash + v4.hashCode()
    return hash
}
inline fun <T1, T2, T3, T4, T5> hashCode(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5): Int {
    var hash = 7
    hash = 31 * hash + v1.hashCode()
    hash = 31 * hash + v2.hashCode()
    hash = 31 * hash + v3.hashCode()
    hash = 31 * hash + v4.hashCode()
    hash = 31 * hash + v5.hashCode()
    return hash
}
inline fun <T1, T2, T3, T4, T5, T6> hashCode(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6): Int {
    var hash = 7
    hash = 31 * hash + v1.hashCode()
    hash = 31 * hash + v2.hashCode()
    hash = 31 * hash + v3.hashCode()
    hash = 31 * hash + v4.hashCode()
    hash = 31 * hash + v5.hashCode()
    hash = 31 * hash + v6.hashCode()
    return hash
}
