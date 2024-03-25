package korlibs.io.hash

import korlibs.crypto.*
import kotlin.test.*

class HashJvmExtTest {
    @Test
    fun testInputStream() {
        byteArrayOf(1, 2, 3).also {
            assertEquals(it.md5(), it.inputStream().hash(MD5))
        }
        ByteArray(0x30000) { (it * 318083817907).toByte() }.also {
            assertEquals(it.sha1(), it.inputStream().hash(SHA1))
        }
    }
}
