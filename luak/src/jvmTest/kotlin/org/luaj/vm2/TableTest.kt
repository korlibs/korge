/*******************************************************************************
 * Copyright (c) 2009 Luaj.org. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.luaj.vm2

import java.util.*
import kotlin.test.*

open class TableTest {
    protected open fun new_Table(): LuaTable = LuaTable()
    protected open fun new_Table(n: Int, m: Int): LuaTable = LuaTable(n, m)
    private fun keyCount(t: LuaTable): Int = keys(t).size

    private fun keys(t: LuaTable): Array<LuaValue> {
        val l = ArrayList<LuaValue>()
        var k = LuaValue.NIL
        while (true) {
            val n = t.next(k)
            if (run {
                    k = n.arg1()
                    (k).isnil()
                })
                break
            l.add(k)
        }
        return l.toTypedArray()
    }


    @Test
    fun testInOrderIntegerKeyInsertion() {
        val t = new_Table()

        for (i in 1..32) {
            t.set(i, LuaValue.valueOf("Test Value! $i"))
        }

        // Ensure all keys are still there.
        for (i in 1..32) {
            assertEquals("Test Value! $i", t.get(i).tojstring())
        }

        // Ensure capacities make sense
        assertEquals(0, t.hashLength)

        assertTrue(t.arrayLength >= 32)
        assertTrue(t.arrayLength <= 64)

    }

    @Test
    fun testRekeyCount() {
        val t = new_Table()

        // NOTE: This order of insertion is important.
        t.set(3, LuaInteger.valueOf(3))
        t.set(1, LuaInteger.valueOf(1))
        t.set(5, LuaInteger.valueOf(5))
        t.set(4, LuaInteger.valueOf(4))
        t.set(6, LuaInteger.valueOf(6))
        t.set(2, LuaInteger.valueOf(2))

        for (i in 1..5) {
            assertEquals(LuaInteger.valueOf(i), t.get(i))
        }

        assertTrue(t.arrayLength >= 3)
        assertTrue(t.arrayLength <= 12)
        assertTrue(t.hashLength <= 3)
    }

    @Test
    fun testOutOfOrderIntegerKeyInsertion() {
        val t = new_Table()

        for (i in 32 downTo 1) {
            t.set(i, LuaValue.valueOf("Test Value! $i"))
        }

        // Ensure all keys are still there.
        for (i in 1..32) {
            assertEquals("Test Value! $i", t.get(i).tojstring())
        }

        // Ensure capacities make sense
        assertEquals(32, t.arrayLength)
        assertEquals(0, t.hashLength)
    }

    @Test
    fun testStringAndIntegerKeys() {
        val t = new_Table()

        for (i in 0..9) {
            val str = LuaValue.valueOf(i.toString())
            t.set(i, str)
            t.set(str, LuaInteger.valueOf(i))
        }

        assertTrue(t.arrayLength >= 8) // 1, 2, ..., 9
        assertTrue(t.arrayLength <= 16)
        assertTrue(t.hashLength >= 11) // 0, "0", "1", ..., "9"
        assertTrue(t.hashLength <= 33)

        val keys = keys(t)

        var intKeys = 0
        var stringKeys = 0

        assertEquals(20, keys.size)
        for (i in keys.indices) {
            val k = keys[i]

            if (k is LuaInteger) {
                val ik = k.toint()
                assertTrue(ik >= 0 && ik < 10)
                val mask = 1 shl ik
                assertEquals(0, intKeys and mask)
                intKeys = intKeys or mask
            } else if (k is LuaString) {
                val ik = Integer.parseInt(k.strvalue()!!.tojstring())
                assertEquals(ik.toString(), k.strvalue()!!.tojstring())
                assertTrue(ik >= 0 && ik < 10)
                val mask = 1 shl ik
                assertEquals(0, stringKeys and mask, "Key \"$ik\" found more than once")
                stringKeys = stringKeys or mask
            } else {
                fail("Unexpected type of key found")
            }
        }

        assertEquals(0x03FF, intKeys)
        assertEquals(0x03FF, stringKeys)
    }

    @Test
    fun testBadInitialCapacity() {
        val t = new_Table(0, 1)

        t.set("test", LuaValue.valueOf("foo"))
        t.set("explode", LuaValue.valueOf("explode"))
        assertEquals(2, keyCount(t))
    }

    @Test
    fun testRemove0() {
        val t = new_Table(2, 0)

        t.set(1, LuaValue.valueOf("foo"))
        t.set(2, LuaValue.valueOf("bah"))
        assertNotSame(LuaValue.NIL, t.get(1))
        assertNotSame(LuaValue.NIL, t.get(2))
        assertEquals(LuaValue.NIL, t.get(3))

        t.set(1, LuaValue.NIL)
        t.set(2, LuaValue.NIL)
        t.set(3, LuaValue.NIL)
        assertEquals(LuaValue.NIL, t.get(1))
        assertEquals(LuaValue.NIL, t.get(2))
        assertEquals(LuaValue.NIL, t.get(3))
    }

    @Test
    fun testRemove1() {
        val t = new_Table(0, 1)

        t.set("test", LuaValue.valueOf("foo"))
        t.set("explode", LuaValue.NIL)
        t.set(42, LuaValue.NIL)
        t.set(new_Table(), LuaValue.NIL)
        t.set("test", LuaValue.NIL)
        assertEquals(0, keyCount(t))

        t.set(10, LuaInteger.valueOf(5))
        t.set(10, LuaValue.NIL)
        assertEquals(0, keyCount(t))
    }

    @Test
    fun testRemove2() {
        val t = new_Table(0, 1)

        t.set("test", LuaValue.valueOf("foo"))
        t.set("string", LuaInteger.valueOf(10))
        assertEquals(2, keyCount(t))

        t.set("string", LuaValue.NIL)
        t.set("three", LuaValue.valueOf(3.14))
        assertEquals(2, keyCount(t))

        t.set("test", LuaValue.NIL)
        assertEquals(1, keyCount(t))

        t.set(10, LuaInteger.valueOf(5))
        assertEquals(2, keyCount(t))

        t.set(10, LuaValue.NIL)
        assertEquals(1, keyCount(t))

        t.set("three", LuaValue.NIL)
        assertEquals(0, keyCount(t))
    }

    @Test
    fun testShrinkNonPowerOfTwoArray() {
        val t = new_Table(6, 2)

        t.set(1, "one")
        t.set(2, "two")
        t.set(3, "three")
        t.set(4, "four")
        t.set(5, "five")
        t.set(6, "six")

        t.set("aa", "aaa")
        t.set("bb", "bbb")

        t.set(3, LuaValue.NIL)
        t.set(4, LuaValue.NIL)
        t.set(6, LuaValue.NIL)

        t.set("cc", "ccc")
        t.set("dd", "ddd")

        assertEquals(4, t.arrayLength)
        assertTrue(t.hashLength < 10)
        assertEquals(5, t.hashEntries)
        assertEquals("one", t.get(1).tojstring())
        assertEquals("two", t.get(2).tojstring())
        assertEquals(LuaValue.NIL, t.get(3))
        assertEquals(LuaValue.NIL, t.get(4))
        assertEquals("five", t.get(5).tojstring())
        assertEquals(LuaValue.NIL, t.get(6))
        assertEquals("aaa", t.get("aa").tojstring())
        assertEquals("bbb", t.get("bb").tojstring())
        assertEquals("ccc", t.get("cc").tojstring())
        assertEquals("ddd", t.get("dd").tojstring())
    }

    @Test
    fun testInOrderLuaLength() {
        val t = new_Table()

        for (i in 1..32) {
            t.set(i, LuaValue.valueOf("Test Value! $i"))
            assertEquals(i, t.length())
        }
    }

    @Test
    fun testOutOfOrderLuaLength() {
        val t = new_Table()

        var j = 8
        while (j < 32) {
            for (i in j downTo 1) {
                t.set(i, LuaValue.valueOf("Test Value! $i"))
            }
            assertEquals(j, t.length())
            j += 8
        }
    }

    @Test
    fun testStringKeysLuaLength() {
        val t = new_Table()

        for (i in 1..32) {
            t.set("str-$i", LuaValue.valueOf("String Key Test Value! $i"))
            assertEquals(0, t.length())
        }
    }

    @Test
    fun testMixedKeysLuaLength() {
        val t = new_Table()

        for (i in 1..32) {
            t.set("str-$i", LuaValue.valueOf("String Key Test Value! $i"))
            t.set(i, LuaValue.valueOf("Int Key Test Value! $i"))
            assertEquals(i, t.length())
        }
    }

    private fun compareLists(t: LuaTable, v: Vector<*>) {
        val n = v.size
        assertEquals(v.size, t.length())
        for (j in 0 until n) {
            var vj = v.elementAt(j)
            val tj = t.get(j + 1).tojstring()
            vj = (vj as LuaString).tojstring()
            assertEquals(vj, tj)
        }
    }

    @Test
    fun testInsertBeginningOfList() {
        val t = new_Table()
        val v = Vector<LuaValue>()

        for (i in 1..32) {
            val test = LuaValue.valueOf("Test Value! $i")
            t.insert(1, test)
            v.insertElementAt(test, 0)
            compareLists(t, v)
        }
    }

    @Test
    fun testInsertEndOfList() {
        val t = new_Table()
        val v = Vector<LuaValue>()

        for (i in 1..32) {
            val test = LuaValue.valueOf("Test Value! $i")
            t.insert(0, test)
            v.insertElementAt(test, v.size)
            compareLists(t, v)
        }
    }

    @Test
    fun testInsertMiddleOfList() {
        val t = new_Table()
        val v = Vector<LuaValue>()

        for (i in 1..32) {
            val test = LuaValue.valueOf("Test Value! $i")
            val m = i / 2
            t.insert(m + 1, test)
            v.insertElementAt(test, m)
            compareLists(t, v)
        }
    }

    private fun prefillLists(t: LuaTable, v: Vector<LuaValue>) {
        for (i in 1..32) {
            val test = LuaValue.valueOf("Test Value! $i")
            t.insert(0, test)
            v.insertElementAt(test, v.size)
        }
    }

    @Test
    fun testRemoveBeginningOfList() {
        val t = new_Table()
        val v = Vector<LuaValue>()
        prefillLists(t, v)
        for (i in 1..32) {
            t.remove(1)
            v.removeElementAt(0)
            compareLists(t, v)
        }
    }

    @Test
    fun testRemoveEndOfList() {
        val t = new_Table()
        val v = Vector<LuaValue>()
        prefillLists(t, v)
        for (i in 1..32) {
            t.remove(0)
            v.removeElementAt(v.size - 1)
            compareLists(t, v)
        }
    }

    @Test
    fun testRemoveMiddleOfList() {
        val t = new_Table()
        val v = Vector<LuaValue>()
        prefillLists(t, v)
        for (i in 1..32) {
            val m = v.size / 2
            t.remove(m + 1)
            v.removeElementAt(m)
            compareLists(t, v)
        }
    }

    @Test
    fun testRemoveWhileIterating() {
        val t = LuaValue.tableOf(
            arrayOf<LuaValue>(
                LuaValue.valueOf("a"),
                LuaValue.valueOf("aa"),
                LuaValue.valueOf("b"),
                LuaValue.valueOf("bb"),
                LuaValue.valueOf("c"),
                LuaValue.valueOf("cc"),
                LuaValue.valueOf("d"),
                LuaValue.valueOf("dd"),
                LuaValue.valueOf("e"),
                LuaValue.valueOf("ee")
            ),
            arrayOf<LuaValue>(
                LuaValue.valueOf("11"),
                LuaValue.valueOf("22"),
                LuaValue.valueOf("33"),
                LuaValue.valueOf("44"),
                LuaValue.valueOf("55")
            )
        )
        // Find expected order after removal.
        val expected = ArrayList<String>()
        var n: Varargs
        var i: Int
        n = t.next(LuaValue.NIL)
        i = 0
        while (!n.arg1().isnil()) {
            if (i % 2 == 0)
                expected.add(n.arg1().toString() + "=" + n.arg(2))
            n = t.next(n.arg1())
            ++i
        }
        // Remove every other key while iterating over the table.
        n = t.next(LuaValue.NIL)
        i = 0
        while (!n.arg1().isnil()) {
            if (i % 2 != 0)
                t.set(n.arg1(), LuaValue.NIL)
            n = t.next(n.arg1())
            ++i
        }
        // Iterate over remaining table, and form list of entries still in table.
        val actual = ArrayList<String>()
        n = t.next(LuaValue.NIL)
        while (!n.arg1().isnil()) {
            actual.add(n.arg1().toString() + "=" + n.arg(2))
            n = t.next(n.arg1())
        }
        assertEquals(expected, actual)
    }
}
