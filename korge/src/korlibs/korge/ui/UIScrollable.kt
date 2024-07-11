package korlibs.korge.ui

import korlibs.datastructure.iterators.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.io.async.*
import korlibs.korge.component.*
import korlibs.korge.input.*
import korlibs.korge.internal.*
import korlibs.korge.render.*
import korlibs.korge.view.*
import korlibs.korge.view.property.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*
import korlibs.time.*
import kotlin.math.*
import kotlin.time.*

inline fun Container.uiScrollable(
    size: Size = Size(256, 256),
    config: UIScrollable.() -> Unit = {},
    cache: Boolean = true,
    block: @ViewDslMarker Container.(UIScrollable) -> Unit = {},
): UIScrollable = UIScrollable(size, cache)
    .addTo(this).apply(config).also { block(it.container, it) }

open class UIScrollable(size: Size, cache: Boolean = true) : UIView(size, cache = cache) {
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

        val scaledSize: Double get() = if (isHorizontal) view.scaledWidth else view.scaledHeight
        var containerPos: Double by if (isHorizontal) container::x else container::y
        //get() = if (isHorizontal) container.x else container.y
        //set(value) { if (isHorizontal) container.x = value else container.y = value }

        val overflowPixelsBegin: Double get() = if (isHorizontal) scrollable.overflowPixelsLeft else scrollable.overflowPixelsTop
        val overflowPixelsEnd: Double get() = if (isHorizontal) scrollable.overflowPixelsRight else scrollable.overflowPixelsBottom
        val onScrollPosChange = Signal<UIScrollable>()
        val size: Double get() = if (isHorizontal) scrollable.width else scrollable.height
        val shouldBeVisible get() = (size < totalSize)
        val totalSize: Double
            get() = (container.getLocalBounds().let { if (isHorizontal) max(scrollable.width, it.right) else max(scrollable.height, it.bottom) })

        //.also { println("totalSize=$it") }
        val scrollArea: Double get() = totalSize - size
        val positionEnd: Double get() = position + size
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

        @KorgeInternal
        fun scrollBarPositionToScrollTopLeft(pos: Double): Double {
            val d = size - scaledSize
            if (d == 0.0) return 0.0
            return (pos / d) * scrollArea
        }

        @KorgeInternal
        fun scrollTopLeftToScrollBarPosition(pos: Double): Double {
            val d = scrollArea
            if (d == 0.0) return 0.0
            return (pos / d) * (size - scaledSize)
        }

        fun ensurePositionIsVisible(position: Double, anchor: Double = 0.5) {
            ensureRangeIsVisible(position, position, anchor)
        }

        fun ensureRangeIsVisible(start: Double, end: Double, anchor: Double = 0.5) {
            if (start !in this.position..this.positionEnd || end !in this.position..this.positionEnd) {
                this.position = (start - size * anchor).clamp(0.0, scrollArea)
            }
        }

        var positionRatio: Double
            get() = position / scrollArea
            set(value) {
                position = scrollArea * value
            }

        var pixelSpeed: Double = 0.0

        var startScrollPos: Double = 0.0
    }

    //private val background = solidRect(width, height, Colors["#161a1d"])
    private val contentContainer = fixedSizeContainer(size, clip = true)
    val container = contentContainer.container(cull = true)
    //private val verticalScrollBar = solidRect(10.0, height / 2, Colors["#57577a"])
    //private val horizontalScrollBar = solidRect(width / 2, 10.0, Colors["#57577a"])

    private val vertical = MyScrollbarInfo(this, UIDirection.VERTICAL, solidRect(Size(10.0, size.height / 2), Colors["#57577a"]))
    private val horizontal = MyScrollbarInfo(this, UIDirection.HORIZONTAL, solidRect(Size(size.width / 2, 10.0), Colors["#57577a"]))
    private val infos = arrayOf(horizontal, vertical)

    private val totalHeight: Double get() = vertical.totalSize
    private val totalWidth: Double get() = horizontal.totalSize

    // HORIZONTAL SCROLLBAR
    val onScrollLeftChange: Signal<UIScrollable> get() = horizontal.onScrollPosChange
    val scrollWidth: Double get() = horizontal.totalSize
    var scrollLeft: Double by horizontal::position
    var scrollLeftRatio: Double by horizontal::positionRatio

    // VERTICAL SCROLLBAR
    val onScrollTopChange: Signal<UIScrollable> get() = vertical.onScrollPosChange
    val scrollHeight: Double get() = vertical.totalSize
    var scrollTop: Double by vertical::position
    var scrollTopRatio: Double by vertical::positionRatio

    @ViewProperty
    var frictionRate = 0.75

    @ViewProperty
    var overflowRate = 0.1
    val overflowPixelsVertical: Double get() = height * overflowRate
    val overflowPixelsHorizontal: Double get() = width * overflowRate
    val overflowPixelsTop: Double get() = overflowPixelsVertical
    val overflowPixelsBottom: Double get() = overflowPixelsVertical
    val overflowPixelsLeft: Double get() = overflowPixelsHorizontal
    val overflowPixelsRight: Double get() = overflowPixelsHorizontal

    @ViewProperty
    var containerX: Double by container::x

    @ViewProperty
    var containerY: Double by container::y

    @ViewProperty
    var timeScrollBar: Duration
        set(value) { fastTimeScrollBar = value.fast }
        get() = fastTimeScrollBar.toDuration()

    private var fastTimeScrollBar: FastDuration = 0.fastSeconds

    @ViewProperty
    var autohideScrollBar = false

    @ViewProperty
    var scrollBarAlpha = 0.75

    @ViewProperty
    var backgroundColor: RGBA = Colors["#161a1d"]

    @ViewProperty
    var mobileBehaviour = true

    private fun showScrollBar() {
        horizontal.view.alpha = scrollBarAlpha
        vertical.view.alpha = scrollBarAlpha
        fastTimeScrollBar = 0.fastSeconds
    }

    override fun renderInternal(ctx: RenderContext) {
        if (backgroundColor != Colors.TRANSPARENT) {
            ctx.useBatcher { batch ->
                batch.drawQuad(ctx.getTex(Bitmaps.white), 0f, 0f, width.toFloat(), height.toFloat(), globalMatrix, colorMul = backgroundColor * renderColorMul)
            }
        }
        super.renderInternal(ctx)
    }

    fun ensurePointIsVisible(x: Double, y: Double, anchor: Anchor = Anchor.CENTER) {
        horizontal.ensurePositionIsVisible(x, anchor.sx)
        vertical.ensurePositionIsVisible(y, anchor.sy)
    }

    fun ensureRectIsVisible(rect: Rectangle, anchor: Anchor = Anchor.CENTER) {
        horizontal.ensureRangeIsVisible(rect.left, rect.right, anchor.sx)
        vertical.ensureRangeIsVisible(rect.top, rect.bottom, anchor.sy)
    }

    fun ensureViewIsVisible(view: View, anchor: Anchor = Anchor.CENTER) {
        ensureRectIsVisible(view.getBounds(this), anchor)
        scrollParentsToMakeVisible()
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
                val axisY = when {
                    !horizontal.shouldBeVisible -> vertical
                    !vertical.shouldBeVisible -> horizontal
                    it.isAltDown -> horizontal
                    else -> vertical
                }
                //val axisX = if (axisY == vertical) horizontal else vertical
                val axisX = if (it.isAltDown) vertical else horizontal

                //println(it.lastEvent.scrollDeltaMode)
                //val infoAlt = if (it.isAltDown) vertical else horizontal
                if (axisX.shouldBeVisible) {
                    axisX.position = (axisX.position + it.scrollDeltaXPixels * (axisY.size / 16f))
                    //infoAlt.position = (info.position + it.scrollDeltaX * (info.size / 16.0))
                    if (it.scrollDeltaXPixels != 0f) axisX.pixelSpeed = 0.0
                }
                if (axisY.shouldBeVisible) {
                    axisY.position = (axisY.position + it.scrollDeltaYPixels * (axisY.size / 16f))
                    //infoAlt.position = (info.position + it.scrollDeltaX * (info.size / 16.0))
                    if (it.scrollDeltaYPixels != 0f) axisY.pixelSpeed = 0.0
                    //if (it.scrollDeltaX != 0.0) infoAlt.pixelSpeed = 0.0
                }
                it.stopPropagation()
                invalidateRender()
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
            //println("DRAG: $it")
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
                    info.pixelSpeed = -(localDXY * 1.1f) / elapsedTime.seconds.toFloat()
                }
            }
        }
        addFastUpdater {
            if (it.milliseconds == 0.0) return@addFastUpdater
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
                    info.position += info.pixelSpeed * it.seconds.toFloat()
                    if (oldScrollPos == info.position) {
                        info.pixelSpeed = 0.0
                    }
                } else {
                    //scrollTop = round(scrollTop)

                    if (!dragging && (info.position < 0f || info.position > info.scrollArea)) {
                        //println("scrollRatio=$scrollRatio, scrollTop=$scrollTop")
                        val destScrollPos = if (info.position < 0.0) 0.0 else info.scrollArea
                        if ((destScrollPos - info.position).absoluteValue < 0.1) {
                            info.position = destScrollPos
                        } else {
                            info.position =
                                (0.5f * (it.seconds * 10f)).toRatio().interpolate(info.position, destScrollPos)
                        }
                    }

                    if (!dragging && autohideScrollBar) {
                        if (fastTimeScrollBar >= 1.fastSeconds) {
                            info.view.alphaF *= 0.9f
                        } else {
                            fastTimeScrollBar += it
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
        super.onSizeChanged()
        contentContainer.size(this.width, this.height)
        vertical.view.position(width - 10.0, 0.0)
        horizontal.view.position(0.0, height - 10.0)
        //println(vertical.overflowPixelsEnd)
        //background.size(width, height)
        invalidateRender()
    }
}
