package com.soywiz.korag.log

import com.soywiz.kds.FastArrayList
import com.soywiz.kds.IntArrayList
import com.soywiz.kds.fastArrayListOf
import com.soywiz.kds.linkedHashMapOf
import com.soywiz.kds.mapInt
import com.soywiz.kds.toIntArrayList
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.Attribute
import com.soywiz.korag.shader.Program
import com.soywiz.korag.shader.ProgramConfig
import com.soywiz.korag.shader.Shader
import com.soywiz.korag.shader.ShaderType
import com.soywiz.korag.shader.UniformLayout
import com.soywiz.korag.shader.VarKind
import com.soywiz.korag.shader.gl.GlslGenerator
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.annotations.KorInternal
import com.soywiz.korio.util.niceStr

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
			log("$this.uploadedBitmap($source, ${source.width}, ${source.height})", Kind.TEXTURE_UPLOAD)
		}

		override fun close() {
			super.close()
			log("$this.close()", Kind.CLOSE)
		}
		override fun toString(): String = "Texture[$id]"
	}

	inner class LogBuffer(val id: Int, list: AGList) : AGBuffer(this, list) {
		val logmem: com.soywiz.kmem.Buffer? get() = mem
		override fun afterSetMem() {
            super.afterSetMem()
            log("$this.afterSetMem(mem[${mem!!.size}])", LogBaseAG.Kind.BUFFER)
        }
		override fun close(list: AGList) = log("$this.close()", LogBaseAG.Kind.BUFFER)
		override fun toString(): String = "Buffer[$id]"
	}

	inner class LogRenderBuffer(override val id: Int, val isMain: Boolean) : AGRenderBuffer(this) {
        override fun setSize(x: Int, y: Int, width: Int, height: Int, fullWidth: Int, fullHeight: Int) {
            super.setSize(x, y, width, height, fullWidth, fullHeight)
            log("$this.setSize($width, $height)", Kind.FRAME_BUFFER)
        }
        override fun set() = log("$this.set()", Kind.FRAME_BUFFER)
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

	override fun createBuffer(): AGBuffer =
        commandsNoWait { LogBuffer(bufferId++, _list).apply { log("createBuffer():$id", Kind.BUFFER) } }

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

    val agProcessor = object : AGQueueProcessor {
        override fun flush() = log("flush", Kind.FLUSH)

        override fun finish() = log("finish", Kind.CLOSE)

        override fun contextLost() { log("contextLost", Kind.CONTEXT_LOST) }

        override fun enableDisable(kind: AGEnable, enable: Boolean) {
            log("${if (enable) "enable" else "disable"}: $kind", Kind.ENABLE_DISABLE)
        }

        override fun readPixelsToTexture(textureId: Int, x: Int, y: Int, width: Int, height: Int, kind: AGReadKind) {
            log("readPixelsToTexture($textureId, $x, $y, $width, $height, $kind)", Kind.READ)
        }

        override fun readPixels(x: Int, y: Int, width: Int, height: Int, data: Any, kind: AGReadKind) =
            log("readPixels($x, $y, $width, $height, $kind)", Kind.READ)

        override fun draw(
            type: AGDrawType,
            vertexCount: Int,
            offset: Int,
            instances: Int,
            indexType: AGIndexType,
            indices: AGBuffer?
        ) {
            val _indices: IntArrayList? = when {
                indices != null -> {
                    val indexMem = (indices as LogBuffer).logmem!!
                    val range = offset until offset + vertexCount
                    when (indexType) {
                        AGIndexType.UBYTE -> range.mapInt { indexMem.getUInt8(it) }
                        AGIndexType.USHORT -> range.mapInt { indexMem.getUInt16(it) }
                        AGIndexType.UINT -> range.mapInt { indexMem.getInt32(it) }
                        else -> null
                    }
                }
                else -> null
            }
            val _indicesSure: IntArrayList = _indices ?: (0 until vertexCount).mapInt { offset + it }
            log("draw: $type, offset=$offset, count=$vertexCount, instances=$instances, indexType=$indexType", Kind.DRAW)
            log("::draw.indices: $_indicesSure", Kind.DRAW_DETAILS)

            val vertexLayoutAttributesEx = vertexData.flatMap { vd ->
                vd.layout.attributes.zip(vd.layout.attributePositions).mapIndexed { index, pair ->
                    VertexAttributeEx(index, pair.first, pair.second, vd)
                }
            }

            log("::draw.attributes[${vertexData.size}]: ${vertexLayoutAttributesEx.map { it.attribute }}", Kind.DRAW_DETAILS)

            for (doInstances in listOf(false, true)) {
                for (index in if (doInstances) IntArray(instances) { it }.toIntArrayList() else _indicesSure.sorted().distinct()) {
                    val attributes = arrayListOf<String>()
                    for (vlae in vertexLayoutAttributesEx) {
                        if ((vlae.attribute.divisor == 0) == doInstances) continue
                        val attribute = vlae.attribute
                        val vm = vlae.buffer.logmem!!
                        val attributeType = attribute.type
                        val o = (index * vlae.layout.totalSize) + vlae.pos
                        val acount = attributeType.elementCount

                        val info: List<Number> = when (attributeType.kind) {
                            VarKind.TBOOL -> (0 until acount).map { vm.getUnalignedInt8(o + it * 1) }.map { if (attribute.normalized) it.toFloat() / Byte.MAX_VALUE else it }
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
                            log("::draw.instance[$index]: ${attributes.joinToString(", ")}", Kind.DRAW_DETAILS)
                        }
                    } else {
                        log("::draw.vertex[$index]: ${attributes.joinToString(", ")}", Kind.DRAW_DETAILS)
                    }
                }
            }
        }

        override fun bufferCreate(id: Int) = log("bufferCreate: $id", Kind.BUFFER)
        override fun bufferDelete(id: Int) = log("bufferDelete: $id", Kind.BUFFER)
        override fun uniformsSet(layout: UniformLayout, data: com.soywiz.kmem.Buffer) = log("uniformsSet: $layout", Kind.UNIFORM)
        override fun uboCreate(id: Int) = log("uboCreate: $id", Kind.UNIFORM)
        override fun uboDelete(id: Int) = log("uboDelete: $id", Kind.UNIFORM)
        override fun uboSet(id: Int, ubo: AGUniformValues) {
            log("uboSet: $id", Kind.UNIFORM)
            ubo.fastForEach { uniformValue ->
                log("uboSet.uniform: ${uniformValue.uniform} = ${AGUniformValues.valueToString(uniformValue)}", Kind.UNIFORM_VALUES)
            }
        }
        override fun uboUse(id: Int) = log("uboUse: $id", Kind.UNIFORM)
        override fun cullFace(face: AGCullFace) = log("cullFace: $face", Kind.OTHER)
        override fun frontFace(face: AGFrontFace) = log("frontFace: $face", Kind.OTHER)
        override fun blendEquation(rgb: AGBlendEquation, a: AGBlendEquation) = log("blendEquation: $rgb, $a", Kind.OTHER)
        override fun blendFunction(srcRgb: AGBlendFactor, dstRgb: AGBlendFactor, srcA: AGBlendFactor, dstA: AGBlendFactor) = log("blendFunction: $srcRgb, $dstRgb, $srcA, $dstA", Kind.OTHER)
        override fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) = log("colorMask: $red, $green, $blue, $alpha", Kind.OTHER)
        override fun depthFunction(depthTest: AGCompareMode) = log("depthFunction: $depthTest", Kind.OTHER)
        override fun depthMask(depth: Boolean) = log("depthMask: $depth", Kind.OTHER)
        override fun depthRange(near: Float, far: Float) = log("depthRange: $near, $far", Kind.OTHER)
        override fun stencilFunction(compareMode: AGCompareMode, referenceValue: Int, readMask: Int) = log("stencilFunction: $compareMode, $referenceValue, $readMask", Kind.OTHER)

        override fun stencilOperation(
            actionOnDepthFail: AGStencilOp,
            actionOnDepthPassStencilFail: AGStencilOp,
            actionOnBothPass: AGStencilOp
        ) = log("stencilOperation: $actionOnDepthFail, $actionOnDepthPassStencilFail, $actionOnBothPass", Kind.OTHER)

        override fun stencilMask(writeMask: Int) = log("stencilMask: $writeMask", Kind.OTHER)
        override fun scissor(x: Int, y: Int, width: Int, height: Int) = log("scissor: $x, $y, $width, $height", Kind.SCISSORS)
        override fun viewport(x: Int, y: Int, width: Int, height: Int) = log("viewport: $x, $y, $width, $height", Kind.VIEWPORT)

        override fun clear(color: Boolean, depth: Boolean, stencil: Boolean) = log("clear: color=$color, depth=$depth, stencil=$stencil", Kind.CLEAR)
        override fun clearColor(red: Float, green: Float, blue: Float, alpha: Float) = log("createColor: $red, $green, $blue, $alpha", Kind.CLEAR)
        override fun clearDepth(depth: Float) = log("createDepth: $depth", Kind.CLEAR)
        override fun clearStencil(stencil: Int) = log("createStencil: $stencil", Kind.CLEAR)

        override fun programCreate(programId: Int, program: Program, programConfig: ProgramConfig?) {
            val fragmentGlSl = GlslGenerator(ShaderType.FRAGMENT).generate(program.fragment)
            val vertexGlSl = GlslGenerator(ShaderType.VERTEX).generate(program.vertex)
            log("programCreate: $programId, $program, $programConfig\nprogramCreate.fragment:${fragmentGlSl}\nprogramCreate.vertex:${vertexGlSl}", Kind.SHADER)
        }
        override fun programDelete(programId: Int) = log("programDelete: $programId", Kind.SHADER)
        override fun programUse(programId: Int) = log("programUse: $programId", Kind.SHADER)
        override fun vaoCreate(id: Int) = log("vaoCreate: $id", Kind.VERTEX)
        override fun vaoDelete(id: Int) = log("vaoDelete: $id", Kind.VERTEX)
        private var vertexData: FastArrayList<AGVertexData> = fastArrayListOf()
        override fun vaoSet(id: Int, vao: AGVertexArrayObject) {
            log("vaoSet: $id, $vao", Kind.VERTEX)
            vertexData = vao.list
        }
        override fun vaoUse(id: Int) = log("vaoUse: $id", Kind.VERTEX)
        override fun textureCreate(textureId: Int) = log("textureCreate: $textureId", Kind.TEXTURE)
        override fun textureDelete(textureId: Int) = log("textureDelete: $textureId", Kind.TEXTURE)
        override fun textureUpdate(
            textureId: Int,
            target: AGTextureTargetKind,
            index: Int,
            bmp: Bitmap?,
            source: AGBitmapSourceBase,
            doMipmaps: Boolean,
            premultiplied: Boolean
        ) {
            log("textureUpdate: $textureId, $target, $index, $bmp, $source, $doMipmaps, $premultiplied", Kind.TEXTURE_UPLOAD)
        }

        override fun textureBind(textureId: Int, target: AGTextureTargetKind, implForcedTexId: Int) = log("textureBind: $textureId", Kind.TEXTURE)
        override fun textureBindEnsuring(tex: AGTexture?) = log("textureBindEnsuring: $tex", Kind.TEXTURE)
        override fun textureSetFromFrameBuffer(textureId: Int, x: Int, y: Int, width: Int, height: Int) = log("textureSetFromFrameBuffer: $textureId, $x, $y, $width, $height", Kind.TEXTURE)
        override fun frameBufferCreate(id: Int) = log("frameBufferCreate: $id", Kind.FRAME_BUFFER)
        override fun frameBufferDelete(id: Int) = log("frameBufferDelete: $id", Kind.FRAME_BUFFER)
        override fun frameBufferSet(
            id: Int,
            textureId: Int,
            width: Int,
            height: Int,
            hasStencil: Boolean,
            hasDepth: Boolean
        ) =
            log("frameBufferSet: $id, $textureId, size=($width, $height), hasStencil=$hasStencil, hasDepth=$hasDepth", Kind.FRAME_BUFFER)

        override fun frameBufferUse(id: Int) = log("frameBufferUse: $id", Kind.FRAME_BUFFER)
    }

    override fun executeList(list: AGList) {
        list.listFlush()
        agProcessor.processBlockingAll(list)
    }

    fun Any?.convertToStriangle(): Any? = when (this) {
        is IntArray -> this.toList()
        is FloatArray -> this.toList()
        else -> this
    }

    override fun disposeTemporalPerFrameStuff() = log("disposeTemporalPerFrameStuff()", Kind.DISPOSE)
	override fun createRenderBuffer(): AGRenderBuffer =
		LogRenderBuffer(renderBufferId++, isMain = false).apply { log("createRenderBuffer():$id", Kind.FRAME_BUFFER) }

    override fun createMainRenderBuffer(): AGRenderBuffer =
        LogRenderBuffer(renderBufferId++, isMain = true).apply { log("createMainRenderBuffer():$id", Kind.FRAME_BUFFER) }

    override fun flipInternal() = log("flipInternal()", Kind.FLIP)
	override fun readColor(bitmap: Bitmap32, x: Int, y: Int) = log("$this.readBitmap($bitmap, $x, $y)", Kind.READ)
	override fun readDepth(width: Int, height: Int, out: FloatArray) = log("$this.readDepth($width, $height, $out)", Kind.READ)
}
