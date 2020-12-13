package com.soywiz.korag.software

import com.soywiz.kds.*
import com.soywiz.korag.shader.*
import com.soywiz.korio.lang.*

class ShaderToSGVM {
    val program = IntArrayList()
    val flits = FloatArrayList()

    fun toProgram() = SGVM(program.toIntArray(), flits.toFloatArray())

    var currentIndex: Int = 0
    data class Allocation(val index: Int, val type: VarType)

    val allocatedVariable = LinkedHashMap<Variable, Allocation>()

    fun getAllocation(variable: Variable): Allocation {
        // @TODO: This won't work for matrices that require 16 registers
        return allocatedVariable.getOrPut(variable) { Allocation(currentIndex, variable.type).also { currentIndex += 4 } }
    }

    fun swizzleIndex(c: Char) = when (c) {
        'r', 'x' -> 0
        'g', 'y' -> 1
        'b', 'z' -> 2
        'a', 'w' -> 3
        else -> TODO("Unknown swizzle index '$c'")
    }

    val ftemps = Pool { getAllocation(Temp(it, VarType.Float4)) }

    fun handle(shader: Shader): ShaderToSGVM {
        handle(shader.stm)
        return this
    }

    fun handle(stm: Program.Stm) {
        when (stm) {
            is Program.Stm.Stms -> for (s in stm.stms) handle(s)
            is Program.Stm.Set -> {
                val to = stm.to
                val from = stm.from
                when (to) {
                    is Variable -> {
                        val allocation = getAllocation(to)
                        //allocation.index
                        //println("allocation=$allocation, to=${to}")
                        handle(from, allocation)
                    }
                    is Program.Swizzle -> {
                        val toleft = to.left
                        val swizzle = to.swizzle
                        if (toleft !is Variable) TODO("[c] $stm")
                        if (swizzle.length != 1) TODO("[d] $stm")
                        val allocation = getAllocation(toleft)
                        val allocationIndexed = allocation.copy(
                            index = allocation.index + swizzleIndex(swizzle[0]),
                            type = allocation.type.withElementCount(swizzle.length)
                        )
                        handle(from, allocationIndexed)
                    }
                    else -> TODO("[b] $stm")
                }
            }
            else -> TODO("[a] $stm")
        }
    }

    fun handle(op: Operand, dest: Allocation) {
        when (op) {
            is Program.FloatLiteral -> {
                assert(dest.type.elementCount == 1)
                val flitsIndex = flits.size
                flits.add(op.value)
                program.add(SGVMInstruction.opl(SGVMOpcode.FLIT, 1, dest.index, flitsIndex).value)
            }
            else -> TODO("[a] $op - dest=$dest")
        }
    }
}
