package com.soywiz.korge.ui

import com.soywiz.kmem.clamp
import com.soywiz.kmem.clamp01
import com.soywiz.korge.input.onClick
import com.soywiz.korge.input.onDown
import com.soywiz.korge.input.onMouseDrag
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.ui.UIScrollBar.Direction
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.ViewDslMarker
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.position
import com.soywiz.korge.view.size
import com.soywiz.korge.view.solidRect
import com.soywiz.korio.async.Signal
import com.soywiz.korma.geom.MPoint
import kotlin.math.sign

@Deprecated("Use UINewScrollable")
inline fun Container.uiScrollBar(
    width: Double,
    height: Double,
    current: Double = 0.0,
    pageSize: Double = 1.0,
    totalSize: Double = 10.0,
    buttonSize: Double = 32.0,
    stepSize: Double = pageSize / 10.0,
    direction: Direction = Direction.auto(width, height),
    block: @ViewDslMarker UIScrollBar.() -> Unit = {}
): UIScrollBar = UIScrollBar(width, height, current, pageSize, totalSize, buttonSize, stepSize, direction).addTo(this).apply(block)

@Deprecated("Use UINewScrollable")
open class UIScrollBar(
    width: Double,
    height: Double,
    current: Double,
    pageSize: Double,
    totalSize: Double,
    buttonSize: Double = 32.0,
    var stepSize: Double = pageSize / 10.0,
    direction: Direction = Direction.auto(width, height)
) : UIView() {

    enum class Direction {
        Vertical, Horizontal;

        companion object {
            fun auto(width: Double, height: Double) = if (width > height) Horizontal else Vertical
        }
    }

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

    protected val background = solidRect(100, 100, buttonBackColor)
    protected val upButton = uiButton(16.0, 16.0)
    protected val downButton = uiButton(16.0, 16.0)
    protected val thumb = uiButton(16.0, 16.0)

    protected val views get() = stage?.views

    override fun renderInternal(ctx: RenderContext) {
        background.color = buttonBackColor
        upButton.icon = if (direction == Direction.Horizontal) this.iconLeft else this.iconUp
        downButton.icon = if (direction == Direction.Horizontal) this.iconRight else this.iconDown
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
            val pos = if (isHorizontal) thumb.localMouseX(views!!) else thumb.localMouseY(views!!)
            changeCurrent(pos.sign * 0.8 * this.pageSize)
        }

        val tempP = MPoint()
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
            background.position(buttonWidth, 0.0).size(trackWidth, trackHeight)
            upButton.position(0, 0).size(buttonWidth, buttonHeight)
            downButton.position(width - buttonWidth, 0.0).size(buttonWidth, buttonHeight)
        } else {
            background.position(0.0, buttonHeight).size(trackWidth, trackHeight)
            upButton.position(0, 0).size(buttonWidth, buttonHeight)
            downButton.position(0.0, height - buttonHeight).size(buttonWidth, buttonHeight)
        }
        updatePosition()
    }

    protected fun updatePosition() {
        if (isHorizontal) {
            val thumbWidth = (trackWidth * (pageSize / totalSize)).clamp(4.0, trackWidth)
            thumb.position(buttonWidth + (trackWidth - thumbWidth) * ratio, 0.0).size(thumbWidth, trackHeight)
        } else {
            val thumbHeight = (trackHeight * (pageSize / totalSize)).clamp(4.0, trackHeight)
            thumb.position(0.0, buttonHeight + (trackHeight - thumbHeight) * ratio).size(trackWidth, thumbHeight)
        }
        onChange(this)
    }
}
