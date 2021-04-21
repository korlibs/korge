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

import java.lang.ref.*
import kotlin.test.*

abstract class WeakTableTest : TableTest() {

    class MyData(val value: Int) {
        override fun hashCode(): Int = value
        override fun equals(o: Any?): Boolean = o is MyData && o.value == value
        override fun toString(): String = "mydata-$value"
    }

    class WeakValueTableTest : WeakTableTest() {
        override fun new_Table(): LuaTable = WeakTable.make(false, true)
        override fun new_Table(n: Int, m: Int): LuaTable = WeakTable.make(false, true)

        @Test
        @Suppress("UNUSED_VALUE")
        fun testWeakValuesTable() {
            val t = new_Table()

            var obj: Any? = Any()
            var tableValue: LuaTable? = LuaTable()
            var stringValue: LuaString? = LuaString.valueOf("this is a test")
            var tableValue2: LuaTable? = LuaTable()

            t["table"] = tableValue!!
            t["userdata"] = LuaValue.userdataOf(obj!!, null)
            t["string"] = stringValue!!
            t["string2"] = LuaValue.valueOf("another string")
            t[1] = tableValue2!!
            assertTrue(t.hashLength >= 4, "table must have at least 4 elements")
            assertTrue(t.arrayLength >= 1, "array part must have 1 element")

            // check that table can be used to get elements
            assertEquals(tableValue, t["table"])
            assertEquals(stringValue, t["string"])
            assertEquals(obj, t["userdata"].checkuserdata())
            assertEquals(tableValue2, t[1])

            // nothing should be collected, since we have strong references here
            collectGarbage()

            // check that elements are still there
            assertEquals(tableValue, t["table"])
            assertEquals(stringValue, t["string"])
            assertEquals(obj, t["userdata"].checkuserdata())
            assertEquals(tableValue2, t[1])

            // drop our strong references
            obj = null
            tableValue = null
            tableValue2 = null
            stringValue = null

            // Garbage collection should cause weak entries to be dropped.
            collectGarbage()

            // check that they are dropped
            assertEquals(LuaValue.NIL, t["table"])
            assertEquals(LuaValue.NIL, t["userdata"])
            assertEquals(LuaValue.NIL, t[1])
            assertFalse(t["string"].isnil(), "strings should not be in weak references")
        }
    }

    class WeakKeyTableTest : WeakTableTest() {
        override fun new_Table(): LuaTable = WeakTable.make(true, false)
        override fun new_Table(n: Int, m: Int): LuaTable = WeakTable.make(true, false)

        @Test
        @Ignore // @TODO: Kotlin 1.5-RC Is there a leak? origkey.get() doesn't return null after GC
        fun testWeakKeysTable() {
            val t = WeakTable.make(true, false)

            var key: LuaValue = LuaValue.userdataOf(MyData(111))
            var `val`: LuaValue = LuaValue.userdataOf(MyData(222))

            // set up the table
            t[key] = `val`
            assertEquals(`val`, t[key])
            System.gc()
            assertEquals(`val`, t[key])

            // drop key and value references, replace them with new ones
            val origkey = WeakReference(key)
            val origval = WeakReference(`val`)
            key = LuaValue.userdataOf(MyData(111))
            `val` = LuaValue.userdataOf(MyData(222))

            // new key and value should be interchangeable (feature of this test class)
            assertEquals(key, origkey.get())
            assertEquals(`val`, origval.get())
            assertEquals(`val`, t[key])
            assertEquals(`val`, t[origkey.get()!!])
            assertEquals(origval.get(), t[key])

            // value should not be reachable after gc
            collectGarbage()
            assertNull(origkey.get())
            assertEquals(LuaValue.NIL, t[key])
            collectGarbage()
            assertNull(origval.get())
        }

        @Test
        fun testNext() {
            val t = WeakTable.make(true, true)

            val key = LuaValue.userdataOf(MyData(111))
            val `val` = LuaValue.userdataOf(MyData(222))
            var key2: LuaValue? = LuaValue.userdataOf(MyData(333))
            var val2: LuaValue? = LuaValue.userdataOf(MyData(444))
            val key3 = LuaValue.userdataOf(MyData(555))
            val val3 = LuaValue.userdataOf(MyData(666))

            // set up the table
            t[key] = `val`
            t[key2!!] = val2!!
            t[key3] = val3

            // forget one of the keys
            key2 = null
            val2 = null
            collectGarbage()

            // table should have 2 entries
            var size = 0
            var k = t.next(LuaValue.NIL).arg1()
            while (!k.isnil()) {
                size++
                k = t.next(k).arg1()
            }
            assertEquals(2, size)
        }
    }

    class WeakKeyValueTableTest : WeakTableTest() {
        override fun new_Table(): LuaTable = WeakTable.make(true, true)
        override fun new_Table(n: Int, m: Int): LuaTable = WeakTable.make(true, true)

        @Test
        @Suppress("UNUSED_VALUE")
        fun testWeakKeysValuesTable() {
            val t = WeakTable.make(true, true)

            var key: LuaValue = LuaValue.userdataOf(MyData(111))
            var `val`: LuaValue = LuaValue.userdataOf(MyData(222))
            var key2: LuaValue = LuaValue.userdataOf(MyData(333))
            var val2: LuaValue? = LuaValue.userdataOf(MyData(444))
            var key3: LuaValue? = LuaValue.userdataOf(MyData(555))
            var val3: LuaValue = LuaValue.userdataOf(MyData(666))

            // set up the table
            t[key] = `val`
            t[key2] = val2!!
            t[key3!!] = val3
            assertEquals(`val`, t[key])
            assertEquals(val2, t[key2])
            assertEquals(val3, t[key3])
            System.gc()
            assertEquals(`val`, t[key])
            assertEquals(val2, t[key2])
            assertEquals(val3, t[key3])

            // drop key and value references, replace them with new ones
            val origkey = WeakReference(key)
            val origval = WeakReference(`val`)
            val origkey2 = WeakReference(key2)
            val origval2 = WeakReference(val2)
            val origkey3 = WeakReference(key3)
            val origval3 = WeakReference(val3)
            key = LuaValue.userdataOf(MyData(111))
            `val` = LuaValue.userdataOf(MyData(222))
            key2 = LuaValue.userdataOf(MyData(333))
            // don't drop val2, or key3
            val3 = LuaValue.userdataOf(MyData(666))

            // no values should be reachable after gc
            collectGarbage()
            assertEquals(null, origkey.get())
            assertEquals(null, origval.get())
            assertEquals(null, origkey2.get())
            assertEquals(null, origval3.get())
            assertEquals(LuaValue.NIL, t[key])
            assertEquals(LuaValue.NIL, t[key2])
            assertEquals(LuaValue.NIL, t[key3])

            // all originals should be gone after gc, then access
            val2 = null
            key3 = null
            collectGarbage()
            assertEquals(null, origval2.get())
            assertEquals(null, origkey3.get())
        }

        @Test
        fun testReplace() {
            val t = WeakTable.make(true, true)

            val key = LuaValue.userdataOf(MyData(111))
            val `val` = LuaValue.userdataOf(MyData(222))
            val key2 = LuaValue.userdataOf(MyData(333))
            val val2 = LuaValue.userdataOf(MyData(444))
            val key3 = LuaValue.userdataOf(MyData(555))
            val val3 = LuaValue.userdataOf(MyData(666))

            // set up the table
            t[key] = `val`
            t[key2] = val2
            t[key3] = val3

            val val4 = LuaValue.userdataOf(MyData(777))
            t[key2] = val4

            // table should have 3 entries
            var size = 0
            var k = t.next(LuaValue.NIL).arg1()
            while (!k.isnil() && size < 1000) {
                size++
                k = t.next(k).arg1()
            }
            assertEquals(3, size)
        }
    }

    companion object {
        internal fun collectGarbage() {
            val rt = Runtime.getRuntime()
            rt.gc()
            try {
                Thread.sleep(20)
                rt.gc()
                Thread.sleep(20)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            rt.gc()
        }
    }
}
