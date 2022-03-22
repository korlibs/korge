package com.soywiz.korge.view.filter

import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.view.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import kotlin.math.*

/**
 * A filter that simulates a page of a book.
 */
class PageFilter(
    hratio: Double = 0.5,
    hamplitude0: Double = 0.0,
    hamplitude1: Double = 10.0,
    hamplitude2: Double = 0.0,

    vratio: Double = 0.5,
    vamplitude0: Double = 0.0,
    vamplitude1: Double = 0.0,
    vamplitude2: Double = 0.0
) : ShaderFilter() {
    companion object {
        val u_Offset = Uniform("u_Offset", VarType.Float2)
        val u_HAmplitude = Uniform("u_HAmplitude", VarType.Float3)
        val u_VAmplitude = Uniform("u_VAmplitude", VarType.Float3)

        private fun Program.Builder.sin01(arg: Operand) = sin(arg * (PI.lit * 0.5.lit))
        private val FRAGMENT_SHADER = FragmentShader {
            val x01 = DefaultShaders.t_Temp0["zw"]
            SET(x01, v_Tex01)
            for (n in 0..1) {
                val vr = x01[n]
                val offset = u_Offset[n]
                val amplitudes = if (n == 0) u_HAmplitude else u_VAmplitude
                val tmp = DefaultShaders.t_Temp0[n]
                IF(vr lt offset) {
                    val ratio = ((vr - 0.0.lit) / offset)
                    SET(tmp, mix(amplitudes[0], amplitudes[1], sin01(ratio)))
                } ELSE {
                    val ratio = 1.0.lit + ((vr - offset) / (1.0.lit - offset))
                    SET(tmp, mix(amplitudes[2], amplitudes[1], sin01(ratio)))
                }
            }

            SET(out, tex(fragmentCoords + DefaultShaders.t_Temp0["yx"]))
        }
    }

    private val offset = uniforms.storageFor(u_Offset)
    private val hamplitude = scaledUniforms.storageFor(u_HAmplitude)
    private val vamplitude = scaledUniforms.storageFor(u_VAmplitude)

    var hratio by offset.doubleDelegateX(default = hratio)
    var hamplitude0 by hamplitude.doubleDelegate(0, default = hamplitude0)
    var hamplitude1 by hamplitude.doubleDelegate(1, default = hamplitude1)
    var hamplitude2 by hamplitude.doubleDelegate(2, default = hamplitude2)

    var vratio by offset.doubleDelegateY(default = vratio)
    var vamplitude0 by vamplitude.doubleDelegate(0, default = vamplitude0)
    var vamplitude1 by vamplitude.doubleDelegate(1, default = vamplitude1)
    var vamplitude2 by vamplitude.doubleDelegate(2, default = vamplitude2)

    override val fragment = FRAGMENT_SHADER

    override fun computeBorder(out: MutableMarginInt, texWidth: Int, texHeight: Int) {
        out.setTo(max(max(abs(hamplitude0), abs(hamplitude1)), abs(hamplitude2)).toIntCeil())
    }

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiEditableValue(::hratio)
        container.uiEditableValue(::hamplitude0)
        container.uiEditableValue(::hamplitude1)
        container.uiEditableValue(::hamplitude2)
        container.uiEditableValue(::vratio)
        container.uiEditableValue(::vamplitude0)
        container.uiEditableValue(::vamplitude1)
        container.uiEditableValue(::vamplitude2)
    }
}
