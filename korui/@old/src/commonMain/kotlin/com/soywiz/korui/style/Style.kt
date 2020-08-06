package com.soywiz.korui.style

import com.soywiz.korma.geom.*
import com.soywiz.korui.geom.len.*
import com.soywiz.korui.geom.len.Size
import com.soywiz.korui.ui.*

class Style(var parent: Style? = null) : Styled {
	var position = Position(null, null)
	val defaultSize = Size(3.cm, 0.8.cm)
	//val defaultSize = Size(140.pt, 52.pt)
	val size = Size(null, null)
	val minSize = Size(null, null)
	val maxSize = Size(null, null)
	val padding = Padding(null)

	var relativeTo: Component? = null

	var top: Length? = null
	var bottom: Length? = null
	var left: Length? = null
	var right: Length? = null

	override val style: Style = this

	fun copyFrom(other: Style) {
		this.position = other.position
		this.size.copyFrom(other.size)
		this.minSize.copyFrom(other.minSize)
		this.maxSize.copyFrom(other.maxSize)
		this.padding.setTo(other.padding)
		this.top = other.top
		this.bottom = other.bottom
		this.left = other.left
		this.right = other.right
	}
}

fun Style(callback: Style.() -> Unit): Style = Style().apply(callback)

var Styled.classStyle: Style?; get() = style.parent; set(value) = run { style.parent = value }

interface Styled {
	val style: Style
}

val Styled.computedX: Length get() = style.position.x ?: style.parent?.computedX ?: Length.ZERO
val Styled.computedY: Length get() = style.position.y ?: style.parent?.computedY ?: Length.ZERO

var Styled.width: Length? get() = style.size.width; set(v) = run { style.size.width = v }
var Styled.height: Length? get() = style.size.height; set(v) = run { style.size.height = v }

var Styled.minWidth: Length? get() = style.minSize.width; set(v) = run { style.minSize.width = v }
var Styled.minHeight: Length? get() = style.minSize.height; set(v) = run { style.minSize.height = v }

var Styled.maxWidth: Length? get() = style.maxSize.width; set(v) = run { style.maxSize.width = v }
var Styled.maxHeight: Length? get() = style.maxSize.height; set(v) = run { style.maxSize.height = v }

var Styled.padding: Padding get() = style.padding; set(value) = run { style.padding.setTo(value) }

var Styled.relativeTo: Component? get() = style.relativeTo; set(value) = run { style.relativeTo = value }
var Styled.top: Length? get() = style.top; set(value) = run { style.top = value }
var Styled.bottom: Length? get() = style.bottom; set(value) = run { style.bottom = value }
var Styled.left: Length? get() = style.left; set(value) = run { style.left = value }
var Styled.right: Length? get() = style.right; set(value) = run { style.right = value }

val Styled.computedPaddingTop: Length get() = padding.top ?: style.parent?.computedPaddingTop ?: Length.ZERO
val Styled.computedPaddingRight: Length get() = padding.right ?: style.parent?.computedPaddingRight ?: Length.ZERO
val Styled.computedPaddingBottom: Length get() = padding.bottom ?: style.parent?.computedPaddingBottom ?: Length.ZERO
val Styled.computedPaddingLeft: Length get() = padding.left ?: style.parent?.computedPaddingLeft ?: Length.ZERO
val Styled.computedPaddingLeftPlusRight: Length get() = computedPaddingLeft + computedPaddingRight
val Styled.computedPaddingTopPlusBottom: Length get() = computedPaddingTop + computedPaddingBottom

val Styled.computedWidth: Length? get() = style.size.width ?: style.parent?.computedWidth
val Styled.computedHeight: Length? get() = style.size.height ?: style.parent?.computedHeight

val Styled.computedDefaultWidth: Length get() = style.defaultSize.width ?: 120.pt
val Styled.computedDefaultHeight: Length get() = style.defaultSize.height ?: 32.pt

val Styled.computedMinWidth: Length? get() = style.minSize.width ?: style.parent?.computedMinWidth
val Styled.computedMinHeight: Length? get() = style.minSize.height ?: style.parent?.computedMinHeight

val Styled.computedMaxWidth: Length? get() = style.maxSize.width ?: style.parent?.computedMaxWidth
val Styled.computedMaxHeight: Length? get() = style.maxSize.height ?: style.parent?.computedMaxHeight

val Styled.computedRelativeTo: Component? get() = style.relativeTo ?: style.parent?.computedRelativeTo

val Styled.computedTop: Length? get() = style.top ?: style.parent?.computedTop
val Styled.computedBottom: Length? get() = style.bottom ?: style.parent?.computedBottom
val Styled.computedLeft: Length? get() = style.left ?: style.parent?.computedLeft
val Styled.computedRight: Length? get() = style.right ?: style.parent?.computedRight


fun Styled.computedCalcWidth(ctx: Length.Context, ignoreBounds: Boolean = false): Int =
	Length.calc(ctx, computedDefaultWidth, computedWidth, computedMinWidth, computedMaxWidth, ignoreBounds)

fun Styled.computedCalcHeight(ctx: Length.Context, ignoreBounds: Boolean = false): Int =
	Length.calc(ctx, computedDefaultHeight, computedHeight, computedMinHeight, computedMaxHeight, ignoreBounds)

fun Styled.computedCalcSize(
	ctx: Length.Context,
	size: SizeInt,
	out: SizeInt = SizeInt(),
	ignoreBounds: Boolean = false
): SizeInt = out.setTo(
	this.computedCalcWidth(ctx.setSize(size.width), ignoreBounds),
	this.computedCalcHeight(ctx.setSize(size.height), ignoreBounds)
)

