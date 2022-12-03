package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.kds.lock.*
import com.soywiz.kmem.*
import com.soywiz.korag.shader.*
import com.soywiz.korim.color.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import kotlin.math.*

class AGUniformValue constructor(val uniform: Uniform, data: Buffer, nativeValue: Any?) : AGValue(uniform, data, nativeValue) {
    override fun equals(other: Any?): Boolean = other is AGUniformValue && this.uniform == uniform && this.data == other.data && this.nativeValue == other.nativeValue
    override fun hashCode(): Int = uniform.hashCode() + super.hashCode()
}

class AGUniformValues(val capacity: Int = 4 * 1024) {
    private val data = Buffer(capacity)
    private var allocOffset = 0

    @PublishedApi internal val values = FastArrayList<AGUniformValue>()

    inline fun fastForEach(block: (AGUniformValue) -> Unit) {
        values.fastForEach(block)
    }

    fun clear() {
        values.clear()
        allocOffset = 0
    }

    fun put(other: AGUniformValues?) {
        copyFrom(other)
    }

    fun copyFrom(other: AGUniformValues?) {
        other?.fastForEach {
            this[it.uniform].set(it)
        }
    }

    fun setTo(other: AGUniformValues) {
        clear()
        copyFrom(other)
    }

    fun cloneReadOnly(): AGUniformValues {
        //return AGUniformValues().copyFrom(this)
        val out = AGUniformValues(allocOffset)
        arraycopy(this.data, 0, out.data, 0, allocOffset)
        values.fastForEach { value ->
            out.values.add(AGUniformValue(value.uniform, out.data.sliceWithSize(value.data.byteOffset, value.data.sizeInBytes), value.nativeValue))
        }
        return out
    }

    operator fun get(uniform: Uniform): AGUniformValue {
        for (n in 0 until values.size) {
            val value = values[n]
            if (value.uniform == uniform) return value
        }
        val dataSize = uniform.totalElementCount * 4
        if (allocOffset + dataSize >= this.data.size) error("this=$this : uniform=$uniform, allocOffset=$allocOffset, dataSize=$dataSize")
        val out = AGUniformValue(uniform, data.sliceWithSize(allocOffset, dataSize), null)
        values.add(out)
        allocOffset += dataSize
        return out
    }

    operator fun set(uniform: Uniform, value: Unit) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: AGValue) { this[uniform].set(value) }
    @Deprecated("Use NAGTextureUnit")
    operator fun set(uniform: Uniform, value: AGTextureUnit) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: NAGTextureUnit) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: Boolean) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: BooleanArray) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: Int) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: IntArray) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: Float) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: FloatArray) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: Double) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: Matrix3D) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: IPoint) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: Vector3D) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: RGBAf) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: RGBA) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: RGBAPremultiplied) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: Array<Vector3D>) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: Array<Matrix3D>) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: Array<FloatArray>) { this[uniform].set(value) }

    companion object {
        @PublishedApi internal val EMPTY = AGUniformValues()

        operator fun invoke(block: (AGUniformValues) -> Unit): AGUniformValues = AGUniformValues().also(block)
        fun valueToString(value: AGValue): String = value.toString()
    }
}

open class AGValue(val type: VarType, val arrayCount: Int, val data: Buffer, var nativeValue: Any?) {
    constructor(variable: Variable, data: Buffer, nativeValue: Any?) : this(variable.type, variable.arrayCount, data, nativeValue)

    val kind: VarKind = type.kind
    val stride: Int = type.elementCount
    val totalElements: Int = stride * arrayCount
    val totalBytes: Int = type.bytesSize * arrayCount

    //val data: Buffer = Buffer(type.bytesSize * arrayCount * 4)
    val i32: Int32Buffer = data.i32
    val f32: Float32Buffer = data.f32

    fun set(value: Unit) = Unit
    fun set(value: Boolean) = set(value.toInt())

    fun set(value: AGValue) {
        arraycopy(value.data, 0, this.data, 0, min(this.data.size, value.data.size))
        this.nativeValue = value.nativeValue
    }

    @Deprecated("Use NAGTextureUnit")
    fun set(value: AGTextureUnit) {
        set(value.index)
        nativeValue = value
    }

    fun set(value: NAGTextureUnit) {
        set(value.unitId)
        nativeValue = value
    }

    fun set(value: Int) = when (kind) {
        VarKind.TFLOAT -> f32[0] = value.toFloat()
        else -> i32[0] = value
    }

    fun set(value: Float) = when (kind) {
        VarKind.TFLOAT -> data.setFloat32(0, value)
        else -> set(value.toInt())
    }

    fun set(value: Double) = set(value.toFloat())

    fun set(v0: Float, v1: Float) {
        tempFloatsLock {
            tempFloats[0] = v0
            tempFloats[1] = v1
            set(tempFloats, 0, 2)
        }
    }

    fun set(v0: Float, v1: Float, v2: Float) {
        tempFloatsLock {
            tempFloats[0] = v0
            tempFloats[1] = v1
            tempFloats[2] = v2
            set(tempFloats, 0, 3)
        }
    }

    fun set(v0: Float, v1: Float, v2: Float, v3: Float) {
        tempFloatsLock {
            tempFloats[0] = v0
            tempFloats[1] = v1
            tempFloats[2] = v2
            tempFloats[3] = v3
            set(tempFloats, 0, 4)
        }
    }

    fun set(value: Vector3D) = set(value.data, 0, stride)
    fun set(value: RGBAf) = set(value.data, 0, 4)
    fun set(value: IPoint) = set(value.x.toFloat(), value.y.toFloat())
    fun set(value: Margin) = set(value.top.toFloat(), value.right.toFloat(), value.bottom.toFloat(), value.left.toFloat())
    fun set(value: RectCorners) = set(value.topLeft.toFloat(), value.topRight.toFloat(), value.bottomRight.toFloat(), value.bottomLeft.toFloat())
    fun set(value: RGBA) = set(value.rf, value.gf, value.bf, value.af)
    fun set(value: RGBAPremultiplied) = set(value.rf, value.gf, value.bf, value.af)
    fun set(mat: Matrix3D) = tempMatrixLock { set(tempMatrix.also { it[0] = mat }) }


    fun set(value: BooleanArray, offset: Int = 0, size: Int = value.size - offset) {
        tempIntsLock {
            for (n in 0 until size) tempInts[n] = value[offset + n].toInt()
            set(tempInts, 0, size)
        }
    }

    fun set(value: IntArray, offset: Int = 0, size: Int = value.size - offset) {
        when (kind) {
            VarKind.TFLOAT -> for (n in 0 until size) data.setFloat32(n, value[offset + n].toFloat())
            else -> data.setArrayInt32(0, value, offset, size)
        }
    }

    fun set(value: FloatArray, offset: Int = 0, size: Int = value.size - offset) {
        when (kind) {
            VarKind.TFLOAT -> data.setArrayFloat32(0, value, offset, size)
            else -> for (n in 0 until size) i32[n] = value[offset + n].toInt()
        }
    }

    fun set(value: DoubleArray, offset: Int = 0, size: Int = value.size - offset) {
        when (kind) {
            VarKind.TFLOAT -> for (n in 0 until size) f32[n] = value[offset + n].toFloat()
            else -> for (n in 0 until size) i32[n] = value[offset + n].toInt()
        }
    }

    fun set(value: Array<FloatArray>) {
        for (n in 0 until min(value.size, arrayCount)) {
            val data = value[n]
            f32.setArray(n * stride, data, 0, min(stride, data.size))
        }
    }

    fun set(vectors: Array<Vector3D>) {
        for (n in 0 until min(vectors.size, arrayCount)) {
            val data = vectors[n].data
            f32.setArray(n * stride, data, 0, min(data.size, stride))
        }
    }

    fun set(matArray: Array<Matrix3D>) {
        val arrayCount = min(arrayCount, matArray.size)
        val matSize = when (type) {
            VarType.Mat2 -> 2; VarType.Mat3 -> 3; VarType.Mat4 -> 4; else -> -1
        }
        tempFloatsLock {
            for (n in 0 until arrayCount) {
                matArray[n].copyToFloatWxH(tempFloats, matSize, matSize, MajorOrder.COLUMN, n * stride)
            }
            data.setArrayFloat32(0, tempFloats, 0, stride * arrayCount)
        }
    }

    override fun equals(other: Any?): Boolean = other is AGValue && this.data == other.data && this.nativeValue == other.nativeValue
    override fun hashCode(): Int = data.hashCode()

    companion object {
        private val tempIntsLock = NonRecursiveLock()
        private val tempInts = IntArray(64 * (4 * 4))
        private val tempFloatsLock = NonRecursiveLock()
        private val tempFloats = FloatArray(64 * (4 * 4))
        private val tempMatrixLock = NonRecursiveLock()
        private val tempMatrix = Array<Matrix3D>(1) { Matrix3D() }
    }

    override fun toString(): String {
        return buildString {
            append("AGValue[$kind](")
            append('[')
            for (i in 0 until arrayCount) {
                if (i != 0) append(",")
                append('[')
                for (n in 0 until stride) {
                    if (n != 0) append(", ")
                    if (kind == VarKind.TFLOAT) {
                        append(f32[i * stride + n].niceStr)
                    } else {
                        append(i32[i * stride + n])
                    }
                }
                append(']')
            }
            append(']')
            append(")")
        }
    }
}
