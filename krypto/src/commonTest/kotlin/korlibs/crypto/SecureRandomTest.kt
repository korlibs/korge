package korlibs.crypto

import kotlin.test.Test
import kotlin.test.Ignore
import kotlin.test.assertNotEquals

class SecureRandomTest {
    @Test
    fun test() {
        SecureRandom.addSeed(byteArrayOf(1, 2, 3)) // This shouldn't reduce entropy
        println(SecureRandom.nextBytes(15).toList())
        println(SecureRandom.nextBytes(15).toList())
        assertNotEquals(SecureRandom.nextBytes(16).toList(), SecureRandom.nextBytes(16).toList())
        assertNotEquals(SecureRandom.nextBytes(16).toList(), SecureRandom.nextBytes(16).toList())
        println(SecureRandom.nextBytes(15).toList())
    }
}
