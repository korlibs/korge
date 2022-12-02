package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kds.lock.*
import com.soywiz.kgl.*
import com.soywiz.kmem.*
import com.soywiz.korag.gl.*
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.lang.*

open class NAGOpengl(val gl: KmlGl) : NAG() {
    override fun execute(command: NAGCommand) {
        when (command) {
            is NAGCommandFinish -> execute(command)
            is NAGCommandFullBatch -> command.batches.fastForEach { execute(it) }
            is NAGCommandTransfer.CopyToTexture -> execute(command)
            is NAGCommandTransfer.ReadBits -> execute(command)
        }
    }

    private fun execute(command: NAGCommandTransfer.CopyToTexture) {
        val target = AGTextureTargetKind.TEXTURE_2D
        bindFrameBuffer(command.renderBuffer)
        bindTexture(command.texture, target)
        gl.copyTexImage2D(target.toGl(), 0, KmlGl.RGBA, command.x, command.y, command.width, command.height, 0)
    }

    private fun execute(command: NAGCommandTransfer.ReadBits) {
        bindFrameBuffer(command.renderBuffer)
        val target = command.target
        val data = when (target) {
            is Bitmap32 -> target.ints
            else -> target
        }
        val bytesPerPixel = when (data) {
            is IntArray -> 4
            is FloatArray -> 4
            is ByteArray -> 1
            else -> unsupported()
        }
        val x = command.x
        val y = command.y
        val width = command.width
        val height = command.height
        val area = width * height
        BufferTemp(area * bytesPerPixel) { buffer ->
            when (command.readKind) {
                AGReadKind.COLOR -> gl.readPixels(x, y, width, height, KmlGl.RGBA, KmlGl.UNSIGNED_BYTE, buffer)
                AGReadKind.DEPTH -> gl.readPixels(x, y, width, height, KmlGl.DEPTH_COMPONENT, KmlGl.FLOAT, buffer)
                AGReadKind.STENCIL -> gl.readPixels(x, y, width, height, KmlGl.STENCIL_INDEX, KmlGl.UNSIGNED_BYTE, buffer)
            }
            when (data) {
                is IntArray -> buffer.getArrayInt32(0, data, size = area)
                is FloatArray -> buffer.getArrayFloat32(0, data, size = area)
                is ByteArray -> buffer.getArrayInt8(0, data, size = area)
                else -> unsupported()
            }
            //println("readColor.HASH:" + bitmap.computeHash())
        }
    }

    private fun execute(command: NAGCommandFinish) {
        while (true) {
            val deletesArray = deletesLock { (if (deletes.isNotEmpty()) deletes.toList() else null).also { deletes.clear() } } ?: break
            deletesArray.fastForEach { it.delete() }
        }

        gl.finish()
        command.completed?.invoke(Unit)
    }

    private fun execute(batch: NAGBatch) {
        batch.vertexData?.let { bindVertexData(it, bind = true) }
        try {
            if (batch.indexData != null) {
                bindBuffer(AGBufferKind.INDEX, batch.indexData)
            }
            batch.batches.fastForEach {
                execute(it)
            }
        } finally {
            batch.vertexData?.let { bindVertexData(it, bind = false) }
        }
    }

    private val currentState = AGFullState()

    private fun execute(batch: NAGUniformBatch) {
        bindFrameBuffer(batch.renderBuffer)
        batch.clear?.let {
            var value = 0
            it.color?.let { gl.clearColor(it.rf, it.gf, it.bf, it.af); value = value or KmlGl.COLOR_BUFFER_BIT }
            it.depth?.let { gl.clearDepthf(it); value = value or KmlGl.DEPTH_BUFFER_BIT }
            it.stencil?.let { gl.clearStencil(it); value = value or KmlGl.STENCIL_BUFFER_BIT }
            gl.clear(value)
        }
        batch.program?.let { useProgram(it) }
        batch.state?.let { execute(it) }
        batch.uniforms?.let { execute(it) }
        batch.drawCommands.fastForEach { drawType, indexType, offset, count, instances ->
            if (instances > 1) {
                when (indexType) {
                    AGIndexType.NONE -> gl.drawArraysInstanced(drawType.toGl(), offset, count, instances)
                    else -> gl.drawElementsInstanced(drawType.toGl(), count, indexType.toGl(), offset, instances)
                }
            } else {
                when (indexType) {
                    AGIndexType.NONE -> gl.drawArrays(drawType.toGl(), offset, count)
                    else -> gl.drawElements(drawType.toGl(), count, indexType.toGl(), offset)
                }
            }
        }
    }

    private fun execute(uniforms: AGUniformValues) {
        val program = currentProgram ?: error("Program not used")
        val glProgram = program.info
        val currentUniforms = program.uniforms
        uniforms.fastForEach { value ->
            var textureUnit: Int = -1
            val uniform = value.uniform
            val uniformType = uniform.type
            val uniformName = uniform.name
            val location = glProgram.getUniformLocation(gl, uniformName)
            val declArrayCount = uniform.arrayCount
            val data = value.data

            val old = currentUniforms[value.uniform]
            if (old != value) {
                old.set(value)
                return@fastForEach
            }

            if (uniformType.isSampler) {
                val unit = value.nativeValue as NAGTextureUnit

                bindTextureUnit(unit.unitId, unit, when (uniformType) {
                    VarType.Sampler2D -> AGTextureTargetKind.TEXTURE_2D
                    VarType.Sampler3D -> AGTextureTargetKind.TEXTURE_3D
                    VarType.SamplerCube -> AGTextureTargetKind.TEXTURE_CUBE_MAP
                    else -> unreachable
                })
                value.set(unit.unitId)
            }

            // UPDATE UNIFORM
            when (uniformType.kind) {
                VarKind.TFLOAT -> when (uniform.type) {
                    VarType.Mat2 -> gl.uniformMatrix2fv(location, declArrayCount, false, data)
                    VarType.Mat3 -> gl.uniformMatrix3fv(location, declArrayCount, false, data)
                    VarType.Mat4 -> gl.uniformMatrix4fv(location, declArrayCount, false, data)
                    else -> when (uniformType.elementCount) {
                        1 -> gl.uniform1fv(location, uniform.arrayCount, data)
                        2 -> gl.uniform2fv(location, uniform.arrayCount, data)
                        3 -> gl.uniform3fv(location, uniform.arrayCount, data)
                        4 -> gl.uniform4fv(location, uniform.arrayCount, data)
                    }
                }
                else -> when (uniformType.elementCount) {
                    1 -> gl.uniform1iv(location, uniform.arrayCount, data)
                    2 -> gl.uniform2iv(location, uniform.arrayCount, data)
                    3 -> gl.uniform3iv(location, uniform.arrayCount, data)
                    4 -> gl.uniform4iv(location, uniform.arrayCount, data)
                }
            }
        }
    }

    private fun execute(state: AGFullState) {
        val colorMask = state.colorMask
        if (currentState.colorMask != colorMask) {
            currentState.colorMask = colorMask
            gl.colorMask(colorMask.red, colorMask.green, colorMask.blue, colorMask.alpha)
        }

        val scissorXY = state.scissorXY
        val scissorWH = state.scissorWH
        if (currentState.scissorXY != scissorXY || currentState.scissorWH != scissorWH) {
            currentState.scissorXY = scissorXY
            currentState.scissorWH = scissorWH
            gl.enableDisable(KmlGl.SCISSOR_TEST, state.scissor != AGRect.NIL && state.scissor != AGRect.FULL) {
                gl.scissor(scissorXY.x, scissorXY.y, scissorWH.width, scissorWH.height)
            }
        }

        val viewportXY = state.viewportXY
        val viewportWH = state.viewportWH
        if (currentState.viewportXY != viewportXY || currentState.viewportWH != viewportWH) {
            currentState.viewportXY = viewportXY
            currentState.viewportWH = viewportWH
            gl.viewport(viewportXY.x, viewportXY.y, viewportWH.width, viewportWH.height)
        }

        val stencilOpFunc = state.stencilOpFunc
        val stencilRef = state.stencilRef
        if (currentState.stencilOpFunc != stencilOpFunc || currentState.stencilRef != stencilRef) {
            currentState.stencilOpFunc = stencilOpFunc
            currentState.stencilRef = stencilRef
            gl.enableDisable(KmlGl.STENCIL_TEST, state.stencilOpFunc != AGStencilOpFuncState.DISABLED) {
                gl.stencilFunc(stencilOpFunc.compareMode.toGl(), stencilRef.referenceValue, stencilRef.readMask)
                gl.stencilMask(stencilRef.writeMask)
                gl.stencilOp(stencilOpFunc.actionOnDepthFail.toGl(), stencilOpFunc.actionOnDepthPassStencilFail.toGl(), stencilOpFunc.actionOnBothPass.toGl())
            }
        }

        val blending = state.blending
        if (currentState.blending != blending) {
            currentState.blending = blending
            gl.enableDisable(KmlGl.BLEND, blending.enabled) {
                gl.blendFuncSeparate(blending.srcRGB.toGl(), blending.dstRGB.toGl(), blending.srcA.toGl(), blending.dstA.toGl())
                gl.blendEquationSeparate(blending.eqRGB.toGl(), blending.eqA.toGl())
            }
        }

        val render = state.render
        if (currentState.render != render) {
            currentState.render = render
            gl.depthMask(render.depthMask)
            gl.depthRangef(render.depthNear, render.depthFar)
            gl.enableDisable(KmlGl.CULL_FACE, render.frontFace != AGFrontFace.BOTH) {
                gl.frontFace(render.frontFace.toGl())
            }
            gl.enableDisable(KmlGl.DEPTH_TEST, render.depthFunc != AGCompareMode.ALWAYS) {
                gl.depthFunc(render.depthFunc.toGl())
            }
        }
    }

    private fun bindVertexData(vertices: NAGVertices, bind: Boolean) {
        vertices.data.fastForEach { info ->
            val vertexLayout = info.layout
            val vertices = info.buffer
            val vattrs = vertexLayout.attributes
            val vattrspos = vertexLayout.attributePositions

            //if (vertices.kind != AG.BufferKind.VERTEX) invalidOp("Not a VertexBuffer")

            bindBuffer(AGBufferKind.VERTEX, vertices)
            val totalSize = vertexLayout.totalSize
            for (n in 0 until vattrspos.size) {
                val att = vattrs[n]
                if (!att.active) continue
                val off = vattrspos[n]
                if (att.fixedLocation == null) {
                    //error("WOOPS!")
                }
                val loc = att.fixedLocation ?: error("fixedLocation not set")
                val glElementType = att.type.toGl()
                val elementCount = att.type.elementCount
                if (bind) {
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
                } else {
                    if (att.divisor != 0) {
                        gl.vertexAttribDivisor(loc, 0)
                    }
                    gl.disableVertexAttribArray(loc)
                }
            }
        }
        bindBuffer(AGBufferKind.VERTEX, null)
    }

    private val emptyBuffer = Buffer(0)

    private val deletesLock = Lock()
    private val deletes = fastArrayListOf<GLObject>()

    private fun bindBuffer(kind: AGBufferKind, buffer: NAGBuffer?) {
        val glTarget = kind.toGl()
        if (buffer == null) {
            gl.bindBuffer(glTarget, 0)
            return
        }
        val glBuffer = buffer.gl
        glBuffer.bind(kind)
        buffer.updateObject {
            gl.bufferData(
                glTarget,
                buffer.content?.size ?: 0, buffer.content ?: emptyBuffer,
                when (buffer.usage) {
                    NAGBuffer.Usage.DYNAMIC -> KmlGl.DYNAMIC_DRAW
                    NAGBuffer.Usage.STATIC -> KmlGl.STATIC_DRAW
                    NAGBuffer.Usage.STREAM -> KmlGl.STREAM_DRAW
                }
            )
        }
    }

    private val programs = LinkedHashMap<Program, NAGProgram>()

    private var currentProgram: GLProgram? = null

    private val NAGProgram.gl: GLProgram get() = createObjectIfRequired { GLProgram(this.program) }
    private val Program.gl: GLProgram get() = nag.gl
    private val Program.nag: NAGProgram get() = programs.getOrPut(this) { NAGProgram(this@nag) }

    private fun useProgram(program: Program) {
        currentProgram = program.gl.also { it.use() }
    }

    private var currentRenderBuffer: NAGFrameBuffer? = null
    private var currentTextureUnitId: Int = -1

    private fun bindTextureUnit(textureUnitId: Int, textureUnit: NAGTextureUnit, target: AGTextureTargetKind) {
        val glTarget = target.toGl()
        currentTextureUnitId = textureUnitId
        gl.activeTexture(textureUnitId)
        bindTexture(textureUnit.texture, target)
        gl.texParameteri(glTarget, KmlGl.TEXTURE_MIN_FILTER, textureUnit.minFilter.toGl())
        gl.texParameteri(glTarget, KmlGl.TEXTURE_MAG_FILTER, textureUnit.magFilter.toGl())
        gl.texParameteri(glTarget, KmlGl.TEXTURE_WRAP_S, textureUnit.wrap.toGl())
        gl.texParameteri(glTarget, KmlGl.TEXTURE_WRAP_T, textureUnit.wrap.toGl())
    }

    private fun bindTexture(texture: NAGTexture?, target: AGTextureTargetKind) {
        val tgl = texture?.gl
        if (tgl != null) {
            val content = texture.content
            if (tgl.cachedContentVersion != content?.contentVersion) {
                tgl.cachedContentVersion = content?.contentVersion ?: -1
                texture._nativeObjectVersion++
            }

            texture.updateObject {
                when (content) {
                    is Bitmap32 -> tgl.upload(target, content.width, content.height, null, GLTexKind.RGBA, GLTexKindStorage.UNSIGNED_BYTE)
                    null -> tgl.upload(target, 0, 0, null, GLTexKind.RGBA, GLTexKindStorage.UNSIGNED_BYTE)
                }
            }
        }
        gl.bindTexture(target.toGl(), tgl?.id ?: 0)
    }

    private fun bindFrameBuffer(fb: NAGFrameBuffer?) {
        if (fb == null) {
            gl.bindFramebuffer(KmlGl.FRAMEBUFFER, 0)
            return
        }

        val nfb = fb.gl
        fb.updateObject { nfb.setSize(fb.width, fb.height) }
        nfb.bind()
    }

    private abstract inner class GLObject : NAGNativeObject {
        abstract fun delete()
        override fun markDelete() {
            deletesLock { deletes += this }
        }
    }

    private val NAGFrameBuffer.gl: GLFrameBuffer get() = createObjectIfRequired { GLFrameBuffer(this) }
    private inner class GLFrameBuffer(val fb: NAGFrameBuffer) : GLObject() {
        val texture = fb.texture
        val frameBuffer = gl.genFramebuffer()
        val renderBuffer = gl.genRenderbuffer()

        override fun delete() {
            texture.delete()
            gl.deleteFramebuffer(frameBuffer)
            gl.deleteRenderbuffer(renderBuffer)
        }

        val internalFormat: Int get() = when {
            fb.hasStencilAndDepth -> KmlGl.DEPTH_STENCIL_ATTACHMENT
            fb.hasStencil -> KmlGl.STENCIL_ATTACHMENT
            fb.hasDepth -> KmlGl.DEPTH_ATTACHMENT
            else -> 0
        }

        fun setSize(width: Int, height: Int) {
            val texTarget = AGTextureTargetKind.TEXTURE_2D
            val texTargetGl = texTarget.toGl()
            bindTexture(texture, texTarget)
            gl.texImage2D(texTargetGl, 0, KmlGl.RGBA, width, height, 0, KmlGl.RGBA, KmlGl.UNSIGNED_BYTE, null)
            bindTexture(null, texTarget)

            gl.bindRenderbuffer(KmlGl.RENDERBUFFER, renderBuffer)
            if (internalFormat != 0) {
                gl.renderbufferStorage(KmlGl.RENDERBUFFER, internalFormat, fb.width, fb.height)
            }
            gl.bindRenderbuffer(KmlGl.RENDERBUFFER, 0)

            this.bind()
            gl.framebufferTexture2D(KmlGl.FRAMEBUFFER, KmlGl.COLOR_ATTACHMENT0, KmlGl.TEXTURE_2D, texture.gl.id, 0)
            if (internalFormat != 0) {
                gl.framebufferRenderbuffer(KmlGl.FRAMEBUFFER, internalFormat, KmlGl.RENDERBUFFER, renderBuffer)
            } else {
                gl.framebufferRenderbuffer(KmlGl.FRAMEBUFFER, KmlGl.STENCIL_ATTACHMENT, KmlGl.RENDERBUFFER, 0)
                gl.framebufferRenderbuffer(KmlGl.DEPTH_ATTACHMENT, KmlGl.STENCIL_ATTACHMENT, KmlGl.RENDERBUFFER, 0)
            }

        }

        fun bind() {
            gl.bindFramebuffer(KmlGl.FRAMEBUFFER, frameBuffer)
        }
    }

    enum class GLTexKind {
        RGBA, LUMINANCE
    }

    enum class GLTexKindStorage {
        UNSIGNED_BYTE, UNSIGNED_SHORT_5_6_5
    }

    fun GLTexKind.toGl() = when (this) {
        GLTexKind.RGBA -> KmlGl.RGBA
        GLTexKind.LUMINANCE -> KmlGl.LUMINANCE
    }
    fun GLTexKindStorage.toGl() = when (this) {
        GLTexKindStorage.UNSIGNED_BYTE -> KmlGl.UNSIGNED_BYTE
        GLTexKindStorage.UNSIGNED_SHORT_5_6_5 -> KmlGl.UNSIGNED_SHORT_5_6_5
    }

    private val NAGTexture.gl: GLTexture get() = createObjectIfRequired { GLTexture() }
    private inner class GLTexture() : GLObject() {
        var cachedContentVersion: Int = -1
        val id = gl.genTexture()
        fun bind(target: AGTextureTargetKind) {
            gl.bindTexture(target.toGl(), id)
        }
        fun upload(target: AGTextureTargetKind, width: Int, height: Int, buffer: Buffer?, type: GLTexKind, storage: GLTexKindStorage, level: Int = 0) {
            val internalFormat = type.toGl()
            gl.texImage2D(target.toGl(), level, internalFormat, width, height, 0, internalFormat, storage.toGl(), buffer)
        }
        override fun delete() {
            gl.deleteTexture(id)
        }
    }

    private val NAGBuffer.gl: GLBuffer get() = createObjectIfRequired { GLBuffer() }
    private inner class GLBuffer : GLObject() {
        var id: Int = gl.genBuffer()

        override fun delete() {
            gl.deleteBuffer(id)
            id = -1
        }

        fun bind(kind: AGBufferKind) {
            gl.bindBuffer(kind.toGl(), id)
        }
    }
    private inner class GLProgram(val program: Program) : GLObject() {
        val uniforms: AGUniformValues = AGUniformValues()
        val info: GLProgramInfo = GLShaderCompiler.programCreate(gl, GlslConfig(), program)

        fun use() {
            info.use(gl)
        }

        override fun delete() {
            info.delete(gl)
        }
    }

    private inline fun <T : NAGNativeObject> NAGObject.createObjectIfRequired(block: (NAGObject) -> T): T {
        if (this._nativeContextVersion != contextVersion) {
            this._nativeContextVersion = contextVersion
            this._native = block(this)
        }
        return this._native as T
    }

    private inline fun <T : NAGObject> T.updateObject(block: (T) -> Unit) {
        if (this._nativeObjectVersion != _version) {
            this._nativeObjectVersion = _version
            block(this)
        }
    }
}
