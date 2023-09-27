@file:OptIn(kotlin.ExperimentalStdlibApi::class)

package korlibs.wasm

import korlibs.logger.AnsiEscape.Companion.green
import korlibs.logger.AnsiEscape.Companion.red
import korlibs.math.*
import korlibs.memory.*
import kotlin.jvm.*
import kotlin.rotateLeft
import kotlin.rotateRight

open class WasmRuntime(module: WasmModule, val memSize: Int, val memMax: Int) {
    var usedClassMemory = 0
    var trace = false

    //var instructionsHistoriogram = IntArray(0x1000)
    var instructionsExecuted = 0L
    var memory = Buffer.allocDirect(memSize * PAGE_SIZE)
    val memoryNumPages: Int get() = memory.sizeInBytes / PAGE_SIZE

    interface WasmFuncCall {
        operator fun invoke(runtime: WasmRuntime, args: Array<Any?>): Any?
    }

    class WasmTable(val limit: WasmType.Limit) {
        var elements: Array<WasmFuncCall?> = arrayOfNulls(limit.min)

        fun tableEnsureSize(size: Int) {
            if (size > elements.size) {
                elements = elements.copyOf(size)
            }
        }
    }

    val tables = module.tables.map { WasmTable(it.limit) }

    val functions = LinkedHashMap<String, LinkedHashMap<String, WasmRuntime.(Array<Any?>) -> Any?>>()
    open val exported: Set<String> get() = functions.keys

    open fun register(moduleName: String, name: String, func: WasmRuntime.(Array<Any?>) -> Any?) {
        val moduleFunctions = functions.getOrPut(moduleName) { linkedMapOf() }
        moduleFunctions[name] = func
    }

    open operator fun invoke(funcName: String, vararg params: Any?): Any? = TODO()
    open fun invokeIndirect(index: Int, vararg params: Any?): Any? = TODO()

    fun writeBytes(ptr: Int, data: ByteArray) { memory.setArrayInt8(ptr, data) }
    fun readBytes(ptr: Int, out: ByteArray): ByteArray = memory.getArrayInt8(ptr, out)
    fun readBytes(ptr: Int, size: Int): ByteArray = readBytes(ptr, ByteArray(size))
    fun stackSave(): Int = this("stackSave") as Int
    fun stackRestore(stack: Int) { this("stackRestore", stack) }
    fun stackAlloc(size: Int): Int = this("stackAlloc", size) as Int
    fun stackAllocAndWrite(data: ByteArray): Int = stackAlloc(data.size).also { writeBytes(it, data) }
    fun alloc(size: Int): Int = this("malloc", size) as Int
    fun allocAndWrite(data: ByteArray): Int = alloc(data.size).also { writeBytes(it, data) }
    fun free(ptr: Int) { this("free", ptr) }
    fun free(vararg ptrs: Int) { for (ptr in ptrs) free(ptr) }
    open fun close() {
    }

    fun strlen(ptr: Int): Int {
        var n = 0
        while (memory.getS8(ptr + n) != 0.toByte()) n++
        return n
    }
    fun strlen16(ptr: Int): Int {
        var n = 0
        while (memory.getS16(ptr + n) != 0.toShort()) n += 2
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
            val v = memory.getS16(ptr + n).toInt() and 0xFFFF
            if (v == 0) break
            out += v.toChar()
            n += 2
        }
        return out
    }

    fun readString(ptr: Int): String {
        return readStringz16(ptr)
    }


    companion object {
        const val PAGE_SIZE = 64 * 1024 // (1 shl 16)

        @JvmStatic fun assert_return_any(actual: Any?, expect: Any?, msg: String): Int {
            val result = actual == expect
            val resultStr = if (result) "ok" else "error ($actual != $expect)"
            val line = "$msg... $resultStr"
            if (!result) {
                println(if (result) line.green else line.red)
            }
            return if (result) 0 else 1
            //if (actual != expect) error("$actual != $expect :: msg=$msg")
        }

        @JvmStatic fun assert_return_void(msg: String): Int = 0
        @JvmStatic fun assert_return_ref(actual: Any?, expect: Any?, msg: String): Int = assert_return_any(actual, expect, msg)
        @JvmStatic fun assert_return_i32(actual: Int, expect: Int, msg: String): Int = assert_return_any(actual, expect, msg)
        @JvmStatic fun assert_return_i64(actual: Long, expect: Long, msg: String): Int = assert_return_any(actual, expect, msg)
        @JvmStatic fun assert_return_f32(actual: Float, expect: Float, msg: String): Int = assert_return_any(actual, expect, msg)
        @JvmStatic fun assert_return_f64(actual: Double, expect: Double, msg: String): Int = assert_return_any(actual, expect, msg)

        @JvmStatic fun assert_summary(failed: Int, total: Int) {
            if (failed != 0) {
                println("SUMMARY: $failed/$total".red)
                error("Some assets failed!")
            }
        }

        @JvmStatic fun set_memory(ptr: Int, data: String, bytesLen: Int, runtime: WasmRuntime) {
            var ptr = ptr
            val mem = runtime.memory
            val isOdd = (bytesLen % 2 != 0)
            for (n in data.indices) {
                val c = data[n]
                if (isOdd && n == data.length - 1) {
                    mem.setUnalignedUInt8(ptr, c.code)
                    ptr++
                } else {
                    mem.setUnalignedUInt16(ptr, c.code)
                    ptr += 2
                }
            }
        }

        @JvmStatic fun read_string(ptr: Int, runtime: WasmRuntime): String {
            if (ptr == 0) return "<null>"
            var out = ""
            var n = 0
            while (true) {
                val v = runtime.memory.getUnalignedUInt16(ptr + n)
                if (v == 0) break
                out += v.toChar()
                n += 2
            }
            return out
        }

        //@JvmStatic fun env_abort(msgPtr: Int, filePtr: Int, line: Int, column: Int, runtime: WasmMemory): Unit {
        //    val msg = read_string(msgPtr, runtime)
        //    val file = read_string(filePtr, runtime)
        //    error("abort msg='$msg', file='$file', line=$line, column=$column")
        //}


        @JvmStatic fun create_unreachable_exception_instance(msg: String): Throwable = Exception("unreachable:$msg")
        @JvmStatic fun create_todo_exception_instance(): Throwable = NotImplementedError()
        @JvmStatic fun create_unknown_indirect_function(index: Int, msg: String): Throwable = Exception("Unknown indirect function at index $index, msg='$msg'")
        @JvmStatic fun call_indirect(tableIndex: Int, index: Int, args: Array<Any?>, runtime: WasmRuntime): Any? {
            val func = runtime.tables[tableIndex].elements[index] ?: error("call_indirect: tableIndex=$tableIndex, func=$index, args=${args.toList()}")
            return func.invoke(runtime, args)
        }

        inline fun checkAddr(addr: Int, offset: Int, runtime: WasmRuntime): Int {
            return (addr + offset).also { 
                if (it < 0 || it >= runtime.memory.sizeInBytes) error("Out of bounds addr=$addr, offset=$offset, memorySize=${runtime.memory.sizeInBytes}")
            }
        }

        @JvmStatic fun Op_i32_load(addr: Int, offset: Int, runtime: WasmRuntime): Int = runtime.memory.getS32(checkAddr(addr, offset, runtime))
        @JvmStatic fun Op_i32_load8_s(addr: Int, offset: Int, runtime: WasmRuntime): Int = runtime.memory.getS8(checkAddr(addr, offset, runtime)).toInt()
        @JvmStatic fun Op_i32_load8_u(addr: Int, offset: Int, runtime: WasmRuntime): Int = runtime.memory.getU8(checkAddr(addr, offset, runtime)).toInt()
        @JvmStatic fun Op_i32_load16_s(addr: Int, offset: Int, runtime: WasmRuntime): Int = runtime.memory.getS16(checkAddr(addr, offset, runtime)).toInt()
        @JvmStatic fun Op_i32_load16_u(addr: Int, offset: Int, runtime: WasmRuntime): Int = runtime.memory.getU16(checkAddr(addr, offset, runtime)).toInt()
        @JvmStatic fun Op_i64_load(addr: Int, offset: Int, runtime: WasmRuntime): Long = runtime.memory.getS64(checkAddr(addr, offset, runtime))
        @JvmStatic fun Op_i64_load8_s(addr: Int, offset: Int, runtime: WasmRuntime): Long = runtime.memory.getS8(checkAddr(addr, offset, runtime)).toLong()
        @JvmStatic fun Op_i64_load8_u(addr: Int, offset: Int, runtime: WasmRuntime): Long = runtime.memory.getU8(checkAddr(addr, offset, runtime)).toLong()
        @JvmStatic fun Op_i64_load16_s(addr: Int, offset: Int, runtime: WasmRuntime): Long = runtime.memory.getS16(checkAddr(addr, offset, runtime)).toLong()
        @JvmStatic fun Op_i64_load16_u(addr: Int, offset: Int, runtime: WasmRuntime): Long = runtime.memory.getU16(checkAddr(addr, offset, runtime)).toLong()
        @JvmStatic fun Op_i64_load32_s(addr: Int, offset: Int, runtime: WasmRuntime): Long = runtime.memory.getS32(checkAddr(addr, offset, runtime)).toLong()
        @JvmStatic fun Op_i64_load32_u(addr: Int, offset: Int, runtime: WasmRuntime): Long = runtime.memory.getS32(checkAddr(addr, offset, runtime)).toUInt().toLong()
        @JvmStatic fun Op_f32_load(addr: Int, offset: Int, runtime: WasmRuntime): Float = runtime.memory.getF32(checkAddr(addr, offset, runtime))
        @JvmStatic fun Op_f64_load(addr: Int, offset: Int, runtime: WasmRuntime): Double = runtime.memory.getF64(checkAddr(addr, offset, runtime))
        @JvmStatic fun Op_i32_store(addr: Int, value: Int, offset: Int, runtime: WasmRuntime) = runtime.memory.set32(checkAddr(addr, offset, runtime), value)
        @JvmStatic fun Op_i32_store8(addr: Int, value: Int, offset: Int, runtime: WasmRuntime) = runtime.memory.set8(checkAddr(addr, offset, runtime), value.toByte())
        @JvmStatic fun Op_i32_store16(addr: Int, value: Int, offset: Int, runtime: WasmRuntime) = runtime.memory.set16(checkAddr(addr, offset, runtime), value.toShort())
        @JvmStatic fun Op_i64_store(addr: Int, value: Long, offset: Int, runtime: WasmRuntime) = runtime.memory.set64(checkAddr(addr, offset, runtime), value)
        @JvmStatic fun Op_i64_store8(addr: Int, value: Long, offset: Int, runtime: WasmRuntime) = runtime.memory.set8(checkAddr(addr, offset, runtime), value.toInt().toByte())
        @JvmStatic fun Op_i64_store16(addr: Int, value: Long, offset: Int, runtime: WasmRuntime) = runtime.memory.set16(checkAddr(addr, offset, runtime), value.toShort())
        @JvmStatic fun Op_i64_store32(addr: Int, value: Long, offset: Int, runtime: WasmRuntime) = runtime.memory.set32(checkAddr(addr, offset, runtime), value.toInt())
        @JvmStatic fun Op_f32_store(addr: Int, value: Float, offset: Int, runtime: WasmRuntime) = runtime.memory.setF32(checkAddr(addr, offset, runtime), value)
        @JvmStatic fun Op_f64_store(addr: Int, value: Double, offset: Int, runtime: WasmRuntime) = runtime.memory.setF64(checkAddr(addr, offset, runtime), value)
        @JvmStatic fun Op_i32_eqz(value: Int): Int = (value == 0).toInt()
        @JvmStatic fun Op_i32_eq(l: Int, r: Int): Int = (l == r).toInt()
        @JvmStatic fun Op_i32_ne(l: Int, r: Int): Int = (l != r).toInt()
        @JvmStatic fun Op_i32_lt_s(l: Int, r: Int): Int = (l < r).toInt()
        @JvmStatic fun Op_i32_le_s(l: Int, r: Int): Int = (l <= r).toInt()
        @JvmStatic fun Op_i32_ge_s(l: Int, r: Int): Int = (l >= r).toInt()
        @JvmStatic fun Op_i32_gt_s(l: Int, r: Int): Int = (l > r).toInt()
        @JvmStatic fun Op_i32_lt_u(l: Int, r: Int): Int = (l.toUInt() < r.toUInt()).toInt()
        @JvmStatic fun Op_i32_le_u(l: Int, r: Int): Int = (l.toUInt() <= r.toUInt()).toInt()
        @JvmStatic fun Op_i32_ge_u(l: Int, r: Int): Int = (l.toUInt() >= r.toUInt()).toInt()
        @JvmStatic fun Op_i32_gt_u(l: Int, r: Int): Int = (l.toUInt() > r.toUInt()).toInt()
        @JvmStatic fun Op_i64_eqz(value: Long): Int = (value == 0L).toInt()
        @JvmStatic fun Op_i64_eq(l: Long, r: Long): Int = (l == r).toInt()
        @JvmStatic fun Op_i64_ne(l: Long, r: Long): Int = (l != r).toInt()
        @JvmStatic fun Op_i64_lt_s(l: Long, r: Long): Int = (l < r).toInt()
        @JvmStatic fun Op_i64_le_s(l: Long, r: Long): Int = (l <= r).toInt()
        @JvmStatic fun Op_i64_ge_s(l: Long, r: Long): Int = (l >= r).toInt()
        @JvmStatic fun Op_i64_gt_s(l: Long, r: Long): Int = (l > r).toInt()
        @JvmStatic fun Op_i64_lt_u(l: Long, r: Long): Int = (l.toULong() < r.toULong()).toInt()
        @JvmStatic fun Op_i64_le_u(l: Long, r: Long): Int = (l.toULong() <= r.toULong()).toInt()
        @JvmStatic fun Op_i64_ge_u(l: Long, r: Long): Int = (l.toULong() >= r.toULong()).toInt()
        @JvmStatic fun Op_i64_gt_u(l: Long, r: Long): Int = (l.toULong() > r.toULong()).toInt()
        @JvmStatic fun Op_f32_le(l: Float, r: Float): Int = (l <= r).toInt()
        @JvmStatic fun Op_f32_lt(l: Float, r: Float): Int = (l < r).toInt()
        @JvmStatic fun Op_f32_eq(l: Float, r: Float): Int = (l == r).toInt()
        @JvmStatic fun Op_f32_ne(l: Float, r: Float): Int = (l != r).toInt()
        @JvmStatic fun Op_f32_gt(l: Float, r: Float): Int = (l > r).toInt()
        @JvmStatic fun Op_f32_ge(l: Float, r: Float): Int = (l >= r).toInt()
        @JvmStatic fun Op_f64_le(l: Double, r: Double): Int = (l <= r).toInt()
        @JvmStatic fun Op_f64_lt(l: Double, r: Double): Int = (l < r).toInt()
        @JvmStatic fun Op_f64_eq(l: Double, r: Double): Int = (l == r).toInt()
        @JvmStatic fun Op_f64_ne(l: Double, r: Double): Int = (l != r).toInt()
        @JvmStatic fun Op_f64_gt(l: Double, r: Double): Int = (l > r).toInt()
        @JvmStatic fun Op_f64_ge(l: Double, r: Double): Int = (l >= r).toInt()
        @JvmStatic fun Op_selectI(l: Int, r: Int, v: Int): Int = if (v != 0) l else r
        @JvmStatic fun Op_selectL(l: Long, r: Long, v: Int): Long = if (v != 0) l else r
        @JvmStatic fun Op_selectF(l: Float, r: Float, v: Int): Float = if (v != 0) l else r
        @JvmStatic fun Op_selectD(l: Double, r: Double, v: Int): Double = if (v != 0) l else r
        @JvmStatic fun Op_i32_div_u(l: Int, r: Int): Int = (l.toUInt() / r.toUInt()).toInt()
        @JvmStatic fun Op_i32_rem_u(l: Int, r: Int): Int = (l.toUInt() % r.toUInt()).toInt()
        @JvmStatic fun Op_i32_clz(v: Int): Int = v.countLeadingZeroBits()
        @JvmStatic fun Op_i32_ctz(v: Int): Int = v.countTrailingZeroBits()
        @JvmStatic fun Op_i32_popcnt(v: Int): Int = v.countOneBits()
        @JvmStatic fun Op_i64_popcnt(v: Long): Long = v.countOneBits().toLong()
        @JvmStatic fun Op_i32_rotl(v: Int, bits: Int): Int = v.rotateLeft(bits)
        @JvmStatic fun Op_i32_rotr(v: Int, bits: Int): Int = v.rotateRight(bits)
        @JvmStatic fun Op_i64_div_u(l: Long, r: Long): Long = (l.toULong() / r.toULong()).toLong()
        @JvmStatic fun Op_i64_rem_u(l: Long, r: Long): Long = (l.toULong() % r.toULong()).toLong()
        @JvmStatic fun Op_i64_clz(v: Long): Long = v.countLeadingZeroBits().toLong()
        @JvmStatic fun Op_i64_ctz(v: Long): Long = v.countTrailingZeroBits().toLong()
        @JvmStatic fun Op_i64_rotl(v: Long, bits: Long): Long = v.rotateLeft(bits.toInt())
        @JvmStatic fun Op_i64_rotr(v: Long, bits: Long): Long = v.rotateRight(bits.toInt())
        @JvmStatic fun Op_i64_extend8_s(v: Long): Long = v.toByte().toLong()
        @JvmStatic fun Op_i64_extend16_s(v: Long): Long = v.toShort().toLong()
        @JvmStatic fun Op_i64_extend32_s(v: Long): Long = v.toInt().toLong()
        @JvmStatic fun Op_i64_extend_i32_u(v: Int): Long = v.toUInt().toLong()
        @JvmStatic fun Op_i64_extend_i32_s(v: Int): Long = v.toLong()
        @JvmStatic fun Op_i32_wrap_i64(v: Long): Int = v.toInt()
        @JvmStatic fun Op_i32_extend8_s(v: Int): Int = v.toByte().toInt()
        @JvmStatic fun Op_i32_extend16_s(v: Int): Int = v.toShort().toInt()
        @JvmStatic fun Op_i32_reinterpret_f32(v: Float): Int = v.toRawBits()
        @JvmStatic fun Op_f32_reinterpret_i32(v: Int): Float = Float.fromBits(v)
        @JvmStatic fun Op_i64_reinterpret_f64(v: Double): Long = v.toRawBits()
        @JvmStatic fun Op_f64_reinterpret_i64(v: Long): Double = Double.fromBits(v)
        @JvmStatic fun Op_f32_convert_s_i32(v: Int): Float = v.toFloat()
        @JvmStatic fun Op_f32_convert_u_i32(v: Int): Float = v.toUInt().toFloat()
        @JvmStatic fun Op_f32_convert_s_i64(v: Long): Float = v.toFloat()
        @JvmStatic fun Op_f32_convert_u_i64(v: Long): Float = v.toULong().toFloat()
        @JvmStatic fun Op_f32_demote_f64(v: Double): Float = v.toFloat()
        @JvmStatic fun Op_f64_convert_s_i32(v: Int): Double = v.toDouble()
        @JvmStatic fun Op_f64_convert_u_i32(v: Int): Double = v.toUInt().toDouble()
        @JvmStatic fun Op_f64_convert_s_i64(v: Long): Double = v.toDouble()
        @JvmStatic fun Op_f64_convert_u_i64(v: Long): Double = v.toULong().toDouble()
        @JvmStatic fun Op_f64_promote_f32(v: Float): Double = v.toDouble()
        @JvmStatic fun Op_i32_trunc_u_f32(v: Float): Int = v.toUInt().toInt()
        @JvmStatic fun Op_i32_trunc_s_f32(v: Float): Int = v.toInt()
        @JvmStatic fun Op_i32_trunc_u_f64(v: Double): Int = v.toUInt().toInt()
        @JvmStatic fun Op_i32_trunc_s_f64(v: Double): Int = v.toInt()
        @JvmStatic fun Op_i32_trunc_sat_f32_u(v: Float): Int = v.coerceIn(UInt.MIN_VALUE.toFloat(), UInt.MAX_VALUE.toFloat()).toUInt().toInt()
        @JvmStatic fun Op_i32_trunc_sat_f32_s(v: Float): Int = v.coerceIn(Int.MIN_VALUE.toFloat(), Int.MAX_VALUE.toFloat()).toInt()
        @JvmStatic fun Op_i32_trunc_sat_f64_u(v: Double): Int = v.coerceIn(UInt.MIN_VALUE.toDouble(), UInt.MAX_VALUE.toDouble()).toUInt().toInt()
        @JvmStatic fun Op_i32_trunc_sat_f64_s(v: Double): Int = v.coerceIn(Int.MIN_VALUE.toDouble(), Int.MAX_VALUE.toDouble()).toInt()
        @JvmStatic fun Op_i64_trunc_u_f32(v: Float): Long = v.toULong().toLong()
        @JvmStatic fun Op_i64_trunc_s_f32(v: Float): Long = v.toLong()
        @JvmStatic fun Op_i64_trunc_u_f64(v: Double): Long = v.toULong().toLong()
        @JvmStatic fun Op_i64_trunc_s_f64(v: Double): Long = v.toLong()
        @JvmStatic fun Op_i64_trunc_sat_f32_u(v: Float): Long = v.coerceIn(UInt.MIN_VALUE.toFloat(), UInt.MAX_VALUE.toFloat()).toULong().toLong()
        @JvmStatic fun Op_i64_trunc_sat_f32_s(v: Float): Long = v.coerceIn(Int.MIN_VALUE.toFloat(), Int.MAX_VALUE.toFloat()).toLong()
        @JvmStatic fun Op_i64_trunc_sat_f64_u(v: Double): Long = v.coerceIn(UInt.MIN_VALUE.toDouble(), UInt.MAX_VALUE.toDouble()).toULong().toLong()
        @JvmStatic fun Op_i64_trunc_sat_f64_s(v: Double): Long = v.coerceIn(Int.MIN_VALUE.toDouble(), Int.MAX_VALUE.toDouble()).toLong()
        @JvmStatic fun Op_f32_min(l: Float, r: Float): Float = kotlin.math.min(l, r)
        @JvmStatic fun Op_f32_max(l: Float, r: Float): Float = kotlin.math.max(l, r)
        @JvmStatic fun Op_f32_copysign(magnitude: Float, sign: Float): Float {
            val SIGN_BIT_MASK = -0x80000000
            val EXP_BIT_MASK = 0x7F800000
            val SIGNIF_BIT_MASK = 0x007FFFFF
            return Float.fromBits(
                sign.toRawBits() and SIGN_BIT_MASK or magnitude.toRawBits() and (EXP_BIT_MASK or SIGNIF_BIT_MASK)
            )
        }
        @JvmStatic fun Op_f32_abs(v: Float): Float = kotlin.math.abs(v)
        @JvmStatic fun Op_f32_sqrt(v: Float): Float = kotlin.math.sqrt(v.toDouble()).toFloat()
        @JvmStatic fun Op_f32_neg(v: Float): Float = -v
        @JvmStatic fun Op_f32_ceil(v: Float): Float = kotlin.math.ceil(v.toDouble()).toFloat()
        @JvmStatic fun Op_f32_floor(v: Float): Float = kotlin.math.floor(v.toDouble()).toFloat()
        @JvmStatic fun Op_f32_trunc(v: Float): Float = v.toInt().toFloat() // @TODO: REVIEW!
        @JvmStatic fun Op_f32_nearest(v: Float): Float = v.toInt().toFloat() // @TODO: REVIEW!
        @JvmStatic fun Op_f64_min(l: Double, r: Double): Double = kotlin.math.min(l, r)
        @JvmStatic fun Op_f64_max(l: Double, r: Double): Double = kotlin.math.max(l, r)
        @JvmStatic fun Op_f64_copysign(magnitude: Double, sign: Double): Double {
            val SIGN_BIT_MASK = 0x8000000000000000UL.toLong()
            //val SIGN_BIT_MASK = 0x8000000000000000L
            val EXP_BIT_MASK = 0x7FF0000000000000L
            val SIGNIF_BIT_MASK = 0x000FFFFFFFFFFFFFL
            return Double.fromBits(
                sign.toRawBits() and SIGN_BIT_MASK or magnitude.toRawBits() and (EXP_BIT_MASK or SIGNIF_BIT_MASK)
            )
        }
        @JvmStatic fun Op_f64_abs(v: Double): Double = kotlin.math.abs(v)
        @JvmStatic fun Op_f64_sqrt(v: Double): Double = kotlin.math.sqrt(v)
        @JvmStatic fun Op_f64_neg(v: Double): Double = -v
        @JvmStatic fun Op_f64_ceil(v: Double): Double = kotlin.math.ceil(v)
        @JvmStatic fun Op_f64_floor(v: Double): Double = kotlin.math.floor(v)
        @JvmStatic fun Op_f64_trunc(v: Double): Double = v.toInt().toDouble() // @TODO: REVIEW!
        @JvmStatic fun Op_f64_nearest(v: Double): Double = v.toInt().toDouble() // @TODO: REVIEW!

        @JvmStatic fun Op_memory_init(runtime: WasmRuntime): Unit {
            TODO()
        }

        @JvmStatic fun Op_memory_size(runtime: WasmRuntime): Int = runtime.memoryNumPages
        @JvmStatic fun Op_memory_grow(deltaPages: Int, runtime: WasmRuntime): Int {
            val oldMemory = runtime.memory
            val oldPages = runtime.memoryNumPages
            val newPages = oldPages + deltaPages
            if (newPages > runtime.memMax) {
                println("Op_memory_grow: oldPages=$oldPages, newPages=$newPages -> FAILED. Restored oldPages")
                return -1
            }
            runtime.memory = Buffer.allocDirect(newPages * PAGE_SIZE)
            arraycopy(oldMemory, 0, runtime.memory, 0, oldPages * PAGE_SIZE)
            return oldPages
        }
        @JvmStatic fun Op_memory_copy(dst: Int, src: Int, count: Int, runtime: WasmRuntime) {
            val mem = runtime.memory
            for (n in 0 until count) {
                mem.set8(dst + n, mem.getS8(src + n))
            }
        }
        @JvmStatic fun Op_memory_fill(dst: Int, value: Int, count: Int, runtime: WasmRuntime) {
            val mem = runtime.memory
            for (n in 0 until count) {
                mem.setUnalignedInt8(dst + n, value)
            }
        }
    }
}
