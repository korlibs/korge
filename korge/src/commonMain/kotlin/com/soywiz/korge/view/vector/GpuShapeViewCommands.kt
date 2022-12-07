package com.soywiz.korge.view.vector

import com.soywiz.kds.FastArrayList
import com.soywiz.kds.fastArrayListOf
import com.soywiz.kds.floatArrayListOf
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.internal.KorgeInternal
import com.soywiz.korge.render.AgCachedBuffer
import com.soywiz.korge.render.BatchBuilder2D
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.BlendMode
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.writeFloat
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.applyTransform

@KorgeInternal
class GpuShapeViewCommands {
    private var vertexIndex = 0
    private val bufferVertexData = floatArrayListOf()
    private val commands = arrayListOf<ICommand>()
    private var vertices: AgCachedBuffer? = null
    private val verticesToDelete = FastArrayList<AgCachedBuffer>()

    fun clear() {
        vertexIndex = 0
        bufferVertexData.clear()
        commands.clear()
    }

    fun updateVertex(index: Int, x: Float, y: Float, len: Float = 0f, maxLen: Float = len) {
        val p = index * 4
        bufferVertexData[p + 0] = x
        bufferVertexData[p + 1] = y
        bufferVertexData[p + 2] = len
        bufferVertexData[p + 3] = maxLen
    }

    private var warning = 0

    fun addVertex(x: Float, y: Float, len: Float = 0f, maxLen: Float = len): Int {
        if (maxLen <= 0f && warning++ <= 0) {
            println("Invalid maxLen=$maxLen")
        }
        bufferVertexData.add(x, y, len, maxLen)
        return vertexIndex++
    }

    private var verticesStartIndex: Int = 0
    fun verticesStart(): Int {
        verticesStartIndex = vertexIndex
        return verticesStartIndex
    }

    fun verticesEnd(): Int {
        return vertexIndex
    }

    fun draw(
        drawType: AGDrawType,
        paintShader: GpuShapeViewPrograms.PaintShader?,
        colorMask: AGColorMaskState? = null,
        stencilOpFunc: AGStencilOpFuncState? = null,
        stencilRef: AGStencilReferenceState = AGStencilReferenceState.DEFAULT,
        blendMode: BlendMode? = null,
        cullFace: AGCullFace? = null,
        startIndex: Int = this.verticesStartIndex,
        endIndex: Int = this.vertexIndex
    ) {
        commands += ShapeCommand(
            drawType = drawType,
            vertexIndex = startIndex,
            vertexEnd = endIndex,
            paintShader = paintShader,
            colorMask = colorMask,
            stencilOpFunc = stencilOpFunc,
            stencilRef = stencilRef,
            blendMode = blendMode,
            cullFace = cullFace,
        )
    }

    fun clearStencil(i: Int = 0) {
        commands += ClearCommand(i)
    }

    fun setScissor(scissor: Rectangle?) {
        commands += ScissorCommand(scissor)
    }

    fun finish() {
        vertices?.let { verticesToDelete += it }
        vertices = AgCachedBuffer(Float32Buffer(bufferVertexData.toFloatArray()).buffer)
    }

    private val decomposed = Matrix.Transform()
    private val tempColorMul = FloatArray(4)
    private val texturesToDelete = FastArrayList<AGTexture>()
    private val tempUniforms = AGUniformValues()
    private val tempMat = Matrix()
    fun render(ctx: RenderContext, globalMatrix: Matrix, localMatrix: Matrix, applyScissor: Boolean, colorMul: RGBA, doRequireTexture: Boolean) {
        val vertices = this.vertices ?: return
        ctx.agBufferManager.delete(verticesToDelete)
        verticesToDelete.clear()

        ctx.flush()
        val ag = ctx.ag
        ctx.useBatcher { batcher ->
            batcher.updateStandardUniforms()
            colorMul.writeFloat(tempColorMul)
            batcher.keepUniform(GpuShapeViewPrograms.u_ColorMul) {
                it[GpuShapeViewPrograms.u_ColorMul] = tempColorMul
                //tempMat.identity()
                when {
                    doRequireTexture -> tempMat.identity()
                    else -> tempMat.copyFrom(batcher.viewMat2D)
                }
                tempMat.premultiply(globalMatrix)
                batcher.setViewMatrixTemp(tempMat) {
                    globalMatrix.decompose(decomposed)
                    ag.commandsNoWait { list ->
                        // applyScissor is for using the ctx.batch.scissor infrastructure
                        if (!applyScissor) {
                            list.disableScissor()
                        }

                        //list.setScissorState(ag, AGScissor().setTo(rect))
                        //list.disableScissor()

                        //ag.commandsSync { list ->
                        // Set to default state
                        //list.useProgram(ag.getProgram(GpuShapeViewPrograms.PROGRAM_COMBINED))
                        //println(bufferVertexData)
                        list.useProgram(ag, GpuShapeViewPrograms.PROGRAM_COMBINED)
                        //list.vertexArrayObjectSet(ag, GpuShapeViewPrograms.LAYOUT_POS_TEX_FILL_DIST, bufferVertexData) {
                        list.vertexArrayObjectSet(
                            AGVertexArrayObject(
                                fastArrayListOf(
                                    AGVertexData(
                                        ctx.getBuffer(
                                            vertices
                                        ), GpuShapeViewPrograms.LAYOUT_POS_TEX_FILL_DIST
                                    )
                                )
                            )
                        ) {
                            list.uniformsSet(batcher.uniforms)
                            commands.fastForEach { cmd ->
                                when (cmd) {
                                    //is FinishCommand -> list.flush()
                                    is ScissorCommand -> {
                                        val rect = cmd.scissor?.clone()
                                        //rect.normalize()
                                        // @TODO: Do scissor intersection
                                        if (applyScissor) {
                                        }
                                        if (rect != null) {
                                            rect.applyTransform(globalMatrix)
                                            list.setScissorState(ag, AGScissor(rect))
                                        } else {
                                            list.setScissorState(ag, AGScissor.NIL)
                                        }
                                    }

                                    is ClearCommand -> {
                                        list.clearStencil(cmd.i)
                                        list.stencilMask(0xFF)
                                        list.clear(false, false, true)
                                    }

                                    is ShapeCommand -> {
                                        val paintShader = cmd.paintShader
                                        //println("cmd.vertexCount=${cmd.vertexCount}, cmd.vertexIndex=${cmd.vertexIndex}, paintShader=$paintShader")
                                        batcher.simulateBatchStats(cmd.vertexCount)
                                        //println(paintShader.uniforms)
                                        tempUniforms.clear()
                                        paintShader?.uniforms?.let { resolve(ctx, it, paintShader.texUniforms) }
                                        tempUniforms.put(paintShader?.uniforms)
                                        val pixelScale = decomposed.scaleAvg / ctx.bp.globalToWindowScaleAvg
                                        //val pixelScale = 1f
                                        tempUniforms[GpuShapeViewPrograms.u_GlobalPixelScale] = pixelScale

                                        val texUnit = tempUniforms[DefaultShaders.u_Tex] as? AGTextureUnit?
                                        val premultiplied = texUnit?.texture?.premultiplied ?: false
                                        //val premultiplied = false
                                        val outPremultiplied = ag.isRenderingToTexture

                                        //println("outPremultiplied=$outPremultiplied, blendMode=${cmd.blendMode?.name}")

                                        tempUniforms[GpuShapeViewPrograms.u_InputPre] = premultiplied.toInt().toFloat()
                                        tempUniforms[BatchBuilder2D.u_OutputPre] = outPremultiplied

                                        list.uniformsSet(tempUniforms)
                                        list.setStencilState(cmd.stencilOpFunc, cmd.stencilRef)
                                        list.setColorMaskState(cmd.colorMask)
                                        list.setBlendingState((cmd.blendMode ?: BlendMode.NORMAL)?.factors(outPremultiplied))
                                        if (cmd.cullFace != null) {
                                            list.enableCullFace()
                                            list.cullFace(cmd.cullFace!!)
                                        } else {
                                            list.disableCullFace()
                                        }
                                        //println(ctx.batch.viewMat2D)
                                        list.draw(cmd.drawType, cmd.vertexCount, cmd.vertexIndex)
                                    }
                                }
                            }

                        }
                        //list.finish()

                        list.disableCullFace()
                    }
                }
            }
        }
        for (tex in texturesToDelete) {
            ag.tempTexturePool.free(tex)
        }
        texturesToDelete.clear()
    }

    private fun resolve(ctx: RenderContext, uniforms: AGUniformValues, texUniforms: Map<Uniform, Bitmap>) {
        var unitId = 0
        for ((uniform, value) in texUniforms) {
            val tex = ctx.ag.tempTexturePool.alloc()
            tex.upload(value)
            uniforms[uniform] = AGTextureUnit(unitId++, tex)
            texturesToDelete.add(tex)
        }
    }

    sealed interface ICommand

    data class ScissorCommand(val scissor: Rectangle?) : ICommand

    data class ClearCommand(val i: Int) : ICommand

    //object FinishCommand : ICommand

    data class ShapeCommand(
        var drawType: AGDrawType = AGDrawType.LINE_STRIP,
        var vertexIndex: Int = 0,
        var vertexEnd: Int = 0,
        var paintShader: GpuShapeViewPrograms.PaintShader?,
        var program: Program? = null,
        var colorMask: AGColorMaskState? = null,
        var stencilOpFunc: AGStencilOpFuncState? = null,
        var stencilRef: AGStencilReferenceState = AGStencilReferenceState.DEFAULT,
        var blendMode: BlendMode? = null,
        var cullFace: AGCullFace? = null
    ) : ICommand {
        val vertexCount: Int get() = vertexEnd - vertexIndex
    }
}
