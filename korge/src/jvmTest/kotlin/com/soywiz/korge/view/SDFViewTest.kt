package com.soywiz.korge.view

import com.soywiz.korag.shader.*
import com.soywiz.korge.render.*
import com.soywiz.korge.testing.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import org.junit.*
import kotlin.math.*

class SDFViewTest {
    @Test
    fun test() = korgeScreenshotTest(200, 200) {
        addChild(
            CircleSDFView(width = 200.0, height = 200.0, time = 2.0)
                .skew(15.degrees, 0.degrees)
                .also {
                    it.colorMul = Colors.DARKGREY
                    //it.radius = 0.3
                })

        assertScreenshot(posterize = 5)
    }


    open class CircleSDFView(width: Double = 100.0, height: Double = 100.0, var time: Double = 0.0) : ShadedView(PROGRAM, width, height) {
        var radius = 0.49
        var feather = 0.005
        var center = MPoint(0.5, 0.5)

        override fun renderInternal(ctx: RenderContext) {
            this.programUniforms[u_Center] = center
            this.programUniforms[u_Radius] = radius
            this.programUniforms[u_Feather] = feather
            this.programUniforms[u_Time] = sind(time.radians).absoluteValue

            super.renderInternal(ctx)
        }

        companion object {
            val u_Center by Uniform(VarType.Float2)
            val u_Radius by Uniform(VarType.Float1)
            val u_Feather by Uniform(VarType.Float1)
            val u_Time by Uniform(VarType.Float1)
            val PROGRAM = buildShader {
                val d = t_Temp0.x
                val SDF = SDFShaders

                SET(d,
                    SDF.opInterpolate(
                        SDF.circle(v_Tex - u_Center, u_Radius),
                        SDF.opBorder(SDF.box(v_Tex - u_Center + vec2(.1f.lit, 0f.lit), vec2(u_Radius * .4.lit, u_Radius * .4.lit)), .02f.lit),
                        clamp(u_Time, 0f.lit, 1f.lit)
                    )
                )

                //SET(alpha, SDF.computeAAAlphaFromDist(d))
                SET(out, v_Col * SDF.opAA(d))
            }
        }
    }


}
