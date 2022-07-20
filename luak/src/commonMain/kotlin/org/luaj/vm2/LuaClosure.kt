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

/**
 * Extension of [LuaFunction] which executes lua bytecode.
 *
 *
 * A [LuaClosure] is a combination of a [Prototype]
 * and a [LuaValue] to use as an environment for execution.
 * Normally the [LuaValue] is a [Globals] in which case the environment
 * will contain standard lua libraries.
 *
 *
 *
 * There are three main ways [LuaClosure] instances are created:
 *
 *  * Construct an instance using [.LuaClosure]
 *  * Construct it indirectly by loading a chunk via [Globals.load]
 *  * Execute the lua bytecode [Lua.OP_CLOSURE] as part of bytecode processing
 *
 *
 *
 * To construct it directly, the [Prototype] is typically created via a compiler such as
 * [org.luaj.vm2.compiler.LuaC]:
 * <pre> `String script = "print( 'hello, world' )";
 * InputStream is = new ByteArrayInputStream(script.getBytes());
 * Prototype p = LuaC.instance.compile(is, "script");
 * LuaValue globals = JsePlatform.standardGlobals();
 * LuaClosure f = new LuaClosure(p, globals);
 * f.call();
`</pre> *
 *
 *
 * To construct it indirectly, the [Globals.load] method may be used:
 * <pre> `Globals globals = JsePlatform.standardGlobals();
 * LuaFunction f = globals.load(new StringReader(script), "script");
 * LuaClosure c = f.checkclosure();  // This may fail if LuaJC is installed.
 * c.call();
`</pre> *
 *
 *
 * In this example, the "checkclosure()" may fail if direct lua-to-java-bytecode
 * compiling using LuaJC is installed, because no LuaClosure is created in that case
 * and the value returned is a [LuaFunction] but not a [LuaClosure].
 *
 *
 * Since a [LuaClosure] is a [LuaFunction] which is a [LuaValue],
 * all the value operations can be used directly such as:
 *
 *  * [LuaValue.call]
 *  * [LuaValue.call]
 *  * [LuaValue.invoke]
 *  * [LuaValue.invoke]
 *  * [LuaValue.method]
 *  * [LuaValue.method]
 *  * [LuaValue.invokemethod]
 *  * [LuaValue.invokemethod]
 *  *  ...
 *
 * @see LuaValue
 *
 * @see LuaFunction
 *
 * @see LuaValue.isclosure
 * @see LuaValue.checkclosure
 * @see LuaValue.optclosure
 * @see LoadState
 *
 * @see Globals.compiler
 */
class LuaClosure
/** Create a closure around a Prototype with a specific environment.
 * If the prototype has upvalues, the environment will be written into the first upvalue.
 * @param p the Prototype to construct this Closure for.
 * @param env the environment to associate with the closure.
 */
    (val p: Prototype, env: LuaValue?) : LuaFunction() {

    var upValues: Array<UpValue?> = when {
        p.upvalues == null || p.upvalues.isEmpty() -> NOUPVALUES
        else -> arrayOfNulls<UpValue>(p.upvalues.size).also { it[0] = UpValue(arrayOf(env), 0) }
    }

    internal val globals: Globals? = if (env is Globals) env else null

    override fun isclosure(): Boolean = true
    override fun optclosure(defval: LuaClosure?): LuaClosure? = this
    override fun checkclosure(): LuaClosure? = this
    override fun getmetatable(): LuaValue? = LuaFunction.s_metatable
    override fun tojstring(): String = "function: $p"

    override fun call(): LuaValue {
        val stack = arrayOfNulls<LuaValue>(p.maxstacksize) as Array<LuaValue>
        for (i in 0 until p.numparams) stack[i] = LuaValue.NIL
        return execute(stack, LuaValue.NONE).arg1()
    }

    override fun call(arg: LuaValue): LuaValue {
        val stack = arrayOfNulls<LuaValue>(p.maxstacksize) as Array<LuaValue>
        arraycopy(LuaValue.NILS, 0, stack, 0, p.maxstacksize)
        for (i in 1 until p.numparams) stack[i] = LuaValue.NIL
        when (p.numparams) {
            0 -> return execute(stack, arg).arg1()
            else -> {
                stack[0] = arg
                return execute(stack, LuaValue.NONE).arg1()
            }
        }
    }

    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val stack = arrayOfNulls<LuaValue>(p.maxstacksize) as Array<LuaValue>
        for (i in 2 until p.numparams) stack[i] = LuaValue.NIL
        when (p.numparams) {
            1 -> {
                stack[0] = arg1
                return execute(stack, arg2).arg1()
            }
            0 -> return execute(stack, if (p.is_vararg != 0) LuaValue.varargsOf(arg1, arg2) else LuaValue.NONE).arg1()
            else -> {
                stack[0] = arg1
                stack[1] = arg2
                return execute(stack, LuaValue.NONE).arg1()
            }
        }
    }

    override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue): LuaValue {
        val stack = arrayOfNulls<LuaValue>(p.maxstacksize) as Array<LuaValue>
        for (i in 3 until p.numparams) stack[i] = LuaValue.NIL
        return when (p.numparams) {
            0 -> execute(stack, if (p.is_vararg != 0) LuaValue.varargsOf(arg1, arg2, arg3) else LuaValue.NONE).arg1()
            1 -> {
                stack[0] = arg1
                execute(stack, if (p.is_vararg != 0) LuaValue.varargsOf(arg2, arg3) else LuaValue.NONE).arg1()
            }
            2 -> {
                stack[0] = arg1
                stack[1] = arg2
                execute(stack, arg3).arg1()
            }
            else -> {
                stack[0] = arg1
                stack[1] = arg2
                stack[2] = arg3
                execute(stack, LuaValue.NONE).arg1()
            }
        }
    }

    override fun invoke(varargs: Varargs): Varargs = onInvoke(varargs).eval()

    override fun onInvoke(varargs: Varargs): Varargs {
        val stack = arrayOfNulls<LuaValue>(p.maxstacksize) as Array<LuaValue>
        for (i in 0 until p.numparams) stack[i] = varargs.arg(i + 1)
        return execute(stack, if (p.is_vararg != 0) varargs.subargs(p.numparams + 1) else LuaValue.NONE)
    }

    protected fun execute(stack: Array<LuaValue>, varargs: Varargs): Varargs {
        // loop through instructions
        var i: Int
        var a: Int
        var b: Int
        var c: Int
        var pc = 0
        var top = 0
        var o: LuaValue
        var v: Varargs = LuaValue.NONE
        val code = p.code
        val k = p.k

        // upvalues are only possible when closures create closures
        // TODO: use linked list.
        val openups = if (p.p.size > 0) arrayOfNulls<UpValue>(stack.size) else null

        // allow for debug hooks
        if (globals != null && globals.debuglib != null)
            globals.debuglib!!.onCall(this, varargs, stack)

        // process instructions
        try {
            loop@while (true) {
                if (globals != null && globals.debuglib != null)
                    globals.debuglib!!.onInstruction(pc, v, top)

                // pull out instruction
                i = code[pc]
                a = i shr 6 and 0xff

                // process the op code
                when (i and 0x3f) {

                    Lua.OP_MOVE/*	A B	R(A):= R(B)					*/ -> {
                        stack[a] = stack[i.ushr(23)]
                        ++pc
                        continue@loop
                    }

                    Lua.OP_LOADK/*	A Bx	R(A):= Kst(Bx)					*/ -> {
                        stack[a] = k[i.ushr(14)]
                        ++pc
                        continue@loop
                    }

                    Lua.OP_LOADBOOL/*	A B C	R(A):= (Bool)B: if (C) pc++			*/ -> {
                        stack[a] = if (i.ushr(23) != 0) LuaValue.BTRUE else LuaValue.BFALSE
                        if (i and (0x1ff shl 14) != 0)
                            ++pc /* skip next instruction (if C) */
                        ++pc
                        continue@loop
                    }

                    Lua.OP_LOADNIL /*	A B	R(A):= ...:= R(A+B):= nil			*/ -> {
                        b = i.ushr(23)
                        while (b-- >= 0)
                            stack[a++] = LuaValue.NIL
                        ++pc
                        continue@loop
                    }

                    Lua.OP_GETUPVAL /*	A B	R(A):= UpValue[B]				*/ -> {
                        stack[a] = upValues[i.ushr(23)]!!.value!!
                        ++pc
                        continue@loop
                    }

                    Lua.OP_GETTABUP /*	A B C	R(A) := UpValue[B][RK(C)]			*/ -> {
                        stack[a] = upValues[i.ushr(23)]!!.value!![if ((run { c = i shr 14 and 0x1ff; c }) > 0xff
                        ) k[c and 0x0ff] else stack[c]]
                        ++pc
                        continue@loop
                    }

                    Lua.OP_GETTABLE /*	A B C	R(A):= R(B)[RK(C)]				*/ -> {
                        stack[a] = stack[i.ushr(23)][if ((run { c = i shr 14 and 0x1ff; c }) > 0xff) k[c and 0x0ff] else stack[c]]
                        ++pc
                        continue@loop
                    }

                    Lua.OP_SETTABUP /*	A B C	UpValue[A][RK(B)] := RK(C)			*/ -> {
                        upValues[a]!!.value!![if ((run { b = i.ushr(23); b }) > 0xff) k[b and 0x0ff] else stack[b]] =
                            if ((run { c = i shr 14 and 0x1ff; c }) > 0xff) k[c and 0x0ff] else stack[c]
                        ++pc
                        continue@loop
                    }

                    Lua.OP_SETUPVAL /*	A B	UpValue[B]:= R(A)				*/ -> {
                        upValues[i.ushr(23)]?.value = stack[a]
                        ++pc
                        continue@loop
                    }

                    Lua.OP_SETTABLE /*	A B C	R(A)[RK(B)]:= RK(C)				*/ -> {
                        stack[a][if ((run { b = i.ushr(23); b }) > 0xff) k[b and 0x0ff] else stack[b]] =
                            if ((run { c = i shr 14 and 0x1ff; c }) > 0xff) k[c and 0x0ff] else stack[c]
                        ++pc
                        continue@loop
                    }

                    Lua.OP_NEWTABLE /*	A B C	R(A):= {} (size = B,C)				*/ -> {
                        stack[a] = LuaTable(i.ushr(23), i shr 14 and 0x1ff)
                        ++pc
                        continue@loop
                    }

                    Lua.OP_SELF /*	A B C	R(A+1):= R(B): R(A):= R(B)[RK(C)]		*/ -> {
                        stack[a + 1] = (run { o = stack[i.ushr(23)]; o })
                        stack[a] = o[if ((run { c = i shr 14 and 0x1ff; c }) > 0xff) k[c and 0x0ff] else stack[c]]
                        ++pc
                        continue@loop
                    }

                    Lua.OP_ADD /*	A B C	R(A):= RK(B) + RK(C)				*/ -> {
                        stack[a] = (if ((run { b = i.ushr(23); b }) > 0xff) k[b and 0x0ff] else stack[b]).add(
                            if ((run { c = i shr 14 and 0x1ff; c }) > 0xff) k[c and 0x0ff] else stack[c]
                        )
                        ++pc
                        continue@loop
                    }

                    Lua.OP_SUB /*	A B C	R(A):= RK(B) - RK(C)				*/ -> {
                        stack[a] = (if ((run { b = i.ushr(23); b }) > 0xff) k[b and 0x0ff] else stack[b]).sub(
                            if ((run { c = i shr 14 and 0x1ff; c }) > 0xff) k[c and 0x0ff] else stack[c]
                        )
                        ++pc
                        continue@loop
                    }

                    Lua.OP_MUL /*	A B C	R(A):= RK(B) * RK(C)				*/ -> {
                        stack[a] = (if ((run { b = i.ushr(23); b }) > 0xff) k[b and 0x0ff] else stack[b]).mul(
                            if ((run { c = i shr 14 and 0x1ff; c }) > 0xff) k[c and 0x0ff] else stack[c]
                        )
                        ++pc
                        continue@loop
                    }

                    Lua.OP_DIV /*	A B C	R(A):= RK(B) / RK(C)				*/ -> {
                        stack[a] = (if ((run { b = i.ushr(23); b }) > 0xff) k[b and 0x0ff] else stack[b]).div(
                            if ((run { c = i shr 14 and 0x1ff; c }) > 0xff) k[c and 0x0ff] else stack[c]
                        )
                        ++pc
                        continue@loop
                    }

                    Lua.OP_MOD /*	A B C	R(A):= RK(B) % RK(C)				*/ -> {
                        stack[a] = (if ((run { b = i.ushr(23); b }) > 0xff) k[b and 0x0ff] else stack[b]).mod(
                            if ((run { c = i shr 14 and 0x1ff; c }) > 0xff) k[c and 0x0ff] else stack[c]
                        )
                        ++pc
                        continue@loop
                    }

                    Lua.OP_POW /*	A B C	R(A):= RK(B) ^ RK(C)				*/ -> {
                        stack[a] = (if ((run { b = i.ushr(23); b }) > 0xff) k[b and 0x0ff] else stack[b]).pow(
                            if ((run { c = i shr 14 and 0x1ff; c }) > 0xff) k[c and 0x0ff] else stack[c]
                        )
                        ++pc
                        continue@loop
                    }

                    Lua.OP_UNM /*	A B	R(A):= -R(B)					*/ -> {
                        stack[a] = stack[i.ushr(23)].neg()
                        ++pc
                        continue@loop
                    }

                    Lua.OP_NOT /*	A B	R(A):= not R(B)				*/ -> {
                        stack[a] = stack[i.ushr(23)].not()
                        ++pc
                        continue@loop
                    }

                    Lua.OP_LEN /*	A B	R(A):= length of R(B)				*/ -> {
                        stack[a] = stack[i.ushr(23)].len()
                        ++pc
                        continue@loop
                    }

                    Lua.OP_CONCAT /*	A B C	R(A):= R(B).. ... ..R(C)			*/ -> {
                        b = i.ushr(23)
                        c = i shr 14 and 0x1ff
                        run {
                            if (c > b + 1) {
                                var sb = stack[c].buffer()
                                while (--c >= b)
                                    sb = stack[c].concat(sb)
                                stack[a] = sb.value()
                            } else {
                                stack[a] = stack[c - 1].concat(stack[c])
                            }
                        }
                        ++pc
                        continue@loop
                    }

                    Lua.OP_JMP /*	sBx	pc+=sBx					*/ -> {
                        pc += i.ushr(14) - 0x1ffff
                        if (a > 0) {
                            --a
                            b = openups!!.size
                            while (--b >= 0)
                                if (openups[b] != null && openups[b]!!.index >= a) {
                                    openups[b]!!.close()
                                    openups[b] = null
                                }
                        }
                        ++pc
                        continue@loop
                    }

                    Lua.OP_EQ /*	A B C	if ((RK(B) == RK(C)) ~= A) then pc++		*/ -> {
                        if ((if ((run { b = i.ushr(23); b }) > 0xff) k[b and 0x0ff] else stack[b]).eq_b(
                                if ((run { c = i shr 14 and 0x1ff; c }) > 0xff) k[c and 0x0ff] else stack[c]
                            ) != (a != 0)
                        )
                            ++pc
                        ++pc
                        continue@loop
                    }

                    Lua.OP_LT /*	A B C	if ((RK(B) <  RK(C)) ~= A) then pc++  		*/ -> {
                        if ((if ((run { b = i.ushr(23); b }) > 0xff) k[b and 0x0ff] else stack[b]).lt_b(
                                if ((run { c = i shr 14 and 0x1ff; c }) > 0xff) k[c and 0x0ff] else stack[c]
                            ) != (a != 0)
                        )
                            ++pc
                        ++pc
                        continue@loop
                    }

                    Lua.OP_LE /*	A B C	if ((RK(B) <= RK(C)) ~= A) then pc++  		*/ -> {
                        if ((if ((run { b = i.ushr(23); b }) > 0xff) k[b and 0x0ff] else stack[b]).lteq_b(
                                if ((run { c = i shr 14 and 0x1ff; c }) > 0xff) k[c and 0x0ff] else stack[c]
                            ) != (a != 0)
                        )
                            ++pc
                        ++pc
                        continue@loop
                    }

                    Lua.OP_TEST /*	A C	if not (R(A) <=> C) then pc++			*/ -> {
                        if (stack[a].toboolean() != (i and (0x1ff shl 14) != 0))
                            ++pc
                        ++pc
                        continue@loop
                    }

                    Lua.OP_TESTSET /*	A B C	if (R(B) <=> C) then R(A):= R(B) else pc++	*/ -> {
                        /* note: doc appears to be reversed */
                        if ((run { o = stack[i.ushr(23)]; o }).toboolean() != (i and (0x1ff shl 14) != 0))
                            ++pc
                        else
                            stack[a] = o // TODO: should be sBx?
                        ++pc
                        continue@loop
                    }

                    Lua.OP_CALL /*	A B C	R(A), ... ,R(A+C-2):= R(A)(R(A+1), ... ,R(A+B-1)) */ -> when (i and (Lua.MASK_B or Lua.MASK_C)) {
                        1 shl Lua.POS_B or (0 shl Lua.POS_C) -> {
                            v = stack[a].invoke(LuaValue.NONE)
                            top = a + v.narg()
                            ++pc
                            continue@loop
                        }
                        2 shl Lua.POS_B or (0 shl Lua.POS_C) -> {
                            v = stack[a].invoke(stack[a + 1])
                            top = a + v.narg()
                            ++pc
                            continue@loop
                        }
                        1 shl Lua.POS_B or (1 shl Lua.POS_C) -> {
                            stack[a].call()
                            ++pc
                            continue@loop
                        }
                        2 shl Lua.POS_B or (1 shl Lua.POS_C) -> {
                            stack[a].call(stack[a + 1])
                            ++pc
                            continue@loop
                        }
                        3 shl Lua.POS_B or (1 shl Lua.POS_C) -> {
                            stack[a].call(stack[a + 1], stack[a + 2])
                            ++pc
                            continue@loop
                        }
                        4 shl Lua.POS_B or (1 shl Lua.POS_C) -> {
                            stack[a].call(stack[a + 1], stack[a + 2], stack[a + 3])
                            ++pc
                            continue@loop
                        }
                        1 shl Lua.POS_B or (2 shl Lua.POS_C) -> {
                            stack[a] = stack[a].call()
                            ++pc
                            continue@loop
                        }
                        2 shl Lua.POS_B or (2 shl Lua.POS_C) -> {
                            stack[a] = stack[a].call(stack[a + 1])
                            ++pc
                            continue@loop
                        }
                        3 shl Lua.POS_B or (2 shl Lua.POS_C) -> {
                            stack[a] = stack[a].call(stack[a + 1], stack[a + 2])
                            ++pc
                            continue@loop
                        }
                        4 shl Lua.POS_B or (2 shl Lua.POS_C) -> {
                            stack[a] = stack[a].call(stack[a + 1], stack[a + 2], stack[a + 3])
                            ++pc
                            continue@loop
                        }
                        else -> {
                            b = i.ushr(23)
                            c = i shr 14 and 0x1ff
                            v = stack[a].invoke(
                                if (b > 0)
                                    LuaValue.varargsOf(stack, a + 1, b - 1)
                                else
                                // exact arg count
                                    LuaValue.varargsOf(stack, a + 1, top - v.narg() - (a + 1), v)
                            )  // from prev top
                            if (c > 0) {
                                v.copyto(stack, a, c - 1)
                                v = LuaValue.NONE
                            } else {
                                top = a + v.narg()
                                v = v.dealias()
                            }
                            ++pc
                            continue@loop
                        }
                    }

                    Lua.OP_TAILCALL /*	A B C	return R(A)(R(A+1), ... ,R(A+B-1))		*/ -> when (i and Lua.MASK_B) {
                        1 shl Lua.POS_B -> return TailcallVarargs(stack[a], LuaValue.NONE)
                        2 shl Lua.POS_B -> return TailcallVarargs(stack[a], stack[a + 1])
                        3 shl Lua.POS_B -> return TailcallVarargs(
                            stack[a],
                            LuaValue.varargsOf(stack[a + 1], stack[a + 2])
                        )
                        4 shl Lua.POS_B -> return TailcallVarargs(
                            stack[a],
                            LuaValue.varargsOf(stack[a + 1], stack[a + 2], stack[a + 3])
                        )
                        else -> {
                            b = i.ushr(23)
                            v = if (b > 0)
                                LuaValue.varargsOf(stack, a + 1, b - 1)
                            else
                            // exact arg count
                                LuaValue.varargsOf(stack, a + 1, top - v.narg() - (a + 1), v) // from prev top
                            return TailcallVarargs(stack[a], v)
                        }
                    }

                    Lua.OP_RETURN /*	A B	return R(A), ... ,R(A+B-2)	(see note)	*/ -> {
                        b = i.ushr(23)
                        when (b) {
                            0 -> return LuaValue.varargsOf(stack, a, top - v.narg() - a, v)
                            1 -> return LuaValue.NONE
                            2 -> return stack[a]
                            else -> return LuaValue.varargsOf(stack, a, b - 1)
                        }
                    }

                    Lua.OP_FORLOOP /*	A sBx	R(A)+=R(A+2): if R(A) <?= R(A+1) then { pc+=sBx: R(A+3)=R(A) }*/ -> {
                        run {
                            val limit = stack[a + 1]
                            val step = stack[a + 2]
                            val idx = step.add(stack[a])
                            if (if (step.gt_b(0)) idx.lteq_b(limit) else idx.gteq_b(limit)) {
                                stack[a] = idx
                                stack[a + 3] = idx
                                pc += i.ushr(14) - 0x1ffff
                            }
                        }
                        ++pc
                        continue@loop
                    }

                    Lua.OP_FORPREP /*	A sBx	R(A)-=R(A+2): pc+=sBx				*/ -> {
                        run {
                            val init = stack[a].checknumber("'for' initial value must be a number")
                            val limit = stack[a + 1].checknumber("'for' limit must be a number")
                            val step = stack[a + 2].checknumber("'for' step must be a number")
                            stack[a] = init.sub(step)
                            stack[a + 1] = limit
                            stack[a + 2] = step
                            pc += i.ushr(14) - 0x1ffff
                        }
                        ++pc
                        continue@loop
                    }

                    Lua.OP_TFORCALL /* A C	R(A+3), ... ,R(A+2+C) := R(A)(R(A+1), R(A+2));	*/ -> {
                        v = stack[a].invoke(LuaValue.varargsOf(stack[a + 1], stack[a + 2]))
                        c = i shr 14 and 0x1ff
                        while (--c >= 0)
                            stack[a + 3 + c] = v.arg(c + 1)
                        v = LuaValue.NONE
                        ++pc
                        continue@loop
                    }

                    Lua.OP_TFORLOOP /* A sBx	if R(A+1) ~= nil then { R(A)=R(A+1); pc += sBx */ -> {
                        if (!stack[a + 1].isnil()) { /* continue loop? */
                            stack[a] = stack[a + 1]  /* save control varible. */
                            pc += i.ushr(14) - 0x1ffff
                        }
                        ++pc
                        continue@loop
                    }

                    Lua.OP_SETLIST /*	A B C	R(A)[(C-1)*FPF+i]:= R(A+i), 1 <= i <= B	*/ -> {
                        run {
                            if ((run { c = i shr 14 and 0x1ff; c }) == 0)
                                c = code[++pc]
                            val offset = (c - 1) * Lua.LFIELDS_PER_FLUSH
                            o = stack[a]
                            if ((run { b = i.ushr(23); b }) == 0) {
                                b = top - a - 1
                                val m = b - v.narg()
                                var j = 1
                                while (j <= m) {
                                    o[offset + j] = stack[a + j]
                                    j++
                                }
                                while (j <= b) {
                                    o[offset + j] = v.arg(j - m)
                                    j++
                                }
                            } else {
                                o.presize(offset + b)
                                for (j in 1..b)
                                    o[offset + j] = stack[a + j]
                            }
                        }
                        ++pc
                        continue@loop
                    }

                    Lua.OP_CLOSURE /*	A Bx	R(A):= closure(KPROTO[Bx])	*/ -> {
                        run {
                            val newp = p.p[i.ushr(14)]
                            val ncl = LuaClosure(newp, globals)
                            val uv = newp.upvalues
                            var j = 0
                            val nup = uv.size
                            while (j < nup) {
                                if (uv[j].instack)
                                /* upvalue refes to local variable? */
                                    ncl.upValues[j] = findupval(stack as Array<LuaValue?>, uv[j].idx, openups!!)
                                else
                                /* get upvalue from enclosing function */
                                    ncl.upValues[j] = upValues[uv[j].idx.toInt()]
                                ++j
                            }
                            stack[a] = ncl
                        }
                        ++pc
                        continue@loop
                    }

                    Lua.OP_VARARG /*	A B	R(A), R(A+1), ..., R(A+B-1) = vararg		*/ -> {
                        b = i.ushr(23)
                        if (b == 0) {
                            top = a + (run { b = varargs.narg(); b })
                            v = varargs
                        } else {
                            for (j in 1 until b)
                                stack[a + j - 1] = varargs.arg(j)
                        }
                        ++pc
                        continue@loop
                    }

                    Lua.OP_EXTRAARG -> throw IllegalArgumentException("Uexecutable opcode: OP_EXTRAARG")

                    else -> throw IllegalArgumentException("Illegal opcode: " + (i and 0x3f))
                }
                ++pc
            }
        } catch (le: LuaError) {
            if (le.traceback == null) processErrorHooks(le, p, pc)
            throw le
        } catch (e: Exception) {
            val le = LuaError(e)
            processErrorHooks(le, p, pc)
            throw le
        } finally {
            if (openups != null) {
                var u = openups.size
                while (--u >= 0) if (openups[u] != null) openups[u]!!.close()
            }
            if (globals != null && globals.debuglib != null) globals.debuglib!!.onReturn()
        }
    }

    /**
     * Run the error hook if there is one
     * @param msg the message to use in error hook processing.
     */
    internal fun errorHook(msg: String, level: Int): String {
        if (globals == null) return msg
        val r = globals.running
        if (r.errorfunc == null) return if (globals.debuglib != null) msg + "\n" + globals.debuglib!!.traceback(level) else msg
        val e = r.errorfunc
        r.errorfunc = null
        return try {
            e!!.call(LuaValue.valueOf(msg)).tojstring()
        } catch (t: Throwable) {
            "error in error handling"
        } finally {
            r.errorfunc = e
        }
    }

    private fun processErrorHooks(le: LuaError, p: Prototype, pc: Int) {
        le.fileline = ((if (p.source != null) p.source.tojstring() else "?") + ":" + if (p.lineinfo != null && pc >= 0 && pc < p.lineinfo.size) p.lineinfo[pc].toString() else "?")
        le.traceback = errorHook(le.message!!, le.level)
    }

    private fun findupval(stack: Array<LuaValue?>, idx: Short, openups: Array<UpValue?>): UpValue? {
        val n = openups.size
        for (i in 0 until n) if (openups[i] != null && openups[i]!!.index == idx.toInt()) return openups[i]
        for (i in 0 until n) if (openups[i] == null) return UpValue(stack, idx.toInt()).also { openups[i] = it }
        LuaValue.error("No space for upvalue")
        return null
    }

    protected fun getUpvalue(i: Int): LuaValue? = upValues[i]?.value
    protected fun setUpvalue(i: Int, v: LuaValue) { upValues[i]?.value = v }
    override fun name(): String = "<" + p.shortsource() + ":" + p.linedefined + ">"

    companion object {
        private val NOUPVALUES = arrayOfNulls<UpValue>(0)
    }
}
