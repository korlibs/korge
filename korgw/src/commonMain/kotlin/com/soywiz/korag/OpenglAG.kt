package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kgl.*
import com.soywiz.klock.*
import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korag.internal.setFloats
import com.soywiz.korag.shader.Program
import com.soywiz.korag.shader.ProgramConfig
import com.soywiz.korag.shader.VarKind
import com.soywiz.korag.shader.VarType
import com.soywiz.korag.shader.gl.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.RGBA
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

@OptIn(KorIncomplete::class, KorInternal::class)
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
                backBufferTextureBinding2d = gl.getIntegerv(gl.TEXTURE_BINDING_2D)
                backBufferRenderBufferBinding = gl.getIntegerv(gl.RENDERBUFFER_BINDING)
                backBufferFrameBufferBinding = gl.getIntegerv(gl.FRAMEBUFFER_BINDING)
            }

            override fun set() {
                setViewport(this)
                gl.bindTexture(gl.TEXTURE_2D, backBufferTextureBinding2d)
                gl.bindRenderbuffer(gl.RENDERBUFFER, backBufferRenderBufferBinding)
                gl.bindFramebuffer(gl.FRAMEBUFFER, backBufferFrameBufferBinding)
            }

            override fun unset() {
                backBufferTextureBinding2d = gl.getIntegerv(gl.TEXTURE_BINDING_2D)
                backBufferRenderBufferBinding = gl.getIntegerv(gl.RENDERBUFFER_BINDING)
                backBufferFrameBufferBinding = gl.getIntegerv(gl.FRAMEBUFFER_BINDING)
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
                    doMsaa -> gl.TEXTURE_2D_MULTISAMPLE
                    else -> gl.TEXTURE_2D
                }

                gl.bindTexture(texTarget, ftex.tex)
                gl.texParameteri(texTarget, gl.TEXTURE_MAG_FILTER, gl.LINEAR)
                gl.texParameteri(texTarget, gl.TEXTURE_MIN_FILTER, gl.LINEAR)
                if (doMsaa) {
                    gl.texImage2DMultisample(texTarget, nsamples, gl.RGBA, width, height, false)
                } else {
                    gl.texImage2D(texTarget, 0, gl.RGBA, width, height, 0, gl.RGBA, gl.UNSIGNED_BYTE, null)
                }
                gl.bindTexture(texTarget, 0)
                gl.bindRenderbuffer(gl.RENDERBUFFER, depth.getInt(0))
                val internalFormat = when {
                    hasStencilAndDepth -> gl.DEPTH_STENCIL
                    hasStencil -> gl.STENCIL_INDEX8 // On android this is buggy somehow?
                    hasDepth -> gl.DEPTH_COMPONENT
                    else -> 0
                }
                if (internalFormat != 0) {
                    if (doMsaa) {
                        gl.renderbufferStorageMultisample(gl.RENDERBUFFER, nsamples, internalFormat, width, height)
                        //gl.renderbufferStorage(gl.RENDERBUFFER, internalFormat, width, height)
                    } else {
                        gl.renderbufferStorage(gl.RENDERBUFFER, internalFormat, width, height)
                    }
                }
                gl.bindRenderbuffer(gl.RENDERBUFFER, 0)
                //gl.renderbufferStorageMultisample()
            }

            gl.bindFramebuffer(gl.FRAMEBUFFER, framebuffer.getInt(0))
            gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.TEXTURE_2D, ftex.tex, 0)
            val internalFormat = when {
                hasStencilAndDepth -> gl.DEPTH_STENCIL_ATTACHMENT
                hasStencil -> gl.STENCIL_ATTACHMENT
                hasDepth -> gl.DEPTH_ATTACHMENT
                else -> 0
            }
            if (internalFormat != 0) {
                gl.framebufferRenderbuffer(gl.FRAMEBUFFER, internalFormat, gl.RENDERBUFFER, depth.getInt(0))
            }
            //val status = gl.checkFramebufferStatus(gl.FRAMEBUFFER);
            //if (status != gl.FRAMEBUFFER_COMPLETE) error("Error getting framebuffer")
            //gl.bindFramebuffer(gl.FRAMEBUFFER, 0)
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

    private val finalScissorBL = Rectangle()
    private val tempRect = Rectangle()

    fun applyScissorState(scissor: AG.Scissor? = null) {
        //println("applyScissorState")
        if (this.currentRenderBuffer == null) {
            //println("this.currentRenderBuffer == null")
        }
        val currentRenderBuffer = this.currentRenderBuffer ?: return
        if (currentRenderBuffer === mainRenderBuffer) {
            var realScissors: Rectangle? = finalScissorBL
            realScissors?.setTo(0.0, 0.0, realBackWidth.toDouble(), realBackHeight.toDouble())
            if (scissor != null) {
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

            gl.enable(gl.SCISSOR_TEST)
            if (realScissors != null) {
                gl.scissor(realScissors.x.toInt(), realScissors.y.toInt(), realScissors.width.toInt(), realScissors.height.toInt())
            } else {
                gl.scissor(0, 0, 0, 0)
            }
        } else {
            //println("[RENDER_TARGET] scissor: $scissor")

            gl.enableDisable(gl.SCISSOR_TEST, scissor != null)
            if (scissor != null) {
                gl.scissor(scissor.x.toIntRound(), scissor.y.toIntRound(), scissor.width.toIntRound(), scissor.height.toIntRound())
            }
        }
    }

    private val globalState = AGGlobalState()
    private val glProcessor = AGQueueProcessorOpenGL(KmlGlDummy)
    private val glList = globalState.createList()

    override fun draw(batch: Batch) {
        glProcessor.gl = gl

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
        applyScissorState(scissor)

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
                        if (tex.forcedTexTarget != gl.TEXTURE_2D && tex.forcedTexTarget != -1) {
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

        batch.vertexData.fastForEach { entry ->
            val vertices = entry.buffer as GlBuffer
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
                val loc = glProgram.getAttribLocation(att.name)
                val glElementType = att.type.toGl(gl)
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
                    gl.activeTexture(gl.TEXTURE0 + textureUnit)

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

        if (blending.enabled) {
            glList.enable(AGEnable.BLEND)
            glList.blendEquation(blending.eqRGB, blending.eqA)
            glList.blendFunction(blending.srcRGB, blending.dstRGB, blending.srcA, blending.dstA)
        } else {
            glList.disable(AGEnable.BLEND)
        }

        if (renderState.frontFace == FrontFace.BOTH) {
            glList.disable(AGEnable.CULL_FACE)
        } else {
            glList.enable(AGEnable.CULL_FACE)
            glList.frontFace(renderState.frontFace)
        }
        glProcessor.processBlockingAll(glList)

        gl.depthMask(renderState.depthMask)
        gl.depthRangef(renderState.depthNear, renderState.depthFar)
        //gl.lineWidth(renderState.lineWidth) // In WebGL this doesn't have effect, so let's ignore it: https://developer.mozilla.org/en-US/docs/Web/API/WebGLRenderingContext/lineWidth

        if (renderState.depthFunc != CompareMode.ALWAYS) {
            gl.enable(gl.DEPTH_TEST)
            gl.depthFunc(renderState.depthFunc.toGl(gl))
        } else {
            gl.disable(gl.DEPTH_TEST)
        }

        gl.colorMask(colorMask.red, colorMask.green, colorMask.blue, colorMask.alpha)

        if (stencil.enabled) {
            gl.enable(gl.STENCIL_TEST)
            gl.stencilFunc(stencil.compareMode.toGl(gl), stencil.referenceValue, stencil.readMask)
            gl.stencilOp(
                stencil.actionOnDepthFail.toGl(gl),
                stencil.actionOnDepthPassStencilFail.toGl(gl),
                stencil.actionOnBothPass.toGl(gl)
            )
            gl.stencilMask(stencil.writeMask)
        } else {
            gl.disable(gl.STENCIL_TEST)
            gl.stencilMask(0)
        }

        //val viewport = FBuffer(4 * 4)
        //gl.getIntegerv(gl.VIEWPORT, viewport)
        //println("viewport=${viewport.getAlignedInt32(0)},${viewport.getAlignedInt32(1)},${viewport.getAlignedInt32(2)},${viewport.getAlignedInt32(3)}")

        glList.draw(type, vertexCount, offset, instances, if (indices != null) indexType else null)
        glProcessor.processBlockingAll(glList)

        //glSetActiveTexture(gl.TEXTURE0)

        batch.vertexData.fastForEach { entry ->
            val vattrs = entry.layout.attributes
            vattrs.fastForEach { att ->
                if (att.active) {
                    val loc = glProgram.getAttribLocation(att.name).toInt()
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

    private val programs = FastIdentityMap<Program, FastIdentityMap<ProgramConfig, GlProgram>>()

    @JvmOverloads
    fun getProgram(program: Program, config: ProgramConfig = ProgramConfig.DEFAULT): GlProgram {
        return programs.getOrPut(program) { FastIdentityMap() }.getOrPut(config) { GlProgram(gl, program, config) }
    }

    inner class GlProgram(val gl: KmlGl, val program: Program, val programConfig: ProgramConfig) : Closeable {
        var cachedVersion = -1
        var id: Int = 0
        var fragmentShaderId: Int = 0
        var vertexShaderId: Int = 0

        val cachedAttribLocations = FastStringMap<Int>()
        val cachedUniformLocations = FastStringMap<Int>()

        fun getAttribLocation(name: String): Int =
            cachedAttribLocations.getOrPut(name) { gl.getAttribLocation(id, name) }

        fun getUniformLocation(name: String): Int =
            cachedUniformLocations.getOrPut(name) { gl.getUniformLocation(id, name) }

        private fun String.replaceVersion(version: Int) = this.replace("#version 100", "#version $version")

        private fun ensure() {
            if (cachedVersion != contextVersion) {
                val time = measureTime {
                    cachedVersion = contextVersion
                    val program = GLShaderCompiler.programCreate(
                        gl, GlslConfig(gles = gles, android = android, programConfig = programConfig),
                        program,
                        glSlVersion
                    )
                    id = program.programId
                    fragmentShaderId = program.fragmentId
                    vertexShaderId = program.vertexId
                }
                if (GlslGenerator.DEBUG_GLSL) {
                    Console.info("OpenglAG: Created program ${program.name} with id $id in time=$time")
                }
            }
        }

        fun use() {
            ensure()
            gl.useProgram(id)
        }

        fun unuse() {
            ensure()
            gl.useProgram(0)
        }

        override fun close() {
            gl.deleteShader(fragmentShaderId)
            gl.deleteShader(vertexShaderId)
            gl.deleteProgram(id)
        }
    }

    // @TODO: Kotlin inline bug
    //Back-end (JVM) Internal error: wrong code generated
    //org.jetbrains.kotlin.codegen.CompilationException Back-end (JVM) Internal error: Couldn't transform method node:
    //clear (IFIZZZ)V:
    override fun clear(
        color: RGBA,
        depth: Float,
        stencil: Int,
        clearColor: Boolean,
        clearDepth: Boolean,
        clearStencil: Boolean,
        scissor: AG.Scissor?,
    ) {
        //println("CLEAR: $color, $depth")
        var bits = 0
        applyScissorState(scissor)
        //gl.disable(gl.SCISSOR_TEST)
        if (clearColor) {
            bits = bits or gl.COLOR_BUFFER_BIT
            gl.colorMask(true, true, true, true)
            gl.clearColor(color.rf, color.gf, color.bf, color.af)
        }
        if (clearDepth) {
            bits = bits or gl.DEPTH_BUFFER_BIT
            gl.depthMask(true)
            gl.clearDepthf(depth)
        }
        if (clearStencil) {
            bits = bits or gl.STENCIL_BUFFER_BIT
            gl.stencilMask(-1)
            gl.clearStencil(stencil)
        }
        gl.clear(bits)
    }

    override fun createTexture(premultiplied: Boolean, targetKind: TextureTargetKind): Texture =
        GlTexture(this.gl, premultiplied, targetKind)

    inner class GlBuffer(kind: Buffer.Kind) : Buffer(kind) {
        var cachedVersion = -1
        private var id = -1
        val glKind = if (kind == Buffer.Kind.INDEX) gl.ELEMENT_ARRAY_BUFFER else gl.ARRAY_BUFFER

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
                    gl.bufferData(glKind, memLength, mem!!, gl.STATIC_DRAW)
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
        var forcedTexTarget: Int = targetKind.toGl(gl)

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
                source.rgba -> gl.RGBA //if (source is NativeImage) gl.BGRA else gl.RGBA
                else -> gl.LUMINANCE
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
                        gl.TEXTURE_CUBE_MAP -> gl.TEXTURE_CUBE_MAP_POSITIVE_X + index
                        else -> forcedTexTarget
                    }

                    if (bmp is NativeImage) {
                        prepareUploadNativeTexture(bmp)
                        if (bmp.area != 0) {
                            prepareTexImage2D()
                            gl.texImage2D(texTarget, 0, type, type, gl.UNSIGNED_BYTE, bmp)
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
                                isFloat -> gl.FLOAT
                                else -> gl.UNSIGNED_BYTE
                            }
                            //println("actualSyncUpload: webgl=$webgl, internalFormat=${internalFormat.hex}, format=${format.hex}, textype=${texType.hex}")
                            gl.texImage2D(texTarget, 0, internalFormat, source.width, source.height, 0, format, texType, buffer)
                        }
                    }
                    //println(buffer)
                }
            } else {
                gl.texImage2D(forcedTexTarget, 0, type, source.width, source.height, 0, type, gl.UNSIGNED_BYTE, null)
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
                //gl.pixelStorei(GL_UNPACK_LSB_FIRST, gl.TRUE)
                gl.pixelStorei(GL_UNPACK_LSB_FIRST, gl.GFALSE)
                gl.pixelStorei(GL_UNPACK_SWAP_BYTES, gl.GTRUE)
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
                        trilinear -> gl.LINEAR_MIPMAP_LINEAR
                        else -> gl.LINEAR_MIPMAP_NEAREST
                    }
                    else -> when {
                        trilinear -> gl.NEAREST_MIPMAP_LINEAR
                        else -> gl.NEAREST_MIPMAP_NEAREST
                    }
                }
            } else {
                if (linear) gl.LINEAR else gl.NEAREST
            }
            val magFilter = if (linear) gl.LINEAR else gl.NEAREST

            setMinMag(minFilter, magFilter)
        }

        fun setWrap() {
            gl.texParameteri(forcedTexTarget, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE)
            gl.texParameteri(forcedTexTarget, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE)
            if (forcedTexTarget == gl.TEXTURE_CUBE_MAP || forcedTexTarget == gl.TEXTURE_3D) {
                gl.texParameteri(forcedTexTarget, gl.TEXTURE_WRAP_R, gl.CLAMP_TO_EDGE)
            }
        }

        private fun setMinMag(min: Int, mag: Int) {
            gl.texParameteri(forcedTexTarget, gl.TEXTURE_MIN_FILTER, min)
            gl.texParameteri(forcedTexTarget, gl.TEXTURE_MAG_FILTER, mag)
        }

        override fun toString(): String = "AGOpengl.GlTexture($tex)"
    }

    override fun readColor(bitmap: Bitmap32) {
        fbuffer(bitmap.area * 4) { buffer ->
            gl.readPixels(
                0, 0, bitmap.width, bitmap.height,
                gl.RGBA, gl.UNSIGNED_BYTE, buffer
            )
            buffer.getAlignedArrayInt32(0, bitmap.data.ints, 0, bitmap.area)
            //println("readColor.HASH:" + bitmap.computeHash())
        }
    }

    override fun readDepth(width: Int, height: Int, out: FloatArray) {
        val area = width * height
        fbuffer(area * 4) { buffer ->
            gl.readPixels(
                0, 0, width, height, gl.DEPTH_COMPONENT, gl.FLOAT,
                buffer
            )
            buffer.getAlignedArrayFloat32(0, out, 0, area)
        }
    }

    override fun readStencil(bitmap: Bitmap8) {
        fbuffer(bitmap.area * 1) { buffer ->
            gl.readPixels(
                0, 0, bitmap.width, bitmap.height,
                gl.STENCIL_INDEX, gl.UNSIGNED_BYTE, buffer
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
