package korlibs.math.geom

import korlibs.datastructure.*
import korlibs.math.*
import kotlin.math.*
import kotlin.test.*

fun <
    //@OnlyInputTypes
T> assertEqualsFloat(
    expected: T?,
    actual: T?,
    absoluteTolerance: Double = 0.001,
    message: String = ""
) {
    if (expected is List<*> && actual is List<*> && expected.size != actual.size) {
        throw AssertionError("${expected.size} != ${actual.size} : ${expected}, ${actual}")
    }
    if (!expected.isAlmostEqualsGeneric(actual, absoluteTolerance)) {
        //org.junit.ComparisonFailure: expected:<[a]> but was:<[b]>

        //throw AssertionError("Actual: $actual\nExpected: $expected\nabsoluteTolerance=$absoluteTolerance\n$message")
        assertEquals("$expected", "$actual", message)
        throw AssertionError("expected:<[$expected]> but was:<[$actual]>\nabsoluteTolerance=$absoluteTolerance\n$message")
    }
}

private fun toNumberList(v: Any): List<Number> {
    return when (v) {
        is IntArray -> v.map { it.toDouble() }
        is FloatArray -> v.map { it.toDouble() }
        is DoubleArray -> v.map { it }
        else -> TODO()
    }
}

private fun <T : Any> T?.isAlmostEqualsGeneric(
    a: T?,
    absoluteTolerance: Double = 0.00001,
): Boolean {
    val e = this
    if (e == null || a == null) return (e == null) && (a == null)
    return when (e) {
        is Number -> when {
            a !is Number? -> false
            e.toDouble().isNaN() && a.toDouble().isNaN() -> true
            else -> e.toDouble().isAlmostEquals(a.toDouble(), absoluteTolerance)
        }
        is IntArray, is FloatArray, is DoubleArray -> toNumberList(e).isAlmostEqualsGeneric(toNumberList(a), absoluteTolerance)
        is IsAlmostEquals<*> -> (e as IsAlmostEquals<Any>).isAlmostEquals(a, absoluteTolerance)
        is IsAlmostEqualsF<*> -> (e as IsAlmostEqualsF<Any>).isAlmostEquals(a, sqrt(absoluteTolerance).toFloat())
        is DoubleList -> e.isAlmostEquals(a as DoubleList, absoluteTolerance)
        is FloatList -> e.isAlmostEquals(a as FloatList, absoluteTolerance.toFloat())
        is List<*> -> {
            if (a !is List<*>?) return false
            if (e.size != a.size) return false
            for (n in 0 until e.size) {
                if (!e[n].isAlmostEqualsGeneric(a[n], absoluteTolerance)) return false
            }
            true
        }
        else -> e == a
    }
}
