package com.soywiz.korag

import com.soywiz.kds.fastArrayListOf
import com.soywiz.korag.shader.Program
import com.soywiz.korag.shader.VertexLayout
import com.soywiz.korma.geom.Rectangle

fun AGList.enableBlend(): Unit = enable(AGEnable.BLEND)
fun AGList.enableCullFace(): Unit = enable(AGEnable.CULL_FACE)
fun AGList.enableDepth(): Unit = enable(AGEnable.DEPTH)
fun AGList.enableScissor(): Unit = enable(AGEnable.SCISSOR)
fun AGList.enableStencil(): Unit = enable(AGEnable.STENCIL)
fun AGList.disableBlend(): Unit = disable(AGEnable.BLEND)
fun AGList.disableCullFace(): Unit = disable(AGEnable.CULL_FACE)
fun AGList.disableDepth(): Unit = disable(AGEnable.DEPTH)
fun AGList.disableScissor(): Unit = disable(AGEnable.SCISSOR)
fun AGList.disableStencil(): Unit = disable(AGEnable.STENCIL)

fun AGList.setBlendingState(blending: AGBlending? = null) {
    val blending = blending ?: AGBlending.NORMAL
    enableDisable(AGEnable.BLEND, blending.enabled) {
        blendEquation(blending.eqRGB, blending.eqA)
        blendFunction(blending.srcRGB, blending.dstRGB, blending.srcA, blending.dstA)
    }
}

fun AGList.setRenderState(renderState: AGRenderState) {
    enableDisable(AGEnable.CULL_FACE, renderState.frontFace != AGFrontFace.BOTH) {
        frontFace(renderState.frontFace)
    }

    depthMask(renderState.depthMask)
    depthRange(renderState.depthNear, renderState.depthFar)

    enableDisable(AGEnable.DEPTH, renderState.depthFunc != AGCompareMode.ALWAYS) {
        depthFunction(renderState.depthFunc)
    }
}

fun AGList.setColorMaskState(colorMask: AGColorMaskState?) {
    colorMask(colorMask?.red ?: true, colorMask?.green ?: true, colorMask?.blue ?: true, colorMask?.alpha ?: true)
}

fun AGList.setStencilState(stencilOpFunc: AGStencilOpFuncState?, stencilRef: AGStencilReferenceState) {
    if (stencilOpFunc != null && stencilOpFunc.enabled) {
        enable(AGEnable.STENCIL)
        stencilFunction(stencilOpFunc.compareMode, stencilRef.referenceValue, stencilRef.readMask)
        stencilOperation(
            stencilOpFunc.actionOnDepthFail,
            stencilOpFunc.actionOnDepthPassStencilFail,
            stencilOpFunc.actionOnBothPass
        )
        stencilMask(stencilRef.writeMask)
    } else {
        disable(AGEnable.STENCIL)
        stencilMask(0)
    }
}

fun AGList.setScissorState(ag: AG, scissor: AGScissor = AGScissor.NIL) =
    setScissorState(ag.currentRenderBuffer, ag.mainRenderBuffer, scissor)

fun AGList.setScissorState(currentRenderBuffer: AGBaseRenderBuffer?, mainRenderBuffer: AGBaseRenderBuffer, scissor: AGScissor = AGScissor.NIL) {
    if (currentRenderBuffer == null) return

    //println("applyScissorState")
    val finalScissorBL = tempRect

    val realBackWidth = mainRenderBuffer.fullWidth
    val realBackHeight = mainRenderBuffer.fullHeight

    if (currentRenderBuffer === mainRenderBuffer) {
        var realScissors: Rectangle? = finalScissorBL
        realScissors?.setTo(0.0, 0.0, realBackWidth.toDouble(), realBackHeight.toDouble())
        if (scissor != AGScissor.NIL) {
            tempRect.setTo(
                currentRenderBuffer.x + scissor.x,
                ((currentRenderBuffer.y + currentRenderBuffer.height) - (scissor.y + scissor.height)),
                (scissor.width),
                scissor.height
            )
            realScissors = realScissors?.intersection(tempRect, realScissors)
        }

        //println("currentRenderBuffer: $currentRenderBuffer")

        val renderBufferScissor = currentRenderBuffer.scissor
        if (renderBufferScissor != null) {
            realScissors = realScissors?.intersection(renderBufferScissor.rect, realScissors)
        }

        //println("[MAIN_BUFFER] realScissors: $realScissors")

        enable(AGEnable.SCISSOR)
        if (realScissors != null) {
            scissor(realScissors.x.toInt(), realScissors.y.toInt(), realScissors.width.toInt(), realScissors.height.toInt())
        } else {
            scissor(0, 0, 0, 0)
        }
    } else {
        //println("[RENDER_TARGET] scissor: $scissor")

        enableDisable(AGEnable.SCISSOR, scissor != AGScissor.NIL) {
            scissor(scissor.x, scissor.y, scissor.width, scissor.height)
        }
    }
}

fun AGList.setState(
    blending: AGBlending = AGBlending.NORMAL,
    stencilOpFunc: AGStencilOpFuncState = AGStencilOpFuncState.DEFAULT,
    stencilRef: AGStencilReferenceState = AGStencilReferenceState.DEFAULT,
    colorMask: AGColorMaskState = AGColorMaskState.ALL_ENABLED,
    renderState: AGRenderState = AGRenderState.DEFAULT,
) {
    setBlendingState(blending)
    setRenderState(renderState)
    setColorMaskState(colorMask)
    setStencilState(stencilOpFunc, stencilRef)
}

inline fun AGList.useProgram(ag: AG, program: Program) {
    useProgram(ag.getProgram(program))
}

inline fun AGList.uniformsSet(uniforms: AGUniformValues?, block: () -> Unit) {
    //if (true) {
        tempUBOs.alloc { ubo ->
            uboSet(ubo, uniforms ?: AGUniformValues())
            uboUse(ubo)
            block()
        }
    //} else {
    //    val ubo = uboCreate()
    //    try {
    //        uboSet(ubo, uniforms ?: AGUniformValues())
    //        uboUse(ubo)
    //        block()
    //    } finally {
    //        uboDelete(ubo)
    //    }
    //}
}

inline fun AGList.vertexArrayObjectSet(vao: AGVertexArrayObject, block: () -> Unit) {
    val vaoId = vaoCreate()
    try {
        vaoSet(vaoId, vao)
        vaoUse(vaoId)
        block()
    } finally {
        vaoUse(0)
        vaoDelete(vaoId)
    }
}

inline fun AGList.vertexArrayObjectSet(ag: AG, layout: VertexLayout, data: Any, offset: Int = 0, length: Int = -1, block: () -> Unit) {
    ag.tempVertexBufferPool.alloc { buffer ->
        buffer.upload(data, offset, length)
        vertexArrayObjectSet(AGVertexArrayObject(fastArrayListOf(AGVertexData(buffer, layout)))) {
            block()
        }
    }
}
