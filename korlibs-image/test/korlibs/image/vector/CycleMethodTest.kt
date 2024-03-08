package korlibs.image.vector

import korlibs.math.roundDecimalPlaces
import korlibs.number.*
import kotlin.test.Test
import kotlin.test.assertEquals

class CycleMethodTest {
    @Test
    fun test() {
        val values = listOf(-3.1, -2.5, -2.0, -1.9, -1.1, -1.0, -0.1, 0.0, 0.1, 1.0, 1.1, 1.9, 2.0, 2.5, 3.1)

        assertEquals(
            """
                NO_CYCLE: [-3.1, -2.5, -2, -1.9, -1.1, -1, -0.1, 0, 0.1, 1, 1.1, 1.9, 2, 2.5, 3.1]
                NO_CYCLE_CLAMP: [0, 0, 0, 0, 0, 0, 0, 0, 0.1, 1, 1, 1, 1, 1, 1]
                REPEAT: [0.9, 0.5, 0, 0.1, 0.9, 0, 0.9, 0, 0.1, 0, 0.1, 0.9, 0, 0.5, 0.1]
                REFLECT: [0.9, 0.5, 0, 0.1, 0.9, 1, 0.1, 0, 0.1, 1, 0.9, 0.1, 0, 0.5, 0.9]
            """.trimIndent(),
            CycleMethod.values().joinToString("\n") { method ->
                "$method: ${values.map { method.apply(it).roundDecimalPlaces(3).niceStr }}"
            }
        )
    }
}
