package com.soywiz.korui.layout

import com.soywiz.kds.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import kotlin.math.*

interface UiLayout {
    fun computePreferredSize(container: UiContainer, available: SizeInt): SizeInt
    fun relayout(container: UiContainer)
}

var UiContainer.layoutChildrenPadding by Extra.Property { 0 }

object UiFillLayout : UiLayout {
    override fun computePreferredSize(container: UiContainer, available: SizeInt): SizeInt {
        /*
        var maxWidth = 0
        var maxHeight = 0
        val ctx = LayoutContext(available)

        container.forEachVisibleChild { child ->
            val size = ctx.computeChildSize(child)
            maxWidth = max(size.width, maxWidth)
            maxHeight = max(size.height, maxHeight)
        }
        return SizeInt(maxWidth, maxHeight)
        */
        return available.clone()
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

object VerticalUiLayout : LineUiLayout(LayoutDirection.VERTICAL)
object HorizontalUiLayout : LineUiLayout(LayoutDirection.HORIZONTAL)

fun Length.Context.computeChildSize(child: UiComponent, direction: LayoutDirection): Int {
    val ctx = this
    val preferredSize = child.preferredSize
    return when {
        preferredSize != null -> Length.calc(ctx, 32.pt, preferredSize.getDirection(direction), child.minimumSize.getDirection(direction), child.maximumSize.getDirection(direction))
        child is UiContainer -> child.computePreferredSize(SizeInt(32, 32)).getDirection(direction)
        else -> when (direction) {
            LayoutDirection.HORIZONTAL -> 128
            LayoutDirection.VERTICAL -> 32
        }
    }
}

class LayoutContext(val available: SizeInt) {
    val widthContext = Length.Context().also { it.size = available.width }
    val heightContext = Length.Context().also { it.size = available.height }

    fun computeChildSize(child: UiComponent): SizeInt {
        return SizeInt(
            widthContext.computeChildSize(child, LayoutDirection.HORIZONTAL),
            heightContext.computeChildSize(child, LayoutDirection.VERTICAL),
        )
    }
}

open class LineUiLayout(
    open var direction: LayoutDirection = LayoutDirection.VERTICAL
) : UiLayout, LengthExtensions {
    val revDirection = direction.reversed
    override fun computePreferredSize(container: UiContainer, available: SizeInt): SizeInt {
        var sum = 0
        var max = 0
        val ctx = LayoutContext(available)
        val padding = container.layoutChildrenPadding
        container.forEachVisibleChild { child ->
            val size = ctx.computeChildSize(child)
            val main = size.getDirection(direction)
            val rev = size.getDirection(revDirection)

            //println(main)
            sum += main + padding
            max = max(max, rev)
        }
        //println("${container.preferredSize} - ${container.minimumSize} - ${container.maximumSize}")

        return SizeInt(if (direction.horizontal) sum else max, if (direction.vertical) sum else max)
    }

    override fun relayout(container: UiContainer) {
        val bounds = container.bounds
        //println("$bounds: ${container.children}")
        //val ctx = Length.Context()
        //ctx.size = bounds.getSizeDirection(direction)
        val ctx = LayoutContext(bounds.size)

        val padding = container.layoutChildrenPadding
        val padding2 = padding * 2

        var sum = 0
        var cur = padding

        container.forEachVisibleChild { child ->
            val size = ctx.computeChildSize(child)
            val value = size.getDirection(direction)
            val childBounds = when (direction) {
                LayoutDirection.VERTICAL -> RectangleInt(0, cur, bounds.width, value)
                LayoutDirection.HORIZONTAL -> RectangleInt(cur, 0, value, bounds.height)
            }
            //println("$child: $childBounds")
            child.bounds = childBounds
            if (child is UiContainer) {
                child.layout?.relayout(child)
            }
            sum += value
            cur += value + padding
        }
    }
}

fun RectangleInt.getSizeDirection(direction: LayoutDirection) = if (direction == LayoutDirection.VERTICAL) height else width
fun Size.getDirection(direction: LayoutDirection) = if (direction == LayoutDirection.VERTICAL) height else width
fun SizeInt.getDirection(direction: LayoutDirection) = if (direction == LayoutDirection.VERTICAL) height else width
enum class LayoutDirection {
    VERTICAL, HORIZONTAL;

    val reversed get() = if (vertical) HORIZONTAL else VERTICAL

    val vertical get() = this == VERTICAL
    val horizontal get() = this == HORIZONTAL
}

private val DEFAULT_WIDTH = Length.PT(128.0)
private val DEFAULT_HEIGHT = Length.PT(32.0)

//var UiComponent.preferredSize by Extra.PropertyThis<UiComponent, Size> { Size(DEFAULT_WIDTH, DEFAULT_HEIGHT) }
var UiComponent.preferredSize by Extra.PropertyThis<UiComponent, Size?> { null }
var UiComponent.minimumSize by Extra.PropertyThis<UiComponent, Size> { Size(null, null) }
var UiComponent.maximumSize by Extra.PropertyThis<UiComponent, Size> { Size(null, null) }

fun UiComponent.preferredSize(width: Length?, height: Length?) {
    preferredSize = Size(width, height)
}

var UiComponent.preferredWidth: Length?
    get() = preferredSize?.width
    set(value) {
        preferredSize = Size(value, preferredSize?.height)
    }
var UiComponent.preferredHeight: Length?
    get() = preferredSize?.height
    set(value) {
        preferredSize = Size(preferredSize?.width, value)
    }

fun UiContainer.vertical(block: UiContainer.() -> Unit): UiContainer {
    return UiContainer(app).also { it.layout = LineUiLayout(LayoutDirection.VERTICAL) }.also { it.parent = this }.also(block)
}

fun UiContainer.horizontal(block: UiContainer.() -> Unit): UiContainer {
    return UiContainer(app).also { it.layout = LineUiLayout(LayoutDirection.HORIZONTAL) }.also { it.parent = this }.also(block)
}
