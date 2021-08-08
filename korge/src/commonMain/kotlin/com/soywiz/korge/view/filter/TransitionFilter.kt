package com.soywiz.korge.view.filter

import com.soywiz.korag.AG
import com.soywiz.korag.DefaultShaders.t_Temp1
import com.soywiz.korag.DefaultShaders.v_Tex
import com.soywiz.korag.shader.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.color.*
import com.soywiz.korim.paint.GradientPaint
import com.soywiz.korim.paint.LinearGradientPaint
import com.soywiz.korim.paint.RadialGradientPaint
import com.soywiz.korim.paint.SweepGradientPaint
import com.soywiz.korma.geom.vector.rect
import com.soywiz.korui.*

class TransitionFilter(
    var transition: Transition = Transition.CIRCULAR,
    reversed: Boolean = false,
    smooth: Boolean = true,
    ratio: Double = 1.0,
    filtering: Boolean = false,
) : ShaderFilter() {
    class Transition(val bmp: Bitmap) {
        fun inverted() = bmp.toBMP32().also { it.invert() }

        companion object {
            private val BMP_SIZE = 64

            private fun createTransitionBox(paint: GradientPaint): Transition {
                return Transition(Bitmap32(BMP_SIZE, BMP_SIZE).context2d {
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

    companion object {
        private val u_Reversed = Uniform("u_Reversed", VarType.Float1)
        private val u_Smooth = Uniform("u_Smooth", VarType.Float1)
        private val u_Ratio = Uniform("u_Ratio", VarType.Float1)
        private val u_Mask = Uniform("u_Mask", VarType.TextureUnit)
        private val FRAGMENT_SHADER = Filter.DEFAULT_FRAGMENT.appending {
            t_Temp1.x setTo texture2D(u_Mask, v_Tex["xy"]).r
            IF(u_Reversed eq 1f.lit) {
                t_Temp1.x setTo 1f.lit - t_Temp1.x
            }
            t_Temp1.x setTo clamp(t_Temp1.x + ((u_Ratio * 2f.lit) - 1f.lit), 0f.lit, 1f.lit)
            IF(u_Smooth ne 1f.lit) {
                IF(t_Temp1.x ge 1f.lit) {
                    t_Temp1.x setTo 1f.lit
                } ELSE {
                    t_Temp1.x setTo 0f.lit
                }
            }
            out setTo (out * t_Temp1.x)
            //out setTo texture2D(u_Mask, v_Tex["xy"])
            //out setTo vec4(1.lit, 1.lit, 1.lit, 1.lit)
        }
    }

    init {
        this.filtering = filtering
    }

    override val fragment: FragmentShader = FRAGMENT_SHADER

    private val textureUnit = AG.TextureUnit()
    private val s_ratio = uniforms.storageFor(u_Ratio)
    private val s_tex = uniforms.storageForTextureUnit(u_Mask, textureUnit)
    var reversed by uniforms.storageFor(u_Reversed).boolDelegateX(reversed)
    var smooth by uniforms.storageFor(u_Smooth).boolDelegateX(smooth)
    var ratio by s_ratio.doubleDelegateX(ratio)

    override fun updateUniforms(ctx: RenderContext) {
        textureUnit.texture = ctx.getTex(transition.bmp).base
    }

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiEditableValue(::ratio)
        container.uiEditableValue(::smooth)
        container.uiEditableValue(::reversed)
        //container.uiEditableValue(::bitmap)
    }
}
