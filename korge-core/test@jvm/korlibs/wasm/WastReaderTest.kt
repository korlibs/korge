package korlibs.wasm

import korlibs.io.async.*
import korlibs.io.file.std.*
import java.io.*
import kotlin.test.*

//class WastReaderTestInterpreter : WastReaderTest() {
//    override fun runModule(module: WasmModule, codeTrace: Boolean) {
//        WasmRunInterpreter(module).initGlobals().invoke("run\$asserts")
//    }
//}

class WastReaderTestJit : WastReaderTest() {
    override fun runModule(module: WasmModule, codeTrace: Boolean) {
        WasmRunJVMOutputExt.build(module, codeTrace = codeTrace).invoke("run\$asserts")
    }
}

// https://webassembly.github.io/wabt/demo/wat2wasm/
open class WastReaderTest {
    @Ignore @Test fun testAddress() = runAssetsWast("wasm/test-core/address.wast")
    @Ignore @Test fun testAlign() = runAssetsWast("wasm/test-core/align.wast")
    @Test fun testBinary_leb128() = runAssetsWast("wasm/test-core/binary-leb128.wast")
    @Test fun testBinary() = runAssetsWast("wasm/test-core/binary.wast")
    @Ignore @Test fun testBlock() = runAssetsWast("wasm/test-core/block.wast")
    @Ignore @Test fun testBr() = runAssetsWast("wasm/test-core/br.wast", codeTrace = false)
    @Ignore @Test fun testBr0() = runAssetsWast("wasm/test-core/br0.wast", codeTrace = true)
    @Ignore @Test fun testBr_if() = runAssetsWast("wasm/test-core/br_if.wast")
    @Ignore @Test fun testBr_table() = runAssetsWast("wasm/test-core/br_table.wast")
    @Ignore @Test fun testBulk() = runAssetsWast("wasm/test-core/bulk.wast")
    @Ignore @Test fun testCall() = runAssetsWast("wasm/test-core/call.wast")
    @Ignore @Test fun testCall_indirect() = runAssetsWast("wasm/test-core/call_indirect.wast")
    @Ignore @Test fun testComments() = runAssetsWast("wasm/test-core/comments.wast")
    @Ignore @Test fun testConst() = runAssetsWast("wasm/test-core/const.wast")
    @Ignore @Test fun testConversions() = runAssetsWast("wasm/test-core/conversions.wast")
    @Ignore @Test fun testCustom() = runAssetsWast("wasm/test-core/custom.wast")
    @Ignore @Test fun testData() = runAssetsWast("wasm/test-core/data.wast")
    @Ignore @Test fun testElem() = runAssetsWast("wasm/test-core/elem.wast")
    @Ignore @Test fun testEndianness() = runAssetsWast("wasm/test-core/endianness.wast")
    @Ignore @Test fun testExports() = runAssetsWast("wasm/test-core/exports.wast")
    @Ignore @Test fun testF32() = runAssetsWast("wasm/test-core/f32.wast")
    @Ignore @Test fun testF32_bitwise() = runAssetsWast("wasm/test-core/f32_bitwise.wast")
    @Ignore @Test fun testF32_cmp() = runAssetsWast("wasm/test-core/f32_cmp.wast")
    @Ignore @Test fun testF64() = runAssetsWast("wasm/test-core/f64.wast")
    @Ignore @Test fun testF64_bitwise() = runAssetsWast("wasm/test-core/f64_bitwise.wast")
    @Ignore @Test fun testF64_cmp() = runAssetsWast("wasm/test-core/f64_cmp.wast")
    @Ignore @Test fun testFac() = runAssetsWast("wasm/test-core/fac.wast")
    @Ignore @Test fun testFloat_exprs() = runAssetsWast("wasm/test-core/float_exprs.wast")
    @Ignore @Test fun testFloat_literals() = runAssetsWast("wasm/test-core/float_literals.wast")
    @Ignore @Test fun testFloat_memory() = runAssetsWast("wasm/test-core/float_memory.wast")
    @Ignore @Test fun testFloat_misc() = runAssetsWast("wasm/test-core/float_misc.wast")
    @Ignore @Test fun testForward() = runAssetsWast("wasm/test-core/forward.wast")
    @Ignore @Test fun testFunc() = runAssetsWast("wasm/test-core/func.wast")
    @Ignore @Test fun testFunc_ptrs() = runAssetsWast("wasm/test-core/func_ptrs.wast")
    @Ignore @Test fun testGlobal() = runAssetsWast("wasm/test-core/global.wast")
    @Test fun testI32() = runAssetsWast("wasm/test-core/i32.wast")
    @Test fun testI64() = runAssetsWast("wasm/test-core/i64.wast")
    @Ignore @Test fun testIf() = runAssetsWast("wasm/test-core/if.wast")
    @Ignore @Test fun testImports() = runAssetsWast("wasm/test-core/imports.wast")
    @Ignore @Test fun testInline_module() = runAssetsWast("wasm/test-core/inline-module.wast")
    @Ignore @Test fun testInt_exprs() = runAssetsWast("wasm/test-core/int_exprs.wast")
    @Ignore @Test fun testInt_literals() = runAssetsWast("wasm/test-core/int_literals.wast")
    @Ignore @Test fun testLabels() = runAssetsWast("wasm/test-core/labels.wast")
    @Ignore @Test fun testLeft_to_right() = runAssetsWast("wasm/test-core/left-to-right.wast")
    @Ignore @Test fun testLinking() = runAssetsWast("wasm/test-core/linking.wast")
    @Ignore @Test fun testLoad() = runAssetsWast("wasm/test-core/load.wast")
    @Ignore @Test fun testLocal_get() = runAssetsWast("wasm/test-core/local_get.wast")
    @Ignore @Test fun testLocal_get0() = runAssetsWast("wasm/test-core/local_get0.wast", codeTrace = true)
    @Ignore @Test fun testLocal_get1() = runAssetsWast("wasm/test-core/local_get1.wast", codeTrace = true)
    @Ignore @Test fun testLocal_set() = runAssetsWast("wasm/test-core/local_set.wast")
    @Ignore @Test fun testLocal_tee() = runAssetsWast("wasm/test-core/local_tee.wast")
    @Ignore @Test fun testLocal_tee0() = runAssetsWast("wasm/test-core/local_tee0.wast", codeTrace = false)
    @Ignore @Test fun testLoop() = runAssetsWast("wasm/test-core/loop.wast")
    @Ignore @Test fun testMemory() = runAssetsWast("wasm/test-core/memory.wast")
    @Ignore @Test fun testMemory_copy() = runAssetsWast("wasm/test-core/memory_copy.wast")
    @Ignore @Test fun testMemory_fill() = runAssetsWast("wasm/test-core/memory_fill.wast")
    @Ignore @Test fun testMemory_grow() = runAssetsWast("wasm/test-core/memory_grow.wast")
    @Ignore @Test fun testMemory_init() = runAssetsWast("wasm/test-core/memory_init.wast")
    @Ignore @Test fun testMemory_redundancy() = runAssetsWast("wasm/test-core/memory_redundancy.wast")
    @Ignore @Test fun testMemory_size() = runAssetsWast("wasm/test-core/memory_size.wast")
    @Ignore @Test fun testMemory_trap() = runAssetsWast("wasm/test-core/memory_trap.wast")
    @Ignore @Test fun testNames() = runAssetsWast("wasm/test-core/names.wast")
    @Ignore @Test fun testNop() = runAssetsWast("wasm/test-core/nop.wast")
    @Ignore @Test fun testRef_func() = runAssetsWast("wasm/test-core/ref_func.wast")
    @Ignore @Test fun testRef_is_null() = runAssetsWast("wasm/test-core/ref_is_null.wast")
    @Ignore @Test fun testRef_null() = runAssetsWast("wasm/test-core/ref_null.wast")
    @Ignore @Test fun testReturn() = runAssetsWast("wasm/test-core/return.wast")
    @Ignore @Test fun testSelect() = runAssetsWast("wasm/test-core/select.wast")
    @Ignore @Test fun testSimd_address() = runAssetsWast("wasm/test-core/simd/simd_address.wast")
    @Ignore @Test fun testSimd_align() = runAssetsWast("wasm/test-core/simd/simd_align.wast")
    @Ignore @Test fun testSimd_bit_shift() = runAssetsWast("wasm/test-core/simd/simd_bit_shift.wast")
    @Ignore @Test fun testSimd_bitwise() = runAssetsWast("wasm/test-core/simd/simd_bitwise.wast")
    @Ignore @Test fun testSimd_boolean() = runAssetsWast("wasm/test-core/simd/simd_boolean.wast")
    @Ignore @Test fun testSimd_const() = runAssetsWast("wasm/test-core/simd/simd_const.wast")
    @Ignore @Test fun testSimd_conversions() = runAssetsWast("wasm/test-core/simd/simd_conversions.wast")
    @Ignore @Test fun testSimd_f32x4() = runAssetsWast("wasm/test-core/simd/simd_f32x4.wast")
    @Ignore @Test fun testSimd_f32x4_arith() = runAssetsWast("wasm/test-core/simd/simd_f32x4_arith.wast")
    @Ignore @Test fun testSimd_f32x4_cmp() = runAssetsWast("wasm/test-core/simd/simd_f32x4_cmp.wast")
    @Ignore @Test fun testSimd_f32x4_pmin_pmax() = runAssetsWast("wasm/test-core/simd/simd_f32x4_pmin_pmax.wast")
    @Ignore @Test fun testSimd_f32x4_rounding() = runAssetsWast("wasm/test-core/simd/simd_f32x4_rounding.wast")
    @Ignore @Test fun testSimd_f64x2() = runAssetsWast("wasm/test-core/simd/simd_f64x2.wast")
    @Ignore @Test fun testSimd_f64x2_arith() = runAssetsWast("wasm/test-core/simd/simd_f64x2_arith.wast")
    @Ignore @Test fun testSimd_f64x2_cmp() = runAssetsWast("wasm/test-core/simd/simd_f64x2_cmp.wast")
    @Ignore @Test fun testSimd_f64x2_pmin_pmax() = runAssetsWast("wasm/test-core/simd/simd_f64x2_pmin_pmax.wast")
    @Ignore @Test fun testSimd_f64x2_rounding() = runAssetsWast("wasm/test-core/simd/simd_f64x2_rounding.wast")
    @Ignore @Test fun testSimd_i16x8_arith() = runAssetsWast("wasm/test-core/simd/simd_i16x8_arith.wast")
    @Ignore @Test fun testSimd_i16x8_arith2() = runAssetsWast("wasm/test-core/simd/simd_i16x8_arith2.wast")
    @Ignore @Test fun testSimd_i16x8_cmp() = runAssetsWast("wasm/test-core/simd/simd_i16x8_cmp.wast")
    @Ignore @Test fun testSimd_i16x8_extadd_pairwise_i8x16() = runAssetsWast("wasm/test-core/simd/simd_i16x8_extadd_pairwise_i8x16.wast")
    @Ignore @Test fun testSimd_i16x8_extmul_i8x16() = runAssetsWast("wasm/test-core/simd/simd_i16x8_extmul_i8x16.wast")
    @Ignore @Test fun testSimd_i16x8_q15mulr_sat_s() = runAssetsWast("wasm/test-core/simd/simd_i16x8_q15mulr_sat_s.wast")
    @Ignore @Test fun testSimd_i16x8_sat_arith() = runAssetsWast("wasm/test-core/simd/simd_i16x8_sat_arith.wast")
    @Ignore @Test fun testSimd_i32x4_arith() = runAssetsWast("wasm/test-core/simd/simd_i32x4_arith.wast")
    @Ignore @Test fun testSimd_i32x4_arith2() = runAssetsWast("wasm/test-core/simd/simd_i32x4_arith2.wast")
    @Ignore @Test fun testSimd_i32x4_cmp() = runAssetsWast("wasm/test-core/simd/simd_i32x4_cmp.wast")
    @Ignore @Test fun testSimd_i32x4_dot_i16x8() = runAssetsWast("wasm/test-core/simd/simd_i32x4_dot_i16x8.wast")
    @Ignore @Test fun testSimd_i32x4_extadd_pairwise_i16x8() = runAssetsWast("wasm/test-core/simd/simd_i32x4_extadd_pairwise_i16x8.wast")
    @Ignore @Test fun testSimd_i32x4_extmul_i16x8() = runAssetsWast("wasm/test-core/simd/simd_i32x4_extmul_i16x8.wast")
    @Ignore @Test fun testSimd_i32x4_trunc_sat_f32x4() = runAssetsWast("wasm/test-core/simd/simd_i32x4_trunc_sat_f32x4.wast")
    @Ignore @Test fun testSimd_i32x4_trunc_sat_f64x2() = runAssetsWast("wasm/test-core/simd/simd_i32x4_trunc_sat_f64x2.wast")
    @Ignore @Test fun testSimd_i64x2_arith() = runAssetsWast("wasm/test-core/simd/simd_i64x2_arith.wast")
    @Ignore @Test fun testSimd_i64x2_arith2() = runAssetsWast("wasm/test-core/simd/simd_i64x2_arith2.wast")
    @Ignore @Test fun testSimd_i64x2_cmp() = runAssetsWast("wasm/test-core/simd/simd_i64x2_cmp.wast")
    @Ignore @Test fun testSimd_i64x2_extmul_i32x4() = runAssetsWast("wasm/test-core/simd/simd_i64x2_extmul_i32x4.wast")
    @Ignore @Test fun testSimd_i8x16_arith() = runAssetsWast("wasm/test-core/simd/simd_i8x16_arith.wast")
    @Ignore @Test fun testSimd_i8x16_arith2() = runAssetsWast("wasm/test-core/simd/simd_i8x16_arith2.wast")
    @Ignore @Test fun testSimd_i8x16_cmp() = runAssetsWast("wasm/test-core/simd/simd_i8x16_cmp.wast")
    @Ignore @Test fun testSimd_i8x16_sat_arith() = runAssetsWast("wasm/test-core/simd/simd_i8x16_sat_arith.wast")
    @Ignore @Test fun testSimd_int_to_int_extend() = runAssetsWast("wasm/test-core/simd/simd_int_to_int_extend.wast")
    @Ignore @Test fun testSimd_lane() = runAssetsWast("wasm/test-core/simd/simd_lane.wast")
    @Ignore @Test fun testSimd_linking() = runAssetsWast("wasm/test-core/simd/simd_linking.wast")
    @Ignore @Test fun testSimd_load() = runAssetsWast("wasm/test-core/simd/simd_load.wast")
    @Ignore @Test fun testSimd_load16_lane() = runAssetsWast("wasm/test-core/simd/simd_load16_lane.wast")
    @Ignore @Test fun testSimd_load32_lane() = runAssetsWast("wasm/test-core/simd/simd_load32_lane.wast")
    @Ignore @Test fun testSimd_load64_lane() = runAssetsWast("wasm/test-core/simd/simd_load64_lane.wast")
    @Ignore @Test fun testSimd_load8_lane() = runAssetsWast("wasm/test-core/simd/simd_load8_lane.wast")
    @Ignore @Test fun testSimd_load_extend() = runAssetsWast("wasm/test-core/simd/simd_load_extend.wast")
    @Ignore @Test fun testSimd_load_splat() = runAssetsWast("wasm/test-core/simd/simd_load_splat.wast")
    @Ignore @Test fun testSimd_load_zero() = runAssetsWast("wasm/test-core/simd/simd_load_zero.wast")
    @Ignore @Test fun testSimd_splat() = runAssetsWast("wasm/test-core/simd/simd_splat.wast")
    @Ignore @Test fun testSimd_store() = runAssetsWast("wasm/test-core/simd/simd_store.wast")
    @Ignore @Test fun testSimd_store16_lane() = runAssetsWast("wasm/test-core/simd/simd_store16_lane.wast")
    @Ignore @Test fun testSimd_store32_lane() = runAssetsWast("wasm/test-core/simd/simd_store32_lane.wast")
    @Ignore @Test fun testSimd_store64_lane() = runAssetsWast("wasm/test-core/simd/simd_store64_lane.wast")
    @Ignore @Test fun testSimd_store8_lane() = runAssetsWast("wasm/test-core/simd/simd_store8_lane.wast")
    @Ignore @Test fun testSkip_stack_guard_page() = runAssetsWast("wasm/test-core/skip-stack-guard-page.wast")
    @Ignore @Test fun testStack() = runAssetsWast("wasm/test-core/stack.wast")
    @Ignore @Test fun testStart() = runAssetsWast("wasm/test-core/start.wast")
    @Ignore @Test fun testStore() = runAssetsWast("wasm/test-core/store.wast")
    @Ignore @Test fun testSwitch() = runAssetsWast("wasm/test-core/switch.wast")
    @Test fun testTable_sub() = runAssetsWast("wasm/test-core/table-sub.wast")
    @Ignore @Test fun testTable() = runAssetsWast("wasm/test-core/table.wast")
    @Ignore @Test fun testTable_copy() = runAssetsWast("wasm/test-core/table_copy.wast")
    @Ignore @Test fun testTable_fill() = runAssetsWast("wasm/test-core/table_fill.wast")
    @Ignore @Test fun testTable_get() = runAssetsWast("wasm/test-core/table_get.wast")
    @Ignore @Test fun testTable_grow() = runAssetsWast("wasm/test-core/table_grow.wast")
    @Ignore @Test fun testTable_init() = runAssetsWast("wasm/test-core/table_init.wast")
    @Ignore @Test fun testTable_set() = runAssetsWast("wasm/test-core/table_set.wast")
    @Ignore @Test fun testTable_size() = runAssetsWast("wasm/test-core/table_size.wast")
    @Test fun testToken() = runAssetsWast("wasm/test-core/token.wast")
    @Ignore @Test fun testTokens() = runAssetsWast("wasm/test-core/tokens.wast")
    @Test fun testTraps() = runAssetsWast("wasm/test-core/traps.wast")
    @Ignore @Test fun testType() = runAssetsWast("wasm/test-core/type.wast")
    @Ignore @Test fun testUnreachable() = runAssetsWast("wasm/test-core/unreachable.wast")
    @Test fun testUnreached_invalid() = runAssetsWast("wasm/test-core/unreached-invalid.wast")
    @Test fun testUnreached_valid() = runAssetsWast("wasm/test-core/unreached-valid.wast")
    @Ignore @Test fun testUnwind() = runAssetsWast("wasm/test-core/unwind.wast")
    @Test fun testUtf8_custom_section_id() = runAssetsWast("wasm/test-core/utf8-custom-section-id.wast")
    @Test fun testUtf8_import_field() = runAssetsWast("wasm/test-core/utf8-import-field.wast")
    @Test fun testUtf8_import_module() = runAssetsWast("wasm/test-core/utf8-import-module.wast")
    @Test fun testUtf8_invalid_encoding() = runAssetsWast("wasm/test-core/utf8-invalid-encoding.wast")

    @Ignore @Test fun testSimple() {
        runAssetsWastCode("""
            (module
                (func (export "8u_bad") (param ${'$'}i i32)
                    (drop (i32.load8_u offset=4294967295 (local.get ${'$'}i)))
                )
            )
            (assert_return (invoke "8u_bad" (i32.const 0)) (i32.const 0))
            ;;(assert_trap (invoke "8u_bad" (i32.const 0)) "out of bounds memory access")
        """.trimIndent(), codeTrace = true)
    }

    fun runAssetsWast(file: String, codeTrace: Boolean = false) = suspendTest { runAssetsWastCode(resourcesVfs[file].readString(), codeTrace) }
    fun runAssetsWastCode(
        wastCode: String,
        codeTrace: Boolean = false,
    ) {
        val modules = WasmReaderText().readTopLevel(wastCode).modules
        for (module in modules) {
            runModule(module.buildModule(), codeTrace = codeTrace)
        }
    }

    open fun runModule(module: WasmModule, codeTrace: Boolean) {
        //WasmRunInterpreter(module).initGlobals().runAsserts()
        WasmRunInterpreter(module).initGlobals().runAsserts()
        //WasmRunJVMJIT.build(module, codeTrace = codeTrace).invoke("run\$asserts")
    }

    @Test
    //@Ignore
    fun genNames() {
        for (file in File("src/test/resources/test-core").walkTopDown().sorted()) {
            if (file.name.endsWith(".wast")) {
                val capital = file.nameWithoutExtension.replace("-", "_").capitalize()
                //println(file)
                println("@Test fun test$capital() = runAssetsWast(\"${file.path.removePrefix("src/test/resources/")}\")")
            }
        }
    }
}
