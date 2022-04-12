package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.kgl.*
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import com.soywiz.korio.annotations.*

@OptIn(KorIncomplete::class, KorInternal::class)
class AGQueueProcessorOpenGL(var gl: KmlGl, var config: GlslConfig = GlslConfig()) : AGQueueProcessor {
    override fun finish() {
        gl.flush()
        gl.finish()
    }

    override fun enableDisable(kind: AGEnable, enable: Boolean) {
        gl.enableDisable(kind.toGl(), enable)
    }

    override fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) {
        gl.colorMask(red, green, blue, alpha)
    }

    override fun blendEquation(rgb: AGBlendEquation, a: AGBlendEquation) {
        gl.blendEquationSeparate(rgb.toGl(), a.toGl())
    }

    override fun blendFunction(srcRgb: AGBlendFactor, dstRgb: AGBlendFactor, srcA: AGBlendFactor, dstA: AGBlendFactor) {
        gl.blendFuncSeparate(srcRgb.toGl(), dstRgb.toGl(), srcA.toGl(), dstA.toGl())
    }

    override fun cullFace(face: AGCullFace) {
        gl.cullFace(face.toGl())
    }

    override fun frontFace(face: AGFrontFace) {
        gl.frontFace(face.toGl())
    }

    override fun depthFunction(depthTest: AGCompareMode) {
        gl.depthFunc(depthTest.toGl())
    }

    ///////////////////////////////////////
    // PROGRAMS
    ///////////////////////////////////////

    private val programs = IntMap<GLProgramInfo>()

    override fun programCreate(programId: Int, program: Program) {
        programs[programId] = GLShaderCompiler.programCreate(gl, config, program)
    }

    override fun programDelete(programId: Int) {
        programs[programId]?.delete(gl)
        programs.remove(programId)
    }

    override fun programUse(programId: Int) {
        programs[programId]?.use(gl)
    }

    ///////////////////////////////////////
    // DRAW
    ///////////////////////////////////////
    override fun draw(type: AGDrawType, vertexCount: Int, offset: Int, instances: Int, indexType: AGIndexType?) {
        if (indexType != null) {
            if (instances != 1) {
                gl.drawElementsInstanced(type.toGl(), vertexCount, indexType.toGl(), offset, instances)
            } else {
                gl.drawElements(type.toGl(), vertexCount, indexType.toGl(), offset)
            }
        } else {
            if (instances != 1) {
                gl.drawArraysInstanced(type.toGl(), offset, vertexCount, instances)
            } else {
                gl.drawArrays(type.toGl(), offset, vertexCount)
            }
        }
    }
}
