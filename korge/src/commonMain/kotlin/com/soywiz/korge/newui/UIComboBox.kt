package com.soywiz.korge.newui

import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*

inline fun <T> Container.uiComboBox(
	width: Number = 192.0,
	height: Number = 32.0,
	selectedIndex: Int = 0,
	items: List<T>,
	skin: UISkin = defaultUISkin,
	block: UIComboBox<T>.() -> Unit = {}
) = UIComboBox(width.toDouble(), height.toDouble(), selectedIndex, items, skin).also { addChild(it) }.also(block)

open class UIComboBox<T>(
	width: Double = 192.0,
	height: Double = 32.0,
	selectedIndex: Int = 0,
	items: List<T>,
	private val skin: UISkin = DefaultUISkin
) : UIView(width, height) {

	val onUpdatedSelection = Signal<UIComboBox<T>>()

	var selectedIndex by uiObservable(selectedIndex) { updatedSelection() }
	var selectedItem: T?
		set(value) = run { selectedIndex = items.indexOf(value) }
		get() = items.getOrNull(selectedIndex)
	var items: List<T> by uiObservable(items) { updatedItems() }

	val itemHeight get() = 32
	private val buttonSize get() = height
	private val itemsView = uiScrollableArea(verticalScroll = true, horizontalScroll = false, skin = skin, config = { visible = false }) {  }
	private val selectedButton = uiButton(16, 16, "", skin = skin).also { it.mouseEnabled = false }
	private val dropButton = uiButton(16, 16, "+", skin = skin).also { it.mouseEnabled = false }
	private val invisibleRect = solidRect(16, 16, Colors.TRANSPARENT_BLACK)
	private var showItems = false

	init {
		updatedItems()
		invisibleRect.onOver {
			selectedButton.simulateHover()
			dropButton.simulateHover()
		}
		invisibleRect.onOut {
			selectedButton.simulateOut()
			dropButton.simulateOut()
		}
		invisibleRect.onClick {
			showItems = !showItems
			updatedSize()
		}
	}

	override fun updatedSize() {
		super.updatedSize()
		itemsView.visible = showItems
		itemsView.size(width, 196).position(0, height)
		selectedButton.simulatePressing(showItems)
		dropButton.simulatePressing(showItems)
		dropButton.label = if (showItems) "-" else "+"
		invisibleRect.size(width, height)
		selectedButton.position(0, 0).size(width - buttonSize, height)
		selectedButton.label = selectedItem?.toString() ?: ""
		//println("selectedIndex: $selectedIndex, selectedItem: $selectedItem")
		dropButton.position(width - buttonSize, 0).size(buttonSize, height)
	}

	protected fun updatedSelection() {
		updatedSize()
		for (n in items.indices) {
			val button = itemsView.container.children[n] as? UIButton? ?: continue
			button.forcePressed = selectedIndex == n
		}
		onUpdatedSelection(this)
	}

	protected fun updatedItems() {
		itemsView.container.removeChildren()
		for ((index, item) in items.withIndex()) {
			val button = itemsView.container.uiButton(width - 32, itemHeight, label = item.toString(), skin = skin) {
				position(0, index * itemHeight)
				onClick {
					showItems = false
					selectedIndex = index
					updatedSize()
				}
			}
		}
		itemsView.contentHeight = ((items.size) * itemHeight).toDouble()
		updatedSelection()
	}
}
