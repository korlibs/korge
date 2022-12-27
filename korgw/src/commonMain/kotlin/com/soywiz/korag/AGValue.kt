package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kds.lock.*
import com.soywiz.kmem.*
import com.soywiz.korag.shader.*
import com.soywiz.korim.color.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import kotlin.math.*

class AGUniformValue constructor(
    val uniform: Uniform,
    data: Buffer,
    texture: AGTexture? = null,
    textureUnitInfo: AGTextureUnitInfo = AGTextureUnitInfo.DEFAULT
) : AGValue(uniform, data, texture, textureUnitInfo) {
    override fun equals(other: Any?): Boolean = other is AGUniformValue && this.uniform == uniform && this.data == other.data && this.texture == other.texture && this.textureUnitInfo == other.textureUnitInfo
    override fun hashCode(): Int = uniform.hashCode() + super.hashCode()
    override fun toString(): String = "AGUniformValue[$uniform][${super.toString()}]"
}

interface AGUniformContainer {
    fun getAllUniformValues(): List<AGUniformValue>
    operator fun get(uniform: Uniform): AGUniformValue
}

fun AGUniformContainer.copyTo(other: AGUniformContainer) {
    for (value in this.getAllUniformValues()) {
        other[value.uniform].set(value)
    }
}

@Deprecated("")
class AGUniformValues(val capacity: Int = 8 * 1024) : AGUniformContainer {
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
            out.values.add(AGUniformValue(value.uniform, out.data.sliceWithSize(value.data.byteOffset, value.data.sizeInBytes), value.texture, value.textureUnitInfo))
        }
        return out
    }

    override operator fun get(uniform: Uniform): AGUniformValue {
        for (n in 0 until values.size) {
            val value = values[n]
            if (value.uniform == uniform) return value
        }
        val dataSize = uniform.totalElementCount * 4
        if (allocOffset + dataSize >= this.data.size) error("this=$this : uniform=$uniform, allocOffset=$allocOffset, dataSize=$dataSize")
        val out = AGUniformValue(uniform, data.sliceWithSize(allocOffset, dataSize), null, AGTextureUnitInfo.INVALID)
        values.add(out)
        allocOffset += dataSize
        return out
    }

    override fun getAllUniformValues(): List<AGUniformValue> {
        return values
    }

    companion object {
        @PublishedApi internal val EMPTY = AGUniformValues()

        operator fun invoke(block: (AGUniformValues) -> Unit): AGUniformValues = AGUniformValues().also(block)
        fun valueToString(value: AGValue): String = value.toString()
    }

    override fun toString(): String = "AGUniformValues(${values.joinToString(", ") { "${it.uniform.name}=" + valueToString(it) }})"
}


operator fun AGUniformContainer.set(uniform: Uniform, value: Unit) { this[uniform].set(value) }
operator fun AGUniformContainer.set(uniform: Uniform, value: AGValue) { this[uniform].set(value) }
fun AGUniformContainer.set(uniform: Uniform, value: AGTexture?, info: AGTextureUnitInfo = AGTextureUnitInfo.DEFAULT) { this[uniform].set(value, info) }
operator fun AGUniformContainer.set(uniform: Uniform, value: Boolean) { this[uniform].set(value) }
operator fun AGUniformContainer.set(uniform: Uniform, value: BooleanArray) { this[uniform].set(value) }
operator fun AGUniformContainer.set(uniform: Uniform, value: Int) { this[uniform].set(value) }
operator fun AGUniformContainer.set(uniform: Uniform, value: IntArray) { this[uniform].set(value) }
operator fun AGUniformContainer.set(uniform: Uniform, value: Float) { this[uniform].set(value) }
operator fun AGUniformContainer.set(uniform: Uniform, value: FloatArray) { this[uniform].set(value) }
operator fun AGUniformContainer.set(uniform: Uniform, value: Double) { this[uniform].set(value) }
operator fun AGUniformContainer.set(uniform: Uniform, value: Matrix3D) { this[uniform].set(value) }
operator fun AGUniformContainer.set(uniform: Uniform, value: IPoint) { this[uniform].set(value) }
operator fun AGUniformContainer.set(uniform: Uniform, value: Vector3D) { this[uniform].set(value) }
operator fun AGUniformContainer.set(uniform: Uniform, value: RGBAf) { this[uniform].set(value) }
operator fun AGUniformContainer.set(uniform: Uniform, value: RGBA) { this[uniform].set(value) }
operator fun AGUniformContainer.set(uniform: Uniform, value: RGBAPremultiplied) { this[uniform].set(value) }
operator fun AGUniformContainer.set(uniform: Uniform, value: Array<Vector3D>) { this[uniform].set(value) }
operator fun AGUniformContainer.set(uniform: Uniform, value: Array<Matrix3D>) { this[uniform].set(value) }
operator fun AGUniformContainer.set(uniform: Uniform, value: Array<FloatArray>) { this[uniform].set(value) }

open class AGValue(
    val type: VarType,
    val arrayCount: Int,
    val data: Buffer,
    var texture: AGTexture?,
    var textureUnitInfo: AGTextureUnitInfo,
) {
    constructor(
        variable: Variable,
        data: Buffer,
        texture: AGTexture?,
        textureUnitInfo: AGTextureUnitInfo
    ) : this(variable.type, variable.arrayCount, data, texture, textureUnitInfo)

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

    fun set(value: Unit) = Unit
    fun set(value: Boolean) = set(value.toInt())

    fun set(value: AGValue) {
        arraycopy(value.data, 0, this.data, 0, min(this.data.size, value.data.size))
        this.texture = value.texture
        this.textureUnitInfo = value.textureUnitInfo
    }

    fun set(value: AGTexture?, info: AGTextureUnitInfo = AGTextureUnitInfo.DEFAULT) {
        set(-1)
        texture = value
        textureUnitInfo = info
    }

    fun set(value: Int) = when (kind) {
        VarKind.TFLOAT -> f32[0] = value.toFloat()
        else -> i32[0] = value
    }

    fun set(value: Float) = when (kind) {
        VarKind.TFLOAT -> f32[0] = value
        else -> i32[0] = value.toInt()
    }

    fun set(value: Double) = set(value.toFloat())
    fun set(v0: Double, v1: Double) = set(v0.toFloat(), v1.toFloat())
    fun set(v0: Double, v1: Double, v2: Double) = set(v0.toFloat(), v1.toFloat(), v2.toFloat())
    fun set(v0: Double, v1: Double, v2: Double, v3: Double) = set(v0.toFloat(), v1.toFloat(), v2.toFloat(), v3.toFloat())

    fun set(v0: Float, v1: Float) = _set(v0, v1, 0f, 0f, 2)
    fun set(v0: Float, v1: Float, v2: Float) = _set(v0, v1, v2, 0f, 3)
    fun set(v0: Float, v1: Float, v2: Float, v3: Float) = _set(v0, v1, v2, v3, 4)

    private fun _set(v0: Float, v1: Float, v2: Float, v3: Float, size: Int = 4) {
        val msize = min(size, this.data.size / 4)
        if (kind == VarKind.TFLOAT) {
            if (msize >= 1) f32[0] = v0
            if (msize >= 2) f32[1] = v1
            if (msize >= 3) f32[2] = v2
            if (msize >= 4) f32[3] = v3
        } else {
            if (msize >= 1) i32[0] = v0.toInt()
            if (msize >= 2) i32[1] = v1.toInt()
            if (msize >= 3) i32[2] = v2.toInt()
            if (msize >= 4) i32[3] = v3.toInt()
        }
    }

    private inline fun _setFloatArray(size: Int, gen: (index: Int) -> Float) {
        if (kind == VarKind.TFLOAT) {
            for (n in 0 until min(size, this.f32.size)) f32[n] = gen(n)
        } else {
            for (n in 0 until min(size, this.i32.size)) i32[n] = gen(n).toInt()
        }
    }

    private inline fun _setIntArray(size: Int, gen: (index: Int) -> Int) {
        if (kind == VarKind.TFLOAT) {
            for (n in 0 until min(size, this.f32.size)) f32[n] = gen(n).toFloat()
        } else {
            for (n in 0 until min(size, this.i32.size)) i32[n] = gen(n)
        }
    }

    fun set(value: Vector3D) = set(value.data)
    fun set(value: RGBAf) = set(value.data, 0, 4)
    fun set(value: IPoint) = set(value.x.toFloat(), value.y.toFloat())
    fun set(value: Margin) = set(value.top.toFloat(), value.right.toFloat(), value.bottom.toFloat(), value.left.toFloat())
    fun set(value: RectCorners) = set(value.topLeft.toFloat(), value.topRight.toFloat(), value.bottomRight.toFloat(), value.bottomLeft.toFloat())
    fun set(value: RGBA) = set(value.rf, value.gf, value.bf, value.af)
    fun set(value: RGBAPremultiplied) = set(value.rf, value.gf, value.bf, value.af)
    fun set(mat: Matrix3D) = tempMatrixLock { set(tempMatrix.also { it[0] = mat }) }

    fun set(value: BooleanArray, offset: Int = 0, size: Int = value.size - offset) {
        _setIntArray(size) { value[offset + it].toInt() }
    }

    fun set(value: IntArray, offset: Int = 0, size: Int = value.size - offset) {
        val rsize = min(size, this.i32.size)
        when (kind) {
            VarKind.TFLOAT -> for (n in 0 until rsize) data.setFloat32(n, value[offset + n].toFloat())
            else -> data.setArrayInt32(0, value, offset, rsize)
        }
    }

    fun set(value: FloatArray, offset: Int = 0, size: Int = value.size - offset) {
        val rsize = min(size, this.f32.size)
        when (kind) {
            VarKind.TFLOAT -> data.setArrayFloat32(0, value, offset, rsize)
            else -> for (n in 0 until rsize) i32[n] = value[offset + n].toInt()
        }
    }

    fun set(value: DoubleArray, offset: Int = 0, size: Int = value.size - offset) {
        val rsize = min(size, this.f32.size)
        when (kind) {
            VarKind.TFLOAT -> for (n in 0 until rsize) f32[n] = value[offset + n].toFloat()
            else -> for (n in 0 until rsize) i32[n] = value[offset + n].toInt()
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

    override fun equals(other: Any?): Boolean = other is AGValue && this.data == other.data && this.texture == other.texture && this.textureUnitInfo == other.textureUnitInfo
    override fun hashCode(): Int = data.hashCode()

    companion object {
        private val tempFloatsLock = NonRecursiveLock()
        private val tempFloats = FloatArray(64 * (4 * 4))
        private val tempMatrixLock = NonRecursiveLock()
        private val tempMatrix = Array<Matrix3D>(1) { Matrix3D() }
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
            if (texture != null) {
                append(',')
                append(texture)
                append(',')
                append(textureUnitInfo)
            }
            append(']')
            append(")")
        }
    }
}

class UniformBlockData(val block: UniformBlock) : AGUniformContainer {
    val data: Buffer = Buffer(block.totalSize)
    val values: List<AGUniformValue> = block.itemsWithInfo.map {
        val uniform = it.uniform
        val position = it.offset
        val size = it.size
        AGUniformValue(uniform, data.sliceWithSize(position, size), null, AGTextureUnitInfo.DEFAULT)
    }
    val valuesByUniform = values.associateBy { it.uniform }
    override fun getAllUniformValues(): List<AGUniformValue> = values
    override operator fun get(uniform: Uniform): AGUniformValue = valuesByUniform[uniform] ?: error("Can't get uniform $uniform in $this")

    override fun toString(): String = "UniformBlockData[$block]($values)"
}

class UniformBlockBuffer(val block: UniformBlock, val maxElements: Int) {
    val tempBuffer = Buffer(block.totalSize)
    val buffer = Buffer(block.totalSize * maxElements)
    val textures = arrayOfNulls<AGTexture>(block.numElements * maxElements)
    val textureUnitInfos = IntArray(block.numElements * maxElements)

    val tempValues: UniformBlockData = UniformBlockData(block)

    operator fun set(index: Int, data: UniformBlockData) {
        if (data.block != block) error("${data.block} != $block")
        arraycopy(data.data, 0, this.buffer, index * block.totalSize, block.totalSize)
        for (n in 0 until block.numElements) {
            this.textures[index * block.numElements + n] = data.values[n].texture
            this.textureUnitInfos[index * block.numElements + n] = data.values[n].textureUnitInfo.data
        }
    }
    fun copyIndexTo(index: Int, data: UniformBlockData) {
        if (data.block != block) error("${data.block} != $block")
        arraycopy(this.buffer, index * block.totalSize, data.data, 0, block.totalSize)

        for (n in 0 until block.numElements) {
            data.values[n].texture = this.textures[index * block.numElements + n]
            data.values[n].textureUnitInfo = AGTextureUnitInfo.raw(this.textureUnitInfos[index * block.numElements + n])
        }
    }

    fun readValues(index: Int, out: UniformBlockData = tempValues): UniformBlockData {
        copyIndexTo(index, out)
        return out
    }

    var lastIndex = 0
        private set

    fun reset() {
        lastIndex = 0
    }

    fun put(data: UniformBlockData): Int {
        val index = lastIndex++
        this[index] = data
        return index
    }

    override fun toString(): String {
        return buildString {
            append("UniformBlockBuffer[$maxElements](")
            for (n in 0 until maxElements) {
                val temp = readValues(n)
                if (n > 0) append(", ")
                append(temp)
            }
            append(")")
        }
    }
}

class AGUniformBlockValues(val buffers: List<UniformBlockBuffer>, val indices: IntArray) {
    companion object {
        val EMPTY = AGUniformBlockValues(emptyList(), intArrayOf())
    }
    inline fun forEachBlock(block: (UniformBlockData) -> Unit) {
        for (n in 0 until kotlin.math.min(buffers.size, indices.size)) {
            val buffer = buffers[n]
            val index = indices[n]
            block(buffer.readValues(index))
        }
    }

    inline fun forEachValue(block: (AGUniformValue) -> Unit) {
        forEachBlock {
            it.values.fastForEach { block(it) }
        }
    }

    inline fun fastForEach(block: (AGUniformValue) -> Unit) {
        forEachValue { block(it) }
    }

    override fun toString(): String = "AGUniformBlockValues(buffers=$buffers, indices=${indices.size})"
}

@Deprecated("")
fun AGUniformBlockValuesSimple(block: AGUniformContainer.() -> Unit): AGUniformBlockValues {
    val temp = AGUniformValues()
    block(temp)
    return temp.cloneReadOnlyToUniformBlockValues()
}

@Deprecated("")
fun AGUniformValues.cloneReadOnlyToUniformBlockValues(): AGUniformBlockValues {
    val allUniformBlock = UniformBlock(this.getAllUniformValues().map { it.uniform })
    val data = UniformBlockData(allUniformBlock)
    val buffer = UniformBlockBuffer(allUniformBlock, 1)
    this.copyTo(data)
    buffer[0] = data
    return AGUniformBlockValues(listOf(buffer), intArrayOf(0))
}
