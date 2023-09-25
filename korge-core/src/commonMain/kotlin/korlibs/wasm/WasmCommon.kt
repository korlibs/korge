package korlibs.wasm

import korlibs.encoding.*
import korlibs.io.lang.*
import kotlin.reflect.*

/**
 * <https://pengowray.github.io/wasm-ops/>
 */
// https://pengowray.github.io/wasm-ops/
// https://webassembly.github.io/spec/core/text/instructions.html
enum class WasmOp(
    val id: Int,
    val sname: String,
    val istack: Int = -1, // input stack
    val rstack: Int = -1, // result/return/output stack
    val symbol: String = "<?>",
    val itypeOpt: WasmSType? = null, // input types
    val outType: WasmSType = WasmSType.VOID, // output type
    val kind: Kind
) {
    //Op_i32(0x7f),
    //Op_i64(0x7e),
    //Op_f32(0x7d),
    //Op_f64(0x7c),
    //Op_anyfunc(0x70),
    //Op_func(0x60),
    //Op_empty(0x40),


    // Control flow operators
    Op_unreachable(0x00, "unreachable", 0, 0, kind = Kind.FLOW),
    Op_nop(0x01, "nop", kind = Kind.FLOW),
    Op_block(0x02, "block", kind = Kind.FLOW),
    Op_loop(0x03, "loop", kind = Kind.FLOW),
    Op_if(0x04, "if", 1, -1, kind = Kind.FLOW),
    Op_else(0x05, "else", kind = Kind.FLOW),
    Op_try(0x06, "try", kind = Kind.EXCEPTION),
    Op_catch(0x07, "catch", kind = Kind.EXCEPTION),
    Op_throw(0x08, "throw", kind = Kind.EXCEPTION),
    Op_rethrow(0x09, "rethrow", kind = Kind.EXCEPTION),
    Op_reserved_0x0a(0x0a, "reserved.0x0a", kind = Kind.RESERVED),
    Op_end(0x0b, "end", kind = Kind.FLOW),
    Op_br(0x0c, "br", kind = Kind.FLOW),
    Op_br_if(0x0d, "br_if", 1, kind = Kind.FLOW),
    Op_br_table(0x0e, "br_table", kind = Kind.FLOW),
    Op_return(0x0f, "return", kind = Kind.FLOW),

    // Call operators
    Op_call(0x10, "call", -1, kind = Kind.CALL),
    Op_call_indirect(0x11, "call_indirect", -1, kind = Kind.CALL),

    Op_return_call(0x12, "return_call", -1, kind = Kind.CALL), // TAIL CALL PROPOSAL
    Op_return_call_indirect(0x13, "return_call_indirect", -1, kind = Kind.CALL), // TAIL CALL PROPOSAL

    Op_call_ref(0x14, "call_ref", -1, kind = Kind.CALL),
    Op_return_call_ref(0x15, "return_call_ref", -1, kind = Kind.CALL), // TAIL CALL PROPOSAL

    Op_delegate(0x18, "delegate", -1, kind = Kind.EXCEPTION),
    Op_catch_all(0x19, "catch.all", -1, kind = Kind.EXCEPTION),

    // Parametric operators
    Op_drop(0x1a, "drop", 1, 0, kind = Kind.DROP),
    Op_select(0x1b, "select", 3, 1, kind = Kind.TEROP),

    // Variable access
    Op_local_get(0x20, "local.get", 0, 1, kind = Kind.LOCAL_GLOBAL),
    Op_local_set(0x21, "local.set", 1, 0, kind = Kind.LOCAL_GLOBAL),
    Op_local_tee(0x22, "local.tee", 1, 1, kind = Kind.LOCAL_GLOBAL),
    Op_global_get(0x23, "global.get", 0, 1, kind = Kind.LOCAL_GLOBAL),
    Op_global_set(0x24, "global.set", 1, 0, kind = Kind.LOCAL_GLOBAL),


    // Memory-related operators
    Op_i32_load(0x28, "i32.load", 1, 1, itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.MEMORY_LOAD),
    Op_i64_load(0x29, "i64.load", 1, 1, itypeOpt = WasmSType.I64, outType = WasmType.i64, kind = Kind.MEMORY_LOAD),
    Op_f32_load(0x2a, "f32.load", 1, 1, itypeOpt = WasmSType.F32, outType = WasmType.f32, kind = Kind.MEMORY_LOAD),
    Op_f64_load(0x2b, "f64.load", 1, 1, itypeOpt = WasmSType.F64, outType = WasmType.f64, kind = Kind.MEMORY_LOAD),

    Op_i32_load8_s(0x2c, "i32.load8_s", 1, 1, itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.MEMORY_LOAD),
    Op_i32_load8_u(0x2d, "i32.load8_u", 1, 1, itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.MEMORY_LOAD),
    Op_i32_load16_s(0x2e, "i32.load16_s", 1, 1, itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.MEMORY_LOAD),
    Op_i32_load16_u(0x2f, "i32.load16_u", 1, 1, itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.MEMORY_LOAD),

    Op_i64_load8_s(0x30, "i64.load8_s", 1, 1, itypeOpt = WasmSType.I64, outType = WasmType.i64, kind = Kind.MEMORY_LOAD),
    Op_i64_load8_u(0x31, "i64.load8_u", 1, 1, itypeOpt = WasmSType.I64, outType = WasmType.i64, kind = Kind.MEMORY_LOAD),
    Op_i64_load16_s(0x32, "i64.load16_s", 1, 1, itypeOpt = WasmSType.I64, outType = WasmType.i64, kind = Kind.MEMORY_LOAD),
    Op_i64_load16_u(0x33, "i64.load16_u", 1, 1, itypeOpt = WasmSType.I64, outType = WasmType.i64, kind = Kind.MEMORY_LOAD),
    Op_i64_load32_s(0x34, "i64.load32_s", 1, 1, itypeOpt = WasmSType.I64, outType = WasmType.i64, kind = Kind.MEMORY_LOAD),
    Op_i64_load32_u(0x35, "i64.load32_u", 1, 1, itypeOpt = WasmSType.I64, outType = WasmType.i64, kind = Kind.MEMORY_LOAD),

    Op_i32_store(0x36, "i32.store", 2, 0, itypeOpt = WasmSType.I32, kind = Kind.MEMORY_STORE),
    Op_i64_store(0x37, "i64.store", 2, 0, itypeOpt = WasmSType.I64, kind = Kind.MEMORY_STORE),
    Op_f32_store(0x38, "f32.store", 2, 0, itypeOpt = WasmSType.F32, kind = Kind.MEMORY_STORE),
    Op_f64_store(0x39, "f64.store", 2, 0, itypeOpt = WasmSType.F64, kind = Kind.MEMORY_STORE),
    Op_i32_store8(0x3a, "i32.store8", 2, 0, itypeOpt = WasmSType.I32, kind = Kind.MEMORY_STORE),
    Op_i32_store16(0x3b, "i32.store16", 2, 0, itypeOpt = WasmSType.I32, kind = Kind.MEMORY_STORE),
    Op_i64_store8(0x3c, "i64.store8", 2, 0, itypeOpt = WasmSType.I64, kind = Kind.MEMORY_STORE),
    Op_i64_store16(0x3d, "i64.store16", 2, 0, itypeOpt = WasmSType.I64, kind = Kind.MEMORY_STORE),
    Op_i64_store32(0x3e, "i64.store32", 2, 0, itypeOpt = WasmSType.I64, kind = Kind.MEMORY_STORE),

    Op_memory_size(0x3f, "memory.size", 0, 1, itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.MEMORY_OP),
    Op_memory_grow(0x40, "memory.grow", 1, 1, itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.MEMORY_OP),

    // Constants opcodes
    Op_i32_const(0x41, "i32.const", 0, 1, itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.LITERAL),
    Op_i64_const(0x42, "i64.const", 0, 1, itypeOpt = WasmSType.I64, outType = WasmType.i64, kind = Kind.LITERAL),
    Op_f32_const(0x43, "f32.const", 0, 1, itypeOpt = WasmSType.F32, outType = WasmType.f32, kind = Kind.LITERAL),
    Op_f64_const(0x44, "f64.const", 0, 1, itypeOpt = WasmSType.F64, outType = WasmType.f64, kind = Kind.LITERAL),

    // Comparison operators unop
    Op_i32_eqz(0x45, "i32.eqz", 1, 1, itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.UNOP),
    Op_i64_eqz(0x50, "i64.eqz", 1, 1, itypeOpt = WasmSType.I64, outType = WasmType.i32, kind = Kind.UNOP),

    // Comparison operators
    Op_i32_eq  (0x46, "i32.eq",   2, 1, "==", itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_i32_ne  (0x47, "i32.ne",   2, 1, "!=", itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_i32_lt_s(0x48, "i32.lt_s", 2, 1, "<s" , itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_i32_lt_u(0x49, "i32.lt_u", 2, 1, "<u" , itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_i32_gt_s(0x4a, "i32.gt_s", 2, 1, ">s" , itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_i32_gt_u(0x4b, "i32.gt_u", 2, 1, ">u" , itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_i32_le_s(0x4c, "i32.le_s", 2, 1, "<=s", itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_i32_le_u(0x4d, "i32.le_u", 2, 1, "<=u", itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_i32_ge_s(0x4e, "i32.ge_s", 2, 1, ">=s", itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_i32_ge_u(0x4f, "i32.ge_u", 2, 1, ">=u", itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP_COMP),

    Op_i64_eq(0x51, "i64.eq", 2, 1, "=="    , itypeOpt = WasmSType.I64, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_i64_ne(0x52, "i64.ne", 2, 1, "!="    , itypeOpt = WasmSType.I64, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_i64_lt_s(0x53, "i64.lt_s", 2, 1, "<s" , itypeOpt = WasmSType.I64, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_i64_lt_u(0x54, "i64.lt_u", 2, 1, "<u" , itypeOpt = WasmSType.I64, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_i64_gt_s(0x55, "i64.gt_s", 2, 1, ">s" , itypeOpt = WasmSType.I64, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_i64_gt_u(0x56, "i64.gt_u", 2, 1, ">u" , itypeOpt = WasmSType.I64, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_i64_le_s(0x57, "i64.le_s", 2, 1, "<=s", itypeOpt = WasmSType.I64, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_i64_le_u(0x58, "i64.le_u", 2, 1, "<=u", itypeOpt = WasmSType.I64, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_i64_ge_s(0x59, "i64.ge_s", 2, 1, ">=s", itypeOpt = WasmSType.I64, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_i64_ge_u(0x5a, "i64.ge_u", 2, 1, ">=u", itypeOpt = WasmSType.I64, outType = WasmType.i32, kind = Kind.BINOP_COMP),

    Op_f32_eq(0x5b, "f32.eq", 2, 1, "==", itypeOpt = WasmSType.F32, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_f32_ne(0x5c, "f32.ne", 2, 1, "!=", itypeOpt = WasmSType.F32, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_f32_lt(0x5d, "f32.lt", 2, 1, "<" , itypeOpt = WasmSType.F32, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_f32_gt(0x5e, "f32.gt", 2, 1, ">" , itypeOpt = WasmSType.F32, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_f32_le(0x5f, "f32.le", 2, 1, "<=", itypeOpt = WasmSType.F32, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_f32_ge(0x60, "f32.ge", 2, 1, ">=", itypeOpt = WasmSType.F32, outType = WasmType.i32, kind = Kind.BINOP_COMP),

    Op_f64_eq(0x61, "f64.eq", 2, 1, "==", itypeOpt = WasmSType.F64, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_f64_ne(0x62, "f64.ne", 2, 1, "!=", itypeOpt = WasmSType.F64, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_f64_lt(0x63, "f64.lt", 2, 1, "<" , itypeOpt = WasmSType.F64, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_f64_gt(0x64, "f64.gt", 2, 1, ">" , itypeOpt = WasmSType.F64, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_f64_le(0x65, "f64.le", 2, 1, "<=", itypeOpt = WasmSType.F64, outType = WasmType.i32, kind = Kind.BINOP_COMP),
    Op_f64_ge(0x66, "f64.ge", 2, 1, ">=", itypeOpt = WasmSType.F64, outType = WasmType.i32, kind = Kind.BINOP_COMP),


    // Numeric operators

    // int unary
    Op_i32_clz(0x67, "i32.clz", 1, 1, itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.UNOP),
    Op_i32_ctz(0x68, "i32.ctz", 1, 1, itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.UNOP),
    Op_i32_popcnt(0x69, "i32.popcnt", 1, 1, itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.UNOP),

    // int binary
    Op_i32_add(0x6a, "i32.add", 2, 1, "+", itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP),
    Op_i32_sub(0x6b, "i32.sub", 2, 1, "-", itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP),
    Op_i32_mul(0x6c, "i32.mul", 2, 1, "*", itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP),
    Op_i32_div_s(0x6d, "i32.div_s", 2, 1, "/", itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP),
    Op_i32_div_u(0x6e, "i32.div_u", 2, 1, "/", itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP),
    Op_i32_rem_s(0x6f, "i32.rem_s", 2, 1, "%", itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP),
    Op_i32_rem_u(0x70, "i32.rem_u", 2, 1, "%", itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP),
    Op_i32_and(0x71, "i32.and", 2, 1, "&", itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP),
    Op_i32_or(0x72, "i32.or", 2, 1, "|", itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP),
    Op_i32_xor(0x73, "i32.xor", 2, 1, "^", itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP),
    Op_i32_shl(0x74, "i32.shl", 2, 1, "<<", itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP),
    Op_i32_shr_s(0x75, "i32.shr_s", 2, 1, ">>", itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP),
    Op_i32_shr_u(0x76, "i32.shr_u", 2, 1, ">>>", itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP),
    Op_i32_rotl(0x77, "i32.rotl", 2, 1, itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP),
    Op_i32_rotr(0x78, "i32.rotr", 2, 1, itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.BINOP),

    // long unary
    Op_i64_clz(0x79, "i64.clz", 1, 1, itypeOpt = WasmSType.I64, outType = WasmType.i32, kind = Kind.UNOP),
    Op_i64_ctz(0x7a, "i64.ctz", 1, 1, itypeOpt = WasmSType.I64, outType = WasmType.i32, kind = Kind.UNOP),
    Op_i64_popcnt(0x7b, "i64.popcnt", 1, 1, itypeOpt = WasmSType.I64, outType = WasmType.i32, kind = Kind.UNOP),

    // long binary
    Op_i64_add(0x7c, "i64.add", 2, 1, itypeOpt = WasmSType.I64, outType = WasmType.i64, kind = Kind.BINOP),
    Op_i64_sub(0x7d, "i64.sub", 2, 1, itypeOpt = WasmSType.I64, outType = WasmType.i64, kind = Kind.BINOP),
    Op_i64_mul(0x7e, "i64.mul", 2, 1, itypeOpt = WasmSType.I64, outType = WasmType.i64, kind = Kind.BINOP),
    Op_i64_div_s(0x7f, "i64.div_s", 2, 1, itypeOpt = WasmSType.I64, outType = WasmType.i64, kind = Kind.BINOP),
    Op_i64_div_u(0x80, "i64.div_u", 2, 1, itypeOpt = WasmSType.I64, outType = WasmType.i64, kind = Kind.BINOP),
    Op_i64_rem_s(0x81, "i64.rem_s", 2, 1, itypeOpt = WasmSType.I64, outType = WasmType.i64, kind = Kind.BINOP),
    Op_i64_rem_u(0x82, "i64.rem_u", 2, 1, itypeOpt = WasmSType.I64, outType = WasmType.i64, kind = Kind.BINOP),
    Op_i64_and(0x83, "i64.and", 2, 1, itypeOpt = WasmSType.I64, outType = WasmType.i64, kind = Kind.BINOP),
    Op_i64_or(0x84, "i64.or", 2, 1, itypeOpt = WasmSType.I64, outType = WasmType.i64, kind = Kind.BINOP),
    Op_i64_xor(0x85, "i64.xor", 2, 1, itypeOpt = WasmSType.I64, outType = WasmType.i64, kind = Kind.BINOP),
    Op_i64_shl(0x86, "i64.shl", 2, 1, itypeOpt = WasmSType.I64, outType = WasmType.i64, kind = Kind.BINOP),
    Op_i64_shr_s(0x87, "i64.shr_s", 2, 1, itypeOpt = WasmSType.I64, outType = WasmType.i64, kind = Kind.BINOP),
    Op_i64_shr_u(0x88, "i64.shr_u", 2, 1, itypeOpt = WasmSType.I64, outType = WasmType.i64, kind = Kind.BINOP),
    Op_i64_rotl(0x89, "i64.rotl", 2, 1, itypeOpt = WasmSType.I64, outType = WasmType.i64, kind = Kind.BINOP),
    Op_i64_rotr(0x8a, "i64.rotr", 2, 1, itypeOpt = WasmSType.I64, outType = WasmType.i64, kind = Kind.BINOP),

    // float unary
    Op_f32_abs(0x8b, "f32.abs", 1, 1, itypeOpt = WasmSType.F32, outType = WasmType.f32, kind = Kind.UNOP),
    Op_f32_neg(0x8c, "f32.neg", 1, 1, itypeOpt = WasmSType.F32, outType = WasmType.f32, kind = Kind.UNOP),
    Op_f32_ceil(0x8d, "f32.ceil", 1, 1, itypeOpt = WasmSType.F32, outType = WasmType.f32, kind = Kind.UNOP),
    Op_f32_floor(0x8e, "f32.floor", 1, 1, itypeOpt = WasmSType.F32, outType = WasmType.f32, kind = Kind.UNOP),
    Op_f32_trunc(0x8f, "f32.trunc", 1, 1, itypeOpt = WasmSType.F32, outType = WasmType.f32, kind = Kind.UNOP),
    Op_f32_nearest(0x90, "f32.nearest", 1, 1, itypeOpt = WasmSType.F32, outType = WasmType.f32, kind = Kind.UNOP),
    Op_f32_sqrt(0x91, "f32.sqrt", 1, 1, itypeOpt = WasmSType.F32, outType = WasmType.f32, kind = Kind.UNOP),

    // float binary
    Op_f32_add(0x92, "f32.add", 2, 1, itypeOpt = WasmSType.F32, outType = WasmType.f32, kind = Kind.BINOP),
    Op_f32_sub(0x93, "f32.sub", 2, 1, itypeOpt = WasmSType.F32, outType = WasmType.f32, kind = Kind.BINOP),
    Op_f32_mul(0x94, "f32.mul", 2, 1, itypeOpt = WasmSType.F32, outType = WasmType.f32, kind = Kind.BINOP),
    Op_f32_div(0x95, "f32.div", 2, 1, itypeOpt = WasmSType.F32, outType = WasmType.f32, kind = Kind.BINOP),
    Op_f32_min(0x96, "f32.min", 2, 1, itypeOpt = WasmSType.F32, outType = WasmType.f32, kind = Kind.BINOP),
    Op_f32_max(0x97, "f32.max", 2, 1, itypeOpt = WasmSType.F32, outType = WasmType.f32, kind = Kind.BINOP),
    Op_f32_copysign(0x98, "f32.copysign", 2, 1, itypeOpt = WasmSType.F32, outType = WasmType.f32, kind = Kind.BINOP),

    // double unary
    Op_f64_abs(0x99, "f64.abs", 1, 1, itypeOpt = WasmSType.F64, outType = WasmType.f64, kind = Kind.UNOP),
    Op_f64_neg(0x9a, "f64.neg", 1, 1, itypeOpt = WasmSType.F64, outType = WasmType.f64, kind = Kind.UNOP),
    Op_f64_ceil(0x9b, "f64.ceil", 1, 1, itypeOpt = WasmSType.F64, outType = WasmType.f64, kind = Kind.UNOP),
    Op_f64_floor(0x9c, "f64.floor", 1, 1, itypeOpt = WasmSType.F64, outType = WasmType.f64, kind = Kind.UNOP),
    Op_f64_trunc(0x9d, "f64.trunc", 1, 1, itypeOpt = WasmSType.F64, outType = WasmType.f64, kind = Kind.UNOP),
    Op_f64_nearest(0x9e, "f64.nearest", 1, 1, itypeOpt = WasmSType.F64, outType = WasmType.f64, kind = Kind.UNOP),
    Op_f64_sqrt(0x9f, "f64.sqrt", 1, 1, itypeOpt = WasmSType.F64, outType = WasmType.f64, kind = Kind.UNOP),

    // double binary
    Op_f64_add(0xa0, "f64.add", 2, 1, itypeOpt = WasmSType.F64, outType = WasmType.f64, kind = Kind.BINOP),
    Op_f64_sub(0xa1, "f64.sub", 2, 1, itypeOpt = WasmSType.F64, outType = WasmType.f64, kind = Kind.BINOP),
    Op_f64_mul(0xa2, "f64.mul", 2, 1, itypeOpt = WasmSType.F64, outType = WasmType.f64, kind = Kind.BINOP),
    Op_f64_div(0xa3, "f64.div", 2, 1, itypeOpt = WasmSType.F64, outType = WasmType.f64, kind = Kind.BINOP),
    Op_f64_min(0xa4, "f64.min", 2, 1, itypeOpt = WasmSType.F64, outType = WasmType.f64, kind = Kind.BINOP),
    Op_f64_max(0xa5, "f64.max", 2, 1, itypeOpt = WasmSType.F64, outType = WasmType.f64, kind = Kind.BINOP),
    Op_f64_copysign(0xa6, "f64.copysign", 2, 1, itypeOpt = WasmSType.F64, outType = WasmType.f64, kind = Kind.BINOP),

    // Conversions
    Op_i32_wrap_i64(0xa7, "i32.wrap_i64", 1, 1, itypeOpt = WasmSType.I64, outType = WasmType.i32, kind = Kind.UNOP),
    Op_i32_trunc_f32_s(0xa8, "i32.trunc_f32_s", 1, 1, itypeOpt = WasmSType.F32, outType = WasmType.i32, kind = Kind.UNOP),
    Op_i32_trunc_f32_u(0xa9, "i32.trunc_f32_u", 1, 1, itypeOpt = WasmSType.F32, outType = WasmType.i32, kind = Kind.UNOP),
    Op_i32_trunc_f64_s(0xaa, "i32.trunc_f64_s", 1, 1, itypeOpt = WasmSType.F64, outType = WasmType.i32, kind = Kind.UNOP),
    Op_i32_trunc_f64_u(0xab, "i32.trunc_f64_u", 1, 1, itypeOpt = WasmSType.F64, outType = WasmType.i32, kind = Kind.UNOP),

    Op_i64_extend_i32_s(0xac, "i64.extend_i32_s", 1, 1, itypeOpt = WasmSType.I32, outType = WasmType.i64, kind = Kind.UNOP),
    Op_i64_extend_i32_u(0xad, "i64.extend_i32_u", 1, 1, itypeOpt = WasmSType.I32, outType = WasmType.i64, kind = Kind.UNOP),
    Op_i64_trunc_f32_s(0xae, "i64.trunc_f32_s", 1, 1, itypeOpt = WasmSType.F32, outType = WasmType.i64, kind = Kind.UNOP),
    Op_i64_trunc_f32_u(0xaf, "i64.trunc_f32_u", 1, 1, itypeOpt = WasmSType.F32, outType = WasmType.i64, kind = Kind.UNOP),
    Op_i64_trunc_f64_s(0xb0, "i64.trunc_f64_s", 1, 1, itypeOpt = WasmSType.F64, outType = WasmType.i64, kind = Kind.UNOP),
    Op_i64_trunc_f64_u(0xb1, "i64.trunc_f64_u", 1, 1, itypeOpt = WasmSType.F64, outType = WasmType.i64, kind = Kind.UNOP),

    Op_f32_convert_i32_s(0xb2, "f32.convert_i32_s", 1, 1, itypeOpt = WasmSType.I32, outType = WasmType.f32, kind = Kind.UNOP),
    Op_f32_convert_i32_u(0xb3, "f32.convert_i32_u", 1, 1, itypeOpt = WasmSType.I32, outType = WasmType.f32, kind = Kind.UNOP),
    Op_f32_convert_i64_s(0xb4, "f32.convert_i64_s", 1, 1, itypeOpt = WasmSType.I64, outType = WasmType.f32, kind = Kind.UNOP),
    Op_f32_convert_i64_u(0xb5, "f32.convert_i64_u", 1, 1, itypeOpt = WasmSType.I64, outType = WasmType.f32, kind = Kind.UNOP),
    Op_f32_demote_f64(0xb6, "f32.demote_f64", 1, 1, itypeOpt = WasmSType.F64, outType = WasmType.f32, kind = Kind.UNOP),
    Op_f64_convert_i32_s(0xb7, "f64.convert_i32_s", 1, 1, itypeOpt = WasmSType.I32, outType = WasmType.f64, kind = Kind.UNOP),
    Op_f64_convert_i32_u(0xb8, "f64.convert_i32_u", 1, 1, itypeOpt = WasmSType.I32, outType = WasmType.f64, kind = Kind.UNOP),
    Op_f64_convert_i64_s(0xb9, "f64.convert_i64_s", 1, 1, itypeOpt = WasmSType.I64, outType = WasmType.f64, kind = Kind.UNOP),
    Op_f64_convert_i64_u(0xba, "f64.convert_i64_u", 1, 1, itypeOpt = WasmSType.I64, outType = WasmType.f64, kind = Kind.UNOP),
    Op_f64_promote_f32(0xbb, "f64.promote_f32", 1, 1, itypeOpt = WasmSType.F32, outType = WasmType.f64, kind = Kind.UNOP),

    // Reinterpretations
    Op_i32_reinterpret_f32(0xbc, "i32.reinterpret_f32", 1, 1, itypeOpt = WasmSType.F32, outType = WasmType.i32, kind = Kind.UNOP),
    Op_i64_reinterpret_f64(0xbd, "i64.reinterpret_f64", 1, 1, itypeOpt = WasmSType.F64, outType = WasmType.i64, kind = Kind.UNOP),
    Op_f32_reinterpret_i32(0xbe, "f32.reinterpret_i32", 1, 1, itypeOpt = WasmSType.I32, outType = WasmType.f32, kind = Kind.UNOP),
    Op_f64_reinterpret_i64(0xbf, "f64.reinterpret_i64", 1, 1, itypeOpt = WasmSType.I64, outType = WasmType.f64, kind = Kind.UNOP),

    Op_i32_extend8_s(0xc0, "i32.extend8_s", 1, 1, itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.UNOP),
    Op_i32_extend16_s(0xc1, "i32.extend16_s", 1, 1, itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.UNOP),
    Op_i64_extend8_s(0xc2, "i64.extend8_s", 1, 1, itypeOpt = WasmSType.I32, outType = WasmType.i64, kind = Kind.UNOP),
    Op_i64_extend16_s(0xc3, "i64.extend16_s", 1, 1, itypeOpt = WasmSType.I32, outType = WasmType.i64, kind = Kind.UNOP),
    Op_i64_extend32_s(0xc4, "i64.extend32_s", 1, 1, itypeOpt = WasmSType.I32, outType = WasmType.i64, kind = Kind.UNOP),

    Op_ref_null(0xd0, "ref.null", 0, 1, itypeOpt = WasmSType.ANYREF, outType = WasmSType.ANYREF, kind = Kind.OTHER),
    Op_ref_is_null(0xd1, "ref.is_null", 1, 1, itypeOpt = WasmSType.ANYREF, outType = WasmSType.I32, kind = Kind.UNOP),

    // GC extensions: Multibyte instructions beginning with 0xFC.
    // https://github.com/WebAssembly/bulk-memory-operations/blob/dcaa1b6791401c29b67e8cd7929ec80949f1f849/proposals/bulk-memory-operations/Overview.md

    Op_i32_trunc_sat_f32_s(0xFC00, "i32.trunc_sat_f32_s", 1, 1, itypeOpt = WasmSType.F32, outType = WasmSType.I32, kind = Kind.OTHER),
    Op_i32_trunc_sat_f32_u(0xFC01, "i32.trunc_sat_f32_u", 1, 1, itypeOpt = WasmSType.F32, outType = WasmSType.I32, kind = Kind.OTHER),
    Op_i32_trunc_sat_f64_s(0xFC02, "i32.trunc_sat_f64_s", 1, 1, itypeOpt = WasmSType.F64, outType = WasmSType.I32, kind = Kind.OTHER),
    Op_i32_trunc_sat_f64_u(0xFC03, "i32.trunc_sat_f64_u", 1, 1, itypeOpt = WasmSType.F64, outType = WasmSType.I32, kind = Kind.OTHER),
    Op_i64_trunc_sat_f32_s(0xFC04, "i64.trunc_sat_f32_s", 1, 1, itypeOpt = WasmSType.F32, outType = WasmSType.I64, kind = Kind.OTHER),
    Op_i64_trunc_sat_f32_u(0xFC05, "i64.trunc_sat_f32_u", 1, 1, itypeOpt = WasmSType.F32, outType = WasmSType.I64, kind = Kind.OTHER),
    Op_i64_trunc_sat_f64_s(0xFC06, "i64.trunc_sat_f64_s", 1, 1, itypeOpt = WasmSType.F64, outType = WasmSType.I64, kind = Kind.OTHER),
    Op_i64_trunc_sat_f64_u(0xFC07, "i64.trunc_sat_f64_u", 1, 1, itypeOpt = WasmSType.F64, outType = WasmSType.I64, kind = Kind.OTHER),

    Op_memory_init(0xFC08, "memory.init", 3, 0, itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.OTHER),
    Op_data_drop(0xFC09, "data.drop", 0, 0, itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.OTHER),
    Op_memory_copy(0xFC0A, "memory.copy", 3, 0, itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.OTHER),
    Op_memory_fill(0xFC0B, "memory.fill", 3, 0, itypeOpt = WasmSType.I32, outType = WasmType.i32, kind = Kind.OTHER),

    Op_table_init(0xFC0C, "table.init", -1, -1, kind = Kind.OTHER),
    Op_elem_drop(0xFC0D, "elem.drop", -1, -1, kind = Kind.OTHER),
    Op_table_copy(0xFC0E, "table.copy", -1, -1, kind = Kind.OTHER),
    Op_table_grow(0xFC0F, "table.grow", -1, -1, kind = Kind.OTHER),
    Op_table_size(0xFC10, "table.size", -1, -1, kind = Kind.OTHER),
    Op_table_fill(0xFC11, "table.fill", -1, -1, kind = Kind.OTHER),
    ;

    val itype = itypeOpt ?: WasmSType.VOID

    enum class Kind(
        val memoryTransfer: Boolean = false
    ) {
        EXCEPTION,
        FLOW,
        LITERAL,
        TEROP,
        BINOP,
        BINOP_COMP,
        UNOP,
        DROP,
        MEMORY_LOAD(memoryTransfer = true),
        MEMORY_STORE(memoryTransfer = true),
        MEMORY_OP,
        LOCAL_GLOBAL,
        CALL,
        RESERVED,
        OTHER,
    }

    //val rname = name.removePrefix("Op_").replace('_', '.')

    init {
        val ename = "Op_" + sname.replace('.', '_').replace('/', '_')
        check(ename == name) { "'$ename' != '$name'"}
    }

    companion object {
        val OPS_BY_ID = values().associateBy { it.id }
        val OPS_BY_SNAME = values().associateBy { it.sname }
        operator fun get(index: Int): WasmOp = OPS_BY_ID[index] ?: invalidOp("Invalid OP ${index.hex}")
        operator fun get(name: String): WasmOp = OPS_BY_SNAME[name] ?: invalidOp("Invalid OP '$name'")
        fun getOrNull(id: Int): WasmOp? = OPS_BY_ID[id]
        fun getOrNull(name: String): WasmOp? = OPS_BY_SNAME[name]
        operator fun invoke(index: Int): WasmOp = this[index]
        operator fun invoke(name: String): WasmOp = this[name]
    }
}

sealed interface WasmInstruction {
    val op: WasmOp
    val itype: WasmSType get() = op.itype ?: WasmSType.VOID

    data object End : WasmInstruction {
        override val op: WasmOp = WasmOp.Op_end
    }

    data object unreachable : WasmInstruction {
        override val op: WasmOp = WasmOp.Op_unreachable
    }

    data object nop : WasmInstruction {
        override val op: WasmOp = WasmOp.Op_nop
    }

    sealed interface ControlStructureInstruction : WasmInstruction {
        val b: WasmType
    }

    sealed interface BlockOrLoop : ControlStructureInstruction {
        val expr: WasmExpr
    }

    data class block(override val b: WasmType, override val expr: WasmExpr) : BlockOrLoop {
        override val op: WasmOp = WasmOp.Op_block
    }

    data class loop(override val b: WasmType, override val expr: WasmExpr) : BlockOrLoop {
        override val op: WasmOp = WasmOp.Op_loop
    }

    data class IF(override val b: WasmType, val btrue: WasmExpr, val bfalse: WasmExpr?) : ControlStructureInstruction {
        var compOp: WasmOp? = null
        override val op: WasmOp = WasmOp.Op_if
    }

    data class ELSE(val code: WasmExpr) : WasmInstruction {
        override val op: WasmOp = WasmOp.Op_else
    }

    interface br_base : WasmInstruction {
        val label: Int
    }

    data class br(override val label: Int) : br_base {
        override val op: WasmOp = WasmOp.Op_br
    }

    data class br_if(override val label: Int) : br_base {
        var compOp: WasmOp? = null
        override val op: WasmOp = WasmOp.Op_br_if
    }

    //data class br_if_with_op(val compOp: WasmOp, val label: Int) : WasmInstruction {
    //    override val op: WasmOp = WasmOp.Op_br_if
    //}

    data class br_table constructor(val labels: List<Int>, val default: Int) : WasmInstruction {
        override val op: WasmOp = WasmOp.Op_br_table
    }

    data object RETURN : WasmInstruction {
        override val op: WasmOp = WasmOp.Op_return
    }

    data class INVOKE(val name: String) : WasmInstruction {
        override val op: WasmOp = WasmOp.Op_call
    }

    data class CALL(val funcIdx: Int) : WasmInstruction {
        override val op: WasmOp = WasmOp.Op_call
    }

    data class CALL_INDIRECT(val typeIdx: Int, val zero: Int) : WasmInstruction {
        override val op: WasmOp = WasmOp.Op_call_indirect
    }

    data class Ins(override val op: WasmOp) : WasmInstruction
    data class InsInt(override val op: WasmOp, val param: Int) : WasmInstruction
    data class InsType(override val op: WasmOp, val param: WasmType) : WasmInstruction

    sealed interface InsConst : WasmInstruction {
        val type: WasmSType
    }

    data class InsConstInt(val value: Int, override val op: WasmOp = WasmOp.Op_i32_const) : InsConst {
        override val type get() = WasmSType.I32
    }
    data class InsConstLong(val value: Long, override val op: WasmOp = WasmOp.Op_i64_const) : InsConst {
        override val type get() = WasmSType.I64
    }
    data class InsConstFloat(val value: Float, override val op: WasmOp = WasmOp.Op_f32_const) : InsConst {
        override val type get() = WasmSType.F32
    }
    data class InsConstDouble(val value: Double, override val op: WasmOp = WasmOp.Op_f64_const) : InsConst {
        override val type get() = WasmSType.F64
    }

    data class InsMemarg(override val op: WasmOp, val align: Int, val offset: Int) : WasmInstruction
}

interface WasmFuncResolver {
    fun resolveFunc(name: String): WasmFunc
}

data class FuncWithType(val name: String, val func: WasmType.Function)

interface WasmFuncRef {
    val name: String
    val func: WasmFunc
}

data class WasmFuncName(override val name: String, val resolve: (name: String) -> WasmFunc) : WasmFuncRef {
    override val func get() = resolve(name)
}

data class WasmFuncWithType(val name: String, val type: WasmType.Function)

data class WasmFunc(
    val index: Int,
    val type: WasmType.Function,
    var code: WasmCode? = null,
    var fimport: WasmImport? = null,
    //var export: WasmExport? = null,
    //var code2: WasmCode2? = null,
    val name2: String? = null
) : WasmFuncRef {
    val exports = arrayListOf<WasmExport>()

    fun addExport(export: WasmExport?) {
        export?.let { this.exports += it }
    }

    companion object {
        fun anonymousFunc(type: WasmType.Function, expr: WasmExpr): WasmFunc {
            return WasmFunc(-1, type, code = WasmCode(
                params = type.args,
                locals = emptyList(),
                body = expr
            ))
        }
    }

    var importFunc: (WasmRuntime.(Array<Any?>) -> Any?)? = null

    override fun toString(): String = "WasmFunc[$index](type=$type, import=$fimport, code=$code)"

    //fun getAst(wasm: WasmModule): Wast.Stm? = when {
    //    code != null -> {
    //        val body = code!!.body
    //        val bodyAst = body.toAst(wasm, func)
    //        bodyAst
    //    }
    //    code2 != null -> code2!!.body
    //    else -> null
    //}

    override val func = this
    val rlocals: List<WastLocal> by lazy { type.args + (code?.flatLocals ?: listOf()) }

    val exportName: String? get() = exports.firstOrNull()?.name

    override val name: String get() = name2 ?: fimport?.name ?: exportName ?: "f$index"
    val rname: String get() = fimport?.name ?: exportName ?: "f$index"

    val ftype: FuncWithType by lazy { FuncWithType(name, type) }
    val fwt = WasmFuncWithType(name, type)
}

sealed interface WasmAssert
data class WasmAssertReturn(val actual: WasmExpr, val expect: WasmExpr?, val msg: String) : WasmAssert

data class WastLocal(val name: String, val type: WasmType, val index: Int = -1) {
    val stype = type.toWasmSType()
    constructor(index: Int, type: WasmType) : this("l$index", type, index)

    override fun toString(): String = "$name: $type"
}


data class WasmGlobal(
    val globalType: WasmType,
    val index: Int = -1,
    val expr: WasmExpr? = null,
    //val ast: Wast.Stm? = null,
    var gimport: WasmImport? = null,
    val name: String = gimport?.name ?: "g$index"
) {
    var globalOffset = -1
    //val astGlobal = AstGlobal(name, globalType.type)
    //val name get() = import?.name ?: "g$index"
}

data class WasmData(
    val memindex: Int,
    val data: ByteArray,
    val index: Int,
    val e: WasmExpr? = null,
    //val ast: Wast.Expr? = null,
) {
    //fun toAst(module: WasmModule): Wast.Stm = when {
    //    e != null -> e.toAst(module, WasmFunc(-1, WasmReader.INT_FUNC_TYPE))
    //    ast != null -> Wast.RETURN(ast)
    //    else -> TODO()
    //}
}

class WasmCode constructor(val params: List<WastLocal>?, val locals: List<List<WastLocal>>, val body: WasmExpr) {
    var interpreterCode: WasmInterpreterCode? = null
    val flatLocals get() = locals.flatMap { it }
}

//class WasmCode2(val locals: List<WastLocal>, val body: Wast.Stm) {
//}

data class WasmImport(val moduleName: String, val name: String, val indexSpace: Int, val index: Int, val type: Any) {
    val importPair = Pair(moduleName, name)
}
class WasmExport constructor(val name: String, val tid: Int, val idx: Int, val obj: Any?) {
    val tidName get() = when (tid) {
        0 -> "function"
        1 -> "table"
        2 -> "memory"
        3 -> "global"
        else -> "$tid"
    }
    override fun toString(): String = "Export(name='$name', tid=$tid:$tidName, idx=$idx, kind=${(obj ?: Unit)::class.simpleName})"
}

data class WasmElement(
    val tableIdx: Int,
    val funcIdxs: List<Int>? = null,
    val funcNames: List<String>? = null,
    val expr: WasmExpr? = null,
    //val exprAst: Wast.Expr? = null
) {
    val size: Int get() = funcIdxs?.size ?: funcNames?.size ?: 0
    val funcRefs: List<Any> = (funcIdxs ?: funcNames)!!

    fun getFunctions(module: WasmModule): List<WasmFunc> {
        return (0 until size).map { get(module, it) }
    }

    fun get(module: WasmModule, index: Int): WasmFunc {
        funcNames?.get(index)?.let {
            return module.functionsByName[it] ?: error("Can't find function '$it'")
        }
        funcIdxs?.get(index)?.let {
            return module.functions[it]
        }
        TODO("Can't find function at index=$index")
    }
}

class WasmModule constructor(
    val functions: List<WasmFunc>,
    val datas: List<WasmData>,
    val types: List<NamedWasmType>,
    val globals: List<WasmGlobal>,
    val elements: List<WasmElement>,
    val tables: List<WasmType.TableType<*>>,
    val memories: List<WasmType.Limit>,
    val exports: List<WasmExport>,
    val startFunc: Int = -1,
    val asserts: List<WasmAssert> = emptyList(),
) {
    init {
        var offset = 0
        for (global in globals) {
            global.globalOffset = offset
            offset += global.globalType.toWasmSType().nbytes
        }
    }
    val exportsByName = exports.associateBy { it.name }
    //val functionsByName = functions.filter { it.export != null }.associateBy { it.export!!.name }
    val functionsByName = exports.associate { it.name to functions[it.idx] }
    val globalsByIndex = globals.associateBy { it.index }
    fun getFunction(item: Int): WasmFunc = functions[item]
    fun getFunction(item: String): WasmFunc = functionsByName[item] ?: error("Can't find function $item")
    fun getFunction(item: Any): WasmFunc {
        return when (item) {
            is Int -> getFunction(item)
            is String -> getFunction(item)
            else -> TODO("getFunction($item)")
        }
    }
}


enum class WasmSType(override val id: Int, override val signature: String, val nbytes: Int) : WasmType {
    VOID(0, "v", nbytes = 0),
    I32(1, "i", nbytes = 4),
    I64(2, "l", nbytes = 8),
    F32(3, "f", nbytes = 4),
    F64(4, "d", nbytes = 8),
    V128(5, "8", nbytes = 16),
    ANYREF(6, "R", nbytes = 4),
    FUNCREF(7, "r", nbytes = 4);
}

fun WasmType.toWasmSType(): WasmSType {
    return when (this) {
        is WasmSType -> this
        is WasmType.Mutable -> this.rtype.toWasmSType()
        else -> TODO("$this")
    }
}

class CustomWasmType(val name: String) : WasmType {
    override val id: Int = -1
    override val signature: String = "?"
    override fun toString() = name
}

data class NamedWasmType(val index: Int, val name: String, val type: WasmType)

sealed interface WasmType {
    val id: Int
    val signature: String

    class _ARRAY(val element: WasmType) : WasmType {
        override val id = -1
        override fun toString() = "$element[]"
        override val signature: String = "[${element.signature}"
    }

    class _VARARG(val element: WasmType) : WasmType {
        override val id = -1
        override fun toString() = "$element..."
        override val signature: String = "[${element.signature}"
    }

    class _NULLABLE(val element: WasmType) : WasmType {
        override val id = -1
        override fun toString() = "$element"
        override val signature: String = "${element.signature}"
    }

    data object _boolean : WasmType {
        override val id = -1
        override fun toString() = "boolean"
        override val signature: String = "z"
    }

    data object _i8 : WasmType {
        override val id = -1
        override fun toString() = "i8"
        override val signature: String = "b"
    }

    data object _i16 : WasmType {
        override val id = -1
        override fun toString() = "i16"
        override val signature: String = "s"
    }

    data object v128 : WasmType {
        override val id = 0
        override fun toString() = "Vector128"
        override val signature: String = "LVector128;"
    }

    data class Limit(var min: Int = 0, var max: Int? = null) : WasmType {
        override val id = -1
        override val signature: String = "l$min$max"
    }

    data class Mutable(val rtype: WasmType) : WasmType {
        override val id = -1
        override val signature: String = "mut $rtype"
    }

    data class Function(val args: List<WastLocal>, val rets: List<WasmType>) : WasmType {
        override val id = -1

        var cachedJDescriptor: String? = null

        val argsByName by lazy { args.associateBy { it.name } }

        init {
            check(rets.size <= 1) { "Multiple return values are not supported" }
        }

        val retType get() = rets.firstOrNull() ?: WasmType.void
        val retTypeVoid get() = retType == WasmType.void
        val argsPlusRet: List<WasmType> get() = args.map { it.type } + listOf(retType)
        override val signature: String get() = argsPlusRet.joinToString("") { it.signature }

        fun withoutArgNames(): Function = Function(
            args = args.withIndex().map { WastLocal(it.index, it.value.type) },
            rets = rets
        )

        override fun toString(): String = "(${args.joinToString(", ")}): $retType"
    }

    //data class Global(val type: WasmType, val mutable: Boolean) : WasmType {
    //    override val id = -1
    //    override val signature: String = "g${type.signature}${if (mutable) "m" else "i"}"
    //}

    data class TableType<T : Any> constructor(var limit: Limit, val clazz: KClass<T>)

    companion object {
        val void = WasmSType.VOID
        val i32 = WasmSType.I32
        val i64 = WasmSType.I64
        val f32 = WasmSType.F32
        val f64 = WasmSType.F64

        operator fun invoke(str: String): WasmType {
            return when (str) {
                "i32" -> i32
                "i64" -> i64
                "f32" -> f32
                "f64" -> f64
                else -> TODO("Unsupported WasmType '$str'")
            }
        }
    }
}
