@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korag.software

import com.soywiz.kds.*
import com.soywiz.kgl.internal.*
import com.soywiz.kmem.*
import com.soywiz.korio.lang.*
import kotlin.math.*

// Software Graphics Virtual Machine
// @TODO: Port dynarek for even faster performance https://github.com/kpspemu/kpspemu/tree/master/dynarek2/
class SGVM(
    var program: SGVMProgram
) {
    val freg = FloatArray(128)

    fun copyFrom(other: SGVM) {
        this.program = other.program
        this.tex2d = other.tex2d
    }

    fun clone() = SGVM(program).also {
        it.copyFrom(this)
    }

    var tex2d: (sampler: Int, x: Float, y: Float, out: FloatArray, outIndex: Int) -> Unit = { sampler: Int, x: Float, y: Float, out: FloatArray, outIndex: Int ->
        println("tex2d: sampler=$sampler, x=$x, y=$y, outIndex=$outIndex")
    }

    var cpc = 0
    fun execute(pc: Int = 0): SGVM {
        cpc = pc
        val programData = program.rdata
        while (cpc < programData.size) {
            val i = SGVMInstruction(readPC())
            if (i.OP == SGVMOpcode.END) break
            i.interpret()
        }
        return this
    }

    fun readPC() = program.rdata[cpc++]

    fun setF(n: Int, value: Float) {
        //println("freg[$n] = $value")
        freg[n] = value
    }

    private fun SGVMInstruction.interpret() {
        val op = OP
        when (op) {
            SGVMOpcode.END -> Unit
            SGVMOpcode.FZERO -> fset { 0f }
            SGVMOpcode.FONE -> fset { 1f }
            SGVMOpcode.FLIT -> fset { Float.fromBits(readPC()) }
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
            SGVMOpcode.FTSW -> {
                //println("FTSW[$EXT2]")
                for (n in 0 until EXT2) {
                    //println("n[$n] = ${SWIZZLE(n)}")
                    setF(DST + SWIZZLE(n), fsrc(n))
                }
            }
            SGVMOpcode.FFSW -> {
                //println("FFSW[$EXT2]")
                for (n in 0 until EXT2) setF(DST + n, fsrc(SWIZZLE(n)))
            }
            else -> TODO("$op")
        }
    }

    private inline fun SGVMInstruction.fset(block: (Int) -> Float) {
        val dst = DST
        for (n in 0 until EXT) setF(dst + n, block(n))
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
    const val FFSW = 20  // Copy from swizzled
    const val FTSW = 21  // Copy to swizzled
    const val TEX2D = 100 // texture2D
}

class SGVMProgram(val data: IntArrayList = IntArrayList()) {
    val rdata by lazy { data.toIntArray() }

    companion object {
        operator fun invoke(block: SGVMProgram.() -> Unit): SGVMProgram {
            return SGVMProgram().also { block(it) }
        }
    }

    fun flit(dst: Int, lit: Float) {
        flit(dst, 1, floatArrayOf(lit))
    }

    fun flit(dst: Int, count: Int, lit: FloatArray, litPos: Int = 0) {
        opl(SGVMOpcode.FLIT, count, dst, 0)
        for (n in 0 until count) data.add(lit[litPos + n].toRawBits())
    }

    fun op(op: Int, ext: Int = 1, dst: Int = 0, src: Int = 0, src2: Int = 0) {
        data.add(0
            .insert(op, 0, 7)
            .insert(dst, 7, 7)
            .insert(src, 14, 7)
            .insert(src2, 21, 7)
            .insert(ext, 28, 4)
        )
    }
    fun opl(op: Int, ext: Int = 1, dst: Int = 0, srcL: Int = 0) {
        data.add(0
            .insert(op, 0, 7)
            .insert(dst, 7, 7)
            .insert(srcL, 14, 14)
            .insert(ext, 28, 4)
        )
    }

    private fun _setSwizzle(op: Int, dst: Int, swizzle: IntArray, src: Int) {
        assert(swizzle.size in 1..4)
        data.add(0
            .insert(op, 0, 7)
            .insert(dst, 7, 7)
            .insert(src, 14, 7)
            .insert(swizzle.getOrElse(0) { 0 }, 21, 2)
            .insert(swizzle.getOrElse(1) { 0 }, 23, 2)
            .insert(swizzle.getOrElse(2) { 0 }, 25, 2)
            .insert(swizzle.getOrElse(3) { 0 }, 27, 2)
            .insert(swizzle.size, 30, 2)
        )
    }

    fun setToSwizzle(dst: Int, swizzle: IntArray, src: Int) {
        _setSwizzle(SGVMOpcode.FTSW, dst, swizzle, src)
    }

    fun setFromSwizzle(dst: Int, swizzle: IntArray, src: Int) {
        _setSwizzle(SGVMOpcode.FFSW, dst, swizzle, src)
    }
}

inline class SGVMInstruction(val value: Int) {
    val OP get() = value.extract(0, 7)
    val DST get() = value.extract(7, 7)

    val SRC get() = value.extract(14, 7)
    val SRC2 get() = value.extract(21, 7)
    val EXT get() = value.extract(28, 4)

    fun SWIZZLE(n: Int) = value.extract(21 + 2 * n, 2)

    val EXT2: Int get() {
        val res = value.extract(30, 2)
        return if (res == 0) 4 else res
    }
}
