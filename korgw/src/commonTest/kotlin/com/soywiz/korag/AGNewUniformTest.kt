package com.soywiz.korag

import com.soywiz.kmem.*
import com.soywiz.kmem.dyn.*
import com.soywiz.korag.shader.*
import com.soywiz.korma.geom.*
import kotlin.reflect.*
import kotlin.test.*

class AGNewUniformTest {
    // from korge
    object ProjViewUB : NewUniformBlock(fixedLocation = 0) {
        val u_ProjMat by mat4()
        val u_ViewMat by mat4()
    }

    @Test
    fun testBlockLayout() {
        assertEquals(ProjViewUB, ProjViewUB.u_ProjMat.block)
        assertEquals(ProjViewUB::u_ProjMat.name, ProjViewUB.u_ProjMat.name)
        assertEquals(ProjViewUB::u_ViewMat.name, ProjViewUB.u_ViewMat.name)
        assertEquals(0, ProjViewUB.u_ProjMat.offset)
        assertEquals(64, ProjViewUB.u_ViewMat.offset)
        assertEquals(128, ProjViewUB.size)
        assertEquals(listOf(ProjViewUB.u_ProjMat, ProjViewUB.u_ViewMat), ProjViewUB.items)
    }

    @Test
    fun testWrite() {
        val ref = NewUniformRef(ProjViewUB, Buffer(ProjViewUB.size), 0)
        val ref2 = NewUniformRef(ProjViewUB, Buffer(ProjViewUB.size), 0)
        ref[ProjViewUB.u_ProjMat] = MMatrix4().setColumns4x4(FloatArray(16) { it.toFloat() }, 0)
        ref[ProjViewUB.u_ViewMat] = MMatrix4().setColumns4x4(FloatArray(16) { -it.toFloat() }, 0)
        assertEquals(
            "000000000000803f0000004000004040000080400000a0400000c0400000e040000000410000104100002041000030410000404100005041000060410000704100000080000080bf000000c0000040c0000080c00000a0c00000c0c00000e0c0000000c1000010c1000020c1000030c1000040c1000050c1000060c1000070c1",
            ref.buffer.hex()
        )
        assertEquals(false, arrayequal(ref.buffer, 0, ref2.buffer, 0, ref.buffer.size))
        arraycopy(ref.buffer, 0, ref2.buffer, 0, ref.buffer.size)
        assertEquals(true, arrayequal(ref.buffer, 0, ref2.buffer, 0, ref.buffer.size))
    }
}

@Suppress("unused")
class NewTypedUniform<T>(val name: String, val offset: Int, val block: NewUniformBlock, val uniform: Uniform) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): NewTypedUniform<T> = this
}

open class NewUniformBlock(val fixedLocation: Int) {
    private val layout = KMemLayoutBuilder()
    private val _items = arrayListOf<NewTypedUniform<*>>()
    val items: List<NewTypedUniform<*>> get() = _items
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

class NewUniformRef(val block: NewUniformBlock, var buffer: Buffer, var offset: Int) {
    fun getOffset(uniform: NewTypedUniform<*>): Int = offset * block.size + uniform.offset

    operator fun set(uniform: NewTypedUniform<Int>, value: Int) {
        getOffset(uniform).also { buffer.setInt32(it, value) }
    }
    operator fun set(uniform: NewTypedUniform<Point>, value: Point) = set(uniform, value.x, value.y)
    operator fun set(uniform: NewTypedUniform<MVector4>, value: Vector4) = set(uniform, value.x, value.y, value.z, value.w)
    operator fun set(uniform: NewTypedUniform<MVector4>, value: MVector4) = set(uniform, value.x, value.y, value.z, value.w)
    operator fun set(uniform: NewTypedUniform<MMatrix4>, value: MMatrix4) = set(uniform, value.data)

    operator fun set(uniform: NewTypedUniform<MMatrix4>, value: FloatArray) {
        getOffset(uniform).also { buffer.setUnalignedArrayFloat32(it, value, 0, 16) }
    }
    operator fun set(uniform: NewTypedUniform<Point>, value: Float) {
        getOffset(uniform).also {
            buffer.setFloat32(it + 0, value)
        }
    }
    operator fun set(uniform: NewTypedUniform<Point>, x: Float, y: Float) {
        getOffset(uniform).also {
            buffer.setFloat32(it + 0, x)
            buffer.setFloat32(it + 4, y)
        }
    }
    operator fun set(uniform: NewTypedUniform<MVector4>, x: Float, y: Float, z: Float, w: Float) {
        getOffset(uniform).also {
            buffer.setFloat32(it + 0, x)
            buffer.setFloat32(it + 4, y)
            buffer.setFloat32(it + 8, z)
            buffer.setFloat32(it + 12, w)
        }
    }
}
