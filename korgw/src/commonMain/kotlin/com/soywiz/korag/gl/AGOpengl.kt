package com.soywiz.korag.gl

import com.soywiz.kds.*
import com.soywiz.kgl.*
import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korio.annotations.*
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
        commandsNoWait { it.viewport(buffer.x, buffer.y, buffer.width, buffer.height) }
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
        override val id = lastRenderContextId++

        var frameBufferId: Int = -1

        // http://wangchuan.github.io/coding/2016/05/26/multisampling-fbo.html
        override fun set() {
            setViewport(this)

            commandsNoWait { list ->
                if (dirty) {
                    if (frameBufferId < 0) {
                        frameBufferId = list.frameBufferCreate()
                    }
                    list.frameBufferSet(frameBufferId, tex.texId, width, height, hasStencil, hasDepth)
                }
                list.frameBufferUse(frameBufferId)
            }
        }

        override fun close() {
            super.close()
            commandsNoWait { list ->
                if (frameBufferId >= 0) {
                    list.frameBufferDelete(frameBufferId)
                    frameBufferId = -1
                }
            }
        }

        override fun toString(): String = "GlRenderBuffer[$id]($width, $height)"
    }

    override fun createRenderBuffer(): RenderBuffer = GlRenderBuffer()

    private var _glProcessor: AGQueueProcessorOpenGL? = null
    private val glProcessor: AGQueueProcessorOpenGL get() {
        if (_glProcessor == null) _glProcessor = AGQueueProcessorOpenGL(gl, _globalState)
        return _glProcessor!!
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
