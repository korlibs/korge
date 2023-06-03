package korlibs.graphics.gl

import korlibs.crypto.encoding.*
import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.graphics.shader.gl.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.io.lang.*
import korlibs.kgl.*
import korlibs.memory.*

//val ENABLE_UNIFORM_BLOCKS = Environment["ENABLE_UNIFORM_BLOCKS"] == "true"
val ENABLE_UNIFORM_BLOCKS = Environment["DISABLE_UNIFORM_BLOCKS"] != "true"
val ENABLE_VERTEX_ARRAY_OBJECTS = Environment["DISABLE_VERTEX_ARRAY_OBJECTS"] != "true"
//val ENABLE_UNIFORM_BLOCKS = false

class AGOpengl(val gl: KmlGl, var context: KmlGlContext? = null) : AG() {
    val contextsToFree = linkedSetOf<KmlGlContext?>()

    class ShaderException(val str: String, val error: String, val errorInt: Int, val gl: KmlGl, val debugName: String?, val type: Int, val shaderReturnInt: Int) :
        RuntimeException("Error Compiling Shader : $debugName type=$type : ${errorInt.hex} : '$error' : source='$str', shaderReturnInt=$shaderReturnInt, gl.versionInt=${gl.versionInt}, gl.versionString='${gl.versionString}', gl=$gl")

    override val parentFeatures: AGFeatures get() = gl

    protected val glGlobalState = GLGlobalState(gl, this)

    //val queue = Deque<(gl: GL) -> Unit>()

    fun sync() {
    }

    override fun contextLost() {
        val gl: KmlGl = this.gl
        super.contextLost()
        gl.handleContextLost()
        gl.graphicExtensions // Ensure extensions are available outside the GL thread
        normalPrograms.clear()
        resetObjects()
        dynamicVaoGlId = -1
        dynamicVaoContextVersion = -1
        reallyIsVertexArraysSupported = true
        //externalPrograms.clear()
    }

    open fun setSwapInterval(value: Int) {
        //gl.swapInterval = 0
    }

    fun createGlState() = KmlGlState(gl)

    override fun readToTexture(frameBuffer: AGFrameBufferBase, frameBufferInfo: AGFrameBufferInfo, texture: AGTexture, x: Int, y: Int, width: Int, height: Int) {
        val gl: KmlGl = this.gl
        bindFrameBuffer(frameBuffer, frameBufferInfo)
        setScissorState(AGScissor.FULL, frameBuffer, frameBufferInfo)
        //gl.flush()
        //gl.finish()
        selectTextureUnitTemp(TEMP_TEXTURE_UNIT, setToNullLater = true) {
            textureBind(texture, AGTextureTargetKind.TEXTURE_2D)
            if (gl.variant.supportTextureLevel) {
                gl.texParameteri(gl.TEXTURE_2D, KmlGl.TEXTURE_BASE_LEVEL, 0)
                gl.texParameteri(gl.TEXTURE_2D, KmlGl.TEXTURE_MAX_LEVEL, 0)
            }
            gl.copyTexImage2D(gl.TEXTURE_2D, 0, gl.RGBA, x, y, width, height, 0)
            textureUnitParameters(AGTextureTargetKind.TEXTURE_2D, AGWrapMode.CLAMP_TO_EDGE, KmlGl.LINEAR, KmlGl.LINEAR, 2)
        }
    }

    override fun clear(frameBuffer: AGFrameBufferBase, frameBufferInfo: AGFrameBufferInfo, color: RGBA, depth: Float, stencil: Int, clearColor: Boolean, clearDepth: Boolean, clearStencil: Boolean, scissor: AGScissor) {
        val gl: KmlGl = this.gl
        bindFrameBuffer(frameBuffer, frameBufferInfo)
        //println("CLEAR: $color, $depth")
        setScissorState(scissor, frameBuffer, frameBufferInfo)
        //gl.disable(KmlGl.SCISSOR_TEST)
        var mask = 0
        if (clearColor) {
            setColorMaskState(AGColorMask.ALL_ENABLED)
            gl.clearColor(color.rf, color.gf, color.bf, color.af)
            mask = mask or KmlGl.COLOR_BUFFER_BIT
        }
        if (clearDepth) {
            gl.depthMask(true)
            gl.clearDepthf(depth)
            mask = mask or KmlGl.DEPTH_BUFFER_BIT
        }
        if (clearStencil) {
            currentStencilOpFunc = AGStencilOpFunc.INVALID
            gl.stencilMask(-1)
            gl.clearStencil(stencil)
            mask = mask or KmlGl.STENCIL_BUFFER_BIT
        }
        gl.clear(mask)
    }

    //////////////

    // @TODO: Simplify this. Why do we need external? Maybe we could copy external textures into normal ones to avoid issues
    //private val programs = FastIdentityMap<Program, FastIdentityMap<ProgramConfig, AgProgram>>()
    //private val programs = HashMap<Program, FastIdentityMap<ProgramConfig, AgProgram>>()
    //private val normalPrograms = FastIdentityMap<Program, AgProgram>()
    //private val externalPrograms = FastIdentityMap<Program, AgProgram>()
    private val normalPrograms = HashMap<Program, GLBaseProgram>()
    //private val externalPrograms = HashMap<Program, GLBaseProgram>()

    var shadingLanguageVersion: String? = null

    private fun useProgram(program: Program) {
        val gl: KmlGl = this.gl
        //val map = if (config.externalTextureSampler) externalPrograms else normalPrograms
        val map = normalPrograms

        if (shadingLanguageVersion == null) {
            shadingLanguageVersion = gl.getString(KmlGl.SHADING_LANGUAGE_VERSION)
            logger.info { "GL_VERSION=" + gl.getString(KmlGl.VERSION) }
            logger.info { "GL_SHADING_LANGUAGE_VERSION=$shadingLanguageVersion" }
            logger.info { "GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT=" + gl.getInteger(KmlGl.UNIFORM_BUFFER_OFFSET_ALIGNMENT) }
            logger.info { "gl.versionString=${gl.versionString}" }
            logger.info { "gl.versionInt=${gl.versionInt}" }
        }

        val nprogram: GLBaseProgram = map.getOrPut(program) {
            GLBaseProgram(glGlobalState, GLShaderCompiler.programCreate(gl, this.glslConfig, program, debugName = program.name)).also { baseProgram ->
                baseProgram.use()

                val programInfo = baseProgram.programInfo
                val programId = programInfo.programId

                program.samplers.forEach {
                    val location = programInfo.getUniformLocation(gl, it.name)
                    gl.uniform1i(location, it.index)
                }

                if (programInfo.config.useUniformBlocks) {
                    program.uniformBlocks.fastForEach {
                        val index = gl.getUniformBlockIndex(programId, it.name)
                        gl.uniformBlockBinding(programId, index, it.fixedLocation)
                    }
                }
            }
        }
        //nprogram.agProgram._native = nprogram.
        //nprogram.
        if (currentProgram != nprogram) {
            currentProgram = nprogram
            nprogram.use()
        }
    }

    override fun draw(
        frameBuffer: AGFrameBufferBase,
        frameBufferInfo: AGFrameBufferInfo,
        vertexData: AGVertexArrayObject,
        program: Program,
        drawType: AGDrawType,
        vertexCount: Int,
        indices: AGBuffer?,
        indexType: AGIndexType,
        drawOffset: Int,
        blending: AGBlending,
        uniformBlocks: UniformBlocksBuffersRef,
        textureUnits: AGTextureUnits,
        stencilRef: AGStencilReference,
        stencilOpFunc: AGStencilOpFunc,
        colorMask: AGColorMask,
        depthAndFrontFace: AGDepthAndFrontFace,
        scissor: AGScissor,
        cullFace: AGCullFace,
        instances: Int
    ) {
        val gl: KmlGl = this.gl

        //println("newUniformBlocks=$newUniformBlocks")
        //println("textureUnits=$textureUnits")

        //println("SCISSOR: $scissor")

        //finalScissor.setTo(0, 0, backWidth, backHeight)

        bindFrameBuffer(frameBuffer, frameBufferInfo)
        setScissorState(scissor, frameBuffer, frameBufferInfo)

        if (currentVertexData?.list != vertexData.list) {
        //if (true) {
            //println("vertexData=$vertexData")
            currentVertexData?.let { vaoUnuse(it) }
            currentVertexData = vertexData
            vaoUse(vertexData)
            //println("NOT REUSING: vertexData=$vertexData")
        } else {
            vertexData.list.fastForEach { entry ->
                bindBuffer(entry.buffer, AGBufferKind.VERTEX, onlyUpdate = true)
            }
            //println("REUSING: currentVertexData=$currentVertexData, vertexData=$vertexData")
        }

        useProgram(program)
        uniformsSet(
            //uniforms,
            uniformBlocks, textureUnits, program, frameBuffer
        )

        if (currentBlending != blending) {
            currentBlending = blending
            gl.enableDisable(KmlGl.BLEND, blending.enabled) {
                gl.blendEquationSeparate(blending.eqRGB.toGl(), blending.eqA.toGl())
                gl.blendFuncSeparate(blending.srcRGB.toGl(), blending.dstRGB.toGl(), blending.srcA.toGl(), blending.dstA.toGl())
            }
        }

        setDepthAndFrontFace(depthAndFrontFace)
        setColorMaskState(colorMask)

        if (currentCullFace != cullFace) {
            currentCullFace = cullFace
            cullFace(cullFace)
        }

        if (currentStencilOpFunc != stencilOpFunc || currentStencilRef != stencilRef) {
        //if (true) {
            currentStencilOpFunc = stencilOpFunc
            currentStencilRef = stencilRef
            if (stencilOpFunc.enabled) {
                gl.enable(KmlGl.STENCIL_TEST)
                //stencilOpFunc.triangleFace.ordinal

                if (stencilOpFunc.compareModeFront == stencilOpFunc.compareModeBack && stencilRef.referenceValueFront == stencilRef.referenceValueBack && stencilRef.readMaskFront == stencilRef.readMaskBack) {
                    gl.stencilFunc(stencilOpFunc.compareModeFront.toGl(), stencilRef.referenceValueFront, stencilRef.readMaskFront)
                } else {
                    gl.stencilFuncSeparate(KmlGl.FRONT, stencilOpFunc.compareModeFront.toGl(), stencilRef.referenceValueFront, stencilRef.readMaskFront)
                    gl.stencilFuncSeparate(KmlGl.BACK, stencilOpFunc.compareModeBack.toGl(), stencilRef.referenceValueBack, stencilRef.readMaskBack)
                }

                if (stencilOpFunc.actionOnDepthFailFront == stencilOpFunc.actionOnDepthFailBack && stencilOpFunc.actionOnDepthPassStencilFailFront == stencilOpFunc.actionOnDepthPassStencilFailBack && stencilOpFunc.actionOnBothPassFront == stencilOpFunc.actionOnBothPassBack) {
                    gl.stencilOp(stencilOpFunc.actionOnDepthFailFront.toGl(), stencilOpFunc.actionOnDepthPassStencilFailFront.toGl(), stencilOpFunc.actionOnBothPassFront.toGl())
                } else {
                    gl.stencilOpSeparate(KmlGl.FRONT, stencilOpFunc.actionOnDepthFailFront.toGl(), stencilOpFunc.actionOnDepthPassStencilFailFront.toGl(), stencilOpFunc.actionOnBothPassFront.toGl())
                    gl.stencilOpSeparate(KmlGl.BACK, stencilOpFunc.actionOnDepthFailBack.toGl(), stencilOpFunc.actionOnDepthPassStencilFailBack.toGl(), stencilOpFunc.actionOnBothPassBack.toGl())
                }

                if (stencilRef.writeMaskFront == stencilRef.writeMaskBack) {
                    gl.stencilMask(stencilRef.writeMaskFront)
                } else {
                    gl.stencilMaskSeparate(KmlGl.FRONT, stencilRef.writeMaskFront)
                    gl.stencilMaskSeparate(KmlGl.BACK, stencilRef.writeMaskBack)
                }

                //println("ENABLE STENCIL: writeMask=${stencilRef.writeMask}, func=[${stencilOpFunc.compareMode}, ${stencilRef.referenceValue}, ${stencilRef.readMask}], op=[${stencilOpFunc.actionOnDepthFail}, ${stencilOpFunc.actionOnDepthPassStencilFail}, ${stencilOpFunc.actionOnBothPass}]")
            } else {
                gl.disable(KmlGl.STENCIL_TEST)
                gl.stencilMask(0)
                //println("DISABLE STENCIL")
            }
        }

        //val viewport = Buffer(4 * 4)
        //gl.getIntegerv(KmlGl.VIEWPORT, viewport)
        //println("viewport=${viewport.getAlignedInt32(0)},${viewport.getAlignedInt32(1)},${viewport.getAlignedInt32(2)},${viewport.getAlignedInt32(3)}")

        indices?.let { bindBuffer(it, AGBufferKind.INDEX) }

        val indexType = if (indices != null) indexType else AGIndexType.NONE
        //println("GLDRAW: drawOffset=$drawOffset, vertexCount=$vertexCount, instances=$instances")
        if (indexType != AGIndexType.NONE) when {
            instances != 1 -> gl.drawElementsInstanced(drawType.toGl(), vertexCount, indexType.toGl(), drawOffset, instances)
            else -> gl.drawElements(drawType.toGl(), vertexCount, indexType.toGl(), drawOffset)
        } else when {
            instances != 1 -> gl.drawArraysInstanced(drawType.toGl(), drawOffset, vertexCount, instances)
            else -> gl.drawArrays(drawType.toGl(), drawOffset, vertexCount)
        }
        //println("/GLDRAW")

        //currentVertexData?.let { vaoUnuse(it) }
    }

    val glslConfig: GlslConfig by lazy { GlslConfig(gl.variant, gl) }

    private var currentVertexData: AGVertexArrayObject? = null
    private var currentBlending: AGBlending = AGBlending.INVALID
    private var currentCullFace: AGCullFace = AGCullFace.INVALID
    private var currentStencilOpFunc: AGStencilOpFunc = AGStencilOpFunc.INVALID
    private var currentStencilRef: AGStencilReference = AGStencilReference.INVALID
    private var currentColorMask: AGColorMask = AGColorMask.INVALID
    private var currentRenderState: AGDepthAndFrontFace = AGDepthAndFrontFace.INVALID
    private var currentProgram: GLBaseProgram? = null
    private val currentTextureUnits = AGTextureUnits()
    private val currentTextureVersions = IntArray(currentTextureUnits.size)
    var backBufferFrameBufferBinding: Int = 0
    private var currentScissor: AGScissor = AGScissor.INVALID

    override fun startFrame() {
        val gl: KmlGl = this.gl
        context?.set()
        gl.beforeDoRender(contextVersion)

        resetObjects()
        backBufferFrameBufferBinding = gl.getIntegerv(KmlGl.FRAMEBUFFER_BINDING)
        gl.activeTexture(KmlGl.TEXTURE0)
        if (gl.variant.supportTextureLevel) {
            gl.texParameteri(gl.TEXTURE_2D, KmlGl.TEXTURE_BASE_LEVEL, 0)
            gl.texParameteri(gl.TEXTURE_2D, KmlGl.TEXTURE_MAX_LEVEL, 0)
        }
    }

    private fun resetObjects() {
        currentVertexData = null
        currentScissor = AGScissor.INVALID
        currentBlending = AGBlending.INVALID
        currentCullFace = AGCullFace.INVALID
        currentStencilOpFunc = AGStencilOpFunc.INVALID
        currentStencilRef = AGStencilReference.INVALID
        currentColorMask = AGColorMask.INVALID
        currentRenderState = AGDepthAndFrontFace.INVALID
        currentTextureUnits.clear()
        currentProgram = null
        _currentFrameBuffer = -1
        _currentTextureUnit = 0
        _currentViewportSize = AGSize.INVALID
        textureParams.fastForEach { it.reset() }
    }

    override fun endFrame() {
        currentVertexData?.let { vaoUnuse(it) }
        currentVertexData = null
        bindFrameBuffer(mainFrameBuffer.base, mainFrameBuffer.info)
        context?.unset()
    }

    fun listStart() {
        if (renderThreadId == -1L) {
            renderThreadId = currentThreadId
            renderThreadName = currentThreadName
            if (currentThreadName?.contains("DefaultDispatcher-worker") == true) {
                println("DefaultDispatcher-worker!")
                printStackTrace()
            }
        }
        if (renderThreadId != currentThreadId) {
            println("AGQueueProcessorOpenGL.listStart: CALLED FROM DIFFERENT THREAD! ${renderThreadName}:${renderThreadId} != $currentThreadName:$currentThreadId")
            printStackTrace()
        }
    }

    //var doPrintTimer = Stopwatch().also { it.start() }
    //var doPrint = false

    override fun finish() {
        currentVertexData?.let { vaoUnuse(it) }
        currentVertexData = null

        gl.flush()
        //gl.finish()

        deletePendingObjects()

        //doPrint = if (doPrintTimer.elapsed >= 1.seconds) {
        //    println("---------------------------------")
        //    doPrintTimer.restart()
        //    true
        //} else {
        //    false
        //}

    }

    fun cullFace(face: AGCullFace) {
        if (gl.enableDisable(KmlGl.CULL_FACE, face.ordinal > AGCullFace.NONE.ordinal)) {
            // @TODO: Move frontFace here too!
            gl.cullFace(face.toGl())
        }
    }

    ///////////////////////////////////////
    // PROGRAMS
    ///////////////////////////////////////

    private fun deletePendingObjects() {
        while (true) {
            glGlobalState.objectsToDeleteLock {
                if (glGlobalState.objectsToDelete.isNotEmpty()) {
                    glGlobalState.objectsToDelete.toList().also {
                        glGlobalState.objectsToDelete.clear()
                    }
                } else {
                    return
                }
            }.fastForEach {
                it.delete()
            }
        }

    }

    fun <T : AGObject> T.update(block: (T) -> Unit) {
        //println("UPDATE $this: this._cachedVersion != this._version  :: ${this._cachedVersion} != ${this._version}")
        if (this._cachedVersion != this._version) {
            this._cachedVersion = this._version
            block(this)
        }
    }

    private fun bindBuffer(
        buffer: AGBuffer?,
        target: AGBufferKind,
        onlyUpdate: Boolean = false,
        reallocated: Ref<Boolean>? = null,
        updated: Ref<Boolean>? = null,
    ): GLBuffer? {
        val gl: KmlGl = this.gl

        reallocated?.value = false
        updated?.value = false
        if (buffer == null) {
            gl.bindBuffer(target.toGl(), 0)
            //println("BIND: $target : null")
            return null
        }
        val bufferInfo: GLBuffer = buffer.gl
        if (!onlyUpdate) {
            gl.bindBuffer(target.toGl(), bufferInfo.id)
            //println("BIND: $target : $bufferInfo")
        }
        buffer.update {
            //println("Updated buffer: ${buffer.identityHashCode()} $buffer, $target, $onlyUpdate")
            val mem = buffer.mem ?: Buffer(0)
            bufferInfo.estimatedBytes = mem.sizeInBytes.toLong()
            if (onlyUpdate) gl.bindBuffer(target.toGl(), bufferInfo.id)
            if (bufferInfo.lastUploadedSize < mem.sizeInBytes) {
                //println("UPLOAD FULL DATA buffer=$bufferInfo: uploadedSize=${bufferInfo.lastUploadedSize} = ${mem.sizeInBytes} : contextVersion=$contextVersion")
                bufferInfo.lastUploadedSize = mem.sizeInBytes
                gl.bufferData(target.toGl(), mem.sizeInBytes, mem, KmlGl.DYNAMIC_DRAW)
                reallocated?.value = true
            } else {
                //println("UPDATES PARTIAL DATA buffer=$bufferInfo, mem=${mem.sizeInBytes}, buffer=${bufferInfo.lastUploadedSize}")
                gl.bufferSubData(target.toGl(), 0, mem.sizeInBytes, mem)
            }
            updated?.value = true
        }
        return bufferInfo
    }

    fun vaoUnuse(vao: AGVertexArrayObject) {
        if (reallyIsVertexArraysSupported && gl.isVertexArraysSupported) {
            gl.bindVertexArray(0)
        } else {
            vao.list.fastForEach { entry ->
                entry.layout.attributes.fastForEach { att ->
                    if (att.active) {
                        val loc = att.fixedLocation
                        if (loc >= 0) {
                            if (att.divisor != 0) {
                                gl.vertexAttribDivisor(loc, 0)
                            }
                            gl.disableVertexAttribArray(loc)
                        }
                    }
                }
            }
        }
    }

    var dynamicVaoGlId = -1
    var dynamicVaoContextVersion = -1

    var reallyIsVertexArraysSupported = ENABLE_VERTEX_ARRAY_OBJECTS

    fun vaoUse(vao: AGVertexArrayObject) {
        val gl: KmlGl = this.gl

        if (reallyIsVertexArraysSupported && gl.isVertexArraysSupported) {
            val vaoGl = vao.gl
            if (vao.isDynamic) {
                if (dynamicVaoContextVersion != contextVersion) {
                    dynamicVaoGlId = gl.genVertexArray()
                    dynamicVaoContextVersion = contextVersion
                    if (dynamicVaoGlId <= 0) reallyIsVertexArraysSupported = false
                }
                gl.bindVertexArray(dynamicVaoGlId)
                _vaoUse(vao)
            } else {
                if (vaoGl.contextVersion != contextVersion) {
                    vaoGl.contextVersion = contextVersion
                    vaoGl.glId = gl.genVertexArray()
                    //println("VAO created[${vaoGl.glId}]: ${vao.identityHashCode()} $vao")
                    if (vaoGl.glId <= 0) reallyIsVertexArraysSupported = false
                    //gl.bindVertexArray(vaoGl.glId)
                    //for (n in 0 until 16) gl.disableVertexAttribArray(n)

                    gl.bindVertexArray(vaoGl.glId)
                    _vaoUse(vao)
                }
                gl.bindVertexArray(vaoGl.glId)
                _vaoUse(vao, updateBuffersOnly = reallyIsVertexArraysSupported)
            }
        } else {
            _vaoUse(vao)
        }
        //gl.enableVertexAttribArray()
    }

    val bindBufferReallocated = Ref<Boolean>()

    fun _vaoUse(vao: AGVertexArrayObject, updateBuffersOnly: Boolean = false) {
        val gl: KmlGl = this.gl

        //var locBitSet = 0
        if (updateBuffersOnly) {
            //gl.bindBuffer(AGBufferKind.VERTEX.toGl(), 0)
            vao.list.fastForEach { entry ->
                bindBuffer(entry.buffer, AGBufferKind.VERTEX, updated = bindBufferReallocated)
            }
            //if (!bindBufferReallocated.value) return
            return
        }

        vao.list.fastForEach { entry ->
            val vertices = entry.buffer
            val vertexLayout = entry.layout

            val vattrs = vertexLayout.attributes
            val vattrspos = vertexLayout.attributePositions

            //if (vertices.kind != AG.BufferKind.VERTEX) invalidOp("Not a VertexBuffer")

            bindBuffer(vertices, AGBufferKind.VERTEX, updated = bindBufferReallocated)

            //if (updateBuffersOnly && !bindBufferReallocated.value) {

            val totalSize = vertexLayout.totalSize
            for (n in vattrspos.indices) {
                val att = vattrs[n]
                if (!att.active) continue
                val off = vattrspos[n]
                val loc = att.fixedLocation
                val glElementType = att.type.toGl()
                val elementCount = att.type.elementCount
                //println("loc=$loc")
                if (loc >= 0) {
                    gl.enableVertexAttribArray(loc)
                    gl.vertexAttribPointer(
                        loc,
                        elementCount,
                        glElementType,
                        att.normalized,
                        totalSize,
                        entry.baseOffset + off.toLong()
                    )
                    if (att.divisor != 0) {
                        gl.vertexAttribDivisor(loc, att.divisor)
                    }
                    //locBitSet = locBitSet.insert(true, loc)
                }
            }
        }
        //locBitSet.fastForEachOneBits { gl.enableVertexAttribArray(it) }
        //locBitSet.inv().fastForEachOneBits { gl.disableVertexAttribArray(it) }
    }

    //val tempBuffer = Buffer(4 * 128)
    //val tempBufferBlockCount = Array(128) { tempBuffer.sliceWithSize(0, 4 * it) }

    // UBO
    fun uniformsSet(
        //uniforms: AGUniformValues,
        uniformBlocks: UniformBlocksBuffersRef,
        textureUnits: AGTextureUnits,
        program: Program,
        frameBuffer: AGFrameBufferBase,
    ) {
        val glProgram: GLBaseProgram = currentProgram ?: return

        //println("uniformBlocks=${uniformBlocks}")

        //if (doPrint) println("-----------")

        //for ((uniform, value) in uniforms) {

        //println("textureUnits=$textureUnits")

        //println("PROGRAM=$program")

        textureUnits.fastForEach { index, tex, info ->
            var tex = tex
            if (frameBuffer.tex == tex) {
                //logger.warn { "FrameBuffer and texture loop!" }
                tex = null
            }
            val texVersion = tex?._version ?: -1
            if (currentTextureUnits.textures[index] !== tex || currentTextureUnits.infos[index] != info || currentTextureVersions[index] != texVersion) {
            //if (true) {
                currentTextureUnits.set(index, tex, info)
                currentTextureVersions[index] = texVersion

                //println("TEXTURE: index=$index, tex=$tex, info=$info")
                selectTextureUnit(index)
                //gl.activeTexture(KmlGl.TEXTURE0 + index)
                if (tex != null) {
                    val wrap = info.wrap
                    val linear = info.linear
                    val trilinear = info.trilinear
                    textureBind(tex, info.kind)
                    textureUnitParameters(
                        tex.implForcedTexTarget,
                        wrap,
                        tex.minFilter(linear, trilinear),
                        tex.magFilter(linear, trilinear),
                        tex.implForcedTexTarget.dims
                    )
                } else {
                    textureBind(null, AGTextureTargetKind.TEXTURE_2D)
                    //textureUnitParameters(AGTextureTargetKind.TEXTURE_2D, unitInfo.wrap, tex.minFilter(linear, trilinear), tex.magFilter(linear, trilinear), 2)
                }
            }
        }

        //selectTextureUnit(TEMP_TEXTURE_UNIT)
        //textureBind(null, AGTextureTargetKind.TEXTURE_2D)

        //selectTextureUnit(7)

        val glProgramInfo = glProgram.programInfo
        uniformBlocks.fastForEachBlock { index, block, buffer, valueIndex ->
            //if (gl.isUniformBuffersSupported && glProgram.programInfo.config.useUniformBlocks) {
            if (glProgram.programInfo.config.useUniformBlocks) {
                //println("isUniformBuffersSupported!!")
                val buffer = bindBuffer(buffer, AGBufferKind.UNIFORM)
                gl.bindBufferRange(KmlGl.UNIFORM_BUFFER, block.block.fixedLocation, buffer?.id ?: -1, valueIndex * block.blockSize, block.blockSize)
                return@fastForEachBlock
            }

            val ref = glProgramInfo.uniforms[block.block.fixedLocation]
            val bufferMem = buffer!!.mem!!
            val currentMem = ref!!.buffer
            val ublock = ref.block
            if (valueIndex >= 0) {
                if (!arrayequal(bufferMem, valueIndex, currentMem, 0, ublock.totalSize)) {
                    arraycopy(bufferMem, ublock.totalSize * valueIndex, currentMem, 0, ublock.totalSize)
                    ublock.uniforms.fastForEach { uniform ->
                        //arraycopy(currentMem, uniform.voffset, tempBuffer, 0, uniform.totalBytes)
                        writeUniform(
                            uniform.uniform,
                            glProgramInfo,
                            currentMem.slice(uniform.voffset, uniform.voffset + uniform.totalBytes),
                            "blockUniformSet",
                        )
                    }
                }
            } else {
                println("ERROR block: ${block.block} has an invalid valueIndex=$valueIndex")
            }
        }

        //uniformBlocks.fastForEachUniform {
        //    //println("UNIFORM IN BLOCK: $it")
        //    uniformSet(glProgram, it)
        //}
        //uniforms.fastForEach {
        //    //println("UNIFORM LEGACY: $it")
        //    uniformSet(glProgram, it)
        //}
    }

    private val tempData = Buffer(3 * 3 * 4)

    private fun writeUniform(uniform: Uniform, programInfo: GLProgramInfo, data: Buffer, source: String) {
        val gl: KmlGl = this.gl
        val location = programInfo.getUniformLocation(gl, uniform.name)
        val uniformType = uniform.type
        val arrayCount = uniform.arrayCount

        //println("uniform[$source]=$uniform, data=${data.hex()}")

        when (uniformType.kind) {
            VarKind.TFLOAT -> when (uniformType) {
                VarType.Mat2 -> gl.uniformMatrix2fv(location, arrayCount, false, data)
                VarType.Mat3 -> {
                    for (n in 0 until 3) arraycopy(data, n * 16, tempData, n * 12, 12)
                    gl.uniformMatrix3fv(location, arrayCount, false, tempData)
                }
                VarType.Mat4 -> gl.uniformMatrix4fv(location, arrayCount, false, data)
                else -> when (uniformType.elementCount) {
                    1 -> gl.uniform1fv(location, arrayCount, data)
                    2 -> gl.uniform2fv(location, arrayCount, data)
                    3 -> gl.uniform3fv(location, arrayCount, data)
                    4 -> gl.uniform4fv(location, arrayCount, data)
                }
            }
            else -> when (uniformType.elementCount) {
                1 -> gl.uniform1iv(location, arrayCount, data)
                2 -> gl.uniform2iv(location, arrayCount, data)
                3 -> gl.uniform3iv(location, arrayCount, data)
                4 -> gl.uniform4iv(location, arrayCount, data)
            }
        }
    }

    class TextureUnitParams(val index: Int) {
        var wrap: AGWrapMode = AGWrapMode(-1)
        var min: Int = -1
        var mag: Int = -1
        var baseLevel: Int = -1
        var maxLevel: Int = -1

        fun reset() {
            wrap = AGWrapMode(-1)
            min = -1
            mag = -1
            baseLevel = -1
            maxLevel = -1
        }
    }

    val textureParams = Array(32) { TextureUnitParams(it) }

    private fun textureUnitParameters(
        implForcedTexTarget: AGTextureTargetKind,
        wrap: AGWrapMode,
        minFilter: Int,
        magFilter: Int,
        dims: Int,
    ) {
        val gl: KmlGl = this.gl
        val params = textureParams[_currentTextureUnit]
        //currentTextureUnits.infos[_currentTextureUnit] = params

        val glTarget = implForcedTexTarget.toGl()

        // @TODO: Cache somehow doesn't work. Please, check RpgSample with cache enabled, it should use nearest

        //if (params.wrap != wrap) {
        if (true) {
            params.wrap = wrap
            val glWrap = wrap.toGl()
            gl.texParameteri(glTarget, KmlGl.TEXTURE_WRAP_S, glWrap)
            gl.texParameteri(glTarget, KmlGl.TEXTURE_WRAP_T, glWrap)
            if (dims >= 3) {
                gl.texParameteri(glTarget, KmlGl.TEXTURE_WRAP_R, glWrap)
            }
        }

        //if (params.min != minFilter || params.mag != magFilter) {
        if (true) {
            params.min = minFilter
            params.mag = magFilter
            //println("_currentTextureUnit=$_currentTextureUnit, minFilter=$minFilter, magFilter=$magFilter")
            gl.texParameteri(glTarget, KmlGl.TEXTURE_MIN_FILTER, minFilter)
            gl.texParameteri(glTarget, KmlGl.TEXTURE_MAG_FILTER, magFilter)
        } else {
            //println("cached _currentTextureUnit=$_currentTextureUnit, minFilter=$minFilter, magFilter=$magFilter")
        }
    }

    fun AGTexture?.minFilter(linear: Boolean, trilinear: Boolean = linear): Int = if (this?.mipmaps == true) {
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

    fun AGTexture?.magFilter(linear: Boolean, trilinear: Boolean = linear): Int = if (linear) KmlGl.LINEAR else KmlGl.NEAREST

    fun region(scissor: AGScissor, frameBuffer: AGFrameBufferBase, frameBufferInfo: AGFrameBufferInfo): AGScissor {
        return when {
            scissor == AGScissor.NIL || scissor == AGScissor.FULL -> scissor
            frameBuffer.isMain -> AGScissor.fromBounds(scissor.left, frameBufferInfo.height - scissor.bottom, scissor.right, frameBufferInfo.height - scissor.top)
            else -> scissor
        }
    }

    override fun readToMemory(frameBuffer: AGFrameBufferBase, frameBufferInfo: AGFrameBufferInfo, x: Int, y: Int, width: Int, height: Int, data: Any, kind: AGReadKind) {
        val gl: KmlGl = this.gl
        
        bindFrameBuffer(frameBuffer, frameBufferInfo)
        val region = region(AGScissor(x, y, width, height), frameBuffer, frameBufferInfo)

        val bytesPerPixel = when (data) {
            is IntArray -> 4
            is FloatArray -> 4
            is ByteArray -> 1
            else -> TODO()
        }
        val flipY = frameBuffer.isMain
        val area = width * height
        val stride = width * bytesPerPixel
        BufferTemp(height * stride) { buffer ->
            BufferTemp(stride) { temp ->
                when (kind) {
                    AGReadKind.COLOR -> gl.readPixels(region.x, region.y, region.width, region.height, KmlGl.RGBA, KmlGl.UNSIGNED_BYTE, buffer)
                    AGReadKind.DEPTH -> gl.readPixels(region.x, region.y, region.width, region.height, KmlGl.DEPTH_COMPONENT, KmlGl.FLOAT, buffer)
                    AGReadKind.STENCIL -> gl.readPixels(region.x, region.y, region.width, region.height, KmlGl.STENCIL_INDEX, KmlGl.UNSIGNED_BYTE, buffer)
                }
                when (data) {
                    is IntArray -> buffer.getArrayInt32(0, data, size = area)
                    is FloatArray -> buffer.getArrayFloat32(0, data, size = area)
                    is ByteArray -> buffer.getArrayInt8(0, data, size = area)
                    else -> TODO()
                }
                if (flipY) {
                    when (data) {
                        is IntArray -> Bitmap32(width, height, RgbaArray(data))
                        is FloatArray -> FloatBitmap32(width, height, data)
                        is ByteArray -> Bitmap8(width, height, data)
                        else -> TODO()
                    }.flipY()
                }
            }
            //println("readColor.HASH:" + bitmap.computeHash())
        }
    }

    fun readPixelsToTexture(tex: AGTexture, x: Int, y: Int, width: Int, height: Int, kind: AGReadKind) {
        selectTextureUnitTemp(TEMP_TEXTURE_UNIT, setToNullLater = true) {
            //println("BEFORE:" + gl.getError())
            //textureBindEnsuring(tex)
            textureBind(tex, AGTextureTargetKind.TEXTURE_2D)
            //println("BIND:" + gl.getError())
            gl.copyTexImage2D(KmlGl.TEXTURE_2D, 0, KmlGl.RGBA, x, y, width, height, 0)

            //val data = Buffer.alloc(800 * 800 * 4)
            //for (n in 0 until 800 * 800) data.setInt(n, Colors.RED.value)
            //gl.texImage2D(KmlGl.TEXTURE_2D, 0, KmlGl.RGBA, 800, 800, 0, KmlGl.RGBA, KmlGl.UNSIGNED_BYTE, data)
            //println("COPY_TEX:" + gl.getError())
        }
    }

    fun textureBind(tex: AGTexture?, target: AGTextureTargetKind) {
        val gl: KmlGl = this.gl
        val glTex = tex?.gl
        gl.bindTexture(target.toGl(), glTex?.id ?: 0)

        //println("BINDTEXTURE: ${glTex?.id}")
        val texBitmap = tex?.bitmap
        if (glTex != null && texBitmap != null) {
            if (glTex.cachedContentVersion != texBitmap.contentVersion || glTex.cachedAGContextVersion != contextVersion) {
                glTex.cachedContentVersion = texBitmap.contentVersion
                glTex.cachedAGContextVersion = contextVersion
                tex._resetVersion()
            }
            tex.update {
                //gl.texImage2D(target.toGl(), 0, type, source.width, source.height, 0, type, KmlGl.UNSIGNED_BYTE, null)
                val rbmp = tex.bitmap
                val bmps = (rbmp as? MultiBitmap?)?.bitmaps ?: listOf(rbmp)
                val requestMipmaps: Boolean = tex.requestMipmaps
                tex.mipmaps = tex.doMipmaps(rbmp, requestMipmaps)

                //println("UPDATE BITMAP: rbmp=$rbmp")

                var totalSize = 0L

                for ((index, bmp) in bmps.withIndex()) {
                    val isFloat = bmp is FloatBitmap32

                    val type = when {
                        bmp is Bitmap8 -> KmlGl.LUMINANCE
                        else -> KmlGl.RGBA //if (source is NativeImage) KmlGl.BGRA else KmlGl.RGBA
                    }

                    val texTarget = when (target) {
                        AGTextureTargetKind.TEXTURE_CUBE_MAP -> KmlGl.TEXTURE_CUBE_MAP_POSITIVE_X + index
                        else -> target.toGl()
                    }

                    //val tex = textures.getOrNull(textureId)
                    //println("_textureUpdate: texId=$textureId, id=${tex?.id}, glId=${tex?.glId}, target=$target, source=${source.width}x${source.height}")
                    //println(buffer)
                    val internalFormat = when {
                        isFloat && (gl.variant.isWebGL && gl.variant.version >= 2) -> KmlGl.RGBA32F
                        //isFloat && (gl.webgl) -> KmlGl.FLOAT
                        //isFloat && (gl.webgl) -> KmlGl.RGBA
                        else -> type
                    }
                    val format = type
                    val texType = when {
                        isFloat -> KmlGl.FLOAT
                        else -> KmlGl.UNSIGNED_BYTE
                    }


                    if (gl.variant.os.isLinux) {
                        //println("prepareTexImage2D")
                        //gl.pixelStorei(GL_UNPACK_LSB_FIRST, KmlGl.TRUE)
                        gl.pixelStorei(KmlGl.UNPACK_LSB_FIRST, KmlGl.GFALSE)
                        gl.pixelStorei(KmlGl.UNPACK_SWAP_BYTES, KmlGl.GTRUE)
                    }

                    when (bmp) {
                        null -> gl.texImage2D(target.toGl(), 0, type, tex.width, tex.height, 0, type, KmlGl.UNSIGNED_BYTE, null)
                        is NativeImage -> if (bmp.area != 0) {
                            gl.texImage2D(texTarget, 0, type, type, KmlGl.UNSIGNED_BYTE, bmp)
                        }
                        is NullBitmap -> {
                            //gl.texImage2DMultisample(texTarget, fb.ag.nsamples, KmlGl.RGBA, fb.ag.width, fb.ag.height, false)
                            gl.texImage2D(texTarget, 0, internalFormat, bmp.width, bmp.height, 0, format, texType, null)
                        }
                        else -> {
                            val buffer = createBufferForBitmap(bmp)
                            if (buffer != null && bmp.width != 0 && bmp.height != 0 && buffer.size != 0) {
                                //println("actualSyncUpload: internalFormat=${internalFormat.hex}, format=${format.hex}, textype=${texType.hex}")
                                gl.texImage2D(texTarget, 0, internalFormat, bmp.width, bmp.height, 0, format, texType, buffer)
                            }
                        }
                    }

                    totalSize += tex.width * tex.height * 4

                    if (gl.variant.supportTextureLevel) {
                        gl.texParameteri(KmlGl.TEXTURE_2D, KmlGl.TEXTURE_BASE_LEVEL, if (tex.mipmaps) tex.baseMipmapLevel ?: 0 else 0)
                        gl.texParameteri(KmlGl.TEXTURE_2D, KmlGl.TEXTURE_MAX_LEVEL, if (tex.mipmaps) tex.maxMipmapLevel ?: 1000 else 0)
                    }
                    if (tex.mipmaps) {
                        gl.generateMipmap(texTarget)
                    }
                }

                glTex.estimatedBytes = totalSize
            }
        }
    }

    //private val TEMP_TEXTURE_UNIT = 15
    private val TEMP_TEXTURE_UNIT = 7 // GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS might be 8

    fun textureSetFromFrameBuffer(tex: AGTexture, x: Int, y: Int, width: Int, height: Int) {
        val gl: KmlGl = this.gl
        selectTextureUnitTemp(TEMP_TEXTURE_UNIT, setToNullLater = true) {
            gl.bindTexture(gl.TEXTURE_2D, tex.gl.id)
            gl.copyTexImage2D(gl.TEXTURE_2D, 0, gl.RGBA, x, y, width, height, 0)
        }
    }

    private fun createBufferForBitmap(bmp: Bitmap?): Buffer? = when (bmp) {
        null -> null
        is NativeImage -> unsupported("Should not call createBufferForBitmap with a NativeImage")
        is Bitmap8 -> Buffer(bmp.area).also { mem -> arraycopy(bmp.data, 0, mem.i8, 0, bmp.area) }
        is FloatBitmap32 -> Buffer(bmp.area * 4 * 4).also { mem -> arraycopy(bmp.data, 0, mem.f32, 0, bmp.area * 4) }
        else -> Buffer(bmp.area * 4).also { mem ->
            val abmp: Bitmap32 = if (bmp.premultiplied) bmp.toBMP32IfRequired().premultipliedIfRequired() else bmp.toBMP32IfRequired().depremultipliedIfRequired()
            arraycopy(abmp.ints, 0, mem.i32, 0, abmp.area)
        }
    }

    private var _currentTextureUnit = 0
    private fun selectTextureUnit(index: Int): Int {
        val old = _currentTextureUnit
        if (_currentTextureUnit != index) {
        //if (true) {
            _currentTextureUnit = index
            if (index >= 0) {
                gl.activeTexture(KmlGl.TEXTURE0 + index)
            } else {
                //error("Invalid index $index for textureUnit")
            }
            //println("ACTIVE_TEXTURE_UNIT=$index")
        }
        return old
    }

    private inline fun selectTextureUnitTemp(index: Int, setToNullLater: Boolean = true, block: () -> Unit) {
        val old = selectTextureUnit(index)
        try {
            block()
        } finally {
            if (setToNullLater) textureBind(null, AGTextureTargetKind.TEXTURE_2D)
            selectTextureUnit(old)
        }
    }

    private var _currentViewportSize: AGSize = AGSize.INVALID
    private var _currentFrameBuffer: Int = -1

    fun bindFrameBuffer(frameBuffer: AGFrameBufferBase, info: AGFrameBufferInfo) {
        val gl: KmlGl = this.gl

        //println("bindFrameBuffer: $frameBuffer, info=$info")
        if (_currentViewportSize != info.size) {
            //println("viewport: 0, 0, ${info.width}, ${info.height}")
            gl.viewport(0, 0, info.width, info.height)
        }
        if (frameBuffer.isMain) {
            if (_currentFrameBuffer != backBufferFrameBufferBinding) {
                _currentFrameBuffer = backBufferFrameBufferBinding
                gl.bindFramebuffer(KmlGl.FRAMEBUFFER, backBufferFrameBufferBinding)
            }
            return
        }
        // Ensure everything has been executed already. @TODO: We should remove this since this is a bottleneck
        val fb = frameBuffer.gl
        val tex = fb.ag.tex
        // http://wangchuan.github.io/coding/2016/05/26/multisampling-fbo.html
        val doMsaa = false

        if (fb.info != info) {
            fb.info = info

            val internalFormat = when {
                info.hasStencilAndDepth -> KmlGl.DEPTH_STENCIL
                info.hasStencil -> KmlGl.STENCIL_INDEX8 // On android this is buggy somehow?
                info.hasDepth -> KmlGl.DEPTH_COMPONENT
                else -> 0
            }
            val texTarget = when {
                doMsaa -> KmlGl.TEXTURE_2D_MULTISAMPLE
                else -> KmlGl.TEXTURE_2D
            }

            tex.bitmap = NullBitmap(info.width, info.height, false)
            //textureParams[TEMP_TEXTURE_UNIT].reset()
            selectTextureUnitTemp(TEMP_TEXTURE_UNIT, setToNullLater = true) {
                textureBind(tex, AGTextureTargetKind.TEXTURE_2D)
                textureUnitParameters(AGTextureTargetKind.TEXTURE_2D, AGWrapMode.CLAMP_TO_EDGE, KmlGl.LINEAR, KmlGl.LINEAR, 2)
                //gl.texImage2D(texTarget, 0, KmlGl.RGBA, fb.ag.width, fb.ag.height, 0, KmlGl.RGBA, KmlGl.UNSIGNED_BYTE, null)
                //gl.bindTexture(texTarget, 0)
            }
            gl.bindRenderbuffer(KmlGl.RENDERBUFFER, fb.renderBufferId)
            //println("renderBuffer : fb.renderBufferId=${fb.renderBufferId}, internalFormat=$internalFormat, info.width=${info.width}, info.height=${info.height}")
            if (internalFormat != 0) {
                //gl.renderbufferStorageMultisample(KmlGl.RENDERBUFFER, fb.nsamples, internalFormat, fb.width, fb.height)
                gl.renderbufferStorage(KmlGl.RENDERBUFFER, internalFormat, info.width, info.height)
            }
            gl.bindRenderbuffer(KmlGl.RENDERBUFFER, 0)
            //gl.renderbufferStorageMultisample()
            gl.bindFramebuffer(KmlGl.FRAMEBUFFER, fb.frameBufferId)
            gl.framebufferTexture2D(KmlGl.FRAMEBUFFER, KmlGl.COLOR_ATTACHMENT0, KmlGl.TEXTURE_2D, fb.ag.tex.gl.id, 0)
            if (internalFormat != 0) {
                //println("framebufferRenderbuffer: FRAMEBUFFER, $internalFormat, RENDERBUFFER, ${fb.renderBufferId}")
                gl.framebufferRenderbuffer(KmlGl.FRAMEBUFFER, when {
                    info.hasStencilAndDepth -> KmlGl.DEPTH_STENCIL_ATTACHMENT
                    info.hasStencil -> KmlGl.STENCIL_ATTACHMENT // On android this is buggy somehow?
                    info.hasDepth -> KmlGl.DEPTH_ATTACHMENT
                    else -> 0
                }, KmlGl.RENDERBUFFER, fb.renderBufferId)
            } else {
                //println("framebufferRenderbuffer: FRAMEBUFFER, STENCIL_ATTACHMENT/DEPTH_ATTACHMENT, RENDERBUFFER, 0")
                gl.framebufferRenderbuffer(KmlGl.FRAMEBUFFER, KmlGl.STENCIL_ATTACHMENT, KmlGl.RENDERBUFFER, 0)
                gl.framebufferRenderbuffer(KmlGl.FRAMEBUFFER, KmlGl.DEPTH_ATTACHMENT, KmlGl.RENDERBUFFER, 0)
            }
        }

        if (_currentFrameBuffer != fb.frameBufferId) {
            _currentFrameBuffer = fb.frameBufferId
            gl.bindFramebuffer(KmlGl.FRAMEBUFFER, fb.frameBufferId)
        }
        //val status = gl.checkFramebufferStatus(KmlGl.FRAMEBUFFER)
        //if (status != KmlGl.FRAMEBUFFER_COMPLETE) { gl.bindFramebuffer(KmlGl.FRAMEBUFFER, 0); error("Error getting framebuffer") }
    }

    private val AGBuffer.gl: GLBuffer get() = gl(glGlobalState)
    private val AGFrameBufferBase.gl: GLFrameBuffer get() = gl(glGlobalState)
    private val AGTexture.gl: GLTexture get() = gl(glGlobalState)

    fun setDepthAndFrontFace(renderState: AGDepthAndFrontFace) {
        val gl: KmlGl = this.gl
        if (currentRenderState != renderState) {
            currentRenderState = renderState
            gl.frontFace(renderState.frontFace.toGl())

            gl.depthMask(renderState.depthMask)
            gl.depthRangef(renderState.depthNear, renderState.depthFar)

            gl.enableDisable(KmlGl.DEPTH_TEST, renderState.depthFunc != AGCompareMode.ALWAYS) {
                gl.depthFunc(renderState.depthFunc.toGl())
            }
        }
    }

    fun setColorMaskState(colorMask: AGColorMask) {
        val gl: KmlGl = this.gl
        if (currentColorMask != colorMask) {
            currentColorMask = colorMask
            gl.colorMask(colorMask.red, colorMask.green, colorMask.blue, colorMask.alpha)
        }
    }

    fun setScissorState(scissor: AGScissor, frameBuffer: AGFrameBufferBase, frameBufferInfo: AGFrameBufferInfo) {
        val gl: KmlGl = this.gl
        //println("scissor=$scissor, frameBuffer=${frameBuffer.isMain}, frameBufferInfo=$frameBufferInfo")
        val scissor = region(scissor, frameBuffer, frameBufferInfo)
        if (currentScissor != scissor) {
            currentScissor = scissor
            gl.enableDisable(KmlGl.SCISSOR_TEST, scissor != AGScissor.NIL) {
                // Depending on the frame-buffer, y might be bottom based or top based
                // for the main framebuffer (0, 0) is in the left, bottom part of the window
                // while on texture framebuffers it is top-left
                //println("scissor=$scissor")
                gl.scissor(scissor.x, scissor.y, scissor.width, scissor.height)
            }
        }
    }

    internal var renderThreadId: Long = -1L
    internal var renderThreadName: String? = null


    override fun readStats(out: AGStats) {
        glGlobalState.readStats(out)
    }

    //fun AGUniformValues.useExternalSampler(): Boolean {
    //    var useExternalSampler = false
    //    this.fastForEach { value ->
    //        val uniform = value.uniform
    //        val uniformType = uniform.type
    //        when (uniformType) {
    //            VarType.Sampler2D -> {
    //                val tex = value.texture
    //                if (tex != null) {
    //                    if (tex.implForcedTexTarget == AGTextureTargetKind.EXTERNAL_TEXTURE) {
    //                        useExternalSampler = true
    //                    }
    //                }
    //            }
    //            else -> Unit
    //        }
    //    }
    //    //println("useExternalSampler=$useExternalSampler")
    //    return useExternalSampler
    //}
}

private class GLVAO(var vao: AGVertexArrayObject, var glId: Int = -1, var contextVersion: Int = -1)

private var AGVertexArrayObject.gl: GLVAO by Extra.PropertyThis { GLVAO(this) }

