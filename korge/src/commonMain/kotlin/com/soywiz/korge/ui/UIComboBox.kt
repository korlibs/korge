package com.soywiz.korge.ui

import com.soywiz.korge.html.*
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*

inline fun <T> Container.uiComboBox(
	width: Number = 192.0,
	height: Number = 32.0,
	selectedIndex: Int = 0,
	items: List<T>,
	verticalScroll: Boolean = true,
	textFont: Html.FontFace = defaultUIFont,
	skin: UISkin = defaultUISkin,
	showSkin: UISkin = skin,
	hideSkin: UISkin? = null,
	upSkin: UISkin? = null,
	downSkin: UISkin? = null,
	scrollbarSkin: UISkin = skin,
	block: @ViewsDslMarker UIComboBox<T>.() -> Unit = {}
) = UIComboBox(
	width.toDouble(),
	height.toDouble(),
	selectedIndex,
	items,
	verticalScroll,
	textFont,
	skin,
	showSkin,
	hideSkin,
	upSkin,
	downSkin,
	scrollbarSkin
).addTo(this).apply(block)

open class UIComboBox<T>(
	width: Double = 192.0,
	height: Double = 32.0,
	selectedIndex: Int = 0,
	items: List<T>,
	verticalScroll: Boolean = true,
	private val textFont: Html.FontFace = DefaultUIFont,
	private val skin: UISkin = DefaultUISkin,
	private val showSkin: UISkin = skin,
	private val hideSkin: UISkin? = null,
	upSkin: UISkin? = null,
	downSkin: UISkin? = null,
	scrollbarSkin: UISkin = skin
) : UIView(width, height) {

	var selectedIndex by uiObservable(selectedIndex) { updateState() }
	var selectedItem: T?
		get() = items.getOrNull(selectedIndex)
		set(value) = run { selectedIndex = items.indexOf(value) }
	var items: List<T> by uiObservable(items) { updateItems() }
	var itemHeight by uiObservable(32) { updateItemsSize() }
	var viewportHeight by uiObservable(196) { onSizeChanged() }

	private val itemsView = uiScrollableArea(
		verticalScroll = verticalScroll,
		horizontalScroll = false,
		font = textFont,
		skin = scrollbarSkin,
		upSkin = upSkin,
		downSkin = downSkin,
		config = { visible = false }
	)
	private val selectedButton = uiTextButton(width - height, height, "", skin, textFont)
	private val dropButton =
		if (hideSkin == null) uiTextButton(height, height, "+", showSkin, textFont).position(width - height, 0)
		else uiButton(height, height, showSkin).position(width - height, 0)
	private val invisibleRect = solidRect(width, height, Colors.TRANSPARENT_BLACK)
	private var showItems = false

	val onSelectionUpdate = Signal<UIComboBox<T>>()

	init {
		updateItems()
		invisibleRect.onOver {
			selectedButton.simulateOver()
			dropButton.simulateOver()
		}
		invisibleRect.onOut {
			selectedButton.simulateOut()
			dropButton.simulateOut()
		}
		invisibleRect.onClick {
			showItems = !showItems
			onSizeChanged()
		}
	}

	private fun updateItemsSize() {
		itemsView.container.forEachChildrenWithIndex { index, child ->
			child.height = itemHeight.toDouble()
			child.position(0, index * itemHeight)
		}
	}

	private fun updateItems() {
		itemsView.container.removeChildren()
		for ((index, item) in items.withIndex()) {
			itemsView.container.uiTextButton(width - 32, itemHeight, item.toString(), skin, textFont) {
				position(0, index * itemHeight)
				onClick {
					showItems = false
					selectedIndex = index
				}
			}
		}
		itemsView.contentHeight = (items.size * itemHeight).toDouble()
		updateState()
	}

	override fun updateState() {
		onSizeChanged()
		for (i in items.indices) {
			val button = itemsView.container.getChildAtOrNull(i) as? UIButton ?: continue
			button.forcePressed = selectedIndex == i
		}
		onSelectionUpdate(this)
	}

	override fun onSizeChanged() {
		super.onSizeChanged()
		itemsView.visible = showItems
		itemsView.size(width, viewportHeight).position(0, height)
		selectedButton.simulatePressing(showItems)
		dropButton.simulatePressing(showItems)
		if (hideSkin == null) {
			(dropButton as UITextButton).text = if (showItems) "-" else "+"
		} else {
			dropButton.skin = if (showItems) hideSkin else showSkin
		}
		invisibleRect.size(width, height)
		selectedButton.size(width - height, height)
		selectedButton.text = selectedItem?.toString() ?: ""
		dropButton.position(width - height, 0).size(height, height)
	}
}
