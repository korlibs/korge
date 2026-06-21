package org.korge.kproject.util

import org.korge.kproject.internal.*
import kotlin.test.*

class JsonTest {
    @Test
    fun test() {
        assertEquals(1, "{\"a\": 1}".fromJson().dyn["a"].int)
        assertEquals("{\"a\":1}", mapOf("a" to 1).toJson())
    }
}
