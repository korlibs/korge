package com.soywiz.ktruth

import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BooleanSubject(val actual: Boolean) {
    fun isTrue() {
        assertTrue(actual)
    }
    fun isFalse() {
        assertFalse(actual)
    }
}
