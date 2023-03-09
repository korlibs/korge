package com.soywiz.korag

import com.soywiz.kmem.*
import com.soywiz.korag.shader.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class AGUniformBlockTest {
    val uniform1 by Uniform(VarType.Bool4)
    val uniform2 by Uniform(VarType.Short2)
    val uniform3 by Uniform(VarType.Int1)
    val uniformBlock = UniformBlock(uniform1, uniform2, fixedLocation = 2)

    @Test
    fun test() {
        val data = UniformBlockData(uniformBlock)
        data[uniform1].set(1f, 0f, 0f, 1f)
        data[uniform2].set(7033f, 9999f)
        assertFails { data[uniform3] }
        assertEquals("01000001791b0f27", data.data.hex())
    }

    @Test
    fun testProgram() {
        val program = Program(
            vertex = VertexShaderDefault {
                SET(v_Tex, a_Tex)
                SET(v_Col, a_Col)
                SET(out, u_ProjMat * u_ViewMat * vec4(a_Pos, 0f.lit, 1f.lit))
            },
            fragment = FragmentShaderDefault {
                SET(out, texture2D(v_Tex, u_Tex))
            }
        )
        val data = ProgramWithUniforms(program)
        data[DefaultShaders.u_ProjMat].set(MMatrix4().setToOrtho(0f, 0f, 100f, 100f))
        data[DefaultShaders.u_ViewMat].set(MMatrix4().identity())
        data[DefaultShaders.u_Tex].set(1)
        println(data.uniformsBlocksData.map { it?.data?.hex() })
        println(data)
    }
}

class ProgramWithUniforms(val program: Program) {
    val uniformLayouts = program.uniforms.map { it.linkedLayout as? UniformBlock? }.distinct().filterNotNull()
    val maxLocation = uniformLayouts.maxOf { it?.fixedLocation?.plus(1) ?: -1 }
    val uniformsBlocks = Array<UniformBlock?>(maxLocation) { null }.also {
        for (layout in uniformLayouts) {
            it[layout.fixedLocation] = layout
        }
    }
    val uniformsBlocksData = Array<UniformBlockData?>(uniformsBlocks.size) {
        uniformsBlocks[it]?.let { UniformBlockData(it) }
    }
    operator fun get(block: UniformBlock): UniformBlockData {
        val rblock = uniformsBlocks.getOrNull(block.fixedLocation) ?: error("Can't find block")
        if (rblock !== block) error("Block $block not used in program")
        return uniformsBlocksData[block.fixedLocation]!!
    }

    operator fun invoke(ublock: UniformBlock, block: (UniformBlockData) -> Unit) {
        block(this[ublock])
    }

    operator fun get(uniform: Uniform): AGUniformValue = this[(uniform.linkedLayout as UniformBlock)][uniform]
}
