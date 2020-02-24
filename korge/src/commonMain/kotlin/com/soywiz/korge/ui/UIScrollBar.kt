package com.soywiz.korge.ui

import com.soywiz.kmem.*
import com.soywiz.korge.input.*
import com.soywiz.korge.ui.UIScrollBar.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import kotlin.math.*

inline fun Container.uiScrollBar(
    width: Number,
    height: Number,
    current: Number = 0.0,
    pageSize: Number = 1.0,
    totalSize: Number = 10.0,
    buttonSize: Number = 32.0,
    stepSize: Double = pageSize.toDouble() / 10.0,
    direction: Direction = if (width.toDouble() > height.toDouble()) Direction.Horizontal else Direction.Vertical,
    skin: ScrollBarSkin = if (direction == Direction.Horizontal) defaultHorScrollBarSkin else defaultVerScrollBarSkin,
    block: @ViewsDslMarker UIScrollBar.() -> Unit = {}
): UIScrollBar = UIScrollBar(
    width.toDouble(),
    height.toDouble(),
    current.toDouble(),
    pageSize.toDouble(),
    totalSize.toDouble(),
    buttonSize.toDouble(),
    stepSize,
    direction,
    skin
).addTo(this).apply(block)

open class UIScrollBar(
    width: Double,
    height: Double,
    current: Double,
    pageSize: Double,
    totalSize: Double,
    buttonSize: Double = 32.0,
    var stepSize: Double = pageSize / 10.0,
    direction: Direction = if (width > height) Direction.Horizontal else Direction.Vertical,
    skin: ScrollBarSkin = if (direction == Direction.Horizontal) DefaultHorScrollBarSkin else DefaultVerScrollBarSkin
) : UIView() {

    enum class Direction { Vertical, Horizontal }

    override var width by uiObservable(width) { reshape() }
    override var height by uiObservable(height) { reshape() }
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

    val buttonWidth get() = if (!buttonVisible) 0.0 else if (isHorizontal) buttonSize else width
    val buttonHeight get() = if (!buttonVisible) 0.0 else if (isHorizontal) height else buttonSize
    val trackWidth get() = if (isHorizontal) width - buttonWidth * 2 else width
    val trackHeight get() = if (isHorizontal) height else height - buttonHeight * 2

    val onChange = Signal<UIScrollBar>()

    override var ratio: Double
        get() = (current / (totalSize - pageSize)).clamp01()
        set(value) {
            current = value.clamp01() * (totalSize - pageSize)
        }

    protected val background = solidRect(100, 100, skin.backColor)
    protected val upButton = iconButton(16, 16, skin.upSkin, skin.upIcon)
    protected val downButton = iconButton(16, 16, skin.downSkin, skin.downIcon)
    protected val thumb = uiButton(16, 16, skin.thumbSkin)

    protected val views get() = stage?.views

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
            val pos = if (isHorizontal) thumb.localMouseX(views!!) else thumb.localMouseY(views!!)
            changeCurrent(pos.sign * 0.8 * this.pageSize)
        }

        val tempP = Point()
        var initRatio = 0.0
        var startRatio = 0.0
        thumb.onMouseDrag {
            val lmouse = background.localMouseXY(views, tempP)
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

    protected fun changeCurrent(value: Double) {
        current = (current + value).clamp(0.0, totalSize - pageSize)
    }

    protected fun reshape() {
        if (isHorizontal) {
            background.position(buttonWidth, 0).size(trackWidth, trackHeight)
            upButton.position(0, 0).size(buttonWidth, buttonHeight)
            downButton.position(width - buttonWidth, 0).size(buttonWidth, buttonHeight)
        } else {
            background.position(0, buttonHeight).size(trackWidth, trackHeight)
            upButton.position(0, 0).size(buttonWidth, buttonHeight)
            downButton.position(0, height - buttonHeight).size(buttonWidth, buttonHeight)
        }
        updatePosition()
    }

    protected fun updatePosition() {
        if (isHorizontal) {
            val thumbWidth = (trackWidth * (pageSize / totalSize)).clamp(4.0, trackWidth)
            thumb.position(buttonWidth + (trackWidth - thumbWidth) * ratio, 0).size(thumbWidth, trackHeight)
        } else {
            val thumbHeight = (trackHeight * (pageSize / totalSize)).clamp(4.0, trackHeight)
            thumb.position(0, buttonHeight + (trackHeight - thumbHeight) * ratio).size(trackWidth, thumbHeight)
        }
        onChange(this)
    }
}

data class ScrollBarSkin(
    val thumbSkin: UISkin,
    val upSkin: UISkin,
    val downSkin: UISkin,
    val upIcon: IconSkin,
    val downIcon: IconSkin,
    val backColor: RGBA
)

val DefaultVerScrollBarSkin by lazy {
    ScrollBarSkin(
        thumbSkin = DefaultUISkin,
        upSkin = DefaultUISkin,
        downSkin = DefaultUISkin,
        upIcon = DefaultUpSkin,
        downIcon = DefaultDownSkin,
        backColor = Colors.DARKGREY
    )
}

val DefaultHorScrollBarSkin by lazy {
    ScrollBarSkin(
        thumbSkin = DefaultUISkin,
        upSkin = DefaultUISkin,
        downSkin = DefaultUISkin,
        upIcon = DefaultLeftSkin,
        downIcon = DefaultRightSkin,
        backColor = Colors.DARKGREY
    )
}

var View.defaultVerScrollBarSkin: ScrollBarSkin by defaultElement(DefaultVerScrollBarSkin)
var View.defaultHorScrollBarSkin: ScrollBarSkin by defaultElement(DefaultHorScrollBarSkin)
