package com.soywiz.korge.view

import com.soywiz.korag.shader.*
import com.soywiz.korge.render.*
import com.soywiz.korma.geom.*

abstract class FastRoundRectBase(
    width: Double = 100.0,
    height: Double = 100.0,
    cornersRatio: RectCorners = RectCorners(.0f, .0f, .0f, .0f),
    doScale: Boolean = true
) : ShadedView(PROGRAM, width, height) {
    protected var cornersRatio = cornersRatio
    protected var doScale = doScale

    override fun renderInternal(ctx: RenderContext) {
        //colorMul = Colors.RED
        this.programUniforms[u_Corners] = cornersRatio
        this.programUniforms[u_Scale] = when {
            !doScale || width == height -> Point(1f, 1f)
            width > height -> Point(width / height, 1.0)
            else -> Point(1.0, height / width)
        }

        super.renderInternal(ctx)
    }
    companion object {
        val u_Corners by Uniform(VarType.Float4)
        val u_Scale by Uniform(VarType.Float2)
        val PROGRAM = buildShader {
            val SDF = SDFShaders
            SET(out, v_Col * SDF.opAA(SDF.roundedBox((v_Tex - vec2(.5f, .5f)) * u_Scale, vec2(.5f, .5f) * u_Scale, u_Corners * .5f.lit)))
        }
    }
}
