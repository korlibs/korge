package korlibs.korge.ui

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.io.async.*
import korlibs.korge.input.*
import korlibs.korge.render.*
import korlibs.korge.style.*
import korlibs.korge.ui.UIOldScrollBar.Direction
import korlibs.korge.view.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.math.geom.slice.*
import kotlin.math.*

@Deprecated("Use UINewScrollable")
inline fun Container.uiOldScrollBar(
    size: Size,
    current: Float = 0f,
    pageSize: Float = 1f,
    totalSize: Float = 10f,
    buttonSize: Float = 32f,
    stepSize: Float = pageSize / 10f,
    direction: Direction = Direction.auto(size),
    block: @ViewDslMarker UIOldScrollBar.() -> Unit = {}
): UIOldScrollBar = UIOldScrollBar(size, current, pageSize, totalSize, buttonSize, stepSize, direction).addTo(this).apply(block)

@Deprecated("Use UINewScrollable")
open class UIOldScrollBar(
    size: Size,
    current: Float,
    pageSize: Float,
    totalSize: Float,
    buttonSize: Float = 32f,
    var stepSize: Float = pageSize / 10f,
    direction: Direction = Direction.auto(size)
) : UIView(size) {

    enum class Direction {
        Vertical, Horizontal;

        companion object {
            fun auto(size: Size) = if (size.width > size.height) Horizontal else Vertical
            fun auto(width: Double, height: Double) = if (width > height) Horizontal else Vertical
            fun auto(width: Float, height: Float) = if (width > height) Horizontal else Vertical
        }
    }

    override var unscaledSize: Size by uiObservable(size) { reshape() }

    var buttonSize by uiObservable(buttonSize) { reshape() }
    var direction by uiObservable(direction) { reshape() }

    var current by uiObservable(current) { updatePosition() }
    var pageSize by uiObservable(pageSize) { updatePosition() }
    var totalSize by uiObservable(totalSize) { updatePosition() }

    var buttonVisible by uiObservable(true) {
        upButton.visible = it
        downButton.visible = it
        reshape()
    }

    val isHorizontal get() = direction == Direction.Horizontal
    val isVertical get() = direction == Direction.Vertical

    val buttonWidth: Float get() = if (!buttonVisible) 0f else if (isHorizontal) buttonSize else width
    val buttonHeight: Float get() = if (!buttonVisible) 0f else if (isHorizontal) height else buttonSize
    val trackWidth: Float get() = if (isHorizontal) width - buttonWidth * 2 else width
    val trackHeight: Float get() = if (isHorizontal) height else height - buttonHeight * 2

    val onChange = Signal<UIOldScrollBar>()

    override var ratio: Float
        get() = (current / (totalSize - pageSize)).clamp01()
        set(value) {
            current = value.clamp01() * (totalSize - pageSize)
        }

    protected val background = solidRect(100, 100, styles.buttonBackColor)
    protected val upButton = uiButton(size = Size(16, 16))
    protected val downButton = uiButton(size = Size(16, 16))
    protected val thumb = uiButton(size = Size(16, 16))

    protected val views get() = stage?.views

    override fun renderInternal(ctx: RenderContext) {
        background.color = styles.buttonBackColor
        upButton.icon = if (direction == Direction.Horizontal) styles.iconLeft else styles.iconUp
        downButton.icon = if (direction == Direction.Horizontal) styles.iconRight else styles.iconDown
        super.renderInternal(ctx)
    }

    init {
        reshape()

        upButton.onDown {
            changeCurrent(-stepSize)
            reshape()
        }
        downButton.onDown {
            changeCurrent(+stepSize)
            reshape()
        }
        background.onClick {
            val p = thumb.localMousePos(views!!)
            val pos = if (isHorizontal) p.x else p.y
            changeCurrent(pos.sign * 0.8f * this.pageSize)
        }

        var initRatio = 0f
        var startRatio = 0f
        thumb.onMouseDrag {
            val lmouse = background.localMousePos(views)
            val curPosition = if (isHorizontal) lmouse.x else lmouse.y
            val size = if (isHorizontal) background.width - thumb.width else background.height - thumb.height
            val curRatio = curPosition / size
            if (it.start) {
                initRatio = ratio
                startRatio = curRatio
            }
            ratio = initRatio + (curRatio - startRatio)
            reshape()
        }
    }

    protected fun changeCurrent(value: Float) {
        current = (current + value).clamp(0f, totalSize - pageSize)
    }

    protected fun reshape() {
        if (isHorizontal) {
            background.position(buttonWidth, 0f).size(trackWidth, trackHeight)
            upButton.position(0, 0).size(buttonWidth, buttonHeight)
            downButton.position(widthD - buttonWidth, 0.0).size(buttonWidth, buttonHeight)
        } else {
            background.position(0f, buttonHeight).size(trackWidth, trackHeight)
            upButton.position(0, 0).size(buttonWidth, buttonHeight)
            downButton.position(0.0, heightD - buttonHeight).size(buttonWidth, buttonHeight)
        }
        updatePosition()
    }

    protected fun updatePosition() {
        if (isHorizontal) {
            val thumbWidth = (trackWidth * (pageSize / totalSize)).clamp(4f, trackWidth)
            thumb.position(buttonWidth + (trackWidth - thumbWidth) * ratio, 0f).size(thumbWidth, trackHeight)
        } else {
            val thumbHeight = (trackHeight * (pageSize / totalSize)).clamp(4f, trackHeight)
            thumb.position(0f, buttonHeight + (trackHeight - thumbHeight) * ratio).size(trackWidth, thumbHeight)
        }
        onChange(this)
    }
}

var ViewStyles.iconLeft: RectSlice<Bitmap32> by ViewStyle(Bitmap32(16, 16, Colors.WHITE.premultipliedFast).slice())
var ViewStyles.iconRight: RectSlice<Bitmap32> by ViewStyle(Bitmap32(16, 16, Colors.WHITE.premultipliedFast).slice())
var ViewStyles.iconUp: RectSlice<Bitmap32> by ViewStyle(Bitmap32(16, 16, Colors.WHITE.premultipliedFast).slice())
var ViewStyles.iconDown: RectSlice<Bitmap32> by ViewStyle(Bitmap32(16, 16, Colors.WHITE.premultipliedFast).slice())
