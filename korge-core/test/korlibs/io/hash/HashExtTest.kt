package korlibs.io.hash

import korlibs.io.async.suspendTest
import korlibs.io.file.std.MemoryVfsMix
import korlibs.io.stream.MemorySyncStream
import kotlin.test.Test
import kotlin.test.assertEquals

class HashExtTest {
    @Test
    fun testMd5() = suspendTest {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", MemorySyncStream(byteArrayOf()).md5().hexLower)
        assertEquals("7d793037a0760186574b0282f2f435e7", MemoryVfsMix("hello" to "world")["hello"].md5().hexLower)
        assertEquals("c90d6a50d0f66bef0ff687d066d7f144", MemoryVfsMix("hello" to ByteArray(0x31234) { '7'.toByte() })["hello"].md5().hexLower)
    }
}
