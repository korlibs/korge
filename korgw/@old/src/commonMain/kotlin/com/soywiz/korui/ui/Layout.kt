package com.soywiz.korui.ui

import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import com.soywiz.korui.geom.len.*
import com.soywiz.korui.style.*
import kotlin.math.*

open class Layout(val app: Application) {
	val ctx = app.lengthContext
	//open fun calculateSize(parent: Component, out: ISize = ISize(0, 0)): ISize {
	//	//parent.children
	//	return out
	//}

	fun applyLayout(parent: Component, children: Iterable<Component>, inoutBounds: RectangleInt) {
		ctx.keep {
			viewportWidth = inoutBounds.width.toDouble()
			viewportHeight = inoutBounds.height.toDouble()
			applyLayoutInternal(parent, children, inoutBounds)
		}
	}

	protected open fun applyLayoutInternal(
		parent: Component,
		children: Iterable<Component>,
		inoutBounds: RectangleInt
	) {
	}

	fun applyLayout(
		parent: Component,
		children: Iterable<Component>,
		x: Int,
		y: Int,
		width: Int,
		height: Int,
		out: RectangleInt = RectangleInt()
	): RectangleInt {
		applyLayout(parent, children, out.setTo(x, y, width, height))
		return out
	}

	enum class ScaleMode2 { NEVER, SHRINK, ALWAYS }

	data class ResultBounds<T>(
		val child: T,
		val bounds: IntRange,
		val padPrev: Int
	) {
		val len = bounds.endInclusive - bounds.start
	}

	fun <T> genAxisBounds(
		size: Int,
		list: Iterable<T>,
		itemSize: T.(Int) -> Int,
		paddingPrev: T.() -> Length?,
		paddingNext: T.() -> Length?,
		scaled: ScaleMode2
	): List<ResultBounds<T>> {
		var pos = 0
		var lastPadding = 0
		val out = arrayListOf<ResultBounds<T>>()

		for (item in list) {
			val itemPaddingPrev = max(lastPadding, item.paddingPrev().calcMax(ctx.setSize(size)))
			val itemSizeSize = item.itemSize(size)
			val actualItemPaddingPrev = if (lastPadding != 0) itemPaddingPrev else 0
			pos += actualItemPaddingPrev
			val start = pos
			pos += itemSizeSize
			val end = pos
			out += ResultBounds(item, start until end, actualItemPaddingPrev)
			lastPadding = item.paddingNext().calcMax(ctx.setSize(size))
		}

		val scaleFit = size.toDouble() / pos.toDouble()

		val scale = when (scaled) {
			ScaleMode2.SHRINK -> if (pos > size) scaleFit else 1.0
			ScaleMode2.ALWAYS -> scaleFit
			ScaleMode2.NEVER -> 1.0
		}

		return out.map { res ->
			res.copy(
				bounds = IntRange((res.bounds.start * scale).toInt(), (res.bounds.endInclusive * scale).toInt()),
				padPrev = (res.padPrev * scale).toInt()
			)
		}
	}
}

class LayeredLayout(app: Application) : Layout(app) {
	override fun applyLayoutInternal(parent: Component, children: Iterable<Component>, inoutBounds: RectangleInt) {
		val actualBounds = RectangleInt().setNewBoundsTo(
			ctx, inoutBounds,
			parent.style.computedPaddingLeft, parent.style.computedPaddingTop,
			100.percent - parent.style.computedPaddingRight, 100.percent - parent.style.computedPaddingBottom
		)

		for (child in children) {
			child.setBoundsAndRelayout(actualBounds)
		}
	}
}

class LayeredKeepAspectLayout(app: Application, val anchor: Anchor, val scaleMode: ScaleMode = ScaleMode.SHOW_ALL) :
	Layout(app) {
	override fun applyLayoutInternal(parent: Component, children: Iterable<Component>, inoutBounds: RectangleInt) {
		val actualBounds = RectangleInt().setNewBoundsTo(
			ctx, inoutBounds,
			parent.style.computedPaddingLeft, parent.style.computedPaddingTop,
			100.percent - parent.style.computedPaddingRight, 100.percent - parent.style.computedPaddingBottom
		)
        val actualBoundsSize = SizeInt(actualBounds.width, actualBounds.height)

		for (child in children) {
			val size1 = child.computedCalcSize(ctx, actualBoundsSize, ignoreBounds = true)
			val asize = size1.applyScaleMode(actualBoundsSize, scaleMode)
			//println("$size1, $asize")

			val endSize = asize.anchoredIn(actualBounds, anchor)
			child.setBoundsAndRelayout(endSize)
			child.setBoundsInternal(endSize)
		}
	}
}

abstract class VerticalHorizontalLayout(
	app: Application,
	val vertical: Boolean,
	val scaleMode: ScaleMode2,
	val resizeContainer: Boolean
) : Layout(app) {
	override fun applyLayoutInternal(parent: Component, children: Iterable<Component>, inoutBounds: RectangleInt) {
		//val width2 = width - parent.style.computedPaddingRight.calc(width)
		//val height2 = height - parent.style.computedPaddingBottom.calc(height)

		val paddingPrev = if (vertical) parent.style.computedPaddingTop else parent.style.computedPaddingLeft
		val paddingNext = if (vertical) parent.style.computedPaddingBottom else parent.style.computedPaddingRight
		val inboundsSide = if (vertical) inoutBounds.height else inoutBounds.width

		val posList = genAxisBounds(
			inboundsSide, children,
			{ if (vertical) this.computedCalcHeight(ctx.setSize(it)) else this.computedCalcWidth(ctx.setSize(it)) },
			{ paddingPrev },
			{ paddingNext },
			scaled = scaleMode
		)

		var roffset = 0
		for (pos in posList) {
			roffset += pos.padPrev
			if (vertical) {
				val cbounds = pos.child.setBoundsAndRelayout(0, roffset, inoutBounds.width, pos.len)
				roffset += cbounds.height
			} else {
				val cbounds = pos.child.setBoundsAndRelayout(roffset, 0, pos.len, inoutBounds.height)
				roffset += cbounds.width
			}
		}

		if (resizeContainer) {
			if (vertical) {
				inoutBounds.setSize(inoutBounds.width, roffset)
			} else {
				inoutBounds.setSize(roffset, inoutBounds.height)
			}
		}
	}
}

class VerticalLayout(app: Application) :
	VerticalHorizontalLayout(app, vertical = true, scaleMode = ScaleMode2.SHRINK, resizeContainer = true)

class HorizontalLayout(app: Application) :
	VerticalHorizontalLayout(app, vertical = false, scaleMode = ScaleMode2.ALWAYS, resizeContainer = true)

class ScrollPaneLayout(app: Application) :
	VerticalHorizontalLayout(app, vertical = true, scaleMode = ScaleMode2.NEVER, resizeContainer = false)

class InlineLayout(app: Application) : Layout(app) {
	override fun applyLayoutInternal(parent: Component, children: Iterable<Component>, inoutBounds: RectangleInt) {
		val posList = genAxisBounds(
			inoutBounds.width, children,
			{ this.computedCalcWidth(ctx.setSize(it)) },
			{ parent.style.computedPaddingLeft },
			{ parent.style.computedPaddingRight },
			scaled = ScaleMode2.NEVER
		)

		var maxheight = 0
		var xoffset = 0
		for (pos in posList) {
			val cheight = pos.child.computedCalcHeight(ctx.setSize(inoutBounds.height), ignoreBounds = true)
			xoffset += pos.padPrev
			maxheight = max(maxheight, cheight)
			val cbounds = pos.child.setBoundsAndRelayout(xoffset, 0, pos.len, cheight)
			xoffset += cbounds.width
		}

		inoutBounds.setSize(inoutBounds.width, maxheight)
	}
}

class RelativeLayout(app: Application) : Layout(app) {
	override fun applyLayoutInternal(parent: Component, children: Iterable<Component>, inoutBounds: RectangleInt) {
		val parentWidth = inoutBounds.width
		val parentHeight = inoutBounds.height

		val childrenSet = HashSet(children.toList())
		val computed = LinkedHashSet<Component>()

		var maxHeight = parentHeight

		fun compute(c: Component): RectangleInt {
			if (c in computed) return c.actualBounds
			computed += c
			if (c !in childrenSet) return c.actualBounds

			val relativeTo = c.computedRelativeTo
			val cw = c.computedCalcWidth(ctx.setSize(parentWidth))
			val ch = c.computedCalcHeight(ctx.setSize(parentHeight))
			val cComputedLeft = c.computedLeft
			val cComputedTop = c.computedTop
			val cComputedRight = c.computedRight
			val cComputedBottom = c.computedBottom

			val cActualBounds = c.actualBounds
			cActualBounds.setSize(cw, ch)

			if (cComputedLeft != null) {
				val leftRelative = if (relativeTo != null) compute(relativeTo).right else 0
				cActualBounds.x = leftRelative + cComputedLeft.calc(ctx.setSize(parentWidth))
			} else if (cComputedRight != null) {
				val rightRelative = if (relativeTo != null) compute(relativeTo).left else parentWidth
				cActualBounds.x = rightRelative - cComputedRight.calc(ctx.setSize(parentWidth)) - c.actualBounds.width
			} else {
				cActualBounds.x = if (relativeTo != null) compute(relativeTo).x else 0
			}

			if (cComputedTop != null) {
				val topRelative = if (relativeTo != null) compute(relativeTo).bottom else 0
				cActualBounds.y = topRelative + cComputedTop.calc(ctx.setSize(parentHeight))
			} else if (cComputedBottom != null) {
				val bottomRelative = if (relativeTo != null) compute(relativeTo).top else parentHeight
				cActualBounds.y = bottomRelative - cComputedBottom.calc(ctx.setSize(parentHeight)) -
						c.actualBounds.height
			} else {
				cActualBounds.y = if (relativeTo != null) compute(relativeTo).y else 0
			}

			c.setBoundsAndRelayout(cActualBounds)

			maxHeight = max(maxHeight, cActualBounds.height)

			return c.actualBounds
		}

		for (c in children) {
			compute(c)
		}

		//inoutBounds.setSize(parentWidth, maxHeight)
	}
}
