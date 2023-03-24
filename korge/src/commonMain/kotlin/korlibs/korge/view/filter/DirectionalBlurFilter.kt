package korlibs.korge.view.filter

import korlibs.memory.*
import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.view.property.*
import korlibs.math.geom.*
import kotlin.math.*

// https://en.wikipedia.org/wiki/Gaussian_blur
class DirectionalBlurFilter(
    @ViewProperty
    var angle: Angle = 0.degrees,
    @ViewProperty
    var radius: Double = 4.0,
    @ViewProperty
    var expandBorder: Boolean = true
) : ShaderFilter() {
    object BlurUB : UniformBlock(fixedLocation = 5) {
        val u_radius by float()
        val u_constant1 by float()
        val u_constant2 by float()
        val u_direction by vec2()
    }

    companion object : BaseProgramProvider() {
        override val fragment = FragmentShaderDefault {
            val loopLen = createTemp(Int1)
            val gaussianResult = createTemp(Float1)
            IF (BlurUB.u_radius lt 1f.lit) {
                SET(out, texture2D(u_Tex, fragmentCoords01))
            } ELSE {
            //run {
                SET(out, vec4(0f.lit, 0f.lit, 0f.lit, 0f.lit))
                SET(loopLen, int(ceil(BlurUB.u_radius)))
                //FOR_0_UNTIL_FIXED_BREAK(loopLen / 2.lit, maxLen = 256) { x ->
                FOR_0_UNTIL_FIXED_BREAK(loopLen, maxLen = 256) { x ->
                    val xfloat = createTemp(Float1)
                    SET(xfloat, float(x))
                    SET(gaussianResult, BlurUB.u_constant1 * exp((-xfloat * xfloat) * BlurUB.u_constant2))
                    val addTemp = createTemp(Float2)
                    SET(addTemp, (BlurUB.u_direction * xfloat) * TexInfoUB.u_StdTexDerivates)
                    //SET(addTemp, (u_direction * xfloat) * u_StdTexDerivates * 2f.lit + (u_StdTexDerivates * .5f.lit))
                    SET(out, out + (texture2DZeroOutside(u_Tex, fragmentCoords01 + addTemp, check = !VIEW_FILTER_TRANSPARENT_EDGE) * gaussianResult))
                    IF(x ne 0.lit) {
                        SET(out, out + (texture2DZeroOutside(u_Tex, fragmentCoords01 - addTemp, check = !VIEW_FILTER_TRANSPARENT_EDGE) * gaussianResult))
                    }
                }

            }
            //BatchBuilder2D.DO_INPUT_OUTPUT(this, out)
            //SET(out["ba"], vec2(1f.lit, 1f.lit))
            //SET(out["a"], 1f.lit)
        }.also {
            //println(it.toNewGlslString(GlslConfig()))
        }
    }

    private val qfactor: Double = sqrt(2 * ln(255.0))

    //private val rradius: Double get() = (radius * ln(radius).coerceAtLeast(1.0)).coerceAtLeast(0.0)
    private val rradius: Double get() = (radius * qfactor)

    // @TODO: Here we cannot do this, but we should be able to do this trick: https://www.rastergrid.com/blog/2010/09/efficient-gaussian-blur-with-linear-sampling/
    //override val recommendedFilterScale: Double get() = if (rradius <= 2.0) 1.0 else 1.0 / log2(rradius.coerceAtLeast(1.0))

    override fun computeBorder(texWidth: Int, texHeight: Int): MarginInt {
        if (!expandBorder) return MarginInt.ZERO
        val radius = this.rradius
        return MarginInt(
            (angle.sineD.absoluteValue * radius).toIntCeil(),//.coerceAtMost(texWidth),
            (angle.cosineD.absoluteValue * radius).toIntCeil(),//.coerceAtMost(texHeight),
        )
    }

    private fun gaussian(x: Double, constant1: Double, constant2: Double): Double = constant1 * exp((-x * x) * constant2)

    override fun updateUniforms(ctx: RenderContext, filterScale: Double) {
        val radius = this.rradius * filterScale
        //println("rradius=$rradius")
        //val sigma = max(radius / 3.0, 0.9)
        val sigma = (radius + 1) / qfactor
        //val sigma = 128.0
        //println("radius=$radius, sigma=$sigma")
        val constant1 = 1.0 / (sigma * sqrt(2.0 * PI))
        val constant2 = 1.0 / (2.0 * sigma * sigma)

        var scaleSum = 0.0
        if (radius.isFinite()) {
            for (n in 0 until radius.toIntCeil()) {
                val gauss = gaussian(n.toDouble(), constant1, constant2)
                scaleSum += if (n != 0) gauss * 2 else gauss
            }
        }

        //println("RADIUS: $radius")
        ctx[BlurUB].push {
            it[u_radius] = radius
            it[u_constant1] = constant1 * (1.0 / scaleSum)
            it[u_constant2] = constant2
            it[u_direction] = Point(angle.cosineF, angle.sineF)
        }
    }

    override val programProvider: ProgramProvider get() = DirectionalBlurFilter

    override val isIdentity: Boolean get() = radius == 0.0
}