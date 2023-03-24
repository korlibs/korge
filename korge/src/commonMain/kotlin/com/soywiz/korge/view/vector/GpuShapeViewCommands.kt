package com.soywiz.korge.view.vector

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
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

    private var decomposed = MatrixTransform()
    private val texturesToDelete = FastArrayList<AGTexture>()
    fun render(ctx: RenderContext, globalMatrix: Matrix, localMatrix: Matrix, applyScissor: Boolean, colorMul: RGBA, doRequireTexture: Boolean) {
        val vertices = this.vertices ?: return
        ctx.agBufferManager.delete(verticesToDelete)
        verticesToDelete.clear()

        ctx.flush()
        val ag = ctx.ag
        ctx.useBatcher { batcher ->
            batcher.updateStandardUniforms()
            //tempMat.identity()
            val tempMat = when {
                doRequireTexture -> Matrix.IDENTITY
                else -> batcher.viewMat2D
            }.premultiplied(globalMatrix)
            batcher.setViewMatrixTemp(tempMat) {
                decomposed = globalMatrix.toTransform()

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
                //println("----")
                //println("----")
                //println("----")
                commands.fastForEach { cmd ->
                    //println("cmd:$cmd :: ${ctx.currentFrameBuffer}")
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
                            val pixelScale = decomposed.scaleAvg / ctx.bp.globalToWindowScaleAvg
                            if (paintShader != null) {
                                batcher.ctx[GpuShapeViewPrograms.ShapeViewUB].push {
                                    it.copyFrom(paintShader.uniforms)
                                    it[u_GlobalPixelScale] = pixelScale
                                    it[u_ColorMul] = colorMul
                                }
                                if (paintShader.texture != null) {
                                    val tex = ctx.tempTexturePool.alloc()
                                    tex.upload(paintShader.texture)
                                    ctx.textureUnits.set(DefaultShaders.u_Tex, tex)
                                    //println("texture.tex=$tex")
                                    texturesToDelete.add(tex)
                                }
                            }
                            //val pixelScale = 1f

                            //val texUnit = tempUniforms[DefaultShaders.u_Tex] as? AGTextureUnit?

                            //println("outPremultiplied=$outPremultiplied, blendMode=${cmd.blendMode?.name}")

                            val _program = cmd.program ?: program
                            ag.draw(AGBatch(
                                ctx.currentFrameBuffer.base,
                                ctx.currentFrameBuffer.info,
                                program = _program,
                                vertexData = vertices,
                                //indices = indices,
                                scissor = scissor.applyMatrixBounds(tempMat),
                                uniformBlocks = ctx.createCurrentUniformsRef(_program),
                                textureUnits = ctx.textureUnits.clone(),
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
        for (tex in texturesToDelete) {
            ctx.tempTexturePool.free(tex)
        }
        texturesToDelete.clear()
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
