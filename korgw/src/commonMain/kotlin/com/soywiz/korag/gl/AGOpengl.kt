package com.soywiz.korag.gl

import com.soywiz.kds.*
import com.soywiz.kgl.*
import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.annotations.*
import com.soywiz.korio.lang.*
import com.soywiz.krypto.encoding.*
import kotlin.native.concurrent.*

open class SimpleAGOpengl<TKmlGl : KmlGl>(override val gl: TKmlGl, override val nativeComponent: Any = Unit) : AGOpengl()

@OptIn(KorIncomplete::class, KorInternal::class)
abstract class AGOpengl : AG() {
    class ShaderException(val str: String, val error: String, val errorInt: Int, val gl: KmlGl) :
        RuntimeException("Error Compiling Shader : ${errorInt.hex} : '$error' : source='$str', gl.versionInt=${gl.versionInt}, gl.versionString='${gl.versionString}', gl=$gl")

    open var isGlAvailable = true
    abstract val gl: KmlGl

    override val parentFeatures: AGFeatures get() = gl

    override fun contextLost() {
        Console.info("AG.contextLost()", this, gl, gl.root)
        contextVersion++
    }

    //val queue = Deque<(gl: GL) -> Unit>()

    open fun setSwapInterval(value: Int) {
        //gl.swapInterval = 0
    }

    private fun setViewport(buffer: BaseRenderBuffer) {
        commandsSync {  }
        gl.viewport(buffer.x, buffer.y, buffer.width, buffer.height)
        //println("setViewport: ${buffer.x}, ${buffer.y}, ${buffer.width}, ${buffer.height}")
    }

    override fun createMainRenderBuffer(): BaseRenderBufferImpl {
        var backBufferTextureBinding2d: Int = 0
        var backBufferRenderBufferBinding: Int = 0
        var backBufferFrameBufferBinding: Int = 0

        return object : BaseRenderBufferImpl() {
            override fun init() {
                commandsSync {  }
                backBufferTextureBinding2d = gl.getIntegerv(KmlGl.TEXTURE_BINDING_2D)
                backBufferRenderBufferBinding = gl.getIntegerv(KmlGl.RENDERBUFFER_BINDING)
                backBufferFrameBufferBinding = gl.getIntegerv(KmlGl.FRAMEBUFFER_BINDING)
            }

            override fun set() {
                commandsSync {  }
                setViewport(this)
                gl.bindTexture(KmlGl.TEXTURE_2D, backBufferTextureBinding2d)
                gl.bindRenderbuffer(KmlGl.RENDERBUFFER, backBufferRenderBufferBinding)
                gl.bindFramebuffer(KmlGl.FRAMEBUFFER, backBufferFrameBufferBinding)
            }

            override fun unset() {
                commandsSync {  }
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

        val ftex get() = tex

        var depth = 0
        var framebuffer = 0

        // http://wangchuan.github.io/coding/2016/05/26/multisampling-fbo.html
        override fun set() {
            // Ensure everything has been executed already. @TODO: We should remove this since this is a bottleneck
            commandsSync { }

            setViewport(this)

            val hasStencilAndDepth: Boolean = when {
                gl.android -> hasStencil || hasDepth // stencil8 causes strange bug artifacts in Android (at least in one of my devices)
                else -> hasStencil && hasDepth
            }

            //val width = this.width.nextPowerOfTwo
            //val height = this.height.nextPowerOfTwo
            if (dirty) {
                dirty = false
                setSwapInterval(0)

                if (cachedVersion != contextVersion) {
                    cachedVersion = contextVersion
                    depth = gl.genRenderbuffer()
                    framebuffer = gl.genFramebuffer()
                }

                //val doMsaa = nsamples != 1
                val doMsaa = false
                val texTarget = when {
                    doMsaa -> KmlGl.TEXTURE_2D_MULTISAMPLE
                    else -> KmlGl.TEXTURE_2D
                }

                commandsSync {
                    ftex.bind()
                }
                gl.texParameteri(texTarget, KmlGl.TEXTURE_MAG_FILTER, KmlGl.LINEAR)
                gl.texParameteri(texTarget, KmlGl.TEXTURE_MIN_FILTER, KmlGl.LINEAR)
                if (doMsaa) {
                    gl.texImage2DMultisample(texTarget, nsamples, gl.RGBA, width, height, false)
                } else {
                    gl.texImage2D(texTarget, 0, KmlGl.RGBA, width, height, 0, KmlGl.RGBA, KmlGl.UNSIGNED_BYTE, null)
                }
                gl.bindTexture(texTarget, 0)
                gl.bindRenderbuffer(KmlGl.RENDERBUFFER, depth)
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

            gl.bindFramebuffer(KmlGl.FRAMEBUFFER, framebuffer)
            gl.framebufferTexture2D(KmlGl.FRAMEBUFFER, KmlGl.COLOR_ATTACHMENT0, KmlGl.TEXTURE_2D, glProcessorSync.textures[ftex.tex]!!.glId, 0)
            val internalFormat = when {
                hasStencilAndDepth -> KmlGl.DEPTH_STENCIL_ATTACHMENT
                hasStencil -> KmlGl.STENCIL_ATTACHMENT
                hasDepth -> KmlGl.DEPTH_ATTACHMENT
                else -> 0
            }
            if (internalFormat != 0) {
                gl.framebufferRenderbuffer(KmlGl.FRAMEBUFFER, internalFormat, KmlGl.RENDERBUFFER, depth)
            } else {
                gl.framebufferRenderbuffer(KmlGl.FRAMEBUFFER, KmlGl.STENCIL_ATTACHMENT, KmlGl.RENDERBUFFER, 0)
                gl.framebufferRenderbuffer(KmlGl.DEPTH_ATTACHMENT, KmlGl.STENCIL_ATTACHMENT, KmlGl.RENDERBUFFER, 0)
            }
            //val status = gl.checkFramebufferStatus(KmlGl.FRAMEBUFFER);
            //if (status != KmlGl.FRAMEBUFFER_COMPLETE) error("Error getting framebuffer")
            //gl.bindFramebuffer(KmlGl.FRAMEBUFFER, 0)
        }

        override fun close() {
            super.close()
            commandsSync {  }
            gl.deleteFramebuffer(framebuffer)
            gl.deleteRenderbuffer(depth)
            framebuffer = 0
            depth = 0
        }

        override fun toString(): String = "GlRenderBuffer[$id]($width, $height)"
    }

    override fun createRenderBuffer(): RenderBuffer = GlRenderBuffer()

    private var _glProcessor: AGQueueProcessorOpenGL? = null
    private val glProcessor: AGQueueProcessorOpenGL get() {
        if (_glProcessor == null) _glProcessor = AGQueueProcessorOpenGL(gl, _globalState)
        return _glProcessor!!
    }

    val glProcessorSync: AGQueueProcessorOpenGL get() {
        commandsSync {  }
        return glProcessor
    }

    override fun executeList(list: AGList) {
        glProcessor?.processBlockingAll(list)
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
