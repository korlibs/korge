package com.soywiz.korge.ui

import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korge.annotations.*
import com.soywiz.korge.input.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*

@KorgeExperimental
inline fun Container.uiWindow(
    title: String,
    width: Double = 256.0,
    height: Double = 256.0,
    configure: @ViewDslMarker UIWindow.() -> Unit = {},
    block: @ViewDslMarker Container.(UIWindow) -> Unit = {},
): UIWindow = UIWindow(title, width, height).addTo(this).apply(configure).also { block(it.container.container, it) }

@KorgeExperimental
class UIWindow(title: String, width: Double = 256.0, height: Double = 256.0) : UIView(width, height) {
    private val titleHeight = 32.0
    private val buttonSeparation = 3.0
    val isFocused get() = this.index == (parent?.numChildren ?: 0) -1
    private val colorBg = Colors["#6f6e85"]
    private val colorBgTitle = Colors["#6f6e85"]
    private val borderColorFocused = Colors["#471175"]
    private val borderColorNoFocused = Colors.BLACK
    var minWidth = 128.0
    var minHeight = 64.0
    var maxWidth = 4096.0
    var maxHeight = 4096.0

    private val bg = renderableView(width, height, ViewRenderer {
        val isFocused = this@UIWindow.isFocused
        ctx2d.rect(0.0, 0.0, this@UIWindow.width, this@UIWindow.height, colorBg.withAd(renderAlpha))
        ctx2d.rect(0.0, 0.0, this@UIWindow.width, titleHeight.toDouble(), colorBgTitle.withAd(renderAlpha))
        val borderSize = if (isFocused) 2.0 else 1.0
        val borderColor = if (isFocused) borderColorFocused else borderColorNoFocused
        ctx2d.rectOutline(-borderSize, -borderSize, this@UIWindow.width + borderSize * 2, this@UIWindow.height + borderSize * 2, borderSize, borderColor.withAd(renderAlpha))
    })
    private val titleContainer = fixedSizeContainer(width, titleHeight)
    private val titleView = titleContainer.text(title).position(6, 6)
    private val closeButton = titleContainer.uiButton(titleHeight - buttonSeparation * 2, titleHeight - buttonSeparation * 2, text = "X") {
        colorMul = Colors["#b2434e"]
        onClick { closeAnimated() }
    }
    var title: String by titleView::text
    val container = uiNewScrollable(width, height - titleHeight).position(0.0, titleHeight)
    var isCloseable: Boolean = true
        set(value) {
            field = value
            closeButton.visible = value
        }

    private val scaleHandlerRight = solidRect(10.0, 10.0, Colors.TRANSPARENT_BLACK) {
        val sh = this
        anchor(Anchor.TOP_CENTER)
        position(width, 0.0)
        cursor = GameWindow.Cursor.RESIZE_EAST
        sh.draggable {
            sh.x = sh.x.clamp(minWidth, maxWidth)
            sh.y = 0.0
            this@UIWindow.scaledWidth = sh.x
        }
    }
    private val scaleHandlerBottom = solidRect(10.0, 10.0, Colors.TRANSPARENT_BLACK) {
        val sh = this
        anchor(Anchor.MIDDLE_LEFT)
        position(0.0, height)
        cursor = GameWindow.Cursor.RESIZE_SOUTH
        sh.draggable {
            sh.x = 0.0
            sh.y = sh.y.clamp(minHeight, maxHeight)
            this@UIWindow.scaledHeight = sh.y
        }
    }
    private val scaleHandler = solidRect(10.0, 10.0, Colors.TRANSPARENT_BLACK) {
        val sh = this
        anchor(Anchor.MIDDLE_CENTER)
        position(width, height)
        cursor = GameWindow.Cursor.RESIZE_SOUTH_EAST
        sh.draggable {
            sh.x = sh.x.clamp(minWidth, maxWidth)
            sh.y = sh.y.clamp(minHeight, maxHeight)
            this@UIWindow.setSize(sh.x, sh.y)
        }
    }

    init {
        this.mouse.down { this.bringToTop() }
        this.draggable(titleContainer)
        onSizeChanged()
    }

    override fun onSizeChanged() {
        bg.setSize(width, height)
        titleContainer.setSize(width, titleHeight)
        container.setSize(width, height - titleHeight)
        closeButton.position(width - titleHeight - buttonSeparation, buttonSeparation)
        scaleHandler.position(width + 4.0, height + 4.0).size(10.0, 10.0)
        scaleHandlerRight.position(width + 4.0, 0.0).size(10.0, height)
        scaleHandlerBottom.position(0.0, height + 4.0).size(width, 10.0)
    }

    fun close() {
        removeFromParent()
    }

    suspend fun closeAnimated() {
        tween(this::height[0.0], this::alpha[0.0], time = 300.milliseconds)
        removeFromParent()
    }
}
