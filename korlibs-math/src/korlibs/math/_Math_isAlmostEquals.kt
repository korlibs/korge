package korlibs.math

interface IsAlmostEquals<T> {
    fun isAlmostEquals(other: T, epsilon: Double = 0.000001): Boolean
}

interface IsAlmostEqualsF<T> {
    fun isAlmostEquals(other: T, epsilon: Float = 0.0001f): Boolean
}
