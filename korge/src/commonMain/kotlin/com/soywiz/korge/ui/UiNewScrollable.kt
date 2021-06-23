package com.soywiz.korge.ui

import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korge.annotations.*
import com.soywiz.korge.component.*
import com.soywiz.korge.input.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korma.interpolation.*
import kotlin.math.*

@KorgeExperimental
inline fun Container.uiNewScrollable(
    width: Double = 256.0,
    height: Double = 256.0,
    config: UiNewScrollable.() -> Unit = {},
    block: @ViewDslMarker Container.(UiNewScrollable) -> Unit = {}
): UiNewScrollable = UiNewScrollable(width, height)
    .addTo(this).apply(config).also { block(it.container, it) }

// @TODO: Horizontal. And to be able to toggle vertical/horizontal
@KorgeExperimental
open class UiNewScrollable(width: Double, height: Double) : UIView(width, height) {
    private val background = solidRect(width, height, Colors["#161a1d"])
    private val contentContainer = fixedSizeContainer(width, height, clip = true)
    val container = contentContainer.container()
    private val verticalScrollBar = solidRect(10.0, height / 2, Colors["#57577a"])

    private val totalHeight: Double
        get() = container.getLocalBoundsOptimized().bottom

    @KorgeInternal
    val scrollHeightArea: Double
        get() = totalHeight - height

    private val verticalScrollRatio get() = height / totalHeight
    private val scrollbarHeight get() = height * verticalScrollRatio

    val scrollHeight: Double
        get() = totalHeight

    val onScrollTopChange = Signal<UiNewScrollable>()

    var scrollTop: Double
        get() = -container.y
        set(value) {
            val oldValue = container.y
            val newValue = -(value.clamp(-overflowPixelsTop, scrollHeightArea + overflowPixelsBottom))
            if (newValue != oldValue) {
                container.y = newValue
                onScrollTopChange(this)
            }
        }

    var scrollTopRatio: Double
        get() = scrollTop / scrollHeightArea
        set(value) {
            scrollTop = scrollHeightArea * value
        }

    private var scrollBarPos: Double
        get() = verticalScrollBar.y
        set(value) {
            verticalScrollBar.y = value
        }

    @KorgeInternal
    fun scrollBarPositionToScrollTop(pos: Double): Double {
        return (pos / (height - verticalScrollBar.scaledHeight)) * scrollHeightArea
    }

    @KorgeInternal
    fun scrollTopToScrollBarPosition(pos: Double): Double {
        return (pos / scrollHeightArea) * (height - verticalScrollBar.scaledHeight)
    }

    private var pixelSpeed = 0.0
    var frictionRate = 0.75
    var overflowRate = 0.1
    val overflowPixels get() = height * overflowRate
    val overflowPixelsTop get() = overflowPixels
    val overflowPixelsBottom get() = overflowPixels
    val overflowPixelsLeft get() = overflowPixels
    val overflowPixelsRight get() = overflowPixels
    var timeScrollBar = 0.seconds
    var autohideScrollBar = false
    var scrollBarAlpha = 0.75
    var backgroundColor: RGBA
        get() = background.colorMul
        set(value: RGBA) { background.colorMul = value }

    private fun showScrollBar() {
        verticalScrollBar.alpha = scrollBarAlpha
        timeScrollBar = 0.seconds
    }

    init {
        container.y = 0.0
        showScrollBar()
        //onScrollTopChange.add { println(it.scrollRatio) }
        onSizeChanged()
        mouse {
            scroll {
                showScrollBar()
                scrollTop = (scrollTop + it.scrollDeltaY * (height / 16.0))
                pixelSpeed = 0.0
            }
        }
        var startScrollTop = 0.0
        var dragging = false

        verticalScrollBar.decorateOutOverAlpha({ 1.0 }, { scrollBarAlpha })

        var startScrollBarPos = 0.0
        verticalScrollBar.onMouseDrag {
            if (it.start) {
                startScrollBarPos = scrollBarPos
            }
            scrollTop = scrollBarPositionToScrollTop(startScrollBarPos + it.dy).clamp(0.0, scrollHeightArea)
        }

        contentContainer.onMouseDrag {
            if (it.start) {
                showScrollBar()
                dragging = true
                startScrollTop = scrollTop
                pixelSpeed = 0.0
            }

            if (pixelSpeed.absoluteValue < 0.0001) {
                pixelSpeed = 0.0
            }

            scrollTop = startScrollTop - it.localDY
            if (it.end) {
                dragging = false
                pixelSpeed = 300.0
                val elapsedTime = it.elapsed
                pixelSpeed = -(it.localDY * 1.1) / elapsedTime.seconds
            }
        }
        addUpdater {
            if (it.milliseconds == 0.0) return@addUpdater
            verticalScrollBar.scaledHeight = scrollbarHeight
            verticalScrollBar.y = scrollTopToScrollBarPosition(scrollTop)
            //verticalScrollBar.y = scrollTop
            if (pixelSpeed.absoluteValue <= 1.0) {
                pixelSpeed = 0.0
            }
            if (pixelSpeed != 0.0) {
                val oldScrollTop = scrollTop
                scrollTop += pixelSpeed * it.seconds
                if (oldScrollTop == scrollTop) {
                    pixelSpeed = 0.0
                }
            } else {
                //scrollTop = round(scrollTop)

                if (!dragging && (scrollTop < 0.0 || scrollTop > scrollHeightArea)) {
                    //println("scrollRatio=$scrollRatio, scrollTop=$scrollTop")
                    val destScrollTop = if (scrollTop < 0.0) 0.0 else scrollHeightArea
                    if ((destScrollTop - scrollTop).absoluteValue < 0.1) {
                        scrollTop = destScrollTop
                    } else {
                        scrollTop = (0.5 * (it.seconds * 10.0)).interpolate(scrollTop, destScrollTop)
                    }
                }

                if (!dragging && autohideScrollBar) {
                    if (timeScrollBar >= 1.seconds) {
                        verticalScrollBar.alpha *= 0.9
                    } else {
                        timeScrollBar += it
                    }
                }
            }
        }
        addFixedUpdater(0.1.seconds) {
            //pixelSpeed *= 0.95
            //pixelSpeed *= 0.75
            pixelSpeed *= frictionRate
        }
    }

    override fun onSizeChanged() {
        contentContainer.size(this.width, this.height)
        verticalScrollBar.position(width - 10.0, 0.0)
        background.size(width, height)
        super.onSizeChanged()
    }
}
