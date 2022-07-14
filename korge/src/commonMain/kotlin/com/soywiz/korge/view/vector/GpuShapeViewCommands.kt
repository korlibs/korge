package com.soywiz.korge.view.vector

import com.soywiz.kds.FastArrayList
import com.soywiz.kds.fastArrayListOf
import com.soywiz.kds.floatArrayListOf
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korag.AG
import com.soywiz.korag.DefaultShaders
import com.soywiz.korag.disableCullFace
import com.soywiz.korag.disableScissor
import com.soywiz.korag.enableCullFace
import com.soywiz.korag.setBlendingState
import com.soywiz.korag.setColorMaskState
import com.soywiz.korag.setScissorState
import com.soywiz.korag.setStencilState
import com.soywiz.korag.shader.Program
import com.soywiz.korag.uniformsSet
import com.soywiz.korag.useProgram
import com.soywiz.korag.vertexArrayObjectSet
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
        drawType: AG.DrawType,
        paintShader: GpuShapeViewPrograms.PaintShader?,
        colorMask: AG.ColorMaskState? = null,
        stencil: AG.StencilState? = null,
        blendMode: BlendMode? = null,
        cullFace: AG.CullFace? = null,
        startIndex: Int = this.verticesStartIndex,
        endIndex: Int = this.vertexIndex
    ) {
        commands += ShapeCommand(
            drawType = drawType,
            vertexIndex = startIndex,
            vertexEnd = endIndex,
            paintShader = paintShader,
            colorMask = colorMask,
            stencil = stencil,
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
        vertices = AgCachedBuffer(bufferVertexData)
    }

    private val decomposed = Matrix.Transform()
    private val tempColorMul = FloatArray(4)
    private val texturesToDelete = FastArrayList<AG.Texture>()
    private val tempUniforms = AG.UniformValues()
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
            batcher.setTemporalUniform(GpuShapeViewPrograms.u_ColorMul, tempColorMul) {
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

                        //list.setScissorState(ag, AG.Scissor().setTo(rect))
                        //list.disableScissor()

                        //ag.commandsSync { list ->
                        // Set to default state
                        //list.useProgram(ag.getProgram(GpuShapeViewPrograms.PROGRAM_COMBINED))
                        //println(bufferVertexData)
                        list.useProgram(ag, GpuShapeViewPrograms.PROGRAM_COMBINED)
                        //list.vertexArrayObjectSet(ag, GpuShapeViewPrograms.LAYOUT_POS_TEX_FILL_DIST, bufferVertexData) {
                        list.vertexArrayObjectSet(
                            AG.VertexArrayObject(
                                fastArrayListOf(
                                    AG.VertexData(
                                        ctx.getBuffer(
                                            vertices
                                        ), GpuShapeViewPrograms.LAYOUT_POS_TEX_FILL_DIST
                                    )
                                )
                            )
                        ) {
                            list.uniformsSet(batcher.uniforms) {
                                val ubo = list.uboCreate()
                                try {
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
                                                    list.setScissorState(ag, AG.Scissor().setTo(rect))
                                                } else {
                                                    list.setScissorState(ag, null)
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
                                                tempUniforms.put(GpuShapeViewPrograms.u_GlobalPixelScale, pixelScale)

                                                val texUnit = tempUniforms[DefaultShaders.u_Tex] as? AG.TextureUnit?
                                                //val premultiplied = texUnit?.texture?.premultiplied ?: false
                                                val premultiplied = false
                                                val outPremultiplied = ag.isRenderingToTexture

                                                //println("outPremultiplied=$outPremultiplied, blendMode=${cmd.blendMode?.name}")

                                                tempUniforms[BatchBuilder2D.u_InputPre] = premultiplied
                                                tempUniforms[BatchBuilder2D.u_OutputPre] = outPremultiplied

                                                list.uboSet(ubo, tempUniforms)
                                                list.uboUse(ubo)

                                                list.setStencilState(cmd.stencil)
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
                                } finally {
                                    list.uboDelete(ubo)
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

    private fun resolve(ctx: RenderContext, uniforms: AG.UniformValues, texUniforms: AG.UniformValues) {
        texUniforms.fastForEach { uniform, value ->
            if (value is Bitmap) {
                val tex = ctx.ag.tempTexturePool.alloc()
                tex.upload(value)
                uniforms[uniform] = AG.TextureUnit(tex)
                texturesToDelete.add(tex)
            }
        }
    }

    sealed interface ICommand

    data class ScissorCommand(val scissor: Rectangle?) : ICommand

    data class ClearCommand(val i: Int) : ICommand

    //object FinishCommand : ICommand

    data class ShapeCommand(
        var drawType: AG.DrawType = AG.DrawType.LINE_STRIP,
        var vertexIndex: Int = 0,
        var vertexEnd: Int = 0,
        var paintShader: GpuShapeViewPrograms.PaintShader?,
        var program: Program? = null,
        var colorMask: AG.ColorMaskState? = null,
        var stencil: AG.StencilState? = null,
        var blendMode: BlendMode? = null,
        var cullFace: AG.CullFace? = null
    ) : ICommand {
        val vertexCount: Int get() = vertexEnd - vertexIndex
    }
}
