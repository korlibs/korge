@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korag.software

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import kotlin.math.*

// Software Graphics Virtual Machine
// @TODO: Port dynarek for even faster performance https://github.com/kpspemu/kpspemu/tree/master/dynarek2/
class SGVM(
    var program: SGVMProgram,
    val allocator: ShaderAllocator = ShaderAllocator(),
) {
    val freg = FloatArray(128)
    val textures = arrayOfNulls<AGSoftware.SoftwareTexture>(128)

    fun getAllocation(name: String) = allocator.allocatedNames[name]
    fun getAllocation(variable: Variable) = allocator.allocatedNames[variable.name]

    fun copyFrom(other: SGVM) {
        this.program = other.program
        this.tex2d = other.tex2d
    }

    fun clone() = SGVM(program, allocator).also {
        it.copyFrom(this)
    }

    var tex2d: (sampler: Int, x: Float, y: Float, out: FloatArray, outIndex: Int) -> Unit = { sampler: Int, x: Float, y: Float, out: FloatArray, outIndex: Int ->
        println("tex2d: sampler=$sampler, x=$x, y=$y, outIndex=$outIndex")
    }

    var cpc = 0
    var discard = false

    fun execute(pc: Int = 0): SGVM {
        discard = false
        cpc = pc
        val programData = program.rdata
        while (cpc < programData.size) {
            val i = SGVMInstruction(readPC())
            if (!i.interpret()) break
        }
        return this
    }

    fun readPC() = program.rdata[cpc++]

    fun setF(n: Int, value: Float) {
        debug { "freg[$n] = $value" }
        freg[n] = value
    }

    fun setFloats(pos: Int, values: FloatArray, count: Int) {
        for (n in 0 until count) setF(pos + n, values[n])
    }

    fun writeMatrix(pos: Int, m: Matrix3D, rows: Int, cols: Int) {
        var n = 0
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                setF(pos + n, m[row, col])
                n++
            }
        }
    }

    fun setUniform(pos: Int, uniform: Uniform, value: Any) {
        val type = uniform.type
        debug { "setUniform: pos=$pos, uniform=$uniform, value=$value" }
        if (type.isMatrix) {
            when (type.elementCount) {
                4 -> writeMatrix(pos, value as Matrix3D, 2, 2)
                9 -> writeMatrix(pos, value as Matrix3D, 3, 3)
                16 -> writeMatrix(pos, value as Matrix3D, 4, 4)
                else -> TODO()
            }
        } else {
            when (type.kind) {
                VarKind.TINT -> {
                    if (type.elementCount != 1) TODO("${type.elementCount}")
                    if (value is AG.TextureUnit) {
                        val softwareTexture = value.texture as? AGSoftware.SoftwareTexture?
                        softwareTexture?.bindEnsuring()
                        textures[pos] = softwareTexture
                        debug { "${textures[pos]}" }
                    } else {
                        TODO("value=$value")
                    }
                    //setF(pos, (value as Int).toFloat())
                }
                else -> TODO("${type.kind}")
            }
        }
    }

    private inline fun debug(block: () -> String) {
        //println(block())
    }

    private fun SGVMInstruction.interpret(): Boolean {
        val op = OP
        debug { "INTERPRET op=$op" }
        when (op) {
            SGVMOpcode.END -> return false
            SGVMOpcode.DISCARD -> {
                discard = true
                return false
            }
            SGVMOpcode.FZERO -> fset { 0f }
            SGVMOpcode.FONE -> fset { 1f }
            SGVMOpcode.FLIT -> {
                debug { "FLIT $DST <- $SRC ($EXT)" }
                fset { Float.fromBits(readPC()) }
            }
            SGVMOpcode.FSET -> {
                //println("FSET(COPY) $DST <- $SRC ($EXT)")
                debug { "FSET(COPY) $DST <- $SRC ($EXT)" }
                debug { "${(0 until EXT).map { freg[SRC + it] }}" }
                fset { fsrc(it) }
            }
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

            SGVMOpcode.SMUL -> fset { fsrc(it) * fsrc2(0) }
            SGVMOpcode.SDIV -> fset { fsrc(it) / fsrc2(0) }
            SGVMOpcode.SREM -> fset { fsrc(it) % fsrc2(0) }
            SGVMOpcode.SMAX -> fset { max(fsrc(it), fsrc2(0)) }
            SGVMOpcode.SMIN -> fset { min(fsrc(it), fsrc2(0)) }

            SGVMOpcode.FCOMP_EQ -> fset { if (fsrc(it) == fsrc2(it)) 1f else 0f }
            SGVMOpcode.FCOMP_NE -> fset { if (fsrc(it) != fsrc2(it)) 1f else 0f }
            SGVMOpcode.FCOMP_LT -> fset { if (fsrc(it) < fsrc2(it)) 1f else 0f }
            SGVMOpcode.FCOMP_LE -> fset { if (fsrc(it) <= fsrc2(it)) 1f else 0f }
            SGVMOpcode.FCOMP_GT -> fset { if (fsrc(it) > fsrc2(it)) 1f else 0f }
            SGVMOpcode.FCOMP_GE -> fset { if (fsrc(it) >= fsrc2(it)) 1f else 0f }

            SGVMOpcode.TEX2D -> {
                //println("TEXT2D: $DST")
                tex2d(SRC, fsrc2(0), fsrc2(1), freg, DST)
            }
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
            SGVMOpcode.MMMUL -> {
                readMatrix4x4(SRC, matSrcL)
                readMatrix4x4(SRC2, matSrcR)
                matDst.multiply(matSrcL, matSrcR)
                writeMatrix4x4(DST, matDst)
                debug { "MMMUL: [$DST]$matDst = [$SRC]$matSrcL * [$SRC2]$matSrcR" }
                //println("MMMUL: [$DST]$matDst = [$SRC]$matSrcL * [$SRC2]$matSrcR")
            }
            SGVMOpcode.MFMUL -> {
                readMatrix4x4(SRC, matSrcL)
                readVector4(SRC2, vecSrcR)
                matDst.transform(vecSrcR, vecDst)
                writeVector4(DST, vecDst)
                debug { "MFMUL: [$DST]$vecDst = [$SRC]$matSrcL * [$SRC2]$vecSrcR" }
                //println("MFMUL: [$DST]$vecDst = [$SRC]$matSrcL * [$SRC2]$vecSrcR")
            }
            SGVMOpcode.JUMP, SGVMOpcode.JUMP_TRUE, SGVMOpcode.JUMP_FALSE -> {
                val addr = readPC()
                val cond = when (op) {
                    SGVMOpcode.JUMP_TRUE -> fsrc(0) != 0f
                    SGVMOpcode.JUMP_FALSE -> fsrc(0) == 0f
                    else -> true
                }
                if (cond) cpc = addr
            }
            else -> TODO("$op")
        }
        return true
    }
    private val matSrcL = Matrix3D()
    private val matSrcR = Matrix3D()
    private val matDst = Matrix3D()

    private val vecSrcL = Vector3D()
    private val vecSrcR = Vector3D()
    private val vecDst = Vector3D()

    fun readMatrix4x4(pos: Int, m: Matrix3D) {
        debug { "readMatrix4x4[pos=$pos]: " + freg.slice(pos until (pos + 16)).toList() }
        //m.setColumns4x4(freg, pos)
        m.setRows4x4(freg, pos)
    }

    fun writeMatrix4x4(pos: Int, m: Matrix3D) {
        m.copyToFloat4x4(freg, MajorOrder.ROW, pos)
        debug { "writeMatrix4x4[pos=$pos]: " + freg.slice(pos until (pos + 16)).toList() }
    }

    fun readVector4(pos: Int, v: Vector3D) {
        for (n in 0 until 4) v[n] = freg[pos + n]
    }

    fun writeVector4(pos: Int, v: Vector3D) {
        for (n in 0 until 4) freg[pos + n] = v[n]
    }

    private inline fun SGVMInstruction.fset(block: (Int) -> Float) {
        val dst = DST
        for (n in 0 until EXT) {
            setF(dst + n, block(n))
        }
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

    const val SMUL = 16 // Multiplication
    const val SDIV = 17 // Division
    const val SREM = 18 // Remaining
    const val SMAX = 19 // Maximum
    const val SMIN = 20 // Minimum

    const val FFSW = 30 // Copy from swizzled
    const val FTSW = 31 // Copy to swizzled

    const val MFMUL = 40 // Matrix * Vector multiplication
    const val MMMUL = 41 // Matrix * Matrix multiplication

    const val FCOMP_EQ = 50
    const val FCOMP_NE = 51
    const val FCOMP_LT = 52
    const val FCOMP_LE = 53
    const val FCOMP_GT = 54
    const val FCOMP_GE = 55

    const val JUMP = 60
    const val JUMP_TRUE = 61
    const val JUMP_FALSE = 62
    const val DISCARD = 63

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
    val EXT: Int get() {
        val res = value.extract(28, 4)
        return if (res == 0) 16 else res
    }

    fun SWIZZLE(n: Int) = value.extract(21 + 2 * n, 2)

    val EXT2: Int get() {
        val res = value.extract(30, 2)
        return if (res == 0) 4 else res
    }
}
