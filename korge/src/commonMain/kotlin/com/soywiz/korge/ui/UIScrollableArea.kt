package com.soywiz.korge.ui

import com.soywiz.korge.html.*
import com.soywiz.korge.view.*

inline fun Container.uiScrollableArea(
	width: Number = 256.0,
	height: Number = 256.0,
	contentWidth: Number = 512.0,
	contentHeight: Number = 512.0,
	buttonSize: Number = 32.0,
	verticalScroll: Boolean = true,
	horizontalScroll: Boolean = true,
	font: Html.FontFace = defaultUIFont,
	skin: UISkin = defaultUISkin,
	upSkin: UISkin? = null,
	downSkin: UISkin? = null,
	leftSkin: UISkin? = null,
	rightSkin: UISkin? = null,
	config: @ViewsDslMarker UIScrollableArea.() -> Unit = {},
	block: @ViewsDslMarker Container.() -> Unit = {}
): UIScrollableArea = UIScrollableArea(
	width.toDouble(),
	height.toDouble(),
	contentWidth.toDouble(),
	contentHeight.toDouble(),
	buttonSize.toDouble(),
	verticalScroll,
	horizontalScroll,
	font,
	skin,
	upSkin,
	downSkin,
	leftSkin,
	rightSkin
).addTo(this).apply(config).also { block(it.container) }

// @TODO: Optimize this!
// @TODO: Add an actualContainer = this inside Container
open class UIScrollableArea(
	width: Double = 256.0,
	height: Double = 256.0,
	contentWidth: Double = 512.0,
	contentHeight: Double = 512.0,
	buttonSize: Double = 32.0,
	verticalScroll: Boolean = true,
	horizontalScroll: Boolean = true,
	font: Html.FontFace = DefaultUIFont,
	skin: UISkin = DefaultUISkin,
	upSkin: UISkin? = null,
	downSkin: UISkin? = null,
	leftSkin: UISkin? = null,
	rightSkin: UISkin? = null
) : UIView(width, height) {

	var buttonSize by uiObservable(buttonSize) { onSizeChanged() }

	var contentWidth by uiObservable(contentWidth) { onSizeChanged() }
	var contentHeight by uiObservable(contentHeight) { onSizeChanged() }

	var verticalScroll by uiObservable(verticalScroll) { onSizeChanged() }
	var horizontalScroll by uiObservable(horizontalScroll) { onSizeChanged() }

	var stepRatio by uiObservable(0.1) { onSizeChanged() }

	val viewportWidth get() = if (verticalScroll) width - buttonSize else width
	val viewportHeight get() = if (horizontalScroll) height - buttonSize else height

	val clipContainer = clipContainer(viewportWidth, viewportHeight)
	val container = clipContainer.fixedSizeContainer(contentWidth, contentHeight)

	val horScrollBar = uiScrollBar(width, buttonSize, font = font, skin = skin, upSkin = leftSkin, downSkin = rightSkin)
		.also { it.onChange { onMoved() } }
	val verScrollBar = uiScrollBar(buttonSize, height, font = font, skin = skin, upSkin = upSkin, downSkin = downSkin)
		.also { it.onChange { onMoved() } }

	init {
		onSizeChanged()
	}

	override fun onSizeChanged() {
		super.onSizeChanged()

		horScrollBar.totalSize = contentWidth
		horScrollBar.pageSize = viewportWidth
		horScrollBar.stepSize = viewportWidth * stepRatio

		verScrollBar.totalSize = contentHeight
		verScrollBar.pageSize = viewportHeight
		verScrollBar.stepSize = viewportHeight * stepRatio

		clipContainer.size(viewportWidth, viewportHeight)
		container.size(contentWidth, contentHeight)

		horScrollBar.size(viewportWidth, buttonSize)
		horScrollBar.position(0, height - buttonSize)
		horScrollBar.visible = horizontalScroll

		verScrollBar.size(buttonSize, viewportHeight)
		verScrollBar.position(width - buttonSize, 0)
		verScrollBar.visible = verticalScroll
	}

	protected open fun onMoved() {
		container.x = -horScrollBar.current
		container.y = -verScrollBar.current
	}
}
