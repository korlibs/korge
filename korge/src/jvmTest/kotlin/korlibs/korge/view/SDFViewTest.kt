package korlibs.korge.view

import korlibs.graphics.shader.*
import korlibs.image.color.*
import korlibs.korge.render.*
import korlibs.korge.testing.*
import korlibs.math.geom.*
import org.junit.*
import kotlin.math.*

class SDFViewTest {
    @Test
    fun test() = korgeScreenshotTest(Size(200, 200)) {
        addChild(
            CircleSDFView(size = Size(200f, 200f), time = 2f)
                .skew(15.degrees, 0.degrees)
                .also {
                    it.colorMul = Colors.DARKGREY
                    //it.radius = 0.3
                })

        assertScreenshot(posterize = 5)
    }

    open class CircleSDFView(size: Size = Size(100f, 100f), var time: Float = 0f) : ShadedView(PROGRAM, size) {
        var radius = 0.49f
        var feather = 0.005f
        var center = Point(0.5, 0.5)

        override fun renderInternal(ctx: RenderContext) {
            ctx[SDFUB].push {
                it[u_Center] = center
                it[u_Radius] = radius
                it[u_Feather] = feather
                it[u_Time] = sin(time.radians).absoluteValue
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
                        SDF.opBorder(SDF.box(v_Tex - SDFUB.u_Center + vec2(.1f.lit, 0f.lit), vec2(SDFUB.u_Radius * .4f.lit, SDFUB.u_Radius * .4f.lit)), .02f.lit),
                        clamp(SDFUB.u_Time, 0f.lit, 1f.lit)
                    )
                )

                //SET(alpha, SDF.computeAAAlphaFromDist(d))
                SET(out, v_Col * SDF.opAA(d))
            }
        }
    }


}
