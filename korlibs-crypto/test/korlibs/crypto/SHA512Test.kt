package korlibs.crypto

import korlibs.encoding.ASCII
import kotlin.test.Test
import kotlin.test.assertEquals

class SHA512Test {
    @Test
    fun testRawEmpty() {
        val out = ByteArray(64)
        val sha = SHA512()
        sha.reset()
        sha.update(byteArrayOf(), 0, 0)
        sha.digestOut(out)
        assertEquals(
            "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
            Hash(out).hexLower
        )
    }

    @Test
    fun testEmpty() {
        assertEquals(
            "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
            SHA512.digest("".encodeToByteArray()).hexLower
        )
    }

    @Test
    fun testA() {
        assertEquals(
            "1f40fc92da241694750979ee6cf582f2d5d7d28e18335de05abc54d0560e0f5302860c652bf08d560252aa5e74210546f369fbbbce8c12cfc7957b2652fe9a75",
            SHA512.digest("a".encodeToByteArray()).hexLower
        )
    }

    @Test
    fun test2() {
        assertEquals("01d35c10c6c38c2dcf48f7eebb3235fb5ad74a65ec4cd016e2354c637a8fb49b695ef3c1d6f7ae4cd74d78cc9c9bcac9d4f23a73019998a7f73038a5c9b2dbde", SHA512.digest(ByteArray(64) { 'a'.toByte() }).hex)
        assertEquals("01d35c10c6c38c2dcf48f7eebb3235fb5ad74a65ec4cd016e2354c637a8fb49b695ef3c1d6f7ae4cd74d78cc9c9bcac9d4f23a73019998a7f73038a5c9b2dbde", ByteArray(64) { 'a'.toByte() }.hash(SHA512).hex)
        assertEquals("01d35c10c6c38c2dcf48f7eebb3235fb5ad74a65ec4cd016e2354c637a8fb49b695ef3c1d6f7ae4cd74d78cc9c9bcac9d4f23a73019998a7f73038a5c9b2dbde", ByteArray(64) { 'a'.toByte() }.sha512().hex)
    }

    @Test
    fun test3() {
        assertEquals("07e547d9586f6a73f73fbac0435ed76951218fb7d0c8d788a309d785436bbb642e93a252a954f23912547d1e8a3b5ed6e1bfd7097821233fa0538f3db854fee6", SHA512.digest(ASCII("The quick brown fox jumps over the lazy dog")).hex)
        assertEquals("753c07c6245748d0002359efb1018687880219eb5f10b0015362ba80679589b1679e87dfdba276b2fbcbf8e48377270ddfe99c9ba7b4cbc6763ecd55f9fb1b7d", SHA512.digest(ASCII("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaab")).hex)
    }

    @Test
    fun test4() {
        assertEquals("c82fb0cc171015e9b1c95c912b17b30cfa76c637260cca09e78fdaf1d4dbe9b2d08501b7af8b48cb8bdb02d6e9ca2662971c0f23fd8c5c02c33e6a8f72df5028", "a".repeat(1311).encodeToByteArray().sha512().hex)
    }

    @Test
    fun test5() {
        for (n in 0 until 257) {
            ByteArray(n) { n.toByte() }.sha512().hex
        }
    }
}
