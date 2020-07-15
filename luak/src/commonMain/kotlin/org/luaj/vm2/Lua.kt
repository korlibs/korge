/*******************************************************************************
 * Copyright (c) 2009-2011 Luaj.org. All rights reserved.
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


/**
 * Constants for lua limits and opcodes.
 *
 *
 * This is a direct translation of C lua distribution header file constants
 * for bytecode creation and processing.
 */
open class Lua {
    companion object {
        /** version is supplied by ant build task  */
        const val _VERSION = "Luaj 0.0"

        /** use return values from previous op  */
        const val LUA_MULTRET = -1

        // from lopcodes.h

        /*===========================================================================
	  We assume that instructions are unsigned numbers.
	  All instructions have an opcode in the first 6 bits.
	  Instructions can have the following fields:
		`A' : 8 bits
		`B' : 9 bits
		`C' : 9 bits
		`Bx' : 18 bits (`B' and `C' together)
		`sBx' : signed Bx

	  A signed argument is represented in excess K; that is, the number
	  value is the unsigned value minus K. K is exactly the maximum value
	  for that argument (so that -max is represented by 0, and +max is
	  represented by 2*max), which is half the maximum for the corresponding
	  unsigned argument.
	===========================================================================*/


        /* basic instruction format */
        @kotlin.jvm.JvmField
        val iABC = 0
        @kotlin.jvm.JvmField
        val iABx = 1
        @kotlin.jvm.JvmField
        val iAsBx = 2
        @kotlin.jvm.JvmField
        val iAx = 3


        /*
	** size and position of opcode arguments.
	*/
        const val SIZE_C = 9
        const val SIZE_B = 9
        const val SIZE_Bx = SIZE_C + SIZE_B
        const val SIZE_A = 8
        const val SIZE_Ax = SIZE_C + SIZE_B + SIZE_A

        const val SIZE_OP = 6

        const val POS_OP = 0
        const val POS_A = POS_OP + SIZE_OP
        const val POS_C = POS_A + SIZE_A
        const val POS_B = POS_C + SIZE_C
        const val POS_Bx = POS_C
        const val POS_Ax = POS_A


        const val MAX_OP = (1 shl SIZE_OP) - 1
        const val MAXARG_A = (1 shl SIZE_A) - 1
        const val MAXARG_B = (1 shl SIZE_B) - 1
        const val MAXARG_C = (1 shl SIZE_C) - 1
        const val MAXARG_Bx = (1 shl SIZE_Bx) - 1
        const val MAXARG_sBx = MAXARG_Bx shr 1        /* `sBx' is signed */
        const val MAXARG_Ax = (1 shl SIZE_Ax) - 1

        const val MASK_OP = (1 shl SIZE_OP) - 1 shl POS_OP
        const val MASK_A = (1 shl SIZE_A) - 1 shl POS_A
        const val MASK_B = (1 shl SIZE_B) - 1 shl POS_B
        const val MASK_C = (1 shl SIZE_C) - 1 shl POS_C
        const val MASK_Bx = (1 shl SIZE_Bx) - 1 shl POS_Bx

        const val MASK_NOT_OP = MASK_OP.inv()
        const val MASK_NOT_A = MASK_A.inv()
        const val MASK_NOT_B = MASK_B.inv()
        const val MASK_NOT_C = MASK_C.inv()
        const val MASK_NOT_Bx = MASK_Bx.inv()

        /*
	** the following macros help to manipulate instructions
	*/
         fun GET_OPCODE(i: Int): Int {
            return i shr POS_OP and MAX_OP
        }

         fun GETARG_A(i: Int): Int {
            return i shr POS_A and MAXARG_A
        }

         fun GETARG_Ax(i: Int): Int {
            return i shr POS_Ax and MAXARG_Ax
        }

         fun GETARG_B(i: Int): Int {
            return i shr POS_B and MAXARG_B
        }

         fun GETARG_C(i: Int): Int {
            return i shr POS_C and MAXARG_C
        }

         fun GETARG_Bx(i: Int): Int {
            return i shr POS_Bx and MAXARG_Bx
        }

         fun GETARG_sBx(i: Int): Int {
            return (i shr POS_Bx and MAXARG_Bx) - MAXARG_sBx
        }


        /*
	** Macros to operate RK indices
	*/

        /** this bit 1 means constant (0 means register)  */
        @kotlin.jvm.JvmField val BITRK = 1 shl SIZE_B - 1

        /** test whether value is a constant  */
         fun ISK(x: Int): Boolean {
            return 0 != x and BITRK
        }

        /** gets the index of the constant  */
         fun INDEXK(r: Int): Int {
            return r and BITRK.inv()
        }

        @kotlin.jvm.JvmField val MAXINDEXRK = BITRK - 1

        /** code a constant index as a RK value  */
         fun RKASK(x: Int): Int {
            return x or BITRK
        }


        /**
         * invalid register that fits in 8 bits
         */
        const val NO_REG = MAXARG_A


        /*
	** R(x) - register
	** Kst(x) - constant (in constant table)
	** RK(x) == if ISK(x) then Kst(INDEXK(x)) else R(x)
	*/


        /*
	** grep "ORDER OP" if you change these enums
	*/

        /*----------------------------------------------------------------------
	name		args	description
	------------------------------------------------------------------------*/
        const val OP_MOVE = 0/*	A B	R(A) := R(B)					*/
        const val OP_LOADK = 1/*	A Bx	R(A) := Kst(Bx)					*/
        const val OP_LOADKX = 2/*	A 	R(A) := Kst(extra arg)					*/
        const val OP_LOADBOOL = 3/*	A B C	R(A) := (Bool)B; if (C) pc++			*/
        const val OP_LOADNIL = 4 /*	A B	R(A) := ... := R(A+B) := nil			*/
        const val OP_GETUPVAL = 5 /*	A B	R(A) := UpValue[B]				*/

        const val OP_GETTABUP = 6 /*	A B C	R(A) := UpValue[B][RK(C)]			*/
        const val OP_GETTABLE = 7 /*	A B C	R(A) := R(B)[RK(C)]				*/

        const val OP_SETTABUP = 8 /*	A B C	UpValue[A][RK(B)] := RK(C)			*/
        const val OP_SETUPVAL = 9 /*	A B	UpValue[B] := R(A)				*/
        const val OP_SETTABLE = 10 /*	A B C	R(A)[RK(B)] := RK(C)				*/

        const val OP_NEWTABLE = 11 /*	A B C	R(A) := {} (size = B,C)				*/

        const val OP_SELF = 12 /*	A B C	R(A+1) := R(B); R(A) := R(B)[RK(C)]		*/

        const val OP_ADD = 13 /*	A B C	R(A) := RK(B) + RK(C)				*/
        const val OP_SUB = 14 /*	A B C	R(A) := RK(B) - RK(C)				*/
        const val OP_MUL = 15 /*	A B C	R(A) := RK(B) * RK(C)				*/
        const val OP_DIV = 16 /*	A B C	R(A) := RK(B) / RK(C)				*/
        const val OP_MOD = 17 /*	A B C	R(A) := RK(B) % RK(C)				*/
        const val OP_POW = 18 /*	A B C	R(A) := RK(B) ^ RK(C)				*/
        const val OP_UNM = 19 /*	A B	R(A) := -R(B)					*/
        const val OP_NOT = 20 /*	A B	R(A) := not R(B)				*/
        const val OP_LEN = 21 /*	A B	R(A) := length of R(B)				*/

        const val OP_CONCAT = 22 /*	A B C	R(A) := R(B).. ... ..R(C)			*/

        const val OP_JMP = 23 /*	sBx	pc+=sBx					*/
        const val OP_EQ = 24 /*	A B C	if ((RK(B) == RK(C)) ~= A) then pc++		*/
        const val OP_LT = 25 /*	A B C	if ((RK(B) <  RK(C)) ~= A) then pc++  		*/
        const val OP_LE = 26 /*	A B C	if ((RK(B) <= RK(C)) ~= A) then pc++  		*/

        const val OP_TEST = 27 /*	A C	if not (R(A) <=> C) then pc++			*/
        const val OP_TESTSET = 28 /*	A B C	if (R(B) <=> C) then R(A) := R(B) else pc++	*/

        const val OP_CALL = 29 /*	A B C	R(A), ... ,R(A+C-2) := R(A)(R(A+1), ... ,R(A+B-1)) */
        const val OP_TAILCALL = 30 /*	A B C	return R(A)(R(A+1), ... ,R(A+B-1))		*/
        const val OP_RETURN = 31 /*	A B	return R(A), ... ,R(A+B-2)	(see note)	*/

        const val OP_FORLOOP = 32 /*	A sBx	R(A)+=R(A+2); if R(A) <?= R(A+1) then { pc+=sBx; R(A+3)=R(A) }*/
        const val OP_FORPREP = 33 /*	A sBx	R(A)-=R(A+2); pc+=sBx				*/

        const val OP_TFORCALL = 34 /* A C	R(A+3), ... ,R(A+2+C) := R(A)(R(A+1), R(A+2));	*/
        const val OP_TFORLOOP = 35 /* A sBx   if R(A+1) ~= nil then { R(A)=R(A+1); pc += sBx } */
        const val OP_SETLIST = 36 /*	A B C	R(A)[(C-1)*FPF+i] := R(A+i), 1 <= i <= B	*/

        const val OP_CLOSURE = 37 /*	A Bx	R(A) := closure(KPROTO[Bx], R(A), ... ,R(A+n))	*/

        const val OP_VARARG = 38 /*	A B	R(A), R(A+1), ..., R(A+B-1) = vararg		*/

        const val OP_EXTRAARG = 39 /* Ax	extra (larger) argument for previous opcode	*/

        const val NUM_OPCODES = OP_EXTRAARG + 1

        /* pseudo-opcodes used in parsing only.  */
        const val OP_GT = 63 // >
        const val OP_GE = 62 // >=
        const val OP_NEQ = 61 // ~=
        const val OP_AND = 60 // and
        const val OP_OR = 59 // or

        /*===========================================================================
	  Notes:
	  (*) In OP_CALL, if (B == 0) then B = top. C is the number of returns - 1,
	      and can be 0: OP_CALL then sets `top' to last_result+1, so
	      next open instruction (OP_CALL, OP_RETURN, OP_SETLIST) may use `top'.

	  (*) In OP_VARARG, if (B == 0) then use actual number of varargs and
	      set top (like in OP_CALL with C == 0).

	  (*) In OP_RETURN, if (B == 0) then return up to `top'

	  (*) In OP_SETLIST, if (B == 0) then B = `top';
	      if (C == 0) then next `instruction' is real C

	  (*) For comparisons, A specifies what condition the test should accept
	      (true or false).

	  (*) All `skips' (pc++) assume that next instruction is a jump
	===========================================================================*/


        /*
	** masks for instruction properties. The format is:
	** bits 0-1: op mode
	** bits 2-3: C arg mode
	** bits 4-5: B arg mode
	** bit 6: instruction set register A
	** bit 7: operator is a test
	*/

        const val OpArgN = 0  /* argument is not used */
        const val OpArgU = 1  /* argument is used */
        const val OpArgR = 2  /* argument is a register or a jump offset */
        const val OpArgK = 3  /* argument is a constant or register/constant */

        val luaP_opmodes = intArrayOf(
            /*   T        A           B             C          mode		   opcode	*/
            0 shl 7 or (1 shl 6) or (OpArgR shl 4) or (OpArgN shl 2) or iABC, /* OP_MOVE */
            0 shl 7 or (1 shl 6) or (OpArgK shl 4) or (OpArgN shl 2) or iABx, /* OP_LOADK */
            0 shl 7 or (1 shl 6) or (OpArgN shl 4) or (OpArgN shl 2) or iABx, /* OP_LOADKX */
            0 shl 7 or (1 shl 6) or (OpArgU shl 4) or (OpArgU shl 2) or iABC, /* OP_LOADBOOL */
            0 shl 7 or (1 shl 6) or (OpArgU shl 4) or (OpArgN shl 2) or iABC, /* OP_LOADNIL */
            0 shl 7 or (1 shl 6) or (OpArgU shl 4) or (OpArgN shl 2) or iABC, /* OP_GETUPVAL */
            0 shl 7 or (1 shl 6) or (OpArgU shl 4) or (OpArgK shl 2) or iABC, /* OP_GETTABUP */
            0 shl 7 or (1 shl 6) or (OpArgR shl 4) or (OpArgK shl 2) or iABC, /* OP_GETTABLE */
            0 shl 7 or (0 shl 6) or (OpArgK shl 4) or (OpArgK shl 2) or iABC, /* OP_SETTABUP */
            0 shl 7 or (0 shl 6) or (OpArgU shl 4) or (OpArgN shl 2) or iABC, /* OP_SETUPVAL */
            0 shl 7 or (0 shl 6) or (OpArgK shl 4) or (OpArgK shl 2) or iABC, /* OP_SETTABLE */
            0 shl 7 or (1 shl 6) or (OpArgU shl 4) or (OpArgU shl 2) or iABC, /* OP_NEWTABLE */
            0 shl 7 or (1 shl 6) or (OpArgR shl 4) or (OpArgK shl 2) or iABC, /* OP_SELF */
            0 shl 7 or (1 shl 6) or (OpArgK shl 4) or (OpArgK shl 2) or iABC, /* OP_ADD */
            0 shl 7 or (1 shl 6) or (OpArgK shl 4) or (OpArgK shl 2) or iABC, /* OP_SUB */
            0 shl 7 or (1 shl 6) or (OpArgK shl 4) or (OpArgK shl 2) or iABC, /* OP_MUL */
            0 shl 7 or (1 shl 6) or (OpArgK shl 4) or (OpArgK shl 2) or iABC, /* OP_DIV */
            0 shl 7 or (1 shl 6) or (OpArgK shl 4) or (OpArgK shl 2) or iABC, /* OP_MOD */
            0 shl 7 or (1 shl 6) or (OpArgK shl 4) or (OpArgK shl 2) or iABC, /* OP_POW */
            0 shl 7 or (1 shl 6) or (OpArgR shl 4) or (OpArgN shl 2) or iABC, /* OP_UNM */
            0 shl 7 or (1 shl 6) or (OpArgR shl 4) or (OpArgN shl 2) or iABC, /* OP_NOT */
            0 shl 7 or (1 shl 6) or (OpArgR shl 4) or (OpArgN shl 2) or iABC, /* OP_LEN */
            0 shl 7 or (1 shl 6) or (OpArgR shl 4) or (OpArgR shl 2) or iABC, /* OP_CONCAT */
            0 shl 7 or (0 shl 6) or (OpArgR shl 4) or (OpArgN shl 2) or iAsBx, /* OP_JMP */
            1 shl 7 or (0 shl 6) or (OpArgK shl 4) or (OpArgK shl 2) or iABC, /* OP_EQ */
            1 shl 7 or (0 shl 6) or (OpArgK shl 4) or (OpArgK shl 2) or iABC, /* OP_LT */
            1 shl 7 or (0 shl 6) or (OpArgK shl 4) or (OpArgK shl 2) or iABC, /* OP_LE */
            1 shl 7 or (0 shl 6) or (OpArgN shl 4) or (OpArgU shl 2) or iABC, /* OP_TEST */
            1 shl 7 or (1 shl 6) or (OpArgR shl 4) or (OpArgU shl 2) or iABC, /* OP_TESTSET */
            0 shl 7 or (1 shl 6) or (OpArgU shl 4) or (OpArgU shl 2) or iABC, /* OP_CALL */
            0 shl 7 or (1 shl 6) or (OpArgU shl 4) or (OpArgU shl 2) or iABC, /* OP_TAILCALL */
            0 shl 7 or (0 shl 6) or (OpArgU shl 4) or (OpArgN shl 2) or iABC, /* OP_RETURN */
            0 shl 7 or (1 shl 6) or (OpArgR shl 4) or (OpArgN shl 2) or iAsBx, /* OP_FORLOOP */
            0 shl 7 or (1 shl 6) or (OpArgR shl 4) or (OpArgN shl 2) or iAsBx, /* OP_FORPREP */
            0 shl 7 or (0 shl 6) or (OpArgN shl 4) or (OpArgU shl 2) or iABC, /* OP_TFORCALL */
            1 shl 7 or (1 shl 6) or (OpArgR shl 4) or (OpArgN shl 2) or iAsBx, /* OP_TFORLOOP */
            0 shl 7 or (0 shl 6) or (OpArgU shl 4) or (OpArgU shl 2) or iABC, /* OP_SETLIST */
            0 shl 7 or (1 shl 6) or (OpArgU shl 4) or (OpArgN shl 2) or iABx, /* OP_CLOSURE */
            0 shl 7 or (1 shl 6) or (OpArgU shl 4) or (OpArgN shl 2) or iABC, /* OP_VARARG */
            0 shl 7 or (0 shl 6) or (OpArgU shl 4) or (OpArgU shl 2) or iAx
        )/* OP_EXTRAARG */

         fun getOpMode(m: Int): Int {
            return luaP_opmodes[m] and 3
        }

         fun getBMode(m: Int): Int {
            return luaP_opmodes[m] shr 4 and 3
        }

         fun getCMode(m: Int): Int {
            return luaP_opmodes[m] shr 2 and 3
        }

         fun testAMode(m: Int): Boolean {
            return 0 != luaP_opmodes[m] and (1 shl 6)
        }

         fun testTMode(m: Int): Boolean {
            return 0 != luaP_opmodes[m] and (1 shl 7)
        }

        /* number of list items to accumulate before a SETLIST instruction */
        @kotlin.jvm.JvmField
        val LFIELDS_PER_FLUSH = 50

        private val MAXSRC = 80

         fun chunkid(source: String): String {
            var source = source
            if (source.startsWith("="))
                return source.substring(1)
            var end = ""
            if (source.startsWith("@")) {
                source = source.substring(1)
            } else {
                source = "[string \"$source"
                end = "\"]"
            }
            val n = source.length + end.length
            if (n > MAXSRC)
                source = source.substring(0, MAXSRC - end.length - 3) + "..."
            return source + end
        }
    }
}
