package com.soywiz.korag.gl

import com.soywiz.kds.*
import com.soywiz.kgl.*
import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.annotation.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.vector.BitmapVector
import com.soywiz.korio.annotations.*
import com.soywiz.korio.lang.*
import com.soywiz.krypto.encoding.*
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

    val glSlVersion: Int? = null
    val gles: Boolean get() = gl.gles
    val linux: Boolean get() = gl.linux
    val android: Boolean get() = gl.android
    val webgl: Boolean get() = gl.webgl
    val webgl2: Boolean get() = gl.webgl2

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

    private var glProcessor: AGQueueProcessorOpenGL? = null

    override fun executeList(list: AGList) {
        if (glProcessor == null) glProcessor = AGQueueProcessorOpenGL(gl)
        glProcessor?.processBlockingAll(list)
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

        override fun toString(): String = "AGOpengl.GlTexture($tex)"
    }

    override fun readColorTexture(texture: Texture, width: Int, height: Int) {
        texture.bind()
        gl.copyTexImage2D(gl.TEXTURE_2D, 0, gl.RGBA, 0, 0, width, height, 0)
        texture.unbind()
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
