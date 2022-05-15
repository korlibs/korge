package com.soywiz.korge.ui

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.ViewDslMarker
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.clipContainer
import com.soywiz.korge.view.fixedSizeContainer
import com.soywiz.korge.view.position
import com.soywiz.korge.view.size

@Deprecated("Use UINewScrollable")
inline fun Container.uiScrollableArea(
    width: Double = 256.0,
    height: Double = 256.0,
    contentWidth: Double = 512.0,
    contentHeight: Double = 512.0,
    buttonSize: Double = 32.0,
    verticalScroll: Boolean = true,
    horizontalScroll: Boolean = true,
    config: UIScrollableArea.() -> Unit = {},
    block: @ViewDslMarker Container.(UIScrollableArea) -> Unit = {}
): UIScrollableArea = UIScrollableArea(width, height, contentWidth, contentHeight, buttonSize, verticalScroll, horizontalScroll)
    .addTo(this).apply(config).also { block(it.container, it) }

// @TODO: Optimize this!
// @TODO: Add an actualContainer = this inside Container
@Deprecated("Use UINewScrollable")
open class UIScrollableArea(
    width: Double = 256.0,
    height: Double = 256.0,
    contentWidth: Double = 512.0,
    contentHeight: Double = 512.0,
    buttonSize: Double = 32.0,
    verticalScroll: Boolean = true,
    horizontalScroll: Boolean = true,
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

    val horScrollBar = uiScrollBar(width, buttonSize) { onChange { this@UIScrollableArea.onMoved() } }
    val verScrollBar = uiScrollBar(buttonSize, height) { onChange { this@UIScrollableArea.onMoved() } }

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
        horScrollBar.position(0.0, height - buttonSize)
        horScrollBar.visible = horizontalScroll

        verScrollBar.size(buttonSize, viewportHeight)
        verScrollBar.position(width - buttonSize, 0.0)
        verScrollBar.visible = verticalScroll
    }

    protected open fun onMoved() {
        container.x = -horScrollBar.current
        container.y = -verScrollBar.current
    }
}
