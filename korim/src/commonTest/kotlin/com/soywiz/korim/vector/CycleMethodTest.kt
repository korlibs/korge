package com.soywiz.korim.vector

import com.soywiz.korma.math.*
import kotlin.test.*

class CycleMethodTest {
    @Test
    fun test() {
        val values = listOf(-3.1, -2.5, -2.0, -1.9, -1.1, -1.0, -0.1, 0.0, 0.1, 1.0, 1.1, 1.9, 2.0, 2.5, 3.1)

        assertEquals(
            """
                NO_CYCLE: [-3.1, -2.5, -2.0, -1.9, -1.1, -1.0, -0.1, 0.0, 0.1, 1.0, 1.1, 1.9, 2.0, 2.5, 3.1]
                NO_CYCLE_CLAMP: [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.1, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0]
                REPEAT: [0.9, 0.5, 0.0, 0.1, 0.9, 0.0, 0.9, 0.0, 0.1, 0.0, 0.1, 0.9, 0.0, 0.5, 0.1]
                REFLECT: [0.9, 0.5, 0.0, 0.1, 0.9, 1.0, 0.1, 0.0, 0.1, 1.0, 0.9, 0.1, 0.0, 0.5, 0.9]
            """.trimIndent(),
            CycleMethod.values().map { method ->
                "$method: ${values.map { method.apply(it).roundDecimalPlaces(3) }}"
            }.joinToString("\n")
        )
    }
}
