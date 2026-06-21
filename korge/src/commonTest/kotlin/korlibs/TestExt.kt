
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.math.geom.bezier.*
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

private fun <T : Any> T?.isAlmostEqualsGeneric(
    a: T?,
    absoluteTolerance: Double = 0.00001,
): Boolean {
    val e = this
    if (e == null || a == null) return (e == null) && (a == null)
    return when (e) {
        is Point -> e.isAlmostEquals((a as? Point?) ?: return false, absoluteTolerance)
        is MPoint -> e.isAlmostEquals((a as? MPoint?) ?: return false, absoluteTolerance)
        is Vector3F -> e.isAlmostEquals((a as? Vector3F?) ?: return false, absoluteTolerance.toFloat())
        is Vector4F -> e.isAlmostEquals((a as? Vector4F?) ?: return false, absoluteTolerance.toFloat())
        is Float -> {
            if (a !is Float?) return false
            if (e.isNaN() && a.isNaN()) return true
            e.isAlmostEquals(a, absoluteTolerance.toFloat())
        }
        is Double -> {
            if (a !is Double?) return false
            if (e.isNaN() && a.isNaN()) return true
            e.isAlmostEquals(a, absoluteTolerance)
        }
        is Matrix -> e.isAlmostEquals((a as Matrix), absoluteTolerance)
        is MatrixTransform -> e.isAlmostEquals((a as MatrixTransform), absoluteTolerance)
        is Rectangle -> e.isAlmostEquals((a as Rectangle), absoluteTolerance)
        is MRectangle -> e.isAlmostEquals((a as MRectangle), absoluteTolerance)
        is Bezier -> e.points.isAlmostEqualsGeneric((a as? Bezier)?.points, absoluteTolerance)
        is PointList -> e.toList().isAlmostEqualsGeneric((a as? PointList)?.toList(), absoluteTolerance)
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
