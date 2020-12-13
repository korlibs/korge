package com.soywiz.korag.software

import com.soywiz.kds.*
import com.soywiz.korag.shader.*
import com.soywiz.korio.lang.*

class ShaderToSGVM {
    val program = IntArrayList()
    val flits = FloatArrayList()

    fun addInstruction(i: SGVMInstruction) {
        program.add(i.value)
    }

    fun addFloatLit(value: Float): Int {
        return flits.size.also { flits.add(value) }
    }

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

    val ftemps = Array(4) { size -> Pool { getAllocation(Temp(it, VarType.FLOAT(size))) } }

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
                val flitsIndex = addFloatLit(op.value)
                addInstruction(SGVMInstruction.opl(SGVMOpcode.FLIT, 1, dest.index, flitsIndex))
            }
            is Program.Binop -> {
                val left = op.left
                val right = op.right
                val operation = op.op
                assert(left.elementCount == op.elementCount)
                val elementCount = left.elementCount
                ftemps[elementCount].alloc { tempL ->
                    ftemps[elementCount].alloc { tempR ->
                        handle(left, tempL)
                        handle(right, tempR)
                        val opcode = when (operation) {
                            "+" -> SGVMOpcode.FADD
                            "-" -> SGVMOpcode.FSUB
                            "*" -> SGVMOpcode.FMUL
                            "/" -> SGVMOpcode.FDIV
                            "%" -> SGVMOpcode.FREM
                            else -> TODO("[b] $op - dest=$dest")
                        }
                        addInstruction(SGVMInstruction.op(opcode, elementCount, dest.index, tempL.index, tempR.index))
                    }
                }
            }
            else -> TODO("[a] $op - dest=$dest")
        }
    }
}
