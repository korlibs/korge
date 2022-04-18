package com.soywiz.korag.gl

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kgl.*
import com.soywiz.klock.*
import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.annotation.*
import com.soywiz.korag.internal.setFloats
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.vector.BitmapVector
import com.soywiz.korio.annotations.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.krypto.encoding.*
import kotlin.jvm.JvmOverloads
import kotlin.math.*
import kotlin.native.concurrent.*

open class SimpleAGOpengl<TKmlGl : KmlGl>(override val gl: TKmlGl, override val nativeComponent: Any = Unit) : AGOpengl()

@OptIn(KorIncomplete::class, KorInternal::class, KoragExperimental::class)
abstract class AGOpengl : AG() {
    class ShaderException(val str: String, val error: String, val errorInt: Int, val gl: KmlGl) :
        RuntimeException("Error Compiling Shader : ${errorInt.hex} : '$error' : source='$str', gl.versionInt=${gl.versionInt}, gl.versionString='${gl.versionString}', gl=$gl")

    open var isGlAvailable = true
    abstract val gl: KmlGl

    override val graphicExtensions: Set<String> get() = gl.graphicExtensions
    override val isFloatTextureSupported: Boolean get() = gl.isFloatTextureSupported
    override val isInstancedSupported: Boolean get() = gl.isInstancedSupported

    open val glSlVersion: Int? = null
    open val gles: Boolean get() = false
    open val linux: Boolean get() = false
    open val android: Boolean = OS.isAndroid
    open val webgl: Boolean get() = false
    open val webgl2: Boolean get() = false

    override fun contextLost() {
        Console.info("AG.contextLost()", this, gl, gl.root)
        contextVersion++
    }

    //val queue = Deque<(gl: GL) -> Unit>()

    override fun createBuffer(kind: Buffer.Kind): Buffer = GlBuffer(kind)

    open fun setSwapInterval(value: Int) {
        //gl.swapInterval = 0
    }

    private fun setViewport(buffer: BaseRenderBuffer) {
        gl.viewport(buffer.x, buffer.y, buffer.width, buffer.height)
        //println("setViewport: ${buffer.x}, ${buffer.y}, ${buffer.width}, ${buffer.height}")
    }

    override fun createMainRenderBuffer(): BaseRenderBufferImpl {
        var backBufferTextureBinding2d: Int = 0
        var backBufferRenderBufferBinding: Int = 0
        var backBufferFrameBufferBinding: Int = 0

        return object : BaseRenderBufferImpl() {
            override fun init() {
                backBufferTextureBinding2d = gl.getIntegerv(KmlGl.TEXTURE_BINDING_2D)
                backBufferRenderBufferBinding = gl.getIntegerv(KmlGl.RENDERBUFFER_BINDING)
                backBufferFrameBufferBinding = gl.getIntegerv(KmlGl.FRAMEBUFFER_BINDING)
            }

            override fun set() {
                setViewport(this)
                gl.bindTexture(KmlGl.TEXTURE_2D, backBufferTextureBinding2d)
                gl.bindRenderbuffer(KmlGl.RENDERBUFFER, backBufferRenderBufferBinding)
                gl.bindFramebuffer(KmlGl.FRAMEBUFFER, backBufferFrameBufferBinding)
            }

            override fun unset() {
                backBufferTextureBinding2d = gl.getIntegerv(KmlGl.TEXTURE_BINDING_2D)
                backBufferRenderBufferBinding = gl.getIntegerv(KmlGl.RENDERBUFFER_BINDING)
                backBufferFrameBufferBinding = gl.getIntegerv(KmlGl.FRAMEBUFFER_BINDING)
            }
        }
    }

    fun createGlState() = KmlGlState(gl)

    var lastRenderContextId = 0

    inner class GlRenderBuffer : RenderBuffer() {
        var cachedVersion = -1
        override val id = lastRenderContextId++

        val ftex get() = tex as GlTexture

        val depth = FBuffer(4)
        val framebuffer = FBuffer(4)

        // http://wangchuan.github.io/coding/2016/05/26/multisampling-fbo.html
        override fun set() {
            setViewport(this)

            val hasStencilAndDepth: Boolean = when {
                android -> hasStencil || hasDepth // stencil8 causes strange bug artifacts in Android (at least in one of my devices)
                else -> hasStencil && hasDepth
            }

            //val width = this.width.nextPowerOfTwo
            //val height = this.height.nextPowerOfTwo
            if (dirty) {
                dirty = false
                setSwapInterval(0)

                if (cachedVersion != contextVersion) {
                    cachedVersion = contextVersion
                    gl.genRenderbuffers(1, depth)
                    gl.genFramebuffers(1, framebuffer)
                }

                //val doMsaa = nsamples != 1
                val doMsaa = false
                val texTarget = when {
                    doMsaa -> KmlGl.TEXTURE_2D_MULTISAMPLE
                    else -> KmlGl.TEXTURE_2D
                }

                gl.bindTexture(texTarget, ftex.tex)
                gl.texParameteri(texTarget, KmlGl.TEXTURE_MAG_FILTER, KmlGl.LINEAR)
                gl.texParameteri(texTarget, KmlGl.TEXTURE_MIN_FILTER, KmlGl.LINEAR)
                if (doMsaa) {
                    gl.texImage2DMultisample(texTarget, nsamples, gl.RGBA, width, height, false)
                } else {
                    gl.texImage2D(texTarget, 0, KmlGl.RGBA, width, height, 0, KmlGl.RGBA, KmlGl.UNSIGNED_BYTE, null)
                }
                gl.bindTexture(texTarget, 0)
                gl.bindRenderbuffer(KmlGl.RENDERBUFFER, depth.getInt(0))
                val internalFormat = when {
                    hasStencilAndDepth -> KmlGl.DEPTH_STENCIL
                    hasStencil -> KmlGl.STENCIL_INDEX8 // On android this is buggy somehow?
                    hasDepth -> KmlGl.DEPTH_COMPONENT
                    else -> 0
                }
                if (internalFormat != 0) {
                    if (doMsaa) {
                        gl.renderbufferStorageMultisample(KmlGl.RENDERBUFFER, nsamples, internalFormat, width, height)
                        //gl.renderbufferStorage(KmlGl.RENDERBUFFER, internalFormat, width, height)
                    } else {
                        gl.renderbufferStorage(KmlGl.RENDERBUFFER, internalFormat, width, height)
                    }
                }
                gl.bindRenderbuffer(KmlGl.RENDERBUFFER, 0)
                //gl.renderbufferStorageMultisample()
            }

            gl.bindFramebuffer(KmlGl.FRAMEBUFFER, framebuffer.getInt(0))
            gl.framebufferTexture2D(KmlGl.FRAMEBUFFER, KmlGl.COLOR_ATTACHMENT0, KmlGl.TEXTURE_2D, ftex.tex, 0)
            val internalFormat = when {
                hasStencilAndDepth -> KmlGl.DEPTH_STENCIL_ATTACHMENT
                hasStencil -> KmlGl.STENCIL_ATTACHMENT
                hasDepth -> KmlGl.DEPTH_ATTACHMENT
                else -> 0
            }
            if (internalFormat != 0) {
                gl.framebufferRenderbuffer(KmlGl.FRAMEBUFFER, internalFormat, KmlGl.RENDERBUFFER, depth.getInt(0))
            }
            //val status = gl.checkFramebufferStatus(KmlGl.FRAMEBUFFER);
            //if (status != KmlGl.FRAMEBUFFER_COMPLETE) error("Error getting framebuffer")
            //gl.bindFramebuffer(KmlGl.FRAMEBUFFER, 0)
        }

        override fun close() {
            super.close()
            gl.deleteFramebuffers(1, framebuffer)
            gl.deleteRenderbuffers(1, depth)
            framebuffer.setInt(0, 0)
            depth.setInt(0, 0)
        }

        override fun toString(): String = "GlRenderBuffer[$id]($width, $height)"
    }

    override fun createRenderBuffer(): RenderBuffer = GlRenderBuffer()


    private val TEMP_MAX_MATRICES = 1024
    val tempBuffer1 = FBuffer(4)
    val tempBuffer = FBuffer(4 * 16 * TEMP_MAX_MATRICES)
    val tempBufferM2 = FBuffer.allocUnaligned(4 * 2 * 2)
    val tempBufferM3 = FBuffer.allocUnaligned(4 * 3 * 3)
    val tempBufferM4 = FBuffer.allocUnaligned(4 * 4 * 4)
    val tempF32 = tempBuffer.f32

    private val tempFloats = FloatArray(16 * TEMP_MAX_MATRICES)
    private val mat3dArray = arrayOf(Matrix3D())

    private val glProcessor = AGQueueProcessorOpenGL(KmlGlDummy)

    override fun executeList(list: AGList) {
        glProcessor.gl = gl
        glProcessor.processBlockingAll(list)
    }

    override fun draw(batch: Batch) {
        val instances = batch.instances
        val program = batch.program
        val type = batch.type
        val vertexCount = batch.vertexCount
        val indices = batch.indices
        val indexType = batch.indexType
        val offset = batch.offset
        val blending = batch.blending
        val uniforms = batch.uniforms
        val stencil = batch.stencil
        val colorMask = batch.colorMask
        val renderState = batch.renderState
        val scissor = batch.scissor

        //println("SCISSOR: $scissor")

        //finalScissor.setTo(0, 0, backWidth, backHeight)
        commands { list ->
            applyScissorState(list, scissor)
        }

        var useExternalSampler = false
        for (n in 0 until uniforms.uniforms.size) {
            val uniform = uniforms.uniforms[n]
            val uniformType = uniform.type
            val value = uniforms.values[n]
            when (uniformType) {
                VarType.Sampler2D -> {
                    val unit = value.fastCastTo<TextureUnit>()
                    val tex = (unit.texture.fastCastTo<GlTexture?>())
                    if (tex != null) {
                        if (tex.forcedTexTarget != KmlGl.TEXTURE_2D && tex.forcedTexTarget != -1) {
                            useExternalSampler = true
                        }
                    }
                }
            }
        }

        if (indices != null && indices.kind != Buffer.Kind.INDEX) invalidOp("Not a IndexBuffer")

        val programConfig = if (useExternalSampler) ProgramConfig.EXTERNAL_TEXTURE_SAMPLER else ProgramConfig.DEFAULT
        val glProgram = getProgram(program, programConfig)
        (indices as? GlBuffer?)?.bind(gl)
        glProgram.use()

        val vaoId: Int
        commands { list ->
            vaoId = list.vaoCreate()
            list.vaoSet(vaoId, VertexArrayObject(batch.vertexData))
            list.vaoUse(vaoId, glProgram.programInfo)
        }

        var textureUnit = 0
        //for ((uniform, value) in uniforms) {
        for (n in 0 until uniforms.uniforms.size) {
            val uniform = uniforms.uniforms[n]
            val uniformName = uniform.name
            val uniformType = uniform.type
            val value = uniforms.values[n]
            val location = glProgram.getUniformLocation(uniformName)
            val declArrayCount = uniform.arrayCount
            val stride = uniform.type.elementCount

            //println("uniform: $uniform, arrayCount=$arrayCount, stride=$stride")

            when (uniformType) {
                VarType.Sampler2D, VarType.SamplerCube -> {
                    val unit = value.fastCastTo<TextureUnit>()
                    gl.activeTexture(KmlGl.TEXTURE0 + textureUnit)

                    val tex = (unit.texture.fastCastTo<GlTexture?>())
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

                    if (webgl) {
                        //if (true) {
                        val tb = when (uniformType) {
                            VarType.Mat2 -> tempBufferM2
                            VarType.Mat3 -> tempBufferM3
                            VarType.Mat4 -> tempBufferM4
                            else -> tempBufferM4
                        }

                        for (n in 0 until arrayCount) {
                            val itLocation = if (arrayCount == 1) location else gl.getUniformLocation(glProgram.id, uniform.indexNames[n])
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
                    if (webgl) {
                        val tb = tempBufferM2
                        for (n in 0 until arrayCount) {
                            val itLocation = if (arrayCount == 1) location else gl.getUniformLocation(glProgram.id, uniform.indexNames[n])
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

        commands { list ->
            list.enableDisable(AGEnable.BLEND, blending.enabled) {
                list.blendEquation(blending.eqRGB, blending.eqA)
                list.blendFunction(blending.srcRGB, blending.dstRGB, blending.srcA, blending.dstA)
            }

            list.enableDisable(AGEnable.CULL_FACE, renderState.frontFace != FrontFace.BOTH) {
                list.frontFace(renderState.frontFace)
            }

            list.depthMask(renderState.depthMask)
            list.depthRange(renderState.depthNear, renderState.depthFar)

            list.enableDisable(AGEnable.DEPTH, renderState.depthFunc != CompareMode.ALWAYS) {
                list.depthFunction(renderState.depthFunc)
            }

            list.colorMask(colorMask.red, colorMask.green, colorMask.blue, colorMask.alpha)

            if (stencil.enabled) {
                list.enable(AGEnable.STENCIL)
                list.stencilFunction(stencil.compareMode, stencil.referenceValue, stencil.readMask)
                list.stencilOperation(
                    stencil.actionOnDepthFail,
                    stencil.actionOnDepthPassStencilFail,
                    stencil.actionOnBothPass
                )
                list.stencilMask(stencil.writeMask)
            } else {
                list.disable(AGEnable.STENCIL)
                list.stencilMask(0)
            }

            //val viewport = FBuffer(4 * 4)
            //gl.getIntegerv(KmlGl.VIEWPORT, viewport)
            //println("viewport=${viewport.getAlignedInt32(0)},${viewport.getAlignedInt32(1)},${viewport.getAlignedInt32(2)},${viewport.getAlignedInt32(3)}")

            list.draw(type, vertexCount, offset, instances, if (indices != null) indexType else null)

            list.vaoUse(0, glProgram.programInfo)
            list.vaoDelete(vaoId)
        }
    }

    private val programs = FastIdentityMap<Program, FastIdentityMap<ProgramConfig, GlProgram>>()

    @JvmOverloads
    fun getProgram(program: Program, config: ProgramConfig = ProgramConfig.DEFAULT): GlProgram {
        return programs.getOrPut(program) { FastIdentityMap() }.getOrPut(config) { GlProgram(gl, program, config) }
    }

    inner class GlProgram(val gl: KmlGl, val program: Program, val programConfig: ProgramConfig) : Closeable {
        var cachedVersion = -1
        var programInfo: GLProgramInfo? = null

        val id: Int get() = programInfo?.programId ?: 0

        fun getAttribLocation(name: String): Int = programInfo?.getAttribLocation(gl, name) ?: 0
        fun getUniformLocation(name: String): Int = programInfo?.getUniformLocation(gl, name) ?: 0

        private fun ensure() {
            if (cachedVersion != contextVersion) {
                val time = measureTime {
                    cachedVersion = contextVersion
                    programInfo = GLShaderCompiler.programCreate(
                        gl, GlslConfig(gles = gles, android = android, programConfig = programConfig),
                        program,
                        glSlVersion
                    )
                }
                if (GlslGenerator.DEBUG_GLSL) {
                    Console.info("OpenglAG: Created program ${program.name} with id ${programInfo?.programId} in time=$time")
                }
            }
        }

        fun use() {
            ensure()
            programInfo?.use(gl)
        }

        fun unuse() {
            ensure()
            gl.useProgram(0)
        }

        override fun close() {
            programInfo?.delete(gl)
            programInfo = null
        }
    }

    override fun createTexture(premultiplied: Boolean, targetKind: TextureTargetKind): Texture =
        GlTexture(this.gl, premultiplied, targetKind)

    inner class GlBuffer(kind: Kind) : Buffer(kind) {
        var cachedVersion = -1
        private var id = -1
        val glKind = if (kind == Kind.INDEX) KmlGl.ELEMENT_ARRAY_BUFFER else KmlGl.ARRAY_BUFFER

        override fun afterSetMem() {
        }

        override fun close() {
            fbuffer(4) { buffer ->
                buffer.setInt(0, this.id)
                gl.deleteBuffers(1, buffer)
            }
            id = -1
        }

        fun getGlId(gl: KmlGl): Int {
            if (cachedVersion != contextVersion) {
                cachedVersion = contextVersion
                dirty = true
                id = -1
            }
            if (id < 0) {
                id = fbuffer(4) {
                    gl.genBuffers(1, it)
                    it.getInt(0)
                }
            }
            if (dirty) {
                _bind(gl, id)
                if (mem != null) {
                    gl.bufferData(glKind, memLength, mem!!, KmlGl.STATIC_DRAW)
                }
            }
            return id
        }

        fun _bind(gl: KmlGl, id: Int) {
            gl.bindBuffer(glKind, id)
        }

        fun bind(gl: KmlGl) {
            _bind(gl, getGlId(gl))
        }
    }

    open fun prepareUploadNativeTexture(bmp: NativeImage) {
    }

    inner class GlTexture(
        val gl: KmlGl,
        override val premultiplied: Boolean,
        val targetKind: TextureTargetKind = TextureTargetKind.TEXTURE_2D
    ) : Texture() {
        var cachedVersion = -1
        val texIds = FBuffer(4)

        var forcedTexId: Int = -1
        var forcedTexTarget: Int = targetKind.toGl()

        val tex: Int
            get() {
                if (forcedTexId >= 0) return forcedTexId
                if (cachedVersion != contextVersion) {
                    cachedVersion = contextVersion
                    invalidate()
                    gl.genTextures(1, texIds)
                }
                return texIds.getInt(0)
            }

        override val nativeTexId: Int get() = tex

        fun createBufferForBitmap(bmp: Bitmap?): FBuffer? = when (bmp) {
            null -> null
            is NativeImage -> unsupported("Should not call createBufferForBitmap with a NativeImage")
            is Bitmap8 -> FBuffer(bmp.area).also { mem -> arraycopy(bmp.data, 0, mem.arrayByte, 0, bmp.area) }
            is FloatBitmap32 -> FBuffer(bmp.area * 4 * 4).also { mem -> arraycopy(bmp.data, 0, mem.arrayFloat, 0, bmp.area * 4) }
            else -> FBuffer(bmp.area * 4).also { mem ->
                val abmp: Bitmap32 = if (premultiplied) bmp.toBMP32IfRequired().premultipliedIfRequired() else bmp.toBMP32IfRequired().depremultipliedIfRequired()
                arraycopy(abmp.data.ints, 0, mem.arrayInt, 0, abmp.area)
            }
        }

        override fun actualSyncUpload(source: BitmapSourceBase, bmps: List<Bitmap?>?, requestMipmaps: Boolean) {
            this.mipmaps = false

            val bytesPerPixel = if (source.rgba) 4 else 1
            val type = when {
                source.rgba -> KmlGl.RGBA //if (source is NativeImage) KmlGl.BGRA else KmlGl.RGBA
                else -> KmlGl.LUMINANCE
            }

            //this.bind() // Already bound

            if (bmps != null) {
                for ((index, rbmp) in bmps.withIndex()) {
                    val bmp = when (rbmp) {
                        is BitmapVector -> rbmp.nativeImage
                        else -> rbmp
                    }
                    val isFloat = bmp is FloatBitmap32

                    when {
                        bmp is ForcedTexId -> {
                            this.forcedTexId = bmp.forcedTexId
                            if (bmp is ForcedTexTarget) this.forcedTexTarget = bmp.forcedTexTarget
                            return
                        }
                        bmp is NativeImage && bmp.forcedTexId != -1 -> {
                            this.forcedTexId = bmp.forcedTexId
                            if (bmp.forcedTexTarget != -1) this.forcedTexTarget = bmp.forcedTexTarget
                            gl.bindTexture(forcedTexTarget, forcedTexId) // @TODO: Check. Why do we need to bind it now?
                            return
                        }
                    }

                    val texTarget = when (forcedTexTarget) {
                        KmlGl.TEXTURE_CUBE_MAP -> KmlGl.TEXTURE_CUBE_MAP_POSITIVE_X + index
                        else -> forcedTexTarget
                    }

                    if (bmp is NativeImage) {
                        prepareUploadNativeTexture(bmp)
                        if (bmp.area != 0) {
                            prepareTexImage2D()
                            gl.texImage2D(texTarget, 0, type, type, KmlGl.UNSIGNED_BYTE, bmp)
                        }
                    } else {
                        val buffer = createBufferForBitmap(bmp)
                        if (buffer != null && source.width != 0 && source.height != 0 && buffer.size != 0) {
                            prepareTexImage2D()
                            val internalFormat = when {
                                isFloat && (webgl2 || !webgl) -> GL_RGBA32F
                                else -> type
                            }
                            val format = type
                            val texType = when {
                                isFloat -> KmlGl.FLOAT
                                else -> KmlGl.UNSIGNED_BYTE
                            }
                            //println("actualSyncUpload: webgl=$webgl, internalFormat=${internalFormat.hex}, format=${format.hex}, textype=${texType.hex}")
                            gl.texImage2D(texTarget, 0, internalFormat, source.width, source.height, 0, format, texType, buffer)
                        }
                    }
                    //println(buffer)
                }
            } else {
                gl.texImage2D(forcedTexTarget, 0, type, source.width, source.height, 0, type, KmlGl.UNSIGNED_BYTE, null)
            }

            if (requestMipmaps && source.width.isPowerOfTwo && source.height.isPowerOfTwo) {
                //println(" - mipmaps")
                this.mipmaps = true
                bind()
                //setFilter(true)
                //setWrap()
                //println("actualSyncUpload,generateMipmap.SOURCE: ${source.width},${source.height}, source=$source, bmp=$bmp, requestMipmaps=$requestMipmaps")
                //printStackTrace()
                gl.generateMipmap(forcedTexTarget)
            } else {
                //println(" - nomipmaps")
            }
        }

        private val GL_RGBA32F = 0x8814

        // https://download.blender.org/source/chest/blender_1.72_tree/glut-win/glut_bitmap.c
        private val GL_UNPACK_ALIGNMENT = 0x0CF5
        private val GL_UNPACK_LSB_FIRST = 0x0CF1
        private val GL_UNPACK_ROW_LENGTH = 0x0CF2
        private val GL_UNPACK_SKIP_PIXELS = 0x0CF4
        private val GL_UNPACK_SKIP_ROWS = 0x0CF3
        private val GL_UNPACK_SWAP_BYTES = 0x0CF0
        fun prepareTexImage2D() {
            if (linux) {
                //println("prepareTexImage2D")
                //gl.pixelStorei(GL_UNPACK_LSB_FIRST, KmlGl.TRUE)
                gl.pixelStorei(GL_UNPACK_LSB_FIRST, KmlGl.GFALSE)
                gl.pixelStorei(GL_UNPACK_SWAP_BYTES, KmlGl.GTRUE)
            }
        }

        override fun bind(): Unit = gl.bindTexture(forcedTexTarget, tex)
        override fun unbind(): Unit = gl.bindTexture(forcedTexTarget, 0)

        private var closed = false
        override fun close(): Unit {
            super.close()
            if (!closed) {
                closed = true
                if (cachedVersion == contextVersion) {
                    gl.deleteTextures(1, texIds)
                    //println("DELETE texture: ${texIds[0]}")
                    texIds[0] = -1
                } else {
                    //println("YAY! NO DELETE texture because in new context and would remove the wrong texture: ${texIds[0]}")
                }
            }
        }

        fun setFilter(linear: Boolean, trilinear: Boolean = linear) {
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

            setMinMag(minFilter, magFilter)
        }

        fun setWrap() {
            gl.texParameteri(forcedTexTarget, KmlGl.TEXTURE_WRAP_S, KmlGl.CLAMP_TO_EDGE)
            gl.texParameteri(forcedTexTarget, KmlGl.TEXTURE_WRAP_T, KmlGl.CLAMP_TO_EDGE)
            if (forcedTexTarget == KmlGl.TEXTURE_CUBE_MAP || forcedTexTarget == KmlGl.TEXTURE_3D) {
                gl.texParameteri(forcedTexTarget, KmlGl.TEXTURE_WRAP_R, KmlGl.CLAMP_TO_EDGE)
            }
        }

        private fun setMinMag(min: Int, mag: Int) {
            gl.texParameteri(forcedTexTarget, KmlGl.TEXTURE_MIN_FILTER, min)
            gl.texParameteri(forcedTexTarget, KmlGl.TEXTURE_MAG_FILTER, mag)
        }

        override fun toString(): String = "AGOpengl.GlTexture($tex)"
    }

    override fun readColor(bitmap: Bitmap32) {
        fbuffer(bitmap.area * 4) { buffer ->
            gl.readPixels(
                0, 0, bitmap.width, bitmap.height,
                KmlGl.RGBA, KmlGl.UNSIGNED_BYTE, buffer
            )
            buffer.getAlignedArrayInt32(0, bitmap.data.ints, 0, bitmap.area)
            //println("readColor.HASH:" + bitmap.computeHash())
        }
    }

    override fun readDepth(width: Int, height: Int, out: FloatArray) {
        val area = width * height
        fbuffer(area * 4) { buffer ->
            gl.readPixels(
                0, 0, width, height, KmlGl.DEPTH_COMPONENT, KmlGl.FLOAT,
                buffer
            )
            buffer.getAlignedArrayFloat32(0, out, 0, area)
        }
    }

    override fun readStencil(bitmap: Bitmap8) {
        fbuffer(bitmap.area * 1) { buffer ->
            gl.readPixels(
                0, 0, bitmap.width, bitmap.height,
                KmlGl.STENCIL_INDEX, KmlGl.UNSIGNED_BYTE, buffer
            )
            buffer.getArrayInt8(0, bitmap.data, 0, bitmap.area)
            //println("readColor.HASH:" + bitmap.computeHash())
        }
    }




    override fun readColorTexture(texture: Texture, width: Int, height: Int) {
        gl.apply {
            texture.bind()
            copyTexImage2D(TEXTURE_2D, 0, RGBA, 0, 0, width, height, 0)
            texture.unbind()
        }
    }
}


@SharedImmutable
val KmlGl.versionString by Extra.PropertyThis<KmlGl, String> {
    getString(SHADING_LANGUAGE_VERSION)
}

@SharedImmutable
val KmlGl.versionInt by Extra.PropertyThis<KmlGl, Int> {
    versionString.replace(".", "").trim().toIntOrNull() ?: 100
}
