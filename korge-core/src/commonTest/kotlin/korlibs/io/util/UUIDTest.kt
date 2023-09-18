package korlibs.io.util

import korlibs.memory.*
import korlibs.platform.*
import kotlin.test.Test
import kotlin.test.assertEquals

class UUIDTest {
    @Test
    fun testRandom() {
        if (Platform.isAndroid) return // Do not test in android since Process is not mocked
        val random = UUID.randomUUID()
        assertEquals(4, random.version)
        assertEquals(1, random.variant)
        //assertEquals("00000000-0000-0000-0000-000000000000", UUID.randomUUID().toString())
    }

    @Test
    fun testEqualsAndHash() {
        assertEquals(UUID("00000000-0000-0000-0000-000000000000"), UUID.NIL)
        assertEquals(UUID("00000000-0000-0000-0000-000000000000"), UUID("00000000-0000-0000-0000-000000000000"))
        assertEquals(UUID("fedcba98-7654-3210-fedc-ba9876543210"), UUID("FEDCBA98-7654-3210-FEDC-BA9876543210"))
        assertEquals(UUID("fedcba98-7654-3210-fedc-ba9876543210").hashCode(), UUID("FEDCBA98-7654-3210-FEDC-BA9876543210").hashCode())
    }

    @Test
    fun testNil() {
        assertEquals("00000000-0000-0000-0000-000000000000", UUID("00000000-0000-0000-0000-000000000000").toString())
        assertEquals("00000000-0000-0000-0000-000000000000", UUID.NIL.toString())
    }

    @Test
    fun testParse() {
        assertEquals("01234567-89ab-cdef-0123-456789abcdef", UUID("01234567-89ab-cdef-0123-456789abcdef").toString())
        assertEquals("01234567-89ab-cdef-0123-456789abcdef", UUID("01234567-89AB-CDEF-0123-456789ABCDEF").toString())
        assertEquals("fedcba98-7654-3210-fedc-ba9876543210", UUID("fedcba98-7654-3210-fedc-ba9876543210").toString())
        assertEquals("fedcba98-7654-3210-fedc-ba9876543210", UUID("FEDCBA98-7654-3210-FEDC-BA9876543210").toString())
    }
}
