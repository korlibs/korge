package korlibs.render

import korlibs.graphics.shader.*

class AGUniformsTest {
    object UB : UniformBlock(fixedLocation = 0) {
        val projMatrix by mat4()
        val viewMatrix by mat4()
        val color1 by ubyte4()
    }

    //@Test
    //fun testUniformBlockDataAndBuffer() {
    //    val projMatrix = UB.projMatrix.uniform
    //    val viewMatrix = UB.viewMatrix.uniform
    //    val color1 = UB.color1.uniform
    //    //val block = UB.uniformBlock
//
    //    val data = UniformBlockData(block)
    //    val buffer = UniformBlockBuffer(block, 2)
    //    data[projMatrix].set(MMatrix3D().multiply(2f))
    //    val index1 = buffer.put(data)
    //    data[projMatrix].set(MMatrix3D().multiply(3f))
    //    val index2 = buffer.put(data)
    //    //println(buffer.buffer.f32.toFloatArray().toList())
//
    //    assertEquals(listOf(0, 1), listOf(index1, index2))
//
    //    assertEquals(MMatrix3D().multiply(2f), MMatrix3D().setColumns4x4(buffer[index1][projMatrix].data.f32.toFloatArray(), 0))
    //    assertEquals(MMatrix3D().multiply(3f), MMatrix3D().setColumns4x4(buffer[index2][projMatrix].data.f32.toFloatArray(), 0))
//
    //    //block.attributePositions
    //    //println(block.totalSize)
    //}

}