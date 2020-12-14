package com.soywiz.korag.software

import com.soywiz.kds.iterators.*
import com.soywiz.korag.shader.*
import com.soywiz.korio.lang.*
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

        /*
        run {
            val TOTAL_ITERS = 200_000_000
            val concurrency = CONCURRENCY_COUNT
            //val concurrency = 1
            val vms = (0 until concurrency).map { vm.clone() }
            val itersPerVm = TOTAL_ITERS / vms.size
            parallelForeach(vms.size) {
                val vm = vms[it]
                println(currentThreadId)
                for (n in 0 until itersPerVm) vm.execute()
            }
        }
        */

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
            SET(out["z"], -(1f.lit))
            SET(out["w"], -(out["x"]))
        }
        assertEquals(2f, vm.freg[0])
        assertEquals(1f, vm.freg[1])
        assertEquals(-1f, vm.freg[2])
        assertEquals(-2f, vm.freg[3])
    }

    @Test
    fun test3() {
        val vm = executeShader {
            SET(out["xy"], vec2(1f.lit, 2f.lit))
        }
        assertEquals(1f, vm.freg[0])
        assertEquals(2f, vm.freg[1])
    }

    @Test
    fun test4() {
        val vm = executeShader {
            SET(out["zw"], vec2(1f.lit, 2f.lit))
            SET(out["xy"], out["zw"])
        }
        assertEquals(1f, vm.freg[0])
        assertEquals(2f, vm.freg[1])
    }

    @Test
    fun test5() {
        val vm = executeShader {
            //SET(out["zw"], vec2(1f.lit, 2f.lit))
            SET(out["x"], min(1f.lit, 2f.lit))
            SET(out["y"], max(1f.lit, 2f.lit))
            SET(out["zw"], abs(vec2(3f.lit, (-4f).lit)))
        }
        assertEquals(1f, vm.freg[0])
        assertEquals(2f, vm.freg[1])
        assertEquals(3f, vm.freg[2])
        assertEquals(4f, vm.freg[3])
    }

    fun executeShader(callback: Program.Builder.() -> Unit): SGVM {
        val s2vm = ShaderToSGVM().handle(VertexShader(callback))
        val vm = s2vm.toProgram()
        return vm.execute()
    }
}
