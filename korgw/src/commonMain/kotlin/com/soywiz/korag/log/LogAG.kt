package com.soywiz.korag.log

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korag.shader.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*

open class PrintAG(
    width: Int = 640,
    height: Int = 480
) : LogBaseAG() {
    override fun log(str: String) {
        println("PrintAG: $str")
    }
}

open class LogAG(
    width: Int = 640,
    height: Int = 480
) : LogBaseAG(width, height) {
    val log = arrayListOf<String>()
    fun getLogAsString(): String = log.joinToString("\n")
    override fun log(str: String) {
        this.log += str
    }
}

open class LogBaseAG(
	width: Int = 640,
	height: Int = 480
) : DummyAG(width, height) {
	open fun log(str: String) {
	}

	override fun clear(
		color: RGBA,
		depth: Float,
		stencil: Int,
		clearColor: Boolean,
		clearDepth: Boolean,
		clearStencil: Boolean
	) = log("clear($color, $depth, $stencil, $clearColor, $clearDepth, $clearStencil)")

	override var backWidth: Int = width; set(value) = run { field = value; log("backWidth = $value") }
	override var backHeight: Int = height; set(value) = run { field = value; log("backHeight = $value") }

	override fun repaint() = log("repaint()")

	override fun dispose() = log("dispose()")

	inner class LogTexture(val id: Int, override val premultiplied: Boolean) : Texture() {
		override fun uploadedSource() {
			log("$this.uploadedBitmap($source, ${source.width}, ${source.height})")
		}

		override fun close() {
			super.close()
			log("$this.close()")
		}
		override fun toString(): String = "Texture[$id]"
	}

	inner class LogBuffer(val id: Int, kind: Kind) : Buffer(kind) {
		val logmem: FBuffer? get() = mem
		val logmemOffset get() = memOffset
		val logmemLength get() = memLength
		override fun afterSetMem() = log("$this.afterSetMem(mem[${mem!!.size}])")
		override fun close() = log("$this.close()")
		override fun toString(): String = "Buffer[$id]"
	}

	inner class LogRenderBuffer(override val id: Int, val isMain: Boolean) : RenderBuffer() {
        override fun setSize(x: Int, y: Int, width: Int, height: Int, fullWidth: Int, fullHeight: Int) = log("$this.setSize($width, $height)")
        override fun set() = log("$this.set()")
		override fun close() = log("$this.close()")
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
		LogTexture(textureId++, premultiplied).apply { log("createTexture():$id") }

	override fun createBuffer(kind: Buffer.Kind): Buffer =
		LogBuffer(bufferId++, kind).apply { log("createBuffer($kind):$id") }

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
            log("draw(vertexCount=$vertexCount, instances=$instances, indices=$indices, type=$type, offset=$offset)")
            log("::draw.program=$program")
            log("::draw.renderState=$renderState")
            log("::draw.scissor=$scissor")
            log("::draw.stencil=$stencil")
            log("::draw.blending=$blending")
            log("::draw.colorMask=$colorMask")

            for (index in 0 until uniforms.size) {
                val uniform = uniforms.keys[index]
                val value = uniforms.values[index]
                log("::draw.uniform.$uniform = ${value.convertToStriangle()}")
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

            if (missingUniforms.isNotEmpty()) log("::draw.ERROR.Missing:$missingUniforms")
            if (extraUniforms.isNotEmpty()) log("::draw.ERROR.Unexpected:$extraUniforms")

            if (missingAttributes.isNotEmpty()) log("::draw.ERROR.Missing:$missingAttributes")
            if (extraAttributes.isNotEmpty()) log("::draw.ERROR.Unexpected:$extraAttributes")

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
                log("::draw.attribute[${vlae.buffer.id}][${vlae.index}]=${vlae.attribute.toStringEx()}")
            }

            log("::draw.indices=$_indices")
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
                            log("::draw.instance[$index]: ${attributes.joinToString(", ")}")
                        }
                    } else {
                        log("::draw.vertex[$index]: ${attributes.joinToString(", ")}")
                    }
                }
            }
        } catch (e: Throwable) {
            log("LogBaseAG.draw.ERROR: ${e.message}")
            e.printStackTrace()
        }
    }

    fun Any?.convertToStriangle(): Any? = when (this) {
        is IntArray -> this.toList()
        is FloatArray -> this.toList()
        else -> this
    }

    override fun disposeTemporalPerFrameStuff() = log("disposeTemporalPerFrameStuff()")
	override fun createRenderBuffer(): RenderBuffer =
		LogRenderBuffer(renderBufferId++, isMain = false).apply { log("createRenderBuffer():$id") }

    override fun createMainRenderBuffer(): RenderBuffer =
        LogRenderBuffer(renderBufferId++, isMain = true).apply { log("createMainRenderBuffer():$id") }

    override fun flipInternal() = log("flipInternal()")
	override fun readColor(bitmap: Bitmap32) = log("$this.readBitmap($bitmap)")
	override fun readDepth(width: Int, height: Int, out: FloatArray) = log("$this.readDepth($width, $height, $out)")
}
