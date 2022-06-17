package com.soywiz.korge.ui

import com.soywiz.kds.Extra
import com.soywiz.korge.input.onClick
import com.soywiz.korge.input.onDown
import com.soywiz.korge.input.onOut
import com.soywiz.korge.input.onOver
import com.soywiz.korge.input.onUp
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View
import com.soywiz.korge.view.ViewDslMarker
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.position
import com.soywiz.korge.view.size
import com.soywiz.korge.view.solidRect
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.Signal
import com.soywiz.korma.geom.Point
import kotlin.native.concurrent.ThreadLocal

inline fun <T> Container.uiComboBox(
    width: Double = UI_DEFAULT_WIDTH,
    height: Double = UI_DEFAULT_HEIGHT,
    selectedIndex: Int = 0,
    items: List<T>,
    block: @ViewDslMarker UIComboBox<T>.() -> Unit = {}
) = UIComboBox(width, height, selectedIndex, items).addTo(this).apply(block)

@ThreadLocal
var Views.openedComboBox by Extra.Property<UIComboBox<*>?>() { null }

open class UIComboBox<T>(
    width: Double = UI_DEFAULT_WIDTH,
    height: Double = UI_DEFAULT_HEIGHT,
    selectedIndex: Int = 0,
    items: List<T> = listOf(),
) : UIView(width, height) {
    val onSelectionUpdate = Signal<UIComboBox<T>>()

    var selectedIndex by uiObservable(selectedIndex) {
        updateState()
        onSelectionUpdate(this)
    }
    var selectedItem: T?
        get() = items.getOrNull(selectedIndex)
        set(value) {
            selectedIndex = items.indexOf(value)
        }
    var items: List<T> by uiObservable(items) { updateItems() }
    //var itemHeight by uiObservable(height) { updateItemsSize() }
    val itemHeight get() = height
    var viewportHeight by uiObservable(196) { onSizeChanged() }

    private val itemsView = uiScrollable(width, height = 128.0)
    private val verticalList = itemsView.container.uiVerticalList(object : UIVerticalList.Provider {
        override val numItems: Int = items.size
        override val fixedHeight: Double = itemHeight
        override fun getItemHeight(index: Int): Double = fixedHeight
        override fun getItemView(index: Int, vlist: UIVerticalList): View = UIButton(text = items[index].toString(), width = width, height = itemHeight).also {
            it.onClick {
                this@UIComboBox.selectedIndex = index
                this@UIComboBox.close()
            }
        }
    }, width = width)
    private val selectedButton = uiButton(width - height, height, "")
    private val expandButton = uiButton(height, height, icon = comboBoxExpandIcon).position(width - height, 0.0)
    private val invisibleRect = solidRect(width, height, Colors.TRANSPARENT_BLACK)
    private var showItems = false

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
        onSizeChanged()
    }

    fun open() {
        val views = stage?.views
        if (views != null) {
            if (views.openedComboBox != this) {
                views.openedComboBox?.close()
            }
            views?.openedComboBox = this
        }

        containerRoot.addChild(itemsView)

        //itemsView.size(width, viewportHeight.toDouble()).position(0.0, height)
        itemsView.size(width, 128.0.toDouble()).setGlobalXY(localToGlobal(Point(0.0, height)))
        verticalList.size(width, verticalList.height)

        verticalList.invalidateList()

        showItems = true
        updateProps()
    }

    fun close() {
        val views = stage?.views
        if (views != null) {
            if (views.openedComboBox == this) {
                views.openedComboBox = null
            }
        }

        itemsView.removeFromParent()
        showItems = false
        updateProps()
    }

    private fun updateItemsSize() {
        itemsView.container.forEachChildWithIndex { index, child ->
            child.scaledHeight = itemHeight
            child.position(0.0, index * itemHeight)
        }
    }

    private fun updateItems() {
        verticalList.updateList()
        //itemsView.container.removeChildren()
        //for ((index, item) in items.withIndex()) {
        //    itemsView.container.uiButton(
        //        width - 32.0,
        //        itemHeight.toDouble(),
        //        item.toString()
        //    ) {
        //        position(0, index * this@UIComboBox.itemHeight)
        //        onClick {
        //            this@UIComboBox.showItems = false
        //            this@UIComboBox.selectedIndex = index
        //        }
        //    }
        //}
        //itemsView.contentHeight = (items.size * itemHeight).toDouble()
        updateState()
    }

    override fun updateState() {
        onSizeChanged()
        for (i in items.indices) {
            val button = itemsView.container.getChildAtOrNull(i) as? UIButton ?: continue
            button.forcePressed = selectedIndex == i
        }
    }

    override fun onSizeChanged() {
        super.onSizeChanged()
        if (showItems) {
            open()
        } else {
            close()
        }
    }

    private fun updateProps() {
        selectedButton.simulatePressing(showItems)
        expandButton.simulatePressing(showItems)
        expandButton.icon = if (showItems) comboBoxShrinkIcon else comboBoxExpandIcon
        invisibleRect.size(width, height)
        selectedButton.size(width - height, height)
        selectedButton.text = selectedItem?.toString() ?: ""
        expandButton.position(width - height, 0.0).size(height, height)
    }
}
