package com.soywiz.korge.view.filter

import com.soywiz.korag.shader.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korui.*

/**
 * A [Filter] applying a multiplicative and additive color transformation to the view.
 */
class ColorTransformFilter(colorTransform: ColorTransform) : ShaderFilter() {
	companion object {
		private val u_ColorMul = Uniform("u_colorMul", VarType.Float4)
		private val u_ColorAdd = Uniform("u_colorAdd", VarType.Float4)

        private val FRAGMENT_SHADER = FragmentShader {
            apply {
                out setTo tex(fragmentCoords)
                out setTo ((out * u_ColorMul) + u_ColorAdd)
                //out setTo (tex(fragmentCoords) + u_ColorAdd)
                //out setTo vec4(1f.lit, 1f.lit, 1f.lit, 1f.lit)
            }
        }
	}

	private val colorMulStorage = uniforms.storageFor(u_ColorMul)
    private val colorAddStorage = uniforms.storageFor(u_ColorAdd)

    var colorMul: RGBA
        get() = RGBA.float(colorMulStorage.array)
        set(value) { value.readFloat(colorMulStorage.array) }

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

	override val fragment = FRAGMENT_SHADER

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiEditableValue(listOf(colorMulStorage::x, colorMulStorage::y, colorMulStorage::z, colorMulStorage::w), name = "mul")
        container.uiEditableValue(listOf(colorAddStorage::x, colorAddStorage::y, colorAddStorage::z, colorAddStorage::w), name = "add")
    }
}
