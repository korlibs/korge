package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kds.lock.*
import com.soywiz.kgl.*
import com.soywiz.kmem.*
import com.soywiz.korag.gl.*
import com.soywiz.korag.shader.*

class NAGOpengl(val gl: KmlGl) : NAG() {
    override fun execute(command: NAGCommand) {
        deletesLock {
            deletes.fastForEach {
                it._nativeDelete?.invoke(it)
                it._nativeDelete = null
            }
            deletes.clear()
        }
        when (command) {
            is NAGCommandFinish -> execute(command)
            is NAGCommandFullBatch -> command.batches.fastForEach { execute(it) }
            is NAGCommandTransfer.CopyToTexture -> TODO()
            is NAGCommandTransfer.ReadBits -> TODO()
        }
    }

    private fun execute(command: NAGCommandFinish) {
        gl.finish()
        command.completed?.invoke(Unit)
    }

    private fun execute(batch: NAGBatch) {
        bindVertexData(batch.vertexData, bind = true)
        try {
            if (batch.indexData != null) {
                bindBuffer(AGBufferKind.INDEX, batch.indexData)
            }
        } finally {
            bindVertexData(batch.vertexData, bind = false)
        }
    }

    private val currentState = AGFullState()

    private fun execute(batch: NAGUniformBatch) {
        bindRenderBuffer(batch.renderBuffer)
        bindProgram(batch.program)
        execute(batch.state)
        execute(batch.uniforms)
        batch.drawCommands.fastForEach { drawType, indexType, offset, count, instances ->
            if (instances > 1) {
                when (indexType) {
                    AGIndexType.NONE -> gl.drawArraysInstanced(drawType.toGl(), offset, count, instances)
                    else -> gl.drawElementsInstanced(drawType.toGl(), count, indexType.toGl(), offset, instances)
                }
            } else {
                when (indexType) {
                    AGIndexType.NONE -> gl.drawArrays(drawType.toGl(), offset, count)
                    else -> gl.drawElements(drawType.toGl(), count, indexType.toGl(), offset)
                }
            }
        }
    }

    private fun execute(uniforms: AGUniformValues) {
        TODO()
    }

    private fun execute(state: AGFullState) {
        val colorMask = state.colorMask
        if (currentState.colorMask != colorMask) {
            currentState.colorMask = colorMask
            gl.colorMask(colorMask.red, colorMask.green, colorMask.blue, colorMask.alpha)
        }

        val scissorXY = state.scissorXY
        val scissorWH = state.scissorWH
        if (currentState.scissorXY != scissorXY || currentState.scissorWH != scissorWH) {
            currentState.scissorXY = scissorXY
            currentState.scissorWH = scissorWH
            gl.scissor(scissorXY.x, scissorXY.y, scissorWH.width, scissorWH.height)
        }

        val stencilOpFunc = state.stencilOpFunc
        val stencilRef = state.stencilRef
        if (currentState.stencilOpFunc != stencilOpFunc || currentState.stencilRef != stencilRef) {
            currentState.stencilOpFunc = stencilOpFunc
            currentState.stencilRef = stencilRef
            gl.stencilFunc(stencilOpFunc.compareMode.toGl(), stencilRef.referenceValue, stencilRef.readMask)
            gl.stencilMask(stencilRef.writeMask)
            gl.stencilOp(stencilOpFunc.actionOnDepthFail.toGl(), stencilOpFunc.actionOnDepthPassStencilFail.toGl(), stencilOpFunc.actionOnBothPass.toGl())
        }

        val blending = state.blending
        if (currentState.blending != blending) {
            currentState.blending = blending
            gl.blendFuncSeparate(blending.srcRGB.toGl(), blending.dstRGB.toGl(), blending.srcA.toGl(), blending.dstA.toGl())
            gl.blendEquationSeparate(blending.eqRGB.toGl(), blending.eqA.toGl())
        }
    }

    private fun bindVertexData(vertices: NAGVertices, bind: Boolean) {
        vertices.data.fastForEach { (vertexLayout, vertices) ->
            val vattrs = vertexLayout.attributes
            val vattrspos = vertexLayout.attributePositions

            //if (vertices.kind != AG.BufferKind.VERTEX) invalidOp("Not a VertexBuffer")

            bindBuffer(AGBufferKind.VERTEX, vertices)
            val totalSize = vertexLayout.totalSize
            for (n in 0 until vattrspos.size) {
                val att = vattrs[n]
                if (!att.active) continue
                val off = vattrspos[n]
                if (att.fixedLocation == null) {
                    //error("WOOPS!")
                }
                val loc = att.fixedLocation ?: error("fixedLocation not set")
                val glElementType = att.type.toGl()
                val elementCount = att.type.elementCount
                if (bind) {
                    if (loc >= 0) {
                        gl.enableVertexAttribArray(loc)
                        gl.vertexAttribPointer(
                            loc,
                            elementCount,
                            glElementType,
                            att.normalized,
                            totalSize,
                            off.toLong()
                        )
                        if (att.divisor != 0) {
                            gl.vertexAttribDivisor(loc, att.divisor)
                        }
                    }
                } else {
                    if (att.divisor != 0) {
                        gl.vertexAttribDivisor(loc, 0)
                    }
                    gl.disableVertexAttribArray(loc)
                }
            }
        }
        bindBuffer(AGBufferKind.VERTEX, null)
    }

    private val emptyBuffer = Buffer(0)

    private val deletesLock = Lock()
    private val deletes = fastArrayListOf<NAGObject>()

    private val _nativeDeleteRegister = { obj: NAGObject ->
        deletesLock { deletes += obj }
    }

    private fun bindBuffer(kind: AGBufferKind, buffer: NAGBuffer?) {
        val target = kind.toGl()
        if (buffer == null) {
            gl.bindBuffer(target, 0)
            return
        }
        buffer._nativeCreateObject(contextVersion, _nativeDeleteRegister) {
            buffer._nativeInt = gl.genBuffer()
            buffer._nativeDelete = { gl.deleteBuffer(it._nativeInt) }
        }
        gl.bindBuffer(target, buffer._nativeInt)
        buffer._nativeUploadObject {
            gl.bufferData(
                target,
                buffer.content?.size ?: 0, buffer.content ?: emptyBuffer,
                when (buffer.usage) {
                    NAGBuffer.Usage.DYNAMIC -> KmlGl.DYNAMIC_DRAW
                    NAGBuffer.Usage.STATIC -> KmlGl.STATIC_DRAW
                    NAGBuffer.Usage.STREAM -> KmlGl.STREAM_DRAW
                }
            )
        }
    }

    private val programs = LinkedHashMap<Program, NAGProgram>()

    private fun bindProgram(program: Program) {
        val nagProgram = programs.getOrPut(program) { NAGProgram().also { it.set(program) } }
        nagProgram._nativeCreateObject(contextVersion, _nativeDeleteRegister) {
            nagProgram._nativeInt = gl.createProgram()
            nagProgram._nativeInt1 = gl.createShader(KmlGl.FRAGMENT_SHADER)
            nagProgram._nativeInt2 = gl.createShader(KmlGl.VERTEX_SHADER)
            nagProgram._nativeDelete = {
                gl.deleteProgram(it._nativeInt)
                gl.deleteShader(it._nativeInt1)
                gl.deleteShader(it._nativeInt2)
            }
        }
        TODO()
    }

    private fun bindRenderBuffer(renderBuffer: NAGRenderBuffer) {
        TODO()
    }
}
