package com.soywiz.korge.ui

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korev.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.input.*
import com.soywiz.korge.render.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.text.*
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.interpolation.*
import kotlin.native.concurrent.*

typealias UIDropDown<T> = UIComboBox<T>

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
) : UIFocusableView(width, height) {
    val onSelectionUpdate = Signal<UIComboBox<T>>()

    var selectedIndex by uiObservable(selectedIndex) {
        focusedIndex = it
        onSelectionUpdate(this)
    }
    var focusedIndex by uiObservable(selectedIndex) {
        ensureSelectedIsInVisibleArea(it)
        updateState()
    }
    fun updateFocusIndex(sdir: Int) {
        val dir = if (sdir == 0) +1 else sdir
        //println("updateFocusIndex: sdir=$sdir, filter='$filter'")
        for (n in (if (sdir == 0) 0 else +1) until items.size) {
            val proposedIndex = (focusedIndex + dir * n) umod items.size
            if (matchesFilter(proposedIndex)) {
                focusedIndex = proposedIndex
                break
            }
        }
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
    var filter: String = ""
        set(value) {
            //if (field == value) return
            field = value
            verticalList.invalidateList()
        }

    private val selectedButton = uiButton("", width = width, height = height).also {
        it.textAlignment = TextAlignment.MIDDLE_LEFT
        it.textView.padding = Margin(0f, 8f)
        it.bgColorOut = Colors.WHITE
        it.bgColorOver = MaterialColors.GRAY_100
        it.bgColorDisabled = MaterialColors.GRAY_100
        //it.elevation = false
        it.textColor = MaterialColors.GRAY_800
        it.background.borderColor = MaterialColors.GRAY_400
        it.background.borderSize = 1.0
        it.isFocusable = false
    }
    private val expandButtonIcon = shapeView(buildVectorPath {
        moveTo(Point(0, 0))
        lineTo(Point(20, 0))
        lineTo(Point(10, 10))
        close()
    }, fill = MaterialColors.GRAY_700, renderer = GraphicsRenderer.SYSTEM).centered.position(width - 16.0, height * 0.5).scale(1.0, +1.0)
    //private val expandButton = uiButton(height, height, icon = comboBoxExpandIcon).position(width - height, 0.0)
    private val invisibleRect = solidRect(width, height, Colors.TRANSPARENT)

    private val itemsViewBackground = uiMaterialLayer(width, height = 128.0) {
        radius = RectCorners(0.0, 0.0, 9.0, 9.0)
        zIndex = -1000.0
    }
    private val itemsView = uiScrollable(width, height = 128.0).also {
        it.backgroundColor = Colors.TRANSPARENT
    }
    private val verticalList = itemsView.container.uiVerticalList(object : UIVerticalList.Provider {
        override val numItems: Int = items.size
        override val fixedHeight: Double = itemHeight
        override fun getItemHeight(index: Int): Double = fixedHeight
        override fun getItemView(index: Int, vlist: UIVerticalList): View {
            val itemText = items[index].toString()
            val richText = when {
                filter.isEmpty() -> {
                    RichTextData(itemText, color = Colors.BLACK, font = DefaultTtfFontAsBitmap)
                }
                matchesFilter(index) -> {
                    val highlightColor = MaterialColors.BLUE_800
                    RichTextData.fromHTML(itemText.htmlspecialchars().replace(Regex(Regex.escapeReplacement(filter), RegexOption.IGNORE_CASE)) {
                        "<span color='${highlightColor.hexString}'>${it.value}</span>"
                    }, RichTextData.Style(color = Colors.BLACK, font = DefaultTtfFontAsBitmap))
                }
                else -> {
                    RichTextData(itemText, color = Colors.DARKGREY, font = DefaultTtfFontAsBitmap)
                }
            }
            //val filter = "twe"
            val it = UIButton(richText = richText, width = width, height = itemHeight).apply {
                this.textAlignment = TextAlignment.MIDDLE_LEFT
                this.textView.padding = Margin(0f, 8f)
                this.radius = 0f
                this.bgColorOut = MaterialColors.GRAY_50
                this.bgColorOver = MaterialColors.GRAY_400
                this.bgColorSelected = MaterialColors.LIGHT_BLUE_A100
                //println("selectedIndex == index : ${this@UIComboBox.selectedIndex} == $index : '${this.text}'")
                this.selected = this@UIComboBox.focusedIndex == index
                this.elevation = false
                this.isFocusable = false
                //println("itemText=$itemText, filter=$filter")
            }
            it.onClick {
                //println("CLICKED ON index=$index")
                selectAndClose(index)
            }
            return it
        }
    }, width = width)
    fun matchesFilter(index: Int): Boolean {
        return items[index].toString().contains(filter, ignoreCase = true)
    }
    private var showItems = false

    private fun ensureSelectedIsInVisibleArea(index: Int) {
        verticalList.updateList()
        itemsView.ensureRectIsVisible(
            MRectangle(
                0.0, verticalList.provider.getItemY(index),
                width,
                verticalList.provider.getItemHeight(index)
            )
        )
    }

    init {
        updateItems()
        invisibleRect.onOver {
            selectedButton.simulateOver()
            //expandButton.simulateOver()
            //selectedButton.background.borderColor = MaterialColors.BLUE_300
            //selectedButton.background.borderSize = 2.0
        }
        invisibleRect.onOut {
            selectedButton.simulateOut()
            //expandButton.simulateOut()
            //selectedButton.background.borderColor = MaterialColors.GRAY_400
            //selectedButton.background.borderSize = 1.0
        }
        invisibleRect.onDown {
            selectedButton.simulateDown()
            //expandButton.simulateDown()
        }
        invisibleRect.onUp {
            selectedButton.simulateUp()
            //expandButton.simulateUp()
        }
        invisibleRect.onClick {
            showItems = !showItems
            toggleOpenClose(immediate = false)
        }
        onSizeChanged()
    }

    fun selectAndClose(index: Int) {
        this@UIComboBox.selectedIndex = index
        filter = ""
        close()
    }

    val isOpened: Boolean get() = itemsView.visible

    fun changeParent(set: Boolean) {
        val parent = if (set) stage else null
        if (parent != null) {
            parent.addChild(itemsViewBackground)
            parent.addChild(itemsView)
        } else {
            itemsViewBackground.removeFromParent()
            itemsView.removeFromParent()
        }
        itemsView.zIndex = +100001.0
        itemsViewBackground.zIndex = +100000.0
    }

    fun open(immediate: Boolean = false) {
        verticalList.invalidateList()
        focused = true

        val views = stage?.views
        if (views != null) {
            if (views.openedComboBox != this) {
                views.openedComboBox?.close()
            }
            views?.openedComboBox = this
        }

        if (!isOpened) {
            changeParent(set = true)
            //itemsView.removeFromParent()
            itemsView.visible = true
            itemsViewBackground.visible = true
            if (immediate) {
                itemsView.alphaF = 1.0f
                itemsView.scaleY = 1.0
                itemsViewBackground.alphaF = 1.0f
                itemsViewBackground.scaleY = 1.0
                expandButtonIcon.scaleY = -1.0
                selectedButton.background.borderColor = MaterialColors.BLUE_300
                selectedButton.background.borderSize = 2.0
            } else {
                itemsView.alphaF = 0.0f
                itemsView.scaleY = 0.0
                itemsViewBackground.alphaF = 0.0f
                itemsViewBackground.scaleY = 0.0
                simpleAnimator.cancel().sequence {
                    tween(
                        itemsView::alpha[0.0f, 1.0f],
                        itemsView::scaleY[0.0, 1.0],
                        itemsViewBackground::alpha[0.0f, 1.0f],
                        itemsViewBackground::scaleY[0.0, 1.0],
                        expandButtonIcon::scaleY[-1.0],
                        selectedButton.background::borderColor[MaterialColors.BLUE_300],
                        selectedButton.background::borderSize[2.0],
                        time = 0.25.seconds,
                        easing = Easing.EASE
                    )
                }
            }
        }
        //containerRoot.addChild(itemsView)

        //itemsView.size(width, viewportHeight.toDouble()).position(0.0, height)
        itemsView
            .size(width, viewportHeight.toDouble())
            .globalPos(localToGlobal(Point(0.0, height + 8.0)))
        itemsViewBackground
            .size(width, itemsView.height + 16)
            .globalPos(localToGlobal(Point(0.0, height)))
        verticalList
            .size(width, verticalList.height)

        verticalList.invalidateList()

        showItems = true
        updateProps()
    }

    fun focusNoOpen() {
        focus()
        close()
    }

    fun close(immediate: Boolean = false) {
        //println("CLOSE!")
        //printStackTrace()

        val views = stage?.views
        if (views != null) {
            if (views.openedComboBox == this) {
                views.openedComboBox = null
            }
        }

        //itemsView.removeFromParent()
        if (isOpened) {
            if (immediate) {
                itemsView.alphaF = 0.0f
                itemsView.scaleY = 0.0
                itemsViewBackground.alphaF = 0.0f
                itemsViewBackground.scaleY = 0.0
                expandButtonIcon.scaleY = +1.0
                selectedButton.background.borderColor = MaterialColors.GRAY_400
                selectedButton.background.borderSize = 1.0
                itemsView.visible = false
                changeParent(set = false)
            } else {
                simpleAnimator.cancel().sequence {
                    tween(
                        itemsView::alpha[0.0f],
                        itemsView::scaleY[0.0],
                        itemsViewBackground::alpha[0.0f],
                        itemsViewBackground::scaleY[0.0],
                        expandButtonIcon::scaleY[+1.0],
                        selectedButton.background::borderColor[MaterialColors.GRAY_400],
                        selectedButton.background::borderSize[1.0],
                        time = 0.25.seconds,
                        easing = Easing.EASE
                    )
                    block {
                        itemsView.visible = false
                        changeParent(set = false)
                    }
                }
            }
        }
        showItems = false
        updateProps()
    }

    private fun updateItemsSize() {
        itemsView.container.forEachChildWithIndex { index, child ->
            child.scaledHeight = itemHeight
            child.position(0.0, index * itemHeight)
        }
    }

    var itemsDirty = false

    override fun renderInternal(ctx: RenderContext) {
        if (itemsDirty && showItems) {
            itemsDirty = false
            verticalList.updateList()
            updateState()
        }
        super.renderInternal(ctx)
    }

    private fun updateItems() {
        itemsDirty = true
    }

    override fun updateState() {
        super.updateState()
        onSizeChanged()
        for (i in items.indices) {
            val button = itemsView.container.getChildAtOrNull(i) as? UIButton ?: continue
            button.forcePressed = selectedIndex == i
        }
    }

    override fun onSizeChanged() {
        super.onSizeChanged()
        toggleOpenClose(immediate = true)
    }

    fun toggleOpenClose(immediate: Boolean) {
        if (showItems) {
            open(immediate = immediate)
        } else {
            close(immediate = immediate)
        }
    }

    private fun updateProps() {
        selectedButton.simulatePressing(showItems)
        //expandButton.simulatePressing(showItems)
        //expandButton.icon = if (showItems) comboBoxShrinkIcon else comboBoxExpandIcon
        //expandButtonIcon.bitmap = if (showItems) comboBoxShrinkIcon else comboBoxExpandIcon
        invisibleRect.size(width, height)
        selectedButton.size(width, height)
        selectedButton.text = selectedItem?.toString() ?: ""
        expandButtonIcon.position(width - 16.0, height * 0.5)
        //expandButton.position(width - height, 0.0).size(height, height)
    }

    override fun focusChanged(value: Boolean) {
        if (value) {
            open()
        } else {
            close()
        }
    }

    init {
        val comboBox = this
        keys {
            typed {
                if (!focused) return@typed
                if (it.key == Key.BACKSPACE) return@typed
                if (it.characters().firstOrNull()?.code ?: 0 < 32) return@typed
                if (!isOpened) {
                    this@UIComboBox.open()
                } else {
                    //println(it.characters().map { it.code })
                    filter += it.characters()
                    this@UIComboBox.updateFocusIndex(0)
                }
            }
            down(Key.BACKSPACE) {
                if (!focused) return@down
                filter = filter.dropLast(1)
                this@UIComboBox.updateFocusIndex(0)
            }
            down(Key.UP, Key.DOWN) {
                if (!focused) return@down
                if (!isOpened) this@UIComboBox.open()
                this@UIComboBox.updateFocusIndex(if (it.key == Key.UP) -1 else +1)
            }
            down(Key.RETURN, Key.SPACE) {
                if (!focused) return@down
                when {
                    !isOpened -> this@UIComboBox.open()
                    else -> this@UIComboBox.selectAndClose(focusedIndex)
                }
            }
            down(Key.ESCAPE) {
                if (focused) {
                    if (isOpened) {
                        this@UIComboBox.close()
                    } else {
                        this@UIComboBox.blur()
                    }
                }
            }
        }
    }
}
