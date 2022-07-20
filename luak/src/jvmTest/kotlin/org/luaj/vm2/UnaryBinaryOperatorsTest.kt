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

import java.lang.reflect.InvocationTargetException

import org.luaj.vm2.lib.TwoArgFunction

import kotlin.test.*

/**
 * Tests of basic unary and binary operators on main value types.
 */
class UnaryBinaryOperatorsTest {

    internal val dummy: LuaValue = LuaValue.ZERO

    @Test
    fun testEqualsBool() {
        assertEquals(LuaValue.BFALSE, LuaValue.BFALSE)
        assertEquals(LuaValue.BTRUE, LuaValue.BTRUE)
        assertTrue(LuaValue.BFALSE == LuaValue.BFALSE)
        assertTrue(LuaValue.BTRUE == LuaValue.BTRUE)
        assertTrue(LuaValue.BFALSE != LuaValue.BTRUE)
        assertTrue(LuaValue.BTRUE != LuaValue.BFALSE)
        assertTrue(LuaValue.BFALSE.eq_b(LuaValue.BFALSE))
        assertTrue(LuaValue.BTRUE.eq_b(LuaValue.BTRUE))
        assertFalse(LuaValue.BFALSE.eq_b(LuaValue.BTRUE))
        assertFalse(LuaValue.BTRUE.eq_b(LuaValue.BFALSE))
        assertEquals(LuaValue.BTRUE, LuaValue.BFALSE.eq(LuaValue.BFALSE))
        assertEquals(LuaValue.BTRUE, LuaValue.BTRUE.eq(LuaValue.BTRUE))
        assertEquals(LuaValue.BFALSE, LuaValue.BFALSE.eq(LuaValue.BTRUE))
        assertEquals(LuaValue.BFALSE, LuaValue.BTRUE.eq(LuaValue.BFALSE))
        assertFalse(LuaValue.BFALSE.neq_b(LuaValue.BFALSE))
        assertFalse(LuaValue.BTRUE.neq_b(LuaValue.BTRUE))
        assertTrue(LuaValue.BFALSE.neq_b(LuaValue.BTRUE))
        assertTrue(LuaValue.BTRUE.neq_b(LuaValue.BFALSE))
        assertEquals(LuaValue.BFALSE, LuaValue.BFALSE.neq(LuaValue.BFALSE))
        assertEquals(LuaValue.BFALSE, LuaValue.BTRUE.neq(LuaValue.BTRUE))
        assertEquals(LuaValue.BTRUE, LuaValue.BFALSE.neq(LuaValue.BTRUE))
        assertEquals(LuaValue.BTRUE, LuaValue.BTRUE.neq(LuaValue.BFALSE))
        assertTrue(LuaValue.BTRUE.toboolean())
        assertFalse(LuaValue.BFALSE.toboolean())
    }

    @Test
    fun testNot() {
        val ia = LuaValue.valueOf(3)
        val da = LuaValue.valueOf(.25)
        val sa = LuaValue.valueOf("1.5")
        val ba = LuaValue.BTRUE
        val bb = LuaValue.BFALSE

        // like kinds
        assertEquals(LuaValue.BFALSE, ia.not())
        assertEquals(LuaValue.BFALSE, da.not())
        assertEquals(LuaValue.BFALSE, sa.not())
        assertEquals(LuaValue.BFALSE, ba.not())
        assertEquals(LuaValue.BTRUE, bb.not())
    }

    @Test
    fun testNeg() {
        val ia = LuaValue.valueOf(3)
        val ib = LuaValue.valueOf(-4)
        val da = LuaValue.valueOf(.25)
        val db = LuaValue.valueOf(-.5)
        val sa = LuaValue.valueOf("1.5")
        val sb = LuaValue.valueOf("-2.0")

        // like kinds
        assertEquals(-3.0, ia.neg().todouble())
        assertEquals(-.25, da.neg().todouble())
        assertEquals(-1.5, sa.neg().todouble())
        assertEquals(4.0, ib.neg().todouble())
        assertEquals(.5, db.neg().todouble())
        assertEquals(2.0, sb.neg().todouble())
    }

    @Test
    fun testDoublesBecomeInts() {
        // DoubleValue.valueOf should return int
        val ia = LuaInteger.valueOf(345)
        val da = LuaDouble.valueOf(345.0)
        val db = LuaDouble.valueOf(345.5)
        val sa = LuaValue.valueOf("3.0")
        val sb = LuaValue.valueOf("3")
        val sc = LuaValue.valueOf("-2.0")
        val sd = LuaValue.valueOf("-2")

        assertEquals(ia, da)
        assertTrue(ia is LuaInteger)
        assertTrue(da is LuaInteger)
        assertTrue(db is LuaDouble)
        assertEquals(ia.toint(), 345)
        assertEquals(da.toint(), 345)
        assertEquals(da.todouble(), 345.0)
        assertEquals(db.todouble(), 345.5)

        assertTrue(sa is LuaString)
        assertTrue(sb is LuaString)
        assertTrue(sc is LuaString)
        assertTrue(sd is LuaString)
        assertEquals(3.0, sa.todouble())
        assertEquals(3.0, sb.todouble())
        assertEquals(-2.0, sc.todouble())
        assertEquals(-2.0, sd.todouble())

    }

    @Test
    fun testEqualsInt() {
        val ia = LuaInteger.valueOf(345)
        val ib = LuaInteger.valueOf(345)
        val ic = LuaInteger.valueOf(-345)
        val sa = LuaString.valueOf("345")
        val sb = LuaString.valueOf("345")
        val sc = LuaString.valueOf("-345")

        // objects should be different
        assertNotSame(ia, ib)
        assertSame(sa, sb)
        assertNotSame(ia, ic)
        assertNotSame(sa, sc)

        // assert equals for same type
        assertEquals(ia, ib)
        assertEquals(sa, sb)
        assertFalse(ia == ic)
        assertFalse(sa == sc)

        // check object equality for different types
        assertNotSame(ia as Any, sa as Any)
        assertNotSame(sa as Any, ia as Any)
    }

    @Test
    fun testEqualsDouble() {
        val da = LuaDouble.valueOf(345.5)
        val db = LuaDouble.valueOf(345.5)
        val dc = LuaDouble.valueOf(-345.5)
        val sa = LuaString.valueOf("345.5")
        val sb = LuaString.valueOf("345.5")
        val sc = LuaString.valueOf("-345.5")

        // objects should be different
        assertNotSame(da, db)
        assertSame(sa, sb)
        assertNotSame(da, dc)
        assertNotSame(sa, sc)

        // assert equals for same type
        assertEquals(da, db)
        assertEquals(sa, sb)
        assertFalse(da == dc)
        assertFalse(sa == sc)

        // check object equality for different types
        assertNotSame(da as Any, sa as Any)
        assertNotSame(sa as Any, da as Any)
    }

    @Test
    fun testEqInt() {
        val ia = LuaInteger.valueOf(345)
        val ib = LuaInteger.valueOf(345)
        val ic = LuaInteger.valueOf(-123)
        val sa = LuaString.valueOf("345")
        val sb = LuaString.valueOf("345")
        val sc = LuaString.valueOf("-345")

        // check arithmetic equality among same types
        assertEquals(ia.eq(ib), LuaValue.BTRUE)
        assertEquals(sa.eq(sb), LuaValue.BTRUE)
        assertEquals(ia.eq(ic), LuaValue.BFALSE)
        assertEquals(sa.eq(sc), LuaValue.BFALSE)

        // check arithmetic equality among different types
        assertEquals(ia.eq(sa), LuaValue.BFALSE)
        assertEquals(sa.eq(ia), LuaValue.BFALSE)

        // equals with mismatched types
        val t = LuaTable()
        assertEquals(ia.eq(t), LuaValue.BFALSE)
        assertEquals(t.eq(ia), LuaValue.BFALSE)
        assertEquals(ia.eq(LuaValue.BFALSE), LuaValue.BFALSE)
        assertEquals(LuaValue.BFALSE.eq(ia), LuaValue.BFALSE)
        assertEquals(ia.eq(LuaValue.NIL), LuaValue.BFALSE)
        assertEquals(LuaValue.NIL.eq(ia), LuaValue.BFALSE)
    }

    @Test
    fun testEqDouble() {
        val da = LuaDouble.valueOf(345.5)
        val db = LuaDouble.valueOf(345.5)
        val dc = LuaDouble.valueOf(-345.5)
        val sa = LuaString.valueOf("345.5")
        val sb = LuaString.valueOf("345.5")
        val sc = LuaString.valueOf("-345.5")

        // check arithmetic equality among same types
        assertEquals(da.eq(db), LuaValue.BTRUE)
        assertEquals(sa.eq(sb), LuaValue.BTRUE)
        assertEquals(da.eq(dc), LuaValue.BFALSE)
        assertEquals(sa.eq(sc), LuaValue.BFALSE)

        // check arithmetic equality among different types
        assertEquals(da.eq(sa), LuaValue.BFALSE)
        assertEquals(sa.eq(da), LuaValue.BFALSE)

        // equals with mismatched types
        val t = LuaTable()
        assertEquals(da.eq(t), LuaValue.BFALSE)
        assertEquals(t.eq(da), LuaValue.BFALSE)
        assertEquals(da.eq(LuaValue.BFALSE), LuaValue.BFALSE)
        assertEquals(LuaValue.BFALSE.eq(da), LuaValue.BFALSE)
        assertEquals(da.eq(LuaValue.NIL), LuaValue.BFALSE)
        assertEquals(LuaValue.NIL.eq(da), LuaValue.BFALSE)
    }

    @Test
    fun testEqualsMetatag() {
        val tru = LuaValue.BTRUE
        val fal = LuaValue.BFALSE
        val zer = LuaValue.ZERO
        val one = LuaValue.ONE
        val abc = LuaValue.valueOf("abcdef").substring(0, 3)
        val def = LuaValue.valueOf("abcdef").substring(3, 6)
        val pi = LuaValue.valueOf(Math.PI)
        val ee = LuaValue.valueOf(Math.E)
        val tbl = LuaTable()
        val tbl2 = LuaTable()
        val tbl3 = LuaTable()
        val uda = LuaUserdata(Any())
        val udb = LuaUserdata(uda.touserdata())
        val uda2 = LuaUserdata(Any())
        val uda3 = LuaUserdata(uda.touserdata())
        val nilb = LuaValue.valueOf(LuaValue.NIL.toboolean())
        val oneb = LuaValue.valueOf(LuaValue.ONE.toboolean())
        assertEquals(LuaValue.BFALSE, nilb)
        assertEquals(LuaValue.BTRUE, oneb)
        val smt = LuaString.s_metatable
        try {
            // always return nil0
            LuaBoolean.s_metatable = LuaValue.tableOf(arrayOf(LuaValue.EQ, RETURN_NIL))
            LuaNumber.s_metatable = LuaValue.tableOf(arrayOf(LuaValue.EQ, RETURN_NIL))
            LuaString.s_metatable = LuaValue.tableOf(arrayOf(LuaValue.EQ, RETURN_NIL))
            tbl.setmetatable(LuaValue.tableOf(arrayOf(LuaValue.EQ, RETURN_NIL)))
            tbl2.setmetatable(LuaValue.tableOf(arrayOf(LuaValue.EQ, RETURN_NIL)))
            uda.setmetatable(LuaValue.tableOf(arrayOf(LuaValue.EQ, RETURN_NIL)))
            udb.setmetatable(uda.getmetatable())
            uda2.setmetatable(LuaValue.tableOf(arrayOf(LuaValue.EQ, RETURN_NIL)))
            // diff metatag function
            tbl3.setmetatable(LuaValue.tableOf(arrayOf(LuaValue.EQ, RETURN_ONE)))
            uda3.setmetatable(LuaValue.tableOf(arrayOf(LuaValue.EQ, RETURN_ONE)))

            // primitive types or same valu do not invoke metatag as per C implementation
            assertEquals(tru, tru.eq(tru))
            assertEquals(tru, one.eq(one))
            assertEquals(tru, abc.eq(abc))
            assertEquals(tru, tbl.eq(tbl))
            assertEquals(tru, uda.eq(uda))
            assertEquals(tru, uda.eq(udb))
            assertEquals(fal, tru.eq(fal))
            assertEquals(fal, fal.eq(tru))
            assertEquals(fal, zer.eq(one))
            assertEquals(fal, one.eq(zer))
            assertEquals(fal, pi.eq(ee))
            assertEquals(fal, ee.eq(pi))
            assertEquals(fal, pi.eq(one))
            assertEquals(fal, one.eq(pi))
            assertEquals(fal, abc.eq(def))
            assertEquals(fal, def.eq(abc))
            // different types.  not comparable
            assertEquals(fal, fal.eq(tbl))
            assertEquals(fal, tbl.eq(fal))
            assertEquals(fal, tbl.eq(one))
            assertEquals(fal, one.eq(tbl))
            assertEquals(fal, fal.eq(one))
            assertEquals(fal, one.eq(fal))
            assertEquals(fal, abc.eq(one))
            assertEquals(fal, one.eq(abc))
            assertEquals(fal, tbl.eq(uda))
            assertEquals(fal, uda.eq(tbl))
            // same type, same value, does not invoke metatag op
            assertEquals(tru, tbl.eq(tbl))
            // same type, different value, same metatag op.  comparabile via metatag op
            assertEquals(nilb, tbl.eq(tbl2))
            assertEquals(nilb, tbl2.eq(tbl))
            assertEquals(nilb, uda.eq(uda2))
            assertEquals(nilb, uda2.eq(uda))
            // same type, different metatag ops.  not comparable
            assertEquals(fal, tbl.eq(tbl3))
            assertEquals(fal, tbl3.eq(tbl))
            assertEquals(fal, uda.eq(uda3))
            assertEquals(fal, uda3.eq(uda))

            // always use right argument
            LuaBoolean.s_metatable = LuaValue.tableOf(arrayOf(LuaValue.EQ, RETURN_ONE))
            LuaNumber.s_metatable = LuaValue.tableOf(arrayOf(LuaValue.EQ, RETURN_ONE))
            LuaString.s_metatable = LuaValue.tableOf(arrayOf(LuaValue.EQ, RETURN_ONE))
            tbl.setmetatable(LuaValue.tableOf(arrayOf(LuaValue.EQ, RETURN_ONE)))
            tbl2.setmetatable(LuaValue.tableOf(arrayOf(LuaValue.EQ, RETURN_ONE)))
            uda.setmetatable(LuaValue.tableOf(arrayOf(LuaValue.EQ, RETURN_ONE)))
            udb.setmetatable(uda.getmetatable())
            uda2.setmetatable(LuaValue.tableOf(arrayOf(LuaValue.EQ, RETURN_ONE)))
            // diff metatag function
            tbl3.setmetatable(LuaValue.tableOf(arrayOf(LuaValue.EQ, RETURN_NIL)))
            uda3.setmetatable(LuaValue.tableOf(arrayOf(LuaValue.EQ, RETURN_NIL)))

            // primitive types or same value do not invoke metatag as per C implementation
            assertEquals(tru, tru.eq(tru))
            assertEquals(tru, one.eq(one))
            assertEquals(tru, abc.eq(abc))
            assertEquals(tru, tbl.eq(tbl))
            assertEquals(tru, uda.eq(uda))
            assertEquals(tru, uda.eq(udb))
            assertEquals(fal, tru.eq(fal))
            assertEquals(fal, fal.eq(tru))
            assertEquals(fal, zer.eq(one))
            assertEquals(fal, one.eq(zer))
            assertEquals(fal, pi.eq(ee))
            assertEquals(fal, ee.eq(pi))
            assertEquals(fal, pi.eq(one))
            assertEquals(fal, one.eq(pi))
            assertEquals(fal, abc.eq(def))
            assertEquals(fal, def.eq(abc))
            // different types.  not comparable
            assertEquals(fal, fal.eq(tbl))
            assertEquals(fal, tbl.eq(fal))
            assertEquals(fal, tbl.eq(one))
            assertEquals(fal, one.eq(tbl))
            assertEquals(fal, fal.eq(one))
            assertEquals(fal, one.eq(fal))
            assertEquals(fal, abc.eq(one))
            assertEquals(fal, one.eq(abc))
            assertEquals(fal, tbl.eq(uda))
            assertEquals(fal, uda.eq(tbl))
            // same type, same value, does not invoke metatag op
            assertEquals(tru, tbl.eq(tbl))
            // same type, different value, same metatag op.  comparabile via metatag op
            assertEquals(oneb, tbl.eq(tbl2))
            assertEquals(oneb, tbl2.eq(tbl))
            assertEquals(oneb, uda.eq(uda2))
            assertEquals(oneb, uda2.eq(uda))
            // same type, different metatag ops.  not comparable
            assertEquals(fal, tbl.eq(tbl3))
            assertEquals(fal, tbl3.eq(tbl))
            assertEquals(fal, uda.eq(uda3))
            assertEquals(fal, uda3.eq(uda))

        } finally {
            LuaBoolean.s_metatable = null
            LuaNumber.s_metatable = null
            LuaString.s_metatable = smt
        }
    }

    @Test
    fun testAdd() {
        val ia = LuaValue.valueOf(111)
        val ib = LuaValue.valueOf(44)
        val da = LuaValue.valueOf(55.25)
        val db = LuaValue.valueOf(3.5)
        val sa = LuaValue.valueOf("22.125")
        val sb = LuaValue.valueOf("7.25")

        // check types
        assertTrue(ia is LuaInteger)
        assertTrue(ib is LuaInteger)
        assertTrue(da is LuaDouble)
        assertTrue(db is LuaDouble)
        assertTrue(sa is LuaString)
        assertTrue(sb is LuaString)

        // like kinds
        assertEquals(155.0, ia.add(ib).todouble())
        assertEquals(58.75, da.add(db).todouble())
        assertEquals(29.375, sa.add(sb).todouble())

        // unlike kinds
        assertEquals(166.25, ia.add(da).todouble())
        assertEquals(166.25, da.add(ia).todouble())
        assertEquals(133.125, ia.add(sa).todouble())
        assertEquals(133.125, sa.add(ia).todouble())
        assertEquals(77.375, da.add(sa).todouble())
        assertEquals(77.375, sa.add(da).todouble())
    }

    @Test
    fun testSub() {
        val ia = LuaValue.valueOf(111)
        val ib = LuaValue.valueOf(44)
        val da = LuaValue.valueOf(55.25)
        val db = LuaValue.valueOf(3.5)
        val sa = LuaValue.valueOf("22.125")
        val sb = LuaValue.valueOf("7.25")

        // like kinds
        assertEquals(67.0, ia.sub(ib).todouble())
        assertEquals(51.75, da.sub(db).todouble())
        assertEquals(14.875, sa.sub(sb).todouble())

        // unlike kinds
        assertEquals(55.75, ia.sub(da).todouble())
        assertEquals(-55.75, da.sub(ia).todouble())
        assertEquals(88.875, ia.sub(sa).todouble())
        assertEquals(-88.875, sa.sub(ia).todouble())
        assertEquals(33.125, da.sub(sa).todouble())
        assertEquals(-33.125, sa.sub(da).todouble())
    }

    @Test
    fun testMul() {
        val ia = LuaValue.valueOf(3)
        val ib = LuaValue.valueOf(4)
        val da = LuaValue.valueOf(.25)
        val db = LuaValue.valueOf(.5)
        val sa = LuaValue.valueOf("1.5")
        val sb = LuaValue.valueOf("2.0")

        // like kinds
        assertEquals(12.0, ia.mul(ib).todouble())
        assertEquals(.125, da.mul(db).todouble())
        assertEquals(3.0, sa.mul(sb).todouble())

        // unlike kinds
        assertEquals(.75, ia.mul(da).todouble())
        assertEquals(.75, da.mul(ia).todouble())
        assertEquals(4.5, ia.mul(sa).todouble())
        assertEquals(4.5, sa.mul(ia).todouble())
        assertEquals(.375, da.mul(sa).todouble())
        assertEquals(.375, sa.mul(da).todouble())
    }

    @Test
    fun testDiv() {
        val ia = LuaValue.valueOf(3)
        val ib = LuaValue.valueOf(4)
        val da = LuaValue.valueOf(.25)
        val db = LuaValue.valueOf(.5)
        val sa = LuaValue.valueOf("1.5")
        val sb = LuaValue.valueOf("2.0")

        // like kinds
        assertEquals(3.0 / 4.0, ia.div(ib).todouble())
        assertEquals(.25 / .5, da.div(db).todouble())
        assertEquals(1.5 / 2.0, sa.div(sb).todouble())

        // unlike kinds
        assertEquals(3.0 / .25, ia.div(da).todouble())
        assertEquals(.25 / 3.0, da.div(ia).todouble())
        assertEquals(3.0 / 1.5, ia.div(sa).todouble())
        assertEquals(1.5 / 3.0, sa.div(ia).todouble())
        assertEquals(.25 / 1.5, da.div(sa).todouble())
        assertEquals(1.5 / .25, sa.div(da).todouble())
    }

    @Test
    fun testPow() {
        val ia = LuaValue.valueOf(3)
        val ib = LuaValue.valueOf(4)
        val da = LuaValue.valueOf(4.0)
        val db = LuaValue.valueOf(.5)
        val sa = LuaValue.valueOf("1.5")
        val sb = LuaValue.valueOf("2.0")

        // like kinds
        assertEquals(Math.pow(3.0, 4.0), ia.pow(ib).todouble())
        assertEquals(Math.pow(4.0, .5), da.pow(db).todouble())
        assertEquals(Math.pow(1.5, 2.0), sa.pow(sb).todouble())

        // unlike kinds
        assertEquals(Math.pow(3.0, 4.0), ia.pow(da).todouble())
        assertEquals(Math.pow(4.0, 3.0), da.pow(ia).todouble())
        assertEquals(Math.pow(3.0, 1.5), ia.pow(sa).todouble())
        assertEquals(Math.pow(1.5, 3.0), sa.pow(ia).todouble())
        assertEquals(Math.pow(4.0, 1.5), da.pow(sa).todouble())
        assertEquals(Math.pow(1.5, 4.0), sa.pow(da).todouble())
    }

    @Test
    fun testMod() {
        val ia = LuaValue.valueOf(3)
        val ib = LuaValue.valueOf(-4)
        val da = LuaValue.valueOf(.25)
        val db = LuaValue.valueOf(-.5)
        val sa = LuaValue.valueOf("1.5")
        val sb = LuaValue.valueOf("-2.0")

        // like kinds
        assertEquals(luaMod(3.0, -4.0), ia.mod(ib).todouble())
        assertEquals(luaMod(.25, -.5), da.mod(db).todouble())
        assertEquals(luaMod(1.5, -2.0), sa.mod(sb).todouble())

        // unlike kinds
        assertEquals(luaMod(3.0, .25), ia.mod(da).todouble())
        assertEquals(luaMod(.25, 3.0), da.mod(ia).todouble())
        assertEquals(luaMod(3.0, 1.5), ia.mod(sa).todouble())
        assertEquals(luaMod(1.5, 3.0), sa.mod(ia).todouble())
        assertEquals(luaMod(.25, 1.5), da.mod(sa).todouble())
        assertEquals(luaMod(1.5, .25), sa.mod(da).todouble())
    }

    @Test
    fun testArithErrors() {
        val ia = LuaValue.valueOf(111)
        val ib = LuaValue.valueOf(44)
        val da = LuaValue.valueOf(55.25)
        val db = LuaValue.valueOf(3.5)
        val sa = LuaValue.valueOf("22.125")
        val sb = LuaValue.valueOf("7.25")

        val ops = arrayOf("add", "sub", "mul", "div", "mod", "pow")
        val vals = arrayOf(LuaValue.NIL, LuaValue.BTRUE, LuaValue.tableOf())
        val numerics = arrayOf(LuaValue.valueOf(111), LuaValue.valueOf(55.25), LuaValue.valueOf("22.125"))
        for (i in ops.indices) {
            for (j in vals.indices) {
                for (k in numerics.indices) {
                    checkArithError(vals[j], numerics[k], ops[i], vals[j].typename())
                    checkArithError(numerics[k], vals[j], ops[i], vals[j].typename())
                }
            }
        }
    }

    private fun checkArithError(a: LuaValue, b: LuaValue, op: String, type: String) {
        try {
            LuaValue::class.java.getMethod(op, *arrayOf<Class<*>>(LuaValue::class.java)).invoke(a, *arrayOf<Any>(b))
        } catch (ite: InvocationTargetException) {
            val actual = ite.targetException.message!!
            if (!actual.startsWith("attempt to perform arithmetic") || actual.indexOf(type) < 0)
                fail("(" + a.typename() + "," + op + "," + b.typename() + ") reported '" + actual + "'")
        } catch (e: Exception) {
            fail("(" + a.typename() + "," + op + "," + b.typename() + ") threw " + e)
        }

    }

    @Test
    fun testArithMetatag() {
        val tru = LuaValue.BTRUE
        val fal = LuaValue.BFALSE
        val tbl = LuaTable()
        val tbl2 = LuaTable()
        try {
            try {
                tru.add(tbl)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                tru.sub(tbl)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                tru.mul(tbl)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                tru.div(tbl)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                tru.pow(tbl)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                tru.mod(tbl)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            // always use left argument
            LuaBoolean.s_metatable = LuaValue.tableOf(arrayOf(LuaValue.ADD, RETURN_LHS))
            assertEquals(tru, tru.add(fal))
            assertEquals(tru, tru.add(tbl))
            assertEquals(tbl, tbl.add(tru))
            try {
                tbl.add(tbl2)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                tru.sub(tbl)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            LuaBoolean.s_metatable = LuaValue.tableOf(arrayOf(LuaValue.SUB, RETURN_LHS))
            assertEquals(tru, tru.sub(fal))
            assertEquals(tru, tru.sub(tbl))
            assertEquals(tbl, tbl.sub(tru))
            try {
                tbl.sub(tbl2)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                tru.add(tbl)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            LuaBoolean.s_metatable = LuaValue.tableOf(arrayOf(LuaValue.MUL, RETURN_LHS))
            assertEquals(tru, tru.mul(fal))
            assertEquals(tru, tru.mul(tbl))
            assertEquals(tbl, tbl.mul(tru))
            try {
                tbl.mul(tbl2)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                tru.sub(tbl)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            LuaBoolean.s_metatable = LuaValue.tableOf(arrayOf(LuaValue.DIV, RETURN_LHS))
            assertEquals(tru, tru.div(fal))
            assertEquals(tru, tru.div(tbl))
            assertEquals(tbl, tbl.div(tru))
            try {
                tbl.div(tbl2)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                tru.sub(tbl)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            LuaBoolean.s_metatable = LuaValue.tableOf(arrayOf(LuaValue.POW, RETURN_LHS))
            assertEquals(tru, tru.pow(fal))
            assertEquals(tru, tru.pow(tbl))
            assertEquals(tbl, tbl.pow(tru))
            try {
                tbl.pow(tbl2)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                tru.sub(tbl)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            LuaBoolean.s_metatable = LuaValue.tableOf(arrayOf(LuaValue.MOD, RETURN_LHS))
            assertEquals(tru, tru.mod(fal))
            assertEquals(tru, tru.mod(tbl))
            assertEquals(tbl, tbl.mod(tru))
            try {
                tbl.mod(tbl2)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                tru.sub(tbl)
                fail("did not throw error")
            } catch (le: LuaError) {
            }


            // always use right argument
            LuaBoolean.s_metatable = LuaValue.tableOf(arrayOf(LuaValue.ADD, RETURN_RHS))
            assertEquals(fal, tru.add(fal))
            assertEquals(tbl, tru.add(tbl))
            assertEquals(tru, tbl.add(tru))
            try {
                tbl.add(tbl2)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                tru.sub(tbl)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            LuaBoolean.s_metatable = LuaValue.tableOf(arrayOf(LuaValue.SUB, RETURN_RHS))
            assertEquals(fal, tru.sub(fal))
            assertEquals(tbl, tru.sub(tbl))
            assertEquals(tru, tbl.sub(tru))
            try {
                tbl.sub(tbl2)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                tru.add(tbl)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            LuaBoolean.s_metatable = LuaValue.tableOf(arrayOf(LuaValue.MUL, RETURN_RHS))
            assertEquals(fal, tru.mul(fal))
            assertEquals(tbl, tru.mul(tbl))
            assertEquals(tru, tbl.mul(tru))
            try {
                tbl.mul(tbl2)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                tru.sub(tbl)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            LuaBoolean.s_metatable = LuaValue.tableOf(arrayOf(LuaValue.DIV, RETURN_RHS))
            assertEquals(fal, tru.div(fal))
            assertEquals(tbl, tru.div(tbl))
            assertEquals(tru, tbl.div(tru))
            try {
                tbl.div(tbl2)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                tru.sub(tbl)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            LuaBoolean.s_metatable = LuaValue.tableOf(arrayOf(LuaValue.POW, RETURN_RHS))
            assertEquals(fal, tru.pow(fal))
            assertEquals(tbl, tru.pow(tbl))
            assertEquals(tru, tbl.pow(tru))
            try {
                tbl.pow(tbl2)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                tru.sub(tbl)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            LuaBoolean.s_metatable = LuaValue.tableOf(arrayOf(LuaValue.MOD, RETURN_RHS))
            assertEquals(fal, tru.mod(fal))
            assertEquals(tbl, tru.mod(tbl))
            assertEquals(tru, tbl.mod(tru))
            try {
                tbl.mod(tbl2)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                tru.sub(tbl)
                fail("did not throw error")
            } catch (le: LuaError) {
            }


        } finally {
            LuaBoolean.s_metatable = null
        }
    }

    @Test
    fun testArithMetatagNumberTable() {
        val zero = LuaValue.ZERO
        val one = LuaValue.ONE
        val tbl = LuaTable()

        try {
            tbl.add(zero)
            fail("did not throw error")
        } catch (le: LuaError) {
        }

        try {
            zero.add(tbl)
            fail("did not throw error")
        } catch (le: LuaError) {
        }

        tbl.setmetatable(LuaValue.tableOf(arrayOf(LuaValue.ADD, RETURN_ONE)))
        assertEquals(one, tbl.add(zero))
        assertEquals(one, zero.add(tbl))

        try {
            tbl.sub(zero)
            fail("did not throw error")
        } catch (le: LuaError) {
        }

        try {
            zero.sub(tbl)
            fail("did not throw error")
        } catch (le: LuaError) {
        }

        tbl.setmetatable(LuaValue.tableOf(arrayOf(LuaValue.SUB, RETURN_ONE)))
        assertEquals(one, tbl.sub(zero))
        assertEquals(one, zero.sub(tbl))

        try {
            tbl.mul(zero)
            fail("did not throw error")
        } catch (le: LuaError) {
        }

        try {
            zero.mul(tbl)
            fail("did not throw error")
        } catch (le: LuaError) {
        }

        tbl.setmetatable(LuaValue.tableOf(arrayOf(LuaValue.MUL, RETURN_ONE)))
        assertEquals(one, tbl.mul(zero))
        assertEquals(one, zero.mul(tbl))

        try {
            tbl.div(zero)
            fail("did not throw error")
        } catch (le: LuaError) {
        }

        try {
            zero.div(tbl)
            fail("did not throw error")
        } catch (le: LuaError) {
        }

        tbl.setmetatable(LuaValue.tableOf(arrayOf(LuaValue.DIV, RETURN_ONE)))
        assertEquals(one, tbl.div(zero))
        assertEquals(one, zero.div(tbl))

        try {
            tbl.pow(zero)
            fail("did not throw error")
        } catch (le: LuaError) {
        }

        try {
            zero.pow(tbl)
            fail("did not throw error")
        } catch (le: LuaError) {
        }

        tbl.setmetatable(LuaValue.tableOf(arrayOf(LuaValue.POW, RETURN_ONE)))
        assertEquals(one, tbl.pow(zero))
        assertEquals(one, zero.pow(tbl))

        try {
            tbl.mod(zero)
            fail("did not throw error")
        } catch (le: LuaError) {
        }

        try {
            zero.mod(tbl)
            fail("did not throw error")
        } catch (le: LuaError) {
        }

        tbl.setmetatable(LuaValue.tableOf(arrayOf(LuaValue.MOD, RETURN_ONE)))
        assertEquals(one, tbl.mod(zero))
        assertEquals(one, zero.mod(tbl))
    }

    @Test
    fun testCompareStrings() {
        // these are lexical compare!
        val sa = LuaValue.valueOf("-1.5")
        val sb = LuaValue.valueOf("-2.0")
        val sc = LuaValue.valueOf("1.5")
        val sd = LuaValue.valueOf("2.0")

        assertEquals(LuaValue.BFALSE, sa.lt(sa))
        assertEquals(LuaValue.BTRUE, sa.lt(sb))
        assertEquals(LuaValue.BTRUE, sa.lt(sc))
        assertEquals(LuaValue.BTRUE, sa.lt(sd))
        assertEquals(LuaValue.BFALSE, sb.lt(sa))
        assertEquals(LuaValue.BFALSE, sb.lt(sb))
        assertEquals(LuaValue.BTRUE, sb.lt(sc))
        assertEquals(LuaValue.BTRUE, sb.lt(sd))
        assertEquals(LuaValue.BFALSE, sc.lt(sa))
        assertEquals(LuaValue.BFALSE, sc.lt(sb))
        assertEquals(LuaValue.BFALSE, sc.lt(sc))
        assertEquals(LuaValue.BTRUE, sc.lt(sd))
        assertEquals(LuaValue.BFALSE, sd.lt(sa))
        assertEquals(LuaValue.BFALSE, sd.lt(sb))
        assertEquals(LuaValue.BFALSE, sd.lt(sc))
        assertEquals(LuaValue.BFALSE, sd.lt(sd))
    }

    @Test
    fun testLt() {
        val ia = LuaValue.valueOf(3)
        val ib = LuaValue.valueOf(4)
        val da = LuaValue.valueOf(.25)
        val db = LuaValue.valueOf(.5)

        // like kinds
        assertEquals(3.0 < 4.0, ia.lt(ib).toboolean())
        assertEquals(.25 < .5, da.lt(db).toboolean())
        assertEquals(3.0 < 4.0, ia.lt_b(ib))
        assertEquals(.25 < .5, da.lt_b(db))

        // unlike kinds
        assertEquals(3.0 < .25, ia.lt(da).toboolean())
        assertEquals(.25 < 3.0, da.lt(ia).toboolean())
        assertEquals(3.0 < .25, ia.lt_b(da))
        assertEquals(.25 < 3.0, da.lt_b(ia))
    }

    @Test
    fun testLtEq() {
        val ia = LuaValue.valueOf(3)
        val ib = LuaValue.valueOf(4)
        val da = LuaValue.valueOf(.25)
        val db = LuaValue.valueOf(.5)

        // like kinds
        assertEquals(3.0 <= 4.0, ia.lteq(ib).toboolean())
        assertEquals(.25 <= .5, da.lteq(db).toboolean())
        assertEquals(3.0 <= 4.0, ia.lteq_b(ib))
        assertEquals(.25 <= .5, da.lteq_b(db))

        // unlike kinds
        assertEquals(3.0 <= .25, ia.lteq(da).toboolean())
        assertEquals(.25 <= 3.0, da.lteq(ia).toboolean())
        assertEquals(3.0 <= .25, ia.lteq_b(da))
        assertEquals(.25 <= 3.0, da.lteq_b(ia))
    }

    @Test
    fun testGt() {
        val ia = LuaValue.valueOf(3)
        val ib = LuaValue.valueOf(4)
        val da = LuaValue.valueOf(.25)
        val db = LuaValue.valueOf(.5)

        // like kinds
        assertEquals(3.0 > 4.0, ia.gt(ib).toboolean())
        assertEquals(.25 > .5, da.gt(db).toboolean())
        assertEquals(3.0 > 4.0, ia.gt_b(ib))
        assertEquals(.25 > .5, da.gt_b(db))

        // unlike kinds
        assertEquals(3.0 > .25, ia.gt(da).toboolean())
        assertEquals(.25 > 3.0, da.gt(ia).toboolean())
        assertEquals(3.0 > .25, ia.gt_b(da))
        assertEquals(.25 > 3.0, da.gt_b(ia))
    }

    @Test
    fun testGtEq() {
        val ia = LuaValue.valueOf(3)
        val ib = LuaValue.valueOf(4)
        val da = LuaValue.valueOf(.25)
        val db = LuaValue.valueOf(.5)

        // like kinds
        assertEquals(3.0 >= 4.0, ia.gteq(ib).toboolean())
        assertEquals(.25 >= .5, da.gteq(db).toboolean())
        assertEquals(3.0 >= 4.0, ia.gteq_b(ib))
        assertEquals(.25 >= .5, da.gteq_b(db))

        // unlike kinds
        assertEquals(3.0 >= .25, ia.gteq(da).toboolean())
        assertEquals(.25 >= 3.0, da.gteq(ia).toboolean())
        assertEquals(3.0 >= .25, ia.gteq_b(da))
        assertEquals(.25 >= 3.0, da.gteq_b(ia))
    }

    @Test
    fun testNotEq() {
        val ia = LuaValue.valueOf(3)
        val ib = LuaValue.valueOf(4)
        val da = LuaValue.valueOf(.25)
        val db = LuaValue.valueOf(.5)
        val sa = LuaValue.valueOf("1.5")
        val sb = LuaValue.valueOf("2.0")

        // like kinds
        assertEquals(3.0 != 4.0, ia.neq(ib).toboolean())
        assertEquals(.25 != .5, da.neq(db).toboolean())
        assertEquals(1.5 != 2.0, sa.neq(sb).toboolean())
        assertEquals(3.0 != 4.0, ia.neq_b(ib))
        assertEquals(.25 != .5, da.neq_b(db))
        assertEquals(1.5 != 2.0, sa.neq_b(sb))

        // unlike kinds
        assertEquals(3.0 != .25, ia.neq(da).toboolean())
        assertEquals(.25 != 3.0, da.neq(ia).toboolean())
        assertEquals(3.0 != 1.5, ia.neq(sa).toboolean())
        assertEquals(1.5 != 3.0, sa.neq(ia).toboolean())
        assertEquals(.25 != 1.5, da.neq(sa).toboolean())
        assertEquals(1.5 != .25, sa.neq(da).toboolean())
        assertEquals(3.0 != .25, ia.neq_b(da))
        assertEquals(.25 != 3.0, da.neq_b(ia))
        assertEquals(3.0 != 1.5, ia.neq_b(sa))
        assertEquals(1.5 != 3.0, sa.neq_b(ia))
        assertEquals(.25 != 1.5, da.neq_b(sa))
        assertEquals(1.5 != .25, sa.neq_b(da))
    }

    @Test
    fun testCompareErrors() {
        val ia = LuaValue.valueOf(111)
        val ib = LuaValue.valueOf(44)
        val da = LuaValue.valueOf(55.25)
        val db = LuaValue.valueOf(3.5)
        val sa = LuaValue.valueOf("22.125")
        val sb = LuaValue.valueOf("7.25")

        val ops = arrayOf("lt", "lteq")
        val vals = arrayOf(LuaValue.NIL, LuaValue.BTRUE, LuaValue.tableOf())
        val numerics = arrayOf(LuaValue.valueOf(111), LuaValue.valueOf(55.25), LuaValue.valueOf("22.125"))
        for (i in ops.indices) {
            for (j in vals.indices) {
                for (k in numerics.indices) {
                    checkCompareError(vals[j], numerics[k], ops[i], vals[j].typename())
                    checkCompareError(numerics[k], vals[j], ops[i], vals[j].typename())
                }
            }
        }
    }

    private fun checkCompareError(a: LuaValue, b: LuaValue, op: String, type: String) {
        try {
            LuaValue::class.java.getMethod(op, *arrayOf<Class<*>>(LuaValue::class.java)).invoke(a, *arrayOf<Any>(b))
        } catch (ite: InvocationTargetException) {
            val actual = ite.targetException.message!!
            if (!actual.startsWith("attempt to compare") || actual.indexOf(type) < 0)
                fail("(" + a.typename() + "," + op + "," + b.typename() + ") reported '" + actual + "'")
        } catch (e: Exception) {
            fail("(" + a.typename() + "," + op + "," + b.typename() + ") threw " + e)
        }

    }

    @Test
    fun testCompareMetatag() {
        val tru = LuaValue.BTRUE
        val fal = LuaValue.BFALSE
        val tbl = LuaTable()
        val tbl2 = LuaTable()
        val tbl3 = LuaTable()
        try {
            // always use left argument
            var mt: LuaValue = LuaValue.tableOf(arrayOf(LuaValue.LT, RETURN_LHS, LuaValue.LE, RETURN_RHS))
            LuaBoolean.s_metatable = mt
            tbl.setmetatable(mt)
            tbl2.setmetatable(mt)
            assertEquals(tru, tru.lt(fal))
            assertEquals(fal, fal.lt(tru))
            assertEquals(tbl, tbl.lt(tbl2))
            assertEquals(tbl2, tbl2.lt(tbl))
            assertEquals(tbl, tbl.lt(tbl3))
            assertEquals(tbl3, tbl3.lt(tbl))
            assertEquals(fal, tru.lteq(fal))
            assertEquals(tru, fal.lteq(tru))
            assertEquals(tbl2, tbl.lteq(tbl2))
            assertEquals(tbl, tbl2.lteq(tbl))
            assertEquals(tbl3, tbl.lteq(tbl3))
            assertEquals(tbl, tbl3.lteq(tbl))

            // always use right argument
            mt = LuaValue.tableOf(arrayOf(LuaValue.LT, RETURN_RHS, LuaValue.LE, RETURN_LHS))
            LuaBoolean.s_metatable = mt
            tbl.setmetatable(mt)
            tbl2.setmetatable(mt)
            assertEquals(fal, tru.lt(fal))
            assertEquals(tru, fal.lt(tru))
            assertEquals(tbl2, tbl.lt(tbl2))
            assertEquals(tbl, tbl2.lt(tbl))
            assertEquals(tbl3, tbl.lt(tbl3))
            assertEquals(tbl, tbl3.lt(tbl))
            assertEquals(tru, tru.lteq(fal))
            assertEquals(fal, fal.lteq(tru))
            assertEquals(tbl, tbl.lteq(tbl2))
            assertEquals(tbl2, tbl2.lteq(tbl))
            assertEquals(tbl, tbl.lteq(tbl3))
            assertEquals(tbl3, tbl3.lteq(tbl))

        } finally {
            LuaBoolean.s_metatable = null
        }
    }

    @Test
    fun testAnd() {
        val ia = LuaValue.valueOf(3)
        val ib = LuaValue.valueOf(4)
        val da = LuaValue.valueOf(.25)
        val db = LuaValue.valueOf(.5)
        val sa = LuaValue.valueOf("1.5")
        val sb = LuaValue.valueOf("2.0")
        val ba = LuaValue.BTRUE
        val bb = LuaValue.BFALSE

        // like kinds
        assertSame(ib, ia.and(ib))
        assertSame(db, da.and(db))
        assertSame(sb, sa.and(sb))

        // unlike kinds
        assertSame(da, ia.and(da))
        assertSame(ia, da.and(ia))
        assertSame(sa, ia.and(sa))
        assertSame(ia, sa.and(ia))
        assertSame(sa, da.and(sa))
        assertSame(da, sa.and(da))

        // boolean values
        assertSame(bb, ba.and(bb))
        assertSame(bb, bb.and(ba))
        assertSame(ia, ba.and(ia))
        assertSame(bb, bb.and(ia))
    }

    @Test
    fun testOr() {
        val ia = LuaValue.valueOf(3)
        val ib = LuaValue.valueOf(4)
        val da = LuaValue.valueOf(.25)
        val db = LuaValue.valueOf(.5)
        val sa = LuaValue.valueOf("1.5")
        val sb = LuaValue.valueOf("2.0")
        val ba = LuaValue.BTRUE
        val bb = LuaValue.BFALSE

        // like kinds
        assertSame(ia, ia.or(ib))
        assertSame(da, da.or(db))
        assertSame(sa, sa.or(sb))

        // unlike kinds
        assertSame(ia, ia.or(da))
        assertSame(da, da.or(ia))
        assertSame(ia, ia.or(sa))
        assertSame(sa, sa.or(ia))
        assertSame(da, da.or(sa))
        assertSame(sa, sa.or(da))

        // boolean values
        assertSame(ba, ba.or(bb))
        assertSame(ba, bb.or(ba))
        assertSame(ba, ba.or(ia))
        assertSame(ia, bb.or(ia))
    }

    @Test
    fun testLexicalComparison() {
        val aaa = LuaValue.valueOf("aaa")
        val baa = LuaValue.valueOf("baa")
        val Aaa = LuaValue.valueOf("Aaa")
        val aba = LuaValue.valueOf("aba")
        val aaaa = LuaValue.valueOf("aaaa")
        val t = LuaValue.BTRUE
        val f = LuaValue.BFALSE

        // basics
        assertEquals(t, aaa.eq(aaa))
        assertEquals(t, aaa.lt(baa))
        assertEquals(t, aaa.lteq(baa))
        assertEquals(f, aaa.gt(baa))
        assertEquals(f, aaa.gteq(baa))
        assertEquals(f, baa.lt(aaa))
        assertEquals(f, baa.lteq(aaa))
        assertEquals(t, baa.gt(aaa))
        assertEquals(t, baa.gteq(aaa))
        assertEquals(t, aaa.lteq(aaa))
        assertEquals(t, aaa.gteq(aaa))

        // different case
        assertEquals(t, Aaa.eq(Aaa))
        assertEquals(t, Aaa.lt(aaa))
        assertEquals(t, Aaa.lteq(aaa))
        assertEquals(f, Aaa.gt(aaa))
        assertEquals(f, Aaa.gteq(aaa))
        assertEquals(f, aaa.lt(Aaa))
        assertEquals(f, aaa.lteq(Aaa))
        assertEquals(t, aaa.gt(Aaa))
        assertEquals(t, aaa.gteq(Aaa))
        assertEquals(t, Aaa.lteq(Aaa))
        assertEquals(t, Aaa.gteq(Aaa))

        // second letter differs
        assertEquals(t, aaa.eq(aaa))
        assertEquals(t, aaa.lt(aba))
        assertEquals(t, aaa.lteq(aba))
        assertEquals(f, aaa.gt(aba))
        assertEquals(f, aaa.gteq(aba))
        assertEquals(f, aba.lt(aaa))
        assertEquals(f, aba.lteq(aaa))
        assertEquals(t, aba.gt(aaa))
        assertEquals(t, aba.gteq(aaa))
        assertEquals(t, aaa.lteq(aaa))
        assertEquals(t, aaa.gteq(aaa))

        // longer
        assertEquals(t, aaa.eq(aaa))
        assertEquals(t, aaa.lt(aaaa))
        assertEquals(t, aaa.lteq(aaaa))
        assertEquals(f, aaa.gt(aaaa))
        assertEquals(f, aaa.gteq(aaaa))
        assertEquals(f, aaaa.lt(aaa))
        assertEquals(f, aaaa.lteq(aaa))
        assertEquals(t, aaaa.gt(aaa))
        assertEquals(t, aaaa.gteq(aaa))
        assertEquals(t, aaa.lteq(aaa))
        assertEquals(t, aaa.gteq(aaa))
    }

    @Test
    fun testBuffer() {
        val abc = LuaValue.valueOf("abcdefghi").substring(0, 3)
        val def = LuaValue.valueOf("abcdefghi").substring(3, 6)
        val ghi = LuaValue.valueOf("abcdefghi").substring(6, 9)
        val n123 = LuaValue.valueOf(123)

        // basic append
        var b = Buffer()
        assertEquals("", b.value().tojstring())
        b.append(def)
        assertEquals("def", b.value().tojstring())
        b.append(abc)
        assertEquals("defabc", b.value().tojstring())
        b.append(ghi)
        assertEquals("defabcghi", b.value().tojstring())
        b.append(n123)
        assertEquals("defabcghi123", b.value().tojstring())

        // basic prepend
        b = Buffer()
        assertEquals("", b.value().tojstring())
        b.prepend(def.strvalue()!!)
        assertEquals("def", b.value().tojstring())
        b.prepend(ghi.strvalue()!!)
        assertEquals("ghidef", b.value().tojstring())
        b.prepend(abc.strvalue()!!)
        assertEquals("abcghidef", b.value().tojstring())
        b.prepend(n123.strvalue()!!)
        assertEquals("123abcghidef", b.value().tojstring())

        // mixed append, prepend
        b = Buffer()
        assertEquals("", b.value().tojstring())
        b.append(def)
        assertEquals("def", b.value().tojstring())
        b.append(abc)
        assertEquals("defabc", b.value().tojstring())
        b.prepend(ghi.strvalue()!!)
        assertEquals("ghidefabc", b.value().tojstring())
        b.prepend(n123.strvalue()!!)
        assertEquals("123ghidefabc", b.value().tojstring())
        b.append(def)
        assertEquals("123ghidefabcdef", b.value().tojstring())
        b.append(abc)
        assertEquals("123ghidefabcdefabc", b.value().tojstring())
        b.prepend(ghi.strvalue()!!)
        assertEquals("ghi123ghidefabcdefabc", b.value().tojstring())
        b.prepend(n123.strvalue()!!)
        assertEquals("123ghi123ghidefabcdefabc", b.value().tojstring())

        // value
        b = Buffer(def)
        assertEquals("def", b.value().tojstring())
        b.append(abc)
        assertEquals("defabc", b.value().tojstring())
        b.prepend(ghi.strvalue()!!)
        assertEquals("ghidefabc", b.value().tojstring())
        b.setvalue(def)
        assertEquals("def", b.value().tojstring())
        b.prepend(ghi.strvalue()!!)
        assertEquals("ghidef", b.value().tojstring())
        b.append(abc)
        assertEquals("ghidefabc", b.value().tojstring())
    }

    @Test
    fun testConcat() {
        val abc = LuaValue.valueOf("abcdefghi").substring(0, 3)
        val def = LuaValue.valueOf("abcdefghi").substring(3, 6)
        val ghi = LuaValue.valueOf("abcdefghi").substring(6, 9)
        val n123 = LuaValue.valueOf(123)

        assertEquals("abc", abc.tojstring())
        assertEquals("def", def.tojstring())
        assertEquals("ghi", ghi.tojstring())
        assertEquals("123", n123.tojstring())
        assertEquals("abcabc", abc.concat(abc).tojstring())
        assertEquals("defghi", def.concat(ghi).tojstring())
        assertEquals("ghidef", ghi.concat(def).tojstring())
        assertEquals("ghidefabcghi", ghi.concat(def).concat(abc).concat(ghi).tojstring())
        assertEquals("123def", n123.concat(def).tojstring())
        assertEquals("def123", def.concat(n123).tojstring())
    }

    @Test
    fun testConcatBuffer() {
        val abc = LuaValue.valueOf("abcdefghi").substring(0, 3)
        val def = LuaValue.valueOf("abcdefghi").substring(3, 6)
        val ghi = LuaValue.valueOf("abcdefghi").substring(6, 9)
        val n123 = LuaValue.valueOf(123)
        var b: Buffer

        b = Buffer(def)
        assertEquals("def", b.value().tojstring())
        b = ghi.concat(b)
        assertEquals("ghidef", b.value().tojstring())
        b = abc.concat(b)
        assertEquals("abcghidef", b.value().tojstring())
        b = n123.concat(b)
        assertEquals("123abcghidef", b.value().tojstring())
        b.setvalue(n123)
        b = def.concat(b)
        assertEquals("def123", b.value().tojstring())
        b = abc.concat(b)
        assertEquals("abcdef123", b.value().tojstring())
    }

    @Test
    fun testConcatMetatag() {
        val def = LuaValue.valueOf("abcdefghi").substring(3, 6)
        val ghi = LuaValue.valueOf("abcdefghi").substring(6, 9)
        val tru = LuaValue.BTRUE
        val fal = LuaValue.BFALSE
        val tbl = LuaTable()
        val uda = LuaUserdata(Any())
        try {
            // always use left argument
            LuaBoolean.s_metatable = LuaValue.tableOf(arrayOf(LuaValue.CONCAT, RETURN_LHS))
            assertEquals(tru, tru.concat(tbl))
            assertEquals(tbl, tbl.concat(tru))
            assertEquals(tru, tru.concat(tbl))
            assertEquals(tbl, tbl.concat(tru))
            assertEquals(tru, tru.concat(tbl.buffer()).value())
            assertEquals(tbl, tbl.concat(tru.buffer()).value())
            assertEquals(fal, fal.concat(tbl.concat(tru.buffer())).value())
            assertEquals(uda, uda.concat(tru.concat(tbl.buffer())).value())
            try {
                tbl.concat(def)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                def.concat(tbl)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                tbl.concat(def.buffer()).value()
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                def.concat(tbl.buffer()).value()
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                uda.concat(def.concat(tbl.buffer())).value()
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                ghi.concat(tbl.concat(def.buffer())).value()
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            // always use right argument
            LuaBoolean.s_metatable = LuaValue.tableOf(arrayOf(LuaValue.CONCAT, RETURN_RHS))
            assertEquals(tbl, tru.concat(tbl))
            assertEquals(tru, tbl.concat(tru))
            assertEquals(tbl, tru.concat(tbl.buffer()).value())
            assertEquals(tru, tbl.concat(tru.buffer()).value())
            assertEquals(tru, uda.concat(tbl.concat(tru.buffer())).value())
            assertEquals(tbl, fal.concat(tru.concat(tbl.buffer())).value())
            try {
                tbl.concat(def)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                def.concat(tbl)
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                tbl.concat(def.buffer()).value()
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                def.concat(tbl.buffer()).value()
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                uda.concat(def.concat(tbl.buffer())).value()
                fail("did not throw error")
            } catch (le: LuaError) {
            }

            try {
                uda.concat(tbl.concat(def.buffer())).value()
                fail("did not throw error")
            } catch (le: LuaError) {
            }

        } finally {
            LuaBoolean.s_metatable = null
        }
    }

    @Test
    fun testConcatErrors() {
        val ia = LuaValue.valueOf(111)
        val ib = LuaValue.valueOf(44)
        val da = LuaValue.valueOf(55.25)
        val db = LuaValue.valueOf(3.5)
        val sa = LuaValue.valueOf("22.125")
        val sb = LuaValue.valueOf("7.25")

        val ops = arrayOf("concat")
        val vals = arrayOf(LuaValue.NIL, LuaValue.BTRUE, LuaValue.tableOf())
        val numerics = arrayOf(LuaValue.valueOf(111), LuaValue.valueOf(55.25), LuaValue.valueOf("22.125"))
        for (i in ops.indices) {
            for (j in vals.indices) {
                for (k in numerics.indices) {
                    checkConcatError(vals[j], numerics[k], ops[i], vals[j].typename())
                    checkConcatError(numerics[k], vals[j], ops[i], vals[j].typename())
                }
            }
        }
    }

    private fun checkConcatError(a: LuaValue, b: LuaValue, op: String, type: String) {
        try {
            LuaValue::class.java.getMethod(op, *arrayOf<Class<*>>(LuaValue::class.java)).invoke(a, *arrayOf<Any>(b))
        } catch (ite: InvocationTargetException) {
            val actual = ite.targetException.message!!
            if (!actual.startsWith("attempt to concatenate") || actual.indexOf(type) < 0)
                fail("(" + a.typename() + "," + op + "," + b.typename() + ") reported '" + actual + "'")
        } catch (e: Exception) {
            fail("(" + a.typename() + "," + op + "," + b.typename() + ") threw " + e)
        }

    }

    companion object {

        private val RETURN_NIL = object : TwoArgFunction() {
            override fun call(lhs: LuaValue, rhs: LuaValue): LuaValue = LuaValue.NIL
        }

        private val RETURN_ONE = object : TwoArgFunction() {
            override fun call(lhs: LuaValue, rhs: LuaValue): LuaValue = LuaValue.ONE
        }

        private fun luaMod(x: Double, y: Double): Double {
            return if (y != 0.0) x - y * Math.floor(x / y) else java.lang.Double.NaN
        }

        private val RETURN_LHS = object : TwoArgFunction() {
            override fun call(lhs: LuaValue, rhs: LuaValue): LuaValue = lhs
        }

        private val RETURN_RHS = object : TwoArgFunction() {
            override fun call(lhs: LuaValue, rhs: LuaValue): LuaValue = rhs
        }
    }

}
