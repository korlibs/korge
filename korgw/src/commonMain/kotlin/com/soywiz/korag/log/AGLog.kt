package com.soywiz.korag.log

import com.soywiz.kds.linkedHashMapOf
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.Attribute
import com.soywiz.korag.shader.Shader
import com.soywiz.korag.shader.ShaderType
import com.soywiz.korag.shader.gl.GlslGenerator
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.annotations.KorInternal

/*
open class ComposedAG(val agBase: AG, val agExtra: AG) : AG(), AGFeatures by agBase {
    override val nativeComponent: Any get() = agBase

    override fun contextLost() {
        agBase.contextLost()
        agExtra.contextLost()
    }

    override val maxTextureSize: Size get() = agBase.maxTextureSize
    override val devicePixelRatio: Double get() = agBase.devicePixelRatio
    override val pixelsPerInch: Double get() = agBase.pixelsPerInch

    override fun beforeDoRender() {
        agBase.beforeDoRender()
        agExtra.beforeDoRender()
    }

    override fun offscreenRendering(callback: () -> Unit) {
        agBase.offscreenRendering {
            agExtra.offscreenRendering {
                callback()
            }
        }
    }

    override fun repaint() {
        agBase.repaint()
        agExtra.repaint()
    }

    override fun resized(x: Int, y: Int, width: Int, height: Int, fullWidth: Int, fullHeight: Int) {
        agBase.resized(x, y, width, height, fullWidth, fullHeight)
        agExtra.resized(x, y, width, height, fullWidth, fullHeight)
    }

    override fun dispose() {
        agBase.dispose()
        agExtra.dispose()
    }

    override val backWidth: Int get() = agBase.backWidth
    override val backHeight: Int get() = agBase.backHeight

    override fun createTexture(premultiplied: Boolean, targetKind: TextureTargetKind): Texture {
        return super.createTexture(premultiplied, targetKind)
    }

    inner class ComposedTexture : AG.Texture() {

    }

    override fun createTexture(targetKind: TextureTargetKind, init: Texture.(gl: KmlGl) -> Unit): Texture {
        return ComposedTexture()
    }

    override fun createBuffer(kind: BufferKind): Buffer {
        return super.createBuffer(kind)
    }

    override fun draw(batch: Batch) {
        agBase.draw(batch)
        agExtra.draw(batch)
    }

    override fun disposeTemporalPerFrameStuff() {
        super.disposeTemporalPerFrameStuff()
    }

    override fun createMainRenderBuffer(): BaseRenderBuffer {
        return super.createMainRenderBuffer()
    }

    override fun createRenderBuffer(): RenderBuffer {
        return super.createRenderBuffer()
    }

    override fun flipInternal() {
        agBase.flipInternal()
        agExtra.flipInternal()
    }

    override fun startFrame() {
        agBase.startFrame()
        agExtra.startFrame()
    }

    override fun clear(
        color: RGBA,
        depth: Float,
        stencil: Int,
        clearColor: Boolean,
        clearDepth: Boolean,
        clearStencil: Boolean
    ) {
        agBase.clear(color, depth, stencil, clearColor, clearDepth, clearStencil)
        agExtra.clear(color, depth, stencil, clearColor, clearDepth, clearStencil)
    }

    override fun readColor(bitmap: Bitmap32) {
        agBase.readColor(bitmap)
        agExtra.readColor(bitmap)
    }

    override fun readDepth(width: Int, height: Int, out: FloatArray) {
        agBase.readDepth(width, height, out)
        agExtra.readDepth(width, height, out)
    }

    override fun readColorTexture(texture: Texture, width: Int, height: Int) {
        agBase.readColorTexture(texture, width, height)
        agExtra.readColorTexture(texture, width, height)
    }
}
*/

open class AGPrint(
    width: Int = 640,
    height: Int = 480
) : AGBaseLog(width, height) {
    override fun log(str: String, kind: Kind) {
        println("PrintAG: $str")
    }
}

open class AGLog(
    width: Int = 640,
    height: Int = 480,
) : AGBaseLog(width, height) {
    val log = arrayListOf<String>()
    fun clearLog() = log.clear()
    fun getLogAsString(): String = log.joinToString("\n")

    var logFilter: (str: String, kind: Kind) -> Boolean = { str, kind -> true }

    override fun log(str: String, kind: Kind) {
        if (logFilter(str, kind)) this.log += str
    }
}

@OptIn(KorInternal::class)
open class AGBaseLog(
	width: Int = 640,
	height: Int = 480,
) : AGDummy(width, height) {
    enum class Kind { COMMAND, DRAW, DRAW_DETAILS, CLEAR, METRICS, FLIP, READ, REPAINT, DISPOSE, TEXTURE_UPLOAD, CLOSE, FRAME_BUFFER, BUFFER, TEXTURE, SHADER, OTHER, UNIFORM, UNIFORM_VALUES, SCISSORS, VIEWPORT, VERTEX, ENABLE_DISABLE, CONTEXT_LOST, FLUSH }

	open fun log(str: String, kind: Kind) {
	}

    override fun execute(command: AGCommand) {
        log("execute($command)", Kind.COMMAND)
    }

    override fun clear(
        frameBuffer: AGFrameBufferBase,
        frameBufferInfo: AGFrameBufferInfo,
		color: RGBA,
		depth: Float,
		stencil: Int,
		clearColor: Boolean,
		clearDepth: Boolean,
		clearStencil: Boolean,
        scissor: AGScissor,
	) {
        log("clear($frameBuffer, $frameBufferInfo, $color, $depth, $stencil, $clearColor, $clearDepth, $clearStencil)", Kind.CLEAR)
    }

	override var backWidth: Int = width; set(value) { field = value; log("backWidth = $value", Kind.METRICS) }
	override var backHeight: Int = height; set(value) { field = value; log("backHeight = $value", Kind.METRICS) }

	override fun dispose() = log("dispose()", Kind.DISPOSE)

	inner class LogFrameBuffer(val id: Int, isMain: Boolean) : AGFrameBuffer(this@AGBaseLog, isMain) {
        override fun setSize(x: Int, y: Int, width: Int, height: Int, fullWidth: Int, fullHeight: Int) {
            super.setSize(x, y, width, height, fullWidth, fullHeight)
            log("$this.setSize($width, $height)", Kind.FRAME_BUFFER)
        }
		override fun close() = log("$this.close()", Kind.FRAME_BUFFER)
		override fun toString(): String = "RenderBuffer[$id]"
        init {
            if (isMain) {
                setSize(0, 0, this@AGBaseLog.backWidth, this@AGBaseLog.backHeight)
            }
        }
	}

	private var textureId = 0
	private var bufferId = 0
	private var renderBufferId = 0

    data class VertexAttributeEx(val index: Int, val attribute: Attribute, val pos: Int, val data: AGVertexData) {
        val layout = data.layout
        val buffer = data.buffer
    }

    class ShaderInfo(val shader: Shader, val id: Int, val code: String) {
        var requested: Int = 0

        override fun toString(): String = if (requested <= 1) "#SHADER-$id: $code" else "#SHADER-$id (already shown)"
    }
    var shaderSourceId = 0
    val shaderSources = linkedHashMapOf<Shader, ShaderInfo>()
    fun getShaderSource(shader: Shader, type: ShaderType): ShaderInfo {
        return shaderSources.getOrPut(shader) {
            ShaderInfo(shader, ++shaderSourceId, GlslGenerator(type).generate(shader))
        }.also {
            it.requested++
        }
    }

    fun Any?.convertToStriangle(): Any? = when (this) {
        is IntArray -> this.toList()
        is FloatArray -> this.toList()
        else -> this
    }

	override fun createFrameBuffer(): AGFrameBuffer =
		LogFrameBuffer(renderBufferId++, isMain = false).apply { log("createRenderBuffer():$id", Kind.FRAME_BUFFER) }

    override fun createMainFrameBuffer(): AGFrameBuffer =
        LogFrameBuffer(renderBufferId++, isMain = true).apply { log("createMainRenderBuffer():$id", Kind.FRAME_BUFFER) }

    override fun flip() = log("flip()", Kind.FLIP)
	override fun readColor(bitmap: Bitmap32, x: Int, y: Int) = log("$this.readBitmap($bitmap, $x, $y)", Kind.READ)
	override fun readDepth(width: Int, height: Int, out: FloatArray) = log("$this.readDepth($width, $height, $out)", Kind.READ)
}
