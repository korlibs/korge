package com.soywiz.korag.software

import com.soywiz.korag.shader.*
import kotlin.test.*

class SGVMTest {
    @Test
    fun test() {
        val vm = SGVM(
            intArrayOf(
                SGVMInstruction.op(SGVMOpcode.FONE, 3, 0).value,
                SGVMInstruction.op(SGVMOpcode.FADD, 3, 4, 0, 0).value,
                SGVMInstruction.op(SGVMOpcode.END).value
            )
        ).execute()
        assertEquals(1f, vm.freg[0])
        assertEquals(1f, vm.freg[1])
        assertEquals(1f, vm.freg[2])
        assertEquals(0f, vm.freg[3])
        assertEquals(2f, vm.freg[4])
        assertEquals(2f, vm.freg[5])
        assertEquals(2f, vm.freg[6])
        assertEquals(0f, vm.freg[7])
    }

    @Test
    fun test2() {
        val vm = executeShader {
            SET(out["x"], 2f.lit)
            SET(out["y"], 1f.lit)
        }
        assertEquals(2f, vm.freg[0])
        assertEquals(1f, vm.freg[1])
    }

    fun executeShader(callback: Program.Builder.() -> Unit): SGVM {
        val s2vm = ShaderToSGVM().handle(VertexShader(callback))
        val vm = s2vm.toProgram()
        return vm.execute()
    }
}
