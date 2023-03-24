package korlibs.graphics

import korlibs.datastructure.*
import korlibs.datastructure.lock.*
import korlibs.memory.*
import korlibs.graphics.shader.*
import korlibs.image.color.*
import korlibs.io.util.*
import korlibs.math.geom.*
import kotlin.math.*

@Deprecated("")
class AGUniformValues(val capacity: Int = 8 * 1024) {
    private val data = Buffer(capacity)
    private var allocOffset = 0

    //@PublishedApi internal val values = FastArrayList<AGUniformValue>()
    val values = FastArrayList<AGUniformValue>()

    fun isNotEmpty(): Boolean = values.isNotEmpty()

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
            out.values.add(AGUniformValue(value.uniform, out.data.sliceWithSize(value.data.byteOffset, value.data.sizeInBytes)))
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
        val out = AGUniformValue(uniform, data.sliceWithSize(allocOffset, dataSize))
        values.add(out)
        allocOffset += dataSize
        return out
    }

    operator fun set(uniform: Uniform, value: Unit) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: AGValue) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: Boolean) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: BooleanArray) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: Int) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: IntArray) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: Float) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: FloatArray) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: Double) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: MMatrix3D) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: Point) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: Size) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: MPoint) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: MVector4) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: RGBAf) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: RGBA) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: RGBAPremultiplied) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: Array<MVector4>) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: Array<MMatrix3D>) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: Array<FloatArray>) { this[uniform].set(value) }

    operator fun set(uniform: Uniform, value: Vector4) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: Margin) { this[uniform].set(value) }
    operator fun set(uniform: Uniform, value: RectCorners) { this[uniform].set(value) }

    fun set(uniform: Uniform, v0: Float, v1: Float) { this[uniform].set(v0, v1) }
    fun set(uniform: Uniform, v0: Float, v1: Float, v2: Float) { this[uniform].set(v0, v1, v2) }
    fun set(uniform: Uniform, v0: Float, v1: Float, v2: Float, v3: Float) { this[uniform].set(v0, v1, v2, v3) }

    fun set(uniform: Uniform, v0: Double, v1: Double) { this[uniform].set(v0, v1) }
    fun set(uniform: Uniform, v0: Double, v1: Double, v2: Double) { this[uniform].set(v0, v1, v2) }
    fun set(uniform: Uniform, v0: Double, v1: Double, v2: Double, v3: Double) { this[uniform].set(v0, v1, v2, v3) }

    companion object {
        @PublishedApi internal val EMPTY = AGUniformValues()

        operator fun invoke(block: (AGUniformValues) -> Unit): AGUniformValues = AGUniformValues().also(block)
        fun valueToString(value: AGValue): String = value.toString()
    }

    override fun toString(): String = "AGUniformValues(${values.joinToString(", ") { "${it.uniform.name}=" + valueToString(it) }})"
}

@Deprecated("")
fun AGBufferExtractToFloatAndInts(type: VarType, arrayCount: Int, buffer: Buffer, offset: Int, out: Buffer) {
    val totalElements: Int = type.elementCount * arrayCount
    val totalBytes: Int = type.bytesSize * arrayCount

    val i32 = out.i32
    when (type.kind) {
        VarKind.TFLOAT, VarKind.TINT -> {
            arraycopy(buffer, offset, out, 0, totalBytes)
        }
        VarKind.TBOOL, VarKind.TBYTE, VarKind.TUNSIGNED_BYTE -> {
            for (n in 0 until totalElements) i32[n] = buffer.getUInt8(offset + n)
        }
        VarKind.TSHORT, VarKind.TUNSIGNED_SHORT -> {
            for (n in 0 until totalElements) i32[n] = buffer.getUInt16(offset + n * 2)
        }
    }
}

@Deprecated("")
class AGUniformValue(
    val uniform: Uniform,
    data: Buffer = Buffer(uniform.totalBytes),
) : AGValue(uniform, data) {
    override fun equals(other: Any?): Boolean = other is AGUniformValue && this.uniform == uniform && this.data == other.data
    override fun hashCode(): Int = uniform.hashCode() + super.hashCode()
    override fun toString(): String = "AGUniformValue[$uniform][${super.toString()}]"
}

@Deprecated("")
open class AGValue(
    val type: VarType,
    val arrayCount: Int,
    val data: Buffer,
) {
    constructor(
        variable: OperandWithArray,
        data: Buffer,
    ) : this(variable.type, variable.arrayCount, data)

    val kind: VarKind = type.kind
    val stride: Int = type.elementCount
    val totalElements: Int = stride * arrayCount
    val totalBytes: Int = type.bytesSize * arrayCount

    //val data: Buffer = Buffer(type.bytesSize * arrayCount * 4)
    val i8: Int8Buffer = data.i8
    val u8: Uint8Buffer = data.u8
    val i16: Int16Buffer = data.i16
    val u16: Uint16Buffer = data.u16
    val i32: Int32Buffer = data.i32
    val f32: Float32Buffer = data.f32

    fun extractToFloatAndInts(out: Buffer): Buffer {
        AGBufferExtractToFloatAndInts(type, arrayCount, data, 0, out)
        return out
    }

    fun set(value: Unit) = Unit
    fun set(value: Boolean) = set(value.toInt())

    fun set(value: AGValue) {
        arraycopy(value.data, 0, this.data, 0, min(this.data.size, value.data.size))
    }

    fun set(value: Int) = when (kind) {
        VarKind.TFLOAT -> f32[0] = value.toFloat()
        VarKind.TBOOL, VarKind.TBYTE, VarKind.TUNSIGNED_BYTE -> u8[0] = value
        VarKind.TSHORT, VarKind.TUNSIGNED_SHORT -> u16[0] = value
        else -> i32[0] = value
    }

    fun set(value: Float) = when (kind) {
        VarKind.TFLOAT -> f32[0] = value
        VarKind.TBOOL, VarKind.TBYTE, VarKind.TUNSIGNED_BYTE -> u8[0] = value.toInt()
        VarKind.TSHORT, VarKind.TUNSIGNED_SHORT -> u16[0] = value.toInt()
        else -> i32[0] = value.toInt()
    }

    fun set(value: Double) = set(value.toFloat())
    fun set(v0: Double, v1: Double) = set(v0.toFloat(), v1.toFloat())
    fun set(v0: Double, v1: Double, v2: Double) = set(v0.toFloat(), v1.toFloat(), v2.toFloat())
    fun set(v0: Double, v1: Double, v2: Double, v3: Double) = set(v0.toFloat(), v1.toFloat(), v2.toFloat(), v3.toFloat())

    fun set(v0: Float, v1: Float) = _set(v0, v1, 0f, 0f, 2)
    fun set(v0: Float, v1: Float, v2: Float) = _set(v0, v1, v2, 0f, 3)
    fun set(v0: Float, v1: Float, v2: Float, v3: Float) = _set(v0, v1, v2, v3, 4)

    private fun _setIndex(index: Int, v: Float) {
        when (kind) {
            VarKind.TFLOAT -> f32[index] = v
            VarKind.TBOOL, VarKind.TBYTE, VarKind.TUNSIGNED_BYTE -> u8[index] = v.toInt()
            VarKind.TSHORT, VarKind.TUNSIGNED_SHORT -> u16[index] = v.toInt()
            VarKind.TINT -> i32[index] = v.toInt()
        }
    }

    private fun _set(v0: Float, v1: Float, v2: Float, v3: Float, size: Int = 4) {
        val msize = min(size, totalElements)
        if (msize >= 1) _setIndex(0, v0)
        if (msize >= 2) _setIndex(1, v1)
        if (msize >= 3) _setIndex(2, v2)
        if (msize >= 4) _setIndex(3, v3)
    }

    inline fun setFloatArray(size: Int, arrayIndex: Int = 0, gen: (index: Int) -> Float) {
        val offset = arrayIndex * stride
        val rsize = min(size, totalElements - offset)
        if (kind == VarKind.TFLOAT) {
            for (n in 0 until rsize) f32[offset + n] = gen(n)
        } else {
            TODO()
        }
    }

    inline fun setIntArray(size: Int, arrayIndex: Int = 0, gen: (index: Int) -> Int) {
        //min(size, this.f32.size)
        val offset = arrayIndex * stride
        val rsize = min(size, totalElements - offset)
        when (kind) {
            VarKind.TFLOAT -> for (n in 0 until rsize) f32[offset + n] = gen(n).toFloat()
            VarKind.TBOOL, VarKind.TBYTE, VarKind.TUNSIGNED_BYTE -> for (n in 0 until rsize) u8[offset + n] = gen(n)
            VarKind.TSHORT, VarKind.TUNSIGNED_SHORT -> for (n in 0 until rsize) u16[offset + n] = gen(n)
            VarKind.TINT -> for (n in 0 until rsize) i32[offset + n] = gen(n)
        }
    }

    fun set(value: RGBAf) = set(value.data, 0, 4)

    fun set(value: MVector4) = set(value.data)
    fun set(value: IVector4) = set(value.x, value.y, value.z, value.w)
    fun set(value: MPoint) = set(value.x.toFloat(), value.y.toFloat())
    fun set(value: IRectCorners) = set(value.topLeft.toFloat(), value.topRight.toFloat(), value.bottomRight.toFloat(), value.bottomLeft.toFloat())
    fun set(value: MMatrix3D) = tempMatrixLock { set(tempIMatrix.also { it[0] = value }) }

    fun set(value: Vector4) = set(value.x, value.y, value.z, value.w)
    fun set(value: Point) = set(value.x, value.y)
    fun set(value: Size) = set(value.width, value.height)
    fun set(value: Margin) = set(value.top.toFloat(), value.right.toFloat(), value.bottom.toFloat(), value.left.toFloat())
    fun set(value: RectCorners) = set(value.bottomRight, value.topRight, value.bottomLeft, value.topLeft)

    //fun set(value: Matrix4) = tempMatrixLock { set(tempMatrix.also { it[0] = value }) }

    fun set(value: RGBA) = set(value.rf, value.gf, value.bf, value.af)
    fun set(value: RGBAPremultiplied) = set(value.rf, value.gf, value.bf, value.af)

    fun set(value: BooleanArray, offset: Int = 0, size: Int = value.size - offset) = setIntArray(size) { value[offset + it].toInt() }
    fun set(value: IntArray, offset: Int = 0, size: Int = value.size - offset) = setIntArray(size) { value[offset + it] }
    fun set(value: FloatArray, offset: Int = 0, size: Int = value.size - offset) = setFloatArray(size) { value[offset + it] }
    fun set(value: DoubleArray, offset: Int = 0, size: Int = value.size - offset) = setFloatArray(size) { value[offset + it].toFloat() }

    fun set(value: Array<FloatArray>) {
        for (n in 0 until min(value.size, arrayCount)) {
            val data = value[n]
            setFloatArray(data.size, n) { data[it] }
        }
    }

    fun set(vectors: Array<MVector4>) {
        for (n in 0 until min(vectors.size, arrayCount)) {
            val data = vectors[n].data
            setFloatArray(data.size, n) { data[it] }
        }
    }

    fun set(matArray: Array<MMatrix3D>) {
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

    //fun set(matArray: Array<Matrix4>) {
    //    val arrayCount = min(arrayCount, matArray.size)
    //    val matSize = when (type) {
    //        VarType.Mat2 -> 2; VarType.Mat3 -> 3; VarType.Mat4 -> 4; else -> -1
    //    }
    //    tempFloatsLock {
    //        for (n in 0 until arrayCount) {
    //            matArray[n].copyToFloatWxH(tempFloats, matSize, matSize, MajorOrder.COLUMN, n * stride)
    //        }
    //        data.setArrayFloat32(0, tempFloats, 0, stride * arrayCount)
    //    }
    //}

    override fun equals(other: Any?): Boolean = other is AGValue && this.data == other.data
    override fun hashCode(): Int = data.hashCode()

    companion object {
        private val tempFloatsLock = NonRecursiveLock()
        private val tempFloats = FloatArray(64 * (4 * 4))
        private val tempMatrixLock = NonRecursiveLock()
        private val tempIMatrix = Array<MMatrix3D>(1) { MMatrix3D() }
        private val tempMatrix = Array<Matrix4>(1) { Matrix4() }
    }

    override fun toString(): String {
        return buildString {
            append("AGValue[$type](")
            append('[')
            for (i in 0 until arrayCount) {
                if (i != 0) append(",")
                append('[')
                for (n in 0 until stride) {
                    if (n != 0) append(", ")
                    when (kind) {
                        VarKind.TFLOAT -> append(f32[i * stride + n].niceStr)
                        VarKind.TBYTE -> append(i8[i * stride + n])
                        VarKind.TUNSIGNED_BYTE -> append(u8[i * stride + n])
                        VarKind.TSHORT -> append(i16[i * stride + n])
                        VarKind.TUNSIGNED_SHORT -> append(u16[i * stride + n])
                        else -> append(i32[i * stride + n])
                    }
                }
                append(']')
            }
            append(']')
            append(")")
        }
    }
}
