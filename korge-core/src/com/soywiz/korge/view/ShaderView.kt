package com.soywiz.korge.view

import com.soywiz.korge.render.RenderContext
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.Rectangle

//class ShaderView(views: Views, val shader: Program) : Container(views) {
class ShaderView(views: Views) : Container(views) {
	override fun render(ctx: RenderContext, m: Matrix2d) {
		val out = Rectangle()
		this.getLocalBounds(out)
		val m2 = Matrix2d()
		ctx.flush()
		val tex = ctx.renderToTexture(out.width.toInt(), out.height.toInt()) {
			super.render(ctx, m2)
		}
		ctx.flush()
		ctx.batch.addQuad(tex, 0f, 0f, tex.width.toFloat() * 2f, tex.height.toFloat() * 2f, m, filtering = false)
		ctx.flush()
	}
}
