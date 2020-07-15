package com.soywiz.korge.view

import com.soywiz.korge.render.*

fun Container.dummyView() = DummyView().also { this.addChild(it) }

/**
 * A Dummy view that doesn't render anything.
 */
open class DummyView : View() {
    override fun createInstance(): View = DummyView()
    override fun renderInternal(ctx: RenderContext) = Unit
}
