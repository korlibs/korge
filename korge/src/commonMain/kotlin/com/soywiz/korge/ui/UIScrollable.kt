package com.soywiz.korge.ui

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.klock.seconds
import com.soywiz.kmem.clamp
import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korge.component.decorateOutOverAlpha
import com.soywiz.korge.input.mouse
import com.soywiz.korge.input.onMouseDrag
import com.soywiz.korge.internal.KorgeInternal
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.SolidRect
import com.soywiz.korge.view.ViewDslMarker
import com.soywiz.korge.view.addFixedUpdater
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.container
import com.soywiz.korge.view.fixedSizeContainer
import com.soywiz.korge.view.position
import com.soywiz.korge.view.size
import com.soywiz.korge.view.solidRect
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.async.Signal
import com.soywiz.korma.interpolation.interpolate
import kotlin.math.absoluteValue
import kotlin.math.max

@KorgeExperimental
inline fun Container.uiScrollable(
    width: Double = 256.0,
    height: Double = 256.0,
    config: UIScrollable.() -> Unit = {},
    block: @ViewDslMarker Container.(UIScrollable) -> Unit = {}
): UIScrollable = UIScrollable(width, height)
    .addTo(this).apply(config).also { block(it.container, it) }

// @TODO: Horizontal. And to be able to toggle vertical/horizontal
@KorgeExperimental
open class UIScrollable(width: Double, height: Double) : UIView(width, height) {
    @PublishedApi
    internal var overflowEnabled: Boolean = true

    class MyScrollbarInfo(val scrollable: UIScrollable, val direction: UIDirection, val view: SolidRect) {
        val isHorizontal get() = direction.isHorizontal
        val isVertical get() = direction.isVertical
        val container get() = scrollable.container

        var scrollBarPos: Double by if (isHorizontal) view::x else view::y
        var viewPos: Double by if (isHorizontal) view::x else view::y
            //get() = if (isHorizontal) view.x else view.y
            //set(value) = if (isHorizontal) view.x = value else view.y = value
        var viewScaledSize: Double by if (isHorizontal) view::scaledWidth else view::scaledHeight
            //get() = if (isHorizontal) view.scaledWidth else view.scaledHeight
            //set(value: Double) = if (isHorizontal) view.scaledWidth = value else view.scaledHeight = value

        val scrollRatio: Double get() = size / totalSize
        val scrollbarSize: Double get() = size * scrollRatio

        val scaledSize get() = if (isHorizontal) view.scaledWidth  else view.scaledHeight
        var containerPos: Double by if (isHorizontal) container::x else container::y
            //get() = if (isHorizontal) container.x else container.y
            //set(value) { if (isHorizontal) container.x = value else container.y = value }

        val overflowPixelsBegin get() = if (isHorizontal) scrollable.overflowPixelsLeft else scrollable.overflowPixelsTop
        val overflowPixelsEnd get() = if (isHorizontal) scrollable.overflowPixelsRight else scrollable.overflowPixelsBottom
        val onScrollPosChange = Signal<UIScrollable>()
        val size get() = if (isHorizontal) scrollable.width else scrollable.height
        val shouldBeVisible get() = (size < totalSize)
        val totalSize get() = (container.getLocalBoundsOptimized().let { if (isHorizontal) max(scrollable.width, it.right) else max(scrollable.height, it.bottom) })
            //.also { println("totalSize=$it") }
        val scrollArea get() = totalSize - size
        var position: Double
            get() = -containerPos
            set(value) {
                val oldValue = -containerPos
                val newValue = when {
                    scrollable.overflowEnabled -> -(value.clamp(-overflowPixelsBegin, scrollArea + overflowPixelsEnd))
                    else -> -(value.clamp(0.0, scrollArea))
                }
                if (newValue != oldValue) {
                    containerPos = newValue
                    onScrollPosChange(scrollable)
                }
            }

        @KorgeInternal fun scrollBarPositionToScrollTopLeft(pos: Double): Double {
            val d = size - scaledSize
            if (d == 0.0) return 0.0
            return (pos / d) * scrollArea
        }
        @KorgeInternal fun scrollTopLeftToScrollBarPosition(pos: Double): Double {
            val d = scrollArea
            if (d == 0.0) return 0.0
            return (pos / d) * (size - scaledSize)
        }

        var positionRatio: Double
            get() = position / scrollArea
            set(value) {
                position = scrollArea * value
            }

        var pixelSpeed = 0.0

        var startScrollPos = 0.0
    }

    //private val background = solidRect(width, height, Colors["#161a1d"])
    private val contentContainer = fixedSizeContainer(width, height, clip = true)
    val container = contentContainer.container()
    //private val verticalScrollBar = solidRect(10.0, height / 2, Colors["#57577a"])
    //private val horizontalScrollBar = solidRect(width / 2, 10.0, Colors["#57577a"])

    private val vertical = MyScrollbarInfo(this, UIDirection.VERTICAL, solidRect(10.0, height / 2, Colors["#57577a"]))
    private val horizontal = MyScrollbarInfo(this, UIDirection.HORIZONTAL, solidRect(width / 2, 10.0, Colors["#57577a"]))
    private val infos = arrayOf(horizontal, vertical)

    private val totalHeight: Double get() = vertical.totalSize
    private val totalWidth: Double get() = horizontal.totalSize

    // HORIZONTAL SCROLLBAR
    val onScrollLeftChange get() = horizontal.onScrollPosChange
    val scrollWidth: Double get() = horizontal.totalSize
    var scrollLeft: Double by horizontal::position
    var scrollLeftRatio: Double by horizontal::positionRatio


    // VERTICAL SCROLLBAR
    val onScrollTopChange get() = vertical.onScrollPosChange
    val scrollHeight: Double get() = vertical.totalSize
    var scrollTop: Double by vertical::position
    var scrollTopRatio: Double by vertical::positionRatio

    var frictionRate = 0.75
    var overflowRate = 0.1
    val overflowPixelsVertical get() = height * overflowRate
    val overflowPixelsHorizontal get() = width * overflowRate
    val overflowPixelsTop get() = overflowPixelsVertical
    val overflowPixelsBottom get() = overflowPixelsVertical
    val overflowPixelsLeft get() = overflowPixelsHorizontal
    val overflowPixelsRight get() = overflowPixelsHorizontal
    var timeScrollBar = 0.seconds
    var autohideScrollBar = false
    var scrollBarAlpha = 0.75
    var backgroundColor: RGBA = Colors["#161a1d"]
    var mobileBehaviour = true

    private fun showScrollBar() {
        horizontal.view.alpha = scrollBarAlpha
        vertical.view.alpha = scrollBarAlpha
        timeScrollBar = 0.seconds
    }

    override fun renderInternal(ctx: RenderContext) {
        ctx.useBatcher { batch ->
            batch.drawQuad(ctx.getTex(Bitmaps.white), 0f, 0f, width.toFloat(), height.toFloat(), globalMatrix, colorMul = backgroundColor * renderColorMul)
        }
        super.renderInternal(ctx)
    }

    init {
        container.y = 0.0
        showScrollBar()
        //onScrollTopChange.add { println(it.scrollRatio) }
        onSizeChanged()
        mouse {
            scroll {
                overflowEnabled = false
                showScrollBar()
                val info = when {
                    !horizontal.shouldBeVisible -> vertical
                    !vertical.shouldBeVisible -> horizontal
                    it.isAltDown -> horizontal
                    else -> vertical
                }
                //println(it.lastEvent.scrollDeltaMode)
                //val infoAlt = if (it.isAltDown) vertical else horizontal
                info.position = (info.position + it.scrollDeltaYPixels * (info.size / 16.0))
                //infoAlt.position = (info.position + it.scrollDeltaX * (info.size / 16.0))
                if (it.scrollDeltaYPixels != 0.0) info.pixelSpeed = 0.0
                //if (it.scrollDeltaX != 0.0) infoAlt.pixelSpeed = 0.0
                it.stopPropagation()
            }
        }

        var dragging = false

        for (info in infos) {
            info.view.decorateOutOverAlpha { if (it) 1.0 else scrollBarAlpha }
        }

        for (info in infos) {
            var startScrollBarPos = 0.0
            info.view.onMouseDrag {
                if (!info.shouldBeVisible) return@onMouseDrag
                val dxy = if (info.isHorizontal) it.localDX else it.localDY
                if (it.start) {
                    startScrollBarPos = info.scrollBarPos
                }
                info.position =
                    info.scrollBarPositionToScrollTopLeft(startScrollBarPos + dxy).clamp(0.0, info.scrollArea)
            }
        }

        contentContainer.onMouseDrag {
            overflowEnabled = true
            if (it.start) {
                showScrollBar()
                dragging = true
                for (info in infos) {
                    if (!info.shouldBeVisible || !mobileBehaviour) continue
                    info.startScrollPos = info.position
                    info.pixelSpeed = 0.0
                }
            }

            for (info in infos) {
                if (!info.shouldBeVisible || !mobileBehaviour) continue
                if (info.pixelSpeed.absoluteValue < 0.0001) {
                    info.pixelSpeed = 0.0
                }
            }

            for (info in infos) {
                if (!info.shouldBeVisible || !mobileBehaviour) continue
                val localDXY = if (info.isHorizontal) it.localDX else it.localDY
                info.position = info.startScrollPos - localDXY
                if (it.end) {
                    dragging = false
                    info.pixelSpeed = 300.0
                    val elapsedTime = it.elapsed
                    info.pixelSpeed = -(localDXY * 1.1) / elapsedTime.seconds
                }
            }
        }
        addUpdater {
            if (it.milliseconds == 0.0) return@addUpdater
            //println("horizontal.scrollbarSize=${horizontal.scrollBarPos},${horizontal.scrollbarSize}(${horizontal.view.visible},${horizontal.view.alpha}), vertical.scrollbarSize=${vertical.scrollbarSize}")
            infos.fastForEach { info ->
                info.view.visible = info.shouldBeVisible

                info.viewScaledSize = max(info.scrollbarSize, 10.0)
                info.viewPos = info.scrollTopLeftToScrollBarPosition(info.position)
                //verticalScrollBar.y = scrollTop
                if (info.pixelSpeed.absoluteValue <= 1.0) {
                    info.pixelSpeed = 0.0
                }
                if (info.pixelSpeed != 0.0) {
                    val oldScrollPos = info.position
                    info.position += info.pixelSpeed * it.seconds
                    if (oldScrollPos == info.position) {
                        info.pixelSpeed = 0.0
                    }
                } else {
                    //scrollTop = round(scrollTop)

                    if (!dragging && (info.position < 0.0 || info.position > info.scrollArea)) {
                        //println("scrollRatio=$scrollRatio, scrollTop=$scrollTop")
                        val destScrollPos = if (info.position < 0.0) 0.0 else info.scrollArea
                        if ((destScrollPos - info.position).absoluteValue < 0.1) {
                            info.position = destScrollPos
                        } else {
                            info.position = (0.5 * (it.seconds * 10.0)).interpolate(info.position, destScrollPos)
                        }
                    }

                    if (!dragging && autohideScrollBar) {
                        if (timeScrollBar >= 1.seconds) {
                            info.view.alpha *= 0.9
                        } else {
                            timeScrollBar += it
                        }
                    }
                }
            }
        }
        addFixedUpdater(0.1.seconds) {
            //pixelSpeed *= 0.95
            //pixelSpeed *= 0.75
            infos.fastForEach { it.pixelSpeed *= frictionRate }
        }
    }

    override fun onSizeChanged() {
        contentContainer.size(this.width, this.height)
        vertical.view.position(width - 10.0, 0.0)
        horizontal.view.position(0.0, height - 10.0)
        //background.size(width, height)
        super.onSizeChanged()
    }
}
