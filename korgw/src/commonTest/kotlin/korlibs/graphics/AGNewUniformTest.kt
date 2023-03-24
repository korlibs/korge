package korlibs.graphics

import korlibs.memory.*
import korlibs.graphics.shader.*
import korlibs.math.geom.*
import kotlin.test.*

class AGNewUniformTest {
    // from korge
    object ProjViewUB : UniformBlock(fixedLocation = 0) {
        val u_ProjMat by mat4()
        val u_ViewMat by mat4()
    }

    @Test
    fun testBlockLayout() {
        assertEquals(ProjViewUB, ProjViewUB.u_ProjMat.block)
        assertEquals(ProjViewUB::u_ProjMat.name, ProjViewUB.u_ProjMat.name)
        assertEquals(ProjViewUB::u_ViewMat.name, ProjViewUB.u_ViewMat.name)
        assertEquals(0, ProjViewUB.u_ProjMat.voffset)
        assertEquals(64, ProjViewUB.u_ViewMat.voffset)
        assertEquals(128, ProjViewUB.totalSize)
        assertEquals(listOf(ProjViewUB.u_ProjMat, ProjViewUB.u_ViewMat), ProjViewUB.uniforms)
    }

    @Test
    fun testWrite() {
        val ref = UniformRef(ProjViewUB)
        val ref2 = UniformRef(ProjViewUB)
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

    @Test
    fun testWriteBlock() {
        val buffer = UniformBlockBuffer(ProjViewUB)
        assertEquals(0, buffer.size)
        buffer.push(deduplicate = true) {
            it[u_ProjMat] = Matrix4.IDENTITY
        }
        assertEquals(1, buffer.size)
        buffer.push(deduplicate = true) {
            it[u_ProjMat] = Matrix4.IDENTITY
        }
        assertEquals(1, buffer.size)
        buffer.push(deduplicate = false) {
            it[u_ProjMat] = Matrix4.IDENTITY
        }
        assertEquals(2, buffer.size)
        buffer.push(deduplicate = true) {
            it[u_ProjMat] = Matrix4.IDENTITY * 2f
        }
        assertEquals(3, buffer.size)
        buffer.push(deduplicate = true) {
            it[u_ProjMat] = Matrix4.IDENTITY * 3f
        }
        assertEquals(4, buffer.size)
    }
}
