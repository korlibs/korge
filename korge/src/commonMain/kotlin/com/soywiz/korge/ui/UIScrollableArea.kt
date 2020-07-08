package com.soywiz.korge.ui

import com.soywiz.korge.view.*

@Deprecated("Kotlin/Native boxes inline+Number")
inline fun Container.uiScrollableArea(
    width: Number,
    height: Number,
    contentWidth: Number = 512.0,
    contentHeight: Number = 512.0,
    buttonSize: Number = 32.0,
    verticalScroll: Boolean = true,
    horizontalScroll: Boolean = true,
    horSkin: ScrollBarSkin = defaultHorScrollBarSkin,
    verSkin: ScrollBarSkin = defaultVerScrollBarSkin,
    config: UIScrollableArea.() -> Unit = {},
    block: Container.() -> Unit = {}
): UIScrollableArea = uiScrollableArea(
    width.toDouble(), height.toDouble(), contentWidth.toDouble(), contentHeight.toDouble(), buttonSize.toDouble(),
    verticalScroll, horizontalScroll, horSkin, verSkin, config, block
)

inline fun Container.uiScrollableArea(
    width: Double = 256.0,
    height: Double = 256.0,
    contentWidth: Double = 512.0,
    contentHeight: Double = 512.0,
    buttonSize: Double = 32.0,
    verticalScroll: Boolean = true,
    horizontalScroll: Boolean = true,
    horSkin: ScrollBarSkin = defaultHorScrollBarSkin,
    verSkin: ScrollBarSkin = defaultVerScrollBarSkin,
    config: UIScrollableArea.() -> Unit = {},
    block: Container.() -> Unit = {}
): UIScrollableArea = UIScrollableArea(width, height, contentWidth, contentHeight, buttonSize, verticalScroll, horizontalScroll, horSkin, verSkin)
    .addTo(this).apply(config).also { block(it.container) }

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
    horSkin: ScrollBarSkin = DefaultHorScrollBarSkin,
    verSkin: ScrollBarSkin = DefaultVerScrollBarSkin
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

    val horScrollBar = uiScrollBar(width, buttonSize, skin = horSkin) { onChange { this@UIScrollableArea.onMoved() } }
    val verScrollBar = uiScrollBar(buttonSize, height, skin = verSkin) { onChange { this@UIScrollableArea.onMoved() } }

    init {
        calculateSizes()
    }

    override fun onSizeChanged() {
        super.onSizeChanged()
        calculateSizes()
    }

    private fun calculateSizes() {
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
