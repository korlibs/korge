package korlibs.korge.view

import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.math.geom.*

abstract class FastRoundRectBase(
    size: Size = Size(100f, 100f),
    cornersRatio: RectCorners = RectCorners(.0f, .0f, .0f, .0f),
    doScale: Boolean = true
) : ShadedView(PROGRAM, size) {
    protected var cornersRatio = cornersRatio
    protected var doScale = doScale

    override fun renderInternal(ctx: RenderContext) {
        //colorMul = Colors.RED
        ctx[SDFUB].push {
            it[u_Corners] = cornersRatio
            it[u_Scale] = when {
                !doScale || widthD == heightD -> Point(1f, 1f)
                widthD > heightD -> Point(widthD / heightD, 1.0)
                else -> Point(1.0, heightD / widthD)
            }
        }
        super.renderInternal(ctx)
    }

    object SDFUB : UniformBlock(fixedLocation = 6) {
        val u_Corners by vec4()
        val u_Scale by vec2()
    }

    companion object {
        val PROGRAM = buildShader {
            val SDF = SDFShaders
            SET(out, v_Col * SDF.opAA(SDF.roundedBox((v_Tex - vec2(.5f, .5f)) * SDFUB.u_Scale, vec2(.5f, .5f) * SDFUB.u_Scale, SDFUB.u_Corners * .5f.lit)))
        }
    }
}
