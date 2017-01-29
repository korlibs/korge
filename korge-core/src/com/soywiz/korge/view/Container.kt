package com.soywiz.korge.view

import com.soywiz.korge.render.RenderContext

open class Container(views: Views) : View(views) {
	val children = arrayListOf<View>()

	override fun invalidate() {
		if (validGlobal && validGlobalInv) return
		validGlobal = false
		validGlobalInv = false
		for (child in children) child.invalidate()
	}

	operator fun plusAssign(view: View) {
		view.parent?.children?.remove(view)
		children += view
		view.parent = this
	}

	override fun render(ctx: RenderContext) {
		for (child in children) child.render(ctx)
	}

	override fun hitTest(x: Double, y: Double): View? {
		for (child in children) return child.hitTest(x, y) ?: continue
		return null
	}
}
