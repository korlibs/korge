package korlibs.math.geom

import korlibs.datastructure.*
import korlibs.math.geom.bezier.*
import korlibs.math.isAlmostEquals
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
        is Ray -> e.isAlmostEquals((a as? Ray?) ?: return false, absoluteTolerance.toFloat())
        is Point -> e.isAlmostEquals((a as? Point?) ?: return false, absoluteTolerance.toFloat())
        is Vector3 -> e.isAlmostEquals((a as? Vector3?) ?: return false, absoluteTolerance.toFloat())
        is Matrix3 -> e.isAlmostEquals((a as? Matrix3?) ?: return false, absoluteTolerance.toFloat())
        is Matrix4 -> e.isAlmostEquals((a as? Matrix4?) ?: return false, absoluteTolerance.toFloat())
        is Quaternion -> e.isAlmostEquals((a as? Quaternion?) ?: return false, absoluteTolerance.toFloat())
        is MPoint -> e.isAlmostEquals((a as? MPoint?) ?: return false, absoluteTolerance)
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
        is Matrix -> e.isAlmostEquals((a as Matrix), absoluteTolerance.toFloat())
        is MatrixTransform -> e.isAlmostEquals((a as MatrixTransform), absoluteTolerance.toFloat())
        is Rectangle -> e.isAlmostEquals((a as Rectangle), absoluteTolerance.toFloat())
        is MRectangle -> e.isAlmostEquals((a as MRectangle), absoluteTolerance)
        is Bezier -> e.points.isAlmostEqualsGeneric((a as? Bezier)?.points, absoluteTolerance)
        is Bezier.ProjectedPoint -> e.isAlmostEquals((a as Bezier.ProjectedPoint), absoluteTolerance.toFloat())
        is PointList -> e.toList().isAlmostEqualsGeneric((a as? PointList)?.toList(), absoluteTolerance)
        is FloatArrayList -> e.toList().isAlmostEqualsGeneric((a as? FloatArrayList)?.toList(), absoluteTolerance)
        is DoubleArrayList -> e.toList().isAlmostEqualsGeneric((a as? DoubleArrayList)?.toList(), absoluteTolerance)
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
