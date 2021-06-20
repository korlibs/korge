/*******************************************************************************
 * Copyright (c) 2015 Luaj.org. All rights reserved.
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
package org.luaj.vm2.compiler

import org.luaj.vm2.LocVars
import org.luaj.vm2.Lua
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Prototype
import org.luaj.vm2.Upvaldesc
import org.luaj.vm2.internal.*
import kotlin.jvm.*
import kotlin.math.*

/**
 * Constants used by the LuaC compiler and related classes.
 *
 * @see LuaC
 *
 * @see FuncState
 */
open class Constants protected constructor() : Lua() {
    companion object {

        /** Maximum stack size of a luaj vm interpreter instance.  */
        @kotlin.jvm.JvmField val MAXSTACK = 250
        @kotlin.jvm.JvmField val LUAI_MAXUPVAL = 0xff
        @kotlin.jvm.JvmField val LUAI_MAXVARS = 200
        @kotlin.jvm.JvmField val NO_REG = Lua.MAXARG_A


        /* OpMode - basic instruction format */
        @kotlin.jvm.JvmField val iABC = 0
        @kotlin.jvm.JvmField val iABx = 1
        @kotlin.jvm.JvmField val iAsBx = 2

        /* OpArgMask */
        @kotlin.jvm.JvmField val OpArgN = 0
        /* argument is not used */
        @kotlin.jvm.JvmField val OpArgU = 1
        /* argument is used */
        @kotlin.jvm.JvmField val OpArgR = 2
        /* argument is a register or a jump offset */
        @kotlin.jvm.JvmField val OpArgK = 3   /* argument is a constant or register/constant */


        @JvmStatic
        protected fun _assert(b: Boolean) {
            if (!b)
                throw LuaError("compiler assert failed")
        }

         fun SET_OPCODE(i: InstructionPtr, o: Int) {
            i.set(i.get() and Lua.MASK_NOT_OP or (o shl Lua.POS_OP and Lua.MASK_OP))
        }

         fun SETARG_A(code: IntArray, index: Int, u: Int) {
            code[index] = code[index] and Lua.MASK_NOT_A or (u shl Lua.POS_A and Lua.MASK_A)
        }

         fun SETARG_A(i: InstructionPtr, u: Int) {
            i.set(i.get() and Lua.MASK_NOT_A or (u shl Lua.POS_A and Lua.MASK_A))
        }

         fun SETARG_B(i: InstructionPtr, u: Int) {
            i.set(i.get() and Lua.MASK_NOT_B or (u shl Lua.POS_B and Lua.MASK_B))
        }

         fun SETARG_C(i: InstructionPtr, u: Int) {
            i.set(i.get() and Lua.MASK_NOT_C or (u shl Lua.POS_C and Lua.MASK_C))
        }

         fun SETARG_Bx(i: InstructionPtr, u: Int) {
            i.set(i.get() and Lua.MASK_NOT_Bx or (u shl Lua.POS_Bx and Lua.MASK_Bx))
        }

         fun SETARG_sBx(i: InstructionPtr, u: Int) {
            SETARG_Bx(i, u + Lua.MAXARG_sBx)
        }

         fun CREATE_ABC(o: Int, a: Int, b: Int, c: Int): Int {
            return o shl Lua.POS_OP and Lua.MASK_OP or
                    (a shl Lua.POS_A and Lua.MASK_A) or
                    (b shl Lua.POS_B and Lua.MASK_B) or
                    (c shl Lua.POS_C and Lua.MASK_C)
        }

         fun CREATE_ABx(o: Int, a: Int, bc: Int): Int {
            return o shl Lua.POS_OP and Lua.MASK_OP or
                    (a shl Lua.POS_A and Lua.MASK_A) or
                    (bc shl Lua.POS_Bx and Lua.MASK_Bx)
        }

        // vector reallocation

         fun realloc(v: Array<LuaValue>?, n: Int): Array<LuaValue> {
            val a = arrayOfNulls<LuaValue>(n)
            if (v != null)
                arraycopy(v, 0, a, 0, min(v.size, n))
            return a as Array<LuaValue>
        }

         fun realloc(v: Array<Prototype>?, n: Int): Array<Prototype> {
            val a = arrayOfNulls<Prototype>(n)
            if (v != null)
                arraycopy(v, 0, a, 0, min(v.size, n))
            return a as Array<Prototype>
        }

         fun realloc(v: Array<LuaString>?, n: Int): Array<LuaString> {
            val a = arrayOfNulls<LuaString>(n)
            if (v != null)
                arraycopy(v, 0, a, 0, min(v.size, n))
            return a as Array<LuaString>
        }

         fun realloc(v: Array<LocVars>?, n: Int): Array<LocVars> {
            val a = arrayOfNulls<LocVars>(n)
            if (v != null)
                arraycopy(v, 0, a, 0, min(v.size, n))
            return a as Array<LocVars>
        }

         fun realloc(v: Array<Upvaldesc>?, n: Int): Array<Upvaldesc> {
            val a = arrayOfNulls<Upvaldesc>(n)
            if (v != null)
                arraycopy(v, 0, a, 0, min(v.size, n))
            return a as Array<Upvaldesc>
        }

         fun realloc(v: Array<LexState.Vardesc>?, n: Int): Array<LexState.Vardesc> {
            val a = arrayOfNulls<LexState.Vardesc>(n)
            if (v != null)
                arraycopy(v, 0, a, 0, min(v.size, n))
            return a as Array<LexState.Vardesc>
        }

         fun grow(v: Array<LexState.Labeldesc>?, min_n: Int): Array<LexState.Labeldesc> {
            return (if (v == null) arrayOfNulls<LexState.Labeldesc>(2) else if (v.size < min_n) realloc(v, v.size * 2) else v) as Array<LexState.Labeldesc>
        }

         fun realloc(v: Array<LexState.Labeldesc>?, n: Int): Array<LexState.Labeldesc> {
            val a = arrayOfNulls<LexState.Labeldesc>(n)
            if (v != null)
                arraycopy(v, 0, a, 0, min(v.size, n))
            return a as Array<LexState.Labeldesc>
        }

         fun realloc(v: IntArray?, n: Int): IntArray {
            val a = IntArray(n)
            if (v != null)
                arraycopy(v, 0, a, 0, min(v.size, n))
            return a
        }

         fun realloc(v: ByteArray?, n: Int): ByteArray {
            val a = ByteArray(n)
            if (v != null)
                arraycopy(v, 0, a, 0, min(v.size, n))
            return a
        }

         fun realloc(v: CharArray?, n: Int): CharArray {
            val a = CharArray(n)
            if (v != null)
                arraycopy(v, 0, a, 0, min(v.size, n))
            return a
        }
    }
}
