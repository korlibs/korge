package korlibs.io.lang

import korlibs.memory.*
import korlibs.io.async.suspendTest
import korlibs.io.file.std.MemoryVfsMix
import korlibs.platform.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PropertiesTest {
    @Test
    fun test() {
        val value = "test"
        val key = "my.custom.property"
        SystemProperties[key] = value
        assertEquals(value, SystemProperties[key])
        SystemProperties.remove(key)
        assertEquals(null, SystemProperties[key])
    }

    @Test
    fun testJvm() {
        if (Platform.isJvm) {
            assertNotNull(SystemProperties["java.version"])
        }
    }

    @Test
    fun testParse() = suspendTest {
        assertEquals("10", Properties.parseString("my.property=10")["my.property"])

        val vfs = MemoryVfsMix("file.properties" to """
            my.property = 10
            hello = world # test
            
            double.equal = 1=2
            
            # this is a comment
        """.trimIndent())

        assertEquals(mapOf(
            "my.property" to "10",
            "hello" to "world",
            "double.equal" to "1=2",
        ), vfs["file.properties"].readProperties().getAll())
    }
}
