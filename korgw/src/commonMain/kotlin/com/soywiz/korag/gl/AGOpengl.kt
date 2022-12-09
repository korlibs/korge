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
import com.soywiz.korim.color.arraycopy
import com.soywiz.korio.annotations.KorIncomplete
import com.soywiz.korio.annotations.KorInternal
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.krypto.encoding.hex
import kotlin.contracts.*
import kotlin.jvm.*
import kotlin.native.concurrent.SharedImmutable

open class SimpleAGOpengl<TKmlGl : KmlGl>(override val gl: TKmlGl, override val nativeComponent: Any = Unit, checked: Boolean = false) : AGOpengl(checked) {

}

@OptIn(KorIncomplete::class, KorInternal::class)
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

    internal fun setViewport(buffer: AGBaseFrameBuffer) {
        gl.viewport(buffer.x, buffer.y, buffer.width, buffer.height)
        //println("VIEWPORT: $x, $y, $width, $height")
        //println("setViewport: ${buffer.x}, ${buffer.y}, ${buffer.width}, ${buffer.height}")
    }

    override fun setRenderBuffer(renderBuffer: AGBaseFrameBuffer?): AGBaseFrameBuffer? {
        val old = currentRenderBuffer
        currentRenderBuffer?.unset()
        currentRenderBuffer = renderBuffer
        if (renderBuffer != null) {
            setViewport(renderBuffer)
            if (renderBuffer is AGFrameBuffer) {
                frameBufferSet(renderBuffer)
            } else {
                renderBuffer.set()
            }
        }
        return old
    }

    protected val glGlobalState by lazy { GLGlobalState(gl, _globalState) }

    //val queue = Deque<(gl: GL) -> Unit>()

    fun sync() {
    }


    override fun contextLost() {
        Console.info("AG.contextLost()", this)
        //printStackTrace("AG.contextLost")
        globalState.contextVersion++
        gl.handleContextLost()
        gl.graphicExtensions // Ensure extensions are available outside the GL thread
    }

    open fun setSwapInterval(value: Int) {
        //gl.swapInterval = 0
    }

    override fun createMainRenderBuffer(): AGBaseFrameBufferImpl {
        var backBufferTextureBinding2d: Int = 0
        var backBufferRenderBufferBinding: Int = 0
        var backBufferFrameBufferBinding: Int = 0

        return object : AGBaseFrameBufferImpl(this) {
            override fun init() {
                sync() // Ensure commands are executed
                backBufferTextureBinding2d = gl.getIntegerv(KmlGl.TEXTURE_BINDING_2D)
                backBufferRenderBufferBinding = gl.getIntegerv(KmlGl.RENDERBUFFER_BINDING)
                backBufferFrameBufferBinding = gl.getIntegerv(KmlGl.FRAMEBUFFER_BINDING)
            }

            override fun set() {
                sync()
                setViewport(this)
                gl.bindTexture(KmlGl.TEXTURE_2D, backBufferTextureBinding2d)
                gl.bindRenderbuffer(KmlGl.RENDERBUFFER, backBufferRenderBufferBinding)
                gl.bindFramebuffer(KmlGl.FRAMEBUFFER, backBufferFrameBufferBinding)
            }

            override fun unset() {
                sync()
                backBufferTextureBinding2d = gl.getIntegerv(KmlGl.TEXTURE_BINDING_2D)
                backBufferRenderBufferBinding = gl.getIntegerv(KmlGl.RENDERBUFFER_BINDING)
                backBufferFrameBufferBinding = gl.getIntegerv(KmlGl.FRAMEBUFFER_BINDING)
            }
        }
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
            gl.colorMask(true, true, true, true)
            gl.clearColor(color.rf, color.gf, color.bf, color.af)
            mask = mask or KmlGl.COLOR_BUFFER_BIT
        }
        if (clearDepth) {
            gl.depthMask(true)
            gl.clearDepthf(depth)
            mask = mask or KmlGl.DEPTH_BUFFER_BIT
        }
        if (clearStencil) {
            gl.stencilMask(0xFF)
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
    private var currentProgram: GLBaseProgram? = null

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
        val instances = batch.instances
        val program = batch.program
        val type = batch.drawType
        val vertexCount = batch.vertexCount
        val indices = batch.indices
        val indexType = batch.indexType
        val offset = batch.drawOffset
        val blending = batch.blending
        val uniforms = batch.uniforms
        val stencilRef = batch.stencilRef
        val stencilOpFunc = batch.stencilOpFunc
        val colorMask = batch.colorMask
        val renderState = batch.depthAndFrontFace
        val scissor = batch.scissor

        //println("SCISSOR: $scissor")

        //finalScissor.setTo(0, 0, backWidth, backHeight)

        setScissorState(this, scissor)

        getProgram(program, config = when {
            uniforms.useExternalSampler() -> ProgramConfig.EXTERNAL_TEXTURE_SAMPLER
            else -> ProgramConfig.DEFAULT
        }, use = true)

        vaoUse(batch.vertexData)
        try {
            uniformsSet(uniforms)
            setState(blending, stencilOpFunc, stencilRef, colorMask, renderState)

            //val viewport = Buffer(4 * 4)
            //gl.getIntegerv(KmlGl.VIEWPORT, viewport)
            //println("viewport=${viewport.getAlignedInt32(0)},${viewport.getAlignedInt32(1)},${viewport.getAlignedInt32(2)},${viewport.getAlignedInt32(3)}")

            draw(type, vertexCount, offset, instances, if (indices != null) indexType else AGIndexType.NONE, indices)
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

    val globalState: AGGlobalState = glGlobalState.agGlobalState

    class FastResources<T : Any>(val create: (id: Int) -> T) {
        private val resources = arrayListOf<T?>()
        operator fun get(id: Int): T? = getOrNull(id)
        fun getOrNull(id: Int): T? = resources.getOrNull(id)
        fun getOrCreate(id: Int): T = getOrNull(id) ?: create(id).also {
            while (resources.size <= id) resources.add(null)
            resources[id] = it
        }
        fun tryGetAndDelete(id: Int): T? = getOrNull(id).also { delete(id) }
        fun delete(id: Int) {
            if (id < resources.size) resources[id] = null
        }
    }

    val config: GlslConfig = GlslConfig(
        gles = gl.gles,
        android = gl.android,
    )

    fun listStart() {
        if (globalState.renderThreadId == -1L) {
            globalState.renderThreadId = currentThreadId
            globalState.renderThreadName = currentThreadName
            if (currentThreadName?.contains("DefaultDispatcher-worker") == true) {
                println("DefaultDispatcher-worker!")
                printStackTrace()
            }
        }
        if (globalState.renderThreadId != currentThreadId) {
            println("AGQueueProcessorOpenGL.listStart: CALLED FROM DIFFERENT THREAD! ${globalState.renderThreadName}:${globalState.renderThreadId} != $currentThreadName:$currentThreadId")
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

    fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) {
        gl.colorMask(red, green, blue, alpha)
    }

    fun blendEquation(rgb: AGBlendEquation, a: AGBlendEquation) {
        gl.blendEquationSeparate(rgb.toGl(), a.toGl())
    }

    fun blendFunction(srcRgb: AGBlendFactor, dstRgb: AGBlendFactor, srcA: AGBlendFactor, dstA: AGBlendFactor) {
        gl.blendFuncSeparate(srcRgb.toGl(), dstRgb.toGl(), srcA.toGl(), dstA.toGl())
    }

    fun cullFace(face: AGCullFace) {
        gl.cullFace(face.toGl())
    }

    fun frontFace(face: AGFrontFace) {
        gl.frontFace(face.toGl())
    }

    fun depthFunction(depthTest: AGCompareMode) {
        gl.depthFunc(depthTest.toGl())
    }

    ///////////////////////////////////////
    // PROGRAMS
    ///////////////////////////////////////

    // BUFFERS
    class BufferInfo(val id: Int) {
        var glId = 0
        var cachedVersion = -1
    }

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

    ///////////////////////////////////////
    // UNIFORMS
    ///////////////////////////////////////
    fun depthRange(near: Float, far: Float) {
        gl.depthRangef(near, far)
    }

    fun stencilFunction(compareMode: AGCompareMode, referenceValue: Int, readMask: Int) {
        gl.stencilFunc(compareMode.toGl(), referenceValue, readMask)
    }

    // @TODO: Separate
    fun stencilOperation(
        actionOnDepthFail: AGStencilOp,
        actionOnDepthPassStencilFail: AGStencilOp,
        actionOnBothPass: AGStencilOp
    ) {
        gl.stencilOp(actionOnDepthFail.toGl(), actionOnDepthPassStencilFail.toGl(), actionOnBothPass.toGl())
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
                        textureSetWrap(tex)
                        textureSetFilter(tex, unit.linear, unit.trilinear ?: unit.linear)
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


    fun textureSetFilter(tex: AGTexture, linear: Boolean, trilinear: Boolean = linear) {
        val minFilter = if (tex.mipmaps) {
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

        gl.texParameteri(tex.implForcedTexTarget.toGl(), KmlGl.TEXTURE_MIN_FILTER, minFilter)
        gl.texParameteri(tex.implForcedTexTarget.toGl(), KmlGl.TEXTURE_MAG_FILTER, magFilter)
    }

    fun textureSetWrap(tex: AGTexture) {
        gl.texParameteri(tex.implForcedTexTarget.toGl(), KmlGl.TEXTURE_WRAP_S, KmlGl.CLAMP_TO_EDGE)
        gl.texParameteri(tex.implForcedTexTarget.toGl(), KmlGl.TEXTURE_WRAP_T, KmlGl.CLAMP_TO_EDGE)
        if (tex.implForcedTexTarget.dims >= 3) gl.texParameteri(tex.implForcedTexTarget.toGl(), KmlGl.TEXTURE_WRAP_R, KmlGl.CLAMP_TO_EDGE)
    }

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

    // TEXTURES
    class TextureInfo(val id: Int) {
        var glId: Int = -1
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

    // FRAME BUFFER
    inner class FrameBufferInfo {
        var cachedVersion = -1
        var texColor = -1
        var renderbuffer = -1
        var framebuffer = -1
        var width = -1
        var height = -1
        var hasDepth = false
        var hasStencil = false
        var nsamples: Int = 1

        val hasStencilAndDepth: Boolean get() = when {
            //gl.android -> hasStencil || hasDepth // stencil8 causes strange bug artifacts in Android (at least in one of my devices)
            else -> hasStencil && hasDepth
        }
    }

    private var currentTextureUnit = 0
    private fun selectTextureUnit(index: Int): Int {
        val old = currentTextureUnit
        currentTextureUnit = index
        gl.activeTexture(KmlGl.TEXTURE0 + index)
        return old
    }

    fun frameBufferSet(frameBuffer: AGFrameBuffer) {
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
        }

        gl.bindFramebuffer(KmlGl.FRAMEBUFFER, fb.frameBufferId)
        gl.framebufferTexture2D(KmlGl.FRAMEBUFFER, KmlGl.COLOR_ATTACHMENT0, KmlGl.TEXTURE_2D, fb.ag.tex.gl.id, 0)
        if (internalFormat != 0) {
            gl.framebufferRenderbuffer(KmlGl.FRAMEBUFFER, internalFormat, KmlGl.RENDERBUFFER, fb.renderBufferId)
        } else {
            gl.framebufferRenderbuffer(KmlGl.FRAMEBUFFER, KmlGl.STENCIL_ATTACHMENT, KmlGl.RENDERBUFFER, 0)
            gl.framebufferRenderbuffer(KmlGl.DEPTH_ATTACHMENT, KmlGl.STENCIL_ATTACHMENT, KmlGl.RENDERBUFFER, 0)
        }
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


    fun setBlendingState(blending: AGBlending? = null) {
        val blending = blending ?: AGBlending.NORMAL
        enableDisable(AGEnable.BLEND, blending.enabled) {
            blendEquation(blending.eqRGB, blending.eqA)
            blendFunction(blending.srcRGB, blending.dstRGB, blending.srcA, blending.dstA)
        }
    }

    fun setRenderState(renderState: AGDepthAndFrontFace) {
        enableDisable(AGEnable.CULL_FACE, renderState.frontFace != AGFrontFace.BOTH) {
            frontFace(renderState.frontFace)
        }

        gl.depthMask(renderState.depthMask)
        depthRange(renderState.depthNear, renderState.depthFar)

        enableDisable(AGEnable.DEPTH, renderState.depthFunc != AGCompareMode.ALWAYS) {
            depthFunction(renderState.depthFunc)
        }
    }

    fun setColorMaskState(colorMask: AGColorMask?) {
        colorMask(colorMask?.red ?: true, colorMask?.green ?: true, colorMask?.blue ?: true, colorMask?.alpha ?: true)
    }

    fun setStencilState(stencilOpFunc: AGStencilOpFunc?, stencilRef: AGStencilReference) {
        if (stencilOpFunc != null && stencilOpFunc.enabled) {
            enable(AGEnable.STENCIL)
            stencilFunction(stencilOpFunc.compareMode, stencilRef.referenceValue, stencilRef.readMask)
            stencilOperation(
                stencilOpFunc.actionOnDepthFail,
                stencilOpFunc.actionOnDepthPassStencilFail,
                stencilOpFunc.actionOnBothPass
            )
            gl.stencilMask(stencilRef.writeMask)
        } else {
            disable(AGEnable.STENCIL)
            gl.stencilMask(0)
        }
    }

    fun setScissorState(ag: AG, scissor: AGScissor = AGScissor.NIL) =
        setScissorState(ag.currentRenderBuffer, ag.mainRenderBuffer, scissor)

    fun setScissorState(currentRenderBuffer: AGBaseFrameBuffer?, mainRenderBuffer: AGBaseFrameBuffer, scissor: AGScissor = AGScissor.NIL) {
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

    fun setState(
        blending: AGBlending = AGBlending.NORMAL,
        stencilOpFunc: AGStencilOpFunc = AGStencilOpFunc.DEFAULT,
        stencilRef: AGStencilReference = AGStencilReference.DEFAULT,
        colorMask: AGColorMask = AGColorMask.ALL_ENABLED,
        renderState: AGDepthAndFrontFace = AGDepthAndFrontFace.DEFAULT,
    ) {
        setBlendingState(blending)
        setRenderState(renderState)
        setColorMaskState(colorMask)
        setStencilState(stencilOpFunc, stencilRef)
    }

}

enum class AGEnable {
    BLEND, CULL_FACE, DEPTH, SCISSOR, STENCIL;
}

class AGGlobalState(val checked: Boolean = false) {
    internal var contextVersion = 0
    internal var renderThreadId: Long = -1L
    internal var renderThreadName: String? = null
    //var programIndex = KorAtomicInt(0)
    private val lock = NonRecursiveLock()
}

@SharedImmutable
val KmlGl.versionString by Extra.PropertyThis<KmlGl, String> {
    getString(SHADING_LANGUAGE_VERSION)
}

@SharedImmutable
val KmlGl.versionInt by Extra.PropertyThis<KmlGl, Int> {
    versionString.replace(".", "").trim().toIntOrNull() ?: 100
}
