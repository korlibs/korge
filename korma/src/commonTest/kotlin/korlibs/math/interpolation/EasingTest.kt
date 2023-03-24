package korlibs.math.interpolation

import korlibs.datastructure.DoubleArrayList
import korlibs.datastructure.mapDouble
import korlibs.math.test.toStringDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class EasingTest {
    @Test
    fun testAll() {
        assertEquals(
            """
                cubic-bezier(0.86, 0.13, 0.22, 0.84): [0, 0.019, 0.049, 0.097, 0.179, 0.365, 0.731, 0.866, 0.934, 0.974, 1]
                ease: [0, 0.096, 0.297, 0.514, 0.682, 0.802, 0.886, 0.941, 0.975, 0.994, 1]
                ease-clamp-end: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1]
                ease-clamp-middle: [0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1]
                ease-clamp-start: [0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]
                ease-in: [0, 0.017, 0.062, 0.129, 0.214, 0.316, 0.429, 0.554, 0.692, 0.838, 1]
                ease-in-back: [0, -0.014, -0.046, -0.08, -0.099, -0.088, -0.029, 0.093, 0.294, 0.591, 1]
                ease-in-bounce: [0, 0.012, 0.06, 0.069, 0.228, 0.234, 0.09, 0.319, 0.698, 0.924, 1]
                ease-in-elastic: [0, 0.002, -0.002, -0.004, 0.016, -0.016, -0.031, 0.125, -0.125, -0.25, 1]
                ease-in-old: [0, 0.001, 0.008, 0.027, 0.064, 0.125, 0.216, 0.343, 0.512, 0.729, 1]
                ease-in-out: [0, 0.019, 0.081, 0.187, 0.332, 0.5, 0.668, 0.813, 0.919, 0.981, 1]
                ease-in-out-back: [0, -0.023, -0.05, -0.015, 0.147, 0.5, 0.853, 1.015, 1.05, 1.023, 1]
                ease-in-out-bounce: [0, 0.03, 0.114, 0.045, 0.349, 0.5, 0.651, 0.955, 0.886, 0.97, 1]
                ease-in-out-elastic: [0, -0.001, 0.008, -0.016, -0.063, 0.5, 1.062, 1.016, 0.992, 1.001, 1]
                ease-in-out-old: [0, 0.004, 0.032, 0.108, 0.256, 0.5, 0.744, 0.892, 0.968, 0.996, 1]
                ease-in-out-quad: [0, 0.02, 0.08, 0.18, 0.32, 0.5, 0.68, 0.82, 0.92, 0.98, 1]
                ease-in-quad: [0, 0.01, 0.04, 0.09, 0.16, 0.25, 0.36, 0.49, 0.64, 0.81, 1]
                ease-out: [0, 0.162, 0.308, 0.446, 0.571, 0.684, 0.786, 0.871, 0.938, 0.983, 1]
                ease-out-back: [0, 0.409, 0.706, 0.907, 1.029, 1.088, 1.099, 1.08, 1.046, 1.014, 1]
                ease-out-bounce: [0, 0.076, 0.303, 0.681, 0.91, 0.766, 0.772, 0.931, 0.94, 0.988, 1]
                ease-out-elastic: [0, 1.25, 1.125, 0.875, 1.031, 1.016, 0.984, 1.004, 1.002, 0.998, 1]
                ease-out-in-back: [0, 0.353, 0.515, 0.55, 0.523, 0.5, 0.477, 0.45, 0.485, 0.647, 1]
                ease-out-in-bounce: [0, 0.151, 0.455, 0.386, 0.47, 0.5, 0.53, 0.614, 0.545, 0.849, 1]
                ease-out-in-elastic: [0, 0.562, 0.516, 0.492, 0.501, 0.5, 0.499, 0.508, 0.484, 0.437, 1]
                ease-out-in-old: [0, 0.244, 0.392, 0.468, 0.496, 0.5, 0.504, 0.532, 0.608, 0.756, 1]
                ease-out-old: [0, 0.271, 0.488, 0.657, 0.784, 0.875, 0.936, 0.973, 0.992, 0.999, 1]
                ease-out-quad: [0, 0.19, 0.36, 0.51, 0.64, 0.75, 0.84, 0.91, 0.96, 0.99, 1]
                ease-sine: [0, 0.156, 0.309, 0.454, 0.588, 0.707, 0.809, 0.891, 0.951, 0.988, 1]
                linear: [0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1]
                smooth: [0, 0.028, 0.104, 0.216, 0.352, 0.5, 0.648, 0.784, 0.896, 0.972, 1]
            """.trimIndent(),
            (Easing.ALL_LIST + Easing.cubic(.86, .13, .22, .84)).sortedBy { it.toString() }.joinToString("\n") {
                "$it: ${it.strSamples()}"
            }
        )
    }

    fun Easing.strSamples(): String = getSamples().str()
    fun Easing.getSamples(): DoubleArrayList = (0..10).mapDouble { this(it.toDouble() / 10.0) }
    fun DoubleArrayList.str() = "[" + this.joinToString(", ") { it.toStringDecimal(3, true) } + "]"
}