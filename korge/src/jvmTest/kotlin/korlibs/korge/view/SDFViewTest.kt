package korlibs.korge.view

import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.testing.*
import korlibs.image.color.*
import korlibs.math.geom.*
import org.junit.*
import kotlin.math.*

class SDFViewTest {
    @Test
    fun test() = korgeScreenshotTest(SizeInt(200, 200)) {
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
        var center = Point(0.5, 0.5)

        override fun renderInternal(ctx: RenderContext) {
            ctx[SDFUB].push {
                it[u_Center] = center
                it[u_Radius] = radius
                it[u_Feather] = feather
                it[u_Time] = sind(time.radians).absoluteValue
            }

            super.renderInternal(ctx)
        }

        object SDFUB : UniformBlock(fixedLocation = 6) {
            val u_Center by vec2()
            val u_Radius by float()
            val u_Feather by float()
            val u_Time by float()
        }

        companion object {
            val PROGRAM = buildShader {
                val d = t_Temp0.x
                val SDF = SDFShaders

                SET(d,
                    SDF.opInterpolate(
                        SDF.circle(v_Tex - SDFUB.u_Center, SDFUB.u_Radius),
                        SDF.opBorder(SDF.box(v_Tex - SDFUB.u_Center + vec2(.1f.lit, 0f.lit), vec2(SDFUB.u_Radius * .4.lit, SDFUB.u_Radius * .4.lit)), .02f.lit),
                        clamp(SDFUB.u_Time, 0f.lit, 1f.lit)
                    )
                )

                //SET(alpha, SDF.computeAAAlphaFromDist(d))
                SET(out, v_Col * SDF.opAA(d))
            }
        }
    }


}