package com.soywiz.korui

import com.soywiz.kds.*
import com.soywiz.korma.geom.*

interface UiLayout {
    fun computePreferredSize(container: UiContainer): SizeInt
    fun relayout(container: UiContainer)
}

var UiContainer.layoutChildrenPadding by Extra.Property { 0 }

object UiFillLayout : UiLayout {
    override fun computePreferredSize(container: UiContainer): SizeInt {
        return SizeInt(128, 128)
    }

    override fun relayout(container: UiContainer) {
        val bounds = container.bounds
        //container.bounds = bounds
        val padding = container.layoutChildrenPadding
        container.forEachChild { child ->
            child.bounds = RectangleInt.fromBounds(padding, padding, bounds.width - padding, bounds.height - padding)
        }
    }
}
