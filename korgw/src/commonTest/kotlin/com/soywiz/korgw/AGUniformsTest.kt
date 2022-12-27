package com.soywiz.korgw

import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class AGUniformsTest {
    @Test
    fun testUniformBlockDataAndBuffer() {
        val projMatrix by Uniform(VarType.Mat4)
        val viewMatrix by Uniform(VarType.Mat4)
        val color1 by Uniform(VarType.UByte4)
        val block = UniformBlock(projMatrix, viewMatrix, color1, layoutSize = null)
        val data = UniformBlockData(block)
        val buffer = UniformBlockBuffer(block, 2)
        data[projMatrix].set(Matrix3D().multiply(2f))
        val index1 = buffer.put(data)
        data[projMatrix].set(Matrix3D().multiply(3f))
        val index2 = buffer.put(data)
        //println(buffer.buffer.f32.toFloatArray().toList())

        AGUniformBlockValues(listOf(buffer, buffer), intArrayOf(0, 1)).forEachValue {
            println(it)
        }

        assertEquals(listOf(0, 1), listOf(index1, index2))

        buffer.copyIndexTo(index1, data)
        assertEquals(Matrix3D().multiply(2f), Matrix3D().setColumns4x4(data[projMatrix].data.f32.toFloatArray(), 0))

        buffer.copyIndexTo(index2, data)
        assertEquals(Matrix3D().multiply(3f), Matrix3D().setColumns4x4(data[projMatrix].data.f32.toFloatArray(), 0))

        //block.attributePositions
        //println(block.totalSize)
    }

}
