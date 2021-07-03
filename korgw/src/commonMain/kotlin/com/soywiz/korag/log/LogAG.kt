package com.soywiz.korag.log

import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.lang.*

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

	inner class LogRenderBuffer(override val id: Int) : RenderBuffer() {
        override fun setSize(x: Int, y: Int, width: Int, height: Int, fullWidth: Int, fullHeight: Int) = log("$this.setSize($width, $height)")
        override fun set() = log("$this.set()")
		override fun close() = log("$this.close()")
		override fun toString(): String = "RenderBuffer[$id]"
	}

	private var textureId = 0
	private var bufferId = 0
	private var renderBufferId = 0

	override fun createTexture(premultiplied: Boolean): Texture =
		LogTexture(textureId++, premultiplied).apply { log("createTexture():$id") }

	override fun createBuffer(kind: Buffer.Kind): Buffer =
		LogBuffer(bufferId++, kind).apply { log("createBuffer($kind):$id") }

    override fun draw(batch: Batch) {
        val vertices = batch.vertices
        val program = batch.program
        val type = batch.type
        val vertexLayout = batch.vertexLayout
        val vertexCount = batch.vertexCount
        val indices = batch.indices
        val offset = batch.offset
        val blending = batch.blending
        val uniforms = batch.uniforms
        val stencil = batch.stencil
        val colorMask = batch.colorMask
        val renderState = batch.renderState
        val scissor = batch.scissor
        try {
            log("draw(vertices=$vertices, indices=$indices, program=$program, type=$type, vertexLayout=$vertexLayout, vertexCount=$vertexCount, offset=$offset, blending=$blending, uniforms=$uniforms, stencil=$stencil, colorMask=$colorMask)")

            val missingUniforms = program.uniforms - uniforms.keys
            val extraUniforms = uniforms.keys - program.uniforms
            val missingAttributes = vertexLayout.attributes.toSet() - program.attributes
            val extraAttributes = program.attributes - vertexLayout.attributes.toSet()

            if (missingUniforms.isNotEmpty()) log("::draw.ERROR.Missing:$missingUniforms")
            if (extraUniforms.isNotEmpty()) log("::draw.ERROR.Unexpected:$extraUniforms")

            if (missingAttributes.isNotEmpty()) log("::draw.ERROR.Missing:$missingAttributes")
            if (extraAttributes.isNotEmpty()) log("::draw.ERROR.Unexpected:$extraAttributes")

            val vertexBuffer = vertices as LogBuffer
            val vertexMem = vertexBuffer.logmem!!
            val vertexMemOffset = vertexBuffer.logmemOffset
            val indexMem = (indices as LogBuffer).logmem
            val _indices = (offset until offset + vertexCount).map { indexMem!!.getAlignedInt16(it) }
            log("::draw.indices=$_indices")
            for (index in _indices.sorted().distinct()) {
                val os = index * vertexLayout.totalSize
                val attributes = arrayListOf<String>()
                for ((attribute, pos) in vertexLayout.attributes.zip(vertexLayout.attributePositions)) {
                    val o = os + pos + vertexMemOffset

                    val info = when (attribute.type) {
                        VarType.Int1 -> "int(" + vertexMem.getUnalignedInt32(o + 0) + ")"
                        VarType.Float1 -> "float(" + vertexMem.getUnalignedFloat32(o + 0) + ")"
                        VarType.Float2 -> "vec2(" + vertexMem.getUnalignedFloat32(o + 0) + "," + vertexMem.getUnalignedFloat32(o + 4) + ")"
                        VarType.Float3 -> "vec3(" + vertexMem.getUnalignedFloat32(o + 0) + "," + vertexMem.getUnalignedFloat32(o + 4) + "," + vertexMem.getUnalignedFloat32(
                            o + 8
                        ) + ")"
                        VarType.Byte4 -> "byte4(" + vertexMem.getUnalignedInt32(o + 0) + ")"
                        else -> "Unsupported(${attribute.type})"
                    }

                    attributes += attribute.name + "[" + info + "]"
                }
                log("::draw.vertex[$index]: " + attributes.joinToString(", "))
            }
        } catch (e: Throwable) {
            log("LogBaseAG.draw.ERROR: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun disposeTemporalPerFrameStuff() = log("disposeTemporalPerFrameStuff()")
	override fun createRenderBuffer(): RenderBuffer =
		LogRenderBuffer(renderBufferId++).apply { log("createRenderBuffer():$id") }

	override fun flipInternal() = log("flipInternal()")
	override fun readColor(bitmap: Bitmap32) = log("$this.readBitmap($bitmap)")
	override fun readDepth(width: Int, height: Int, out: FloatArray) = log("$this.readDepth($width, $height, $out)")
}
