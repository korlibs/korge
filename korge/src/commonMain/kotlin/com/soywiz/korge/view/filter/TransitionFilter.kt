package com.soywiz.korge.view.filter

import com.soywiz.korag.*
import com.soywiz.korag.DefaultShaders.t_Temp1
import com.soywiz.korag.shader.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.property.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.paint.*
import com.soywiz.korma.geom.vector.*

class TransitionFilter(
    var transition: Transition = Transition.CIRCULAR,
    reversed: Boolean = false,
    spread: Double = 1.0,
    ratio: Double = 1.0,
    filtering: Boolean = false,
) : ShaderFilter() {
    class Transition(val bmp: Bitmap) {
        fun inverted() = bmp.toBMP32().also { it.invert() }

        companion object {
            private val BMP_SIZE = 64

            private fun createTransitionBox(paint: GradientPaint): Transition {
                return Transition(Bitmap32Context2d(BMP_SIZE, BMP_SIZE) {
                    fill(paint.add(0.0, Colors.WHITE).add(1.0, Colors.BLACK)) {
                        rect(0, 0, BMP_SIZE, BMP_SIZE)
                    }
                })
            }
            private fun createLinearTransitionBox(x0: Int, y0: Int, x1: Int, y1: Int): Transition =
                createTransitionBox(LinearGradientPaint(x0, y0, x1, y1))

            val VERTICAL by lazy { createLinearTransitionBox(0, 0, 0, BMP_SIZE) }
            val HORIZONTAL by lazy { createLinearTransitionBox(0, 0, BMP_SIZE, 0) }
            val DIAGONAL1 by lazy { createLinearTransitionBox(0, 0, BMP_SIZE, BMP_SIZE) }
            val DIAGONAL2 by lazy { createLinearTransitionBox(BMP_SIZE, 0, 0, BMP_SIZE) }
            val CIRCULAR by lazy { createTransitionBox(RadialGradientPaint(BMP_SIZE / 2, BMP_SIZE / 2, 0, BMP_SIZE / 2, BMP_SIZE / 2, BMP_SIZE / 2)) }
            val SWEEP by lazy { createTransitionBox(SweepGradientPaint(BMP_SIZE / 2, BMP_SIZE / 2)) }
        }
    }

    companion object : BaseProgramProvider() {
        private val u_Reversed = Uniform("u_Reversed", VarType.Float1)
        private val u_Spread = Uniform("u_Smooth", VarType.Float1)
        private val u_Ratio = Uniform("u_Ratio", VarType.Float1)
        private val u_Mask = Uniform("u_Mask", VarType.Sampler2D)

        override val fragment = Filter.DEFAULT_FRAGMENT.appending {
            val alpha = t_Temp1.x
            val spread = t_Temp1.y

            SET(alpha, texture2D(u_Mask, v_Tex01).r)
            IF(u_Reversed eq 1f.lit) {
                SET(alpha, 1f.lit - alpha)
            }
            SET(alpha, clamp(alpha + ((u_Ratio * 2f.lit) - 1f.lit), 0f.lit, 1f.lit))
            SET(spread, clamp(u_Spread, 0.01f.lit, 1f.lit) * 0.5f.lit)
            SET(alpha, smoothstep(clamp01(u_Ratio - spread), clamp01(u_Ratio + spread), alpha))

            SET(out, (out * alpha))
            //BatchBuilder2D.DO_INPUT_OUTPUT(this, out)
            //SET(out, texture2D(u_Mask, v_Tex01))
            //SET(out, vec4(1.lit, 0.lit, 1.lit, 1.lit))
        }
    }

    init {
        this.filtering = filtering
    }

    override val programProvider: ProgramProvider get() = TransitionFilter
    private val textureUnit = AG.TextureUnit()
    private val s_ratio = uniforms.storageFor(u_Ratio)
    private val s_tex = uniforms.storageForTextureUnit(u_Mask, textureUnit)
    @ViewProperty
    var reversed: Boolean by uniforms.storageFor(u_Reversed).boolDelegateX(reversed)
    @ViewProperty
    var spread: Double by uniforms.storageFor(u_Spread).doubleDelegateX(spread)
    @ViewProperty
    var ratio: Double by s_ratio.doubleDelegateX(ratio)

    override fun updateUniforms(ctx: RenderContext, filterScale: Double) {
        textureUnit.texture = ctx.getTex(transition.bmp).base
    }
}
