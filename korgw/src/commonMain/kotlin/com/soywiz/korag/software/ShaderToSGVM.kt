package com.soywiz.korag.software

import com.soywiz.kds.*
import com.soywiz.korag.shader.*
import com.soywiz.korio.lang.*

class ShaderAllocator(
    var currentIndex: Int = 0,
    val allocatedNames: MutableMap<String, Allocation> = LinkedHashMap(),
    val allocatedVariable: MutableMap<Variable, Allocation> = LinkedHashMap(),
) {
    fun clone() = ShaderAllocator(currentIndex, LinkedHashMap(allocatedNames), LinkedHashMap(allocatedVariable))

    data class VaryingInfo(val variable: Variable, val allocation: Allocation)

    val varyings by lazy { allocatedVariable.filter { it.key is Varying }.map { VaryingInfo(it.key, it.value) } }
    val output by lazy { varyings.first { it.variable is Output } }

    val varyingIndices by lazy {
        varyings.flatMap {
            it.allocation.index until (it.allocation.index + it.allocation.elementCount)
        }.toIntArray()
    }

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

    data class Allocation(val index: Int, val type: VarType) {
        val elementCount get() = type.elementCount
        fun extract(n: Int, count: Int) = Allocation(index + n, type.withElementCount(count))
    }

    fun allocate(shader: Shader, filter: (Variable) -> Boolean = { true }) {
        object : Program.Visitor<Unit>(Unit) {
            override fun visit(operand: Variable) {
                if (filter(operand)) getAllocation(operand)
            }
        }.visit(shader.stm)
    }

    fun allocateOutput(shader: Shader) = allocate(shader) { it is Output }
    fun allocateVarying(shader: Shader) = allocate(shader) { it is Varying }
    fun allocateUniform(shader: Shader) = allocate(shader) { it is Uniform }
    fun allocateVaryingUniform(shader: Shader) = allocate(shader) { it is Varying || it is Uniform }
    fun allocateVaryingUniform(program: Program) {
        allocateVarying(program.vertex)
        allocateVarying(program.fragment)
        allocateUniform(program.vertex)
        allocateUniform(program.fragment)
    }
}

fun Shader.toSGVM(allocator: ShaderAllocator) = ShaderToSGVM(allocator).handle(this).toProgram()

class ShaderToSGVM(allocator: ShaderAllocator) {
    val allocator: ShaderAllocator = allocator.clone()
    var program = SGVMProgram()

    fun toProgram() = SGVM(program, allocator)

    fun getAllocation(variable: Variable): ShaderAllocator.Allocation = allocator.getAllocation(variable)

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
    inline fun <T> allocateFTemp(type: VarType, block: (allocation: ShaderAllocator.Allocation) -> T): T {
        return ftemps[type.elementCount].alloc { block(it) }
    }

    fun handle(shader: Shader): ShaderToSGVM {
        handle(shader.stm)
        return this
    }

    class Label() {
        val patches = IntArrayList()
        var address = 0
    }

    fun putLabel(label: Label) {
        label.address = programAddress
    }

    fun patchLabel(label: Label) {
        for (patchAddress in label.patches) {
            program.data[patchAddress] = label.address
        }
    }

    val programAddress get() = program.data.size

    fun addJumpIf(kind: Int, temp: ShaderAllocator.Allocation, label: Label) {
        addOp(kind, 1, temp, temp)
        label.patches.add(programAddress)
        program.data.add(0)
    }

    fun addJump(label: Label) {
        addOp(SGVMOpcode.JUMP, 1, null, null)
        label.patches.add(program.data.size)
        program.data.add(0)
    }

    fun handle(stm: Program.Stm) {
        when (stm) {
            is Program.Stm.Stms -> for (s in stm.stms) handle(s)
            is Program.Stm.Discard -> {
                addOp(SGVMOpcode.DISCARD, 0, null, null)
            }
            is Program.Stm.If -> {
                allocateFTemp(VarType.Float1) { temp ->
                    val tbody = stm.tbody
                    val fbody = stm.fbody
                    val endLabel = Label()
                    val falseLabel = Label()

                    // Condition
                    handle(stm.cond, temp)
                    addJumpIf(SGVMOpcode.JUMP_FALSE, temp, falseLabel)
                    //addJumpIf(SGVMOpcode.JUMP_TRUE, temp, falseLabel)
                    handle(tbody)
                    putLabel(falseLabel)
                    if (fbody != null) {
                        addJump(endLabel)
                        handle(fbody)
                    }
                    putLabel(endLabel)
                    patchLabel(falseLabel)
                    patchLabel(endLabel)
                }
            }
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

    fun addOp(opcode: Int, elementCount: Int, dest: ShaderAllocator.Allocation?, srcL: ShaderAllocator.Allocation?, srcR: ShaderAllocator.Allocation? = null) {
        program.op(opcode, elementCount, dest?.index ?: 0, srcL?.index ?: 0, srcR?.index ?: 0)
    }

    fun handle(op: Operand, dest: ShaderAllocator.Allocation) {
        when (op) {
            is Program.FloatLiteral -> {
                Assert.eq(dest.type.elementCount, 1)
                program.flit(dest.index, op.value)
            }
            is Program.Unop -> {
                handle(op.toBinop(), dest)
            }
            is Program.Binop -> {
                val left = op.left
                val right = op.right
                val ltype = left.type
                val rtype = right.type
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
                            "+" -> {
                                if (ltype.elementCount != rtype.elementCount) TODO()
                                SGVMOpcode.FADD
                            }
                            "-" -> {
                                if (ltype.elementCount != rtype.elementCount) TODO()
                                SGVMOpcode.FSUB
                            }
                            "*" -> {
                                when {
                                    ltype == VarType.Mat4 && rtype == VarType.Mat4 -> SGVMOpcode.MMMUL
                                    ltype == VarType.Mat4 && rtype == VarType.Float4 -> SGVMOpcode.MFMUL
                                    rtype == VarType.Float1 -> SGVMOpcode.SMUL
                                    else -> {
                                        if (ltype.elementCount != rtype.elementCount) TODO("${ltype.elementCount} != ${rtype.elementCount}")
                                        SGVMOpcode.FMUL
                                    }
                                }
                            }
                            "/" -> {
                                if (rtype.elementCount == 1) {
                                    SGVMOpcode.SMUL
                                } else {
                                    if (ltype.elementCount != rtype.elementCount) TODO("OP[/] : ${ltype.elementCount} != ${rtype.elementCount}")
                                    SGVMOpcode.FDIV
                                }
                            }
                            "%" -> {
                                if (ltype.elementCount != rtype.elementCount) TODO()
                                SGVMOpcode.FREM
                            }
                            "<" -> {
                                if (ltype.elementCount != rtype.elementCount) TODO()
                                SGVMOpcode.FCOMP_LT
                            }
                            "<=" -> {
                                if (ltype.elementCount != rtype.elementCount) TODO("OP[<=] : ${ltype.elementCount} != ${rtype.elementCount}")
                                SGVMOpcode.FCOMP_LE
                            }
                            ">" -> {
                                if (ltype.elementCount != rtype.elementCount) TODO()
                                SGVMOpcode.FCOMP_GT
                            }
                            ">=" -> {
                                if (ltype.elementCount != rtype.elementCount) TODO()
                                SGVMOpcode.FCOMP_GE
                            }
                            "==" -> {
                                if (ltype.elementCount != rtype.elementCount) TODO()
                                SGVMOpcode.FCOMP_EQ
                            }
                            "!=" -> {
                                if (ltype.elementCount != rtype.elementCount) TODO()
                                SGVMOpcode.FCOMP_NE
                            }
                            else -> TODO("[b] $op - dest=$dest")
                        }
                        //println("OP: $opcode, elementCount=$elementCount, dest=$dest, tempL=$tempL, tempR=$tempR")
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
                val params = arrayListOf<ShaderAllocator.Allocation>()
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
                                program.op(SGVMOpcode.TEX2D, 1, dest.index, l.index, r.index)
                                /*
                                ftemps[dest.elementCount].alloc { destTemp ->
                                    program.op(SGVMOpcode.TEX2D, 1, destTemp.index, l.index, r.index)
                                    addOp(SGVMOpcode.FSET, dest.elementCount, dest, destTemp)
                                }
                                 */
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
