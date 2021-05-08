package com.soywiz.korge.ui

import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*

inline fun <T> Container.uiComboBox(
    width: Double = 192.0,
    height: Double = 32.0,
    selectedIndex: Int = 0,
    items: List<T>,
    verticalScroll: Boolean = true,
    block: @ViewDslMarker UIComboBox<T>.() -> Unit = {}
) = UIComboBox(width, height, selectedIndex, items, verticalScroll).addTo(this).apply(block)

open class UIComboBox<T>(
    width: Double = 192.0,
    height: Double = 32.0,
    selectedIndex: Int = 0,
    items: List<T> = listOf(),
    verticalScroll: Boolean = true,
) : UIView(width, height) {

    var selectedIndex by uiObservable(selectedIndex) { updateState() }
    var selectedItem: T?
        get() = items.getOrNull(selectedIndex)
        set(value) {
            selectedIndex = items.indexOf(value)
        }
    var items: List<T> by uiObservable(items) { updateItems() }
    var itemHeight by uiObservable(32) { updateItemsSize() }
    var viewportHeight by uiObservable(196) { onSizeChanged() }

    private val itemsView = UIScrollableArea(
        verticalScroll = verticalScroll,
        horizontalScroll = false,
    )
    private val selectedButton = uiButton(width - height, height, "")
    private val expandButton = uiButton(height, height, icon = comboBoxExpandIcon).position(width - height, 0.0)
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

    fun open() {
        addChild(itemsView)

        // Prevent overlap by other controls.
        parent?.sendChildToFront(this)
    }

    fun close() {
        removeChild(itemsView)
    }

    private fun updateItemsSize() {
        itemsView.container.forEachChildWithIndex { index, child ->
            child.scaledHeight = itemHeight.toDouble()
            child.position(0, index * itemHeight)
        }
    }

    private fun updateItems() {
        itemsView.container.removeChildren()
        for ((index, item) in items.withIndex()) {
            itemsView.container.uiButton(
                width - 32.0,
                itemHeight.toDouble(),
                item.toString()
            ) {
                position(0, index * this@UIComboBox.itemHeight)
                onClick {
                    this@UIComboBox.showItems = false
                    this@UIComboBox.selectedIndex = index
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
        if (showItems) {
            open()
        } else {
            close()
        }
        itemsView.size(width, viewportHeight.toDouble()).position(0.0, height)
        selectedButton.simulatePressing(showItems)
        expandButton.simulatePressing(showItems)
        expandButton.icon = if (showItems) comboBoxShrinkIcon else comboBoxExpandIcon
        invisibleRect.size(width, height)
        selectedButton.size(width - height, height)
        selectedButton.text = selectedItem?.toString() ?: ""
        expandButton.position(width - height, 0.0).size(height, height)
    }
}
