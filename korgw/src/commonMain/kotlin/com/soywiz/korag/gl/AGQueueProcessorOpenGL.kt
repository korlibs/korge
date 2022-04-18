package com.soywiz.korag.gl

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kgl.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.internal.*
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import com.soywiz.korio.annotations.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import kotlin.math.*

@OptIn(KorIncomplete::class, KorInternal::class)
class AGQueueProcessorOpenGL(val gl: KmlGl) : AGQueueProcessor {
    val config: GlslConfig = GlslConfig(
        gles = gl.gles,
        android = gl.android,
    )

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

    override fun programCreate(programId: Int, program: Program, config: ProgramConfig?) {
        programs[programId] = GLShaderCompiler.programCreate(
            gl,
            this.config.copy(programConfig = config ?: this.config.programConfig),
            program
        )
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
        programUseExt(programs[programId])
    }

    private fun programUseExt(program: GLProgramInfo?) {
        program?.use(gl)
        currentProgram = program
    }

    ///////////////////////////////////////
    // DRAW
    ///////////////////////////////////////
    override fun draw(
        type: AGDrawType,
        vertexCount: Int,
        offset: Int,
        instances: Int,
        indexType: AGIndexType?,
        indices: AG.Buffer?
    ) {
        (indices as? AGOpengl.GlBuffer?)?.bind(gl)

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
    override fun stencilOperation(
        actionOnDepthFail: AG.StencilOp,
        actionOnDepthPassStencilFail: AG.StencilOp,
        actionOnBothPass: AG.StencilOp
    ) {
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

    override fun vaoUse(id: Int) {
        val prevVao = lastUsedVao
        val vao = vaos.getOrNull(id)
        val cprogram = currentProgram
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
                        gl.vertexAttribPointer(
                            loc,
                            elementCount,
                            glElementType,
                            att.normalized,
                            totalSize,
                            off.toLong()
                        )
                        if (att.divisor != 0) {
                            gl.vertexAttribDivisor(loc, att.divisor)
                        }
                    }
                }
            }
        }
    }

    // UBO

    val ubos = arrayListOf<AG.UniformValues?>()

    private fun ensureUboIndex(index: Int): Int {
        while (ubos.size <= index) ubos.add(null)
        return index
    }


    override fun uboCreate(id: Int) {
    }

    override fun uboDelete(id: Int) {
        if (id < ubos.size) ubos[id] = null
    }

    override fun uboSet(id: Int, ubo: AG.UniformValues) {
        ubos[ensureUboIndex(id)] = ubo
    }

    private val TEMP_MAX_MATRICES = 1024
    val tempBuffer1 = FBuffer(4)
    val tempBuffer = FBuffer(4 * 16 * TEMP_MAX_MATRICES)
    val tempBufferM2 = FBuffer.allocUnaligned(4 * 2 * 2)
    val tempBufferM3 = FBuffer.allocUnaligned(4 * 3 * 3)
    val tempBufferM4 = FBuffer.allocUnaligned(4 * 4 * 4)
    val tempF32 = tempBuffer.f32
    private val tempFloats = FloatArray(16 * TEMP_MAX_MATRICES)
    private val mat3dArray = arrayOf(Matrix3D())

    override fun uboUse(id: Int) {
        val uniforms = ubos[id] ?: return
        val glProgram = currentProgram ?: return

        var textureUnit = 0
        //for ((uniform, value) in uniforms) {
        for (n in 0 until uniforms.uniforms.size) {
            val uniform = uniforms.uniforms[n]
            val uniformName = uniform.name
            val uniformType = uniform.type
            val value = uniforms.values[n]
            val location = glProgram.getUniformLocation(gl, uniformName)
            val declArrayCount = uniform.arrayCount
            val stride = uniform.type.elementCount

            //println("uniform: $uniform, arrayCount=$arrayCount, stride=$stride")

            when (uniformType) {
                VarType.Sampler2D, VarType.SamplerCube -> {
                    val unit = value.fastCastTo<AG.TextureUnit>()
                    gl.activeTexture(KmlGl.TEXTURE0 + textureUnit)

                    val tex = (unit.texture.fastCastTo<AGOpengl.GlTexture?>())
                    tex?.bindEnsuring()
                    tex?.setWrap()
                    tex?.setFilter(unit.linear, unit.trilinear ?: unit.linear)

                    gl.uniform1i(location, textureUnit)
                    //val texBinding = gl.getIntegerv(gl.TEXTURE_BINDING_2D)
                    //println("OpenglAG.draw: textureUnit=$textureUnit, textureBinding=$texBinding, instances=$instances, vertexCount=$vertexCount")
                    textureUnit++
                }
                VarType.Mat2, VarType.Mat3, VarType.Mat4 -> {
                    val matArray = when (value) {
                        is Array<*> -> value
                        is Matrix3D -> mat3dArray.also { it[0].copyFrom(value) }
                        else -> error("Not an array or a matrix3d")
                    }.fastCastTo<Array<Matrix3D>>()
                    val arrayCount = min(declArrayCount, matArray.size)

                    val matSize = when (uniformType) {
                        VarType.Mat2 -> 2; VarType.Mat3 -> 3; VarType.Mat4 -> 4; else -> -1
                    }

                    for (n in 0 until arrayCount) {
                        matArray[n].copyToFloatWxH(tempFloats, matSize, matSize, MajorOrder.COLUMN, n * stride)
                    }
                    tempBuffer.setFloats(0, tempFloats, 0, stride * arrayCount)

                    if (gl.webgl) {
                        //if (true) {
                        val tb = when (uniformType) {
                            VarType.Mat2 -> tempBufferM2
                            VarType.Mat3 -> tempBufferM3
                            VarType.Mat4 -> tempBufferM4
                            else -> tempBufferM4
                        }

                        for (n in 0 until arrayCount) {
                            val itLocation = when (arrayCount) {
                                1 -> location
                                else -> gl.getUniformLocation(glProgram.programId, uniform.indexNames[n])
                            }
                            arraycopy(tempBuffer.f32, n * stride, tb.f32, 0, stride)
                            //println("[WEBGL] uniformName[$uniformName]=$itLocation, tb: ${tb.f32.size}")
                            when (uniform.type) {
                                VarType.Mat2 -> gl.uniformMatrix2fv(itLocation, 1, false, tb)
                                VarType.Mat3 -> gl.uniformMatrix3fv(itLocation, 1, false, tb)
                                VarType.Mat4 -> gl.uniformMatrix4fv(itLocation, 1, false, tb)
                                else -> invalidOp("Don't know how to set uniform matrix ${uniform.type}")
                            }
                        }
                    } else {
                        //println("[NO-WEBGL] uniformName[$uniformName]=$location ")
                        when (uniform.type) {
                            VarType.Mat2 -> gl.uniformMatrix2fv(location, arrayCount, false, tempBuffer)
                            VarType.Mat3 -> gl.uniformMatrix3fv(location, arrayCount, false, tempBuffer)
                            VarType.Mat4 -> gl.uniformMatrix4fv(location, arrayCount, false, tempBuffer)
                            else -> invalidOp("Don't know how to set uniform matrix ${uniform.type}")
                        }
                    }
                }
                VarType.Float1, VarType.Float2, VarType.Float3, VarType.Float4 -> {
                    var arrayCount = declArrayCount
                    when (value) {
                        is Number -> tempBuffer.setAlignedFloat32(0, value.toFloat())
                        is Vector3D -> tempBuffer.setFloats(0, value.data, 0, stride)
                        is FloatArray -> {
                            arrayCount = min(declArrayCount, value.size / stride)
                            tempBuffer.setFloats(0, value, 0, stride * arrayCount)
                        }
                        is Array<*> -> {
                            arrayCount = min(declArrayCount, value.size)
                            for (n in 0 until value.size) {
                                val vector = value[n] as Vector3D
                                tempBuffer.setFloats(n * stride, vector.data, 0, stride)
                            }
                        }
                        else -> error("Unknown type '$value'")
                    }
                    //if (true) {
                    if (gl.webgl) {
                        val tb = tempBufferM2
                        for (n in 0 until arrayCount) {
                            val itLocation = when (arrayCount) {
                                1 -> location
                                else -> gl.getUniformLocation(glProgram.programId, uniform.indexNames[n])
                            }
                            val f32 = tb.f32
                            //println("uniformName[$uniformName] = $itLocation")
                            arraycopy(tempBuffer.f32, 0, tb.f32, 0, stride)

                            when (uniform.type) {
                                VarType.Float1 -> gl.uniform1f(itLocation, f32[0])
                                VarType.Float2 -> gl.uniform2f(itLocation, f32[0], f32[1])
                                VarType.Float3 -> gl.uniform3f(itLocation, f32[0], f32[1], f32[2])
                                VarType.Float4 -> gl.uniform4f(itLocation, f32[0], f32[1], f32[2], f32[3])
                                else -> Unit
                            }
                        }
                    } else {
                        when (uniform.type) {
                            VarType.Float1 -> gl.uniform1fv(location, arrayCount, tempBuffer)
                            VarType.Float2 -> gl.uniform2fv(location, arrayCount, tempBuffer)
                            VarType.Float3 -> gl.uniform3fv(location, arrayCount, tempBuffer)
                            VarType.Float4 -> gl.uniform4fv(location, arrayCount, tempBuffer)
                            else -> Unit
                        }
                    }
                }
                else -> invalidOp("Don't know how to set uniform ${uniform.type}")
            }
        }
    }


    fun AGOpengl.GlTexture.setFilter(linear: Boolean, trilinear: Boolean = linear) {
        val minFilter = if (this.mipmaps) {
            when {
                linear -> when {
                    trilinear -> KmlGl.LINEAR_MIPMAP_LINEAR
                    else -> KmlGl.LINEAR_MIPMAP_NEAREST
                }
                else -> when {
                    trilinear -> KmlGl.NEAREST_MIPMAP_LINEAR
                    else -> KmlGl.NEAREST_MIPMAP_NEAREST
                }
            }
        } else {
            if (linear) KmlGl.LINEAR else KmlGl.NEAREST
        }
        val magFilter = if (linear) KmlGl.LINEAR else KmlGl.NEAREST

        gl.texParameteri(forcedTexTarget, KmlGl.TEXTURE_MIN_FILTER, minFilter)
        gl.texParameteri(forcedTexTarget, KmlGl.TEXTURE_MAG_FILTER, magFilter)
    }

    fun AGOpengl.GlTexture.setWrap() {
        gl.texParameteri(forcedTexTarget, KmlGl.TEXTURE_WRAP_S, KmlGl.CLAMP_TO_EDGE)
        gl.texParameteri(forcedTexTarget, KmlGl.TEXTURE_WRAP_T, KmlGl.CLAMP_TO_EDGE)
        if (forcedTexTarget == KmlGl.TEXTURE_CUBE_MAP || forcedTexTarget == KmlGl.TEXTURE_3D) {
            gl.texParameteri(forcedTexTarget, KmlGl.TEXTURE_WRAP_R, KmlGl.CLAMP_TO_EDGE)
        }
    }

    override fun readPixels(x: Int, y: Int, width: Int, height: Int, data: Any, kind: AG.ReadKind) {
        val bytesPerPixel = when (data) {
            is IntArray -> 4
            is FloatArray -> 4
            is ByteArray -> 1
            else -> TODO()
        }
        val area = width * height
        fbuffer(area * bytesPerPixel) { buffer ->
            when (kind) {
                AG.ReadKind.COLOR -> gl.readPixels(x, y, width, height, KmlGl.RGBA, KmlGl.UNSIGNED_BYTE, buffer)
                AG.ReadKind.DEPTH -> gl.readPixels(x, y, width, height, KmlGl.DEPTH_COMPONENT, KmlGl.FLOAT, buffer)
                AG.ReadKind.STENCIL -> gl.readPixels(x, y, width, height, KmlGl.STENCIL_INDEX, KmlGl.UNSIGNED_BYTE, buffer)
            }
            when (data) {
                is IntArray -> buffer.getAlignedArrayInt32(0, data, 0, area)
                is FloatArray -> buffer.getAlignedArrayFloat32(0, data, 0, area)
                is ByteArray -> buffer.getArrayInt8(0, data, 0, area)
                else -> TODO()
            }
            //println("readColor.HASH:" + bitmap.computeHash())
        }
    }
}
