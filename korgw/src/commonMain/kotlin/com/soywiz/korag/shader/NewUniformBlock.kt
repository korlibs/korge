package com.soywiz.korag.shader

import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.kmem.dyn.*
import com.soywiz.korag.*
import com.soywiz.korim.color.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import kotlin.reflect.*

@Suppress("unused")
class NewTypedUniform<T>(name: String, val voffset: Int, var vindex: Int, val block: NewUniformBlock, type: VarType)
    : VariableWithOffset(name, type, 1, Precision.DEFAULT)
{
    val uniform: Uniform = Uniform(name, type, 1, typedUniform = this)
    val varType: VarType get() = uniform.type
    operator fun getValue(thisRef: Any?, property: KProperty<*>): NewTypedUniform<T> = this
    override fun toString(): String = "TypedUniform(name='$name', offset=$voffset, type=$type)"
}

open class NewUniformBlock(val fixedLocation: Int) {
    private val layout = KMemLayoutBuilder()
    private val _items = arrayListOf<NewTypedUniform<*>>()
    private var lastIndex = 0
    val uniforms: List<NewTypedUniform<*>> get() = _items
    val totalSize: Int get() = layout.size
    /*
    @Deprecated("")
    val uniformBlock: UniformBlock by lazy {
        UniformBlock(*uniforms.map { it.uniform }.toTypedArray(), fixedLocation = fixedLocation).also {
            for (n in uniforms.indices) {
                val item = uniforms[n]
                //println("item=$item, voffset=${item.voffset}")
                item.uniform.linkedOffset = item.voffset
                item.uniform.linkedLayout = it
                item.uniform.linkedIndex = n
                item.linkedOffset = item.voffset
                item.linkedLayout = it
                item.linkedIndex = n
            }
        }.also {
            //println("it.totalSize=${it.totalSize}")
            //println("totalSize=${totalSize}")
        }
    }

     */

    // @TODO: Fix alignment
    protected fun bool(name: String? = null): Gen<List<Boolean>> = Gen(name, layout.rawAlloc(1, 1), lastIndex++, VarType.Bool1)
    protected fun bool2(name: String? = null): Gen<List<Boolean>> = Gen(name, layout.rawAlloc(2, 1), lastIndex++, VarType.Bool2)
    protected fun bool3(name: String? = null): Gen<List<Boolean>> = Gen(name, layout.rawAlloc(3, 1), lastIndex++, VarType.Bool3)
    protected fun bool4(name: String? = null): Gen<List<Boolean>> = Gen(name, layout.rawAlloc(4, 1), lastIndex++, VarType.Bool4)

    protected fun ubyte4(name: String? = null): Gen<Int> = Gen(name, layout.rawAlloc(4, 1), lastIndex++, VarType.UByte4)

    protected fun short(name: String? = null): Gen<List<Boolean>> = Gen(name, layout.rawAlloc(2, 2), lastIndex++, VarType.Short1)
    protected fun short2(name: String? = null): Gen<List<Boolean>> = Gen(name, layout.rawAlloc(4, 2), lastIndex++, VarType.Short2)
    protected fun short3(name: String? = null): Gen<List<Boolean>> = Gen(name, layout.rawAlloc(6, 2), lastIndex++, VarType.Short3)
    protected fun short4(name: String? = null): Gen<List<Boolean>> = Gen(name, layout.rawAlloc(8, 2), lastIndex++, VarType.Short4)

    //= Uniform("u_Tex", VarType.Sampler2D)
    protected fun sampler2D(name: String? = null): Gen<Int> = Gen(name, layout.rawAlloc(4, 4), lastIndex++, VarType.Sampler2D)
    protected fun int(name: String? = null): Gen<Int> = Gen(name, layout.rawAlloc(4, 4), lastIndex++, VarType.SInt1)
    protected fun ivec2(name: String? = null): Gen<PointInt> = Gen(name, layout.rawAlloc(8, 4), lastIndex++, VarType.SInt2)
    protected fun float(name: String? = null): Gen<Float> = Gen(name, layout.rawAlloc(4, 4), lastIndex++, VarType.Float1)
    protected fun vec2(name: String? = null): Gen<Vector2> = Gen(name, layout.rawAlloc(8, 4), lastIndex++, VarType.Float2)
    // @TODO: Some drivers get this wrong
    //protected fun vec3(name: String? = null): Gen<MVector3> = Gen(name, layout.rawAlloc(12, 4), lastIndex++, VarType.Float3)
    protected fun vec4(name: String? = null): Gen<MVector4> = Gen(name, layout.rawAlloc(16, 4), lastIndex++, VarType.Float4)
    protected fun mat3(name: String? = null): Gen<MMatrix4> = Gen(name, layout.rawAlloc(36, 4), lastIndex++, VarType.Mat3)
    protected fun mat4(name: String? = null): Gen<MMatrix4> = Gen(name, layout.rawAlloc(64, 4), lastIndex++, VarType.Mat4)
    //protected fun <T> array(size: Int, gen: Gen<T>): Gen<Array<T>> = TODO()

    override fun toString(): String = "VertexLayout[${uniforms.joinToString(", ")}, fixedLocation=$fixedLocation]"

    class Gen<T>(val name: String?, val voffset: Int, val vindex: Int, val type: VarType) {
        lateinit var uniform: NewTypedUniform<T>

        operator fun provideDelegate(block: NewUniformBlock, property: KProperty<*>): NewTypedUniform<T> {
            val finalName = name ?: property.name
            uniform = NewTypedUniform<T>(finalName, voffset, vindex, block, type)
            block._items.add(uniform)
            return uniform
        }
    }
}

class NewUniformRef(val block: NewUniformBlock, var buffer: Buffer, var textures: Array<AGTexture?>, var index: Int) {
    val blockSize: Int = block.totalSize
    protected fun getOffset(uniform: NewTypedUniform<*>): Int = (index * blockSize) + uniform.voffset

    operator fun set(uniform: NewTypedUniform<Int>, value: Int) {
        getOffset(uniform).also { buffer.setInt32(it, value) }
    }
    operator fun set(uniform: NewTypedUniform<Float>, value: Boolean) = set(uniform, if (value) 1f else 0f)
    operator fun set(uniform: NewTypedUniform<Float>, value: Double) = set(uniform, value.toFloat())
    operator fun set(uniform: NewTypedUniform<Point>, value: Point) = set(uniform, value.x, value.y)
    operator fun set(uniform: NewTypedUniform<Point>, value: Size) = set(uniform, value.width, value.height)
    operator fun set(uniform: NewTypedUniform<MVector4>, value: RGBA) = set(uniform, value.rf, value.gf, value.bf, value.af)
    operator fun set(uniform: NewTypedUniform<MVector4>, value: RGBAPremultiplied) = set(uniform, value.rf, value.gf, value.bf, value.af)
    operator fun set(uniform: NewTypedUniform<MVector4>, value: ColorAdd) = set(uniform, value.rf, value.gf, value.bf, value.af)
    operator fun set(uniform: NewTypedUniform<MVector4>, value: Vector4) = set(uniform, value.x, value.y, value.z, value.w)
    operator fun set(uniform: NewTypedUniform<MVector4>, value: RectCorners) = set(uniform, value.bottomRight, value.topRight, value.bottomLeft, value.topLeft)
    operator fun set(uniform: NewTypedUniform<MVector4>, value: MVector4) = set(uniform, value.x, value.y, value.z, value.w)
    operator fun set(uniform: NewTypedUniform<MMatrix4>, value: MMatrix4) {
        if (uniform.type != VarType.Mat4) TODO()
        set(uniform, value.data)
    }
    operator fun set(uniform: NewTypedUniform<MMatrix4>, value: Matrix4) {
        getOffset(uniform).also {
            //println("SET OFFSET: $it")
            when (uniform.type) {
                VarType.Mat4 -> {
                    for (n in 0 until 16) buffer.setUnalignedFloat32(it + (n * 4), value.getAtIndex(n))
                }
                else -> TODO()
            }
        }
    }

    operator fun set(uniform: NewTypedUniform<MMatrix4>, value: FloatArray) {
        getOffset(uniform).also { buffer.setUnalignedArrayFloat32(it, value, 0, 16) }
    }
    operator fun set(uniform: NewTypedUniform<Float>, value: Float) {
        getOffset(uniform).also {
            buffer.setUnalignedFloat32(it + 0, value)
        }
    }
    operator fun set(uniform: NewTypedUniform<Point>, x: Float, y: Float) {
        getOffset(uniform).also {
            buffer.setUnalignedFloat32(it + 0, x)
            buffer.setUnalignedFloat32(it + 4, y)
        }
    }
    fun set(uniform: NewTypedUniform<MVector4>, x: Float, y: Float, z: Float, w: Float) {
        getOffset(uniform).also {
            buffer.setUnalignedFloat32(it + 0, x)
            buffer.setUnalignedFloat32(it + 4, y)
            buffer.setUnalignedFloat32(it + 8, z)
            buffer.setUnalignedFloat32(it + 12, w)
        }
    }

    fun set(uniform: NewTypedUniform<Int>, tex: AGTexture?, samplerInfo: AGTextureUnitInfo = AGTextureUnitInfo.DEFAULT) {
        buffer.setUnalignedInt32(getOffset(uniform), samplerInfo.data)
        textures[index * blockSize + uniform.vindex] = tex
        //TODO()
    }
}

class NewUniformBlockBuffer<T : NewUniformBlock>(val block: T) {
    val agBuffer = AGBuffer()

    val blockSize = block.totalSize
    val texBlockSize = block.uniforms.size
    @PublishedApi internal var buffer = Buffer(blockSize * 1)
    @PublishedApi internal var textures = arrayOfNulls<AGTexture?>(texBlockSize * 1)
    private val bufferSize: Int get() = buffer.sizeInBytes / blockSize
    val current = NewUniformRef(block, buffer, textures, -1)
    var currentIndex by current::index
    val size: Int get() = currentIndex + 1

    @PublishedApi internal fun ensure(index: Int) {
        if (index >= bufferSize - 1) {
            val newCapacity = kotlin.math.max(index + 1, (index + 2) * 3)
            buffer = buffer.copyOf(blockSize * newCapacity)
            textures = textures.copyOf(texBlockSize * newCapacity)
            current.buffer = buffer
            current.textures = textures
        }
    }

    fun reset() {
        currentIndex = -1
        //arrayfill(buffer, 0) // @TODO: This shouldn't be necessary
        //buffer = Buffer(block.totalSize * 1)
    }

    fun upload(): NewUniformBlockBuffer<T> {
        agBuffer.upload(buffer, 0, kotlin.math.max(0, (currentIndex + 1) * block.totalSize))
        return this
    }

    fun pop() {
        currentIndex--
    }

    inline fun push(deduplicate: Boolean = true, block: T.(NewUniformRef) -> Unit): Boolean {
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
            arraycopy(textures, texIndex0, textures, texIndex1, texBlockSize)
        } else {
            arrayfill(buffer, 0, 0, blockSize)
            arrayfill(textures, null, 0, texBlockSize)
        }
        block(this.block, current)
        if (deduplicate && currentIndex >= 1) {
            //println(buffer.hex())
            //println(buffer.slice(0, 128).hex())
            //println(buffer.slice(128, 256).hex())
            val equals = arrayequal(buffer, index0, buffer, index1, blockSize)
                && arrayequal(textures, texIndex0, textures, texIndex1, texBlockSize)
            if (equals) {
                currentIndex--
                return false
            }
        }
        return true
    }

    val data = Buffer(block.totalSize)
    //val values by lazy { block.uniforms.map { AGUniformValue(it.uniform) } }
    val values = block.uniforms.map { uniform ->
        AGUniformValue(uniform.uniform, data.sliceWithSize(uniform.voffset, uniform.type.bytesSize), null, AGTextureUnitInfo.DEFAULT)
    }

    fun readFrom(buffer: Buffer, textures: Array<AGTexture?>?, offset: Int) {
        if (textures != null) {
            for (n in values.indices) {
                values[n].set(textures[n], AGTextureUnitInfo.DEFAULT)
                //values[n].texture = textures[n]
                //values[n].textureUnitInfo = AGTextureUnitInfo.DEFAULT // @TODO
            }
        }
        arraycopy(buffer, offset, data, 0, block.totalSize)
    }
}
