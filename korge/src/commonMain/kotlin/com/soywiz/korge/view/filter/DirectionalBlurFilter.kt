package com.soywiz.korge.view.filter

import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import kotlin.math.*

// https://en.wikipedia.org/wiki/Gaussian_blur
class DirectionalBlurFilter(var angle: Angle = 0.degrees, var radius: Double = 4.0) : ShaderFilter() {
    companion object {
        private val u_radius = Uniform("u_radius", VarType.Float1)
        private val u_constant1 = Uniform("u_constant1", VarType.Float1)
        private val u_constant2 = Uniform("u_constant2", VarType.Float1)
        private val u_direction = Uniform("u_direction", VarType.Float2)

        val FRAGMENT = FragmentShader {
            val loopLen = createTemp(Int1)
            val gaussianResult = createTemp(Float1)
            IF (u_radius lt 1f.lit) {
                SET(out, tex(fragmentCoords))
            } ELSE {
            //run {
                SET(out, vec4(0f.lit, 0f.lit, 0f.lit, 0f.lit))
                SET(loopLen, int(ceil(u_radius)))
                FOR_0_UNTIL_FIXED_BREAK(loopLen, maxLen = 1024) { x ->
                    val xfloat = createTemp(Float1)
                    SET(xfloat, float(x))
                    SET(gaussianResult, u_constant1 * exp((-xfloat * xfloat) * u_constant2))
                    SET(out, out + (tex(fragmentCoords + (u_direction * xfloat)) * gaussianResult))
                    IF(x ne 0.lit) {
                        SET(out, out + (tex(fragmentCoords - (u_direction * xfloat)) * gaussianResult))
                    }
                }
            }
            //SET(out["ba"], vec2(1f.lit, 1f.lit))
            //SET(out["a"], 1f.lit)
        }.also {
            //println(it.toNewGlslString(GlslConfig()))
        }
    }

    private val qfactor: Double = sqrt(2 * ln(255.0))

    //private val rradius: Double get() = (radius * ln(radius).coerceAtLeast(1.0)).coerceAtLeast(0.0)
    private val rradius: Double get() = (radius * qfactor)

    override fun computeBorder(out: MutableMarginInt) {
        val radius = this.rradius
        out.setTo(
            (angle.sine.absoluteValue * radius).toIntCeil(),
            (angle.cosine.absoluteValue * radius).toIntCeil(),
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
        for (n in 0 until radius.toIntCeil()) {
            val gauss = gaussian(n.toDouble(), constant1, constant2)
            scaleSum += if (n != 0) gauss * 2 else gauss
        }

        uniforms[u_radius] = radius
        uniforms[u_constant1] = constant1 * (1.0 / scaleSum)
        uniforms[u_constant2] = constant2
        uniforms[u_direction] = Vector3D(angle.cosine, angle.sine, 0.0)
    }

    override val fragment: FragmentShader get() = FRAGMENT

    override val isIdentity: Boolean get() = radius == 0.0

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiEditableValue(::angle)
        container.uiEditableValue(::radius)
    }
}
