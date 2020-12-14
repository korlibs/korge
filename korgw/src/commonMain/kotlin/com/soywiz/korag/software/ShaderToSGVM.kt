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
    data class Allocation(val indices: List<Int>, val type: VarType) {
        val elementCount get() = indices.size
        init {
            assert(indices.size == type.elementCount)
        }
        val indicesAreCorrelative by lazy {
            (0 until elementCount).all { indices[it] == indices[0] + it }
        }

        fun extract(n: Int, count: Int) = Allocation(indices.subList(n, n + count), type.withElementCount(count))
    }

    val allocatedVariable = LinkedHashMap<Variable, Allocation>()

    fun getAllocation(variable: Variable): Allocation {
        // @TODO: This won't work for matrices that require 16 registers
        return allocatedVariable
            .getOrPut(variable) {
                Allocation(
                    (0 until variable.elementCount).map { currentIndex + it },
                    variable.type
                ).also { currentIndex += variable.elementCount }
            }
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
                        val allocation = getAllocation(toleft)
                        // allocation.indices.map { swizzleIndex(swizzle[it]) }
                        val allocationIndexed = allocation.copy(
                            indices = (swizzle.indices).map { allocation.indices[swizzleIndex(swizzle[it])] },
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

    fun addOp(opcode: Int, elementCount: Int, dest: Allocation, srcL: Allocation, srcR: Allocation? = null) {
        if (dest.indicesAreCorrelative && srcL.indicesAreCorrelative && srcR?.indicesAreCorrelative != false) {
            addInstruction(SGVMInstruction.op(opcode, elementCount, dest.indices[0], srcL.indices[0], srcR?.indices?.get(0) ?: 0))
        } else {
            for (n in 0 until elementCount) {
                addInstruction(
                    SGVMInstruction.op(
                        opcode,
                        1,
                        dest.indices[n],
                        srcL.indices[n],
                        srcR?.indices?.get(n) ?: 0
                    )
                )
            }
        }
    }

    fun handle(op: Operand, dest: Allocation) {
        when (op) {
            is Program.FloatLiteral -> {
                assert(dest.type.elementCount == 1)
                val flitsIndex = addFloatLit(op.value)
                addInstruction(SGVMInstruction.opl(SGVMOpcode.FLIT, 1, dest.indices[0], flitsIndex))
            }
            is Program.Binop -> {
                val left = op.left
                val right = op.right
                val operation = op.op
                assert(left.elementCount == op.elementCount)
                val elementCount = left.elementCount
                // @TODO: Using temps shouldn't be necessary in some cases
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
                        addOp(opcode, elementCount, dest, tempL, tempR)
                    }
                }
            }
            is Program.Swizzle -> {
                val swizzle = op.swizzle
                assert(op.left is Variable)
                val left = getAllocation(op.left as Variable)
                // @TODO: This SET shouldn't be necessary
                for (n in swizzle.indices) {
                    addInstruction(SGVMInstruction.op(SGVMOpcode.FSET, 1, dest.indices[n], left.indices[swizzleIndex(swizzle[n])]))
                }
            }
            is Program.Vector -> {
                assert(dest.elementCount == op.ops.sumBy { it.elementCount })
                var n = 0
                for (rop in op.ops) {
                    handle(rop, dest.extract(n, rop.elementCount))
                    n += rop.elementCount
                }
            }
            is Program.Func -> {
                val params = arrayListOf<Allocation>()
                val ops = op.ops
                for (n in ops.indices) params.add(ftemps[ops[n].elementCount].alloc())
                try {
                    run {
                        for (n in ops.indices) {
                            handle(ops[n], params[n])
                        }
                        when (op.name) {
                            "abs" -> {
                                assert(params.size == 1)
                                val l = params[0]
                                addOp(SGVMOpcode.FABS, l.elementCount, dest, l)
                            }
                            "max", "min" -> {
                                assert(params.size == 2)
                                val l = params[0]
                                val r = params[1]
                                assert(l.elementCount == r.elementCount)
                                addOp(when (op.name) {
                                    "max" -> SGVMOpcode.FMAX
                                    "min" -> SGVMOpcode.FMIN
                                    else -> TODO()
                                 }, params[0].elementCount, dest, l, r)
                            }
                            else -> TODO("[c] $op - dest=$dest")
                        }
                    }
                } finally {
                    for (n in ops.indices) ftemps[ops[n].elementCount].free(params[n])
                }
            }
            else -> TODO("[a] $op - dest=$dest")
        }
    }
}
