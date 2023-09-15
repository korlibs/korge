package korlibs.wasm

import korlibs.encoding.*
import korlibs.datastructure.*
import korlibs.memory.*

// @TODO: Change stack-based to register based operations:
// example:
//   MUL(A, B, C) :: A = B * C
// instead of:
//   PUSH(B)
//   PUSH(C)
//   MUL()

class WasmRunInterpreter(val module: WasmModule, memPages: Int = 10, maxMemPages: Int = 0x10000) : WasmRuntime(memPages, maxMemPages) {
    val globalsI = IntArray(module.globals.size)
    val globalsF = FloatArray(module.globals.size)
    val globalsD = DoubleArray(module.globals.size)
    val globalsL = LongArray(module.globals.size)

    //val globals = Buffer.allocDirect(module.globals.maxOfOrNull { it.globalOffset + 16 } ?: 0)
    //val stack = Buffer.allocDirect(16 * 1024)
    val stackI = IntArray(4 * 1024)
    val stackF = FloatArray(4 * 1024)
    val stackD = DoubleArray(4 * 1024)
    val stackL = LongArray(4 * 1024)

    var localsPos = 0
    var stackPos = 0

    fun initGlobals(): WasmRunInterpreter {
        //println("GLOBALS: ${globals.sizeInBytes}")
        for (data in module.datas) {
            val e = data.e ?: continue
            eval(e, WasmType.Function(listOf(), listOf(WasmSType.I32)))
            val index = popI32()
            //println("DATA[index=$index] = bytes:${data.data.size}, stack=$stackPos")
            memory.setArrayInt8(index, data.data)
        }
        for (global in module.globals) {
            if (global.expr != null) {
                eval(global.expr, WasmType.Function(listOf(), listOf(global.globalType)))
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
                    val typeActual = eval(assert.actual, WasmType.Function(listOf(), listOf(WasmSType.I32)))
                    val typeExpect = eval(assert.expect, WasmType.Function(listOf(), listOf(WasmSType.I32)))
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

    fun eval(expr: WasmExpr?, type: WasmType.Function): List<WasmType> {
        if (expr == null) return emptyList()
        val code = compile(WasmFunc.anonymousFunc(type, expr), implicitReturn = false)
        //println(code.instructions.toList().map { it and 0xFFF })
        this.evalInstructions(code, 0)
        return code.endStack
    }

    fun WasmFunc.getInterprerCode(): WasmInterpreterCode? {
        if (code == null) return null
        if (code?.interpreterCode == null) {
            code?.interpreterCode = compile(this, implicitReturn = true)
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
            val result = functions[fimport.moduleName]?.get(fimport.name)?.invoke(this, readTypes(func.type.args.map { it.type }))
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
            if (trace) println("OP: ${op.hex}, param=$param : ${WasmOp.getOrNull(op)}")
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
                    val index = popI32()
                    val func = module.tables.first().items[index] as WasmFunc
                    callFunc(func)
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
            WasmSType.I32 -> pushI32(value as Int)
            WasmSType.I64 -> pushI64(value as Long)
            WasmSType.F32 -> pushF32(value as Float)
            WasmSType.F64 -> pushF64(value as Double)
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

    fun compile(func: WasmFunc, implicitReturn: Boolean): WasmInterpreterCode {
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
    }
}


class WasmInterpreterCode constructor(
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
)
