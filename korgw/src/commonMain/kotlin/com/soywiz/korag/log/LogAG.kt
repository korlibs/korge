package com.soywiz.korag.log

import com.soywiz.kds.*
import com.soywiz.kgl.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*

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

    override fun createTexture(premultiplied: Boolean): Texture {
        return super.createTexture(premultiplied)
    }

    inner class ComposedTexture : AG.Texture() {

    }

    override fun createTexture(targetKind: TextureTargetKind, init: Texture.(gl: KmlGl) -> Unit): Texture {
        return ComposedTexture()
    }

    override fun createBuffer(kind: Buffer.Kind): Buffer {
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
    fun getLogAsString(): String = log.joinToString("\n")
    override fun log(str: String, kind: Kind) {
        this.log += str
    }
}

open class LogBaseAG(
	width: Int = 640,
	height: Int = 480,
) : DummyAG(width, height) {
    enum class Kind { DRAW, CLEAR, METRICS, FLIP, READ, REPAINT, DISPOSE, TEXTURE_UPLOAD, CLOSE, RENDER_BUFFER, BUFFER, TEXTURE, SHADER }

	open fun log(str: String, kind: Kind) {
	}

	override fun clear(
		color: RGBA,
		depth: Float,
		stencil: Int,
		clearColor: Boolean,
		clearDepth: Boolean,
		clearStencil: Boolean
	) = log("clear($color, $depth, $stencil, $clearColor, $clearDepth, $clearStencil)", Kind.CLEAR)

	override var backWidth: Int = width; set(value) { field = value; log("backWidth = $value", Kind.METRICS) }
	override var backHeight: Int = height; set(value) { field = value; log("backHeight = $value", Kind.METRICS) }

	override fun repaint() = log("repaint()", Kind.REPAINT)

	override fun dispose() = log("dispose()", Kind.DISPOSE)

	inner class LogTexture(val id: Int, override val premultiplied: Boolean) : Texture() {
		override fun uploadedSource() {
			log("$this.uploadedBitmap($source, ${source.width}, ${source.height})", Kind.TEXTURE_UPLOAD)
		}

		override fun close() {
			super.close()
			log("$this.close()", Kind.CLOSE)
		}
		override fun toString(): String = "Texture[$id]"
	}

	inner class LogBuffer(val id: Int, kind: Kind) : Buffer(kind) {
		val logmem: FBuffer? get() = mem
		val logmemOffset get() = memOffset
		val logmemLength get() = memLength
		override fun afterSetMem() = log("$this.afterSetMem(mem[${mem!!.size}])", LogBaseAG.Kind.BUFFER)
		override fun close() = log("$this.close()", LogBaseAG.Kind.BUFFER)
		override fun toString(): String = "Buffer[$id]"
	}

	inner class LogRenderBuffer(override val id: Int, val isMain: Boolean) : RenderBuffer() {
        override fun setSize(x: Int, y: Int, width: Int, height: Int, fullWidth: Int, fullHeight: Int) {
            super.setSize(x, y, width, height, fullWidth, fullHeight)
            log("$this.setSize($width, $height)", LogBaseAG.Kind.RENDER_BUFFER)
        }
        override fun set() = log("$this.set()", LogBaseAG.Kind.RENDER_BUFFER)
		override fun close() = log("$this.close()", LogBaseAG.Kind.RENDER_BUFFER)
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

	override fun createTexture(premultiplied: Boolean): Texture =
		LogTexture(textureId++, premultiplied).apply { log("createTexture():$id", Kind.TEXTURE) }

	override fun createBuffer(kind: Buffer.Kind): Buffer =
		LogBuffer(bufferId++, kind).apply { log("createBuffer($kind):$id", Kind.BUFFER) }

    data class VertexAttributeEx(val index: Int, val attribute: Attribute, val pos: Int, val data: VertexData) {
        val layout = data.layout
        val buffer = data.buffer as LogBuffer
    }

    override fun draw(batch: Batch) {
        val program = batch.program
        val type = batch.type
        val vertexData = batch.vertexData
        val vertexCount = batch.vertexCount
        val instances = batch.instances
        val indices = batch.indices
        val offset = batch.offset
        val blending = batch.blending
        val uniforms = batch.uniforms
        val stencil = batch.stencil
        val colorMask = batch.colorMask
        val renderState = batch.renderState
        val scissor = batch.scissor
        try {
            log("draw(vertexCount=$vertexCount, instances=$instances, indices=$indices, type=$type, offset=$offset)", Kind.DRAW)
            log("::draw.program=$program", Kind.DRAW)
            log("::draw.renderState=$renderState", Kind.DRAW)
            log("::draw.scissor=$scissor", Kind.DRAW)
            log("::draw.stencil=$stencil", Kind.DRAW)
            log("::draw.blending=$blending", Kind.DRAW)
            log("::draw.colorMask=$colorMask", Kind.DRAW)

            for (index in 0 until uniforms.size) {
                val uniform = uniforms.keys[index]
                val value = uniforms.values[index]
                log("::draw.uniform.$uniform = ${value.convertToStriangle()}", Kind.DRAW)
            }

            val vertexLayoutAttributesEx = vertexData.flatMap { vd ->
                vd.layout.attributes.zip(vd.layout.attributePositions).mapIndexed { index, pair ->
                    VertexAttributeEx(index, pair.first, pair.second, vd)
                }
            }
            val vertexLayoutAttributes = vertexLayoutAttributesEx.map { it.attribute }.toSet()

            val missingUniforms = program.uniforms - uniforms.keys
            val extraUniforms = uniforms.keys - program.uniforms
            val missingAttributes = vertexLayoutAttributes - program.attributes
            val extraAttributes = program.attributes - vertexLayoutAttributes

            if (missingUniforms.isNotEmpty()) log("::draw.ERROR.Missing:$missingUniforms", Kind.DRAW)
            if (extraUniforms.isNotEmpty()) log("::draw.ERROR.Unexpected:$extraUniforms", Kind.DRAW)

            if (missingAttributes.isNotEmpty()) log("::draw.ERROR.Missing:$missingAttributes", Kind.DRAW)
            if (extraAttributes.isNotEmpty()) log("::draw.ERROR.Unexpected:$extraAttributes", Kind.DRAW)

            val _indices = when {
                indices != null -> {
                    val indexMem = (indices as LogBuffer).logmem!!
                    val range = offset until offset + vertexCount
                    when (batch?.indexType) {
                        IndexType.UBYTE -> range.mapInt { indexMem.getAlignedUInt8(it) }
                        IndexType.USHORT -> range.mapInt { indexMem.getAlignedUInt16(it) }
                        IndexType.UINT -> range.mapInt { indexMem.getAlignedInt32(it) }
                    }
                }
                else -> {
                    (0 until vertexCount).toList().toIntList()
                }
            }
            for (vlae in vertexLayoutAttributesEx) {
                log("::draw.attribute[${vlae.buffer.id}][${vlae.index}]=${vlae.attribute.toStringEx()}", Kind.DRAW)
            }

            log("::draw.indices=$_indices", Kind.DRAW)
            for (doInstances in listOf(false, true)) {
                for (index in if (doInstances) IntArray(instances) { it }.toIntArrayList() else _indices.sorted().distinct()) {
                    val attributes = arrayListOf<String>()
                    for (vlae in vertexLayoutAttributesEx) {
                        if ((vlae.attribute.divisor == 0) == doInstances) continue
                        val attribute = vlae.attribute
                        val vm = vlae.buffer.logmem!!
                        val attributeType = attribute.type
                        val o = (index * vlae.layout.totalSize) + vlae.pos + vlae.buffer.logmemOffset
                        val acount = attributeType.elementCount

                        val info: List<Number> = when (attributeType.kind) {
                            VarKind.TBYTE -> (0 until acount).map { vm.getUnalignedInt8(o + it * 1) }.map { if (attribute.normalized) it.toFloat() / Byte.MAX_VALUE else it }
                            VarKind.TUNSIGNED_BYTE -> (0 until acount).map { vm.getUnalignedUInt8(o + it * 1) }.map { if (attribute.normalized) it.toFloat() / 0xFF else it }
                            VarKind.TSHORT -> (0 until acount).map { vm.getUnalignedInt16(o + it * 2) }.map { if (attribute.normalized) it.toFloat() / Short.MAX_VALUE else it }
                            VarKind.TUNSIGNED_SHORT -> (0 until acount).map { vm.getUnalignedUInt16(o + it * 2) }.map { if (attribute.normalized) it.toFloat() / 0xFFFF else it }
                            VarKind.TINT -> (0 until acount).map { vm.getUnalignedInt32(o + it * 4) }.map { if (attribute.normalized) (it.toFloat() / Int.MAX_VALUE) else it }
                            VarKind.TFLOAT -> (0 until acount).map { vm.getUnalignedFloat32(o + it * 4) }.map { if (attribute.normalized) it.clamp01() else it }
                        }

                        attributes += "${attribute.name}[${info.joinToString(",") { if (it is Float) it.niceStr else it.toString() }}]"
                    }
                    if (doInstances) {
                        if (attributes.isNotEmpty()) {
                            log("::draw.instance[$index]: ${attributes.joinToString(", ")}", Kind.DRAW)
                        }
                    } else {
                        log("::draw.vertex[$index]: ${attributes.joinToString(", ")}", Kind.DRAW)
                    }
                }
            }
            log("::draw.shader.vertex=${GlslGenerator(ShaderType.VERTEX).generate(program.vertex.stm)}", Kind.SHADER)
            log("::draw.shader.fragment=${GlslGenerator(ShaderType.FRAGMENT).generate(program.fragment.stm)}", Kind.SHADER)
        } catch (e: Throwable) {
            log("LogBaseAG.draw.ERROR: ${e.message}", Kind.DRAW)
            e.printStackTrace()
        }
    }

    fun Any?.convertToStriangle(): Any? = when (this) {
        is IntArray -> this.toList()
        is FloatArray -> this.toList()
        else -> this
    }

    override fun disposeTemporalPerFrameStuff() = log("disposeTemporalPerFrameStuff()", Kind.DISPOSE)
	override fun createRenderBuffer(): RenderBuffer =
		LogRenderBuffer(renderBufferId++, isMain = false).apply { log("createRenderBuffer():$id", Kind.RENDER_BUFFER) }

    override fun createMainRenderBuffer(): RenderBuffer =
        LogRenderBuffer(renderBufferId++, isMain = true).apply { log("createMainRenderBuffer():$id", Kind.RENDER_BUFFER) }

    override fun flipInternal() = log("flipInternal()", Kind.FLIP)
	override fun readColor(bitmap: Bitmap32) = log("$this.readBitmap($bitmap)", Kind.READ)
	override fun readDepth(width: Int, height: Int, out: FloatArray) = log("$this.readDepth($width, $height, $out)", Kind.READ)
}
