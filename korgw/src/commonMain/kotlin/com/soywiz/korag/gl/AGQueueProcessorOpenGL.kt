package com.soywiz.korag.gl

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kgl.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import com.soywiz.korio.annotations.*
import com.soywiz.korio.lang.*

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
    private var currentProgram: GLProgramInfo? = null

    override fun programCreate(programId: Int, program: Program) {
        programs[programId] = GLShaderCompiler.programCreate(gl, config, program)
    }

    override fun programDelete(programId: Int) {
        val program = programs[programId]
        program?.delete(gl)
        programs.remove(programId)
        if (currentProgram === program) {
            currentProgram = null
        }
    }

    override fun programUse(programId: Int) {
        val program = programs[programId]
        program?.use(gl)
        currentProgram = program
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

    ///////////////////////////////////////
    // UNIFORMS
    ///////////////////////////////////////
    override fun uniformsSet(layout: UniformLayout, data: FBuffer) {
        layout.attributes.fastForEach {
            //currentProgram
        }
        TODO()
    }

    override fun depthMask(depth: Boolean) {
        gl.depthMask(depth)
    }

    override fun depthRange(near: Float, far: Float) {
        gl.depthRangef(near, far)
    }

    override fun stencilFunction(compareMode: AG.CompareMode, referenceValue: Int, readMask: Int) {
        gl.stencilFunc(compareMode.toGl(), referenceValue, readMask)
    }

    // @TODO: Separate
    override fun stencilOperation(actionOnDepthFail: AG.StencilOp, actionOnDepthPassStencilFail: AG.StencilOp, actionOnBothPass: AG.StencilOp) {
        gl.stencilOp(actionOnDepthFail.toGl(), actionOnDepthPassStencilFail.toGl(), actionOnBothPass.toGl())
    }

    // @TODO: Separate
    override fun stencilMask(writeMask: Int) {
        gl.stencilMask(writeMask)
    }

    override fun scissor(x: Int, y: Int, width: Int, height: Int) {
        gl.scissor(x, y, width, height)
    }

    override fun clear(color: Boolean, depth: Boolean, stencil: Boolean) {
        var mask = 0
        if (color) mask = mask or KmlGl.COLOR_BUFFER_BIT
        if (depth) mask = mask or KmlGl.DEPTH_BUFFER_BIT
        if (stencil) mask = mask or KmlGl.STENCIL_BUFFER_BIT
        gl.clear(mask)
    }

    override fun clearColor(red: Float, green: Float, blue: Float, alpha: Float) {
        gl.clearColor(red, green, blue, alpha)
    }

    override fun clearDepth(depth: Float) {
        gl.clearDepthf(depth)
    }

    override fun clearStencil(stencil: Int) {
        gl.clearStencil(stencil)
    }

    val vaos = arrayListOf<AG.VertexArrayObject?>()
    //val vaos = IntMap<AG.VertexArrayObject?>()
    var lastUsedVao: AG.VertexArrayObject? = null

    private fun ensureVaoIndex(index: Int): Int {
        while (vaos.size <= index) vaos.add(null)
        return index
    }

    override fun vaoCreate(id: Int) {
    }

    override fun vaoDelete(id: Int) {
        if (id < vaos.size) vaos[id] = null
    }

    override fun vaoSet(id: Int, vao: AG.VertexArrayObject) {
        vaos[ensureVaoIndex(id)] = vao
    }

    override fun vaoUse(id: Int, program: GLProgramInfo?) {
        val prevVao = lastUsedVao
        val vao = vaos.getOrNull(id)
        val cprogram = program ?: currentProgram
        lastUsedVao = vao
        if (vao == null) {
            val rvao = prevVao
            rvao?.list?.fastForEach { entry ->
                val vattrs = entry.layout.attributes
                vattrs.fastForEach { att ->
                    if (att.active) {
                        val loc = att.fixedLocation ?: cprogram?.getAttribLocation(gl, att.name) ?: 0
                        if (loc >= 0) {
                            if (att.divisor != 0) {
                                gl.vertexAttribDivisor(loc, 0)
                            }
                            gl.disableVertexAttribArray(loc)
                        }
                    }
                }
            }
        } else {
            val rvao = vao
            rvao.list.fastForEach { entry ->
                val vertices = entry.buffer as AGOpengl.GlBuffer
                val vertexLayout = entry.layout

                val vattrs = vertexLayout.attributes
                val vattrspos = vertexLayout.attributePositions

                if (vertices.kind != AG.Buffer.Kind.VERTEX) invalidOp("Not a VertexBuffer")

                vertices.bind(gl)
                val totalSize = vertexLayout.totalSize
                for (n in 0 until vattrspos.size) {
                    val att = vattrs[n]
                    if (!att.active) continue
                    val off = vattrspos[n]
                    val loc = att.fixedLocation ?: cprogram?.getAttribLocation(gl, att.name) ?: 0
                    val glElementType = att.type.toGl()
                    val elementCount = att.type.elementCount
                    if (loc >= 0) {
                        gl.enableVertexAttribArray(loc)
                        gl.vertexAttribPointer(loc, elementCount, glElementType, att.normalized, totalSize, off.toLong())
                        if (att.divisor != 0) {
                            gl.vertexAttribDivisor(loc, att.divisor)
                        }
                    }
                }
            }
        }
    }
}
