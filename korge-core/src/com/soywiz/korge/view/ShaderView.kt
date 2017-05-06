package com.soywiz.korge.view

import com.soywiz.korge.render.RenderContext
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.Rectangle

//class ShaderView(views: Views, val shader: Program) : Container(views) {
open class ShaderView(views: Views) : Container(views) {
	override fun render(ctx: RenderContext, m: Matrix2d) {
		if (!visible) return
		val out = Rectangle()
		this.getBounds(this, out)
		val m2 = Matrix2d()
		//m2.translate(-out.x, +out.y)
		//m2.scale(1.0, -1.0)
		ctx.flush()
		//println(out)
		val tex = ctx.renderToTexture(out.width.toInt(), out.height.toInt()) {
			super.render(ctx, m2)
		}
		ctx.flush()
		//ctx.batch.addQuad(tex, out.x.toFloat(), out.y.toFloat(), tex.width.toFloat() * 2f, tex.height.toFloat() * 2f, m, filtering = false)
		ctx.batch.drawQuad(tex, 0f, 0f, tex.width.toFloat() * 2f, tex.height.toFloat() * 2f, m, filtering = false)
		ctx.flush()
	}
}
