package korlibs.wasm

import korlibs.datastructure.*
import korlibs.encoding.*
import korlibs.memory.*

// @TODO: Change stack-based to register based operations:
// example:
//   MUL(A, B, C) :: A = B * C
// instead of:
//   PUSH(B)
//   PUSH(C)
//   MUL()

class WasmRunInterpreter(val module: WasmModule, memPages: Int = 10, maxMemPages: Int = 0x10000) : WasmRuntime(module, memPages, maxMemPages) {
    val globalsI = IntArray(module.globals.size)
    val globalsF = FloatArray(module.globals.size)
    val globalsD = DoubleArray(module.globals.size)
    val globalsL = LongArray(module.globals.size)

    //val globals = Buffer.allocDirect(module.globals.maxOfOrNull { it.globalOffset + 16 } ?: 0)
    //val stack = Buffer.allocDirect(16 * 1024)
    val MAX_STACK = 2048
    //val MAX_STACK = 128 * 1024
    val stackI = IntArray(MAX_STACK)
    val stackF = FloatArray(MAX_STACK)
    val stackD = DoubleArray(MAX_STACK)
    val stackL = LongArray(MAX_STACK)

    var localsPos = 0
    var stackPos = 0

    class WasmFuncCallInt(val interpreter: WasmRunInterpreter, val func: WasmFunc) : WasmFuncCall {
        override fun invoke(runtime: WasmRuntime, args: Array<Any?>): Any? {
            return interpreter.callFunc(func)
            //TODO("Not yet implemented ${args.toList()}, func=$func")
        }
    }

    fun initGlobals(): WasmRunInterpreter {
        //println("GLOBALS: ${globals.sizeInBytes}")
        for (element in module.elements) {
            val e = element.expr ?: continue
            eval(e, WasmType.Function(listOf(), listOf(WasmSType.I32)), WasmDebugContext("element", element.tableIdx))
            val index = popI32()
            element.getFunctions(module).forEachIndexed { idx, wasmFunc ->
                tables[element.tableIdx].elements[index + idx] = WasmFuncCallInt(this, wasmFunc)
            }
        }
        for (data in module.datas) {
            val e = data.e ?: continue
            eval(e, WasmType.Function(listOf(), listOf(WasmSType.I32)), WasmDebugContext("data", data.index))
            val index = popI32()
            //println("DATA[index=$index] = bytes:${data.data.size}, stack=$stackPos")
            memory.setArrayInt8(index, data.data)
        }
        for (global in module.globals) {
            if (global.expr != null) {
                eval(global.expr, WasmType.Function(listOf(), listOf(global.globalType)), WasmDebugContext("global", global.index))
                //println("; stack=$stackPos, global.globalType=${global.globalType}")
                val value = popType(global.globalType.toWasmSType())
                setGlobal(global, value)
                //println("SET GLOBAL[${global.globalOffset}] = $value ; stack=$stackPos")
            }
        }
        if (module.startFunc >= 0) {
            //println("INVOKE INIT: ${module.startFunc}")
            invoke(module.functions[module.startFunc])
        }
        return this
    }

    fun runAsserts() {
        var failed = 0
        var total = 0
        for (assert in module.asserts) {
            when (assert) {
                is WasmAssertReturn -> {
                    val msg = assert.msg
                    stackPos = 0
                    val typeActual = eval(assert.actual, WasmType.Function(listOf(), listOf(WasmSType.I32)), WasmDebugContext("assertActual", total))
                    val typeExpect = eval(assert.expect, WasmType.Function(listOf(), listOf(WasmSType.I32)), WasmDebugContext("assertExpect", total))
                    total++
                    check(typeActual == typeExpect)
                    //println("stackPos=$stackPos, assert.actual[$typeActual]=${assert.actual}, assert.expect[$typeExpect]=${assert.expect}")
                    val expect = popType(typeExpect.first().toWasmSType())
                    val actual = popType(typeActual.first().toWasmSType())
                    failed += WasmRuntime.assert_return_any(actual, expect, msg)
                }
            }
        }
        WasmRuntime.assert_summary(failed, total)
    }

    fun eval(expr: WasmExpr?, type: WasmType.Function, debug: WasmDebugContext): List<WasmType> {
        if (expr == null) return emptyList()
        val code = compile(WasmFunc.anonymousFunc(type, expr), implicitReturn = false, debug = debug)
        //println(code.instructions.toList().map { it and 0xFFF })
        this.evalInstructions(code, 0)
        return code.endStack
    }

    fun WasmFunc.getInterprerCode(): WasmInterpreterCode? {
        if (code == null) return null
        if (code?.interpreterCode == null) {
            code?.interpreterCode = compile(this, implicitReturn = true, debug = WasmDebugContext(this.name, this.index))
        }
        return code?.interpreterCode
    }

    fun invoke(func: WasmFunc) {
        val code = func.getInterprerCode()
        if (code != null) {
            // Allocate space for locals
            localsPos += 0
            //stackPos += code.localSize
            stackPos += code.localsCount
            this.evalInstructions(code, 0)
            //println("code.interpreterCode=${fcode.interpreterCode}")
        } else {
            val efunc = functions[func.name]
            TODO("efunc=$efunc")
        }
    }

    inline fun binopI32(func: (l: Int, r: Int) -> Int) {
        val r = popI32()
        val l = popI32()
        pushI32(func(l, r))
    }
    inline fun unopI32_bool(func: (it: Int) -> Boolean): Boolean {
        val it = popI32()
        return func(it)
    }
    inline fun unopF32_bool(func: (it: Float) -> Boolean): Boolean {
        val it = popF32()
        return func(it)
    }
    inline fun unopI64_bool(func: (it: Long) -> Boolean): Boolean {
        val it = popI64()
        return func(it)
    }
    inline fun unopF64_bool(func: (it: Double) -> Boolean): Boolean {
        val it = popF64()
        return func(it)
    }
    inline fun binopI32_bool(func: (l: Int, r: Int) -> Boolean): Boolean {
        val r = popI32()
        val l = popI32()
        return func(l, r)
    }
    inline fun binopF32_bool(func: (l: Float, r: Float) -> Boolean): Boolean {
        val r = popF32()
        val l = popF32()
        return func(l, r)
    }
    inline fun binopF64_bool(func: (l: Double, r: Double) -> Boolean): Boolean {
        val r = popF64()
        val l = popF64()
        return func(l, r)
    }
    inline fun binopI64_bool(func: (l: Long, r: Long) -> Boolean): Boolean {
        val r = popI64()
        val l = popI64()
        return func(l, r)
    }
    inline fun binopI64_int(func: (l: Long, r: Long) -> Int) {
        val r = popI64()
        val l = popI64()
        pushI32(func(l, r))
    }
    inline fun binopF32_int(func: (l: Float, r: Float) -> Int) {
        val r = popF32()
        val l = popF32()
        pushI32(func(l, r))
    }
    inline fun binopF64_int(func: (l: Double, r: Double) -> Int) {
        val r = popF64()
        val l = popF64()
        pushI32(func(l, r))
    }
    inline fun unopI32(func: (it: Int) -> Int) = pushI32(func(popI32()))
    inline fun unopI64(func: (it: Long) -> Long) = pushI64(func(popI64()))
    inline fun unopF32(func: (it: Float) -> Float) = pushF32(func(popF32()))
    inline fun unopF64(func: (it: Double) -> Double) = pushF64(func(popF64()))

    inline fun binopF32(func: (l: Float, r: Float) -> Float) {
        val r = popF32()
        val l = popF32()
        pushF32(func(l, r))
    }

    inline fun binopF64(func: (l: Double, r: Double) -> Double) {
        val r = popF64()
        val l = popF64()
        pushF64(func(l, r))
    }

    inline fun binopI64(func: (l: Long, r: Long) -> Long) {
        val r = popI64()
        val l = popI64()
        pushI64(func(l, r))
    }

    fun readTypes(types: List<WasmType>): Array<Any?> {
        return types.reversed().map { popType(it.toWasmSType()) }.reversed().toTypedArray()
    }

    private fun callFunc(func: WasmFunc) {
        val code = func.getInterprerCode()
        if (code != null) {
            //println(" ::: localsPos=$localsPos, stackPos=$stackPos")
            val oldLocalsPos = localsPos

            //localsPos = stackPos - code.paramsSize
            //stackPos = localsPos + code.localSize

            localsPos = stackPos - code.paramsCount
            stackPos = localsPos + code.localsCount

            invoke(func)
            localsPos = oldLocalsPos
            //stackPos = oldLocalsPos
        } else {
            val fimport = func.fimport ?: error("Not body and not an import")
            val ffunc = functions[fimport.moduleName]?.get(fimport.name) ?: error("Can't find function $fimport")
            val args = readTypes(func.type.args.map { it.type })
            val result = ffunc.invoke(this, args)
            pushType(func.type.retType.toWasmSType(), result)
        }
    }

    private fun evalInstructions(code: WasmInterpreterCode, index: Int) {
        var index = index
        val instructions = code.instructions
        var instructionsExecuted = 0
        loop@while (index in instructions.indices) {
            val i = instructions[index]
            val op = i and 0xFFF
            val param = i shr 12
            //instructionsHistoriogram[op]++
            if (trace) println("OP[${code.debug.name}][$index]: ${op.hex}, param=$param : ${WasmFastInstructions.NAME_FROM_OP[op]}, localsPos=$localsPos, stack=${stackPos}")
            instructionsExecuted++
            when (op) {
                WasmFastInstructions.Op_try -> TODO()
                WasmFastInstructions.Op_catch -> TODO()
                WasmFastInstructions.Op_throw -> TODO()
                WasmFastInstructions.Op_rethrow -> TODO()
                WasmFastInstructions.Op_call -> {
                    val func = module.functions[param]
                    callFunc(func)
                }
                WasmFastInstructions.Op_call_indirect -> {
                    val tableIdx = 0 // @TODO: Check pass table index as param
                    val ntype = module.types[param]
                    val funcType = ntype.type as WasmType.Function
                    val funcIndex = popI32()
                    val item = tables[tableIdx].elements[funcIndex] ?: error("null function at table=$tableIdx, funcIndex=$funcIndex, funcType=$funcType")
                    val params = arrayOfNulls<Any?>(funcType.args.size)
                    for (n in 0 until funcType.args.size) {
                        val arg = funcType.args[funcType.args.size - 1 - n]
                        when (arg.stype) {
                            WasmSType.VOID -> TODO()
                            WasmSType.I32 -> params[n] = popI32()
                            WasmSType.I64 -> params[n] = popI64()
                            WasmSType.F32 -> params[n] = popF32()
                            WasmSType.F64 -> params[n] = popF64()
                            WasmSType.V128 -> TODO()
                            WasmSType.ANYREF -> TODO()
                            WasmSType.FUNCREF -> TODO()
                        }
                    }
                    val res = item.invoke(this, params)
                    println("!!INDIRECT")
                    pushType(funcType.retType.toWasmSType(), res)
                }
                WasmFastInstructions.Op_return_call -> TODO() // tail-call
                WasmFastInstructions.Op_return_call_indirect -> TODO() // tail-call
                WasmFastInstructions.Op_call_ref -> TODO()
                WasmFastInstructions.Op_return_call_ref -> TODO()
                WasmFastInstructions.Op_delegate -> TODO()
                WasmFastInstructions.Op_catch_all -> TODO()
                WasmFastInstructions.Op_i32_load -> pushI32(Op_i32_load(popI32(), param, this))
                WasmFastInstructions.Op_i64_load -> pushI64(Op_i64_load(popI32(), param, this))
                WasmFastInstructions.Op_f32_load -> pushF32(Op_f32_load(popI32(), param, this))
                WasmFastInstructions.Op_f64_load -> pushF64(Op_f64_load(popI32(), param, this))
                WasmFastInstructions.Op_i32_load8_s -> pushI32(Op_i32_load8_s(popI32(), param, this))
                WasmFastInstructions.Op_i32_load8_u -> pushI32(Op_i32_load8_u(popI32(), param, this))
                WasmFastInstructions.Op_i32_load16_s -> pushI32(Op_i32_load16_s(popI32(), param, this))
                WasmFastInstructions.Op_i32_load16_u -> pushI32(Op_i32_load16_u(popI32(), param, this))
                WasmFastInstructions.Op_i64_load8_s -> pushI64(Op_i64_load8_s(popI32(), param, this))
                WasmFastInstructions.Op_i64_load8_u -> pushI64(Op_i64_load8_u(popI32(), param, this))
                WasmFastInstructions.Op_i64_load16_s -> pushI64(Op_i64_load16_s(popI32(), param, this))
                WasmFastInstructions.Op_i64_load16_u -> pushI64(Op_i64_load16_u(popI32(), param, this))
                WasmFastInstructions.Op_i64_load32_s -> pushI64(Op_i64_load32_s(popI32(), param, this))
                WasmFastInstructions.Op_i64_load32_u -> pushI64(Op_i64_load32_u(popI32(), param, this))
                WasmFastInstructions.Op_i32_store -> { val value = popI32(); val base = popI32(); Op_i32_store(base, value, param, this) }
                WasmFastInstructions.Op_i64_store -> { val value = popI64(); val base = popI32(); Op_i64_store(base, value, param, this) }
                WasmFastInstructions.Op_f32_store -> { val value = popF32(); val base = popI32(); Op_f32_store(base, value, param, this) }
                WasmFastInstructions.Op_f64_store -> { val value = popF64(); val base = popI32(); Op_f64_store(base, value, param, this) }
                WasmFastInstructions.Op_i32_store8 -> { val value = popI32(); val base = popI32(); Op_i32_store8(base, value, param, this) }
                WasmFastInstructions.Op_i32_store16 -> { val value = popI32(); val base = popI32(); Op_i32_store16(base, value, param, this) }
                WasmFastInstructions.Op_i64_store8 -> { val value = popI64(); val base = popI32(); Op_i64_store8(base, value, param, this) }
                WasmFastInstructions.Op_i64_store16 -> { val value = popI64(); val base = popI32(); Op_i64_store16(base, value, param, this) }
                WasmFastInstructions.Op_i64_store32 -> { val value = popI64(); val base = popI32(); Op_i64_store32(base, value, param, this) }
                WasmFastInstructions.Op_memory_size -> pushI32(Op_memory_size(this))
                WasmFastInstructions.Op_memory_grow -> Op_memory_grow(popI32(), this)
                WasmFastInstructions.Op_i32_const -> pushI32(code.intPool[param])
                WasmFastInstructions.Op_i64_const -> pushI64(code.longPool[param])
                WasmFastInstructions.Op_f32_const -> pushF32(code.floatPool[param])
                WasmFastInstructions.Op_f64_const -> pushF64(code.doublePool[param])
                WasmFastInstructions.Op_i32_eqz -> unopI32 { Op_i32_eqz(it) }
                WasmFastInstructions.Op_i32_eq -> binopI32 { l, r -> Op_i32_eq(l, r) }
                WasmFastInstructions.Op_i32_ne -> binopI32 { l, r -> Op_i32_ne(l, r) }
                WasmFastInstructions.Op_i32_lt_s -> binopI32 { l, r ->
                    Op_i32_lt_s(l, r).also {
                        if (trace) println("l:$l < r:$r == $it")
                    }
                }
                WasmFastInstructions.Op_i32_lt_u -> binopI32 { l, r -> Op_i32_lt_u(l, r) }
                WasmFastInstructions.Op_i32_gt_s -> binopI32 { l, r -> Op_i32_gt_s(l, r) }
                WasmFastInstructions.Op_i32_gt_u -> binopI32 { l, r ->
                    Op_i32_gt_u(l, r).also {
                        if (trace) println("l:$l >u r:$r == $it")
                    }
                }
                WasmFastInstructions.Op_i32_le_s -> binopI32 { l, r -> Op_i32_le_s(l, r) }
                WasmFastInstructions.Op_i32_le_u -> binopI32 { l, r -> Op_i32_le_u(l, r) }
                WasmFastInstructions.Op_i32_ge_s -> binopI32 { l, r -> Op_i32_ge_s(l, r) }
                WasmFastInstructions.Op_i32_ge_u -> binopI32 { l, r -> Op_i32_ge_u(l, r) }
                WasmFastInstructions.Op_i64_eqz -> pushI32(Op_i64_eqz(popI64()))
                WasmFastInstructions.Op_i64_eq -> binopI64_int { l, r -> Op_i64_eq(l, r) }
                WasmFastInstructions.Op_i64_ne -> binopI64_int { l, r -> Op_i64_ne(l, r) }
                WasmFastInstructions.Op_i64_lt_s -> binopI64_int { l, r -> Op_i64_lt_s(l, r) }
                WasmFastInstructions.Op_i64_lt_u -> binopI64_int { l, r -> Op_i64_lt_u(l, r) }
                WasmFastInstructions.Op_i64_gt_s -> binopI64_int { l, r -> Op_i64_gt_s(l, r) }
                WasmFastInstructions.Op_i64_gt_u -> binopI64_int { l, r -> Op_i64_gt_u(l, r) }
                WasmFastInstructions.Op_i64_le_s -> binopI64_int { l, r -> Op_i64_le_s(l, r) }
                WasmFastInstructions.Op_i64_le_u -> binopI64_int { l, r -> Op_i64_le_u(l, r) }
                WasmFastInstructions.Op_i64_ge_s -> binopI64_int { l, r -> Op_i64_ge_s(l, r) }
                WasmFastInstructions.Op_i64_ge_u -> binopI64_int { l, r -> Op_i64_ge_u(l, r) }
                WasmFastInstructions.Op_f32_eq -> binopF32_int { l, r -> Op_f32_eq(l, r) }
                WasmFastInstructions.Op_f32_ne -> binopF32_int { l, r -> Op_f32_ne(l, r) }
                WasmFastInstructions.Op_f32_lt -> binopF32_int { l, r -> Op_f32_lt(l, r) }
                WasmFastInstructions.Op_f32_gt -> binopF32_int { l, r -> Op_f32_gt(l, r) }
                WasmFastInstructions.Op_f32_le -> binopF32_int { l, r -> Op_f32_le(l, r) }
                WasmFastInstructions.Op_f32_ge -> binopF32_int { l, r -> Op_f32_ge(l, r) }
                WasmFastInstructions.Op_f64_eq -> binopF64_int { l, r -> Op_f64_eq(l, r) }
                WasmFastInstructions.Op_f64_ne -> binopF64_int { l, r -> Op_f64_ne(l, r) }
                WasmFastInstructions.Op_f64_lt -> binopF64_int { l, r -> Op_f64_lt(l, r) }
                WasmFastInstructions.Op_f64_gt -> binopF64_int { l, r -> Op_f64_gt(l, r) }
                WasmFastInstructions.Op_f64_le -> binopF64_int { l, r -> Op_f64_le(l, r) }
                WasmFastInstructions.Op_f64_ge -> binopF64_int { l, r -> Op_f64_ge(l, r) }
                WasmFastInstructions.Op_i32_clz -> unopI32 { Op_i32_clz(it) }
                WasmFastInstructions.Op_i32_ctz -> unopI32 { Op_i32_ctz(it) }
                WasmFastInstructions.Op_i32_popcnt -> unopI32 { Op_i32_popcnt(it) }
                WasmFastInstructions.Op_i32_add -> binopI32 { l, r ->
                    //println("add: $l + $r :: ${l + r}")
                    l + r
                }
                WasmFastInstructions.Op_i32_sub -> binopI32 { l, r -> l - r }
                WasmFastInstructions.Op_i32_mul -> binopI32 { l, r -> l * r }
                WasmFastInstructions.Op_i32_div_s -> binopI32 { l, r -> l / r }
                WasmFastInstructions.Op_i32_div_u -> binopI32 { l, r -> Op_i32_div_u(l, r) }
                WasmFastInstructions.Op_i32_rem_s -> binopI32 { l, r -> l % r }
                WasmFastInstructions.Op_i32_rem_u -> binopI32 { l, r -> Op_i32_rem_u(l, r) }
                WasmFastInstructions.Op_i32_and -> binopI32 { l, r -> l and r }
                WasmFastInstructions.Op_i32_or -> binopI32 { l, r -> l or r }
                WasmFastInstructions.Op_i32_xor -> binopI32 { l, r -> l xor r }
                WasmFastInstructions.Op_i32_shl -> binopI32 { l, r -> l shl r }
                WasmFastInstructions.Op_i32_shr_s -> binopI32 { l, r -> l shr r }
                WasmFastInstructions.Op_i32_shr_u -> binopI32 { l, r -> l ushr r }
                WasmFastInstructions.Op_i32_rotl -> binopI32 { l, r -> Op_i32_rotl(l, r) }
                WasmFastInstructions.Op_i32_rotr -> binopI32 { l, r -> Op_i32_rotr(l, r) }
                WasmFastInstructions.Op_i64_clz -> unopI64 { Op_i64_clz(it) }
                WasmFastInstructions.Op_i64_ctz -> unopI64 { Op_i64_ctz(it) }
                WasmFastInstructions.Op_i64_popcnt -> unopI64 { Op_i64_popcnt(it) }
                WasmFastInstructions.Op_i64_add -> binopI64 { l, r -> l + r }
                WasmFastInstructions.Op_i64_sub -> binopI64 { l, r -> (l - r) }
                WasmFastInstructions.Op_i64_mul -> binopI64 { l, r -> (l * r) }
                WasmFastInstructions.Op_i64_div_s -> binopI64 { l, r -> (l / r) }
                WasmFastInstructions.Op_i64_div_u -> binopI64 { l, r -> Op_i64_div_u(l, r) }
                WasmFastInstructions.Op_i64_rem_s -> binopI64 { l, r -> (l % r) }
                WasmFastInstructions.Op_i64_rem_u -> binopI64 { l, r -> Op_i64_rem_u(l, r) }
                WasmFastInstructions.Op_i64_and -> binopI64 { l, r -> (l and r) }
                WasmFastInstructions.Op_i64_or -> binopI64 { l, r -> (l or r) }
                WasmFastInstructions.Op_i64_xor -> binopI64 { l, r -> (l xor r) }
                WasmFastInstructions.Op_i64_shl -> binopI64 { l, r -> (l shl r.toInt()) }
                WasmFastInstructions.Op_i64_shr_s -> binopI64 { l, r -> (l shr r.toInt()) }
                WasmFastInstructions.Op_i64_shr_u -> binopI64 { l, r -> (l ushr r.toInt()) }
                WasmFastInstructions.Op_i64_rotl -> binopI64 { l, r -> Op_i64_rotl(l, r) }
                WasmFastInstructions.Op_i64_rotr -> binopI64 { l, r -> Op_i64_rotr(l, r) }
                WasmFastInstructions.Op_f32_abs -> unopF32 { Op_f32_abs(it) }
                WasmFastInstructions.Op_f32_neg -> unopF32 { Op_f32_neg(it) }
                WasmFastInstructions.Op_f32_ceil -> unopF32 { Op_f32_ceil(it) }
                WasmFastInstructions.Op_f32_floor -> unopF32 { Op_f32_floor(it) }
                WasmFastInstructions.Op_f32_trunc -> unopF32 { Op_f32_trunc(it) }
                WasmFastInstructions.Op_f32_nearest -> unopF32 { Op_f32_nearest(it) }
                WasmFastInstructions.Op_f32_sqrt -> unopF32 { Op_f32_sqrt(it) }
                WasmFastInstructions.Op_f32_add -> binopF32 { l, r -> l + r }
                WasmFastInstructions.Op_f32_sub -> binopF32 { l, r -> l - r }
                WasmFastInstructions.Op_f32_mul -> binopF32 { l, r -> l * r }
                WasmFastInstructions.Op_f32_div -> binopF32 { l, r -> l / r }
                WasmFastInstructions.Op_f32_min -> binopF32 { l, r -> Op_f32_min(l, r) }
                WasmFastInstructions.Op_f32_max -> binopF32 { l, r -> Op_f32_max(l, r) }
                WasmFastInstructions.Op_f32_copysign -> binopF32 { l, r -> Op_f32_copysign(l, r) }
                WasmFastInstructions.Op_f64_abs -> unopF64 { Op_f64_abs(it) }
                WasmFastInstructions.Op_f64_neg -> unopF64 { Op_f64_neg(it) }
                WasmFastInstructions.Op_f64_ceil -> unopF64 { Op_f64_ceil(it) }
                WasmFastInstructions.Op_f64_floor -> unopF64 { Op_f64_floor(it) }
                WasmFastInstructions.Op_f64_trunc -> unopF64 { Op_f64_trunc(it) }
                WasmFastInstructions.Op_f64_nearest -> unopF64 { Op_f64_nearest(it) }
                WasmFastInstructions.Op_f64_sqrt -> unopF64 { Op_f64_sqrt(it) }
                WasmFastInstructions.Op_f64_add -> binopF64 { l, r -> l + r }
                WasmFastInstructions.Op_f64_sub -> binopF64 { l, r -> l - r }
                WasmFastInstructions.Op_f64_mul -> binopF64 { l, r -> l * r }
                WasmFastInstructions.Op_f64_div -> binopF64 { l, r -> l / r }
                WasmFastInstructions.Op_f64_min -> binopF64 { l, r -> Op_f64_min(l, r) }
                WasmFastInstructions.Op_f64_max -> binopF64 { l, r -> Op_f64_max(l, r) }
                WasmFastInstructions.Op_f64_copysign -> binopF64 { l, r -> Op_f64_copysign(l, r) }
                WasmFastInstructions.Op_i32_wrap_i64 -> pushI32(Op_i32_wrap_i64(popI64()))
                WasmFastInstructions.Op_i32_trunc_f32_s -> pushI32(Op_i32_trunc_s_f32(popF32()))
                WasmFastInstructions.Op_i32_trunc_f32_u -> pushI32(Op_i32_trunc_u_f32(popF32()))
                WasmFastInstructions.Op_i32_trunc_f64_s -> pushI32(Op_i32_trunc_s_f64(popF64()))
                WasmFastInstructions.Op_i32_trunc_f64_u -> pushI32(Op_i32_trunc_u_f64(popF64()))
                WasmFastInstructions.Op_i64_extend_i32_s -> pushI64(Op_i64_extend_i32_s(popI32()))
                WasmFastInstructions.Op_i64_extend_i32_u -> pushI64(Op_i64_extend_i32_u(popI32()))
                WasmFastInstructions.Op_i64_trunc_f32_s -> pushI64(Op_i64_trunc_s_f32(popF32()))
                WasmFastInstructions.Op_i64_trunc_f32_u -> pushI64(Op_i64_trunc_u_f32(popF32()))
                WasmFastInstructions.Op_i64_trunc_f64_s -> pushI64(Op_i64_trunc_s_f64(popF64()))
                WasmFastInstructions.Op_i64_trunc_f64_u -> pushI64(Op_i64_trunc_u_f64(popF64()))
                WasmFastInstructions.Op_f32_convert_i32_s -> pushF32(Op_f32_convert_s_i32(popI32()))
                WasmFastInstructions.Op_f32_convert_i32_u -> pushF32(Op_f32_convert_u_i32(popI32()))
                WasmFastInstructions.Op_f32_convert_i64_s -> pushF32(Op_f32_convert_s_i64(popI64()))
                WasmFastInstructions.Op_f32_convert_i64_u -> pushF32(Op_f32_convert_u_i64(popI64()))
                WasmFastInstructions.Op_f32_demote_f64 -> pushF32(Op_f32_demote_f64(popF64()))
                WasmFastInstructions.Op_f64_convert_i32_s -> pushF64(Op_f64_convert_s_i32(popI32()))
                WasmFastInstructions.Op_f64_convert_i32_u -> pushF64(Op_f64_convert_u_i32(popI32()))
                WasmFastInstructions.Op_f64_convert_i64_s -> pushF64(Op_f64_convert_s_i64(popI64()))
                WasmFastInstructions.Op_f64_convert_i64_u -> pushF64(Op_f64_convert_u_i64(popI64()))
                WasmFastInstructions.Op_f64_promote_f32 -> pushF64(Op_f64_promote_f32(popF32()))
                WasmFastInstructions.Op_i32_reinterpret_f32 -> pushI32(Op_i32_reinterpret_f32(popF32()))
                WasmFastInstructions.Op_i64_reinterpret_f64 -> pushI64(Op_i64_reinterpret_f64(popF64()))
                WasmFastInstructions.Op_f32_reinterpret_i32 -> pushF32(Op_f32_reinterpret_i32(popI32()))
                WasmFastInstructions.Op_f64_reinterpret_i64 -> pushF64(Op_f64_reinterpret_i64(popI64()))
                WasmFastInstructions.Op_i32_extend8_s -> pushI32(Op_i32_extend8_s(popI32()))
                WasmFastInstructions.Op_i32_extend16_s -> pushI32(Op_i32_extend16_s(popI32()))
                WasmFastInstructions.Op_i64_extend8_s -> pushI64(Op_i64_extend8_s(popI64()))
                WasmFastInstructions.Op_i64_extend16_s -> pushI64(Op_i64_extend16_s(popI64()))
                WasmFastInstructions.Op_i64_extend32_s -> pushI64(Op_i64_extend32_s(popI64()))
                WasmFastInstructions.Op_ref_null -> TODO()
                WasmFastInstructions.Op_ref_is_null -> TODO()

                WasmFastInstructions.Op_i32_drop -> stackPos--
                WasmFastInstructions.Op_i64_drop -> stackPos--
                WasmFastInstructions.Op_f32_drop -> stackPos--
                WasmFastInstructions.Op_f64_drop -> stackPos--
                WasmFastInstructions.Op_v128_drop -> stackPos--

                //WasmFastInstructions.Op_i32_drop -> popIndex(4)
                //WasmFastInstructions.Op_i64_drop -> popIndex(8)
                //WasmFastInstructions.Op_f32_drop -> popIndex(4)
                //WasmFastInstructions.Op_f64_drop -> popIndex(8)
                //WasmFastInstructions.Op_v128_drop -> popIndex(16)

                WasmFastInstructions.Op_i32_local_get -> {
                    //println("LOCAL[$param] == ${getLocalI32(param)} :: localsPos=$localsPos")
                    pushI32(getLocalI32(param))
                }
                WasmFastInstructions.Op_i64_local_get -> pushI64(getLocalI64(param))
                WasmFastInstructions.Op_f32_local_get -> pushF32(getLocalF32(param))
                WasmFastInstructions.Op_f64_local_get -> pushF64(getLocalF64(param))
                WasmFastInstructions.Op_v128_local_get -> TODO()
                WasmFastInstructions.Op_i32_local_set -> setLocalI32(param, popI32())
                WasmFastInstructions.Op_i64_local_set -> setLocalI64(param, popI64())
                WasmFastInstructions.Op_f32_local_set -> setLocalF32(param, popF32())
                WasmFastInstructions.Op_f64_local_set -> setLocalF64(param, popF64())
                WasmFastInstructions.Op_v128_local_set -> TODO()
                WasmFastInstructions.Op_i32_local_tee -> pushI32(setLocalI32(param, popI32()))
                WasmFastInstructions.Op_i64_local_tee -> pushI64(setLocalI64(param, popI64()))
                WasmFastInstructions.Op_f32_local_tee -> pushF32(setLocalF32(param, popF32()))
                WasmFastInstructions.Op_f64_local_tee -> pushF64(setLocalF64(param, popF64()))
                WasmFastInstructions.Op_v128_local_tee -> TODO()
                WasmFastInstructions.Op_i32_global_get -> pushI32(globalsI[param])
                WasmFastInstructions.Op_i64_global_get -> pushI64(globalsL[param])
                WasmFastInstructions.Op_f32_global_get -> pushF32(globalsF[param])
                WasmFastInstructions.Op_f64_global_get -> pushF64(globalsD[param])
                WasmFastInstructions.Op_v128_global_get -> TODO()
                WasmFastInstructions.Op_i32_global_set -> globalsI[param] = popI32()
                WasmFastInstructions.Op_i64_global_set -> globalsL[param] = popI64()
                WasmFastInstructions.Op_f32_global_set -> globalsF[param] = popF32()
                WasmFastInstructions.Op_f64_global_set -> globalsD[param] = popF64()

                //WasmFastInstructions.Op_i32_global_get -> pushI32(globals.getUnalignedInt32(param))
                //WasmFastInstructions.Op_i64_global_get -> pushI64(globals.getUnalignedInt64(param))
                //WasmFastInstructions.Op_f32_global_get -> pushF32(globals.getUnalignedFloat32(param))
                //WasmFastInstructions.Op_f64_global_get -> pushF64(globals.getUnalignedFloat64(param))
                //WasmFastInstructions.Op_v128_global_get -> TODO()
                //WasmFastInstructions.Op_i32_global_set -> globals.setUnalignedInt32(param, popI32())
                //WasmFastInstructions.Op_i64_global_set -> globals.setUnalignedInt64(param, popI64())
                //WasmFastInstructions.Op_f32_global_set -> globals.setUnalignedFloat32(param, popF32())
                //WasmFastInstructions.Op_f64_global_set -> globals.setUnalignedFloat64(param, popF64())
                WasmFastInstructions.Op_v128_global_set -> TODO()
                WasmFastInstructions.Op_i32_return -> { val v = popI32(); stackPos = localsPos; pushI32(v); break@loop }
                WasmFastInstructions.Op_i64_return -> { val v = popI64(); stackPos = localsPos; pushI64(v); break@loop }
                WasmFastInstructions.Op_f32_return -> { val v = popF32(); stackPos = localsPos; pushF32(v); break@loop }
                WasmFastInstructions.Op_f64_return -> { val v = popF64(); stackPos = localsPos; pushF64(v); break@loop }
                WasmFastInstructions.Op_v128_return -> TODO()
                WasmFastInstructions.Op_void_return -> { stackPos = localsPos; break@loop }

                WasmFastInstructions.Op_i32_select -> { val v = popI32(); binopI32 { l, r -> if (v != 0) l else r } }
                WasmFastInstructions.Op_i64_select -> { val v = popI32(); binopI64 { l, r -> if (v != 0) l else r } }
                WasmFastInstructions.Op_f32_select -> { val v = popI32(); binopF32 { l, r -> if (v != 0) l else r } }
                WasmFastInstructions.Op_f64_select -> { val v = popI32(); binopF64 { l, r -> if (v != 0) l else r } }
                WasmFastInstructions.Op_v128_select -> TODO()

                WasmFastInstructions.Op_i32_short_const -> pushI32(param)
                WasmFastInstructions.Op_i64_short_const -> pushI64(param.toLong())
                WasmFastInstructions.Op_f32_short_const -> pushF32(param.toFloat())
                WasmFastInstructions.Op_f64_short_const -> pushF64(param.toDouble())
                WasmFastInstructions.Op_goto -> {
                    index = param
                    continue
                }
                WasmFastInstructions.Op_goto_if -> {
                    val res = popI32()
                    if (res != 0) {
                        index = param
                        continue
                    }
                }
                WasmFastInstructions.Op_goto_if_not -> {
                    val res = popI32()
                    //println("goto_if_not: res=$res")
                    if (res == 0) {
                        index = param
                        continue
                    }
                }
                WasmFastInstructions.Op_goto_if_not_i32_eqz  -> if (!unopI32_bool { it == 0 }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_i32_eq   -> if (!binopI32_bool { l, r -> l == r }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_i32_ne   -> if (!binopI32_bool { l, r -> l != r }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_i32_lt_s -> if (!binopI32_bool { l, r -> l < r }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_i32_lt_u -> if (!binopI32_bool { l, r -> l.toUInt() < r.toUInt() }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_i32_gt_s -> if (!binopI32_bool { l, r -> l > r }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_i32_gt_u -> if (!binopI32_bool { l, r -> l.toUInt() > r.toUInt() }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_i32_le_s -> if (!binopI32_bool { l, r -> l <= r }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_i32_le_u -> if (!binopI32_bool { l, r -> l.toUInt() <= r.toUInt() }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_i32_ge_s -> if (!binopI32_bool { l, r -> l >= r }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_i32_ge_u -> if (!binopI32_bool { l, r -> l.toUInt() >= r.toUInt() }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_i64_eqz -> if (!unopI64_bool { it == 0L }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_i64_eq -> if (!binopI64_bool { l, r -> l == r }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_i64_ne -> if (!binopI64_bool { l, r -> l != r }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_i64_lt_s -> if (!binopI64_bool { l, r -> l < r }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_i64_lt_u -> if (!binopI64_bool { l, r -> l.toULong() < r.toULong() }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_i64_gt_s -> if (!binopI64_bool { l, r -> l > r }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_i64_gt_u -> if (!binopI64_bool { l, r -> l.toULong() > r.toULong() }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_i64_le_s -> if (!binopI64_bool { l, r -> l <= r }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_i64_le_u -> if (!binopI64_bool { l, r -> l.toULong() <= r.toULong() }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_i64_ge_s -> if (!binopI64_bool { l, r -> l >= r }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_i64_ge_u -> if (!binopI64_bool { l, r -> l.toULong() >= r.toULong() }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_f32_eq -> if (!binopF32_bool { l, r -> l == r }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_f32_ne -> if (!binopF32_bool { l, r -> l != r }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_f32_lt -> if (!binopF32_bool { l, r -> l < r }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_f32_gt -> if (!binopF32_bool { l, r -> l > r }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_f32_le -> if (!binopF32_bool { l, r -> l <= r }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_f32_ge -> if (!binopF32_bool { l, r -> l >= r }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_f64_eq -> if (!binopF64_bool { l, r -> l == r }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_f64_ne -> if (!binopF64_bool { l, r -> l != r }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_f64_lt -> if (!binopF64_bool { l, r -> l < r }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_f64_gt -> if (!binopF64_bool { l, r -> l > r }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_f64_le -> if (!binopF64_bool { l, r -> l <= r }) { index = param; continue }
                WasmFastInstructions.Op_goto_if_not_f64_ge -> if (!binopF64_bool { l, r -> l >= r }) { index = param; continue }

                WasmFastInstructions.Op_goto_table -> {
                    val value = popI32()
                    val nlabels = code.intPool[param]
                    if (value in 0 until nlabels) {
                        index = code.intPool[param + 2 + value]
                    } else {
                        index = code.intPool[param + 1]
                    }
                    continue
                }
                WasmFastInstructions.Op_i32_trunc_sat_f32_s -> pushI32(Op_i32_trunc_sat_f32_s(popF32()))
                WasmFastInstructions.Op_i32_trunc_sat_f32_u -> pushI32(Op_i32_trunc_sat_f32_u(popF32()))
                WasmFastInstructions.Op_i32_trunc_sat_f64_s -> pushI32(Op_i32_trunc_sat_f64_s(popF64()))
                WasmFastInstructions.Op_i32_trunc_sat_f64_u -> pushI32(Op_i32_trunc_sat_f64_u(popF64()))
                WasmFastInstructions.Op_i64_trunc_sat_f32_s -> pushI64(Op_i64_trunc_sat_f32_s(popF32()))
                WasmFastInstructions.Op_i64_trunc_sat_f32_u -> pushI64(Op_i64_trunc_sat_f32_u(popF32()))
                WasmFastInstructions.Op_i64_trunc_sat_f64_s -> pushI64(Op_i64_trunc_sat_f64_s(popF64()))
                WasmFastInstructions.Op_i64_trunc_sat_f64_u -> pushI64(Op_i64_trunc_sat_f64_u(popF64()))
                WasmFastInstructions.Op_memory_init -> TODO()
                WasmFastInstructions.Op_data_drop -> TODO()
                WasmFastInstructions.Op_memory_copy -> TODO()
                WasmFastInstructions.Op_memory_fill -> TODO()
                WasmFastInstructions.Op_table_init -> TODO()
                WasmFastInstructions.Op_elem_drop -> TODO()
                WasmFastInstructions.Op_table_copy -> TODO()
                WasmFastInstructions.Op_table_grow -> TODO()
                WasmFastInstructions.Op_table_size -> TODO()
                WasmFastInstructions.Op_table_fill -> TODO()
                else -> TODO("op=$op, param=$param, i=$i")
            }
            index++
        }
        this.instructionsExecuted += instructionsExecuted
    }

    fun pushType(type: WasmSType, value: Any?) {
        when (type) {
            WasmSType.VOID -> Unit
            WasmSType.I32 -> pushI32((value as? Int) ?: 0)
            WasmSType.I64 -> pushI64((value as? Long) ?: 0L)
            WasmSType.F32 -> pushF32((value as? Float) ?: 0f)
            WasmSType.F64 -> pushF64((value as? Double) ?: 0.0)
            WasmSType.V128 -> TODO()
            WasmSType.ANYREF -> TODO()
            WasmSType.FUNCREF -> TODO()
        }
    }

    fun pushI32(value: Int) { stackI[stackPos++] = value }
    fun pushF32(value: Float) { stackF[stackPos++] = value }
    fun pushI64(value: Long) { stackL[stackPos++] = value }
    fun pushF64(value: Double) { stackD[stackPos++] = value }
    fun popI32(): Int = stackI[--stackPos]
    fun popF32(): Float = stackF[--stackPos]
    fun popI64(): Long = stackL[--stackPos]
    fun popF64(): Double = stackD[--stackPos]
    fun getLocalI32(offset: Int): Int = stackI[localsPos + offset]
    fun getLocalI64(offset: Int): Long = stackL[localsPos + offset]
    fun getLocalF32(offset: Int): Float = stackF[localsPos + offset]
    fun getLocalF64(offset: Int): Double = stackD[localsPos + offset]
    fun setLocalI32(offset: Int, value: Int): Int = value.also { stackI[localsPos + offset] = value }
    fun setLocalI64(offset: Int, value: Long): Long = value.also { stackL[localsPos + offset] = value }
    fun setLocalF32(offset: Int, value: Float): Float = value.also { stackF[localsPos + offset] = value }
    fun setLocalF64(offset: Int, value: Double): Double = value.also { stackD[localsPos + offset] = value }

    //inline fun pushIndex(size: Int): Int = stackPos.also { stackPos += size }
    //inline fun popIndex(size: Int): Int { stackPos -= size; return stackPos }
    //fun pushI32(value: Int) { stack.setUnalignedInt32(pushIndex(4), value) }
    //fun pushF32(value: Float) { stack.setUnalignedFloat32(pushIndex(4), value) }
    //fun pushI64(value: Long) { stack.setUnalignedInt64(pushIndex(8), value) }
    //fun pushF64(value: Double) { stack.setUnalignedFloat64(pushIndex(8), value) }
    //fun popI32(): Int = stack.getUnalignedInt32(popIndex(4))
    //fun popF32(): Float = stack.getUnalignedFloat32(popIndex(4))
    //fun popI64(): Long = stack.getUnalignedInt64(popIndex(8))
    //fun popF64(): Double = stack.getUnalignedFloat64(popIndex(8))
    //fun getLocalI32(offset: Int): Int = stack.getUnalignedInt32(localsPos + offset)
    //fun getLocalI64(offset: Int): Long = stack.getUnalignedInt64(localsPos + offset)
    //fun getLocalF32(offset: Int): Float = stack.getUnalignedFloat32(localsPos + offset)
    //fun getLocalF64(offset: Int): Double = stack.getUnalignedFloat64(localsPos + offset)
    //fun setLocalI32(offset: Int, value: Int): Int = value.also { stack.setUnalignedInt32(localsPos + offset, value) }
    //fun setLocalI64(offset: Int, value: Long): Long = value.also { stack.setUnalignedInt64(localsPos + offset, value) }
    //fun setLocalF32(offset: Int, value: Float): Float = value.also { stack.setUnalignedFloat32(localsPos + offset, value) }
    //fun setLocalF64(offset: Int, value: Double): Double = value.also { stack.setUnalignedFloat64(localsPos + offset, value) }

    fun setLocal(type: WasmSType, offset: Int, value: Any?) {
        when (type) {
            WasmSType.VOID -> TODO()
            WasmSType.I32 -> setLocalI32(offset, value as Int)
            WasmSType.I64 -> setLocalI64(offset, value as Long)
            WasmSType.F32 -> setLocalF32(offset, value as Float)
            WasmSType.F64 -> setLocalF64(offset, value as Double)
            WasmSType.V128 -> TODO()
            WasmSType.ANYREF -> TODO()
            WasmSType.FUNCREF -> TODO()
        }
    }

    fun setGlobal(global: WasmGlobal, value: Any?) {
        when (global.globalType.toWasmSType()) {
            WasmSType.VOID -> TODO()
            WasmSType.I32 -> globalsI[global.index] = value as Int
            WasmSType.I64 -> globalsL[global.index] = value as Long
            WasmSType.F32 -> globalsF[global.index] = value as Float
            WasmSType.F64 -> globalsD[global.index] = value as Double
            //WasmSType.I32 -> globals.setUnalignedInt32(global.globalOffset, value as Int)
            //WasmSType.I64 -> globals.setUnalignedInt64(global.globalOffset, value as Long)
            //WasmSType.F32 -> globals.setUnalignedFloat32(global.globalOffset, value as Float)
            //WasmSType.F64 -> globals.setUnalignedFloat64(global.globalOffset, value as Double)
            WasmSType.V128 -> TODO()
            WasmSType.ANYREF -> TODO()
            WasmSType.FUNCREF -> TODO()
        }
    }

    fun popType(type: WasmSType): Any? {
        return when (type) {
            WasmSType.VOID -> Unit
            WasmSType.I32 -> popI32()
            WasmSType.I64 -> popI64()
            WasmSType.F32 -> popF32()
            WasmSType.F64 -> popF64()
            WasmSType.V128 -> TODO()
            WasmSType.ANYREF -> TODO()
            WasmSType.FUNCREF -> TODO()
        }
    }

    override fun invoke(funcName: String, vararg params: Any?): Any? {
        //arrayfill(stackI, 0)
        //arrayfill(stackF, 0f)
        //arrayfill(stackL, 0L)
        //arrayfill(stackD, 0.0)
        val func = module.functionsByName[funcName] ?: error("Can't find '$funcName' in ${module.functionsByName.keys}")
        val code = func.getInterprerCode() ?: error("Function '$funcName' doesn't have body")
        for ((index, arg) in func.type.args.withIndex()) {
            //val offset = code.localsOffsets[index]
            val offset = index
            setLocal(arg.stype, offset, params[index])
        }
        //stackPos += code.localSize
        stackPos += code.localsCount
        //println("localsPos=$localsPos, stackPos=$stackPos")
        invoke(func)
        return popType(func.type.retType.toWasmSType())
    }

    override fun invokeIndirect(index: Int, vararg params: Any?): Any? {
        return super.invokeIndirect(index, *params)
    }

    fun compile(func: WasmFunc, implicitReturn: Boolean, debug: WasmDebugContext): WasmInterpreterCode {
        class Patch(val label: WasmCodeVisitor.Label, val intIndex: Int = -1, val instrutionIndex: Int = -1)

        val poolLongs = arrayListOf<Long>()
        val poolInts = intArrayListOf()
        val poolFloats = floatArrayListOf()
        val poolDoubles = doubleArrayListOf()
        val instructions = intArrayListOf()
        val patches = arrayListOf<Patch>()
        var localsOffset = -1
        var context: WasmCodeVisitor.Context = WasmCodeVisitor.Context(func, module)

        fun ins(op: Int, param: Int = 0): Int {
            check(op == (op and 0xFFF))
            check(param == ((param shl 12) shr 12))
            if (op == 2775) {
                TODO()
            }
            return (op and 0xFFF) or (param shl 12)
        }

        func.accept(module, implicitReturn, object : WasmCodeVisitor {
            override fun visit(i: WasmInstruction, context: WasmCodeVisitor.Context) {
                val op = i.op
                val opHI = op.id ushr 8
                val opLO = op.id and 0xFF
                var newOp = when (opHI) {
                    0x00 -> op.id
                    0xFC -> 0x200 + opLO
                    else -> TODO()
                }
                var param = 0

                when (i) {
                    is WasmInstruction.InsConstLong -> {
                        if (i.value in -500_000L..500_000L) {
                            param = i.value.toInt()
                            newOp = WasmFastInstructions.Op_i64_short_const
                        } else {
                            param = poolLongs.size
                            poolLongs += i.value
                        }
                    }
                    is WasmInstruction.InsConstInt -> {
                        if (i.value in -500_000..500_000) {
                            param = i.value
                            newOp = WasmFastInstructions.Op_i32_short_const
                        } else {
                            param = poolInts.size
                            poolInts += i.value
                        }
                    }
                    is WasmInstruction.InsConstFloat -> {
                        val ivalue = i.value.toInt()
                        if (ivalue.toFloat() == i.value && ivalue in -500_000..500_000) {
                            param = ivalue
                            newOp = WasmFastInstructions.Op_f32_short_const
                        } else {
                            param = poolFloats.size
                            poolFloats += i.value
                        }
                    }
                    is WasmInstruction.InsConstDouble -> {
                        val ivalue = i.value.toInt()
                        if (ivalue.toDouble() == i.value && ivalue in -500_000..500_000) {
                            param = ivalue
                            newOp = WasmFastInstructions.Op_f64_short_const
                        } else {
                            param = poolDoubles.size
                            poolDoubles += i.value
                        }
                    }
                    is WasmInstruction.RETURN -> {
                        when (context.retType.toWasmSType()) {
                            WasmSType.VOID -> newOp = WasmFastInstructions.Op_void_return
                            WasmSType.I32 -> newOp = WasmFastInstructions.Op_i32_return
                            WasmSType.I64 -> newOp = WasmFastInstructions.Op_i64_return
                            WasmSType.F32 -> newOp = WasmFastInstructions.Op_f32_return
                            WasmSType.F64 -> newOp = WasmFastInstructions.Op_f64_return
                            WasmSType.V128 -> newOp = WasmFastInstructions.Op_v128_return
                            WasmSType.ANYREF -> TODO()
                            WasmSType.FUNCREF -> TODO()
                            else -> TODO()
                        }
                    }
                    is WasmInstruction.InsMemarg -> {
                        param = i.offset
                    }
                    is WasmInstruction.InsInt -> {
                        when (i.op) {
                            WasmOp.Op_local_get -> {
                                //param = context.localsMemOffset[i.param]
                                param = i.param
                                when (context.lastInstructionOutputType) {
                                    WasmSType.I32 -> newOp = WasmFastInstructions.Op_i32_local_get
                                    WasmSType.I64 -> newOp = WasmFastInstructions.Op_i64_local_get
                                    WasmSType.F32 -> newOp = WasmFastInstructions.Op_f32_local_get
                                    WasmSType.F64 -> newOp = WasmFastInstructions.Op_f64_local_get
                                    WasmSType.V128 -> newOp = WasmFastInstructions.Op_v128_local_get
                                    WasmSType.ANYREF -> TODO("$i")
                                    WasmSType.FUNCREF -> TODO("$i")
                                    WasmSType.VOID -> TODO("$i")
                                }
                            }
                            WasmOp.Op_local_tee -> {
                                //param = context.localsMemOffset[i.param]
                                param = i.param
                                when (context.lastInstructionOutputType) {
                                    WasmSType.I32 -> newOp = WasmFastInstructions.Op_i32_local_tee
                                    WasmSType.I64 -> newOp = WasmFastInstructions.Op_i64_local_tee
                                    WasmSType.F32 -> newOp = WasmFastInstructions.Op_f32_local_tee
                                    WasmSType.F64 -> newOp = WasmFastInstructions.Op_f64_local_tee
                                    WasmSType.V128 -> newOp = WasmFastInstructions.Op_v128_local_tee
                                    WasmSType.ANYREF -> TODO("$i")
                                    WasmSType.FUNCREF -> TODO("$i")
                                    WasmSType.VOID -> TODO("$i")
                                }
                            }
                            WasmOp.Op_local_set -> {
                                //param = context.localsMemOffset[i.param]
                                param = i.param
                                when (context.lastInstructionOutputType) {
                                    WasmSType.I32 -> newOp = WasmFastInstructions.Op_i32_local_set
                                    WasmSType.I64 -> newOp = WasmFastInstructions.Op_i64_local_set
                                    WasmSType.F32 -> newOp = WasmFastInstructions.Op_f32_local_set
                                    WasmSType.F64 -> newOp = WasmFastInstructions.Op_f64_local_set
                                    WasmSType.V128 -> newOp = WasmFastInstructions.Op_v128_local_set
                                    WasmSType.ANYREF -> TODO("$i")
                                    WasmSType.FUNCREF -> TODO("$i")
                                    WasmSType.VOID -> TODO("$i")
                                }
                            }
                            WasmOp.Op_global_get -> {
                                //param = module.globals[i.param].globalOffset
                                param = module.globals[i.param].index
                                when (context.lastInstructionOutputType) {
                                    WasmSType.I32 -> newOp = WasmFastInstructions.Op_i32_global_get
                                    WasmSType.I64 -> newOp = WasmFastInstructions.Op_i64_global_get
                                    WasmSType.F32 -> newOp = WasmFastInstructions.Op_f32_global_get
                                    WasmSType.F64 -> newOp = WasmFastInstructions.Op_f64_global_get
                                    WasmSType.V128 -> newOp = WasmFastInstructions.Op_v128_global_get
                                    WasmSType.ANYREF -> TODO("$i")
                                    WasmSType.FUNCREF -> TODO("$i")
                                    WasmSType.VOID -> TODO("$i")
                                }
                            }
                            WasmOp.Op_global_set -> {
                                //param = module.globals[i.param].globalOffset
                                param = module.globals[i.param].index
                                when (context.lastInstructionType) {
                                    WasmSType.I32 -> newOp = WasmFastInstructions.Op_i32_global_set
                                    WasmSType.I64 -> newOp = WasmFastInstructions.Op_i64_global_set
                                    WasmSType.F32 -> newOp = WasmFastInstructions.Op_f32_global_set
                                    WasmSType.F64 -> newOp = WasmFastInstructions.Op_f64_global_set
                                    WasmSType.V128 -> newOp = WasmFastInstructions.Op_v128_global_set
                                    WasmSType.ANYREF -> TODO("$i")
                                    WasmSType.FUNCREF -> TODO("$i")
                                    WasmSType.VOID -> TODO("$i")
                                }
                            }
                            WasmOp.Op_memory_copy, WasmOp.Op_memory_init, WasmOp.Op_memory_grow, WasmOp.Op_memory_fill, WasmOp.Op_memory_size -> {
                                Unit
                            }
                            else -> TODO("$i")
                        }
                    }
                    is WasmInstruction.Ins -> {
                        when (i.op) {
                            WasmOp.Op_drop -> {
                                when (context.lastInstructionType) {
                                    WasmSType.I32 -> newOp = WasmFastInstructions.Op_i32_drop
                                    WasmSType.I64 -> newOp = WasmFastInstructions.Op_i64_drop
                                    WasmSType.F32 -> newOp = WasmFastInstructions.Op_f32_drop
                                    WasmSType.F64 -> newOp = WasmFastInstructions.Op_f64_drop
                                    WasmSType.V128 -> newOp = WasmFastInstructions.Op_v128_drop
                                    WasmSType.ANYREF -> TODO("$i")
                                    WasmSType.FUNCREF -> TODO("$i")
                                    WasmSType.VOID -> TODO("$i")
                                }
                            }
                            WasmOp.Op_select -> {
                                when (context.lastInstructionOutputType) {
                                    WasmSType.I32 -> newOp = WasmFastInstructions.Op_i32_select
                                    WasmSType.I64 -> newOp = WasmFastInstructions.Op_i64_select
                                    WasmSType.F32 -> newOp = WasmFastInstructions.Op_f32_select
                                    WasmSType.F64 -> newOp = WasmFastInstructions.Op_f64_select
                                    WasmSType.V128 -> newOp = WasmFastInstructions.Op_v128_select
                                    WasmSType.ANYREF -> TODO("$i")
                                    WasmSType.FUNCREF -> TODO("$i")
                                    WasmSType.VOID -> TODO("$i")
                                }
                            }
                            else -> Unit
                        }
                    }
                    is WasmInstruction.CALL -> {
                        param = i.funcIdx
                    }
                    is WasmInstruction.CALL_INDIRECT -> {
                        param = i.typeIdx
                    }
                    is WasmInstruction.unreachable -> Unit
                    else -> TODO("$i")
                }

                instructions += ins(newOp, param)
            }

            override fun visitFuncStart(ctx: WasmCodeVisitor.Context) {
                context = ctx
                for (n in ctx.func.type.args.size until ctx.func.rlocals.size) {
                    val type = ctx.func.rlocals[n].stype
                    when (type) {
                        WasmSType.VOID -> TODO()
                        WasmSType.I32 -> {
                            instructions += ins(WasmFastInstructions.Op_i32_short_const, 0)
                            instructions += ins(WasmFastInstructions.Op_i32_local_set, n)
                        }
                        WasmSType.I64 -> {
                            instructions += ins(WasmFastInstructions.Op_i64_short_const, 0)
                            instructions += ins(WasmFastInstructions.Op_i64_local_set, n)
                        }
                        WasmSType.F32 -> {
                            instructions += ins(WasmFastInstructions.Op_f32_short_const, 0)
                            instructions += ins(WasmFastInstructions.Op_f32_local_set, n)
                        }
                        WasmSType.F64 -> {
                            instructions += ins(WasmFastInstructions.Op_f64_short_const, 0)
                            instructions += ins(WasmFastInstructions.Op_f64_local_set, n)
                        }
                        WasmSType.V128 -> TODO()
                        WasmSType.ANYREF -> TODO()
                        WasmSType.FUNCREF -> TODO()
                    }
                }
            }

            override fun visitFuncEnd(context: WasmCodeVisitor.Context) {
                localsOffset = context.localsOffset
            }

            override fun visitGotoTable(
                labels: List<WasmCodeVisitor.Label>,
                default: WasmCodeVisitor.Label,
                context: WasmCodeVisitor.Context
            ) {
                val tableIndex = poolInts.size
                poolInts.add(labels.size)
                for (label in listOf(default) + labels) {
                    patches += Patch(label, intIndex = poolInts.size)
                    poolInts.add(0)
                }
                instructions += ins(WasmFastInstructions.Op_goto_table, tableIndex)
            }

            override fun visitGoto(label: WasmCodeVisitor.Label, cond: Boolean?, context: WasmCodeVisitor.Context) {
                //println("visitGoto: label=$label, cond=$cond")
                if (cond == false && instructions.isNotEmpty()) {
                    val lastOp = instructions.last() and 0xFFF
                    val replace = when (lastOp) {
                        WasmFastInstructions.Op_i32_eqz -> WasmFastInstructions.Op_goto_if_not_i32_eqz
                        WasmFastInstructions.Op_i32_eq -> WasmFastInstructions.Op_goto_if_not_i32_eq
                        WasmFastInstructions.Op_i32_ne -> WasmFastInstructions.Op_goto_if_not_i32_ne
                        WasmFastInstructions.Op_i32_lt_s -> WasmFastInstructions.Op_goto_if_not_i32_lt_s
                        WasmFastInstructions.Op_i32_lt_u -> WasmFastInstructions.Op_goto_if_not_i32_lt_u
                        WasmFastInstructions.Op_i32_gt_s -> WasmFastInstructions.Op_goto_if_not_i32_gt_s
                        WasmFastInstructions.Op_i32_gt_u -> WasmFastInstructions.Op_goto_if_not_i32_gt_u
                        WasmFastInstructions.Op_i32_le_s -> WasmFastInstructions.Op_goto_if_not_i32_le_s
                        WasmFastInstructions.Op_i32_le_u -> WasmFastInstructions.Op_goto_if_not_i32_le_u
                        WasmFastInstructions.Op_i32_ge_s -> WasmFastInstructions.Op_goto_if_not_i32_ge_s
                        WasmFastInstructions.Op_i32_ge_u -> WasmFastInstructions.Op_goto_if_not_i32_ge_u
                        WasmFastInstructions.Op_i64_eqz -> WasmFastInstructions.Op_goto_if_not_i64_eqz
                        WasmFastInstructions.Op_i64_eq -> WasmFastInstructions.Op_goto_if_not_i64_eq
                        WasmFastInstructions.Op_i64_ne -> WasmFastInstructions.Op_goto_if_not_i64_ne
                        WasmFastInstructions.Op_i64_lt_s -> WasmFastInstructions.Op_goto_if_not_i64_lt_s
                        WasmFastInstructions.Op_i64_lt_u -> WasmFastInstructions.Op_goto_if_not_i64_lt_u
                        WasmFastInstructions.Op_i64_gt_s -> WasmFastInstructions.Op_goto_if_not_i64_gt_s
                        WasmFastInstructions.Op_i64_gt_u -> WasmFastInstructions.Op_goto_if_not_i64_gt_u
                        WasmFastInstructions.Op_i64_le_s -> WasmFastInstructions.Op_goto_if_not_i64_le_s
                        WasmFastInstructions.Op_i64_le_u -> WasmFastInstructions.Op_goto_if_not_i64_le_u
                        WasmFastInstructions.Op_i64_ge_s -> WasmFastInstructions.Op_goto_if_not_i64_ge_s
                        WasmFastInstructions.Op_i64_ge_u -> WasmFastInstructions.Op_goto_if_not_i64_ge_u
                        WasmFastInstructions.Op_f32_eq -> WasmFastInstructions.Op_goto_if_not_f32_eq
                        WasmFastInstructions.Op_f32_ne -> WasmFastInstructions.Op_goto_if_not_f32_ne
                        WasmFastInstructions.Op_f32_lt -> WasmFastInstructions.Op_goto_if_not_f32_lt
                        WasmFastInstructions.Op_f32_gt -> WasmFastInstructions.Op_goto_if_not_f32_gt
                        WasmFastInstructions.Op_f32_le -> WasmFastInstructions.Op_goto_if_not_f32_le
                        WasmFastInstructions.Op_f32_ge -> WasmFastInstructions.Op_goto_if_not_f32_ge
                        WasmFastInstructions.Op_f64_eq -> WasmFastInstructions.Op_goto_if_not_f64_eq
                        WasmFastInstructions.Op_f64_ne -> WasmFastInstructions.Op_goto_if_not_f64_ne
                        WasmFastInstructions.Op_f64_lt -> WasmFastInstructions.Op_goto_if_not_f64_lt
                        WasmFastInstructions.Op_f64_gt -> WasmFastInstructions.Op_goto_if_not_f64_gt
                        WasmFastInstructions.Op_f64_le -> WasmFastInstructions.Op_goto_if_not_f64_le
                        WasmFastInstructions.Op_f64_ge -> WasmFastInstructions.Op_goto_if_not_f64_ge
                        else -> null
                    }
                    if (replace != null) {
                        patches += Patch(label, instrutionIndex = instructions.size - 1)
                        instructions[instructions.size - 1] = replace
                        return
                    }
                }
                patches += Patch(label, instrutionIndex = instructions.size)
                instructions += ins(when (cond) {
                    null -> WasmFastInstructions.Op_goto
                    true -> WasmFastInstructions.Op_goto_if
                    false -> WasmFastInstructions.Op_goto_if_not
                }, 0)
            }

            override fun visitLabel(label: WasmCodeVisitor.Label, context: WasmCodeVisitor.Context) {
                label.target = instructions.size
            }
        })

        for (patch in patches) {
            val intIndex = patch.intIndex
            val instrutionIndex = patch.instrutionIndex
            if (instrutionIndex >= 0) {
                instructions[instrutionIndex] = ins(instructions[instrutionIndex] and 0xFFF, patch.label.target)
            }
            if (intIndex >= 0) {
                poolInts[intIndex] = patch.label.target
            }
        }

        return WasmInterpreterCode(
            instructions.toIntArray(),
            poolInts.toIntArray(),
            poolLongs.toLongArray(),
            poolFloats.toFloatArray(),
            poolDoubles.toDoubleArray(),
            localSize = localsOffset,
            localsOffsets = context.localsMemOffset,
            paramsSize = context.paramsOffset,
            paramsCount = context.func.type.args.size,
            localsCount = context.funcLocals.size,
            endStack = context.stack,
            debug = debug
        )
    }


    // Fast instructions for a switch
    object WasmFastInstructions {
        const val Op_unreachable = 0x00
        const val Op_nop = 0x01
        const val Op_block = 0x02
        const val Op_loop = 0x03
        const val Op_if = 0x04
        const val Op_else = 0x05
        const val Op_try = 0x06
        const val Op_catch = 0x07
        const val Op_throw = 0x08
        const val Op_rethrow = 0x09
        const val Op_reserved_0x0a = 0x0a
        const val Op_end = 0x0b
        const val Op_br = 0x0c
        const val Op_br_if = 0x0d
        const val Op_br_table = 0x0e
        const val Op_return = 0x0f
        const val Op_call = 0x10
        const val Op_call_indirect = 0x11
        const val Op_return_call = 0x12
        const val Op_return_call_indirect = 0x13
        const val Op_call_ref = 0x14
        const val Op_return_call_ref = 0x15
        const val Op_delegate = 0x18
        const val Op_catch_all = 0x19
        const val Op_drop = 0x1a
        const val Op_select = 0x1b
        const val Op_local_get = 0x20
        const val Op_local_set = 0x21
        const val Op_local_tee = 0x22
        const val Op_global_get = 0x23
        const val Op_global_set = 0x24
        const val Op_i32_load = 0x28
        const val Op_i64_load = 0x29
        const val Op_f32_load = 0x2a
        const val Op_f64_load = 0x2b
        const val Op_i32_load8_s = 0x2c
        const val Op_i32_load8_u = 0x2d
        const val Op_i32_load16_s = 0x2e
        const val Op_i32_load16_u = 0x2f
        const val Op_i64_load8_s = 0x30
        const val Op_i64_load8_u = 0x31
        const val Op_i64_load16_s = 0x32
        const val Op_i64_load16_u = 0x33
        const val Op_i64_load32_s = 0x34
        const val Op_i64_load32_u = 0x35
        const val Op_i32_store = 0x36
        const val Op_i64_store = 0x37
        const val Op_f32_store = 0x38
        const val Op_f64_store = 0x39
        const val Op_i32_store8 = 0x3a
        const val Op_i32_store16 = 0x3b
        const val Op_i64_store8 = 0x3c
        const val Op_i64_store16 = 0x3d
        const val Op_i64_store32 = 0x3e
        const val Op_memory_size = 0x3f
        const val Op_memory_grow = 0x40
        const val Op_i32_const = 0x41
        const val Op_i64_const = 0x42
        const val Op_f32_const = 0x43
        const val Op_f64_const = 0x44
        const val Op_i32_eqz = 0x45
        const val Op_i32_eq =  0x46
        const val Op_i32_ne =  0x47
        const val Op_i32_lt_s = 0x48
        const val Op_i32_lt_u = 0x49
        const val Op_i32_gt_s = 0x4a
        const val Op_i32_gt_u = 0x4b
        const val Op_i32_le_s = 0x4c
        const val Op_i32_le_u = 0x4d
        const val Op_i32_ge_s = 0x4e
        const val Op_i32_ge_u = 0x4f
        const val Op_i64_eqz = 0x50
        const val Op_i64_eq = 0x51
        const val Op_i64_ne = 0x52
        const val Op_i64_lt_s = 0x53
        const val Op_i64_lt_u = 0x54
        const val Op_i64_gt_s = 0x55
        const val Op_i64_gt_u = 0x56
        const val Op_i64_le_s = 0x57
        const val Op_i64_le_u = 0x58
        const val Op_i64_ge_s = 0x59
        const val Op_i64_ge_u = 0x5a
        const val Op_f32_eq = 0x5b
        const val Op_f32_ne = 0x5c
        const val Op_f32_lt = 0x5d
        const val Op_f32_gt = 0x5e
        const val Op_f32_le = 0x5f
        const val Op_f32_ge = 0x60
        const val Op_f64_eq = 0x61
        const val Op_f64_ne = 0x62
        const val Op_f64_lt = 0x63
        const val Op_f64_gt = 0x64
        const val Op_f64_le = 0x65
        const val Op_f64_ge = 0x66
        const val Op_i32_clz = 0x67
        const val Op_i32_ctz = 0x68
        const val Op_i32_popcnt = 0x69
        const val Op_i32_add = 0x6a
        const val Op_i32_sub = 0x6b
        const val Op_i32_mul = 0x6c
        const val Op_i32_div_s = 0x6d
        const val Op_i32_div_u = 0x6e
        const val Op_i32_rem_s = 0x6f
        const val Op_i32_rem_u = 0x70
        const val Op_i32_and = 0x71
        const val Op_i32_or = 0x72
        const val Op_i32_xor = 0x73
        const val Op_i32_shl = 0x74
        const val Op_i32_shr_s = 0x75
        const val Op_i32_shr_u = 0x76
        const val Op_i32_rotl = 0x77
        const val Op_i32_rotr = 0x78
        const val Op_i64_clz = 0x79
        const val Op_i64_ctz = 0x7a
        const val Op_i64_popcnt = 0x7b
        const val Op_i64_add = 0x7c
        const val Op_i64_sub = 0x7d
        const val Op_i64_mul = 0x7e
        const val Op_i64_div_s = 0x7f
        const val Op_i64_div_u = 0x80
        const val Op_i64_rem_s = 0x81
        const val Op_i64_rem_u = 0x82
        const val Op_i64_and = 0x83
        const val Op_i64_or = 0x84
        const val Op_i64_xor = 0x85
        const val Op_i64_shl = 0x86
        const val Op_i64_shr_s = 0x87
        const val Op_i64_shr_u = 0x88
        const val Op_i64_rotl = 0x89
        const val Op_i64_rotr = 0x8a
        const val Op_f32_abs = 0x8b
        const val Op_f32_neg = 0x8c
        const val Op_f32_ceil = 0x8d
        const val Op_f32_floor = 0x8e
        const val Op_f32_trunc = 0x8f
        const val Op_f32_nearest = 0x90
        const val Op_f32_sqrt = 0x91
        const val Op_f32_add = 0x92
        const val Op_f32_sub = 0x93
        const val Op_f32_mul = 0x94
        const val Op_f32_div = 0x95
        const val Op_f32_min = 0x96
        const val Op_f32_max = 0x97
        const val Op_f32_copysign = 0x98
        const val Op_f64_abs = 0x99
        const val Op_f64_neg = 0x9a
        const val Op_f64_ceil = 0x9b
        const val Op_f64_floor = 0x9c
        const val Op_f64_trunc = 0x9d
        const val Op_f64_nearest = 0x9e
        const val Op_f64_sqrt = 0x9f
        const val Op_f64_add = 0xa0
        const val Op_f64_sub = 0xa1
        const val Op_f64_mul = 0xa2
        const val Op_f64_div = 0xa3
        const val Op_f64_min = 0xa4
        const val Op_f64_max = 0xa5
        const val Op_f64_copysign = 0xa6
        const val Op_i32_wrap_i64 = 0xa7
        const val Op_i32_trunc_f32_s = 0xa8
        const val Op_i32_trunc_f32_u = 0xa9
        const val Op_i32_trunc_f64_s = 0xaa
        const val Op_i32_trunc_f64_u = 0xab
        const val Op_i64_extend_i32_s = 0xac
        const val Op_i64_extend_i32_u = 0xad
        const val Op_i64_trunc_f32_s = 0xae
        const val Op_i64_trunc_f32_u = 0xaf
        const val Op_i64_trunc_f64_s = 0xb0
        const val Op_i64_trunc_f64_u = 0xb1
        const val Op_f32_convert_i32_s = 0xb2
        const val Op_f32_convert_i32_u = 0xb3
        const val Op_f32_convert_i64_s = 0xb4
        const val Op_f32_convert_i64_u = 0xb5
        const val Op_f32_demote_f64 = 0xb6
        const val Op_f64_convert_i32_s = 0xb7
        const val Op_f64_convert_i32_u = 0xb8
        const val Op_f64_convert_i64_s = 0xb9
        const val Op_f64_convert_i64_u = 0xba
        const val Op_f64_promote_f32 = 0xbb
        const val Op_i32_reinterpret_f32 = 0xbc
        const val Op_i64_reinterpret_f64 = 0xbd
        const val Op_f32_reinterpret_i32 = 0xbe
        const val Op_f64_reinterpret_i64 = 0xbf
        const val Op_i32_extend8_s = 0xc0
        const val Op_i32_extend16_s = 0xc1
        const val Op_i64_extend8_s = 0xc2
        const val Op_i64_extend16_s = 0xc3
        const val Op_i64_extend32_s = 0xc4
        const val Op_ref_null = 0xd0
        const val Op_ref_is_null = 0xd1
        const val Op_i32_drop = 0x100 // local & global instructions one per type at 0x1xx
        const val Op_i64_drop = 0x101
        const val Op_f32_drop = 0x102
        const val Op_f64_drop = 0x103
        const val Op_v128_drop = 0x104
        const val Op_i32_local_get = 0x110
        const val Op_i64_local_get = 0x111
        const val Op_f32_local_get = 0x112
        const val Op_f64_local_get = 0x113
        const val Op_v128_local_get = 0x114
        const val Op_i32_local_set = 0x120
        const val Op_i64_local_set = 0x121
        const val Op_f32_local_set = 0x122
        const val Op_f64_local_set = 0x123
        const val Op_v128_local_set = 0x124
        const val Op_i32_local_tee = 0x130
        const val Op_i64_local_tee = 0x131
        const val Op_f32_local_tee = 0x132
        const val Op_f64_local_tee = 0x133
        const val Op_v128_local_tee = 0x134
        const val Op_i32_global_get = 0x140
        const val Op_i64_global_get = 0x141
        const val Op_f32_global_get = 0x142
        const val Op_f64_global_get = 0x143
        const val Op_v128_global_get = 0x144
        const val Op_i32_global_set = 0x150
        const val Op_i64_global_set = 0x151
        const val Op_f32_global_set = 0x152
        const val Op_f64_global_set = 0x153
        const val Op_v128_global_set = 0x154
        const val Op_i32_return = 0x160
        const val Op_i64_return = 0x161
        const val Op_f32_return = 0x162
        const val Op_f64_return = 0x163
        const val Op_v128_return = 0x164
        const val Op_void_return = 0x165
        const val Op_i32_short_const = 0x170
        const val Op_i64_short_const = 0x171
        const val Op_f32_short_const = 0x172
        const val Op_f64_short_const = 0x173
        const val Op_goto = 0x180
        const val Op_goto_if = 0x181
        const val Op_goto_if_not = 0x182
        const val Op_goto_table = 0x183
        const val Op_i32_select = 0x190
        const val Op_i64_select = 0x191
        const val Op_f32_select = 0x192
        const val Op_f64_select = 0x193
        const val Op_v128_select = 0x194
        const val Op_goto_if_not_i32_eqz = 0x1a0
        const val Op_goto_if_not_i32_eq =  0x1a1
        const val Op_goto_if_not_i32_ne =  0x1a2
        const val Op_goto_if_not_i32_lt_s = 0x1a3
        const val Op_goto_if_not_i32_lt_u = 0x1a4
        const val Op_goto_if_not_i32_gt_s = 0x1a5
        const val Op_goto_if_not_i32_gt_u = 0x1a6
        const val Op_goto_if_not_i32_le_s = 0x1a7
        const val Op_goto_if_not_i32_le_u = 0x1a8
        const val Op_goto_if_not_i32_ge_s = 0x1a9
        const val Op_goto_if_not_i32_ge_u = 0x1aa
        const val Op_goto_if_not_i64_eqz = 0x1ab
        const val Op_goto_if_not_i64_eq = 0x1ac
        const val Op_goto_if_not_i64_ne = 0x1ad
        const val Op_goto_if_not_i64_lt_s = 0x1ae
        const val Op_goto_if_not_i64_lt_u = 0x1af
        const val Op_goto_if_not_i64_gt_s = 0x1b0
        const val Op_goto_if_not_i64_gt_u = 0x1b1
        const val Op_goto_if_not_i64_le_s = 0x1b2
        const val Op_goto_if_not_i64_le_u = 0x1b3
        const val Op_goto_if_not_i64_ge_s = 0x1b4
        const val Op_goto_if_not_i64_ge_u = 0x1b5
        const val Op_goto_if_not_f32_eq = 0x1b6
        const val Op_goto_if_not_f32_ne = 0x1b7
        const val Op_goto_if_not_f32_lt = 0x1b8
        const val Op_goto_if_not_f32_gt = 0x1b9
        const val Op_goto_if_not_f32_le = 0x1ba
        const val Op_goto_if_not_f32_ge = 0x1bb
        const val Op_goto_if_not_f64_eq = 0x1bc
        const val Op_goto_if_not_f64_ne = 0x1bd
        const val Op_goto_if_not_f64_lt = 0x1be
        const val Op_goto_if_not_f64_gt = 0x1bf
        const val Op_goto_if_not_f64_le = 0x1c0
        const val Op_goto_if_not_f64_ge = 0x1c1
        const val Op_i32_trunc_sat_f32_s = 0x200 // 0xFCxx instructions moved to 0x2xx
        const val Op_i32_trunc_sat_f32_u = 0x201
        const val Op_i32_trunc_sat_f64_s = 0x202
        const val Op_i32_trunc_sat_f64_u = 0x203
        const val Op_i64_trunc_sat_f32_s = 0x204
        const val Op_i64_trunc_sat_f32_u = 0x205
        const val Op_i64_trunc_sat_f64_s = 0x206
        const val Op_i64_trunc_sat_f64_u = 0x207
        const val Op_memory_init = 0x208
        const val Op_data_drop = 0x209
        const val Op_memory_copy = 0x20A
        const val Op_memory_fill = 0x20B
        const val Op_table_init = 0x20C
        const val Op_elem_drop = 0x20D
        const val Op_table_copy = 0x20E
        const val Op_table_grow = 0x20F
        const val Op_table_size = 0x210
        const val Op_table_fill = 0x211

        val OP_FROM_NAME = mapOf(
            "Op_unreachable" to 0x00,
            "Op_nop" to 0x01,
            "Op_block" to 0x02,
            "Op_loop" to 0x03,
            "Op_if" to 0x04,
            "Op_else" to 0x05,
            "Op_try" to 0x06,
            "Op_catch" to 0x07,
            "Op_throw" to 0x08,
            "Op_rethrow" to 0x09,
            "Op_reserved_0x0a" to 0x0a,
            "Op_end" to 0x0b,
            "Op_br" to 0x0c,
            "Op_br_if" to 0x0d,
            "Op_br_table" to 0x0e,
            "Op_return" to 0x0f,
            "Op_call" to 0x10,
            "Op_call_indirect" to 0x11,
            "Op_return_call" to 0x12,
            "Op_return_call_indirect" to 0x13,
            "Op_call_ref" to 0x14,
            "Op_return_call_ref" to 0x15,
            "Op_delegate" to 0x18,
            "Op_catch_all" to 0x19,
            "Op_drop" to 0x1a,
            "Op_select" to 0x1b,
            "Op_local_get" to 0x20,
            "Op_local_set" to 0x21,
            "Op_local_tee" to 0x22,
            "Op_global_get" to 0x23,
            "Op_global_set" to 0x24,
            "Op_i32_load" to 0x28,
            "Op_i64_load" to 0x29,
            "Op_f32_load" to 0x2a,
            "Op_f64_load" to 0x2b,
            "Op_i32_load8_s" to 0x2c,
            "Op_i32_load8_u" to 0x2d,
            "Op_i32_load16_s" to 0x2e,
            "Op_i32_load16_u" to 0x2f,
            "Op_i64_load8_s" to 0x30,
            "Op_i64_load8_u" to 0x31,
            "Op_i64_load16_s" to 0x32,
            "Op_i64_load16_u" to 0x33,
            "Op_i64_load32_s" to 0x34,
            "Op_i64_load32_u" to 0x35,
            "Op_i32_store" to 0x36,
            "Op_i64_store" to 0x37,
            "Op_f32_store" to 0x38,
            "Op_f64_store" to 0x39,
            "Op_i32_store8" to 0x3a,
            "Op_i32_store16" to 0x3b,
            "Op_i64_store8" to 0x3c,
            "Op_i64_store16" to 0x3d,
            "Op_i64_store32" to 0x3e,
            "Op_memory_size" to 0x3f,
            "Op_memory_grow" to 0x40,
            "Op_i32_const" to 0x41,
            "Op_i64_const" to 0x42,
            "Op_f32_const" to 0x43,
            "Op_f64_const" to 0x44,
            "Op_i32_eqz" to 0x45,
            "Op_i32_eq" to  0x46,
            "Op_i32_ne" to  0x47,
            "Op_i32_lt_s" to 0x48,
            "Op_i32_lt_u" to 0x49,
            "Op_i32_gt_s" to 0x4a,
            "Op_i32_gt_u" to 0x4b,
            "Op_i32_le_s" to 0x4c,
            "Op_i32_le_u" to 0x4d,
            "Op_i32_ge_s" to 0x4e,
            "Op_i32_ge_u" to 0x4f,
            "Op_i64_eqz" to 0x50,
            "Op_i64_eq" to 0x51,
            "Op_i64_ne" to 0x52,
            "Op_i64_lt_s" to 0x53,
            "Op_i64_lt_u" to 0x54,
            "Op_i64_gt_s" to 0x55,
            "Op_i64_gt_u" to 0x56,
            "Op_i64_le_s" to 0x57,
            "Op_i64_le_u" to 0x58,
            "Op_i64_ge_s" to 0x59,
            "Op_i64_ge_u" to 0x5a,
            "Op_f32_eq" to 0x5b,
            "Op_f32_ne" to 0x5c,
            "Op_f32_lt" to 0x5d,
            "Op_f32_gt" to 0x5e,
            "Op_f32_le" to 0x5f,
            "Op_f32_ge" to 0x60,
            "Op_f64_eq" to 0x61,
            "Op_f64_ne" to 0x62,
            "Op_f64_lt" to 0x63,
            "Op_f64_gt" to 0x64,
            "Op_f64_le" to 0x65,
            "Op_f64_ge" to 0x66,
            "Op_i32_clz" to 0x67,
            "Op_i32_ctz" to 0x68,
            "Op_i32_popcnt" to 0x69,
            "Op_i32_add" to 0x6a,
            "Op_i32_sub" to 0x6b,
            "Op_i32_mul" to 0x6c,
            "Op_i32_div_s" to 0x6d,
            "Op_i32_div_u" to 0x6e,
            "Op_i32_rem_s" to 0x6f,
            "Op_i32_rem_u" to 0x70,
            "Op_i32_and" to 0x71,
            "Op_i32_or" to 0x72,
            "Op_i32_xor" to 0x73,
            "Op_i32_shl" to 0x74,
            "Op_i32_shr_s" to 0x75,
            "Op_i32_shr_u" to 0x76,
            "Op_i32_rotl" to 0x77,
            "Op_i32_rotr" to 0x78,
            "Op_i64_clz" to 0x79,
            "Op_i64_ctz" to 0x7a,
            "Op_i64_popcnt" to 0x7b,
            "Op_i64_add" to 0x7c,
            "Op_i64_sub" to 0x7d,
            "Op_i64_mul" to 0x7e,
            "Op_i64_div_s" to 0x7f,
            "Op_i64_div_u" to 0x80,
            "Op_i64_rem_s" to 0x81,
            "Op_i64_rem_u" to 0x82,
            "Op_i64_and" to 0x83,
            "Op_i64_or" to 0x84,
            "Op_i64_xor" to 0x85,
            "Op_i64_shl" to 0x86,
            "Op_i64_shr_s" to 0x87,
            "Op_i64_shr_u" to 0x88,
            "Op_i64_rotl" to 0x89,
            "Op_i64_rotr" to 0x8a,
            "Op_f32_abs" to 0x8b,
            "Op_f32_neg" to 0x8c,
            "Op_f32_ceil" to 0x8d,
            "Op_f32_floor" to 0x8e,
            "Op_f32_trunc" to 0x8f,
            "Op_f32_nearest" to 0x90,
            "Op_f32_sqrt" to 0x91,
            "Op_f32_add" to 0x92,
            "Op_f32_sub" to 0x93,
            "Op_f32_mul" to 0x94,
            "Op_f32_div" to 0x95,
            "Op_f32_min" to 0x96,
            "Op_f32_max" to 0x97,
            "Op_f32_copysign" to 0x98,
            "Op_f64_abs" to 0x99,
            "Op_f64_neg" to 0x9a,
            "Op_f64_ceil" to 0x9b,
            "Op_f64_floor" to 0x9c,
            "Op_f64_trunc" to 0x9d,
            "Op_f64_nearest" to 0x9e,
            "Op_f64_sqrt" to 0x9f,
            "Op_f64_add" to 0xa0,
            "Op_f64_sub" to 0xa1,
            "Op_f64_mul" to 0xa2,
            "Op_f64_div" to 0xa3,
            "Op_f64_min" to 0xa4,
            "Op_f64_max" to 0xa5,
            "Op_f64_copysign" to 0xa6,
            "Op_i32_wrap_i64" to 0xa7,
            "Op_i32_trunc_f32_s" to 0xa8,
            "Op_i32_trunc_f32_u" to 0xa9,
            "Op_i32_trunc_f64_s" to 0xaa,
            "Op_i32_trunc_f64_u" to 0xab,
            "Op_i64_extend_i32_s" to 0xac,
            "Op_i64_extend_i32_u" to 0xad,
            "Op_i64_trunc_f32_s" to 0xae,
            "Op_i64_trunc_f32_u" to 0xaf,
            "Op_i64_trunc_f64_s" to 0xb0,
            "Op_i64_trunc_f64_u" to 0xb1,
            "Op_f32_convert_i32_s" to 0xb2,
            "Op_f32_convert_i32_u" to 0xb3,
            "Op_f32_convert_i64_s" to 0xb4,
            "Op_f32_convert_i64_u" to 0xb5,
            "Op_f32_demote_f64" to 0xb6,
            "Op_f64_convert_i32_s" to 0xb7,
            "Op_f64_convert_i32_u" to 0xb8,
            "Op_f64_convert_i64_s" to 0xb9,
            "Op_f64_convert_i64_u" to 0xba,
            "Op_f64_promote_f32" to 0xbb,
            "Op_i32_reinterpret_f32" to 0xbc,
            "Op_i64_reinterpret_f64" to 0xbd,
            "Op_f32_reinterpret_i32" to 0xbe,
            "Op_f64_reinterpret_i64" to 0xbf,
            "Op_i32_extend8_s" to 0xc0,
            "Op_i32_extend16_s" to 0xc1,
            "Op_i64_extend8_s" to 0xc2,
            "Op_i64_extend16_s" to 0xc3,
            "Op_i64_extend32_s" to 0xc4,
            "Op_ref_null" to 0xd0,
            "Op_ref_is_null" to 0xd1,
            "Op_i32_drop" to 0x100,
            "Op_i64_drop" to 0x101,
            "Op_f32_drop" to 0x102,
            "Op_f64_drop" to 0x103,
            "Op_v128_drop" to 0x104,
            "Op_i32_local_get" to 0x110,
            "Op_i64_local_get" to 0x111,
            "Op_f32_local_get" to 0x112,
            "Op_f64_local_get" to 0x113,
            "Op_v128_local_get" to 0x114,
            "Op_i32_local_set" to 0x120,
            "Op_i64_local_set" to 0x121,
            "Op_f32_local_set" to 0x122,
            "Op_f64_local_set" to 0x123,
            "Op_v128_local_set" to 0x124,
            "Op_i32_local_tee" to 0x130,
            "Op_i64_local_tee" to 0x131,
            "Op_f32_local_tee" to 0x132,
            "Op_f64_local_tee" to 0x133,
            "Op_v128_local_tee" to 0x134,
            "Op_i32_global_get" to 0x140,
            "Op_i64_global_get" to 0x141,
            "Op_f32_global_get" to 0x142,
            "Op_f64_global_get" to 0x143,
            "Op_v128_global_get" to 0x144,
            "Op_i32_global_set" to 0x150,
            "Op_i64_global_set" to 0x151,
            "Op_f32_global_set" to 0x152,
            "Op_f64_global_set" to 0x153,
            "Op_v128_global_set" to 0x154,
            "Op_i32_return" to 0x160,
            "Op_i64_return" to 0x161,
            "Op_f32_return" to 0x162,
            "Op_f64_return" to 0x163,
            "Op_v128_return" to 0x164,
            "Op_void_return" to 0x165,
            "Op_i32_short_const" to 0x170,
            "Op_i64_short_const" to 0x171,
            "Op_f32_short_const" to 0x172,
            "Op_f64_short_const" to 0x173,
            "Op_goto" to 0x180,
            "Op_goto_if" to 0x181,
            "Op_goto_if_not" to 0x182,
            "Op_goto_table" to 0x183,
            "Op_i32_select" to 0x190,
            "Op_i64_select" to 0x191,
            "Op_f32_select" to 0x192,
            "Op_f64_select" to 0x193,
            "Op_v128_select" to 0x194,
            "Op_goto_if_not_i32_eqz" to 0x1a0,
            "Op_goto_if_not_i32_eq" to  0x1a1,
            "Op_goto_if_not_i32_ne" to  0x1a2,
            "Op_goto_if_not_i32_lt_s" to 0x1a3,
            "Op_goto_if_not_i32_lt_u" to 0x1a4,
            "Op_goto_if_not_i32_gt_s" to 0x1a5,
            "Op_goto_if_not_i32_gt_u" to 0x1a6,
            "Op_goto_if_not_i32_le_s" to 0x1a7,
            "Op_goto_if_not_i32_le_u" to 0x1a8,
            "Op_goto_if_not_i32_ge_s" to 0x1a9,
            "Op_goto_if_not_i32_ge_u" to 0x1aa,
            "Op_goto_if_not_i64_eqz" to 0x1ab,
            "Op_goto_if_not_i64_eq" to 0x1ac,
            "Op_goto_if_not_i64_ne" to 0x1ad,
            "Op_goto_if_not_i64_lt_s" to 0x1ae,
            "Op_goto_if_not_i64_lt_u" to 0x1af,
            "Op_goto_if_not_i64_gt_s" to 0x1b0,
            "Op_goto_if_not_i64_gt_u" to 0x1b1,
            "Op_goto_if_not_i64_le_s" to 0x1b2,
            "Op_goto_if_not_i64_le_u" to 0x1b3,
            "Op_goto_if_not_i64_ge_s" to 0x1b4,
            "Op_goto_if_not_i64_ge_u" to 0x1b5,
            "Op_goto_if_not_f32_eq" to 0x1b6,
            "Op_goto_if_not_f32_ne" to 0x1b7,
            "Op_goto_if_not_f32_lt" to 0x1b8,
            "Op_goto_if_not_f32_gt" to 0x1b9,
            "Op_goto_if_not_f32_le" to 0x1ba,
            "Op_goto_if_not_f32_ge" to 0x1bb,
            "Op_goto_if_not_f64_eq" to 0x1bc,
            "Op_goto_if_not_f64_ne" to 0x1bd,
            "Op_goto_if_not_f64_lt" to 0x1be,
            "Op_goto_if_not_f64_gt" to 0x1bf,
            "Op_goto_if_not_f64_le" to 0x1c0,
            "Op_goto_if_not_f64_ge" to 0x1c1,
            "Op_i32_trunc_sat_f32_s" to 0x200,
            "Op_i32_trunc_sat_f32_u" to 0x201,
            "Op_i32_trunc_sat_f64_s" to 0x202,
            "Op_i32_trunc_sat_f64_u" to 0x203,
            "Op_i64_trunc_sat_f32_s" to 0x204,
            "Op_i64_trunc_sat_f32_u" to 0x205,
            "Op_i64_trunc_sat_f64_s" to 0x206,
            "Op_i64_trunc_sat_f64_u" to 0x207,
            "Op_memory_init" to 0x208,
            "Op_data_drop" to 0x209,
            "Op_memory_copy" to 0x20A,
            "Op_memory_fill" to 0x20B,
            "Op_table_init" to 0x20C,
            "Op_elem_drop" to 0x20D,
            "Op_table_copy" to 0x20E,
            "Op_table_grow" to 0x20F,
            "Op_table_size" to 0x210,
            "Op_table_fill" to 0x211,
        )

        val NAME_FROM_OP = OP_FROM_NAME.flip()
    }
}

data class WasmDebugContext(val name: String, val index: Int)

data class WasmInterpreterCode constructor(
    val instructions: IntArray,
    val intPool: IntArray,
    val longPool: LongArray,
    val floatPool: FloatArray,
    val doublePool: DoubleArray,
    val paramsSize: Int = -1,
    val localSize: Int = -1,
    val paramsCount: Int = -1,
    val localsCount: Int = -1,
    val localsOffsets: IntArray = IntArray(0),
    val endStack: List<WasmType> = emptyList(),
    val debug: WasmDebugContext
)
