package korlibs.korge.ui

import korlibs.korge.view.*
import korlibs.math.geom.*

@Deprecated("Use UINewScrollable")
inline fun Container.uiScrollableArea(
    size: Size = Size(256, 256),
    contentSize: Size = Size(512, 512),
    buttonSize: Number = 32.0,
    verticalScroll: Boolean = true,
    horizontalScroll: Boolean = true,
    config: UIScrollableArea.() -> Unit = {},
    block: @ViewDslMarker Container.(UIScrollableArea) -> Unit = {}
): UIScrollableArea = UIScrollableArea(size, contentSize, buttonSize.toDouble(), verticalScroll, horizontalScroll)
    .addTo(this).apply(config).also { block(it.container, it) }

// @TODO: Optimize this!
// @TODO: Add an actualContainer = this inside Container
@Deprecated("Use UINewScrollable")
open class UIScrollableArea(
    size: Size = Size(256, 256),
    contentSize: Size = Size(512, 512),
    buttonSize: Double = 32.0,
    verticalScroll: Boolean = true,
    horizontalScroll: Boolean = true,
) : UIView(size) {

    var buttonSize by uiObservable(buttonSize) { onSizeChanged() }

    var contentSize by uiObservable(contentSize) { onSizeChanged() }

    var contentWidth get() = contentSize.width; set(value) { contentSize = contentSize.copy(width = value) }
    var contentHeight get() = contentSize.height; set(value) { contentSize = contentSize.copy(height = value) }

    var verticalScroll by uiObservable(verticalScroll) { onSizeChanged() }
    var horizontalScroll by uiObservable(horizontalScroll) { onSizeChanged() }

    var stepRatio by uiObservable(0.125) { onSizeChanged() }

    val viewportWidth: Double get() = if (verticalScroll) width - buttonSize else width
    val viewportHeight: Double get() = if (horizontalScroll) height - buttonSize else height
    val viewportSize: Size get() = Size(viewportWidth, viewportHeight)

    val clipContainer = clipContainer(viewportSize)
    val container = clipContainer.fixedSizeContainer(contentSize)

    val horScrollBar = uiOldScrollBar(Size(size.width, buttonSize)) { onChange { this@UIScrollableArea.onMoved() } }
    val verScrollBar = uiOldScrollBar(Size(buttonSize, size.height)) { onChange { this@UIScrollableArea.onMoved() } }

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
