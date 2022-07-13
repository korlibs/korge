package com.soywiz.korge.view.filter

import com.soywiz.korag.FragmentShaderDefault
import com.soywiz.korag.shader.FragmentShader
import com.soywiz.korag.shader.Uniform
import com.soywiz.korag.shader.VarType
import com.soywiz.korag.shader.storageFor
import com.soywiz.korge.debug.uiEditableValue
import com.soywiz.korge.render.BatchBuilder2D
import com.soywiz.korge.view.Views
import com.soywiz.korim.color.ColorAdd
import com.soywiz.korim.color.ColorTransform
import com.soywiz.korim.color.RGBA
import com.soywiz.korui.UiContainer

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
            BatchBuilder2D.DO_INPUT_OUTPUT_PREMULTIPLIED(this, out)
            //out setTo (tex(fragmentCoords) + u_ColorAdd)
            //out setTo vec4(1f.lit, 1f.lit, 1f.lit, 1f.lit)
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

    override val programProvider: ProgramProvider get() = ColorTransformFilter

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiEditableValue(listOf(colorMulStorage::x, colorMulStorage::y, colorMulStorage::z, colorMulStorage::w), name = "mul")
        container.uiEditableValue(listOf(colorAddStorage::x, colorAddStorage::y, colorAddStorage::z, colorAddStorage::w), name = "add")
    }
}
