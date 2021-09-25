package com.soywiz.korge.gradle

import com.soywiz.korge.gradle.util.*
import kotlin.test.*

class NativeExtTest {
    @Test
    fun test() {
        assertTrue(SemVer("1.6.0-M1") >= SemVer("1.6.0"))
        assertFalse(SemVer("1.5.1") >= SemVer("1.6.0"))
    }
}
