package com.soywiz.korag.gl

import com.soywiz.kds.Extra
import com.soywiz.kgl.KmlGl
import com.soywiz.kgl.KmlGlState
import com.soywiz.kgl.getIntegerv
import com.soywiz.korag.*
import com.soywiz.korio.annotations.KorIncomplete
import com.soywiz.korio.annotations.KorInternal
import com.soywiz.krypto.encoding.hex
import kotlin.native.concurrent.SharedImmutable

open class SimpleAGOpengl<TKmlGl : KmlGl>(override val gl: TKmlGl, override val nativeComponent: Any = Unit, checked: Boolean = false) : AGOpengl(checked)

@OptIn(KorIncomplete::class, KorInternal::class)
abstract class AGOpengl(checked: Boolean = false) : AG(checked) {
    class ShaderException(val str: String, val error: String, val errorInt: Int, val gl: KmlGl, val debugName: String?, val type: Int) :
        RuntimeException("Error Compiling Shader : $debugName type=$type : ${errorInt.hex} : '$error' : source='$str', gl.versionInt=${gl.versionInt}, gl.versionString='${gl.versionString}', gl=$gl")

    open var isGlAvailable = true

    @Deprecated("Do not use directly")
    abstract val gl: KmlGl

    override val parentFeatures: AGFeatures get() = gl
    protected val glGlobalState by lazy { GLGlobalState(gl, _globalState) }

    //val queue = Deque<(gl: GL) -> Unit>()

    open fun setSwapInterval(value: Int) {
        //gl.swapInterval = 0
    }

    override fun createMainRenderBuffer(): AGBaseFrameBufferImpl {
        var backBufferTextureBinding2d: Int = 0
        var backBufferRenderBufferBinding: Int = 0
        var backBufferFrameBufferBinding: Int = 0

        return object : AGBaseFrameBufferImpl(this) {
            override fun init() {
                commands {  }
                backBufferTextureBinding2d = gl.getIntegerv(KmlGl.TEXTURE_BINDING_2D)
                backBufferRenderBufferBinding = gl.getIntegerv(KmlGl.RENDERBUFFER_BINDING)
                backBufferFrameBufferBinding = gl.getIntegerv(KmlGl.FRAMEBUFFER_BINDING)
            }

            override fun set() {
                commands {  }
                setViewport(this)
                gl.bindTexture(KmlGl.TEXTURE_2D, backBufferTextureBinding2d)
                gl.bindRenderbuffer(KmlGl.RENDERBUFFER, backBufferRenderBufferBinding)
                gl.bindFramebuffer(KmlGl.FRAMEBUFFER, backBufferFrameBufferBinding)
            }

            override fun unset() {
                commands {  }
                backBufferTextureBinding2d = gl.getIntegerv(KmlGl.TEXTURE_BINDING_2D)
                backBufferRenderBufferBinding = gl.getIntegerv(KmlGl.RENDERBUFFER_BINDING)
                backBufferFrameBufferBinding = gl.getIntegerv(KmlGl.FRAMEBUFFER_BINDING)
            }
        }
    }

    fun createGlState() = KmlGlState(gl)

    private var _glProcessor: AGQueueProcessorOpenGL? = null
    private val glProcessor: AGQueueProcessorOpenGL get() {
        if (_glProcessor == null) _glProcessor = AGQueueProcessorOpenGL(gl, glGlobalState)
        return _glProcessor!!
    }

    override fun executeList(list: AGList) {
        list.listFlush()
        glProcessor?.processBlockingAll(list)
    }

    override fun readColorTexture(texture: AGTexture, x: Int, y: Int, width: Int, height: Int) {
        //gl.flush()
        //gl.finish()
        commands { it.bindTexture(texture, AGTextureTargetKind.TEXTURE_2D) }
        gl.copyTexImage2D(gl.TEXTURE_2D, 0, gl.RGBA, x, y, width, height, 0)
        commands { it.bindTexture(null, AGTextureTargetKind.TEXTURE_2D) }
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
