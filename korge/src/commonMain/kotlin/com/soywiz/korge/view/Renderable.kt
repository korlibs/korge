package com.soywiz.korge.view

import com.soywiz.korge.render.*

interface Renderable {
	fun render(ctx: RenderContext): Unit
}
