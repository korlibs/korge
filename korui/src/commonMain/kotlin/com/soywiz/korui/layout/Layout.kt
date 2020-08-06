package com.soywiz.korui.layout

import com.soywiz.kds.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*

open class UiLayout(val container: UiContainer) {
    open fun relayout(bounds: RectangleInt) {
    }
}

open class LineUiLayout(
    container: UiContainer,
    open var direction: LayoutDirection = LayoutDirection.VERTICAL
) : UiLayout(container) {
    override fun relayout(bounds: RectangleInt) {
        //println("$bounds: ${container.children}")
        val ctx = Length.Context()

        var sum = 0
        var cur = 0

        container.forEachChild { child ->
            val value = Length.calc(ctx, 32.pt, child.size.getDirection(direction), child.minimumSize.getDirection(direction), child.maximumSize.getDirection(direction))
            val childBounds = when (direction) {
                LayoutDirection.VERTICAL -> RectangleInt(0, cur, bounds.width, value)
                LayoutDirection.HORIZONTAL -> RectangleInt(cur, 0, value, bounds.height)
            }
            child.bounds = RectangleInt(childBounds.x, childBounds.y, childBounds.width, childBounds.height)
            if (child is UiContainer) {
                child.layout?.relayout(childBounds)
            }
            sum += value
            cur += value
        }
    }
}

fun Size.getDirection(direction: LayoutDirection) = if (direction == LayoutDirection.VERTICAL) height else width
enum class LayoutDirection { VERTICAL, HORIZONTAL }

var UiContainer.layout by Extra.PropertyThis<UiContainer, UiLayout?>() { LineUiLayout(this, LayoutDirection.VERTICAL) }

var UiComponent.size by Extra.PropertyThis<UiComponent, Size>() { Size(null, null) }
var UiComponent.minimumSize by Extra.PropertyThis<UiComponent, Size>() { Size(null, null) }
var UiComponent.maximumSize by Extra.PropertyThis<UiComponent, Size>() { Size(null, null) }

var UiComponent.width: Length?
    get() = size.width
    set(value) {
        size = size.copy(width = value)
    }
var UiComponent.height: Length?
    get() = size.height
    set(value) {
        size = size.copy(height = value)
    }

fun UiContainer.vertical(block: UiContainer.() -> Unit): UiContainer {
    return UiContainer(app).also { it.layout = LineUiLayout(it, LayoutDirection.VERTICAL) }.also { it.parent = this }.also(block)
}

fun UiContainer.horizontal(block: UiContainer.() -> Unit): UiContainer {
    return UiContainer(app).also { it.layout = LineUiLayout(it, LayoutDirection.VERTICAL) }.also { it.parent = this }.also(block)
}
