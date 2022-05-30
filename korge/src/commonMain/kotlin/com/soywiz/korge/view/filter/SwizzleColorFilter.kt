package com.soywiz.korge.view.filter

import com.soywiz.kds.CopyOnWriteFrozenMap
import com.soywiz.korag.shader.FragmentShader
import com.soywiz.korag.shader.Program
import com.soywiz.korag.shader.Shader
import com.soywiz.korag.shader.appending
import com.soywiz.korge.debug.uiEditableValue
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture
import com.soywiz.korge.view.BlendMode
import com.soywiz.korge.view.Views
import com.soywiz.korim.color.ColorAdd
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.Matrix
import com.soywiz.korui.UiContainer

/**
 * Allows to swizzle (interchange) color components via the [swizzle] property.
 *
 * - swizzle="rgba" would be the identity
 * - swizzle="bgra" would interchange red and blue channels
 * - swizzle="rrra" would show as greyscale the red component
 */
class SwizzleColorsFilter(initialSwizzle: String = "rgba") : ShaderFilter() {
    companion object {
        class SwizzleProgram(val swizzle: String) : BaseProgramProvider() {
            override val fragment = Filter.DEFAULT_FRAGMENT.appending { SET(out, out[swizzle]) }
        }

        private val CACHE = CopyOnWriteFrozenMap<String, SwizzleProgram>()
    }

    var _programProvider: ProgramProvider? = null

    /**
     * The swizzling string
     *
     * - swizzle="rgba" would be the identity
     * - swizzle="bgra" would interchange red and blue channels
     * - swizzle="rrra" would show as greyscale the red component
     * */
    var swizzle: String = initialSwizzle
        set(value) {
            field = value
            _programProvider = null
        }

    override val programProvider: ProgramProvider get() {
        if (_programProvider == null) {
            _programProvider = CACHE.getOrPut(swizzle) { SwizzleProgram(swizzle) }
        }
        return _programProvider!!
    }

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiEditableValue(::swizzle)
    }
}
