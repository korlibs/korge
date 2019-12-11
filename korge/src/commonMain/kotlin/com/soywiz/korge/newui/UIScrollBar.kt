package com.soywiz.korge.newui

import com.soywiz.kmem.*
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import kotlin.math.*
import kotlin.properties.*

inline fun Container.uiScrollBar(
	width: Number,
	height: Number,
	current: Number = 0.0,
	pageSize: Number = 1.0,
	totalSize: Number = 10.0,
	buttonSize: Number = 32.0,
	direction: UIScrollBar.Direction = if (width.toDouble() > height.toDouble()) UIScrollBar.Direction.Horizontal else UIScrollBar.Direction.Vertical,
	stepSize: Double = pageSize.toDouble() / 10.0,
	skin: UISkin = defaultUISkin,
	block: UIScrollBar.() -> Unit = {}
): UIScrollBar = UIScrollBar(width.toDouble(), height.toDouble(), current.toDouble(), pageSize.toDouble(), totalSize.toDouble(), buttonSize.toDouble(), direction, stepSize, skin).also { addChild(it) }.apply(block)

open class UIScrollBar(
	width: Double,
	height: Double,
	current: Double,
	pageSize: Double,
	totalSize: Double,
	buttonSize: Double = 32.0,
	direction: Direction = if (width > height) Direction.Horizontal else Direction.Vertical,
	var stepSize: Double = pageSize / 10.0,
	skin: UISkin = DefaultUISkin
) : UIView() {
	val onChange = Signal<UIScrollBar>()
	enum class Direction { Vertical, Horizontal }
	var current by uiObservable(current) { updatedPos() }
	var pageSize by uiObservable(pageSize) { updatedPos() }
	var totalSize by uiObservable(totalSize) { updatedPos() }
	var direction by uiObservable(direction) { reshape() }
	val isHorizontal get() = direction == Direction.Horizontal
	val isVertical get() = direction == Direction.Vertical
	override var ratio: Double
		set(value) = run { current = value.clamp01() * (totalSize - pageSize) }
		get() = (current / (totalSize - pageSize)).clamp(0.0, 1.0)
	override var width: Double by uiObservable(width) { reshape() }
	override var height: Double by uiObservable(height) { reshape() }
	var buttonSize by uiObservable(buttonSize) { reshape() }
	val buttonWidth get() = if (isHorizontal) buttonSize else width
	val buttonHeight get() = if (isHorizontal) height else buttonSize
	val clientWidth get() = if (isHorizontal) width - buttonWidth * 2 else width
	val clientHeight get() = if (isHorizontal) height else height - buttonHeight * 2

	protected val background = solidRect(100, 100, skin.backColor)
	protected val lessButton = uiButton(16, 16, "-", skin = skin)
	protected val moreButton = uiButton(16, 16, "+", skin = skin)
	protected val caretButton = uiButton(16, 16, "", skin = skin)

	protected val views get() = stage?.views

	init {
		reshape()

		var slx: Double = 0.0
		var sly: Double = 0.0
		var iratio: Double = 0.0
		var sratio: Double = 0.0
		val tempP = Point()

		lessButton.onDown {
			deltaCurrent(-stepSize)
			reshape()
		}
		moreButton.onDown {
			deltaCurrent(+stepSize)
			reshape()
		}
		background.onClick {
			val pos = if (isHorizontal) caretButton.localMouseX(views!!) else caretButton.localMouseY(views!!)
			deltaCurrent(this.pageSize * pos.sign)
		}
		caretButton.onMouseDrag {
			val lmouse = background.localMouseXY(views, tempP)
			val lx = lmouse.x
			val ly = lmouse.y
			val cratio = if (isHorizontal) lmouse.x / background.width else lmouse.y / background.height
			if (it.start) {
				slx = lx
				sly = ly
				iratio = ratio
				sratio = cratio
			}
			val dratio = cratio - sratio
			ratio = iratio + dratio
			reshape()
		}
	}

	private fun deltaCurrent(value: Double) {
		//println("deltaCurrent: $value")
		current = (current + value).clamp(0.0, totalSize - pageSize)
	}

	private fun reshape() {
		if (isHorizontal) {
			background.position(buttonWidth, 0).size(clientWidth, clientHeight)
			lessButton.position(0, 0).size(buttonWidth, buttonHeight)
			moreButton.position(width - buttonWidth, 0).size(buttonWidth, buttonHeight)
			val caretWidth = (clientWidth * (pageSize / totalSize)).clamp(4.0, clientWidth)
			caretButton.position(buttonWidth + (clientWidth - caretWidth) * ratio, 0).size(caretWidth, buttonHeight)
		} else {
			background.position(0, buttonHeight).size(clientWidth, clientHeight)
			lessButton.position(0, 0).size(buttonWidth, buttonHeight)
			moreButton.position(0, height - buttonHeight).size(buttonWidth, buttonHeight)
			val caretHeight = (clientHeight * (pageSize / totalSize)).clamp(4.0, clientHeight)
			caretButton.position(0, buttonHeight + (clientHeight - caretHeight) * ratio).size(buttonWidth, caretHeight)
		}
	}

	private fun updatedPos() {
		reshape()
		onChange(this)
	}
}
