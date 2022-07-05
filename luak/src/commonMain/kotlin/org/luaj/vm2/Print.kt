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

import org.luaj.vm2.internal.*
import org.luaj.vm2.io.*

/**
 * Debug helper class to pretty-print lua bytecodes.
 * @see Prototype
 *
 * @see LuaClosure
 */
class Print : Lua() {
    private fun _assert(b: Boolean) {
        if (!b) throw NullPointerException("_assert failed")
    }

    companion object {

        /** opcode names  */
        private val STRING_FOR_NULL = "null"
        @kotlin.jvm.JvmField var ps = JSystem.out

        /** String names for each lua opcode value.  */
        @kotlin.jvm.JvmField val OPNAMES = arrayOf(
            "MOVE",
            "LOADK",
            "LOADKX",
            "LOADBOOL",
            "LOADNIL",
            "GETUPVAL",
            "GETTABUP",
            "GETTABLE",
            "SETTABUP",
            "SETUPVAL",
            "SETTABLE",
            "NEWTABLE",
            "SELF",
            "ADD",
            "SUB",
            "MUL",
            "DIV",
            "MOD",
            "POW",
            "UNM",
            "NOT",
            "LEN",
            "CONCAT",
            "JMP",
            "EQ",
            "LT",
            "LE",
            "TEST",
            "TESTSET",
            "CALL",
            "TAILCALL",
            "RETURN",
            "FORLOOP",
            "FORPREP",
            "TFORCALL",
            "TFORLOOP",
            "SETLIST",
            "CLOSURE",
            "VARARG",
            "EXTRAARG",
            null
        )


        internal fun printString(ps: LuaWriter, s: LuaString) {

            ps.print('"')
            var i = 0
            val n = s.m_length
            while (i < n) {
                val c = s.m_bytes[s.m_offset + i].toInt()
                if (c >= ' '.toInt() && c <= '~'.toInt() && c != '\"'.toInt() && c != '\\'.toInt())
                    ps.print(c.toChar())
                else {
                    when (c) {
                        '"'.toInt() -> ps.print("\\\"")
                        '\\'.toInt() -> ps.print("\\\\")
                        0x0007 /* bell */ -> ps.print("\\a")
                        '\b'.toInt() /* backspace */ -> ps.print("\\b")
                        //'\f'.toInt()  /* form feed */ -> ps.print("\\f")
                        '\u000c'.toInt()  /* form feed */ -> ps.print("\\f")
                        '\t'.toInt()  /* tab */ -> ps.print("\\t")
                        '\r'.toInt() /* carriage return */ -> ps.print("\\r")
                        '\n'.toInt() /* newline */ -> ps.print("\\n")
                        0x000B /* vertical tab */ -> ps.print("\\v")
                        else -> {
                            ps.print('\\')
                            ps.print((1000 + 0xff and c).toString(10).substring(1))
                        }
                    }
                }
                i++
            }
            ps.print('"')
        }


        internal fun printValue(ps: LuaWriter, v: LuaValue) {
            when (v.type()) {
                LuaValue.TSTRING -> printString(ps, v as LuaString)
                else -> ps.print(v.tojstring())
            }
        }


        internal fun printConstant(ps: LuaWriter, f: Prototype, i: Int) {
            printValue(ps, f.k[i])
        }


        internal fun printUpvalue(ps: LuaWriter, u: Upvaldesc) {
            ps.print(u.idx.toString() + " ")
            printValue(ps, u.name!!)
        }

        /**
         * Print the code in a prototype
         * @param f the [Prototype]
         */

        fun printCode(f: Prototype) {
            val code = f.code
            var pc: Int
            val n = code.size
            pc = 0
            while (pc < n) {
                printOpCode(f, pc)
                ps.println()
                pc++
            }
        }

        /**
         * Print an opcode in a prototype
         * @param f the [Prototype]
         * @param pc the program counter to look up and print
         */

        fun printOpCode(f: Prototype, pc: Int) {
            printOpCode(ps, f, pc)
        }

        /**
         * Print an opcode in a prototype
         * @param ps the [PrintStream] to print to
         * @param f the [Prototype]
         * @param pc the program counter to look up and print
         */

        fun printOpCode(ps: LuaWriter, f: Prototype, pc: Int) {
            var pc = pc
            val code = f.code
            val i = code[pc]
            val o = Lua.GET_OPCODE(i)
            val a = Lua.GETARG_A(i)
            val b = Lua.GETARG_B(i)
            val c = Lua.GETARG_C(i)
            val bx = Lua.GETARG_Bx(i)
            val sbx = Lua.GETARG_sBx(i)
            val line = getline(f, pc)
            ps.print("  " + (pc + 1) + "  ")
            if (line > 0)
                ps.print("[$line]  ")
            else
                ps.print("[-]  ")
            ps.print(OPNAMES[o] + "  ")
            when (Lua.getOpMode(o)) {
                Lua.iABC -> {
                    ps.print(a)
                    if (Lua.getBMode(o) != Lua.OpArgN) ps.print(" " + if (Lua.ISK(b)) -1 - Lua.INDEXK(b) else b)
                    if (Lua.getCMode(o) != Lua.OpArgN) ps.print(" " + if (Lua.ISK(c)) -1 - Lua.INDEXK(c) else c)
                }
                Lua.iABx -> if (Lua.getBMode(o) == Lua.OpArgK) ps.print(a.toString() + " " + (-1 - bx)) else ps.print("$a $bx")
                Lua.iAsBx -> if (o == Lua.OP_JMP) ps.print(sbx) else ps.print("$a $sbx")
            }
            when (o) {
                Lua.OP_LOADK -> {
                    ps.print("  ; ")
                    printConstant(ps, f, bx)
                }
                Lua.OP_GETUPVAL, Lua.OP_SETUPVAL -> {
                    ps.print("  ; ")
                    printUpvalue(ps, f.upvalues[b])
                }
                Lua.OP_GETTABUP -> {
                    ps.print("  ; ")
                    printUpvalue(ps, f.upvalues[b])
                    ps.print(" ")
                    if (Lua.ISK(c)) printConstant(ps, f, Lua.INDEXK(c)) else ps.print("-")
                }
                Lua.OP_SETTABUP -> {
                    ps.print("  ; ")
                    printUpvalue(ps, f.upvalues[a])
                    ps.print(" ")
                    if (Lua.ISK(b)) printConstant(ps, f, Lua.INDEXK(b)) else ps.print("-")
                    ps.print(" ")
                    if (Lua.ISK(c)) printConstant(ps, f, Lua.INDEXK(c)) else ps.print("-")
                }
                Lua.OP_GETTABLE, Lua.OP_SELF -> if (Lua.ISK(c)) {
                    ps.print("  ; ")
                    printConstant(ps, f, Lua.INDEXK(c))
                }
                Lua.OP_SETTABLE, Lua.OP_ADD, Lua.OP_SUB, Lua.OP_MUL, Lua.OP_DIV, Lua.OP_POW, Lua.OP_EQ, Lua.OP_LT, Lua.OP_LE -> if (Lua.ISK(b) || Lua.ISK(c)) {
                    ps.print("  ; ")
                    if (Lua.ISK(b)) printConstant(ps, f, Lua.INDEXK(b)) else ps.print("-")
                    ps.print(" ")
                    if (Lua.ISK(c)) printConstant(ps, f, Lua.INDEXK(c)) else ps.print("-")
                }
                Lua.OP_JMP, Lua.OP_FORLOOP, Lua.OP_FORPREP -> ps.print("  ; to " + (sbx + pc + 2))
                Lua.OP_CLOSURE -> ps.print("  ; " + f.p[bx]::class.portableName)
                Lua.OP_SETLIST -> if (c == 0) ps.print("  ; " + code[++pc]) else ps.print("  ; $c")
                Lua.OP_VARARG -> ps.print("  ; is_vararg=" + f.is_vararg)
                else -> Unit
            }
        }


        private fun getline(f: Prototype, pc: Int): Int =
            if (pc > 0 && f.lineinfo != null && pc < f.lineinfo.size) f.lineinfo[pc] else -1


        internal fun printHeader(f: Prototype) {
            var s = f.source.toString()
            s = when {
                s.startsWith("@") || s.startsWith("=") -> s.substring(1)
                "\u001bLua" == s -> "(bstring)"
                else -> "(string)"
            }
            val a = if (f.linedefined == 0) "main" else "function"
            ps.print("\n%$a <$s:${f.linedefined},${f.lastlinedefined}> (${f.code.size} instructions, ${f.code.size * 4} bytes at ${id(f)})\n")
            ps.print("${f.numparams} param, ${f.maxstacksize} slot, ${f.upvalues.size} upvalue, ")
            ps.print("${f.locvars.size} local, ${f.k.size} constant, ${f.p.size} function\n")
        }


        internal fun printConstants(f: Prototype) {
            val n = f.k.size
            ps.print("constants ($n) for ${id(f)}:\n")
            var i = 0
            while (i < n) {
                ps.print("  " + (i + 1) + "  ")
                printValue(ps, f.k[i])
                ps.print("\n")
                i++
            }
        }


        internal fun printLocals(f: Prototype) {
            val n = f.locvars.size
            ps.print("locals ($n) for ${id(f)}:\n")
            var i = 0
            while (i < n) {
                ps.println("  $i  ${f.locvars[i].varname} ${f.locvars[i].startpc + 1} ${f.locvars[i].endpc + 1}")
                i++
            }
        }


        internal fun printUpValues(f: Prototype) {
            val n = f.upvalues.size
            ps.print("upvalues (" + n + ") for " + id(f) + ":\n")
            var i = 0
            while (i < n) {
                ps.print("  " + i + "  " + f.upvalues[i] + "\n")
                i++
            }
        }

        /** Pretty-prints contents of a Prototype.
         *
         * @param prototype Prototype to print.
         */

        fun print(prototype: Prototype) {
            printFunction(prototype, true)
        }

        /** Pretty-prints contents of a Prototype in short or long form.
         *
         * @param prototype Prototype to print.
         * @param full true to print all fields, false to print short form.
         */

        fun printFunction(prototype: Prototype, full: Boolean) {
            var i: Int
            val n = prototype.p.size
            printHeader(prototype)
            printCode(prototype)
            if (full) {
                printConstants(prototype)
                printLocals(prototype)
                printUpValues(prototype)
            }
            i = 0
            while (i < n) {
                printFunction(prototype.p[i], full)
                i++
            }
        }


        private fun format(s: String, maxcols: Int) {
            val n = s.length
            if (n > maxcols)
                ps.print(s.substring(0, maxcols))
            else {
                ps.print(s)
                var i = maxcols - n
                while (--i >= 0)
                    ps.print(' ')
            }
        }


        private fun id(f: Prototype): String {
            return "Proto"
        }

        /**
         * Print the state of a [LuaClosure] that is being executed
         * @param cl the [LuaClosure]
         * @param pc the program counter
         * @param stack the stack of [LuaValue]
         * @param top the top of the stack
         * @param varargs any [Varargs] value that may apply
         */

        fun printState(cl: LuaClosure, pc: Int, stack: Array<LuaValue?>, top: Int, varargs: Varargs) {
            // print opcode into buffer
            val previous = ps
            val baos = ByteArrayLuaBinOutput()
            ps = LuaWriterBinOutput(baos)
            printOpCode(cl.p, pc)
            ps.flush()
            ps.close()
            ps = previous
            format(baos.toString(), 50)
            printStack(stack, top, varargs)
            ps.println()
        }


        fun printStack(stack: Array<LuaValue?>, top: Int, varargs: Varargs) {
            // print stack
            ps.print('[')
            for (i in stack.indices) {
                val v = stack[i]
                if (v == null)
                    ps.print(STRING_FOR_NULL)
                else
                    when (v.type()) {
                        LuaValue.TSTRING -> {
                            val s = v.checkstring()
                            ps.print(
                                if (s!!.length() < 48)
                                    s.tojstring()
                                else
                                    s.substring(0, 32).tojstring() + "...+" + (s.length() - 32) + "b"
                            )
                        }
                        LuaValue.TFUNCTION -> ps.print(v.tojstring())
                        LuaValue.TUSERDATA -> {
                            val o = v.touserdata()
                            if (o != null) {
                                var n = o::class.portableName
                                n = n.substring(n.lastIndexOf('.') + 1)
                                ps.print(n + ": " + o.hashCode().toHexString())
                            } else {
                                ps.print(v.toString())
                            }
                        }
                        else -> ps.print(v.tojstring())
                    }
                if (i + 1 == top)
                    ps.print(']')
                ps.print(" | ")
            }
            ps.print(varargs)
        }
    }

}
