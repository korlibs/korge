package korlibs.graphics.log

import korlibs.datastructure.linkedHashMapOf
import korlibs.memory.*
import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.graphics.shader.gl.GlslGenerator
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.io.annotations.KorInternal

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

open class AGPrint(width: Int = 640, height: Int = 480) : AGBaseLog(width, height) {
    override fun log(str: String, kind: Kind) {
        println("PrintAG: $str")
    }
}

open class AGLog(width: Int = 640, height: Int = 480) : AGBaseLog(width, height) {
    val log = arrayListOf<String>()
    fun clearLog() = log.clear()
    fun getLogAsString(): String = log.joinToString("\n")

    var logFilter: (str: String, kind: Kind) -> Boolean = { str, kind -> true }

    override fun log(str: String, kind: Kind) {
        if (logFilter(str, kind)) this.log += str
    }
}

@OptIn(KorInternal::class)
open class AGBaseLog(width: Int = 640, height: Int = 480) : AGDummy(width, height) {
    enum class Kind { COMMAND, DRAW, DRAW_DETAILS, CLEAR, METRICS, FLIP, READ, REPAINT, DISPOSE, TEXTURE_UPLOAD, CLOSE, FRAME_BUFFER, BUFFER, TEXTURE, SHADER, OTHER, UNIFORM, UNIFORM_VALUES, SCISSORS, VIEWPORT, VERTEX, ENABLE_DISABLE, CONTEXT_LOST, FLUSH }

	open fun log(str: String, kind: Kind) {
	}

    override fun execute(command: AGCommand){
        log("$command", Kind.COMMAND)
    }

    override fun dispose() = log("dispose()", Kind.DISPOSE)

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

    override fun finish() = log("finish()", Kind.FLIP)
}
