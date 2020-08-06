package com.soywiz.korui.layout

import com.soywiz.kds.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*

object VerticalUiLayout : LineUiLayout(LayoutDirection.VERTICAL)
object HorizontalUiLayout : LineUiLayout(LayoutDirection.HORIZONTAL)

open class LineUiLayout(
    open var direction: LayoutDirection = LayoutDirection.VERTICAL
) : UiLayout {
    override fun relayout(container: UiContainer) {
        val bounds = container.bounds
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
                child.layout?.relayout(child)
            }
            sum += value
            cur += value
        }
    }
}

fun Size.getDirection(direction: LayoutDirection) = if (direction == LayoutDirection.VERTICAL) height else width
enum class LayoutDirection { VERTICAL, HORIZONTAL }

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
    return UiContainer(app).also { it.layout = LineUiLayout(LayoutDirection.VERTICAL) }.also { it.parent = this }.also(block)
}

fun UiContainer.horizontal(block: UiContainer.() -> Unit): UiContainer {
    return UiContainer(app).also { it.layout = LineUiLayout(LayoutDirection.VERTICAL) }.also { it.parent = this }.also(block)
}
