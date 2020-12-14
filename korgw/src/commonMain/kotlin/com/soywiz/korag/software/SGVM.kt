@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korag.software

import com.soywiz.kmem.*
import kotlin.math.*

// Software Graphics Virtual Machine
// @TODO: Port dynarek for even faster performance https://github.com/kpspemu/kpspemu/tree/master/dynarek2/
class SGVM(
    var program: IntArray,
    var flits: FloatArray = FloatArray(0)
) {
    val freg = FloatArray(128)

    fun clone() = SGVM(program, flits)

    var tex2d: (sampler: Int, x: Float, y: Float, out: FloatArray, outIndex: Int) -> Unit = { sampler: Int, x: Float, y: Float, out: FloatArray, outIndex: Int ->
        println("tex2d: sampler=$sampler, x=$x, y=$y, outIndex=$outIndex")
    }

    fun execute(pc: Int = 0): SGVM {
        var cpc = pc
        while (cpc < program.size) {
            val i = SGVMInstruction(program[cpc++])
            if (i.OP == SGVMOpcode.END) break
            i.interpret()
        }
        return this
    }

    private fun SGVMInstruction.interpret() {
        val op = OP
        when (op) {
            SGVMOpcode.END -> Unit
            SGVMOpcode.FZERO -> fset { 0f }
            SGVMOpcode.FONE -> fset { 1f }
            SGVMOpcode.FLIT -> fset { flits[SRC_L + it] }
            SGVMOpcode.FSET -> fset { fsrc(it) }
            SGVMOpcode.FNEG -> fset { -fsrc(it) }
            SGVMOpcode.FRCP -> fset { 1f / fsrc(it) }
            SGVMOpcode.FSQR -> fset { sqrt(fsrc(it)) }
            SGVMOpcode.FABS -> fset { fsrc(it).absoluteValue }
            SGVMOpcode.FADD -> fset { fsrc(it) + fsrc2(it) }
            SGVMOpcode.FSUB -> fset { fsrc(it) - fsrc2(it) }
            SGVMOpcode.FMUL -> fset { fsrc(it) * fsrc2(it) }
            SGVMOpcode.FDIV -> fset { fsrc(it) / fsrc2(it) }
            SGVMOpcode.FREM -> fset { fsrc(it) % fsrc2(it) }
            SGVMOpcode.FMAX -> fset { max(fsrc(it), fsrc2(it)) }
            SGVMOpcode.FMIN -> fset { min(fsrc(it), fsrc2(it)) }
            SGVMOpcode.TEX2D -> tex2d(SRC, fsrc2(0), fsrc2(1), freg, DST)
            else -> TODO()
        }
    }

    private inline fun SGVMInstruction.fset(block: (Int) -> Float) {
        val dst = DST
        for (n in 0 until EXT) freg[dst + n] = block(n)
    }
    private fun SGVMInstruction.fsrc(n: Int) = freg[SRC + n]
    private fun SGVMInstruction.fsrc2(n: Int) = freg[SRC2 + n]
}

// Up to 128 opcodes (7 bits)
object SGVMOpcode {
    const val END = 0   // Finishes the program
    const val FZERO = 1 // Set to zeros
    const val FONE = 2  // Set to ones
    const val FLIT = 3  // Load Literal
    const val FSET = 4  // Copy register
    const val FNEG = 5  // Negate
    const val FABS = 6  // Absolute Value
    const val FRCP = 7  // Reciprocal
    const val FSQR = 8  // Root Square
    const val FADD = 9  // Addition
    const val FSUB = 10 // Subtraction
    const val FMUL = 11 // Multiplication
    const val FDIV = 12 // Division
    const val FREM = 13 // Remaining
    const val FMAX = 14 // Maximum
    const val FMIN = 15 // Minimum
    const val TEX2D = 100 // texture2D
}

inline class SGVMInstruction(val value: Int) {
    companion object {
        fun op(op: Int, ext: Int = 1, dst: Int = 0, src: Int = 0, src2: Int = 0): SGVMInstruction = SGVMInstruction(0
            .insert(op, 0, 7)
            .insert(dst, 7, 7)
            .insert(src, 14, 7)
            .insert(src2, 21, 7)
            .insert(ext, 28, 4)
        )
        fun opl(op: Int, ext: Int = 1, dst: Int = 0, srcL: Int = 0): SGVMInstruction = SGVMInstruction(0
            .insert(op, 0, 7)
            .insert(dst, 7, 7)
            .insert(srcL, 14, 14)
            .insert(ext, 28, 4)
        )
    }

    val OP get() = value.extract(0, 7)
    val DST get() = value.extract(7, 7)

    val SRC get() = value.extract(14, 7)
    val SRC2 get() = value.extract(21, 7)
    val EXT get() = value.extract(28, 4)

    val SRC_L get() = value.extract(14, 14)
}
