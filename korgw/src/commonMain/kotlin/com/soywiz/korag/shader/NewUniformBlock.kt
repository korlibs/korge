package com.soywiz.korag.shader

import com.soywiz.kmem.*
import com.soywiz.kmem.dyn.*
import com.soywiz.korma.geom.*
import kotlin.reflect.*

@Suppress("unused")
class NewTypedUniform<T>(val name: String, val offset: Int, val block: NewUniformBlock, val uniform: Uniform) {
    val type: VarType get() = uniform.type
    operator fun getValue(thisRef: Any?, property: KProperty<*>): NewTypedUniform<T> = this
}

open class NewUniformBlock(val fixedLocation: Int) {
    private val layout = KMemLayoutBuilder()
    private val _items = arrayListOf<NewTypedUniform<*>>()
    val uniforms: List<NewTypedUniform<*>> get() = _items
    val size: Int get() = layout.size

    protected fun int(name: String? = null): Gen<Int> = Gen(name, layout.rawAlloc(4), VarType.SInt1)
    protected fun ivec2(name: String? = null): Gen<PointInt> = Gen(name, layout.rawAlloc(8), VarType.SInt2)
    protected fun float(name: String? = null): Gen<Float> = Gen(name, layout.rawAlloc(4), VarType.Float1)
    protected fun vec2(name: String? = null): Gen<Vector2> = Gen(name, layout.rawAlloc(8), VarType.Float2)
    //protected fun vec3(name: String? = null): Gen<MVector3> = Gen(name, layout.rawAlloc(12), VarType.Float4) } // @TODO: Some drivers get this wrong
    protected fun vec4(name: String? = null): Gen<MVector4> = Gen(name, layout.rawAlloc(16), VarType.Float4)
    protected fun mat4(name: String? = null): Gen<MMatrix4> = Gen(name, layout.rawAlloc(64), VarType.Mat4)
    //protected fun <T> array(size: Int, gen: Gen<T>): Gen<Array<T>> = TODO()

    class Gen<T>(val name: String?, val offset: Int, val type: VarType) {
        lateinit var uniform: NewTypedUniform<T>

        operator fun provideDelegate(block: NewUniformBlock, property: KProperty<*>): NewTypedUniform<T> {
            val finalName = name ?: property.name
            uniform = NewTypedUniform<T>(finalName, offset, block, Uniform(finalName, type))
            block._items.add(uniform)
            return uniform
        }
    }
}

class NewUniformRef(val block: NewUniformBlock, var buffer: Buffer, var index: Int) {
    val blockSize: Int = block.size
    protected fun getOffset(uniform: NewTypedUniform<*>): Int = (index * blockSize) + uniform.offset

    operator fun set(uniform: NewTypedUniform<Int>, value: Int) {
        getOffset(uniform).also { buffer.setInt32(it, value) }
    }
    operator fun set(uniform: NewTypedUniform<Point>, value: Point) = set(uniform, value.x, value.y)
    operator fun set(uniform: NewTypedUniform<MVector4>, value: Vector4) = set(uniform, value.x, value.y, value.z, value.w)
    operator fun set(uniform: NewTypedUniform<MVector4>, value: MVector4) = set(uniform, value.x, value.y, value.z, value.w)
    operator fun set(uniform: NewTypedUniform<MMatrix4>, value: MMatrix4) = set(uniform, value.data)
    operator fun set(uniform: NewTypedUniform<MMatrix4>, value: Matrix4) {
        getOffset(uniform).also {
            //println("SET OFFSET: $it")
            for (n in 0 until 16) buffer.setUnalignedFloat32(it + (n * 4), value.getAtIndex(n))
        }
    }

    operator fun set(uniform: NewTypedUniform<MMatrix4>, value: FloatArray) {
        getOffset(uniform).also { buffer.setUnalignedArrayFloat32(it, value, 0, 16) }
    }
    operator fun set(uniform: NewTypedUniform<Point>, value: Float) {
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
    operator fun set(uniform: NewTypedUniform<MVector4>, x: Float, y: Float, z: Float, w: Float) {
        getOffset(uniform).also {
            buffer.setUnalignedFloat32(it + 0, x)
            buffer.setUnalignedFloat32(it + 4, y)
            buffer.setUnalignedFloat32(it + 8, z)
            buffer.setUnalignedFloat32(it + 12, w)
        }
    }
}

class NewUniformBlockBuffer(val block: NewUniformBlock) {
    @PublishedApi internal var buffer = Buffer(block.size * 1)
    private val bufferSize: Int get() = buffer.sizeInBytes / block.size
    val current = NewUniformRef(block, buffer, -1)
    var currentIndex by current::index
    val size: Int get() = currentIndex + 1

    @PublishedApi internal fun ensure(index: Int) {
        if (index >= bufferSize - 1) {
            val newCapacity = kotlin.math.max(index + 1, (index + 2) * 3)
            buffer = buffer.copyOf(block.size * newCapacity)
            current.buffer = buffer
        }
    }

    fun reset() {
        currentIndex = -1
    }

    inline fun add(deduplicate: Boolean = true, block: (NewUniformRef) -> Unit): Boolean {
        currentIndex++
        ensure(currentIndex + 1)
        val blockSize = this.block.size
        val index0 = (currentIndex - 1) * blockSize
        val index1 = currentIndex * blockSize
        if (currentIndex > 0) {
            //arrayequal(buffer, index0, buffer, index1, blockSize)
        } else {
            //arrayfill(buffer, 0, index1, blockSize)
        }
        block(current)
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
