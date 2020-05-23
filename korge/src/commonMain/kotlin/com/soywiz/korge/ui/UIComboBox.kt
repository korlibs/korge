package com.soywiz.korge.ui

import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*

@Deprecated("Kotlin/Native boxes inline+Number")
inline fun <T> Container.uiComboBox(
	width: Number = 192.0,
	height: Number = 32.0,
	selectedIndex: Int = 0,
	items: List<T>,
	verticalScroll: Boolean = true,
	skin: ComboBoxSkin = defaultComboBoxSkin,
	block: @ViewsDslMarker UIComboBox<T>.() -> Unit = {}
) = uiComboBox(width.toDouble(), height.toDouble(), selectedIndex, items, verticalScroll, skin, block)

inline fun <T> Container.uiComboBox(
    width: Double = 192.0,
    height: Double = 32.0,
    selectedIndex: Int = 0,
    items: List<T>,
    verticalScroll: Boolean = true,
    skin: ComboBoxSkin = defaultComboBoxSkin,
    block: @ViewsDslMarker UIComboBox<T>.() -> Unit = {}
) = UIComboBox(width, height, selectedIndex, items, verticalScroll, skin).addTo(this).apply(block)

open class UIComboBox<T>(
	width: Double = 192.0,
	height: Double = 32.0,
	selectedIndex: Int = 0,
	items: List<T>,
	verticalScroll: Boolean = true,
    private val skin: ComboBoxSkin = DefaultComboBoxSkin
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
		config = { visible = false }
	)
	private val selectedButton = textButton(width - height, height, "", skin.selectedSkin, skin.textFont)
	private val expandButton = iconButton(height, height, skin.expandSkin).position(width - height, 0)
	private val invisibleRect = solidRect(width, height, Colors.TRANSPARENT_BLACK)
	private var showItems = false

	val onSelectionUpdate = Signal<UIComboBox<T>>()

	init {
		updateItems()
		invisibleRect.onOver {
			selectedButton.simulateOver()
			expandButton.simulateOver()
		}
		invisibleRect.onOut {
			selectedButton.simulateOut()
			expandButton.simulateOut()
		}
        invisibleRect.onDown {
            selectedButton.simulateDown()
            expandButton.simulateDown()
        }
        invisibleRect.onUp {
            selectedButton.simulateUp()
            expandButton.simulateUp()
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
			itemsView.container.textButton(width - 32, itemHeight, item.toString(), skin.itemSkin, skin.textFont) {
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
		expandButton.simulatePressing(showItems)
        expandButton.skin = skin.expandSkin
        expandButton.iconSkin = if (showItems) skin.hideIcon else skin.showIcon
		invisibleRect.size(width, height)
		selectedButton.size(width - height, height)
		selectedButton.text = selectedItem?.toString() ?: ""
		expandButton.position(width - height, 0).size(height, height)
	}
}
