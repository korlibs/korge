package com.soywiz.korge.view.filter

import com.soywiz.kmem.toIntCeil
import com.soywiz.korag.FragmentShaderDefault
import com.soywiz.korag.shader.Uniform
import com.soywiz.korag.shader.VarType
import com.soywiz.korge.debug.uiEditableValue
import com.soywiz.korge.render.BatchBuilder2D
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.Views
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.MutableMarginInt
import com.soywiz.korma.geom.Vector3D
import com.soywiz.korma.geom.cosine
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.sine
import com.soywiz.korui.UiContainer
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

// https://en.wikipedia.org/wiki/Gaussian_blur
class DirectionalBlurFilter(var angle: Angle = 0.degrees, var radius: Double = 4.0, var expandBorder: Boolean = true) : ShaderFilter() {
    companion object : BaseProgramProvider() {
        private val u_radius = Uniform("u_radius", VarType.Float1)
        private val u_constant1 = Uniform("u_constant1", VarType.Float1)
        private val u_constant2 = Uniform("u_constant2", VarType.Float1)
        private val u_direction = Uniform("u_direction", VarType.Float2)

        override val fragment = FragmentShaderDefault {
            val loopLen = createTemp(Int1)
            val gaussianResult = createTemp(Float1)
            IF (u_radius lt 1f.lit) {
                SET(out, texture2D(u_Tex, fragmentCoords01))
            } ELSE {
            //run {
                SET(out, vec4(0f.lit, 0f.lit, 0f.lit, 0f.lit))
                SET(loopLen, int(ceil(u_radius)))
                //FOR_0_UNTIL_FIXED_BREAK(loopLen / 2.lit, maxLen = 256) { x ->
                FOR_0_UNTIL_FIXED_BREAK(loopLen, maxLen = 256) { x ->
                    val xfloat = createTemp(Float1)
                    SET(xfloat, float(x))
                    SET(gaussianResult, u_constant1 * exp((-xfloat * xfloat) * u_constant2))
                    val addTemp = createTemp(Float2)
                    SET(addTemp, (u_direction * xfloat) * u_StdTexDerivates)
                    //SET(addTemp, (u_direction * xfloat) * u_StdTexDerivates * 2f.lit + (u_StdTexDerivates * .5f.lit))
                    SET(out, out + (texture2D(u_Tex, fragmentCoords01 + addTemp) * gaussianResult))
                    IF(x ne 0.lit) {
                        SET(out, out + (texture2D(u_Tex, fragmentCoords01 - addTemp) * gaussianResult))
                    }
                }

            }
            BatchBuilder2D.DO_INPUT_OUTPUT(this, out)
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

    override fun computeBorder(out: MutableMarginInt, texWidth: Int, texHeight: Int) {
        if (!expandBorder) return out.setTo(0)
        val radius = this.rradius
        out.setTo(
            (angle.sine.absoluteValue * radius).toIntCeil(),//.coerceAtMost(texWidth),
            (angle.cosine.absoluteValue * radius).toIntCeil(),//.coerceAtMost(texHeight),
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
        uniforms[u_radius] = radius
        uniforms[u_constant1] = constant1 * (1.0 / scaleSum)
        uniforms[u_constant2] = constant2
        uniforms[u_direction] = Vector3D(angle.cosine, angle.sine, 0.0)
    }

    override val programProvider: ProgramProvider get() = DirectionalBlurFilter

    override val isIdentity: Boolean get() = radius == 0.0

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiEditableValue(::angle)
        container.uiEditableValue(::radius)
    }
}
