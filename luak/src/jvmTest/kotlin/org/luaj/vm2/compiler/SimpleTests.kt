package org.luaj.vm2.compiler

import org.luaj.vm2.*
import org.luaj.vm2.lib.jse.*
import kotlin.test.*

class SimpleTests {

    private var globals: Globals = JsePlatform.standardGlobals()

    private fun doTest(script: String) {
        try {
            val c = globals!!.load(script, "script")
            c.call()
        } catch (e: Exception) {
            assertTrue(false, "i/o exception: $e")
        }

    }

    @Test
    fun testTrivial() {
        val s = "print( 2 )\n"
        doTest(s)
    }

    @Test
    fun testAlmostTrivial() {
        val s = "print( 2 )\n" + "print( 3 )\n"
        doTest(s)
    }

    @Test
    fun testSimple() {
        val s = "print( 'hello, world' )\n" +
            "for i = 2,4 do\n" +
            "	print( 'i', i )\n" +
            "end\n"
        doTest(s)
    }

    @Test
    fun testBreak() {
        val s = "a=1\n" +
            "while true do\n" +
            "  if a>10 then\n" +
            "     break\n" +
            "  end\n" +
            "  a=a+1\n" +
            "  print( a )\n" +
            "end\n"
        doTest(s)
    }

    @Test
    fun testShebang() {
        val s = "#!../lua\n" + "print( 2 )\n"
        doTest(s)
    }

    @Test
    fun testInlineTable() {
        val s = "A = {g=10}\n" + "print( A )\n"
        doTest(s)
    }

    @Test
    fun testEqualsAnd() {
        val s = "print( 1 == b and b )\n"
        doTest(s)
    }

    @Test
    fun testDoubleHashCode() {
        for (i in samehash.indices) {
            val j = LuaInteger.valueOf(samehash[i])
            val d = LuaDouble.valueOf(samehash[i].toDouble())
            val hj = j.hashCode()
            val hd = d.hashCode()
            assertEquals(hj, hd)
        }
        var i = 0
        while (i < diffhash.size) {
            val c = LuaValue.valueOf(diffhash[i + 0])
            val d = LuaValue.valueOf(diffhash[i + 1])
            val hc = c.hashCode()
            val hd = d.hashCode()
            assertTrue(hc != hd, "hash codes are same: $hc")
            i += 2
        }
    }

    companion object {

        private val samehash = intArrayOf(0, 1, -1, 2, -2, 4, 8, 16, 32, Integer.MAX_VALUE, Integer.MIN_VALUE)
        private val diffhash = doubleArrayOf(.5, 1.0, 1.5, 1.0, .5, 1.5, 1.25, 2.5)
    }
}
