package com.soywiz.korge.view

import com.soywiz.korge.render.RenderContext

inline fun Container.scaleView(
	width: Int, height: Int, scale: Double = 2.0, filtering: Boolean = false,
	callback: @ViewDslMarker Container.() -> Unit = {}
) = ScaleView(width, height, scale, filtering).addTo(this, callback)

class ScaleView(width: Int, height: Int, scale: Double = 2.0, var filtering: Boolean = false) :
    SContainer(width.toDouble(), height.toDouble(), clip = false), View.Reference {
	init {
		this.scale = scale
	}

	//val once = Once()

	override fun renderInternal(ctx: RenderContext) {
		val iwidth = width.toInt()
		val iheight = height.toInt()

		ctx.renderToTexture(iwidth, iheight, render = {
			super.renderInternal(ctx)
		}, use = { renderTexture ->
            ctx.useBatcher { batch ->
                batch.drawQuad(
                    tex = renderTexture,
                    x = 0f, y = 0f,
                    width = iwidth.toFloat(),
                    height = iheight.toFloat(),
                    m = globalMatrix,
                    colorMul = renderColorMul,
                    colorAdd = renderColorAdd,
                    filtering = filtering,
                    blendMode = renderBlendMode
                )
            }
		})
	}
}
