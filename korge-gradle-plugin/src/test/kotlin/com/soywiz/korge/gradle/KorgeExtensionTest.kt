package com.soywiz.korge.gradle

import kotlin.test.*

class KorgeExtensionTest {
    @Test
    fun testIsValidId() {
        assertTrue { KorgeExtension.isIdValid("com") }
        assertTrue { KorgeExtension.isIdValid("com.test") }
        assertTrue { KorgeExtension.isIdValid("com.test.demo") }
        assertTrue { KorgeExtension.isIdValid("com.test.n2048") }
        assertFalse { KorgeExtension.isIdValid("com.test.2048") }
    }

    @Test
    fun testVerifyId() {
        KorgeExtension.verifyId("com.test.n2048")
        val ex = assertFailsWith<javax.naming.NamingException> { KorgeExtension.verifyId("com.test.2048") }
        assertTrue { ex.message!!.contains("com.test.2048") }
    }
}
