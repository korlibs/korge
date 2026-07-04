package korlibs.korge.gradle

import korlibs.korge.gradle.util.SemVer
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NativeExtTest {
    @Test
    fun test() {
        assertTrue(SemVer("1.6.0-M1") >= SemVer("1.6.0"))
        assertFalse(SemVer("1.5.1") >= SemVer("1.6.0"))
    }
}
