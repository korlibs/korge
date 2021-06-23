package com.soywiz.korge.ui

import com.soywiz.korge.annotations.*
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*

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
    private val bg = renderableView(width, height, ViewRenderer {
        val isFocused = this@UIWindow.isFocused
        ctx2d.rect(0.0, 0.0, this@UIWindow.width, this@UIWindow.height, colorBg)
        ctx2d.rect(0.0, 0.0, this@UIWindow.width, titleHeight.toDouble(), colorBgTitle)
        val borderSize = if (isFocused) 2.0 else 1.0
        val borderColor = if (isFocused) borderColorFocused else borderColorNoFocused
        ctx2d.rectOutline(-borderSize, -borderSize, this@UIWindow.width + borderSize * 2, this@UIWindow.height + borderSize * 2, borderSize, borderColor)
    })
    private val titleContainer = fixedSizeContainer(width, titleHeight)
    private val titleView = titleContainer.text(title).position(6, 6)
    private val closeButton = titleContainer.uiButton(titleHeight - buttonSeparation * 2, titleHeight - buttonSeparation * 2, text = "X") {
        colorMul = Colors["#b2434e"]
        onClick { close() }
    }
    var title: String by titleView::text
    val container = uiNewScrollable(width, height - titleHeight).position(0.0, titleHeight)

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
    }

    fun close() {
        removeFromParent()
    }
}
