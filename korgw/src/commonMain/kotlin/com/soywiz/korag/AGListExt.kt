package com.soywiz.korag

import com.soywiz.korag.shader.Program
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

fun AGList.setRenderState(renderState: AGDepthAndFrontFace) {
    enableDisable(AGEnable.CULL_FACE, renderState.frontFace != AGFrontFace.BOTH) {
        frontFace(renderState.frontFace)
    }

    depthMask(renderState.depthMask)
    depthRange(renderState.depthNear, renderState.depthFar)

    enableDisable(AGEnable.DEPTH, renderState.depthFunc != AGCompareMode.ALWAYS) {
        depthFunction(renderState.depthFunc)
    }
}

fun AGList.setColorMaskState(colorMask: AGColorMask?) {
    colorMask(colorMask?.red ?: true, colorMask?.green ?: true, colorMask?.blue ?: true, colorMask?.alpha ?: true)
}

fun AGList.setStencilState(stencilOpFunc: AGStencilOpFunc?, stencilRef: AGStencilReference) {
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

fun AGList.setScissorState(currentRenderBuffer: AGBaseFrameBuffer?, mainRenderBuffer: AGBaseFrameBuffer, scissor: AGScissor = AGScissor.NIL) {
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
    stencilOpFunc: AGStencilOpFunc = AGStencilOpFunc.DEFAULT,
    stencilRef: AGStencilReference = AGStencilReference.DEFAULT,
    colorMask: AGColorMask = AGColorMask.ALL_ENABLED,
    renderState: AGDepthAndFrontFace = AGDepthAndFrontFace.DEFAULT,
) {
    setBlendingState(blending)
    setRenderState(renderState)
    setColorMaskState(colorMask)
    setStencilState(stencilOpFunc, stencilRef)
}

inline fun AGList.useProgram(ag: AG, program: Program) {
    useProgram(ag.getProgram(program))
}
