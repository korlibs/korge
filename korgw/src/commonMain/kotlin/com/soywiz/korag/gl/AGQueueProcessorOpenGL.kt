package com.soywiz.korag.gl

import com.soywiz.kds.fastCastTo
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kgl.KmlGl
import com.soywiz.kgl.deleteBuffer
import com.soywiz.kgl.deleteFramebuffer
import com.soywiz.kgl.deleteRenderbuffer
import com.soywiz.kgl.deleteTexture
import com.soywiz.kgl.genBuffer
import com.soywiz.kgl.genFramebuffer
import com.soywiz.kgl.genRenderbuffer
import com.soywiz.kgl.genTexture
import com.soywiz.kmem.*
import com.soywiz.kmem.arraycopy
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.GlslConfig
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.Bitmap8
import com.soywiz.korim.bitmap.FloatBitmap32
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korio.annotations.KorIncomplete
import com.soywiz.korio.annotations.KorInternal
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.lang.*

@OptIn(KorIncomplete::class, KorInternal::class)
class AGQueueProcessorOpenGL(
    private val gl: KmlGl,
    val globalState: AGGlobalState,
    val stats: AGStats,
) : AGQueueProcessor {
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

    val contextVersion: Int get() = globalState.contextVersion

    override fun listStart() {
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

    override fun contextLost() {
        globalState.contextVersion++
        gl.handleContextLost()
        gl.graphicExtensions // Ensure extensions are available outside the GL thread
    }

    //var doPrintTimer = Stopwatch().also { it.start() }
    //var doPrint = false
    override fun flush() {
        gl.flush()
    }

    var frameVersion = 0

    private val enableCache = Array<Boolean?>(16) { null }
    private var scissorCache: AGScissor = AGScissor.NIL
    private var viewportCache: AGScissor = AGScissor.NIL
    private var cacheTextureUnit = Array<AGTextureUnit?>(16) { null }

    override fun finish() {
        gl.flush()
        frameVersion++
        enableCache.fill(null)
        cacheTextureUnit.fill(null)
        scissorCache = AGScissor.NIL
        viewportCache = AGScissor.NIL

        //gl.finish()

       //doPrint = if (doPrintTimer.elapsed >= 1.seconds) {
       //    println("---------------------------------")
       //    doPrintTimer.restart()
       //    true
       //} else {
       //    false
       //}

    }

    override fun enableDisable(kind: AGEnable, enable: Boolean) {
        if (enableCache[kind.ordinal] == enable) return
        enableCache[kind.ordinal] = enable
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

    internal class ProgramInfo(val id: Int) {
        internal var glProgramInfo: GLProgramInfo? = null
    }

    private val programs = FastResources { ProgramInfo(it) }
    private var currentProgram: GLProgramInfo? = null

    override fun programCreate(programId: Int, program: Program, config: ProgramConfig?) {
        programs.getOrCreate(programId).glProgramInfo = GLShaderCompiler.programCreate(
            gl,
            this.config.copy(programConfig = config ?: this.config.programConfig),
            program, debugName = program.name
        )
    }

    override fun programDelete(programId: Int) {
        val program = programs.tryGetAndDelete(programId) ?: return
        program.glProgramInfo?.delete(gl)
        if (currentProgram === program.glProgramInfo) {
            currentProgram = null
        }
        program.glProgramInfo = null
    }

    override fun programUse(programId: Int) {
        programUseExt(programs[programId]?.glProgramInfo)
    }

    private fun programUseExt(program: GLProgramInfo?) {
        program?.use(gl)
        currentProgram = program
    }

    // BUFFERS
    class BufferInfo(val id: Int) {
        var glId = 0
        var cachedVersion = -1
    }

    private val buffers = FastResources { BufferInfo(it) }

    override fun bufferCreate(id: Int) {
        buffers.getOrCreate(id)
    }

    override fun bufferDelete(id: Int) {
        buffers.tryGetAndDelete(id)?.let { gl.deleteBuffer(it.glId); it.glId = 0 }
    }

    private fun bindBuffer(buffer: AGBuffer, target: AGBufferKind) {
        val bufferInfo = buffers[buffer.agId] ?: return
        if (bufferInfo.cachedVersion != globalState.contextVersion) {
            bufferInfo.cachedVersion = globalState.contextVersion
            buffer.dirty = true
            bufferInfo.glId = 0
        }
        if (bufferInfo.glId <= 0) {
            bufferInfo.glId = gl.genBuffer()
        }

        gl.bindBuffer(target.toGl(), bufferInfo.glId)

        if (buffer.dirty) {
            buffer.dirty = false
            val mem = buffer.mem
            if (mem != null) {
                gl.bufferData(target.toGl(), buffer.memLength, mem, KmlGl.STATIC_DRAW)
                stats.updatedBufferCount++
            }
        }
    }

    ///////////////////////////////////////
    // DRAW
    ///////////////////////////////////////
    override fun draw(
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
    override fun uniformsSet(layout: UniformLayout, data: Buffer) {
        layout.uniforms.fastForEach {
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

    override fun stencilFunction(compareMode: AGCompareMode, referenceValue: Int, readMask: Int) {
        gl.stencilFunc(compareMode.toGl(), referenceValue, readMask)
    }

    // @TODO: Separate
    override fun stencilOperation(
        actionOnDepthFail: AGStencilOp,
        actionOnDepthPassStencilFail: AGStencilOp,
        actionOnBothPass: AGStencilOp
    ) {
        gl.stencilOp(actionOnDepthFail.toGl(), actionOnDepthPassStencilFail.toGl(), actionOnBothPass.toGl())
    }

    // @TODO: Separate
    override fun stencilMask(writeMask: Int) {
        gl.stencilMask(writeMask)
    }

    override fun scissor(x: Int, y: Int, width: Int, height: Int) {
        val value = AGScissor(x, y, width, height)
        if (scissorCache != value) {
            scissorCache = value
            gl.scissor(x, y, width, height)
        }
        //println("SCISSOR: $x, $y, $width, $height")
    }

    override fun viewport(x: Int, y: Int, width: Int, height: Int) {
        val value = AGScissor(x, y, width, height)
        if (viewportCache != value) {
            viewportCache = value
            gl.viewport(x, y, width, height)
        }
        //println("VIEWPORT: $x, $y, $width, $height")
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

    val vaos = arrayListOf<AGVertexArrayObject?>()

    //val vaos = IntMap<AG.VertexArrayObject?>()
    var lastUsedVao: AGVertexArrayObject? = null

    private fun ensureVaoIndex(index: Int): Int {
        while (vaos.size <= index) vaos.add(null)
        return index
    }

    override fun vaoCreate(id: Int) {
    }

    override fun vaoDelete(id: Int) {
        if (id < vaos.size) vaos[id] = null
    }

    override fun vaoSet(id: Int, vao: AGVertexArrayObject) {
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
                        if (att.fixedLocation == null) {
                            //error("WOOPS!")
                        }
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
                    if (att.fixedLocation == null) {
                        //error("WOOPS!")
                    }
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

    val ubos = arrayListOf<AGUniformValues?>()

    private fun ensureUboIndex(index: Int): Int {
        while (ubos.size <= index) ubos.add(null)
        return index
    }


    override fun uboCreate(id: Int) {
        //println("uboCreate: id=$id")
    }

    override fun uboDelete(id: Int) {
        //println("uboDelete: id=$id")
        if (id < ubos.size) ubos[id] = null
    }

    override fun uboSet(id: Int, ubo: AGUniformValues) {
        ubos[ensureUboIndex(id)] = ubo
    }

    //val tempData = Buffer.allocDirect(4 * 1024)

    override fun uboUse(id: Int) {
        val uniforms = ubos[id] ?: return
        val glProgram = currentProgram ?: return

        //if (doPrint) println("-----------")

        //for ((uniform, value) in uniforms) {

        uniforms.fastForEach { value ->
            val uniform = value.uniform
            val uniformName = uniform.name
            val uniformType = uniform.type
            val location = glProgram.getUniformLocation(gl, uniformName)
            val declArrayCount = uniform.arrayCount
            var textureUnit: Int = -1

            when (uniformType) {
                VarType.Sampler2D, VarType.SamplerCube -> {
                    val unit = value.nativeValue?.fastCastTo<AGTextureUnit>() ?: AGTextureUnit(0, null)
                    //println("unit=${unit.texture}")
                    textureUnit = glProgram.getTextureUnit(uniform, unit)

                    if (cacheTextureUnit[textureUnit] != unit) {
                        cacheTextureUnit[textureUnit] = unit.clone()
                        gl.activeTexture(KmlGl.TEXTURE0 + textureUnit)

                        val tex = unit.texture
                        if (tex != null) {
                            // @TODO: This might be enqueuing commands, we shouldn't do that here.
                            textureBindEnsuring(tex)
                            textureSetWrap(tex)
                            textureSetFilter(tex, unit.linear, unit.trilinear ?: unit.linear)
                        } else {
                            gl.bindTexture(KmlGl.TEXTURE_2D, 0)
                        }
                    }
                }
                else -> Unit
            }

            val oldValue = glProgram.cache[uniform]
            if (value == oldValue) {
                return@fastForEach
            }
            glProgram.cache[uniform] = value

            //println("uniform: $uniform, arrayCount=${uniform.arrayCount}, stride=${uniform.elementCount}, value=$value old=$oldValue")

            // Store into a direct buffer
            //arraycopy(value.data, 0, tempData, 0, value.data.size)
            val data = value.data

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


    fun textureSetFilter(tex: AGTexture, linear: Boolean, trilinear: Boolean = linear) {
        val minFilter = when {
            tex.mipmaps -> when {
                linear -> if (trilinear) KmlGl.LINEAR_MIPMAP_LINEAR else KmlGl.LINEAR_MIPMAP_NEAREST
                else -> if (trilinear) KmlGl.NEAREST_MIPMAP_LINEAR else KmlGl.NEAREST_MIPMAP_NEAREST
            }
            else -> if (linear) KmlGl.LINEAR else KmlGl.NEAREST
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

    override fun readPixels(x: Int, y: Int, width: Int, height: Int, data: Any, kind: AGReadKind) {
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

    override fun readPixelsToTexture(textureId: Int, x: Int, y: Int, width: Int, height: Int, kind: AGReadKind) {
        //println("BEFORE:" + gl.getError())
        //textureBindEnsuring(tex)
        textureBind(textureId, AGTextureTargetKind.TEXTURE_2D, -1)
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

    internal val textures = FastResources { TextureInfo(it) }

    override fun textureCreate(textureId: Int) {
        val tex = textures.getOrCreate(textureId)
        tex.glId = gl.genTexture()
        //println("gl.textureCreate[$currentThreadId]: textureId=$textureId, tex=${tex.id}, glId=${tex.glId}")
    }

    override fun textureDelete(textureId: Int) {
        val tex = textures.tryGetAndDelete(textureId) ?: return
        //println("gl.textureDelete[$currentThreadId]: textureId=$textureId, tex=${tex.id}, glId=${tex.glId}")
        if (tex.glId <= 0) return
        gl.deleteTexture(tex.glId)
        tex.glId = 0
    }

    override fun textureBind(textureId: Int, target: AGTextureTargetKind, implForcedTexId: Int) {
        val glId = implForcedTexId.takeIf { it >= 0 } ?: textures[textureId]?.glId ?: 0
        //if (glId == -1) println("glId=$glId")
        //println("textureBind: $glId, textureId=$textureId, target=$target, implForcedTexId=$implForcedTexId")
        gl.bindTexture(target.toGl(), glId)
    }

    override fun textureBindEnsuring(tex: AGTexture?) {
        if (tex == null) return gl.bindTexture(KmlGl.TEXTURE_2D, 0)

        // Context lost
        if (tex.cachedVersion != contextVersion) {
            println("Texture context lost, recreating: texId=${tex.texId}, source=${tex.source}")
            tex.cachedVersion = contextVersion
            tex.invalidate()
            textureCreate(tex.texId)
        }

        textureBind(tex.texId, tex.implForcedTexTarget, tex.implForcedTexId)
        if (tex.forcedTexId != null) return

        if (tex.isFbo) return
        val source = tex.source
        if (tex.uploaded) return

        if (!tex.generating) {
            tex.generating = true
            when (source) {
                is AGSyncBitmapSourceList -> {
                    tex.tempBitmaps = source.gen()
                    tex.generated = true
                }
                is AGSyncBitmapSource -> {
                    tex.tempBitmaps = listOf(source.gen())
                    tex.generated = true
                }
                is AGAsyncBitmapSource -> {
                    launchImmediately(source.coroutineContext) {
                        tex.tempBitmaps = listOf(source.gen())
                        tex.generated = true
                    }
                }
            }
        }

        if (tex.generated) {
            tex.uploaded = true
            tex.generating = false
            tex.generated = false
            try {
                val bmps: List<Bitmap?>? = tex.tempBitmaps
                val requestMipmaps: Boolean = tex.requestMipmaps
                tex.mipmaps = tex.doMipmaps(source, requestMipmaps)
                if (bmps != null) {
                    for ((index, rbmp) in bmps.withIndex()) {
                        _textureUpdate(tex.texId, tex.implForcedTexTarget, index, rbmp, tex.source, tex.mipmaps, tex.premultiplied)
                    }
                } else {
                    _textureUpdate(tex.texId, tex.implForcedTexTarget, 0, null, tex.source, tex.mipmaps, tex.premultiplied)
                }
            } finally {
                tex.tempBitmaps = null
                tex.ready = true
            }
        }
        return
    }

    override fun textureSetFromFrameBuffer(textureId: Int, x: Int, y: Int, width: Int, height: Int) {
        val glId = textures[textureId]?.glId ?: return
        gl.bindTexture(gl.TEXTURE_2D, glId)
        gl.copyTexImage2D(gl.TEXTURE_2D, 0, gl.RGBA, x, y, width, height, 0)
        gl.bindTexture(gl.TEXTURE_2D, 0)
    }

    override fun textureUpdate(textureId: Int, target: AGTextureTargetKind, index: Int, bmp: Bitmap?, source: AGBitmapSourceBase, doMipmaps: Boolean, premultiplied: Boolean) {
        //textureBind(textureId, target, -1)
        _textureUpdate(textureId, target, index, bmp, source, doMipmaps, premultiplied)
    }

    fun _textureUpdate(textureId: Int, target: AGTextureTargetKind, index: Int, bmp: Bitmap?, source: AGBitmapSourceBase, doMipmaps: Boolean, premultiplied: Boolean) {
        val bytesPerPixel = if (source.rgba) 4 else 1

        val isFloat = bmp is FloatBitmap32

        val type = when {
            source.rgba -> KmlGl.RGBA //if (source is NativeImage) KmlGl.BGRA else KmlGl.RGBA
            else -> KmlGl.LUMINANCE
        }

        val texTarget = when (target) {
            AGTextureTargetKind.TEXTURE_CUBE_MAP -> KmlGl.TEXTURE_CUBE_MAP_POSITIVE_X + index
            else -> target.toGl()
        }

        //val tex = textures.getOrNull(textureId)
        //println("_textureUpdate: texId=$textureId, id=${tex?.id}, glId=${tex?.glId}, target=$target, source=${source.width}x${source.height}")

        if (bmp == null) {
            gl.texImage2D(target.toGl(), 0, type, source.width, source.height, 0, type, KmlGl.UNSIGNED_BYTE, null)
        } else {

            if (bmp is NativeImage) {
                if (bmp.area != 0) {
                    prepareTexImage2D()
                    gl.texImage2D(texTarget, 0, type, type, KmlGl.UNSIGNED_BYTE, bmp)
                }
            } else {
                val buffer = createBufferForBitmap(bmp, premultiplied)
                if (buffer != null && source.width != 0 && source.height != 0 && buffer.size != 0) {
                    prepareTexImage2D()
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
                    //println("actualSyncUpload: webgl=$webgl, internalFormat=${internalFormat.hex}, format=${format.hex}, textype=${texType.hex}")
                    gl.texImage2D(texTarget, 0, internalFormat, source.width, source.height, 0, format, texType, buffer)
                }
            }
            //println(buffer)
        }

        if (doMipmaps) {
            gl.generateMipmap(texTarget)
        }
    }

    private fun createBufferForBitmap(bmp: Bitmap?, premultiplied: Boolean): Buffer? = when (bmp) {
        null -> null
        is NativeImage -> unsupported("Should not call createBufferForBitmap with a NativeImage")
        is Bitmap8 -> Buffer(bmp.area).also { mem -> arraycopy(bmp.data, 0, mem.i8, 0, bmp.area) }
        is FloatBitmap32 -> Buffer(bmp.area * 4 * 4).also { mem -> arraycopy(bmp.data, 0, mem.f32, 0, bmp.area * 4) }
        else -> Buffer(bmp.area * 4).also { mem ->
            val abmp: Bitmap32 = if (premultiplied) bmp.toBMP32IfRequired().premultipliedIfRequired() else bmp.toBMP32IfRequired().depremultipliedIfRequired()
            arraycopy(abmp.ints, 0, mem.i32, 0, abmp.area)
        }
    }

    private fun prepareTexImage2D() {
        if (gl.linux) {
            //println("prepareTexImage2D")
            //gl.pixelStorei(GL_UNPACK_LSB_FIRST, KmlGl.TRUE)
            gl.pixelStorei(KmlGl.UNPACK_LSB_FIRST, KmlGl.GFALSE)
            gl.pixelStorei(KmlGl.UNPACK_SWAP_BYTES, KmlGl.GTRUE)
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
    val frameBuffers = FastResources { FrameBufferInfo() }

    override fun frameBufferCreate(id: Int) {
        frameBuffers.getOrCreate(id)
    }

    override fun frameBufferDelete(id: Int) {
        val fb = frameBuffers.getOrNull(id)
        if (fb != null) {
            //gl.deleteTexture(fb.texColor).also { fb.texColor = -1 } // Not handled by us
            gl.deleteRenderbuffer(fb.renderbuffer).also { fb.renderbuffer = -1 }
            gl.deleteFramebuffer(fb.framebuffer).also { fb.framebuffer = -1 }
        }
        frameBuffers.delete(id)
    }

    override fun frameBufferSet(id: Int, textureId: Int, width: Int, height: Int, hasStencil: Boolean, hasDepth: Boolean) {
        // Ensure everything has been executed already. @TODO: We should remove this since this is a bottleneck
        val fb = frameBuffers.getOrCreate(id)
        val glTexId = textures.getOrNull(textureId)?.glId ?: 0

        val nsamples = 1
        val dirty = fb.width != width
            || fb.height != height
            || fb.hasDepth != hasDepth
            || fb.hasStencil != hasStencil
            || fb.nsamples != nsamples
            || fb.cachedVersion != contextVersion
            || fb.texColor != glTexId

        //val width = this.width.nextPowerOfTwo
        //val height = this.height.nextPowerOfTwo
        if (dirty) {
            fb.width = width
            fb.height = height
            fb.hasDepth = hasDepth
            fb.hasStencil = hasStencil
            fb.nsamples = nsamples
            fb.texColor = glTexId

            //dirty = false
            //setSwapInterval(0)

            if (fb.cachedVersion != contextVersion) {
                fb.cachedVersion = contextVersion
                fb.renderbuffer = gl.genRenderbuffer()
                fb.framebuffer = gl.genFramebuffer()
            }

            //val doMsaa = nsamples != 1
            val doMsaa = false
            val texTarget = when {
                doMsaa -> KmlGl.TEXTURE_2D_MULTISAMPLE
                else -> KmlGl.TEXTURE_2D
            }

            gl.bindTexture(KmlGl.TEXTURE_2D, fb.texColor)
            gl.texParameteri(texTarget, KmlGl.TEXTURE_MAG_FILTER, KmlGl.LINEAR)
            gl.texParameteri(texTarget, KmlGl.TEXTURE_MIN_FILTER, KmlGl.LINEAR)
            if (doMsaa) {
                gl.texImage2DMultisample(texTarget, fb.nsamples, KmlGl.RGBA, fb.width, fb.height, false)
            } else {
                gl.texImage2D(texTarget, 0, KmlGl.RGBA, fb.width, fb.height, 0, KmlGl.RGBA, KmlGl.UNSIGNED_BYTE, null)
            }
            gl.bindTexture(texTarget, 0)
            gl.bindRenderbuffer(KmlGl.RENDERBUFFER, fb.renderbuffer)
            val internalFormat = when {
                fb.hasStencilAndDepth -> KmlGl.DEPTH_STENCIL
                fb.hasStencil -> KmlGl.STENCIL_INDEX8 // On android this is buggy somehow?
                fb.hasDepth -> KmlGl.DEPTH_COMPONENT
                else -> 0
            }
            if (internalFormat != 0) {
                if (doMsaa) {
                    gl.renderbufferStorageMultisample(KmlGl.RENDERBUFFER, fb.nsamples, internalFormat, fb.width, fb.height)
                    //gl.renderbufferStorage(KmlGl.RENDERBUFFER, internalFormat, width, height)
                } else {
                    gl.renderbufferStorage(KmlGl.RENDERBUFFER, internalFormat, fb.width, fb.height)
                }
            }
            gl.bindRenderbuffer(KmlGl.RENDERBUFFER, 0)
            //gl.renderbufferStorageMultisample()
        }
    }

    override fun frameBufferUse(id: Int) {
        val fb = frameBuffers.getOrCreate(id)
        gl.bindFramebuffer(KmlGl.FRAMEBUFFER, fb.framebuffer)
        gl.framebufferTexture2D(KmlGl.FRAMEBUFFER, KmlGl.COLOR_ATTACHMENT0, KmlGl.TEXTURE_2D, fb.texColor, 0)
        val internalFormat = when {
            fb.hasStencilAndDepth -> KmlGl.DEPTH_STENCIL_ATTACHMENT
            fb.hasStencil -> KmlGl.STENCIL_ATTACHMENT
            fb.hasDepth -> KmlGl.DEPTH_ATTACHMENT
            else -> 0
        }
        if (internalFormat != 0) {
            gl.framebufferRenderbuffer(KmlGl.FRAMEBUFFER, internalFormat, KmlGl.RENDERBUFFER, fb.renderbuffer)
        } else {
            gl.framebufferRenderbuffer(KmlGl.FRAMEBUFFER, KmlGl.STENCIL_ATTACHMENT, KmlGl.RENDERBUFFER, 0)
            gl.framebufferRenderbuffer(KmlGl.DEPTH_ATTACHMENT, KmlGl.STENCIL_ATTACHMENT, KmlGl.RENDERBUFFER, 0)
        }
        //val status = gl.checkFramebufferStatus(KmlGl.FRAMEBUFFER)
        //if (status != KmlGl.FRAMEBUFFER_COMPLETE) { gl.bindFramebuffer(KmlGl.FRAMEBUFFER, 0); error("Error getting framebuffer") }
    }
}
