package com.soywiz.kproject.util

import kotlin.test.*

class VersionComparerTest {
    @Test
    fun test() {
        assertEquals(true, "1.0.0".version == "1.0.0".version)
        assertEquals(true, "1.0.0".version == "v1.0.0".version)
        assertEquals(true, "1.1.0".version > "v1.0.0".version)
        assertEquals(true, "v1.0.0.1".version > "1.0.0".version)
        assertEquals(true, "1.0.0".version > "1.0.0-beta1".version)
        assertEquals(true, "1.0.0-beta2".version > "1.0.0-beta".version)
        assertEquals(true, "v1.0.0-beta".version > "v1.0.0-alpha".version)
        assertEquals(true, "v1.0.0-rc".version > "1.0.0-beta".version)
        assertEquals(true, "1.0.0-rc2".version > "v1.0.0-rc".version)
    }
}
