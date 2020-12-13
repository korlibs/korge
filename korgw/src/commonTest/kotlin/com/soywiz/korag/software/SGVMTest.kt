package com.soywiz.korag.software

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
}
