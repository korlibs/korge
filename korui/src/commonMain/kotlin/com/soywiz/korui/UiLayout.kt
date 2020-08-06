package com.soywiz.korui

import com.soywiz.korma.geom.*

interface UiLayout {
    fun relayout(container: UiContainer)
}

object UiFillLayout : UiLayout {
    override fun relayout(container: UiContainer) {
        val bounds = container.bounds
        //container.bounds = bounds
        container.forEachChild { child ->
            child.bounds = RectangleInt(0, 0, bounds.width, bounds.height)
        }
    }
}
