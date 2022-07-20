package org.luaj.vm2

import org.luaj.vm2.internal.*
import org.luaj.vm2.lib.common.*
import kotlin.test.*

class StringTest {

    init {
        CommonPlatform.standardGlobals()
    }

    /*
    @Test
    fun testToInputStream() {
        val str = LuaString.valueOf("Hello")

        var `is` = str.toInputStream()

        assertEquals('H', `is`.read().toChar())
        assertEquals('e', `is`.read().toChar())
        assertEquals(2, `is`.skip(2))
        assertEquals('o', `is`.read().toChar())
        assertEquals(-1, `is`.read())

        assertTrue(`is`.markSupported())

        `is`.reset()

        assertEquals('H', `is`.read().toChar())
        `is`.mark(4)

        assertEquals('e', `is`.read().toChar())
        `is`.reset()
        assertEquals('e', `is`.read().toChar())

        val substr = str.substring(1, 4)
        assertEquals(3, substr.length())

        `is`.close()
        `is` = substr.toInputStream()

        assertEquals('e', `is`.read().toChar())
        assertEquals('l', `is`.read().toChar())
        assertEquals('l', `is`.read().toChar())
        assertEquals(-1, `is`.read())

        `is` = substr.toInputStream()
        `is`.reset()

        assertEquals('e', `is`.read().toChar())
    }
    */

    private fun userFriendly(s: String): String {
        val sb = StringBuilder()
        var i = 0
        val n = s.length
        while (i < n) {
            val c = s[i].toInt()
            if (c < ' '.toInt() || c >= 0x80) {
                sb.append("\\u" + (0x10000 + c).toHexString().substring(1))
            } else {
                sb.append(c.toChar())
            }
            i++
        }
        return sb.toString()
    }

    @Test
    fun testUtf820482051() {
        val i = 2048
        val c = charArrayOf((i + 0).toChar(), (i + 1).toChar(), (i + 2).toChar(), (i + 3).toChar())
        val before = c.concatToString() + " " + i + "-" + (i + 4)
        val ls = LuaString.valueOf(before)
        val after = ls.tojstring()
        assertEquals(userFriendly(before), userFriendly(after))
    }

    @Test
    fun testUtf8() {
        var i = 4
        while (i < 0xffff) {
            val c = charArrayOf((i + 0).toChar(), (i + 1).toChar(), (i + 2).toChar(), (i + 3).toChar())
            val before = c.concatToString() + " " + i + "-" + (i + 4)
            val ls = LuaString.valueOf(before)
            val after = ls.tojstring()
            assertEquals(userFriendly(before), userFriendly(after))
            i += 4
        }
        val c = charArrayOf(1.toChar(), 2.toChar(), 3.toChar())
        val before = c.concatToString() + " 1-3"
        val ls = LuaString.valueOf(before)
        val after = ls.tojstring()
        assertEquals(userFriendly(before), userFriendly(after))
    }

    @Test
    fun testSpotCheckUtf8() {
        val bytes = byteArrayOf(
            194.toByte(),
            160.toByte(),
            194.toByte(),
            161.toByte(),
            194.toByte(),
            162.toByte(),
            194.toByte(),
            163.toByte(),
            194.toByte(),
            164.toByte()
        )
        val expected = bytes.decodeToString()
        val actual = LuaString.valueOf(bytes).tojstring()
        val d = actual.toCharArray()
        assertEquals(160, d[0].toInt())
        assertEquals(161, d[1].toInt())
        assertEquals(162, d[2].toInt())
        assertEquals(163, d[3].toInt())
        assertEquals(164, d[4].toInt())
        assertEquals(expected, actual)
    }

    @Test
    fun testNullTerminated() {
        val c = charArrayOf('a', 'b', 'c', '\u0000', 'd', 'e', 'f')
        val before = c.concatToString()
        val ls = LuaString.valueOf(before)
        val after = ls.tojstring()
        assertEquals(userFriendly("abc\u0000def"), userFriendly(after))
    }

    @Test
    fun testRecentStringsCacheDifferentHashcodes() {
        val abc = byteArrayOf('a'.toByte(), 'b'.toByte(), 'c'.toByte())
        val xyz = byteArrayOf('x'.toByte(), 'y'.toByte(), 'z'.toByte())
        val abc1 = LuaString.valueOf(abc)
        val xyz1 = LuaString.valueOf(xyz)
        val abc2 = LuaString.valueOf(abc)
        val xyz2 = LuaString.valueOf(xyz)
        val mod = LuaString.RECENT_STRINGS_CACHE_SIZE
        assertTrue(abc1.hashCode() % mod != xyz1.hashCode() % mod)
        assertSame(abc1, abc2)
        assertSame(xyz1, xyz2)
    }

    @Test
    fun testRecentStringsCacheHashCollisionCacheHit() {
        val abc = byteArrayOf('a'.toByte(), 'b'.toByte(), 'c'.toByte())
        val lyz = byteArrayOf('l'.toByte(), 'y'.toByte(), 'z'.toByte())  // chosen to have hash collision with 'abc'
        val abc1 = LuaString.valueOf(abc)
        val abc2 = LuaString.valueOf(abc) // in cache: 'abc'
        val lyz1 = LuaString.valueOf(lyz)
        val lyz2 = LuaString.valueOf(lyz) // in cache: 'lyz'
        val mod = LuaString.RECENT_STRINGS_CACHE_SIZE
        assertEquals(abc1.hashCode() % mod, lyz1.hashCode() % mod)
        assertNotSame(abc1, lyz1)
        assertFalse(abc1 == lyz1)
        assertSame(abc1, abc2)
        assertSame(lyz1, lyz2)
    }

    @Test
    fun testRecentStringsCacheHashCollisionCacheMiss() {
        val abc = byteArrayOf('a'.toByte(), 'b'.toByte(), 'c'.toByte())
        val lyz = byteArrayOf('l'.toByte(), 'y'.toByte(), 'z'.toByte())  // chosen to have hash collision with 'abc'
        val abc1 = LuaString.valueOf(abc)
        val lyz1 = LuaString.valueOf(lyz) // in cache: 'abc'
        val abc2 = LuaString.valueOf(abc) // in cache: 'lyz'
        val lyz2 = LuaString.valueOf(lyz) // in cache: 'abc'
        val mod = LuaString.RECENT_STRINGS_CACHE_SIZE
        assertEquals(abc1.hashCode() % mod, lyz1.hashCode() % mod)
        assertNotSame(abc1, lyz1)
        assertFalse(abc1 == lyz1)
        assertNotSame(abc1, abc2)
        assertNotSame(lyz1, lyz2)
    }

    @Test
    fun testRecentStringsLongStrings() {
        val abc = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".encodeToByteArray()
        assertTrue(abc.size > LuaString.RECENT_STRINGS_MAX_LENGTH)
        val abc1 = LuaString.Companion.valueOf(abc)
        val abc2 = LuaString.Companion.valueOf(abc)
        assertNotSame(abc1, abc2)
    }

    @Test
    fun testRecentStringsUsingJavaStrings() {
        val abc = "abc"
        val lyz = "lyz"  // chosen to have hash collision with 'abc'
        val xyz = "xyz"

        val abc1 = LuaString.valueOf(abc)
        val abc2 = LuaString.valueOf(abc)
        val lyz1 = LuaString.valueOf(lyz)
        val lyz2 = LuaString.valueOf(lyz)
        val xyz1 = LuaString.valueOf(xyz)
        val xyz2 = LuaString.valueOf(xyz)
        val mod = LuaString.RECENT_STRINGS_CACHE_SIZE
        assertEquals(abc1.hashCode() % mod, lyz1.hashCode() % mod)
        assertFalse(abc1.hashCode() % mod == xyz1.hashCode() % mod)
        assertSame(abc1, abc2)
        assertSame(lyz1, lyz2)
        assertSame(xyz1, xyz2)

        val abc3 = LuaString.valueOf(abc)
        val lyz3 = LuaString.valueOf(lyz)
        val xyz3 = LuaString.valueOf(xyz)

        val abc4 = LuaString.valueOf(abc)
        val lyz4 = LuaString.valueOf(lyz)
        val xyz4 = LuaString.valueOf(xyz)
        assertNotSame(abc3, abc4)  // because of hash collision
        assertNotSame(lyz3, lyz4)  // because of hash collision
        assertSame(xyz3, xyz4)  // because hashes do not collide
    }

    @Test
    fun testLongSubstringGetsOldBacking() {
        val src = LuaString.valueOf("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
        val sub1 = src.substring(10, 40)
        assertSame(src.m_bytes, sub1.m_bytes)
        assertEquals(sub1.m_offset, 10)
        assertEquals(sub1.m_length, 30)
    }

    @Test
    fun testShortSubstringGetsNewBacking() {
        val src = LuaString.valueOf("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
        val sub1 = src.substring(10, 20)
        val sub2 = src.substring(10, 20)
        assertEquals(sub1.m_offset, 0)
        assertEquals(sub1.m_length, 10)
        assertSame(sub1, sub2)
        assertFalse(src.m_bytes == sub1.m_bytes)
    }

    @Test
    fun testShortSubstringOfVeryLongStringGetsNewBacking() {
        val src = LuaString.valueOf(
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        )
        val sub1 = src.substring(10, 50)
        val sub2 = src.substring(10, 50)
        assertEquals(sub1.m_offset, 0)
        assertEquals(sub1.m_length, 40)
        assertFalse(sub1 === sub2)
        assertFalse(src.m_bytes == sub1.m_bytes)
    }

    @Test
    fun testIndexOfByteInSubstring() {
        val str = LuaString.valueOf("abcdef:ghi")
        val sub = str.substring(2, 10)
        assertEquals(10, str.m_length)
        assertEquals(8, sub.m_length)
        assertEquals(0, str.m_offset)
        assertEquals(2, sub.m_offset)

        assertEquals(6, str.indexOf(':'.toByte(), 0))
        assertEquals(6, str.indexOf(':'.toByte(), 2))
        assertEquals(6, str.indexOf(':'.toByte(), 6))
        assertEquals(-1, str.indexOf(':'.toByte(), 7))
        assertEquals(-1, str.indexOf(':'.toByte(), 9))
        assertEquals(9, str.indexOf('i'.toByte(), 0))
        assertEquals(9, str.indexOf('i'.toByte(), 2))
        assertEquals(9, str.indexOf('i'.toByte(), 9))
        assertEquals(-1, str.indexOf('z'.toByte(), 0))
        assertEquals(-1, str.indexOf('z'.toByte(), 2))
        assertEquals(-1, str.indexOf('z'.toByte(), 9))

        assertEquals(4, sub.indexOf(':'.toByte(), 0))
        assertEquals(4, sub.indexOf(':'.toByte(), 2))
        assertEquals(4, sub.indexOf(':'.toByte(), 4))
        assertEquals(-1, sub.indexOf(':'.toByte(), 5))
        assertEquals(-1, sub.indexOf(':'.toByte(), 7))
        assertEquals(7, sub.indexOf('i'.toByte(), 0))
        assertEquals(7, sub.indexOf('i'.toByte(), 2))
        assertEquals(7, sub.indexOf('i'.toByte(), 7))
        assertEquals(-1, sub.indexOf('z'.toByte(), 0))
        assertEquals(-1, sub.indexOf('z'.toByte(), 2))
        assertEquals(-1, sub.indexOf('z'.toByte(), 7))
    }

    @Test
    fun testIndexOfPatternInSubstring() {
        val str = LuaString.valueOf("abcdef:ghi")
        val sub = str.substring(2, 10)
        assertEquals(10, str.m_length)
        assertEquals(8, sub.m_length)
        assertEquals(0, str.m_offset)
        assertEquals(2, sub.m_offset)

        val pat = LuaString.valueOf(":")
        val i = LuaString.valueOf("i")
        val xyz = LuaString.valueOf("xyz")

        assertEquals(6, str.indexOf(pat, 0))
        assertEquals(6, str.indexOf(pat, 2))
        assertEquals(6, str.indexOf(pat, 6))
        assertEquals(-1, str.indexOf(pat, 7))
        assertEquals(-1, str.indexOf(pat, 9))
        assertEquals(9, str.indexOf(i, 0))
        assertEquals(9, str.indexOf(i, 2))
        assertEquals(9, str.indexOf(i, 9))
        assertEquals(-1, str.indexOf(xyz, 0))
        assertEquals(-1, str.indexOf(xyz, 2))
        assertEquals(-1, str.indexOf(xyz, 9))

        assertEquals(4, sub.indexOf(pat, 0))
        assertEquals(4, sub.indexOf(pat, 2))
        assertEquals(4, sub.indexOf(pat, 4))
        assertEquals(-1, sub.indexOf(pat, 5))
        assertEquals(-1, sub.indexOf(pat, 7))
        assertEquals(7, sub.indexOf(i, 0))
        assertEquals(7, sub.indexOf(i, 2))
        assertEquals(7, sub.indexOf(i, 7))
        assertEquals(-1, sub.indexOf(xyz, 0))
        assertEquals(-1, sub.indexOf(xyz, 2))
        assertEquals(-1, sub.indexOf(xyz, 7))
    }

    @Test
    fun testLastIndexOfPatternInSubstring() {
        val str = LuaString.valueOf("abcdef:ghi")
        val sub = str.substring(2, 10)
        assertEquals(10, str.m_length)
        assertEquals(8, sub.m_length)
        assertEquals(0, str.m_offset)
        assertEquals(2, sub.m_offset)

        val pat = LuaString.valueOf(":")
        val i = LuaString.valueOf("i")
        val xyz = LuaString.valueOf("xyz")

        assertEquals(6, str.lastIndexOf(pat))
        assertEquals(9, str.lastIndexOf(i))
        assertEquals(-1, str.lastIndexOf(xyz))

        assertEquals(4, sub.lastIndexOf(pat))
        assertEquals(7, sub.lastIndexOf(i))
        assertEquals(-1, sub.lastIndexOf(xyz))
    }

    @Test
    fun testIndexOfAnyInSubstring() {
        val str = LuaString.valueOf("abcdef:ghi")
        val sub = str.substring(2, 10)
        assertEquals(10, str.m_length)
        assertEquals(8, sub.m_length)
        assertEquals(0, str.m_offset)
        assertEquals(2, sub.m_offset)

        val ghi = LuaString.valueOf("ghi")
        val ihg = LuaString.valueOf("ihg")
        val ijk = LuaString.valueOf("ijk")
        val kji = LuaString.valueOf("kji")
        val xyz = LuaString.valueOf("xyz")
        val ABCdEFGHIJKL = LuaString.valueOf("ABCdEFGHIJKL")
        val EFGHIJKL = ABCdEFGHIJKL.substring(4, 12)
        val CdEFGHIJ = ABCdEFGHIJKL.substring(2, 10)
        assertEquals(4, EFGHIJKL.m_offset)
        assertEquals(2, CdEFGHIJ.m_offset)

        assertEquals(7, str.indexOfAny(ghi))
        assertEquals(7, str.indexOfAny(ihg))
        assertEquals(9, str.indexOfAny(ijk))
        assertEquals(9, str.indexOfAny(kji))
        assertEquals(-1, str.indexOfAny(xyz))
        assertEquals(3, str.indexOfAny(CdEFGHIJ))
        assertEquals(-1, str.indexOfAny(EFGHIJKL))

        assertEquals(5, sub.indexOfAny(ghi))
        assertEquals(5, sub.indexOfAny(ihg))
        assertEquals(7, sub.indexOfAny(ijk))
        assertEquals(7, sub.indexOfAny(kji))
        assertEquals(-1, sub.indexOfAny(xyz))
        assertEquals(1, sub.indexOfAny(CdEFGHIJ))
        assertEquals(-1, sub.indexOfAny(EFGHIJKL))
    }

    @Test
    fun testMatchShortPatterns() {
        val args = arrayOf<LuaValue>(LuaString.valueOf("%bxy"))
        val vvv = LuaString.valueOf("")

        val a = LuaString.valueOf("a")
        val ax = LuaString.valueOf("ax")
        val axb = LuaString.valueOf("axb")
        val axby = LuaString.valueOf("axby")
        val xbya = LuaString.valueOf("xbya")
        val bya = LuaString.valueOf("bya")
        val xby = LuaString.valueOf("xby")
        val axbya = LuaString.valueOf("axbya")
        val nil = LuaValue.NIL

        assertEquals(nil, vvv.invokemethod("match", args))
        assertEquals(nil, a.invokemethod("match", args))
        assertEquals(nil, ax.invokemethod("match", args))
        assertEquals(nil, axb.invokemethod("match", args))
        assertEquals(xby, axby.invokemethod("match", args))
        assertEquals(xby, xbya.invokemethod("match", args))
        assertEquals(nil, bya.invokemethod("match", args))
        assertEquals(xby, xby.invokemethod("match", args))
        assertEquals(xby, axbya.invokemethod("match", args))
        assertEquals(xby, axbya.substring(0, 4).invokemethod("match", args))
        assertEquals(nil, axbya.substring(0, 3).invokemethod("match", args))
        assertEquals(xby, axbya.substring(1, 5).invokemethod("match", args))
        assertEquals(nil, axbya.substring(2, 5).invokemethod("match", args))
    }
}
