package com.soywiz.korge.view.filter

import com.soywiz.korag.AG
import com.soywiz.korag.DefaultShaders.t_Temp1
import com.soywiz.korag.DefaultShaders.v_Tex
import com.soywiz.korag.shader.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.Filter
import com.soywiz.korge.view.filter.ShaderFilter
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.paint.GradientPaint
import com.soywiz.korim.vector.paint.LinearGradientPaint
import com.soywiz.korim.vector.paint.RadialGradientPaint
import com.soywiz.korim.vector.paint.SweepGradientPaint
import com.soywiz.korma.geom.vector.rect
import com.soywiz.korui.*

class TransitionFilter(
    var transition: Transition = Transition.VERTICAL,
    reversed: Boolean = false,
    discrete: Boolean = false,
) : ShaderFilter() {
    class Transition(val bmp: Bitmap) {
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
        private val u_Reversed = Uniform("reversed", VarType.Float1)
        private val u_Discrete = Uniform("discrete", VarType.Float1)
        private val u_Time = Uniform("time", VarType.Float1)
        private val u_Mask = Uniform("mask", VarType.TextureUnit)
        private val FRAGMENT_SHADER = Filter.DEFAULT_FRAGMENT.appending {
            t_Temp1.x setTo texture2D(u_Mask, v_Tex["xy"]).r
            IF(u_Reversed.x eq 1f.lit) {
                t_Temp1.x setTo 1f.lit - t_Temp1.x
            }
            t_Temp1.x setTo clamp(t_Temp1.x + ((u_Time.x * 2f.lit) - 1f.lit), 0f.lit, 1f.lit)
            IF(u_Discrete.x eq 1f.lit) {
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

    override val fragment: FragmentShader = FRAGMENT_SHADER

    private val textureUnit = AG.TextureUnit()
    private val s_time = uniforms.storageFor(u_Time)
    private val s_tex = uniforms.storageForTextureUnit(u_Mask, textureUnit)
    var reversed by uniforms.storageFor(u_Reversed).boolDelegateX(reversed)
    var discrete by uniforms.storageFor(u_Discrete).boolDelegateX(discrete)
    var time by s_time.doubleDelegateX()

    override fun updateUniforms(ctx: RenderContext) {
        textureUnit.texture = ctx.getTex(transition.bmp).base
    }

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiEditableValue(::time)
        //container.uiEditableValue(::bitmap)
    }
}
