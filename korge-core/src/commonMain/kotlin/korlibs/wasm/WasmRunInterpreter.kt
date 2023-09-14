package korlibs.wasm

import korlibs.wasm.WasmSType.*
import korlibs.datastructure.*
import korlibs.io.lang.*
import korlibs.math.*
import korlibs.memory.*
import kotlin.rotateLeft
import kotlin.rotateRight

class WasmRunInterpreter(val module: WasmModule, memPages: Int = 10, maxMemPages: Int = 0x10000) : WasmRuntime(memPages, maxMemPages) {
    var stackTop = 0
    var stackPos = 0
    val refStack = arrayListOf<Any?>()
    val stack = Buffer.allocDirect(1024)
    var trace = false

    fun strlen(ptr: Int): Int {
        var n = 0
        while (memory.getUnalignedInt8(ptr + n) != 0.toByte()) n++
        return n
    }
    fun strlen16(ptr: Int): Int {
        var n = 0
        while (memory.getUnalignedInt16(ptr + n) != 0.toShort()) n += 2
        return n / 2
    }

    fun readStringz(ptr: Int): String {
        if (ptr == 0) return "<null>"
        return memory.getArrayInt8(ptr, ByteArray(strlen(ptr))).decodeToString()
    }
    fun readStringz16(ptr: Int): String {
        if (ptr == 0) return "<null>"
        var out = ""
        var n = 0
        while (true) {
            val v = memory.getUnalignedInt16(ptr + n).toInt() and 0xFFFF
            if (v == 0) break
            out += v.toChar()
            n += 2
        }
        return out
    }

    fun readString(ptr: Int): String {
        return readStringz16(ptr)
    }

    val stackAvailable get() = (stackPos - stackTop) + (refStack.size * 4)

    private fun popIndex(size: Int): Int {
        if (stackAvailable < size) error("Can't pop stackTop=$stackTop, stackPos=$stackPos")
        stackPos -= size
        return stackPos
    }
    private fun pushIndex(size: Int): Int = stackPos.also { stackPos += size }

    fun pushInt(v: Boolean) = pushInt(v.toInt())
    fun pushInt(v: Int) = stack.setUnalignedInt32(pushIndex(4), v)
    fun pushLong(v: Long) = stack.setUnalignedInt64(pushIndex(8), v)
    fun pushFloat(v: Float) = stack.setUnalignedFloat32(pushIndex(4), v)
    fun pushDouble(v: Double) = stack.setUnalignedFloat64(pushIndex(8), v)
    fun pushRef(v: Any?) = refStack.add(v)

    fun popBool(): Boolean = popInt() != 0
    fun popInt(): Int = stack.getUnalignedInt32(popIndex(4))
    fun popLong(): Long = stack.getUnalignedInt64(popIndex(8))
    fun popFloat(): Float = stack.getUnalignedFloat32(popIndex(4))
    fun popDouble(): Double = stack.getUnalignedFloat64(popIndex(8))
    fun popRef(): Any? = refStack.removeLast()

    fun popWithType(type: WasmSType): Any {
        return when (type) {
            VOID -> Unit
            I32 -> popInt()
            I64 -> popLong()
            F32 -> popFloat()
            F64 -> popDouble()
            V128 -> TODO()
            ANYREF -> TODO()
            FUNCREF -> TODO()
        }
    }
    fun pushWithType(type: WasmSType, value: Any?) {
        when (type) {
            VOID -> Unit
            I32 -> pushInt(value as Int)
            I64 -> pushLong(value as Long)
            F32 -> pushFloat(value as Float)
            F64 -> pushDouble(value as Double)
            V128 -> TODO()
            ANYREF, FUNCREF -> pushRef(value)
        }
    }

    class Value(val type: WasmSType, val id: Int) {
        var i32: Int = 0
        var i64: Long = 0
        var f32: Float = 0f
        var f64: Double = 0.0
        var ref: Any? = null

        var value: Any?
            get() = when (type) {
                I32 -> i32
                I64 -> i64
                F32 -> f32
                F64 -> f64
                ANYREF, FUNCREF -> ref
                else -> Unit
            }
            set(value) = when (type) {
                I32 -> i32 = value as Int
                I64 -> i64 = value as Long
                F32 -> f32 = value as Float
                F64 -> f64 = value as Double
                ANYREF, FUNCREF -> ref = value
                else -> Unit
            }

        override fun toString(): String = "Value[$id][$type]=$value"
    }

    var locals: List<Value> = emptyList()
    val globals = module.globals.map { Value(it.globalType.toWasmSType(), it.index) }

    fun setValueFromStack(value: Value, consume: Boolean = true) {
        when (value.type) {
            I32 -> {
                value.i32 = popInt()
                if (!consume) pushInt(value.i32)
            }
            I64 -> {
                value.i64 = popLong()
                if (!consume) pushLong(value.i64)
            }
            F32 -> {
                value.f32 = popFloat()
                if (!consume) pushFloat(value.f32)
            }
            F64 -> {
                value.f64 = popDouble()
                if (!consume) pushDouble(value.f64)
            }
            ANYREF, FUNCREF -> {
                value.ref = popRef()
                if (!consume) pushRef(value.ref)
            }
            else -> TODO("${value.type} : $value")
        }
    }

    fun pushValue(g: Value) {
        when (g.type) {
            I32 -> pushInt(g.i32)
            I64 -> pushLong(g.i64)
            F32 -> pushFloat(g.f32)
            F64 -> pushDouble(g.f64)
            else -> TODO()
        }
    }

    fun initGlobals(): WasmRunInterpreter {
        if (trace) println("INIT MEMORIES:")
        for (data in module.datas) {
            interpret(data.e)
            val address = popInt()
            if (trace) println(" MEM[${data.memindex}][${data.index}][$address] -> ${data.data.size}")
            memory.setArrayInt8(address, data.data)
        }
        if (trace) println("INIT GLOBALS:")
        for ((index, global) in module.globals.withIndex()) {
            if (trace) println("GLOBAL: $global")
            interpret(global.expr)
            val global = globals[index]
            setValueFromStack(global)
            if (trace) println(" -> $global")
        }
        if (trace) println("INIT FUNC ${module.startFunc}:")
        if (module.startFunc >= 0) {
            invoke(module.functions[module.startFunc])
        }
        return this
    }

    fun interpret(expr: WasmExpr?) {
        if (expr == null) return
        if (expr.interpreterCode == null) {
            expr.interpreterCode = WasmInterpreterInstructionWriter(module).write(expr).toCode()
        }
        interpret(expr.interpreterCode)
    }

    val functions = LinkedHashMap<String, LinkedHashMap<String, WasmRuntime.(Array<Any?>) -> Any?>>()

    override val exported: Set<String> get() = functions.keys

    override fun register(moduleName: String, name: String, func: WasmRuntime.(Array<Any?>) -> Any?) {
        val moduleFunctions = functions.getOrPut(moduleName) { linkedMapOf() }
        moduleFunctions[name] = func
    }

    override operator fun invoke(funcName: String, vararg params: Any?): Any? {
        return invoke(module.exportsByName[funcName]?.obj as? WasmFunc? ?: error("Can't find function $funcName"), *params)
    }

    operator fun invoke(func: WasmFunc, vararg params: Any?): Any? {
        if (func.importFunc != null) {
            return func.importFunc?.invoke(this, params as Array<Any?>) ?: Unit
        }
        val import = func.import
        if (import != null) {
            func.importFunc = functions.get(import.moduleName)?.get(import.name) ?: error("Can't find imported function $import")
            return invoke(func, *params)
        }
        if (func.code == null) error("Code is null in $func")
        if (func.code?.interpreterCode == null) {
            func.code?.interpreterCode = WasmInterpreterInstructionWriter(module).write(func).toCode()
        }
        val oldlocals = locals.toList()
        val oldStackTop = stackTop

        stackTop = stackPos
        //println(func.rlocals)
        locals = func.rlocals.withIndex().map { (index, it) -> Value(it.type.toWasmSType(), index) }
        for ((index, param) in params.withIndex()) {
            locals[index].value = param
        }
        //println(func.code?.body)
        interpret(func.code?.interpreterCode!!, 0)
        if (trace) println("FUNC stackPos: $stackPos")
        val res: Any? = when (func.type.retType.toWasmSType()) {
            VOID -> Unit
            I32 -> popInt()
            I64 -> popLong()
            F32 -> popFloat()
            F64 -> popDouble()
            V128 -> TODO()
            ANYREF, FUNCREF -> popRef()
        }
        if (trace) println("FUNC RES: $res ${if (res != null) res::class else null}")

        stackTop = oldStackTop
        locals = oldlocals

        return res
    }

    fun interpret(code: WasmInterpreterCode?, index: Int = 0) {
        if (code == null) return
        var index = index
        while (index >= 0) {
            index = interpretOne(code, index)
        }
    }

    inline fun binopInt(block: (l: Int, r: Int) -> Int) {
        val r = popInt()
        val l = popInt()
        pushInt(block(l, r))
    }

    inline fun binopIntBool(reason: String? = null, block: (l: Int, r: Int) -> Boolean) {
        val r = popInt()
        val l = popInt()
        if (trace && reason != null) println(" -- BINOP: $reason l=$l, r=$r")
        pushInt(block(l, r))
    }

    inline fun binopLong(block: (l: Long, r: Long) -> Long) {
        val r = popLong()
        val l = popLong()
        pushLong(block(l, r))
    }

    inline fun binopFloat(block: (l: Float, r: Float) -> Float) {
        val r = popFloat()
        val l = popFloat()
        pushFloat(block(l, r))
    }

    inline fun binopDouble(block: (l: Double, r: Double) -> Double) {
        val r = popDouble()
        val l = popDouble()
        pushDouble(block(l, r))
    }

    inline fun binopLongBool(block: (l: Long, r: Long) -> Boolean) {
        val r = popLong()
        val l = popLong()
        pushInt(block(l, r))
    }

    inline fun binopDoubleBool(block: (l: Double, r: Double) -> Boolean) {
        val r = popDouble()
        val l = popDouble()
        pushInt(block(l, r))
    }

    fun interpretOne(code: WasmInterpreterCode, index: Int): Int {
        if (index >= code.instructions.size) return -1
        val i = WasmIntInstruction(code.instructions[index])
        if (trace) println("I[$index]: $i")
        val ins = i.ins
        val kind = i.kind
        val extra = i.extra
        val param = i.param

        when (ins) {
            WasmIntInstruction.GET_GLOBAL -> {
                pushValue(globals[param])
                if (trace) println("  -> GET_GLOBAL: ${globals[param]}")
            }
            WasmIntInstruction.SET_GLOBAL -> {
                setValueFromStack(globals[param])
                if (trace) println("  -> SET_GLOBAL: ${globals[param]}")
            }
            WasmIntInstruction.GET_LOCAL -> {
                pushValue(locals[param])
                if (trace) println("  -> GET_LOCAL: ${locals[param]}")
            }
            WasmIntInstruction.SET_LOCAL -> {
                setValueFromStack(locals[param])
                if (trace) println("  -> SET_LOCAL: ${locals[param]}")
            }
            WasmIntInstruction.TEE_LOCAL -> {
                setValueFromStack(locals[param])
                pushValue(locals[param])
                if (trace) println("  -> TEE_LOCAL: ${locals[param]}")
            }
            WasmIntInstruction.SET_MEM -> {
                when (kind) {
                    VOID -> TODO("$i")
                    I32 -> {
                        val value = popInt()
                        val base = popInt()
                        val offset = param
                        val address = base + offset
                        if (trace) println("SET_MEM: address=$address ($base + $offset), value=$value")
                        when (extra) {
                            WasmIntInstruction.EXTEND8_S -> memory.setUnalignedInt8(address, value)
                            WasmIntInstruction.EXTEND16_S -> memory.setUnalignedInt16(address, value.toShort())
                            WasmIntInstruction.EXTEND_NONE, WasmIntInstruction.EXTEND32_S -> memory.setUnalignedInt32(address, value)
                            else -> TODO("$i : extra=$extra")
                        }
                    }
                    I64 -> {
                        val value = popLong()
                        val address = popInt() + param
                        if (trace) println("SET_MEM: address=$address, value=$value")
                        when (extra) {
                            0 -> memory.setUnalignedInt64(address, value)
                            else -> TODO("$i")
                        }
                    }
                    F32 -> TODO("$i")
                    F64 -> TODO("$i")
                    V128 -> TODO("$i")
                    ANYREF -> TODO()
                    FUNCREF -> TODO()
                }
            }
            WasmIntInstruction.GET_MEM -> {
                val address = popInt() + param
                when (kind) {
                    VOID -> TODO("$i")
                    I32 -> when (extra) {
                        WasmIntInstruction.EXTEND_NONE, WasmIntInstruction.EXTEND32_S -> pushInt(memory.getUnalignedInt32(address))
                        WasmIntInstruction.EXTEND8_S -> pushInt(memory.getUnalignedInt8(address).toInt())
                        WasmIntInstruction.EXTEND8_U -> pushInt(memory.getUnalignedUInt8(address).toInt())
                        WasmIntInstruction.EXTEND16_S -> pushInt(memory.getUnalignedInt16(address).toInt())
                        WasmIntInstruction.EXTEND16_U -> pushInt(memory.getUnalignedUInt16(address).toInt())
                        else -> TODO("$i : extra=$extra")
                    }
                    I64 -> when (extra) {
                        WasmIntInstruction.EXTEND_NONE, WasmIntInstruction.EXTEND64_S -> pushLong(memory.getUnalignedInt64(address))
                        WasmIntInstruction.EXTEND8_S -> pushLong(memory.getUnalignedInt8(address).toLong())
                        WasmIntInstruction.EXTEND8_U -> pushLong(memory.getUnalignedUInt8(address).toLong())
                        WasmIntInstruction.EXTEND16_S -> pushLong(memory.getUnalignedInt16(address).toLong())
                        WasmIntInstruction.EXTEND16_U -> pushLong(memory.getUnalignedUInt16(address).toLong())
                        WasmIntInstruction.EXTEND32_S -> pushLong(memory.getUnalignedInt32(address).toLong())
                        WasmIntInstruction.EXTEND32_U -> pushLong(memory.getUnalignedInt32(address).toUInt().toLong())
                        else -> TODO("$i : extra=$extra")
                    }
                    F32 -> pushFloat(memory.getUnalignedFloat32(address))
                    F64 -> pushDouble(memory.getUnalignedFloat64(address))
                    V128 -> TODO("$i")
                    ANYREF -> TODO()
                    FUNCREF -> TODO()
                }
            }
            WasmIntInstruction.JUMP -> {
                return param
            }
            WasmIntInstruction.JUMP_IF -> {
                val result = popInt()
                if (result != 0) return param
            }
            WasmIntInstruction.JUMP_IF_NOT -> {
                val result = popInt()
                if (result == 0) return param
            }
            WasmIntInstruction.SELECT -> {
                when (kind) {
                    I32 -> {
                        val result = popInt()
                        val vFalse = popInt()
                        val vTrue = popInt()
                        pushInt(if (result != 0) vTrue else vFalse)
                    }
                    else -> TODO()
                }
            }
            //PUSH_CONST -> {
            //    when (kind) {
            //        I32 -> pushInt(code.intPool[param])
            //        I64 -> pushLong(code.longPool[param])
            //        F32 -> pushFloat(code.floatPool[param])
            //        F64 -> pushDouble(code.doublePool[param])
            //        else -> unreachable
            //    }
            //}
            else -> {
                when (kind) {
                    I32 -> when (ins) {
                        WasmIntInstruction.PUSH_CONST -> pushInt(code.intPool[param])
                        WasmIntInstruction.PUSH_CONST_SHORT -> pushInt(param)
                        WasmIntInstruction.BINOP_ADD -> binopInt { l, r -> (l + r).also { if (trace) println(" --[ADD] $l + $r == ${l + r}") } }
                        WasmIntInstruction.BINOP_SUB -> binopInt { l, r -> (l - r).also { if (trace) println(" --[SUB] $l - $r == ${l - r}") } }
                        WasmIntInstruction.BINOP_MUL -> binopInt { l, r -> l * r }
                        WasmIntInstruction.BINOP_DIV_S -> binopInt { l, r -> l / r }
                        WasmIntInstruction.BINOP_DIV_U -> binopInt { l, r -> (l.toUInt() / r.toUInt()).toInt() }
                        WasmIntInstruction.BINOP_REM_S -> binopInt { l, r -> l % r }
                        WasmIntInstruction.BINOP_REM_U -> binopInt { l, r -> (l.toUInt() % r.toUInt()).toInt() }
                        WasmIntInstruction.BINOP_AND -> binopInt { l, r -> (l and r).also { if (trace) println(" --[AND] $l & $r == ${l and r}") } }
                        WasmIntInstruction.BINOP_OR -> binopInt { l, r -> l or r }
                        WasmIntInstruction.BINOP_XOR -> binopInt { l, r -> l xor r }
                        WasmIntInstruction.BINOP_SHL -> binopInt { l, r -> l shl r }
                        WasmIntInstruction.BINOP_SHR_S -> binopInt { l, r -> l shr r }
                        WasmIntInstruction.BINOP_SHR_U -> binopInt { l, r -> l ushr r }
                        WasmIntInstruction.BINOP_ROTL -> binopInt { l, r -> l.rotateLeft(r) }
                        WasmIntInstruction.BINOP_ROTR -> binopInt { l, r -> l.rotateRight(r) }
                        WasmIntInstruction.BINOP_COMP_ZERO -> pushInt(popInt() == 0)
                        WasmIntInstruction.BINOP_COMP -> {
                            when (param) {
                                WasmIntInstruction.COMP_EQ -> binopIntBool { l, r -> l == r }
                                WasmIntInstruction.COMP_NE -> binopIntBool { l, r -> l != r }
                                WasmIntInstruction.COMP_LT_U -> binopIntBool("<u") { l, r -> (l.toUInt() < r.toUInt()).also { if (trace) println("  -- COMP_LT_U: ${l.toUInt()} < ${r.toUInt()}") } }
                                WasmIntInstruction.COMP_LE_U -> binopIntBool { l, r -> l.toUInt() <= r.toUInt() }
                                WasmIntInstruction.COMP_GT_U -> binopIntBool { l, r -> l.toUInt() > r.toUInt() }
                                WasmIntInstruction.COMP_GE_U -> binopIntBool { l, r -> l.toUInt() >= r.toUInt() }
                                WasmIntInstruction.COMP_LT_S -> binopIntBool { l, r -> l < r }
                                WasmIntInstruction.COMP_LE_S -> binopIntBool { l, r -> l <= r }
                                WasmIntInstruction.COMP_GT_S -> binopIntBool { l, r -> l > r }
                                WasmIntInstruction.COMP_GE_S -> binopIntBool { l, r -> l >= r }
                                else -> TODO("$i")
                            }
                        }
                        WasmIntInstruction.RETURN -> {
                            return -1
                        }
                        WasmIntInstruction.CALL -> {
                            val func = module.functions[i.param]
                            val params = func.type.args.reversed().map { popWithType(it.type.toWasmSType()) }.reversed()
                            if (trace) {
                                println("CALLING: $params : ${params.map { it::class }} : $func")
                            }
                            val res = invoke(func, *params.toTypedArray())
                            pushWithType(func.type.retType.toWasmSType(), res)
                        }
                        WasmIntInstruction.MEM_OP -> {
                            when (extra) {
                                WasmIntInstruction.MEMOP_SIZE -> {
                                    pushInt(WasmRuntime.Op_memory_size(this))
                                }
                                WasmIntInstruction.MEMOP_FILL -> {
                                    val count = popInt()
                                    val value = popInt()
                                    val dst = popInt()
                                    WasmRuntime.Op_memory_fill(dst, value, count, this)
                                }
                                WasmIntInstruction.MEMOP_COPY -> {
                                    val count = popInt()
                                    val src = popInt()
                                    val dst = popInt()
                                    WasmRuntime.Op_memory_copy(dst, src, count, this)
                                }
                                else -> TODO("MEM_OP: $extra")
                            }
                        }
                        WasmIntInstruction.UNOP_CAST -> {
                            when (param) {
                                WasmIntInstruction.EXTEND8_S -> pushInt(popInt().toByte().toInt())
                                WasmIntInstruction.EXTEND16_S -> pushInt(popInt().toShort().toInt())
                                WasmIntInstruction.EXTEND64_S -> pushInt(popLong().toInt())
                                WasmIntInstruction.TRUNC_SAT_F64_U -> pushInt(Op_i32_trunc_sat_f64_u(popDouble()))
                                else -> unexpected("unsupported unop_cast $param")
                            }
                        }
                        WasmIntInstruction.UNOP_CLZ -> pushInt(WasmRuntime.Op_i32_clz(popInt()))
                        WasmIntInstruction.UNOP_CTZ -> pushInt(WasmRuntime.Op_i32_ctz(popInt()))
                        WasmIntInstruction.UNOP_POPCNT -> pushInt(WasmRuntime.Op_i32_popcnt(popInt()))
                        WasmIntInstruction.UNREACHABLE -> {
                            println("!!UNREACHABLE!!")
                            //TODO("Unreachable")
                        }
                        WasmIntInstruction.DROP -> popInt()
                        else -> TODO("$i")
                    }

                    I64 -> when (ins) {
                        WasmIntInstruction.PUSH_CONST -> pushLong(code.longPool[param])
                        WasmIntInstruction.PUSH_CONST_SHORT -> pushLong(param.toLong())
                        WasmIntInstruction.BINOP_ADD -> binopLong { l, r -> l + r }
                        WasmIntInstruction.BINOP_SUB -> binopLong { l, r -> l - r }
                        WasmIntInstruction.BINOP_MUL -> binopLong { l, r -> l * r }
                        WasmIntInstruction.BINOP_DIV_S -> binopLong { l, r -> l / r }
                        WasmIntInstruction.BINOP_DIV_U -> binopLong { l, r -> (l.toULong() / r.toULong()).toLong() }
                        WasmIntInstruction.BINOP_REM_S -> binopLong { l, r -> l % r }
                        WasmIntInstruction.BINOP_REM_U -> binopLong { l, r -> (l.toULong() % r.toULong()).toLong() }
                        WasmIntInstruction.BINOP_AND -> binopLong { l, r -> l and r }
                        WasmIntInstruction.BINOP_OR -> binopLong { l, r -> l or r }
                        WasmIntInstruction.BINOP_XOR -> binopLong { l, r -> l xor r }
                        WasmIntInstruction.BINOP_SHL -> binopLong { l, r -> l shl r.toInt() }
                        WasmIntInstruction.BINOP_SHR_S -> binopLong { l, r -> l shr r.toInt() }
                        WasmIntInstruction.BINOP_SHR_U -> binopLong { l, r -> l ushr r.toInt() }
                        WasmIntInstruction.BINOP_ROTL -> binopLong { l, r -> WasmRuntime.Op_i64_rotl(l, r) }
                        WasmIntInstruction.BINOP_ROTR -> binopLong { l, r -> WasmRuntime.Op_i64_rotr(l, r) }
                        WasmIntInstruction.BINOP_COMP_ZERO -> pushInt(popLong() == 0L)
                        WasmIntInstruction.BINOP_COMP -> when (param) {
                            WasmIntInstruction.COMP_EQ -> binopLongBool { l, r -> l == r }
                            WasmIntInstruction.COMP_NE -> binopLongBool { l, r -> l != r }
                            WasmIntInstruction.COMP_LT_U -> binopLongBool { l, r -> l.toULong() < r.toULong() }
                            WasmIntInstruction.COMP_LE_U -> binopLongBool { l, r -> l.toULong() <= r.toULong() }
                            WasmIntInstruction.COMP_GT_U -> binopLongBool { l, r -> l.toULong() > r.toULong() }
                            WasmIntInstruction.COMP_GE_U -> binopLongBool { l, r -> l.toULong() >= r.toULong() }
                            WasmIntInstruction.COMP_LT_S -> binopLongBool { l, r -> l < r }
                            WasmIntInstruction.COMP_LE_S -> binopLongBool { l, r -> l <= r }
                            WasmIntInstruction.COMP_GT_S -> binopLongBool { l, r -> l > r }
                            WasmIntInstruction.COMP_GE_S -> binopLongBool { l, r -> l >= r }
                            else -> TODO("$i")
                        }
                        WasmIntInstruction.RETURN -> {
                            return -1
                        }
                        WasmIntInstruction.UNOP_CAST -> {
                            when (param) {
                                WasmIntInstruction.EXTEND8_S -> pushLong(popInt().toByte().toLong())
                                WasmIntInstruction.EXTEND16_S -> pushLong(popInt().toShort().toLong())
                                WasmIntInstruction.EXTEND32_S -> pushLong(popInt().toLong())
                                WasmIntInstruction.EXTEND32_U -> pushLong(popInt().toLong() and 0xFFFFFFFFL)
                                else -> unexpected("error UNOP_CAST $param")
                            }
                        }
                        WasmIntInstruction.UNOP_CLZ -> pushLong(WasmRuntime.Op_i64_clz(popLong()))
                        WasmIntInstruction.UNOP_CTZ -> pushLong(WasmRuntime.Op_i64_ctz(popLong()))
                        WasmIntInstruction.UNOP_POPCNT -> pushLong(WasmRuntime.Op_i64_popcnt(popLong()))
                        WasmIntInstruction.UNOP_REINTERPRET -> pushLong(popLong())
                        else -> TODO("$i")
                    }
                    F32 -> when (ins) {
                        WasmIntInstruction.PUSH_CONST -> pushFloat(code.floatPool[param])
                        WasmIntInstruction.UNOP_ABS -> pushFloat(WasmRuntime.Op_f32_abs(popFloat()))
                        WasmIntInstruction.UNOP_NEG -> pushFloat(WasmRuntime.Op_f32_neg(popFloat()))
                        WasmIntInstruction.BINOP_MAX -> binopFloat { l, r -> kotlin.math.max(l, r) }
                        WasmIntInstruction.BINOP_MIN -> binopFloat { l, r -> kotlin.math.min(l, r) }
                        WasmIntInstruction.BINOP_ADD -> binopFloat { l, r -> l + r }
                        WasmIntInstruction.BINOP_SUB -> binopFloat { l, r -> l - r }
                        WasmIntInstruction.BINOP_MUL -> binopFloat { l, r -> l * r }
                        WasmIntInstruction.BINOP_DIV_S -> binopFloat { l, r -> l / r }
                        WasmIntInstruction.BINOP_COPYSIGN -> binopFloat { l, r -> WasmRuntime.Op_f32_copysign(l, r) }
                        else -> TODO("$i")
                    }
                    F64 -> {
                        when (ins) {
                            WasmIntInstruction.PUSH_CONST -> pushDouble(code.doublePool[param])
                            WasmIntInstruction.BINOP_MAX -> binopDouble { l, r -> kotlin.math.max(l, r) }
                            WasmIntInstruction.BINOP_MIN -> binopDouble { l, r -> kotlin.math.min(l, r) }
                            WasmIntInstruction.BINOP_ADD -> binopDouble { l, r -> l + r }
                            WasmIntInstruction.BINOP_SUB -> binopDouble { l, r -> l - r }
                            WasmIntInstruction.BINOP_MUL -> binopDouble { l, r -> l * r }
                            WasmIntInstruction.BINOP_DIV_S -> binopDouble { l, r -> l / r }
                            WasmIntInstruction.BINOP_COMP -> when (param) {
                                WasmIntInstruction.COMP_EQ -> binopDoubleBool { l, r -> l == r }
                                WasmIntInstruction.COMP_NE -> binopDoubleBool { l, r -> l != r }
                                WasmIntInstruction.COMP_LT_S -> binopDoubleBool { l, r -> l < r }
                                WasmIntInstruction.COMP_LE_S -> binopDoubleBool { l, r -> l <= r }
                                WasmIntInstruction.COMP_GT_S -> binopDoubleBool { l, r -> l > r }
                                WasmIntInstruction.COMP_GE_S -> binopDoubleBool { l, r -> l >= r }
                                else -> TODO("$i")
                            }
                            WasmIntInstruction.UNOP_CAST -> {
                                when (i.param) {
                                    WasmIntInstruction.CAST_I32_U -> pushDouble(Op_f64_convert_u_i32(popInt()))
                                    WasmIntInstruction.CAST_I32_S -> pushDouble(Op_f64_convert_s_i32(popInt()))
                                    WasmIntInstruction.CAST_I64_U -> pushDouble(Op_f64_convert_u_i64(popLong()))
                                    WasmIntInstruction.CAST_I64_S -> pushDouble(Op_f64_convert_s_i64(popLong()))
                                    else -> TODO("$i")
                                }
                            }
                            WasmIntInstruction.UNOP_REINTERPRET -> pushDouble(popDouble())
                            else -> TODO("$i")
                        }
                    }
                    VOID -> TODO("$i")
                    V128 -> TODO("$i")
                    ANYREF, FUNCREF -> when (ins) {
                        WasmIntInstruction.PUSH_CONST_NULL -> pushRef(null)
                        else -> TODO("$i")
                    }
                }
            }
        }

        return index + 1
    }

    fun runAsserts() {
        var failed = 0
        var total = 0
        for (assert in module.asserts) {
            when (assert) {
                is WasmAssertReturn -> {
                    val msg = assert.msg
                    stackPos = 0
                    interpret(assert.actual)
                    interpret(assert.expect)
                    total++
                    //println("stackAvailable=$stackAvailable")
                    when {
                        refStack.size >= 1 -> {
                            val actual = popRef()
                            val expect = popRef()
                            failed += WasmRuntime.assert_return_ref(actual, expect, msg)
                        }
                        stackAvailable / 2 == 4 -> {
                            val actual = popInt()
                            val expect = popInt()
                            failed += WasmRuntime.assert_return_i32(actual, expect, msg)
                        }
                        stackAvailable / 2 == 8 || stackAvailable / 2 == 10 -> {
                            val actual = popLong()
                            val expect = popLong()
                            failed += WasmRuntime.assert_return_i64(actual, expect, msg)
                        }
                        else -> TODO("stackAvailable / 2=${stackAvailable / 2}")
                    }
                }
            }
        }
        WasmRuntime.assert_summary(failed, total)
    }


    class WasmInterpreterInstructionWriter(val module: WasmModule) {
        data class DeferredUpdateLabel(val label: LabelOrStructureControl, val instructionPtr: Int)

        interface LabelOrStructureControl {
            val pointer: Int
        }

        class Label(val index: Int) : LabelOrStructureControl {
            var stack = listOf<WasmType>()
            override var pointer: Int = -1

            override fun toString(): String = "Label[$index](pointer=$pointer)"
        }

        class StructureControl(val level: Int, val kind: Kind) : LabelOrStructureControl {
            enum class Kind { BLOCK, LOOP, IF, FUNC }

            override fun toString(): String = "StructureControl[$level]($kind, start=$start, end=$end, pointer=$pointer)"

            var start = -1
            var end = -1
            override val pointer: Int get() = when (kind) {
                Kind.BLOCK -> end
                Kind.LOOP -> start
                Kind.IF -> end
                Kind.FUNC -> end
            }
        }

        val controls = arrayListOf<StructureControl>()

        val allLabels = arrayListOf<Label>()
        val labels = arrayListOf<Label>()
        val inst = IntArrayList()
        val intPool = IntArrayList()
        val longPool = arrayListOf<Long>()
        val floatPool = FloatArrayList()
        val doublePool = DoubleArrayList()
        val current: Int get() = inst.size
        var locals: List<WastLocal> = emptyList()
        val typeStack = arrayListOf<WasmType>()
        val deferredUpdateLabels = arrayListOf<DeferredUpdateLabel>()

        fun finalize() {
            for (deferred in deferredUpdateLabels) {
                val pointer = deferred.label.pointer
                if (pointer < 0) error("Invalid pointer for $deferred")
                inst[deferred.instructionPtr] = WasmIntInstruction(inst[deferred.instructionPtr]).copy(param = pointer).raw
            }
        }

        fun Label.set(): Label {
            this.pointer = current
            return this
        }

        fun popLabel(): Label {
            return labels.removeLast()
        }

        fun createLabel(): Label {
            val label = Label(labels.size - 1)
            allLabels.add(label)
            return label
        }

        fun pushLabel(): Label {
            val label = createLabel()
            labels.add(label)
            return label
        }

        fun genINS(kind: Int, type: WasmSType, index: Int = 0, extra: Int = 0): Int {
            return WasmIntInstruction(kind, type, index, extra).raw
        }

        fun writeINS(kind: Int, type: WasmSType = I32, index: Int = 0, extra: Int = 0) {
            inst.add(genINS(kind, type, index, extra))
        }

        fun writeJump(kind: Int, label: LabelOrStructureControl) {
            deferredUpdateLabels += DeferredUpdateLabel(label, inst.size)
            inst.add(genINS(kind, WasmSType.VOID, 0))
        }

        fun getGlobalType(index: Int): WasmType = module.globals[index].globalType
        fun getLocalType(index: Int): WasmType = locals[index].type

        fun removeStack(i: WasmInstruction): List<WasmType> {
            val removed = arrayListOf<WasmType>()
            when {
                i.op.istack > 0 -> {
                    repeat(i.op.istack) {
                        if (typeStack.isEmpty()) error("Empty stack at $i")
                        removed += typeStack.removeLast()
                    }
                }
                i.op.istack == 0 -> Unit
                else -> {
                    when (i) {
                        is WasmInstruction.block -> {
                            Unit
                        }
                        is WasmInstruction.IF -> {
                            Unit
                        }
                        is WasmInstruction.loop -> {
                            Unit // @TODO: What to do here?
                        }
                        is WasmInstruction.br -> {
                            Unit // @TODO: What to do here?
                        }
                        is WasmInstruction.br_table -> {
                            Unit // @TODO: What to do here?
                        }
                        is WasmInstruction.br_if -> {
                            Unit // @TODO: What to do here?
                        }
                        is WasmInstruction.End -> {
                            Unit // @TODO: What to do here?
                        }
                        is WasmInstruction.CALL -> {
                            val callTypes = module.functions[i.funcIdx].type.args.map { it.type }
                            repeat(callTypes.size) {
                                removed += typeStack.removeLast()
                            }
                        }
                        is WasmInstruction.RETURN -> {
                            repeat(typeStack.size) {
                                removed += typeStack.removeLast()
                            }
                        }
                        is WasmInstruction.unreachable -> {
                            Unit
                        }
                        is WasmInstruction.nop -> {
                            Unit
                        }
                        else -> TODO("$i")
                    }
                    //TODO("$i")
                }
            }
            return removed
        }

        fun write(i: WasmInstruction, level: Int): Boolean {
            //println("INSTRUCTION[$level][${labels.size}]: $i")
            var doContinue = true
            val removed = removeStack(i)

            when (i) {
                is WasmInstruction.Ins -> {
                    when (i.op) {
                        WasmOp.Op_i32_sub, WasmOp.Op_i64_sub, WasmOp.Op_f32_sub, WasmOp.Op_f64_sub -> writeINS(WasmIntInstruction.BINOP_SUB, i.itype, 0)
                        WasmOp.Op_i32_add, WasmOp.Op_i64_add, WasmOp.Op_f32_add, WasmOp.Op_f64_add -> writeINS(WasmIntInstruction.BINOP_ADD, i.itype, 0)
                        WasmOp.Op_i32_mul, WasmOp.Op_i64_mul, WasmOp.Op_f32_mul, WasmOp.Op_f64_mul -> writeINS(WasmIntInstruction.BINOP_MUL, i.itype, 0)
                        WasmOp.Op_i32_div_s, WasmOp.Op_i64_div_s, WasmOp.Op_f32_div, WasmOp.Op_f64_div -> writeINS(WasmIntInstruction.BINOP_DIV_S, i.itype, 0)
                        WasmOp.Op_i32_div_u, WasmOp.Op_i64_div_u -> writeINS(WasmIntInstruction.BINOP_DIV_U, i.itype, 0)
                        WasmOp.Op_i32_rem_s, WasmOp.Op_i64_rem_s -> writeINS(WasmIntInstruction.BINOP_REM_S, i.itype, 0)
                        WasmOp.Op_i32_rem_u, WasmOp.Op_i64_rem_u -> writeINS(WasmIntInstruction.BINOP_REM_U, i.itype, 0)
                        WasmOp.Op_i32_and, WasmOp.Op_i64_and -> writeINS(WasmIntInstruction.BINOP_AND, i.itype, 0)
                        WasmOp.Op_i32_shr_u, WasmOp.Op_i64_shr_u -> writeINS(WasmIntInstruction.BINOP_SHR_U, i.itype, 0)
                        WasmOp.Op_i32_shr_s, WasmOp.Op_i64_shr_s -> writeINS(WasmIntInstruction.BINOP_SHR_S, i.itype, 0)
                        WasmOp.Op_i32_shl, WasmOp.Op_i64_shl -> writeINS(WasmIntInstruction.BINOP_SHL, i.itype, 0)
                        WasmOp.Op_i32_rotl, WasmOp.Op_i64_rotl -> writeINS(WasmIntInstruction.BINOP_ROTL, i.itype, 0)
                        WasmOp.Op_i32_rotr, WasmOp.Op_i64_rotr -> writeINS(WasmIntInstruction.BINOP_ROTR, i.itype, 0)
                        WasmOp.Op_i32_or, WasmOp.Op_i64_or -> writeINS(WasmIntInstruction.BINOP_OR, i.itype, 0)
                        WasmOp.Op_i32_xor, WasmOp.Op_i64_xor -> writeINS(WasmIntInstruction.BINOP_XOR, i.itype, 0)
                        WasmOp.Op_i32_ctz, WasmOp.Op_i64_ctz -> writeINS(WasmIntInstruction.UNOP_CTZ, i.itype, 0)
                        WasmOp.Op_i32_clz, WasmOp.Op_i64_clz -> writeINS(WasmIntInstruction.UNOP_CLZ, i.itype, 0)
                        WasmOp.Op_i32_popcnt, WasmOp.Op_i64_popcnt -> writeINS(WasmIntInstruction.UNOP_POPCNT, i.itype, 0)

                        WasmOp.Op_f32_max, WasmOp.Op_f64_max -> writeINS(WasmIntInstruction.BINOP_MAX, i.itype, 0)
                        WasmOp.Op_f32_min, WasmOp.Op_f64_min -> writeINS(WasmIntInstruction.BINOP_MIN, i.itype, 0)
                        WasmOp.Op_f32_neg, WasmOp.Op_f64_neg -> writeINS(WasmIntInstruction.UNOP_NEG, i.itype, 0)
                        WasmOp.Op_f32_abs, WasmOp.Op_f64_abs -> writeINS(WasmIntInstruction.UNOP_ABS, i.itype, 0)

                        WasmOp.Op_i32_eq, WasmOp.Op_i32_ne, WasmOp.Op_i32_lt_u, WasmOp.Op_i32_le_u, WasmOp.Op_i32_gt_u,
                        WasmOp.Op_i32_ge_u, WasmOp.Op_i32_lt_s, WasmOp.Op_i32_le_s, WasmOp.Op_i32_gt_s, WasmOp.Op_i32_ge_s,
                        WasmOp.Op_i64_eq, WasmOp.Op_i64_ne, WasmOp.Op_i64_lt_u, WasmOp.Op_i64_le_u, WasmOp.Op_i64_gt_u,
                        WasmOp.Op_i64_ge_u, WasmOp.Op_i64_lt_s, WasmOp.Op_i64_le_s, WasmOp.Op_i64_gt_s, WasmOp.Op_i64_ge_s,
                        WasmOp.Op_f32_eq, WasmOp.Op_f32_ne, WasmOp.Op_f32_lt, WasmOp.Op_f32_le, WasmOp.Op_f32_gt,
                        WasmOp.Op_f32_ge, WasmOp.Op_f64_eq, WasmOp.Op_f64_ne, WasmOp.Op_f64_lt, WasmOp.Op_f64_le,
                        WasmOp.Op_f64_gt, WasmOp.Op_f64_ge -> {
                            writeINS(WasmIntInstruction.BINOP_COMP, i.itype, WasmIntInstruction.compFromStr(i.op.symbol))
                        }

                        WasmOp.Op_i32_eqz, WasmOp.Op_i64_eqz -> {
                            writeINS(WasmIntInstruction.BINOP_COMP_ZERO, i.itype, 0)
                        }

                        WasmOp.Op_select -> {
                            writeINS(WasmIntInstruction.SELECT, removed[1].toWasmSType(), 0)
                            //println("removed=$removed")
                        }
                        WasmOp.Op_drop -> {
                            writeINS(WasmIntInstruction.DROP, removed[0].toWasmSType(), 0)
                        }
                        WasmOp.Op_i64_extend8_s, WasmOp.Op_i64_extend16_s, WasmOp.Op_i64_extend32_s, WasmOp.Op_i64_extend_i32_u, WasmOp.Op_i64_extend_i32_s -> {
                            writeINS(WasmIntInstruction.UNOP_CAST, I64, when (i.op) {
                                WasmOp.Op_i64_extend8_s -> WasmIntInstruction.EXTEND8_S
                                WasmOp.Op_i64_extend16_s -> WasmIntInstruction.EXTEND16_S
                                WasmOp.Op_i64_extend32_s -> WasmIntInstruction.EXTEND32_S
                                WasmOp.Op_i64_extend_i32_u -> WasmIntInstruction.EXTEND32_U
                                WasmOp.Op_i64_extend_i32_s -> WasmIntInstruction.EXTEND32_S
                                else -> unreachable
                            })
                        }
                        WasmOp.Op_i32_extend8_s, WasmOp.Op_i32_extend16_s, WasmOp.Op_i32_wrap_i64 -> {
                            writeINS(WasmIntInstruction.UNOP_CAST, I32, when (i.op) {
                                WasmOp.Op_i32_extend8_s -> WasmIntInstruction.EXTEND8_S
                                WasmOp.Op_i32_extend16_s -> WasmIntInstruction.EXTEND16_S
                                WasmOp.Op_i32_wrap_i64 -> WasmIntInstruction.EXTEND64_S
                                else -> unreachable
                            })
                        }

                        WasmOp.Op_f32_convert_i32_u -> writeINS(WasmIntInstruction.UNOP_CAST, F32, WasmIntInstruction.CAST_I32_U)
                        WasmOp.Op_f32_convert_i32_s -> writeINS(WasmIntInstruction.UNOP_CAST, F32, WasmIntInstruction.CAST_I32_S)
                        WasmOp.Op_f32_convert_i64_u -> writeINS(WasmIntInstruction.UNOP_CAST, F32, WasmIntInstruction.CAST_I64_U)
                        WasmOp.Op_f32_convert_i64_s -> writeINS(WasmIntInstruction.UNOP_CAST, F32, WasmIntInstruction.CAST_I64_S)

                        WasmOp.Op_f64_convert_i32_u -> writeINS(WasmIntInstruction.UNOP_CAST, F64, WasmIntInstruction.CAST_I32_U)
                        WasmOp.Op_f64_convert_i32_s -> writeINS(WasmIntInstruction.UNOP_CAST, F64, WasmIntInstruction.CAST_I32_S)
                        WasmOp.Op_f64_convert_i64_u -> writeINS(WasmIntInstruction.UNOP_CAST, F64, WasmIntInstruction.CAST_I64_U)
                        WasmOp.Op_f64_convert_i64_s -> writeINS(WasmIntInstruction.UNOP_CAST, F64, WasmIntInstruction.CAST_I64_S)


                        WasmOp.Op_i32_trunc_sat_f32_s -> writeINS(WasmIntInstruction.UNOP_CAST, I32, WasmIntInstruction.TRUNC_SAT_F32_S)
                        WasmOp.Op_i32_trunc_sat_f32_u -> writeINS(WasmIntInstruction.UNOP_CAST, I32, WasmIntInstruction.TRUNC_SAT_F32_U)
                        WasmOp.Op_i32_trunc_sat_f64_s -> writeINS(WasmIntInstruction.UNOP_CAST, I32, WasmIntInstruction.TRUNC_SAT_F64_S)
                        WasmOp.Op_i32_trunc_sat_f64_u -> writeINS(WasmIntInstruction.UNOP_CAST, I32, WasmIntInstruction.TRUNC_SAT_F64_U)

                        WasmOp.Op_i32_reinterpret_f32 -> writeINS(WasmIntInstruction.UNOP_REINTERPRET, I32)
                        WasmOp.Op_f32_reinterpret_i32 -> writeINS(WasmIntInstruction.UNOP_REINTERPRET, F32)
                        WasmOp.Op_i64_reinterpret_f64 -> writeINS(WasmIntInstruction.UNOP_REINTERPRET, I64)
                        WasmOp.Op_f64_reinterpret_i64 -> writeINS(WasmIntInstruction.UNOP_REINTERPRET, F64)

                        WasmOp.Op_f32_copysign -> writeINS(WasmIntInstruction.BINOP_COPYSIGN, F32)

                        else -> TODO("$i : ${i::class}")
                    }
                }
                is WasmInstruction.InsMemarg -> {
                    when (i.op) {
                        WasmOp.Op_i32_load, WasmOp.Op_i64_load, WasmOp.Op_f32_load, WasmOp.Op_f64_load -> {
                            writeINS(WasmIntInstruction.GET_MEM, i.op.itype, i.offset, extra = WasmIntInstruction.EXTEND_NONE)
                        }
                        WasmOp.Op_i32_load8_s -> writeINS(WasmIntInstruction.GET_MEM, i.op.itype, i.offset, extra = WasmIntInstruction.EXTEND8_S)
                        WasmOp.Op_i32_load8_u -> writeINS(WasmIntInstruction.GET_MEM, i.op.itype, i.offset, extra = WasmIntInstruction.EXTEND8_U)
                        WasmOp.Op_i32_load16_s -> writeINS(WasmIntInstruction.GET_MEM, i.op.itype, i.offset, extra = WasmIntInstruction.EXTEND16_S)
                        WasmOp.Op_i32_load16_u -> writeINS(WasmIntInstruction.GET_MEM, i.op.itype, i.offset, extra = WasmIntInstruction.EXTEND16_U)

                        WasmOp.Op_i64_load8_s -> writeINS(WasmIntInstruction.GET_MEM, i.op.itype, i.offset, extra = WasmIntInstruction.EXTEND8_S)
                        WasmOp.Op_i64_load8_u -> writeINS(WasmIntInstruction.GET_MEM, i.op.itype, i.offset, extra = WasmIntInstruction.EXTEND8_U)
                        WasmOp.Op_i64_load16_s -> writeINS(WasmIntInstruction.GET_MEM, i.op.itype, i.offset, extra = WasmIntInstruction.EXTEND16_S)
                        WasmOp.Op_i64_load16_u -> writeINS(WasmIntInstruction.GET_MEM, i.op.itype, i.offset, extra = WasmIntInstruction.EXTEND16_U)
                        WasmOp.Op_i64_load32_s -> writeINS(WasmIntInstruction.GET_MEM, i.op.itype, i.offset, extra = WasmIntInstruction.EXTEND32_S)
                        WasmOp.Op_i64_load32_u -> writeINS(WasmIntInstruction.GET_MEM, i.op.itype, i.offset, extra = WasmIntInstruction.EXTEND32_U)

                        WasmOp.Op_i32_store, WasmOp.Op_i64_store, WasmOp.Op_f32_store, WasmOp.Op_f64_store -> {
                            writeINS(WasmIntInstruction.SET_MEM, i.op.itype, i.offset, extra = WasmIntInstruction.EXTEND_NONE)
                        }
                        WasmOp.Op_i32_store8 -> writeINS(WasmIntInstruction.SET_MEM, I32, i.offset, extra = WasmIntInstruction.EXTEND8_S)
                        WasmOp.Op_i32_store16 -> writeINS(WasmIntInstruction.SET_MEM, I32, i.offset, extra = WasmIntInstruction.EXTEND16_S)

                        else -> TODO("${i.op}")
                    }
                }
                is WasmInstruction.InsInt -> {
                    when (i.op) {
                        WasmOp.Op_global_get, WasmOp.Op_global_set -> {
                            val op = if (i.op == WasmOp.Op_global_get) WasmIntInstruction.GET_GLOBAL else WasmIntInstruction.SET_GLOBAL
                            writeINS(op, module.globals[i.param].globalType.toWasmSType(), i.param)
                        }
                        WasmOp.Op_local_tee, WasmOp.Op_local_set, WasmOp.Op_local_get -> {
                            val localType = locals[i.param].type.toWasmSType()
                            val iop = when (i.op) {
                                WasmOp.Op_local_get -> WasmIntInstruction.GET_LOCAL
                                WasmOp.Op_local_set -> WasmIntInstruction.SET_LOCAL
                                WasmOp.Op_local_tee -> WasmIntInstruction.TEE_LOCAL
                                else -> TODO()
                            }
                            writeINS(iop, localType, i.param)
                        }
                        WasmOp.Op_memory_copy -> writeINS(WasmIntInstruction.MEM_OP, I32, 0, WasmIntInstruction.MEMOP_COPY)
                        WasmOp.Op_memory_fill -> writeINS(WasmIntInstruction.MEM_OP, I32, 0, WasmIntInstruction.MEMOP_FILL)
                        WasmOp.Op_memory_size -> writeINS(WasmIntInstruction.MEM_OP, I32, i.param, extra = WasmIntInstruction.MEMOP_SIZE)
                        WasmOp.Op_memory_grow -> writeINS(WasmIntInstruction.MEM_OP, I32, i.param, extra = WasmIntInstruction.MEMOP_GROW)

                        else -> TODO("$i : ${i::class}")
                    }
                }
                is WasmInstruction.InsConstInt -> {
                    if (i.value >= -524287 && i.value <= 524287) {
                        writeINS(WasmIntInstruction.PUSH_CONST_SHORT, i.type, i.value)
                    } else {
                        writeINS(WasmIntInstruction.PUSH_CONST, i.type, intPool.size).also { intPool.add(i.value) }
                    }
                }
                is WasmInstruction.InsConstLong -> {
                    if (i.value >= -524287L && i.value <= 524287L) {
                        writeINS(WasmIntInstruction.PUSH_CONST_SHORT, i.type, i.value.toInt())
                    } else {
                        writeINS(WasmIntInstruction.PUSH_CONST, i.type, longPool.size).also { longPool.add(i.value) }
                    }
                }
                is WasmInstruction.InsConstFloat -> writeINS(WasmIntInstruction.PUSH_CONST, F32, floatPool.size).also { floatPool.add(i.value) }
                is WasmInstruction.InsConstDouble -> writeINS(WasmIntInstruction.PUSH_CONST, F64, doublePool.size).also { doublePool.add(i.value) }
                is WasmInstruction.block -> {
                    val label = pushLabel()
                    write(i.expr, level + 1, StructureControl.Kind.BLOCK, typeStack.toList())
                    label.set()
                    popLabel()
                }
                is WasmInstruction.loop -> {
                    val label = pushLabel()
                    label.stack = typeStack.toList()
                    label.set()
                    write(i.expr, level + 1, StructureControl.Kind.LOOP, typeStack.toList())
                    popLabel()
                }
                is WasmInstruction.IF -> {
                    val endLabel = createLabel()
                    val elseLabel = createLabel()

                    endLabel.stack = typeStack.toList() + i.b

                    writeJump(WasmIntInstruction.JUMP_IF_NOT, label = elseLabel)
                    write(i.btrue, level + 1, StructureControl.Kind.IF, typeStack.toList())
                    writeJump(WasmIntInstruction.JUMP, label = endLabel)
                    elseLabel.set()
                    write(i.bfalse, level + 1, StructureControl.Kind.IF, typeStack.toList())
                    endLabel.set()
                }
                is WasmInstruction.br -> {
                    val jumpLabel = controls[controls.size - 1 - i.label]
                    writeJump(WasmIntInstruction.JUMP, label = jumpLabel)
                }
                is WasmInstruction.br_if -> {
                    val jumpLabel = controls[controls.size - 1 - i.label]
                    writeJump(WasmIntInstruction.JUMP_IF, label = jumpLabel)
                }
                is WasmInstruction.br_table -> {
                    //val labels = i.labels.map { controls[controls.size - 1 - it] }
                    //val defaultLabel = controls[controls.size - 1 - i.default]
                    //writeJump(WasmIntInstruction.JUMP_TABLE, label = defaultLabel)
                    TODO("br_table")
                }
                is WasmInstruction.CALL -> {
                    writeINS(WasmIntInstruction.CALL, index = i.funcIdx)
                }
                is WasmInstruction.End -> {
                    doContinue = false
                }
                is WasmInstruction.unreachable -> {
                    writeINS(WasmIntInstruction.UNREACHABLE)
                }
                is WasmInstruction.RETURN -> {
                    writeINS(WasmIntInstruction.RETURN)
                }
                is WasmInstruction.InsType -> {
                    when (i.op) {
                        WasmOp.Op_ref_null -> writeINS(WasmIntInstruction.PUSH_CONST_NULL, i.param.toWasmSType())
                        else -> TODO()
                    }
                }
                is WasmInstruction.nop -> {
                    Unit
                }
                else -> TODO("$i : ${i::class}")
            }

            when {
                i.op.rstack == 1 && i.op.outType != WasmType.void -> typeStack.add(i.op.outType)
                i.op.rstack == 0 -> Unit
                else -> {
                    when (i) {
                        is WasmInstruction.Ins -> {
                            when (i.op) {
                                WasmOp.Op_select -> typeStack.add(removed[1])
                                WasmOp.Op_drop -> Unit
                                else -> TODO("${i.op}")
                            }
                        }
                        is WasmInstruction.InsInt -> {
                            when (i.op) {
                                WasmOp.Op_global_get -> typeStack.add(getGlobalType(i.param))
                                WasmOp.Op_local_get, WasmOp.Op_local_tee -> typeStack.add(getLocalType(i.param))
                                else -> TODO("${i.op}")
                            }
                        }
                        is WasmInstruction.block -> if (i.b != WasmType.void) typeStack.add(i.b)
                        is WasmInstruction.IF -> if (i.b != WasmType.void) typeStack.add(i.b)
                        is WasmInstruction.br, is WasmInstruction.br_if -> {
                            Unit // @TODO?
                        }
                        is WasmInstruction.End -> {
                            Unit // @TODO?
                        }
                        is WasmInstruction.loop -> {
                            Unit // @TODO?
                        }
                        is WasmInstruction.CALL -> {
                            val retType = module.functions[i.funcIdx].type.retType
                            if (retType != WasmType.void) typeStack.add(retType)
                        }
                        is WasmInstruction.unreachable -> {
                            Unit // @TODO?
                        }
                        is WasmInstruction.RETURN -> {
                            Unit // @TODO?
                            doContinue = false
                        }
                        is WasmInstruction.nop -> {
                            Unit
                        }
                        else -> TODO("${i.op} : ${i::class}")
                    }

                }
            }

            return doContinue
        }

        fun write(expr: WasmExpr?, level: Int = 0, kind: StructureControl.Kind = StructureControl.Kind.FUNC, stack: List<WasmType> = emptyList()): WasmInterpreterInstructionWriter {
            if (expr == null) return this
            val oldStack = this.typeStack.toList()
            this.typeStack.clear()
            this.typeStack.addAll(stack)
            val control = StructureControl(level, kind)
            controls.add(control)
            control.start = inst.size
            for (i in expr.instructions) {
                if (!write(i, level)) break
            }
            control.end = inst.size
            controls.removeLast()

            this.typeStack.clear()
            this.typeStack.addAll(oldStack)
            return this
        }

        fun write(func: WasmFunc?, finalize: Boolean = true): WasmInterpreterInstructionWriter {
            if (func == null) return this
            locals = func.rlocals
            write(func.code?.body, 0, StructureControl.Kind.FUNC, emptyList())
            if (finalize) finalize()
            return this
        }

        fun toCode(): WasmInterpreterCode {
            return WasmInterpreterCode(inst.toIntArray(), intPool.toIntArray(), longPool.toLongArray(), floatPool.toFloatArray(), doublePool.toDoubleArray())
        }
    }


    class WasmInterpreterCode(
        val instructions: IntArray,
        val intPool: IntArray,
        val longPool: LongArray,
        val floatPool: FloatArray,
        val doublePool: DoubleArray
    )

    inline class WasmIntInstruction(val raw: Int) {
        val ins get() = raw.extract6(0)
        val kind get() = WasmSType.entries[raw.extract3(6)]
        val extra get() = raw.extract3(9)
        val param get() = raw.extractSigned(12, 20)

        override fun toString(): String {
            return buildString {
                append("Instruction(")
                append(ioName(ins))
                append(", ")
                append(kind)
                append(", ")
                append(extra)
                append(", ")
                append(param)
                if (ins == BINOP_COMP) {
                    append(": ")
                    append(compName(param))
                }
                append(")")
            }
        }

        fun copy(
            ins: Int = this.ins,
            kind: WasmSType = this.kind,
            param: Int = this.param,
            extra: Int = this.extra,
        ): WasmIntInstruction = WasmIntInstruction(ins, kind, param, extra)

        companion object {
            fun Int.fitsIn(bits: Int): Boolean {
                val rest = this ushr bits
                return rest == 0
            }
            fun Int.fitsInSigned(bits: Int): Boolean {
                val rest = this ushr bits
                return rest == 0 || rest == (32 - bits).mask()
            }

            operator fun invoke(ins: Int, kind: WasmSType, param: Int, extra: Int): WasmIntInstruction {
                check(ins.fitsIn(6)) { "ins: $ins" }
                check(kind.id.fitsIn(3)) { "kind: $kind" }
                check(extra.fitsIn(3)) { "extra: $extra" }
                check(param.fitsInSigned(20)) { "param: $param" }
                val res = 0
                    .insert6(ins, 0)
                    .insert3(kind.id, 6)
                    .insert3(extra, 9)
                    .insert(param, 12, 20)

                return WasmIntInstruction(res)
            }

            const val UNREACHABLE = 0

            const val GET_GLOBAL = 1
            const val SET_GLOBAL = 2
            const val GET_LOCAL = 3
            const val SET_LOCAL = 4
            const val TEE_LOCAL = 5
            const val GET_MEM = 6
            const val SET_MEM = 7

            const val JUMP = 8
            const val JUMP_IF = 9
            const val JUMP_IF_NOT = 10
            const val SELECT = 11
            const val DROP = 12
            const val CALL = 13

            const val PUSH_CONST = 14
            const val PUSH_CONST_SHORT = 15

            const val RETURN = 16
            const val MEM_OP = 17

            const val PUSH_CONST_NULL = 18

            const val JUMP_TABLE = 19

            const val BINOP_ADD = 30
            const val BINOP_SUB = 31
            const val BINOP_MUL = 32
            const val BINOP_DIV_S = 33
            const val BINOP_DIV_U = 34
            const val BINOP_REM_S = 35
            const val BINOP_REM_U = 36
            const val BINOP_SHL = 37
            const val BINOP_SHR_S = 38
            const val BINOP_SHR_U = 39
            const val BINOP_AND = 40
            const val BINOP_OR = 41
            const val BINOP_XOR = 42
            const val BINOP_ROTL = 43
            const val BINOP_ROTR = 44
            const val BINOP_MAX = 45
            const val BINOP_MIN = 46
            const val UNOP_CTZ = 47
            const val UNOP_CLZ = 48
            const val UNOP_ABS = 49
            const val UNOP_NEG = 50
            const val UNOP_POPCNT = 51
            const val BINOP_COMP = 52
            const val BINOP_COMP_ZERO = 53
            const val UNOP_CAST = 54
            const val BINOP_COPYSIGN = 55
            const val UNOP_REINTERPRET = 56

            const val COMP_EQ = 0
            const val COMP_NE = 1
            const val COMP_LT_U = 2
            const val COMP_LE_U = 3
            const val COMP_GT_U = 4
            const val COMP_GE_U = 5
            const val COMP_LT_S = 6
            const val COMP_LE_S = 7
            const val COMP_GT_S = 8
            const val COMP_GE_S = 9

            const val EXTEND_NONE = 0
            const val EXTEND8_S = 1
            const val EXTEND8_U = 2
            const val EXTEND16_S = 3
            const val EXTEND16_U = 4
            const val EXTEND32_S = 5
            const val EXTEND32_U = 6
            const val EXTEND64_S = 7

            const val CAST_I32_S = 8
            const val CAST_I32_U = 9

            const val CAST_I64_S = 10
            const val CAST_I64_U = 11

            const val TRUNC_SAT_I32_S = 12
            const val TRUNC_SAT_I32_U = 13

            const val TRUNC_SAT_F32_S = 14
            const val TRUNC_SAT_F32_U = 15

            const val TRUNC_SAT_F64_S = 16
            const val TRUNC_SAT_F64_U = 17

            const val MEMOP_SIZE = 1
            const val MEMOP_GROW = 2
            const val MEMOP_COPY = 3
            const val MEMOP_FILL = 4

            fun ioName(id: Int): String = when (id) {
                RETURN -> "RETURN"
                GET_GLOBAL -> "GET_GLOBAL"
                SET_GLOBAL -> "SET_GLOBAL"
                GET_LOCAL -> "GET_LOCAL"
                SET_LOCAL -> "SET_LOCAL"
                TEE_LOCAL -> "TEE_LOCAL"
                GET_MEM -> "GET_MEM"
                SET_MEM -> "SET_MEM"
                PUSH_CONST -> "PUSH_CONST"
                PUSH_CONST_SHORT -> "PUSH_CONST_SHORT"
                PUSH_CONST_NULL -> "PUSH_CONST_NULL"
                BINOP_ADD -> "BINOP_ADD"
                BINOP_SUB -> "BINOP_SUB"
                BINOP_MUL -> "BINOP_MUL"
                BINOP_DIV_S -> "BINOP_DIV_S"
                BINOP_DIV_U -> "BINOP_DIV_U"
                BINOP_REM_S -> "BINOP_REM_S"
                BINOP_REM_U -> "BINOP_REM_U"
                BINOP_SHL -> "BINOP_SHL"
                BINOP_SHR_S -> "BINOP_SHR_S"
                BINOP_SHR_U -> "BINOP_SHR_U"
                BINOP_AND -> "BINOP_AND"
                BINOP_OR -> "BINOP_OR"
                BINOP_XOR -> "BINOP_XOR"
                BINOP_ROTL -> "BINOP_ROTL"
                BINOP_ROTR -> "BINOP_ROTR"
                BINOP_MAX -> "BINOP_MAX"
                BINOP_MIN -> "BINOP_MIN"
                BINOP_COPYSIGN -> "BINOP_COPYSIGN"
                UNOP_CTZ -> "UNOP_CTZ"
                UNOP_CLZ -> "UNOP_CLZ"
                UNOP_ABS -> "UNOP_ABS"
                UNOP_NEG -> "UNOP_NEG"
                UNOP_POPCNT -> "UNOP_POPCNT"
                BINOP_COMP -> "BINOP_COMP"
                BINOP_COMP_ZERO -> "BINOP_COMP_ZERO"
                UNOP_REINTERPRET -> "UNOP_REINTERPRET"
                JUMP -> "JUMP"
                JUMP_IF -> "JUMP_IF"
                JUMP_IF_NOT -> "JUMP_IF_NOT"
                SELECT -> "SELECT"
                DROP -> "DROP"
                CALL -> "CALL"
                UNREACHABLE -> "UNREACHABLE"
                MEM_OP -> "MEM_OP"
                UNOP_CAST -> "UNOP_CAST"
                else -> "UNKNOWN$id"
            }

            fun compName(id: Int): String = when (id) {
                COMP_EQ -> "=="
                COMP_NE -> "!="
                COMP_LT_U -> "<u"
                COMP_LE_U -> "<=u"
                COMP_GT_U -> ">u"
                COMP_GE_U -> ">=u"
                COMP_LT_S -> "<"
                COMP_LE_S -> "<="
                COMP_GT_S -> ">"
                COMP_GE_S -> ">="
                else -> "?$id"
            }

            fun compFromStr(str: String): Int = when (str) {
                "==" -> COMP_EQ
                "!=" -> COMP_NE
                "<", "<s" -> COMP_LT_S
                "<=", "<=s" -> COMP_LE_S
                ">", ">s" -> COMP_GT_S
                ">=", ">=s" -> COMP_GE_S
                "<u" -> COMP_LT_U
                "<=u" -> COMP_LE_U
                ">u" -> COMP_GT_U
                ">=u" -> COMP_GE_U
                else -> TODO("$str")
            }
        }
    }
}
