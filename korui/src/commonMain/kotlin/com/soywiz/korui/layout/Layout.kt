package com.soywiz.korui.layout

import com.soywiz.kds.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*

object VerticalUiLayout : LineUiLayout(LayoutDirection.VERTICAL)
object HorizontalUiLayout : LineUiLayout(LayoutDirection.HORIZONTAL)

open class LineUiLayout(
    open var direction: LayoutDirection = LayoutDirection.VERTICAL
) : UiLayout {
    override fun computePreferredSize(container: UiContainer, available: SizeInt): SizeInt {
        var sum = 0
        val ctx = Length.Context()
        val padding = container.layoutChildrenPadding
        ctx.size = 1024
        container.forEachChild { child ->
            val value = when (child) {
                //is UiContainer -> child.computePreferredSize(available).getDirection(direction)
                is UiContainer -> child.computePreferredSize(SizeInt(16, 16)).getDirection(direction)
                else -> Length.calc(ctx, 32.pt, child.preferredSize.getDirection(direction), child.minimumSize.getDirection(direction), child.maximumSize.getDirection(direction))
            }
            sum += value + padding
        }
        //println("${container.preferredSize} - ${container.minimumSize} - ${container.maximumSize}")

        return SizeInt(if (direction.horizontal) sum else 16, if (direction.vertical) sum else 16)
    }

    override fun relayout(container: UiContainer) {
        val bounds = container.bounds
        //println("$bounds: ${container.children}")
        val ctx = Length.Context()
        ctx.size = bounds.getSizeDirection(direction)

        val padding = container.layoutChildrenPadding
        val padding2 = padding * 2

        var sum = 0
        var cur = padding

        container.forEachChild { child ->
            if (child.visible) {
                val value = when (child) {
                    is UiContainer -> child.computePreferredSize(bounds.size).getDirection(direction)
                    else -> Length.calc(ctx, 32.pt, child.preferredSize.getDirection(direction), child.minimumSize.getDirection(direction), child.maximumSize.getDirection(direction))
                }
                val childBounds = when (direction) {
                    LayoutDirection.VERTICAL -> RectangleInt(0, cur, bounds.width, value)
                    LayoutDirection.HORIZONTAL -> RectangleInt(cur, 0, value, bounds.height)
                }
                child.bounds = RectangleInt(childBounds.x, childBounds.y, childBounds.width, childBounds.height)
                if (child is UiContainer) {
                    child.layout?.relayout(child)
                }
                sum += value
                cur += value + padding
            }
        }
    }
}

fun RectangleInt.getSizeDirection(direction: LayoutDirection) = if (direction == LayoutDirection.VERTICAL) height else width
fun Size.getDirection(direction: LayoutDirection) = if (direction == LayoutDirection.VERTICAL) height else width
fun SizeInt.getDirection(direction: LayoutDirection) = if (direction == LayoutDirection.VERTICAL) height else width
enum class LayoutDirection {
    VERTICAL, HORIZONTAL;
    val vertical get() = this == VERTICAL
    val horizontal get() = this == HORIZONTAL
}

var UiComponent.preferredSize by Extra.PropertyThis<UiComponent, Size>() { Size(null, null) }
var UiComponent.minimumSize by Extra.PropertyThis<UiComponent, Size>() { Size(null, null) }
var UiComponent.maximumSize by Extra.PropertyThis<UiComponent, Size>() { Size(null, null) }

var UiComponent.preferredWidth: Length?
    get() = preferredSize.width
    set(value) {
        preferredSize = preferredSize.copy(width = value)
    }
var UiComponent.preferredHeight: Length?
    get() = preferredSize.height
    set(value) {
        preferredSize = preferredSize.copy(height = value)
    }

fun UiContainer.vertical(block: UiContainer.() -> Unit): UiContainer {
    return UiContainer(app).also { it.layout = LineUiLayout(LayoutDirection.VERTICAL) }.also { it.parent = this }.also(block)
}

fun UiContainer.horizontal(block: UiContainer.() -> Unit): UiContainer {
    return UiContainer(app).also { it.layout = LineUiLayout(LayoutDirection.VERTICAL) }.also { it.parent = this }.also(block)
}
