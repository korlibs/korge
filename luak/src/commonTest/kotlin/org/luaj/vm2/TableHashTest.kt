package org.luaj.vm2

import org.luaj.vm2.lib.*
import kotlin.test.*

/**
 * Tests for tables used as lists.
 */
class TableHashTest {
    protected fun new_Table(): LuaTable = LuaTable()
    protected fun new_Table(n: Int, m: Int): LuaTable = LuaTable(n, m)

    @Test
    fun testSetRemove() {
        val t = new_Table()

        assertEquals(0, t.hashLength)
        assertEquals(0, t.length())
        assertEquals(0, t.keyCount())

        val keys = arrayOf(
            "abc",
            "def",
            "ghi",
            "jkl",
            "mno",
            "pqr",
            "stu",
            "wxy",
            "z01",
            "cd",
            "ef",
            "g",
            "hi",
            "jk",
            "lm",
            "no",
            "pq",
            "rs"
        )
        val capacities = intArrayOf(0, 2, 2, 4, 4, 8, 8, 8, 8, 16, 16, 16, 16, 16, 16, 16, 16, 32, 32, 32)
        for (i in keys.indices) {
            assertEquals(capacities[i], t.hashLength)
            val si = "Test Value! $i"
            t[keys[i]] = si
            assertEquals(0, t.length())
            assertEquals(i + 1, t.keyCount())
        }
        assertEquals(capacities[keys.size], t.hashLength)
        for (i in keys.indices) {
            val vi = LuaString.valueOf("Test Value! $i")
            assertEquals(vi, t[keys[i]])
            assertEquals(vi, t[LuaString.valueOf(keys[i])])
            assertEquals(vi, t.rawget(keys[i]))
            assertEquals(vi, t.rawget(keys[i]))
        }

        // replace with new values
        for (i in keys.indices) {
            t[keys[i]] = LuaString.valueOf("Replacement Value! $i")
            assertEquals(0, t.length())
            assertEquals(keys.size, t.keyCount())
            assertEquals(capacities[keys.size], t.hashLength)
        }
        for (i in keys.indices) {
            val vi = LuaString.valueOf("Replacement Value! $i")
            assertEquals(vi, t[keys[i]])
        }

        // remove
        for (i in keys.indices) {
            t[keys[i]] = LuaValue.NIL
            assertEquals(0, t.length())
            assertEquals(keys.size - i - 1, t.keyCount())
            if (i < keys.size - 1)
                assertEquals(capacities[keys.size], t.hashLength)
            else
                assertTrue(0 <= t.hashLength)
        }
        for (i in keys.indices) {
            assertEquals(LuaValue.NIL, t[keys[i]])
        }
    }

    @Test
    fun testIndexMetatag() {
        val t = new_Table()
        val mt = new_Table()
        val fb = new_Table()

        // set basic values
        t["ppp"] = "abc"
        t[123] = "def"
        mt[LuaValue.INDEX] = fb
        fb["qqq"] = "ghi"
        fb[456] = "jkl"

        // check before setting metatable
        assertEquals("abc", t["ppp"].tojstring())
        assertEquals("def", t[123].tojstring())
        assertEquals("nil", t["qqq"].tojstring())
        assertEquals("nil", t[456].tojstring())
        assertEquals("nil", fb["ppp"].tojstring())
        assertEquals("nil", fb[123].tojstring())
        assertEquals("ghi", fb["qqq"].tojstring())
        assertEquals("jkl", fb[456].tojstring())
        assertEquals("nil", mt["ppp"].tojstring())
        assertEquals("nil", mt[123].tojstring())
        assertEquals("nil", mt["qqq"].tojstring())
        assertEquals("nil", mt[456].tojstring())

        // check before setting metatable
        t.setmetatable(mt)
        assertEquals(mt, t.getmetatable())
        assertEquals("abc", t["ppp"].tojstring())
        assertEquals("def", t[123].tojstring())
        assertEquals("ghi", t["qqq"].tojstring())
        assertEquals("jkl", t[456].tojstring())
        assertEquals("nil", fb["ppp"].tojstring())
        assertEquals("nil", fb[123].tojstring())
        assertEquals("ghi", fb["qqq"].tojstring())
        assertEquals("jkl", fb[456].tojstring())
        assertEquals("nil", mt["ppp"].tojstring())
        assertEquals("nil", mt[123].tojstring())
        assertEquals("nil", mt["qqq"].tojstring())
        assertEquals("nil", mt[456].tojstring())

        // set metatable to metatable without values
        t.setmetatable(fb)
        assertEquals("abc", t["ppp"].tojstring())
        assertEquals("def", t[123].tojstring())
        assertEquals("nil", t["qqq"].tojstring())
        assertEquals("nil", t[456].tojstring())

        // set metatable to null
        t.setmetatable(null)
        assertEquals("abc", t["ppp"].tojstring())
        assertEquals("def", t[123].tojstring())
        assertEquals("nil", t["qqq"].tojstring())
        assertEquals("nil", t[456].tojstring())
    }

    @Test
    fun testIndexFunction() {
        val t = new_Table()
        val mt = new_Table()

        val fb = object : TwoArgFunction() {
            override fun call(tbl: LuaValue, key: LuaValue): LuaValue {
                assertEquals(tbl, t)
                return LuaValue.valueOf("from mt: $key")
            }
        }

        // set basic values
        t["ppp"] = "abc"
        t[123] = "def"
        mt[LuaValue.INDEX] = fb

        // check before setting metatable
        assertEquals("abc", t["ppp"].tojstring())
        assertEquals("def", t[123].tojstring())
        assertEquals("nil", t["qqq"].tojstring())
        assertEquals("nil", t[456].tojstring())


        // check before setting metatable
        t.setmetatable(mt)
        assertEquals(mt, t.getmetatable())
        assertEquals("abc", t["ppp"].tojstring())
        assertEquals("def", t[123].tojstring())
        assertEquals("from mt: qqq", t["qqq"].tojstring())
        assertEquals("from mt: 456", t[456].tojstring())

        // use raw set
        t.rawset("qqq", "alt-qqq")
        t.rawset(456, "alt-456")
        assertEquals("abc", t["ppp"].tojstring())
        assertEquals("def", t[123].tojstring())
        assertEquals("alt-qqq", t["qqq"].tojstring())
        assertEquals("alt-456", t[456].tojstring())

        // remove using raw set
        t.rawset("qqq", LuaValue.NIL)
        t.rawset(456, LuaValue.NIL)
        assertEquals("abc", t["ppp"].tojstring())
        assertEquals("def", t[123].tojstring())
        assertEquals("from mt: qqq", t["qqq"].tojstring())
        assertEquals("from mt: 456", t[456].tojstring())

        // set metatable to null
        t.setmetatable(null)
        assertEquals("abc", t["ppp"].tojstring())
        assertEquals("def", t[123].tojstring())
        assertEquals("nil", t["qqq"].tojstring())
        assertEquals("nil", t[456].tojstring())
    }

    @Test
    fun testNext() {
        val t = new_Table()
        assertEquals(LuaValue.NIL, t.next(LuaValue.NIL))

        // insert array elements
        t[1] = "one"
        assertEquals(LuaValue.valueOf(1), t.next(LuaValue.NIL).arg(1))
        assertEquals(LuaValue.valueOf("one"), t.next(LuaValue.NIL).arg(2))
        assertEquals(LuaValue.NIL, t.next(LuaValue.ONE))
        t[2] = "two"
        assertEquals(LuaValue.valueOf(1), t.next(LuaValue.NIL).arg(1))
        assertEquals(LuaValue.valueOf("one"), t.next(LuaValue.NIL).arg(2))
        assertEquals(LuaValue.valueOf(2), t.next(LuaValue.ONE).arg(1))
        assertEquals(LuaValue.valueOf("two"), t.next(LuaValue.ONE).arg(2))
        assertEquals(LuaValue.NIL, t.next(LuaValue.valueOf(2)))

        // insert hash elements
        t["aa"] = "aaa"
        assertEquals(LuaValue.valueOf(1), t.next(LuaValue.NIL).arg(1))
        assertEquals(LuaValue.valueOf("one"), t.next(LuaValue.NIL).arg(2))
        assertEquals(LuaValue.valueOf(2), t.next(LuaValue.ONE).arg(1))
        assertEquals(LuaValue.valueOf("two"), t.next(LuaValue.ONE).arg(2))
        assertEquals(LuaValue.valueOf("aa"), t.next(LuaValue.valueOf(2)).arg(1))
        assertEquals(LuaValue.valueOf("aaa"), t.next(LuaValue.valueOf(2)).arg(2))
        assertEquals(LuaValue.NIL, t.next(LuaValue.valueOf("aa")))
        t["bb"] = "bbb"
        assertEquals(LuaValue.valueOf(1), t.next(LuaValue.NIL).arg(1))
        assertEquals(LuaValue.valueOf("one"), t.next(LuaValue.NIL).arg(2))
        assertEquals(LuaValue.valueOf(2), t.next(LuaValue.ONE).arg(1))
        assertEquals(LuaValue.valueOf("two"), t.next(LuaValue.ONE).arg(2))
        assertEquals(LuaValue.valueOf("aa"), t.next(LuaValue.valueOf(2)).arg(1))
        assertEquals(LuaValue.valueOf("aaa"), t.next(LuaValue.valueOf(2)).arg(2))
        assertEquals(LuaValue.valueOf("bb"), t.next(LuaValue.valueOf("aa")).arg(1))
        assertEquals(LuaValue.valueOf("bbb"), t.next(LuaValue.valueOf("aa")).arg(2))
        assertEquals(LuaValue.NIL, t.next(LuaValue.valueOf("bb")))
    }

    @Test
    fun testLoopWithRemoval() {
        val t = new_Table()

        t[LuaValue.valueOf(1)] = LuaValue.valueOf("1")
        t[LuaValue.valueOf(3)] = LuaValue.valueOf("3")
        t[LuaValue.valueOf(8)] = LuaValue.valueOf("4")
        t[LuaValue.valueOf(17)] = LuaValue.valueOf("5")
        t[LuaValue.valueOf(26)] = LuaValue.valueOf("6")
        t[LuaValue.valueOf(35)] = LuaValue.valueOf("7")
        t[LuaValue.valueOf(42)] = LuaValue.valueOf("8")
        t[LuaValue.valueOf(60)] = LuaValue.valueOf("10")
        t[LuaValue.valueOf(63)] = LuaValue.valueOf("11")

        var entry = t.next(LuaValue.NIL)
        while (!entry.isnil(1)) {
            val k = entry.arg1()
            val v = entry.arg(2)
            if (k.toint() and 1 == 0) {
                t[k] = LuaValue.NIL
            }
            entry = t.next(k)
        }

        var numEntries = 0
        entry = t.next(LuaValue.NIL)
        while (!entry.isnil(1)) {
            val k = entry.arg1()
            // Only odd keys should remain
            assertTrue(k.toint() and 1 == 1)
            numEntries++
            entry = t.next(k)
        }
        assertEquals(5, numEntries)
    }

    @Test
    fun testLoopWithRemovalAndSet() {
        val t = new_Table()

        t[LuaValue.valueOf(1)] = LuaValue.valueOf("1")
        t[LuaValue.valueOf(3)] = LuaValue.valueOf("3")
        t[LuaValue.valueOf(8)] = LuaValue.valueOf("4")
        t[LuaValue.valueOf(17)] = LuaValue.valueOf("5")
        t[LuaValue.valueOf(26)] = LuaValue.valueOf("6")
        t[LuaValue.valueOf(35)] = LuaValue.valueOf("7")
        t[LuaValue.valueOf(42)] = LuaValue.valueOf("8")
        t[LuaValue.valueOf(60)] = LuaValue.valueOf("10")
        t[LuaValue.valueOf(63)] = LuaValue.valueOf("11")

        var entry = t.next(LuaValue.NIL)
        var entry2 = entry
        while (!entry.isnil(1)) {
            val k = entry.arg1()
            val v = entry.arg(2)
            if (k.toint() and 1 == 0) {
                t[k] = LuaValue.NIL
            } else {
                t[k] = v.tonumber()
                entry2 = t.next(entry2.arg1())
            }
            entry = t.next(k)
        }

        var numEntries = 0
        entry = t.next(LuaValue.NIL)
        while (!entry.isnil(1)) {
            val k = entry.arg1()
            // Only odd keys should remain
            assertTrue(k.toint() and 1 == 1)
            assertTrue(entry.arg(2).type() == LuaValue.TNUMBER)
            numEntries++
            entry = t.next(k)
        }
        assertEquals(5, numEntries)
    }
}
