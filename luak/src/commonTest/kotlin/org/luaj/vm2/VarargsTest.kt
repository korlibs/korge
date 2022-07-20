package org.luaj.vm2

import kotlin.test.*

/**
 * Tests of basic unary and binary operators on main value types.
 */
class VarargsTest {

    @Test
    fun testSanity() {
        expectEquals(A_G, A_G)
        expectEquals(A_G_alt, A_G_alt)
        expectEquals(A_G, A_G_alt)
        expectEquals(B_E, B_E_alt)
        expectEquals(C_G, C_G_alt)
        expectEquals(C_E, C_E_alt)
        expectEquals(C_E, C_E_alt2)
        expectEquals(DE, DE_alt)
        expectEquals(DE, DE_alt2)
        expectEquals(E_G, E_G_alt)
        expectEquals(FG, FG_alt)
        expectEquals(FG_alt, FG_alt)
        expectEquals(A, A)
        expectEquals(NONE, NONE)
        expectEquals(NIL, NIL)
    }

    @Test
    fun testNegativeIndices() {
        expectNegSubargsError(A_G)
        expectNegSubargsError(A_G_alt)
        expectNegSubargsError(B_E)
        expectNegSubargsError(B_E_alt)
        expectNegSubargsError(C_G)
        expectNegSubargsError(C_G_alt)
        expectNegSubargsError(C_E)
        expectNegSubargsError(C_E_alt)
        expectNegSubargsError(C_E_alt2)
        expectNegSubargsError(DE)
        expectNegSubargsError(DE_alt)
        expectNegSubargsError(DE_alt2)
        expectNegSubargsError(E_G)
        expectNegSubargsError(FG)
        expectNegSubargsError(A)
        expectNegSubargsError(NONE)
        expectNegSubargsError(NIL)
    }

    @Test
    fun testVarargsSubargs() {
        standardTestsA_G(A_G)
        standardTestsA_G(A_G_alt)
        standardTestsC_G(C_G)
        standardTestsC_G(C_G_alt)
        standardTestsE_G(E_G)
        standardTestsE_G(E_G_alt)
        standardTestsFG(FG)
        standardTestsFG(FG_alt)
        standardTestsNone(NONE)
    }

    @Test
    fun testVarargsMore() {
        var a_g: Varargs
        a_g = LuaValue.varargsOf(arrayOf(A), LuaValue.varargsOf(arrayOf(B, C, D, E, F, G)))
        standardTestsA_G(a_g)
        a_g = LuaValue.varargsOf(arrayOf(A, B), LuaValue.varargsOf(arrayOf(C, D, E, F, G)))
        standardTestsA_G(a_g)
        a_g = LuaValue.varargsOf(arrayOf(A, B, C), LuaValue.varargsOf(arrayOf(D, E, F, G)))
        standardTestsA_G(a_g)
        a_g = LuaValue.varargsOf(arrayOf(A, B, C, D), LuaValue.varargsOf(E, F, G))
        standardTestsA_G(a_g)
        a_g = LuaValue.varargsOf(arrayOf(A, B, C, D, E), LuaValue.varargsOf(F, G))
        standardTestsA_G(a_g)
        a_g = LuaValue.varargsOf(arrayOf(A, B, C, D, E, F), G)
        standardTestsA_G(a_g)
    }

    @Test
    fun testPairVarargsMore() {
        val a_g = Varargs.PairVarargs(
            A,
            Varargs.PairVarargs(
                B,
                Varargs.PairVarargs(
                    C,
                    Varargs.PairVarargs(
                        D,
                        Varargs.PairVarargs(
                            E,
                            Varargs.PairVarargs(F, G)
                        )
                    )
                )
            )
        )
        standardTestsA_G(a_g)
    }

    @Test
    fun testArrayPartMore() {
        var a_g: Varargs
        a_g = Varargs.ArrayPartVarargs(Z_H_array, 1, 1, Varargs.ArrayPartVarargs(Z_H_array, 2, 6))
        standardTestsA_G(a_g)
        a_g = Varargs.ArrayPartVarargs(Z_H_array, 1, 2, Varargs.ArrayPartVarargs(Z_H_array, 3, 5))
        standardTestsA_G(a_g)
        a_g = Varargs.ArrayPartVarargs(Z_H_array, 1, 3, Varargs.ArrayPartVarargs(Z_H_array, 4, 4))
        standardTestsA_G(a_g)
        a_g = Varargs.ArrayPartVarargs(Z_H_array, 1, 4, Varargs.ArrayPartVarargs(Z_H_array, 5, 3))
        standardTestsA_G(a_g)
        a_g = Varargs.ArrayPartVarargs(Z_H_array, 1, 5, Varargs.ArrayPartVarargs(Z_H_array, 6, 2))
        standardTestsA_G(a_g)
        a_g = Varargs.ArrayPartVarargs(Z_H_array, 1, 6, Varargs.ArrayPartVarargs(Z_H_array, 7, 1))
        standardTestsA_G(a_g)
    }

    companion object {

        internal var A: LuaValue = LuaValue.valueOf("a")
        internal var B: LuaValue = LuaValue.valueOf("b")
        internal var C: LuaValue = LuaValue.valueOf("c")
        internal var D: LuaValue = LuaValue.valueOf("d")
        internal var E: LuaValue = LuaValue.valueOf("e")
        internal var F: LuaValue = LuaValue.valueOf("f")
        internal var G: LuaValue = LuaValue.valueOf("g")
        internal var H: LuaValue = LuaValue.valueOf("h")
        internal var Z: LuaValue = LuaValue.valueOf("z")
        internal var NIL = LuaValue.NIL
        internal var A_G = LuaValue.varargsOf(arrayOf(A, B, C, D, E, F, G))
        internal var B_E = LuaValue.varargsOf(arrayOf(B, C, D, E))
        internal var C_G = LuaValue.varargsOf(arrayOf(C, D, E, F, G))
        internal var C_E = LuaValue.varargsOf(arrayOf(C, D, E))
        internal var DE = LuaValue.varargsOf(arrayOf(D, E))
        internal var E_G = LuaValue.varargsOf(arrayOf(E, F, G))
        internal var FG = LuaValue.varargsOf(arrayOf(F, G))
        internal var Z_H_array = arrayOf(Z, A, B, C, D, E, F, G, H)
        internal var A_G_alt: Varargs = Varargs.ArrayPartVarargs(Z_H_array, 1, 7)
        internal var B_E_alt: Varargs = Varargs.ArrayPartVarargs(Z_H_array, 2, 4)
        internal var C_G_alt: Varargs = Varargs.ArrayPartVarargs(Z_H_array, 3, 5)
        internal var C_E_alt: Varargs = Varargs.ArrayPartVarargs(Z_H_array, 3, 3)
        internal var C_E_alt2 = LuaValue.varargsOf(C, D, E)
        internal var DE_alt: Varargs = Varargs.PairVarargs(D, E)
        internal var DE_alt2 = LuaValue.varargsOf(D, E)
        internal var E_G_alt: Varargs = Varargs.ArrayPartVarargs(Z_H_array, 5, 3)
        internal var FG_alt: Varargs = Varargs.PairVarargs(F, G)
        internal var NONE: Varargs = LuaValue.NONE

        internal fun expectEquals(x: Varargs, y: Varargs) {
            assertEquals(x.narg(), y.narg())
            assertEquals(x.arg1(), y.arg1())
            assertEquals(x.arg(0), y.arg(0))
            assertEquals(x.arg(-1), y.arg(-1))
            assertEquals(x.arg(2), y.arg(2))
            assertEquals(x.arg(3), y.arg(3))
            for (i in 4 until x.narg() + 2)
                assertEquals(x.arg(i), y.arg(i))
        }

        internal fun standardTestsA_G(a_g: Varargs) {
            expectEquals(A_G, a_g)
            expectEquals(A_G, a_g.subargs(1))
            expectEquals(C_G, a_g.subargs(3).subargs(1))
            expectEquals(E_G, a_g.subargs(5))
            expectEquals(E_G, a_g.subargs(5).subargs(1))
            expectEquals(FG, a_g.subargs(6))
            expectEquals(FG, a_g.subargs(6).subargs(1))
            expectEquals(G, a_g.subargs(7))
            expectEquals(G, a_g.subargs(7).subargs(1))
            expectEquals(NONE, a_g.subargs(8))
            expectEquals(NONE, a_g.subargs(8).subargs(1))
            standardTestsC_G(A_G.subargs(3))
        }

        internal fun standardTestsC_G(c_g: Varargs) {
            expectEquals(C_G, c_g.subargs(1))
            expectEquals(E_G, c_g.subargs(3))
            expectEquals(E_G, c_g.subargs(3).subargs(1))
            expectEquals(FG, c_g.subargs(4))
            expectEquals(FG, c_g.subargs(4).subargs(1))
            expectEquals(G, c_g.subargs(5))
            expectEquals(G, c_g.subargs(5).subargs(1))
            expectEquals(NONE, c_g.subargs(6))
            expectEquals(NONE, c_g.subargs(6).subargs(1))
            standardTestsE_G(c_g.subargs(3))
        }

        internal fun standardTestsE_G(e_g: Varargs) {
            expectEquals(E_G, e_g.subargs(1))
            expectEquals(FG, e_g.subargs(2))
            expectEquals(FG, e_g.subargs(2).subargs(1))
            expectEquals(G, e_g.subargs(3))
            expectEquals(G, e_g.subargs(3).subargs(1))
            expectEquals(NONE, e_g.subargs(4))
            expectEquals(NONE, e_g.subargs(4).subargs(1))
            standardTestsFG(e_g.subargs(2))
        }

        internal fun standardTestsFG(fg: Varargs) {
            expectEquals(FG, fg.subargs(1))
            expectEquals(G, fg.subargs(2))
            expectEquals(G, fg.subargs(2).subargs(1))
            expectEquals(NONE, fg.subargs(3))
            expectEquals(NONE, fg.subargs(3).subargs(1))
        }

        internal fun standardTestsNone(none: Varargs) {
            expectEquals(NONE, none.subargs(1))
            expectEquals(NONE, none.subargs(2))
        }

        internal fun expectNegSubargsError(v: Varargs) {
            val expected_msg = "bad argument #1: start must be > 0"
            try {
                v.subargs(0)
                fail("Failed to throw exception for index 0")
            } catch (e: LuaError) {
                assertEquals(expected_msg, e.message)
            }

            try {
                v.subargs(-1)
                fail("Failed to throw exception for index -1")
            } catch (e: LuaError) {
                assertEquals(expected_msg, e.message)
            }

        }
    }
}
