package com.soywiz.korag.gl

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kds.lock.*
import com.soywiz.kgl.KmlGl
import com.soywiz.kgl.KmlGlState
import com.soywiz.kgl.getIntegerv
import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.krypto.encoding.hex
import kotlin.contracts.*
import kotlin.jvm.*

open class SimpleAGOpengl<TKmlGl : KmlGl>(override val gl: TKmlGl, override val nativeComponent: Any = Unit, checked: Boolean = false) : AGOpengl(checked)

abstract class AGOpengl(checked: Boolean = false) : AG(checked) {
    class ShaderException(val str: String, val error: String, val errorInt: Int, val gl: KmlGl, val debugName: String?, val type: Int) :
        RuntimeException("Error Compiling Shader : $debugName type=$type : ${errorInt.hex} : '$error' : source='$str', gl.versionInt=${gl.versionInt}, gl.versionString='${gl.versionString}', gl=$gl")

    open var isGlAvailable = true

    @Deprecated("Do not use directly")
    abstract val gl: KmlGl

    override val parentFeatures: AGFeatures get() = gl

    val multithreadedRendering: Boolean get() = false

    override fun flip() {
        disposeTemporalPerFrameStuff()
        renderBuffers.free(frameRenderBuffers)
        if (frameRenderBuffers.isNotEmpty()) frameRenderBuffers.clear()
        flipInternal()
        finish()
    }

    override fun flush() {
        gl.flush()
    }

    internal fun setViewport(buffer: AGFrameBuffer) {
        gl.viewport(buffer.x, buffer.y, buffer.width, buffer.height)
        //println("VIEWPORT: $x, $y, $width, $height")
        //println("setViewport: ${buffer.x}, ${buffer.y}, ${buffer.width}, ${buffer.height}")
    }

    override fun setRenderBuffer(renderBuffer: AGFrameBuffer?): AGFrameBuffer? {
        val old = currentRenderBuffer
        //currentRenderBuffer?.unset()
        currentRenderBuffer = renderBuffer
        if (renderBuffer != null) {
            setViewport(renderBuffer)
            if (!renderBuffer.isMain) {
                bindFrameBuffer(renderBuffer)
            } else {
                bindFrameBuffer(null)
            }
        }
        return old
    }

    protected val glGlobalState by lazy { GLGlobalState(gl, this) }

    //val queue = Deque<(gl: GL) -> Unit>()

    fun sync() {
    }


    override fun contextLost() {
        Console.info("AG.contextLost()", this)
        //printStackTrace("AG.contextLost")
        contextVersion++
        gl.handleContextLost()
        gl.graphicExtensions // Ensure extensions are available outside the GL thread
    }

    open fun setSwapInterval(value: Int) {
        //gl.swapInterval = 0
    }

    fun createGlState() = KmlGlState(gl)

    override fun readColorTexture(texture: AGTexture, x: Int, y: Int, width: Int, height: Int) {
        //gl.flush()
        //gl.finish()
        textureBind(texture, AGTextureTargetKind.TEXTURE_2D)
        gl.copyTexImage2D(gl.TEXTURE_2D, 0, gl.RGBA, x, y, width, height, 0)
        textureBind(null, AGTextureTargetKind.TEXTURE_2D)
    }

    override fun clear(color: RGBA, depth: Float, stencil: Int, clearColor: Boolean, clearDepth: Boolean, clearStencil: Boolean, scissor: AGScissor) {
        //println("CLEAR: $color, $depth")
        setScissorState(this, scissor)
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
    private val externalPrograms = HashMap<Program, GLBaseProgram>()

    private fun getProgram(program: Program, config: ProgramConfig = ProgramConfig.DEFAULT, use: Boolean = true): GLBaseProgram {
        val map = if (config.externalTextureSampler) externalPrograms else normalPrograms
        val nprogram: GLBaseProgram = map.getOrPut(program) {
            GLBaseProgram(glGlobalState, GLShaderCompiler.programCreate(
                gl,
                this.config.copy(programConfig = config),
                program, debugName = program.name
            ))
        }
        //nprogram.agProgram._native = nprogram.
        if (use) {
            //nprogram.
            currentProgram = nprogram
            nprogram.use()
        }
        return nprogram
    }

    override fun draw(batch: AGBatch) {
        //println("SCISSOR: $scissor")

        //finalScissor.setTo(0, 0, backWidth, backHeight)

        setScissorState(this, batch.scissor)

        getProgram(batch.program, config = when {
            batch.uniforms.useExternalSampler() -> ProgramConfig.EXTERNAL_TEXTURE_SAMPLER
            else -> ProgramConfig.DEFAULT
        }, use = true)

        vaoUse(batch.vertexData)
        try {
            uniformsSet(batch.uniforms)

            val blending = batch.blending
            if (currentBlending != blending) {
                currentBlending = blending
                enableDisable(AGEnable.BLEND, blending.enabled) {
                    gl.blendEquationSeparate(blending.eqRGB.toGl(), blending.eqA.toGl())
                    gl.blendFuncSeparate(blending.srcRGB.toGl(), blending.dstRGB.toGl(), blending.srcA.toGl(), blending.dstA.toGl())
                }
            }

            setDepthAndFrontFace(batch.depthAndFrontFace)
            setColorMaskState(batch.colorMask)

            val stencilOpFunc = batch.stencilOpFunc
            val stencilRef = batch.stencilRef
            if (currentStencilOpFunc != stencilOpFunc || currentStencilRef != stencilRef) {
                currentStencilOpFunc = stencilOpFunc
                currentStencilRef = stencilRef
                if (stencilOpFunc.enabled) {
                    gl.enable(KmlGl.STENCIL_TEST)
                    gl.stencilFunc(stencilOpFunc.compareMode.toGl(), stencilRef.referenceValue, stencilRef.readMask)
                    gl.stencilOp(
                        stencilOpFunc.actionOnDepthFail.toGl(),
                        stencilOpFunc.actionOnDepthPassStencilFail.toGl(),
                        stencilOpFunc.actionOnBothPass.toGl()
                    )
                    gl.stencilMask(stencilRef.writeMask)
                } else {
                    gl.disable(KmlGl.STENCIL_TEST)
                    gl.stencilMask(0)
                }
            }

            //val viewport = Buffer(4 * 4)
            //gl.getIntegerv(KmlGl.VIEWPORT, viewport)
            //println("viewport=${viewport.getAlignedInt32(0)},${viewport.getAlignedInt32(1)},${viewport.getAlignedInt32(2)},${viewport.getAlignedInt32(3)}")

            draw(
                batch.drawType, batch.vertexCount,
                batch.drawOffset,
                batch.instances,
                if (batch.indices != null) batch.indexType else AGIndexType.NONE,
                batch.indices
            )
        } finally {
            vaoUnuse(batch.vertexData)
        }
    }

    override fun readColor(bitmap: Bitmap32, x: Int, y: Int) {
        readPixels(x, y, bitmap.width, bitmap.height, bitmap.ints, AGReadKind.COLOR)
    }
    override fun readDepth(width: Int, height: Int, out: FloatArray) {
        readPixels(0, 0, width, height, out, AGReadKind.DEPTH)
    }
    override fun readStencil(bitmap: Bitmap8) {
        readPixels(0, 0, bitmap.width, bitmap.height, bitmap.data, AGReadKind.STENCIL)
    }

    val config: GlslConfig by lazy {
        GlslConfig(
            gles = gl.gles,
            android = gl.android,
        )
    }

    private var currentBlending: AGBlending = AGBlending.INVALID
    private var currentStencilOpFunc: AGStencilOpFunc = AGStencilOpFunc.INVALID
    private var currentStencilRef: AGStencilReference = AGStencilReference.INVALID
    private var currentColorMask: AGColorMask = AGColorMask.INVALID
    private var currentProgram: GLBaseProgram? = null
    var backBufferFrameBufferBinding: Int = 0

    override fun startFrame() {
        currentBlending = AGBlending.INVALID
        currentStencilOpFunc = AGStencilOpFunc.INVALID
        currentStencilRef = AGStencilReference.INVALID
        currentColorMask = AGColorMask.INVALID
        currentProgram = null
        backBufferFrameBufferBinding = gl.getIntegerv(KmlGl.FRAMEBUFFER_BINDING)
    }

    override fun endFrame() {
        bindFrameBuffer(null)
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

    fun finish() {
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

    fun enableDisable(kind: AGEnable, enable: Boolean) {
        gl.enableDisable(kind.toGl(), enable)
    }

    fun cullFace(face: AGCullFace) {
        gl.cullFace(face.toGl())
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
        if (this._cachedVersion != this._version) {
            this._cachedVersion = this._version
            block(this)
        }
    }

    private fun bindBuffer(buffer: AGBuffer, target: AGBufferKind) {
        val bufferInfo = buffer.gl
        gl.bindBuffer(target.toGl(), bufferInfo.id)
        buffer.update {
            val mem = buffer.mem ?: Buffer(0)
            gl.bufferData(target.toGl(), mem.sizeInBytes, mem, KmlGl.STATIC_DRAW)
        }
    }

    ///////////////////////////////////////
    // DRAW
    ///////////////////////////////////////
    fun draw(
        type: AGDrawType,
        vertexCount: Int,
        offset: Int,
        instances: Int,
        indexType: AGIndexType,
        indices: AGBuffer?
    ) {
        indices?.let { bindBuffer(it, AGBufferKind.INDEX) }

        if (indexType != AGIndexType.NONE) {
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

    fun scissor(x: Int, y: Int, width: Int, height: Int) {
        gl.scissor(x, y, width, height)
        //println("SCISSOR: $x, $y, $width, $height")
    }

    fun vaoUnuse(vao: AGVertexArrayObject) {
        vao.list.fastForEach { entry ->
            val vattrs = entry.layout.attributes
            vattrs.fastForEach { att ->
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

    fun vaoUse(vao: AGVertexArrayObject) {
        vao.list.fastForEach { entry ->
            val vertices = entry.buffer
            val vertexLayout = entry.layout

            val vattrs = vertexLayout.attributes
            val vattrspos = vertexLayout.attributePositions

            //if (vertices.kind != AG.BufferKind.VERTEX) invalidOp("Not a VertexBuffer")

            bindBuffer(vertices, AGBufferKind.VERTEX)
            val totalSize = vertexLayout.totalSize
            for (n in 0 until vattrspos.size) {
                val att = vattrs[n]
                if (!att.active) continue
                val off = vattrspos[n]
                val loc = att.fixedLocation
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

    // UBO

    fun uniformsSet(uniforms: AGUniformValues) {
        val glProgram = currentProgram ?: return

        //if (doPrint) println("-----------")

        var textureUnit = -1
        //for ((uniform, value) in uniforms) {
        uniforms.fastForEach { value ->
            val uniform = value.uniform
            val uniformName = uniform.name
            val uniformType = uniform.type
            val location = glProgram.programInfo.getUniformLocation(gl, uniformName)
            val declArrayCount = uniform.arrayCount

            when (uniformType) {
                VarType.Sampler2D, VarType.SamplerCube -> {
                    textureUnit++
                    val unit = value.nativeValue?.fastCastTo<AGTextureUnit>() ?: AGTextureUnit(textureUnit, null)
                    //val textureUnit = unit.index
                    //println("unit=${unit.texture}")
                    //textureUnit = glProgram.getTextureUnit(uniform, unit)

                    //if (cacheTextureUnit[textureUnit] != unit) {
                    //    cacheTextureUnit[textureUnit] = unit.clone()
                    selectTextureUnit(textureUnit)
                    value.i32[0] = textureUnit

                    val tex = unit.texture
                    if (tex != null) {
                        // @TODO: This might be enqueuing commands, we shouldn't do that here.
                        textureBind(tex, when (uniformType) {
                            VarType.Sampler2D -> AGTextureTargetKind.TEXTURE_2D
                            else -> AGTextureTargetKind.TEXTURE_CUBE_MAP
                        })

                        gl.texParameteri(tex.implForcedTexTarget.toGl(), KmlGl.TEXTURE_WRAP_S, unit.wrap.toGl())
                        gl.texParameteri(tex.implForcedTexTarget.toGl(), KmlGl.TEXTURE_WRAP_T, unit.wrap.toGl())
                        if (tex.implForcedTexTarget.dims >= 3) {
                            gl.texParameteri(tex.implForcedTexTarget.toGl(), KmlGl.TEXTURE_WRAP_R, unit.wrap.toGl())
                        }

                        gl.texParameteri(tex.implForcedTexTarget.toGl(), KmlGl.TEXTURE_MIN_FILTER, unit.minFilter())
                        gl.texParameteri(tex.implForcedTexTarget.toGl(), KmlGl.TEXTURE_MAG_FILTER, unit.magFilter())

                    } else {
                        gl.bindTexture(KmlGl.TEXTURE_2D, 0)
                    }
                    //}
                }
                else -> Unit
            }

            val oldValue = glProgram.programInfo.cache[uniform]
            if (value == oldValue) {
                return@fastForEach
            }
            glProgram.programInfo.cache[uniform] = value

            //println("uniform: $uniform, arrayCount=${uniform.arrayCount}, stride=${uniform.elementCount}, value=$value old=$oldValue")

            // Store into a direct buffer
            //arraycopy(value.data, 0, tempData, 0, value.data.size)
            val data = value.data

            //println("uniform=$uniform, data=${value.data}")

            when (uniformType.kind) {
                VarKind.TFLOAT -> when (uniform.type) {
                    VarType.Mat2 -> gl.uniformMatrix2fv(location, declArrayCount, false, data)
                    VarType.Mat3 -> gl.uniformMatrix3fv(location, declArrayCount, false, data)
                    VarType.Mat4 -> gl.uniformMatrix4fv(location, declArrayCount, false, data)
                    else -> when (uniformType.elementCount) {
                        1 -> gl.uniform1fv(location, declArrayCount, data)
                        2 -> gl.uniform2fv(location, declArrayCount, data)
                        3 -> gl.uniform3fv(location, declArrayCount, data)
                        4 -> gl.uniform4fv(location, declArrayCount, data)
                    }
                }
                else -> when (uniformType.elementCount) {
                    1 -> gl.uniform1iv(location, declArrayCount, data)
                    2 -> gl.uniform2iv(location, declArrayCount, data)
                    3 -> gl.uniform3iv(location, declArrayCount, data)
                    4 -> gl.uniform4iv(location, declArrayCount, data)
                }
            }
        }
    }


    fun AGTextureUnit.minFilter(): Int = texture.minFilter(linear, trilinear ?: linear)
    fun AGTextureUnit.magFilter(): Int = texture.magFilter(linear, trilinear ?: linear)

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

    fun readPixels(x: Int, y: Int, width: Int, height: Int, data: Any, kind: AGReadKind) {
        val bytesPerPixel = when (data) {
            is IntArray -> 4
            is FloatArray -> 4
            is ByteArray -> 1
            else -> TODO()
        }
        val area = width * height
        BufferTemp(area * bytesPerPixel) { buffer ->
            when (kind) {
                AGReadKind.COLOR -> gl.readPixels(x, y, width, height, KmlGl.RGBA, KmlGl.UNSIGNED_BYTE, buffer)
                AGReadKind.DEPTH -> gl.readPixels(x, y, width, height, KmlGl.DEPTH_COMPONENT, KmlGl.FLOAT, buffer)
                AGReadKind.STENCIL -> gl.readPixels(x, y, width, height, KmlGl.STENCIL_INDEX, KmlGl.UNSIGNED_BYTE, buffer)
            }
            when (data) {
                is IntArray -> buffer.getArrayInt32(0, data, size = area)
                is FloatArray -> buffer.getArrayFloat32(0, data, size = area)
                is ByteArray -> buffer.getArrayInt8(0, data, size = area)
                else -> TODO()
            }
            //println("readColor.HASH:" + bitmap.computeHash())
        }
    }

    fun readPixelsToTexture(tex: AGTexture, x: Int, y: Int, width: Int, height: Int, kind: AGReadKind) {
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

    fun textureBind(tex: AGTexture?, target: AGTextureTargetKind) {
        val glTex = tex?.gl
        gl.bindTexture(target.toGl(), glTex?.id ?: 0)
        val texBitmap = tex?.bitmap
        if (glTex != null && texBitmap != null) {
            if (glTex.cachedContentVersion != texBitmap.contentVersion) {
                glTex.cachedContentVersion = texBitmap.contentVersion
                tex._cachedVersion = -1
                tex._version++
            }
            tex.update {
                //gl.texImage2D(target.toGl(), 0, type, source.width, source.height, 0, type, KmlGl.UNSIGNED_BYTE, null)
                val rbmp = tex.bitmap
                val bmps = (rbmp as? MultiBitmap?)?.bitmaps ?: listOf(rbmp)
                val requestMipmaps: Boolean = tex.requestMipmaps
                tex.mipmaps = tex.doMipmaps(rbmp, requestMipmaps)

                //println("UPDATE BITMAP")

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
                        isFloat && (gl.webgl2 || !gl.webgl) -> KmlGl.RGBA32F
                        //isFloat && (gl.webgl) -> KmlGl.FLOAT
                        //isFloat && (gl.webgl) -> KmlGl.RGBA
                        else -> type
                    }
                    val format = type
                    val texType = when {
                        isFloat -> KmlGl.FLOAT
                        else -> KmlGl.UNSIGNED_BYTE
                    }


                    if (gl.linux) {
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
                                //println("actualSyncUpload: webgl=$webgl, internalFormat=${internalFormat.hex}, format=${format.hex}, textype=${texType.hex}")
                                gl.texImage2D(texTarget, 0, internalFormat, bmp.width, bmp.height, 0, format, texType, buffer)
                            }
                        }
                    }

                    if (tex.mipmaps) {
                        gl.generateMipmap(texTarget)
                    }
                }
            }
        }
    }

    private val tempTextureUnit = 7

    fun textureSetFromFrameBuffer(tex: AGTexture, x: Int, y: Int, width: Int, height: Int) {
        val old = selectTextureUnit(tempTextureUnit)
        gl.bindTexture(gl.TEXTURE_2D, tex.gl.id)
        gl.copyTexImage2D(gl.TEXTURE_2D, 0, gl.RGBA, x, y, width, height, 0)
        gl.bindTexture(gl.TEXTURE_2D, 0)
        selectTextureUnit(old)
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

    private var currentTextureUnit = 0
    private fun selectTextureUnit(index: Int): Int {
        val old = currentTextureUnit
        currentTextureUnit = index
        gl.activeTexture(KmlGl.TEXTURE0 + index)
        return old
    }

    fun bindFrameBuffer(frameBuffer: AGFrameBuffer?) {
        if (frameBuffer == null) {
            gl.bindFramebuffer(KmlGl.FRAMEBUFFER, backBufferFrameBufferBinding)
            return
        }
        // Ensure everything has been executed already. @TODO: We should remove this since this is a bottleneck
        val fb = frameBuffer.gl
        val tex = fb.ag.tex
        // http://wangchuan.github.io/coding/2016/05/26/multisampling-fbo.html
        val doMsaa = false
        val internalFormat = when {
            fb.ag.hasStencilAndDepth -> KmlGl.DEPTH_STENCIL
            fb.ag.hasStencil -> KmlGl.STENCIL_INDEX8 // On android this is buggy somehow?
            fb.ag.hasDepth -> KmlGl.DEPTH_COMPONENT
            else -> 0
        }
        val texTarget = when {
            doMsaa -> KmlGl.TEXTURE_2D_MULTISAMPLE
            else -> KmlGl.TEXTURE_2D
        }

        frameBuffer.update {
            tex.bitmap = NullBitmap(frameBuffer.width, frameBuffer.height, false)
            val old = selectTextureUnit(tempTextureUnit)
            textureBind(tex, AGTextureTargetKind.TEXTURE_2D)
            selectTextureUnit(old)
            gl.texParameteri(texTarget, KmlGl.TEXTURE_MAG_FILTER, KmlGl.LINEAR)
            gl.texParameteri(texTarget, KmlGl.TEXTURE_MIN_FILTER, KmlGl.LINEAR)
            //gl.texImage2D(texTarget, 0, KmlGl.RGBA, fb.ag.width, fb.ag.height, 0, KmlGl.RGBA, KmlGl.UNSIGNED_BYTE, null)
            gl.bindTexture(texTarget, 0)
            gl.bindRenderbuffer(KmlGl.RENDERBUFFER, fb.renderBufferId)
            if (internalFormat != 0) {
                //gl.renderbufferStorageMultisample(KmlGl.RENDERBUFFER, fb.nsamples, internalFormat, fb.width, fb.height)
                gl.renderbufferStorage(KmlGl.RENDERBUFFER, internalFormat, fb.width, fb.height)
            }
            gl.bindRenderbuffer(KmlGl.RENDERBUFFER, 0)
            //gl.renderbufferStorageMultisample()
            gl.bindFramebuffer(KmlGl.FRAMEBUFFER, fb.frameBufferId)
            gl.framebufferTexture2D(KmlGl.FRAMEBUFFER, KmlGl.COLOR_ATTACHMENT0, KmlGl.TEXTURE_2D, fb.ag.tex.gl.id, 0)
            if (internalFormat != 0) {
                gl.framebufferRenderbuffer(KmlGl.FRAMEBUFFER, internalFormat, KmlGl.RENDERBUFFER, fb.renderBufferId)
            } else {
                gl.framebufferRenderbuffer(KmlGl.FRAMEBUFFER, KmlGl.STENCIL_ATTACHMENT, KmlGl.RENDERBUFFER, 0)
                gl.framebufferRenderbuffer(KmlGl.DEPTH_ATTACHMENT, KmlGl.STENCIL_ATTACHMENT, KmlGl.RENDERBUFFER, 0)
            }
        }

        gl.bindFramebuffer(KmlGl.FRAMEBUFFER, fb.frameBufferId)
        //val status = gl.checkFramebufferStatus(KmlGl.FRAMEBUFFER)
        //if (status != KmlGl.FRAMEBUFFER_COMPLETE) { gl.bindFramebuffer(KmlGl.FRAMEBUFFER, 0); error("Error getting framebuffer") }
    }

    private val AGBuffer.gl: GLBuffer get() = gl(glGlobalState)
    private val AGFrameBuffer.gl: GLFrameBuffer get() = gl(glGlobalState)
    private val AGTexture.gl: GLTexture get() = gl(glGlobalState)

    fun enable(kind: AGEnable): Unit {
        gl.enable(kind.toGl())
    }
    fun disable(kind: AGEnable): Unit {
        gl.disable(kind.toGl())
    }

    fun enableBlend(): Unit = enable(AGEnable.BLEND)
    fun enableCullFace(): Unit = enable(AGEnable.CULL_FACE)
    fun enableDepth(): Unit = enable(AGEnable.DEPTH)
    fun enableScissor(): Unit = enable(AGEnable.SCISSOR)
    fun enableStencil(): Unit = enable(AGEnable.STENCIL)
    fun disableBlend(): Unit = disable(AGEnable.BLEND)
    fun disableCullFace(): Unit = disable(AGEnable.CULL_FACE)
    fun disableDepth(): Unit = disable(AGEnable.DEPTH)
    fun disableScissor(): Unit = disable(AGEnable.SCISSOR)
    fun disableStencil(): Unit = disable(AGEnable.STENCIL)

    inline fun enableDisable(kind: AGEnable, enable: Boolean, block: () -> Unit = {}) {
        if (enable) {
            enable(kind)
            block()
        } else {
            disable(kind)
        }
    }


    fun setDepthAndFrontFace(renderState: AGDepthAndFrontFace) {
        enableDisable(AGEnable.CULL_FACE, renderState.frontFace != AGFrontFace.BOTH) {
            gl.frontFace(renderState.frontFace.toGl())
        }

        gl.depthMask(renderState.depthMask)
        gl.depthRangef(renderState.depthNear, renderState.depthFar)

        enableDisable(AGEnable.DEPTH, renderState.depthFunc != AGCompareMode.ALWAYS) {
            gl.depthFunc(renderState.depthFunc.toGl())
        }
    }

    fun setColorMaskState(colorMask: AGColorMask) {
        if (currentColorMask != colorMask) {
            currentColorMask = colorMask
            gl.colorMask(colorMask.red, colorMask.green, colorMask.blue, colorMask.alpha)
        }
    }

    fun setScissorState(ag: AG, scissor: AGScissor = AGScissor.NIL) =
        setScissorState(ag.currentRenderBuffer, ag.mainFrameBuffer, scissor)

    fun setScissorState(currentRenderBuffer: AGFrameBuffer?, mainRenderBuffer: AGFrameBuffer, scissor: AGScissor = AGScissor.NIL) {
        if (currentRenderBuffer == null) return

        //println("applyScissorState")
        val finalScissorBL = tempRect

        val realBackWidth = mainRenderBuffer.fullWidth
        val realBackHeight = mainRenderBuffer.fullHeight

        if (currentRenderBuffer === mainRenderBuffer) {
            var realScissors: Rectangle? = finalScissorBL
            realScissors?.setTo(0.0, 0.0, realBackWidth.toDouble(), realBackHeight.toDouble())
            if (scissor != AGScissor.NIL) {
                tempRect.setTo(
                    currentRenderBuffer.x + scissor.x,
                    ((currentRenderBuffer.y + currentRenderBuffer.height) - (scissor.y + scissor.height)),
                    (scissor.width),
                    scissor.height
                )
                realScissors = realScissors?.intersection(tempRect, realScissors)
            }

            //println("currentRenderBuffer: $currentRenderBuffer")

            val renderBufferScissor = currentRenderBuffer.scissor
            if (renderBufferScissor != null) {
                realScissors = realScissors?.intersection(renderBufferScissor.rect, realScissors)
            }

            //println("[MAIN_BUFFER] realScissors: $realScissors")

            enable(AGEnable.SCISSOR)
            if (realScissors != null) {
                scissor(realScissors.x.toInt(), realScissors.y.toInt(), realScissors.width.toInt(), realScissors.height.toInt())
            } else {
                scissor(0, 0, 0, 0)
            }
        } else {
            //println("[RENDER_TARGET] scissor: $scissor")

            enableDisable(AGEnable.SCISSOR, scissor != AGScissor.NIL) {
                scissor(scissor.x, scissor.y, scissor.width, scissor.height)
            }
        }
    }

    internal var renderThreadId: Long = -1L
    internal var renderThreadName: String? = null
    //var programIndex = KorAtomicInt(0)
    private val lock = NonRecursiveLock()

    enum class AGEnable {
        BLEND, CULL_FACE, DEPTH, SCISSOR, STENCIL;
    }

    fun AGEnable.toGl(): Int = when (this) {
        AGEnable.BLEND -> KmlGl.BLEND
        AGEnable.CULL_FACE -> KmlGl.CULL_FACE
        AGEnable.DEPTH -> KmlGl.DEPTH_TEST
        AGEnable.SCISSOR -> KmlGl.SCISSOR_TEST
        AGEnable.STENCIL -> KmlGl.STENCIL_TEST
    }
}
