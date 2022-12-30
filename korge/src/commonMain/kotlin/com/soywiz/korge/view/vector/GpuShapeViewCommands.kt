package com.soywiz.korge.view.vector

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*

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
        colorMask: AGColorMask = AGColorMask.DEFAULT,
        stencilOpFunc: AGStencilOpFunc = AGStencilOpFunc.DEFAULT,
        stencilRef: AGStencilReference = AGStencilReference.DEFAULT,
        blendMode: BlendMode = BlendMode.NORMAL,
        cullFace: AGCullFace = AGCullFace.NONE,
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

    fun setScissor(scissor: AGScissor) {
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

                    // applyScissor is for using the ctx.batch.scissor infrastructure
                    //list.setScissorState(ag, AGScissor().setTo(rect))
                    //list.disableScissor()

                    //ag.commandsSync { list ->
                    // Set to default state
                    //list.useProgram(ag.getProgram(GpuShapeViewPrograms.PROGRAM_COMBINED))
                    //println(bufferVertexData)
                    val program = GpuShapeViewPrograms.PROGRAM_COMBINED
                    val vertices = AGVertexArrayObject(
                        fastArrayListOf(
                            AGVertexData(
                                GpuShapeViewPrograms.LAYOUT_POS_TEX_FILL_DIST,
                                ctx.getBuffer(
                                    vertices
                                ),
                            )
                        )
                    )
                    var scissor = AGScissor.NIL
                    //list.vertexArrayObjectSet(ag, GpuShapeViewPrograms.LAYOUT_POS_TEX_FILL_DIST, bufferVertexData) {
                    val uniforms = batcher.uniforms
                    commands.fastForEach { cmd ->
                        when (cmd) {
                            //is FinishCommand -> list.flush()
                            is ScissorCommand -> {
                                scissor = cmd.scissor
                            }

                            is ClearCommand -> {
                                ctx.clear(stencil = cmd.i, clearColor = false, clearStencil = true, clearDepth = false)
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

                                //val texUnit = tempUniforms[DefaultShaders.u_Tex] as? AGTextureUnit?

                                //println("outPremultiplied=$outPremultiplied, blendMode=${cmd.blendMode?.name}")

                                ag.draw(AGBatch(
                                    ctx.currentFrameBuffer.base,
                                    ctx.currentFrameBuffer.info,
                                    vertexData = vertices,
                                    uniforms = tempUniforms,
                                    stencilOpFunc = cmd.stencilOpFunc,
                                    stencilRef = cmd.stencilRef,
                                    colorMask = cmd.colorMask,
                                    blending = (cmd.blendMode ?: BlendMode.NORMAL).factors,
                                    cullFace = cmd.cullFace,
                                    drawType = cmd.drawType,
                                    drawOffset = cmd.vertexIndex,
                                    vertexCount = cmd.vertexCount,
                                ))
                            }
                        }
                    }
                    //list.finish()
                }
            }
        }
        for (tex in texturesToDelete) {
            ctx.tempTexturePool.free(tex)
        }
        texturesToDelete.clear()
    }

    private fun resolve(ctx: RenderContext, uniforms: AGUniformValues, texUniforms: Map<Uniform, Bitmap>) {
        for ((uniform, value) in texUniforms) {
            val tex = ctx.tempTexturePool.alloc()
            tex.upload(value)
            uniforms.set(uniform, tex)
            texturesToDelete.add(tex)
        }
    }

    sealed interface ICommand

    data class ScissorCommand(val scissor: AGScissor) : ICommand

    data class ClearCommand(val i: Int) : ICommand

    //object FinishCommand : ICommand

    data class ShapeCommand(
        var drawType: AGDrawType = AGDrawType.LINE_STRIP,
        var vertexIndex: Int = 0,
        var vertexEnd: Int = 0,
        var paintShader: GpuShapeViewPrograms.PaintShader?,
        var program: Program? = null,
        var colorMask: AGColorMask = AGColorMask.DEFAULT,
        var stencilOpFunc: AGStencilOpFunc = AGStencilOpFunc.DEFAULT,
        var stencilRef: AGStencilReference = AGStencilReference.DEFAULT,
        var blendMode: BlendMode? = null,
        var cullFace: AGCullFace = AGCullFace.NONE,
    ) : ICommand {
        val vertexCount: Int get() = vertexEnd - vertexIndex
    }
}
