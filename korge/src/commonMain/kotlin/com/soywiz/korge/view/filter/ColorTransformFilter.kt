package com.soywiz.korge.view.filter

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.property.*
import com.soywiz.korim.color.*

/**
 * A [Filter] applying a multiplicative and additive color transformation to the view.
 */
class ColorTransformFilter(colorTransform: ColorTransform) : ShaderFilter() {
    object ColorTransformUB : UniformBlock(fixedLocation = 5) {
        val u_ColorMul by vec4()
        val u_ColorAdd by vec4()
    }

	companion object : BaseProgramProvider() {
        override val fragment = FragmentShaderDefault {
            SET(out, tex(fragmentCoords))
            SET(out, ((out * ColorTransformUB.u_ColorMul) + ColorTransformUB.u_ColorAdd))
            //BatchBuilder2D.DO_INPUT_OUTPUT(this, out)
            //out setTo (tex(fragmentCoords) + u_ColorAdd)
            //out setTo vec4(1f.lit, 1f.lit, 1f.lit, 1f.lit)
        }
	}

    @ViewProperty
    var colorMul: RGBA = colorTransform.colorMul

    @ViewProperty
    var colorAdd: ColorAdd = colorTransform.colorAdd

    var colorTransform: ColorTransform
        get() = ColorTransform(colorMul, colorAdd)
        set(value) {
            colorMul = value.colorMul
            colorAdd = value.colorAdd
        }

    override val programProvider: ProgramProvider get() = ColorTransformFilter

    override fun updateUniforms(ctx: RenderContext, filterScale: Double) {
        super.updateUniforms(ctx, filterScale)
        ctx[ColorTransformUB].push {
            it[u_ColorMul] = colorMul
            it[u_ColorAdd] = colorAdd
        }
    }
}
