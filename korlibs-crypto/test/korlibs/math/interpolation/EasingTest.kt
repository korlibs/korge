package korlibs.math.interpolation

import korlibs.datastructure.*
import korlibs.math.geom.*
import korlibs.math.test.*
import kotlin.test.*

class EasingTest {
    fun numbers(vararg numbers: Number): DoubleArrayList = doubleArrayListOf(*numbers.mapDouble { it.toDouble() })

    private fun assertEasing(easing: Easing, values: DoubleArrayList) {
        assertEqualsFloat(values, easing.getSamples(), 0.01)

    }

    @Test
    fun testEasingNames() {
        assertEquals(
            "smooth,ease-in-elastic,ease-out-elastic,ease-out-bounce,linear,ease,ease-in,ease-out,ease-in-out,ease-in-old,ease-out-old,ease-in-out-old,ease-out-in-old,ease-in-back,ease-out-back,ease-in-out-back,ease-out-in-back,ease-in-out-elastic,ease-out-in-elastic,ease-in-bounce,ease-in-out-bounce,ease-out-in-bounce,ease-in-quad,ease-out-quad,ease-in-out-quad,ease-sine,ease-clamp-start,ease-clamp-end,ease-clamp-middle",
            Easing.ALL_LIST.joinToString(",") { it.toString() }
        )
    }

    @Test
    fun testAll() {
        assertEasing(Easing.cubic(.86, .13, .22, .84), numbers(0, 0.02, 0.05, 0.1, 0.18, 0.36, 0.73, 0.87, 0.93, 0.97, 1))
        assertEasing(Easing.EASE, numbers(0, 0.1, 0.3, 0.51, 0.68, 0.8, 0.89, 0.94, 0.98, 0.99, 1))
        assertEasing(Easing.EASE_CLAMP_END, numbers(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1))
        assertEasing(Easing.EASE_CLAMP_MIDDLE, numbers(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1))
        assertEasing(Easing.EASE_CLAMP_START, numbers(0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1))
        assertEasing(Easing.EASE_IN, numbers(0, 0.02, 0.06, 0.13, 0.21, 0.32, 0.43, 0.55, 0.69, 0.84, 1))
        assertEasing(Easing.EASE_IN_BACK, numbers(0, -0.01, -0.05, -0.08, -0.1, -0.09, -0.03, 0.09, 0.29, 0.59, 1))
        assertEasing(Easing.EASE_IN_BOUNCE, numbers(0, 0.01, 0.06, 0.07, 0.23, 0.23, 0.09, 0.32, 0.7, 0.92, 1))
        assertEasing(Easing.EASE_IN_ELASTIC, numbers(0, 0, 0, 0, 0.02, -0.02, -0.03, 0.12, -0.13, -0.25, 1))
        assertEasing(Easing.EASE_IN_OLD, numbers(0, 0, 0.01, 0.03, 0.06, 0.12, 0.22, 0.34, 0.51, 0.73, 1))
        assertEasing(Easing.EASE_IN_OUT, numbers(0, 0.02, 0.08, 0.19, 0.33, 0.5, 0.67, 0.81, 0.92, 0.98, 1))
        assertEasing(Easing.EASE_IN_OUT_BACK, numbers(0, -0.02, -0.05, -0.01, 0.15, 0.5, 0.85, 1.01, 1.05, 1.02, 1))
        assertEasing(Easing.EASE_IN_OUT_BOUNCE, numbers(0, 0.03, 0.11, 0.04, 0.35, 0.5, 0.65, 0.96, 0.89, 0.97, 1))
        assertEasing(Easing.EASE_IN_OUT_ELASTIC, numbers(0, 0, 0.01, -0.02, -0.06, 0.5, 1.06, 1.02, 0.99, 1, 1))
        assertEasing(Easing.EASE_IN_OUT_OLD, numbers(0, 0, 0.03, 0.11, 0.26, 0.5, 0.74, 0.89, 0.97, 1, 1))
        assertEasing(Easing.EASE_IN_OUT_QUAD, numbers(0, 0.02, 0.08, 0.18, 0.32, 0.5, 0.68, 0.82, 0.92, 0.98, 1))
        assertEasing(Easing.EASE_IN_QUAD, numbers(0, 0.01, 0.04, 0.09, 0.16, 0.25, 0.36, 0.49, 0.64, 0.81, 1))
        assertEasing(Easing.EASE_OUT, numbers(0, 0.16, 0.31, 0.45, 0.57, 0.68, 0.79, 0.87, 0.94, 0.98, 1))
        assertEasing(Easing.EASE_OUT_BACK, numbers(0, 0.41, 0.71, 0.91, 1.03, 1.09, 1.1, 1.08, 1.05, 1.01, 1))
        assertEasing(Easing.EASE_OUT_BOUNCE, numbers(0, 0.08, 0.3, 0.68, 0.91, 0.77, 0.77, 0.93, 0.94, 0.99, 1))
        assertEasing(Easing.EASE_OUT_ELASTIC, numbers(0, 1.25, 1.12, 0.88, 1.03, 1.02, 0.98, 1, 1, 1, 1))
        assertEasing(Easing.EASE_OUT_IN_BACK, numbers(0, 0.35, 0.51, 0.55, 0.52, 0.5, 0.48, 0.45, 0.49, 0.65, 1))
        assertEasing(Easing.EASE_OUT_IN_BOUNCE, numbers(0, 0.15, 0.46, 0.39, 0.47, 0.5, 0.53, 0.61, 0.54, 0.85, 1))
        assertEasing(Easing.EASE_OUT_IN_ELASTIC, numbers(0, 0.56, 0.52, 0.49, 0.5, 0.5, 0.5, 0.51, 0.48, 0.44, 1))
        assertEasing(Easing.EASE_OUT_IN_OLD, numbers(0, 0.24, 0.39, 0.47, 0.5, 0.5, 0.5, 0.53, 0.61, 0.76, 1))
        assertEasing(Easing.EASE_OUT_OLD, numbers(0, 0.27, 0.49, 0.66, 0.78, 0.88, 0.94, 0.97, 0.99, 1, 1))
        assertEasing(Easing.EASE_OUT_QUAD, numbers(0, 0.19, 0.36, 0.51, 0.64, 0.75, 0.84, 0.91, 0.96, 0.99, 1))
        assertEasing(Easing.EASE_SINE, numbers(0, 0.16, 0.31, 0.45, 0.59, 0.71, 0.81, 0.89, 0.95, 0.99, 1))
        assertEasing(Easing.LINEAR, numbers(0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1))
        assertEasing(Easing.SMOOTH, numbers(0, 0.03, 0.1, 0.22, 0.35, 0.5, 0.65, 0.78, 0.9, 0.97, 1))
    }

    fun Easing.strSamples(): String = getSamples().str()
    fun Easing.getSamples(): DoubleArrayList = (0..10).mapDouble { this(it.toDouble() / 10.0) }
    fun DoubleArrayList.str() = "[" + this.joinToString(", ") { it.toStringDecimal(2, true) } + "]"
}
