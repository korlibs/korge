package korlibs.io.util

import kotlin.test.Test
import kotlin.test.assertEquals

class GlobTest {
    @Test
    fun test() {
        assertEquals(true, Glob("*.txt", full = true) matches "hello.txt")
        assertEquals(false, Glob("*.txt", full = true) matches "hello.bin")
        assertEquals(false, Glob("*.txt", full = true) matches "hello")
    }
}