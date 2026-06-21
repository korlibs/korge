package korlibs.korge.view

import korlibs.korge.render.*
import korlibs.math.geom.*

inline fun Container.scaleView(
    size: Size, scaleAvg: Double = 2.0, filtering: Boolean = false,
    callback: @ViewDslMarker Container.() -> Unit = {}
) = ScaleView(size, scaleAvg, filtering).addTo(this, callback)

class ScaleView(
    size: Size, scaleAvg: Double = 2.0,
    var filtering: Boolean = false
) : SContainer(size, clip = false), View.Reference {
	init {
		this.scaleAvg = scaleAvg
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
                    filtering = filtering,
                    blendMode = renderBlendMode,
                )
            }
		})
	}
}
