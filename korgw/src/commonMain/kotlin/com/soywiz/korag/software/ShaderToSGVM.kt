package com.soywiz.korag.software

import com.soywiz.kds.*
import com.soywiz.korag.shader.*
import com.soywiz.korio.lang.*

class ShaderToSGVM {
    val program = SGVMProgram()

    fun toProgram() = SGVM(program)

    var currentIndex: Int = 0
    data class Allocation(val index: Int, val type: VarType) {
        val elementCount get() = type.elementCount
        fun extract(n: Int, count: Int) = Allocation(index + n, type.withElementCount(count))
    }

    val allocatedNames = LinkedHashMap<String, Allocation>()
    val allocatedVariable = LinkedHashMap<Variable, Allocation>()

    fun getAllocation(variable: Variable): Allocation {
        // @TODO: This won't work for matrices that require 16 registers
        return allocatedVariable
            .getOrPut(variable) {
                Allocation(
                    currentIndex,
                    variable.type
                ).also {
                    currentIndex += variable.elementCount
                    allocatedNames[variable.name] = it
                }
            }
    }

    fun swizzleIndex(c: Char) = when (c) {
        'r', 'x' -> 0
        'g', 'y' -> 1
        'b', 'z' -> 2
        'a', 'w' -> 3
        else -> TODO("Unknown swizzle index '$c'")
    }

    val ftemps = Array(17) { size ->
        Pool {
            val type = when (size) {
                in 0..4 -> VarType.FLOAT(size)
                9 -> VarType.Mat3
                16 -> VarType.Mat4
                else -> VarType.TVOID
            }
            getAllocation(Temp(it, type))
        }
    }
    inline fun <T> allocateFTemp(type: VarType, block: (allocation: Allocation) -> T): T {
        return ftemps[type.elementCount].alloc { block(it) }
    }

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
                        getAllocation(toleft) // so out is registered first
                        allocateFTemp(toleft.type.withElementCount(swizzle.length)) { temp ->
                            handle(from, temp)
                            program.setToSwizzle(getAllocation(toleft).index, swizzle.map { swizzleIndex(it) }.toIntArray(), temp.index)
                        }
                    }
                    else -> TODO("[b] $stm")
                }
            }
            else -> TODO("[a] $stm")
        }
    }

    fun addOp(opcode: Int, elementCount: Int, dest: Allocation, srcL: Allocation, srcR: Allocation? = null) {
        program.op(opcode, elementCount, dest.index, srcL.index, srcR?.index ?: 0)
    }

    fun handle(op: Operand, dest: Allocation) {
        when (op) {
            is Program.FloatLiteral -> {
                Assert.eq(dest.type.elementCount, 1)
                program.flit(dest.index, op.value)
            }
            is Program.Binop -> {
                val left = op.left
                val right = op.right
                val operation = op.op
                assert(left.elementCount == op.elementCount)
                val elementCount = left.elementCount
                val rightElementCount = right.elementCount
                // @TODO: Using temps shouldn't be necessary in some cases
                ftemps[elementCount].alloc { tempL ->
                    ftemps[rightElementCount].alloc { tempR ->
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
                program.setFromSwizzle(dest.index, swizzle.map { swizzleIndex(it) }.toIntArray(), left.index)
            }
            is Program.Vector -> {
                val destCount = dest.elementCount
                val sumCount = op.ops.sumBy { it.elementCount }
                if (destCount != sumCount) error("destCount=$destCount, sumCount=$sumCount, counts=${op.ops.map { it.elementCount }}")
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
                            "texture2D" -> {
                                assert(params.size == 2)
                                val l = params[0]
                                val r = params[1]
                                assert(l.elementCount == 1)
                                assert(r.elementCount == 2)
                                ftemps[dest.elementCount].alloc { destTemp ->
                                    program.op(SGVMOpcode.TEX2D, 1, destTemp.index, l.index, r.index)
                                    addOp(SGVMOpcode.FSET, op.elementCount, dest, destTemp)
                                }
                            }
                            else -> TODO("[c] $op - dest=$dest")
                        }
                    }
                } finally {
                    for (n in ops.indices) ftemps[ops[n].elementCount].free(params[n])
                }
            }
            is Variable -> {
                assert(op.elementCount == dest.elementCount)
                addOp(SGVMOpcode.FSET, op.elementCount, dest, getAllocation(op))
            }
            else -> TODO("[a] $op - dest=$dest")
        }
    }
}
