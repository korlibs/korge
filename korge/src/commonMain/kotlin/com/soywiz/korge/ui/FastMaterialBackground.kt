package com.soywiz.korge.ui

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*

inline fun Container.fastMaterialBackground(
    width: Double = UI_DEFAULT_WIDTH,
    height: Double = UI_DEFAULT_HEIGHT,
    block: @ViewDslMarker FastMaterialBackground.() -> Unit = {}
): FastMaterialBackground = FastMaterialBackground(width, height).addTo(this).apply(block)

class FastMaterialBackground(
    width: Double = 100.0,
    height: Double = 100.0
) : ShadedView(MaterialRender.PROGRAM, width, height, CoordsType.D_W_H) {
    var borderColor: RGBA = Colors.BLACK
    var borderSize: Double = 0.0; set(value) { field = value; invalidateRender() }
    var radius: RectCorners = RectCorners.EMPTY ; set(value) { field = value; invalidateRender() }
    var highlightPos = Point(0.5, 0.5) ; set(value) { field = value; invalidateRender() }
    var highlightRadius = 0.0; set(value) { field = value; invalidateRender() }
    var highlightAlpha = 1.0; set(value) { field = value; invalidateRender() }

    var shadowColor: RGBA = Colors.BLACK.withAd(0.3); set(value) { field = value; invalidateRender() }
    var shadowRadius: Double = 10.0; set(value) { field = value; invalidateRender() }
    var shadowOffsetX: Double = 0.0; set(value) { field = value; invalidateRender() }
    var shadowOffsetY: Double = 0.0; set(value) { field = value; invalidateRender() }

    override fun invalidateRender() {
        super.invalidateRender()
        padding = Margin(shadowRadius)
    }

    init {
        invalidateRender()
    }

    override fun updateUniforms(uniforms: AG.UniformValues, ctx: RenderContext) {
        //uniforms[u_Radius] = height / 2.0
        uniforms[MaterialRender.u_Radius] = radius
        uniforms[MaterialRender.u_Size] = Point(width, height)
        uniforms[MaterialRender.u_HighlightPos] = Point(highlightPos.x * width, highlightPos.y * height)
        uniforms[MaterialRender.u_HighlightRadius] = highlightRadius * kotlin.math.max(width, height) * 1.25
        uniforms[MaterialRender.u_HighlightAlpha] = highlightAlpha
        uniforms[MaterialRender.u_ShadowColor] = shadowColor.premultipliedFast
        uniforms[MaterialRender.u_ShadowOffset] = Point(shadowOffsetX, shadowOffsetY)
        uniforms[MaterialRender.u_ShadowRadius] = shadowRadius
    }
}
