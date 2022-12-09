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

open class PrintAG(
    width: Int = 640,
    height: Int = 480
) : LogBaseAG(width, height) {
    override fun log(str: String, kind: Kind) {
        println("PrintAG: $str")
    }
}

open class LogAG(
    width: Int = 640,
    height: Int = 480,
) : LogBaseAG(width, height) {
    val log = arrayListOf<String>()
    fun clearLog() = log.clear()
    fun getLogAsString(): String = log.joinToString("\n")

    var logFilter: (str: String, kind: Kind) -> Boolean = { str, kind -> true }

    override fun log(str: String, kind: Kind) {
        if (logFilter(str, kind)) this.log += str
    }
}

@OptIn(KorInternal::class)
open class LogBaseAG(
	width: Int = 640,
	height: Int = 480,
) : DummyAG(width, height) {
    enum class Kind { DRAW, DRAW_DETAILS, CLEAR, METRICS, FLIP, READ, REPAINT, DISPOSE, TEXTURE_UPLOAD, CLOSE, FRAME_BUFFER, BUFFER, TEXTURE, SHADER, OTHER, UNIFORM, UNIFORM_VALUES, SCISSORS, VIEWPORT, VERTEX, ENABLE_DISABLE, CONTEXT_LOST, FLUSH }

	open fun log(str: String, kind: Kind) {
	}

	override fun clear(
		color: RGBA,
		depth: Float,
		stencil: Int,
		clearColor: Boolean,
		clearDepth: Boolean,
		clearStencil: Boolean,
        scissor: AGScissor,
	) {
        log("clear($color, $depth, $stencil, $clearColor, $clearDepth, $clearStencil)", Kind.CLEAR)
    }

	override var backWidth: Int = width; set(value) { field = value; log("backWidth = $value", Kind.METRICS) }
	override var backHeight: Int = height; set(value) { field = value; log("backHeight = $value", Kind.METRICS) }

	override fun repaint() = log("repaint()", Kind.REPAINT)

	override fun dispose() = log("dispose()", Kind.DISPOSE)

	inner class LogTexture(val id: Int, premultiplied: Boolean) : AGTexture(this, premultiplied) {
		override fun uploadedSource() {
			log("$this.uploadedBitmap(${width}, ${height})", Kind.TEXTURE_UPLOAD)
		}

		override fun close() {
			super.close()
			log("$this.close()", Kind.CLOSE)
		}
		override fun toString(): String = "Texture[$id]"
	}

	inner class LogBuffer(val id: Int) : AGBuffer(this) {
		val logmem: com.soywiz.kmem.Buffer? get() = mem
		override fun afterSetMem() {
            super.afterSetMem()
            log("$this.afterSetMem(mem[${mem!!.size}])", LogBaseAG.Kind.BUFFER)
        }
		override fun toString(): String = "Buffer[$id]"
	}

	inner class LogFrameBuffer(val id: Int, isMain: Boolean) : AGFrameBuffer(this@LogBaseAG, isMain) {
        override fun setSize(x: Int, y: Int, width: Int, height: Int, fullWidth: Int, fullHeight: Int) {
            super.setSize(x, y, width, height, fullWidth, fullHeight)
            log("$this.setSize($width, $height)", Kind.FRAME_BUFFER)
        }
		override fun close() = log("$this.close()", Kind.FRAME_BUFFER)
		override fun toString(): String = "RenderBuffer[$id]"
        init {
            if (isMain) {
                setSize(0, 0, this@LogBaseAG.backWidth, this@LogBaseAG.backHeight)
            }
        }
	}

	private var textureId = 0
	private var bufferId = 0
	private var renderBufferId = 0

	override fun createTexture(premultiplied: Boolean, targetKind: AGTextureTargetKind): AGTexture =
		LogTexture(textureId++, premultiplied).apply { log("createTexture():$id", Kind.TEXTURE) }

	override fun createBuffer(): AGBuffer = LogBuffer(bufferId++).apply { log("createBuffer():$id", Kind.BUFFER) }

    data class VertexAttributeEx(val index: Int, val attribute: Attribute, val pos: Int, val data: AGVertexData) {
        val layout = data.layout
        val buffer = data.buffer as LogBuffer
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

    override fun disposeTemporalPerFrameStuff() = log("disposeTemporalPerFrameStuff()", Kind.DISPOSE)
	override fun createFrameBuffer(): AGFrameBuffer =
		LogFrameBuffer(renderBufferId++, isMain = false).apply { log("createRenderBuffer():$id", Kind.FRAME_BUFFER) }

    override fun createMainFrameBuffer(): AGFrameBuffer =
        LogFrameBuffer(renderBufferId++, isMain = true).apply { log("createMainRenderBuffer():$id", Kind.FRAME_BUFFER) }

    override fun flipInternal() = log("flipInternal()", Kind.FLIP)
	override fun readColor(bitmap: Bitmap32, x: Int, y: Int) = log("$this.readBitmap($bitmap, $x, $y)", Kind.READ)
	override fun readDepth(width: Int, height: Int, out: FloatArray) = log("$this.readDepth($width, $height, $out)", Kind.READ)
}
