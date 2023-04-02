package korlibs.graphics.shader

import korlibs.graphics.*
import korlibs.image.color.*
import korlibs.io.lang.*
import korlibs.math.geom.*
import korlibs.math.math.*
import korlibs.memory.*
import korlibs.memory.dyn.*
import kotlin.reflect.*

class UniformBlocksBuffersRef(
    val blocks: Array<UniformBlockBuffer<*>?>,
    val buffers: Array<AGBuffer?>,
    val valueIndices: IntArray
) {
    val size: Int get() = blocks.size

    companion object {
        val EMPTY = UniformBlocksBuffersRef(emptyArray(), emptyArray(), IntArray(0))
    }

    inline fun fastForEachBlock(callback: (index: Int, block: UniformBlockBuffer<*>, buffer: AGBuffer?, valueIndex: Int) -> Unit) {
        for (n in 0 until size) {
            val block = blocks[n] ?: continue
            callback(n, block, buffers[n], valueIndices[n])
        }
    }
}

@Suppress("unused")
class TypedUniform<T>(name: String, val voffset: Int, var vindex: Int, val block: UniformBlock, type: VarType)
    : VariableWithOffset(name, type, 1, Precision.DEFAULT)
{
    val uniform: Uniform = Uniform(name, type, 1, typedUniform = this)
    val varType: VarType get() = uniform.type
    operator fun getValue(thisRef: Any?, property: KProperty<*>): TypedUniform<T> = this
    override fun toString(): String = "TypedUniform(name='$name', offset=$voffset, type=$type)"
}

open class UniformBlock(val fixedLocation: Int) {
    val name: String get() = this::class.portableSimpleName
    private val layout = KMemLayoutBuilder()
    private val _items = arrayListOf<TypedUniform<*>>()
    private var lastIndex = 0
    val uniforms: List<TypedUniform<*>> get() = _items
    val totalSize: Int get() = layout.size.nextMultipleOf(256)
    val uniformCount: Int get() = uniforms.size

    // @TODO: Fix alignment
    //protected fun bool(name: String? = null): Gen<List<Boolean>> = gen(name, VarType.Bool1, 4, 4)
    //protected fun bool2(name: String? = null): Gen<List<Boolean>> = gen(name, VarType.Bool2, 8, 8)
    //protected fun bool3(name: String? = null): Gen<List<Boolean>> = gen(name, VarType.Bool3, 12, 16)
    //protected fun bool4(name: String? = null): Gen<List<Boolean>> = gen(name, VarType.Bool4, 16, 16)
    //protected fun ubyte4(name: String? = null): Gen<Int> = gen(name, VarType.UByte4, 4, 4)
    //protected fun short(name: String? = null): Gen<List<Boolean>> = gen(name, VarType.Short1, 2, 2)
    //protected fun short2(name: String? = null): Gen<List<Boolean>> = gen(name, VarType.Short2, 4, 4)
    //protected fun short3(name: String? = null): Gen<List<Boolean>> = gen(name, VarType.Short3, 6, 2)
    //protected fun short4(name: String? = null): Gen<List<Boolean>> = gen(name, VarType.Short4, 8, 2)
    //@Deprecated("") protected fun sampler2D(name: String? = null): Gen<Int> = gen(name, VarType.Sampler2D, 4, 4)

    protected fun int(name: String? = null): Gen<Int> = gen(name, VarType.SInt1, 4, 4)
    protected fun float(name: String? = null): Gen<Float> = gen(name, VarType.Float1, 4, 4)

    protected fun ivec2(name: String? = null): Gen<Vector2Int> = gen(name, VarType.SInt2, 8, 8)
    protected fun vec2(name: String? = null): Gen<Vector2> = gen(name, VarType.Float2, 8, 8)

    // @TODO: Some drivers get this wrong
    //protected fun vec3(name: String? = null): Gen<Vector3> = gen(name, VarType.Float3, 16, 4)

    protected fun ivec4(name: String? = null): Gen<Vector4Int> = gen(name, VarType.SInt4, 16, 16)
    protected fun vec4(name: String? = null): Gen<Vector4> = gen(name, VarType.Float4, 16, 16)

    protected fun mat3(name: String? = null): Gen<Matrix4> = gen(name, VarType.Mat3, 36, 48)
    protected fun mat4(name: String? = null): Gen<Matrix4> = gen(name, VarType.Mat4, 64, 64)
    //protected fun <T> array(size: Int, gen: Gen<T>): Gen<Array<T>> = TODO()

    fun <T> gen(name: String? = null, type: VarType, size: Int, align: Int = size): Gen<T> =
        Gen(this, name, layout.rawAlloc(size, align), lastIndex++, type)

    override fun toString(): String = "UniformBlock[${this::class.portableSimpleName}][${uniforms.joinToString(", ")}, fixedLocation=$fixedLocation]"

    class Gen<T>(val block: UniformBlock, var name: String?, val voffset: Int, val vindex: Int, val type: VarType) {
        val uniform: TypedUniform<T> by lazy {
            TypedUniform<T>(name ?: "unknown${block.uniformCount}", voffset, vindex, block, type).also { block._items.add(it) }
        }

        operator fun provideDelegate(block: UniformBlock, property: KProperty<*>): TypedUniform<T> {
            this.name = this.name ?: property.name
            return uniform
        }
    }
}

class UniformsRef(
    val block: UniformBlock,
    var buffer: Buffer,
    var index: Int
) {
    constructor(block: UniformBlock, size: Int = 1, index: Int = 0) : this(
        block, Buffer(block.totalSize * size),
        index
    )

    val blockSize: Int = block.totalSize
    protected fun getOffset(uniform: TypedUniform<*>): Int = (index * blockSize) + uniform.voffset

    fun <T : UniformBlock> copyFrom(ref: UniformBlockBuffer<T>) {
        arraycopy(ref.buffer, 0, this.buffer, (index * blockSize), blockSize)
    }

    // @TODO: Remaining get functions
    operator fun get(uniform: TypedUniform<Int>): Int = getOffset(uniform).let { buffer.getUnalignedInt32(it) }
    operator fun get(uniform: TypedUniform<Float>): Float = getOffset(uniform).let { buffer.getUnalignedFloat32(it + 0) }
    operator fun get(uniform: TypedUniform<Vector2>): Vector2 = getOffset(uniform).let { Vector2(buffer.getUnalignedFloat32(it + 0), buffer.getUnalignedFloat32(it + 4)) }
    operator fun get(uniform: TypedUniform<Vector3>): Vector3 = getOffset(uniform).let { Vector3(buffer.getUnalignedFloat32(it + 0), buffer.getUnalignedFloat32(it + 4), buffer.getUnalignedFloat32(it + 8)) }
    operator fun get(uniform: TypedUniform<Vector4>): Vector4 = getOffset(uniform).let { Vector4(buffer.getUnalignedFloat32(it + 0), buffer.getUnalignedFloat32(it + 4), buffer.getUnalignedFloat32(it + 8), buffer.getUnalignedFloat32(it + 12)) }
    //operator fun get(uniform: TypedUniform<Matrix4>): Matrix4 = TODO()

    operator fun set(uniform: TypedUniform<Int>, value: Int) {
        getOffset(uniform).also { buffer.setUnalignedInt32(it, value) }
    }
    operator fun set(uniform: TypedUniform<Float>, value: Boolean) = set(uniform, if (value) 1f else 0f)
    operator fun set(uniform: TypedUniform<Float>, value: Double) = set(uniform, value.toFloat())
    operator fun set(uniform: TypedUniform<Point>, value: Point) = set(uniform, value.x, value.y)
    operator fun set(uniform: TypedUniform<Point>, value: Size) = set(uniform, value.width, value.height)
    operator fun set(uniform: TypedUniform<Vector4>, value: RGBA) = set(uniform, value.rf, value.gf, value.bf, value.af)
    operator fun set(uniform: TypedUniform<Vector4>, value: RGBAPremultiplied) = set(uniform, value.rf, value.gf, value.bf, value.af)
    operator fun set(uniform: TypedUniform<Vector4>, value: ColorAdd) = set(uniform, value.rf, value.gf, value.bf, value.af)
    operator fun set(uniform: TypedUniform<Vector4>, value: Vector4) = set(uniform, value.x, value.y, value.z, value.w)
    operator fun set(uniform: TypedUniform<Vector4>, value: RectCorners) = set(uniform, value.bottomRight, value.topRight, value.bottomLeft, value.topLeft)
    operator fun set(uniform: TypedUniform<Matrix4>, value: Matrix4) {
        when (uniform.type) {
            VarType.Mat4 -> set(uniform, value, Matrix4.INDICES_BY_COLUMNS_4x4)
            VarType.Mat3 -> set(uniform, value, Matrix4.INDICES_BY_COLUMNS_3x3)
            else -> TODO()
        }
    }

    fun set(uniform: TypedUniform<Matrix4>, value: Matrix4, indices: IntArray) {
        getOffset(uniform).also {
            //println("SET OFFSET: $it")
            for (n in 0 until indices.size) buffer.setUnalignedFloat32(it + n * 4, value.getAtIndex(indices[n]))
        }
    }
    fun set(uniform: TypedUniform<Matrix4>, value: FloatArray, indices: IntArray) {
        getOffset(uniform).also {
            for (n in 0 until indices.size) buffer.setUnalignedFloat32(it + n * 4, value[indices[n]])
        }
    }
    operator fun set(uniform: TypedUniform<Float>, value: Float) {
        getOffset(uniform).also {
            buffer.setUnalignedFloat32(it + 0, value)
        }
    }
    operator fun set(uniform: TypedUniform<Point>, x: Float, y: Float) {
        getOffset(uniform).also {
            buffer.setUnalignedFloat32(it + 0, x)
            buffer.setUnalignedFloat32(it + 4, y)
        }
    }
    fun set(uniform: TypedUniform<Vector4>, x: Float, y: Float, z: Float, w: Float) {
        getOffset(uniform).also {
            buffer.setUnalignedFloat32(it + 0, x)
            buffer.setUnalignedFloat32(it + 4, y)
            buffer.setUnalignedFloat32(it + 8, z)
            buffer.setUnalignedFloat32(it + 12, w)
        }
    }

    //@Deprecated("")
    //fun set(uniform: TypedUniform<Int>, tex: AGTexture?, samplerInfo: AGTextureUnitInfo = AGTextureUnitInfo.DEFAULT) {
    //    buffer.setUnalignedInt32(getOffset(uniform), -1)
    //    textures[index * blockSize + uniform.vindex] = tex
    //    texturesInfo[index * blockSize + uniform.vindex] = samplerInfo.data
    //}
}

class UniformBlockBuffer<T : UniformBlock>(val block: T) {
    companion object {
        inline fun <T : UniformBlock> single(block: T, gen: T.(UniformsRef) -> Unit): UniformBlockBuffer<T> =
            UniformBlockBuffer(block).also { it.reset() }.also { it.push { gen(it) } }
    }

    val agBuffer = AGBuffer()
    val blockSize = block.totalSize
    val texBlockSize = block.uniforms.size
    @PublishedApi internal var buffer = Buffer(blockSize * 1)
    private val bufferSize: Int get() = buffer.sizeInBytes / blockSize
    val current = UniformsRef(block, buffer, -1)
    var currentIndex by current::index
    val size: Int get() = currentIndex + 1
    val data = Buffer(block.totalSize)
    //val values by lazy { block.uniforms.map { AGUniformValue(it.uniform) } }

    @PublishedApi internal fun ensure(index: Int) {
        if (index >= bufferSize - 1) {
            val newCapacity = kotlin.math.max(index + 1, (index + 2) * 3)
            buffer = buffer.copyOf(blockSize * newCapacity)
            current.buffer = buffer
        }
    }

    fun reset() {
        currentIndex = -1
        //arrayfill(buffer, 0) // @TODO: This shouldn't be necessary
        //buffer = Buffer(block.totalSize * 1)
    }

    fun upload(): UniformBlockBuffer<T> {
        agBuffer.upload(buffer, 0, kotlin.math.max(0, (currentIndex + 1) * block.totalSize))
        return this
    }

    fun pop() {
        currentIndex--
    }

    inline fun pushTemp(block: T.(UniformsRef) -> Unit, use: () -> Unit) {
        val pushed = push(deduplicate = true, block)
        try {
            use()
        } finally {
            if (pushed) {
                pop()
            }
        }
    }

    inline fun push(deduplicate: Boolean = true, block: T.(UniformsRef) -> Unit): Boolean {
        currentIndex++
        ensure(currentIndex + 1)
        val blockSize = this.blockSize
        val index0 = (currentIndex - 1) * blockSize
        val index1 = currentIndex * blockSize

        val texBlockSize = this.block.uniforms.size
        val texIndex0 = (currentIndex - 1) * texBlockSize
        val texIndex1 = currentIndex * texBlockSize
        if (currentIndex > 0) {
            arraycopy(buffer, index0, buffer, index1, blockSize)
        } else {
            arrayfill(buffer, 0, 0, blockSize)
        }
        block(this.block, current)
        if (deduplicate && currentIndex >= 1) {
            //println(buffer.hex())
            //println(buffer.slice(0, 128).hex())
            //println(buffer.slice(128, 256).hex())
            val equals = arrayequal(buffer, index0, buffer, index1, blockSize)
            if (equals) {
                currentIndex--
                return false
            }
        }
        return true
    }
}
