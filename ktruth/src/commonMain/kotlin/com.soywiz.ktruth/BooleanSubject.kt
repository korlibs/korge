package com.soywiz.ktruth

import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BooleanSubject(val subject: Boolean) {
    fun isTrue() {
        assertTrue(subject)
    }
    fun isFalse() {
        assertFalse(subject)
    }
}
