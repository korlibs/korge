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
    private val bg = renderableView(width, height, ViewRenderer {
        ctx2d.rect(0.0, 0.0, this@UIWindow.width, this@UIWindow.height, Colors["#6f6e85"])
        ctx2d.rect(0.0, 0.0, this@UIWindow.width, titleHeight.toDouble(), Colors["#4c4b5b"])
        ctx2d.rectOutline(-1.0, -1.0, this@UIWindow.width + 2.0, this@UIWindow.height + 2.0, 1.0, Colors.BLACK)
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
