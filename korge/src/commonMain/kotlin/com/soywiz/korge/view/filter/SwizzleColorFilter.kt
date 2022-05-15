package com.soywiz.korge.view.filter

import com.soywiz.korag.shader.FragmentShader
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
class SwizzleColorsFilter(initialSwizzle: String = "rgba") : Filter {
    private var proxy: ProxySwizzle = ProxySwizzle(initialSwizzle)

    class ProxySwizzle(val swizzle: String = "rgba") : ShaderFilter() {
        override val fragment: FragmentShader = Filter.DEFAULT_FRAGMENT.appending { out setTo out[swizzle] }
    }

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
            proxy = ProxySwizzle(value)
        }

    override fun render(
        ctx: RenderContext,
        matrix: Matrix,
        texture: Texture,
        texWidth: Int,
        texHeight: Int,
        renderColorAdd: ColorAdd,
        renderColorMul: RGBA,
        blendMode: BlendMode,
        filterScale: Double,
    ) = proxy.render(ctx, matrix, texture, texWidth, texHeight, renderColorAdd, renderColorMul, blendMode, filterScale)

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiEditableValue(::swizzle)
    }
}
