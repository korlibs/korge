package com.soywiz.korge.view.filter

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.view.property.*
import com.soywiz.korim.color.*

/**
 * A [Filter] applying a multiplicative and additive color transformation to the view.
 */
class ColorTransformFilter(colorTransform: ColorTransform) : ShaderFilter() {
	companion object : BaseProgramProvider() {
		private val u_ColorMul = Uniform("u_colorMul", VarType.Float4)
		private val u_ColorAdd = Uniform("u_colorAdd", VarType.Float4)

        override val fragment = FragmentShaderDefault {
            SET(out, tex(fragmentCoords))
            SET(out, ((out * u_ColorMul) + u_ColorAdd))
            //BatchBuilder2D.DO_INPUT_OUTPUT(this, out)
            //out setTo (tex(fragmentCoords) + u_ColorAdd)
            //out setTo vec4(1f.lit, 1f.lit, 1f.lit, 1f.lit)
        }
	}

	private val colorMulStorage = uniforms.storageFor(u_ColorMul)
    private val colorAddStorage = uniforms.storageFor(u_ColorAdd)

    @ViewProperty
    var colorMul: RGBA
        get() = RGBA.float(colorMulStorage.array)
        set(value) { value.readFloat(colorMulStorage.array) }

    @ViewProperty
    var colorAdd: ColorAdd
        get() = ColorAdd.fromFloat(colorAddStorage.array)
        set(value) { value.readFloat(colorAddStorage.array) }

    var colorTransform: ColorTransform
        get() = ColorTransform(colorMul, colorAdd)
        set(value) {
            colorMul = value.colorMul
            colorAdd = value.colorAdd
        }

    init {
        colorMul = colorTransform.colorMul
        colorAdd = colorTransform.colorAdd
    }

    override val programProvider: ProgramProvider get() = ColorTransformFilter
}
