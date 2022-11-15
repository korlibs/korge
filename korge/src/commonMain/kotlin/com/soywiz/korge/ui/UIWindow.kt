package com.soywiz.korge.ui

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.klock.milliseconds
import com.soywiz.kmem.clamp
import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korge.input.DraggableInfo
import com.soywiz.korge.input.cursor
import com.soywiz.korge.input.draggable
import com.soywiz.korge.input.mouse
import com.soywiz.korge.input.onClick
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.*
import com.soywiz.korgw.GameWindow
import com.soywiz.korim.color.Colors
import com.soywiz.korim.text.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.layout.HorizontalUiLayout.percent

@KorgeExperimental
inline fun Container.uiWindow(
    title: String,
    width: Double = 256.0,
    height: Double = 256.0,
    configure: @ViewDslMarker UIWindow.() -> Unit = {},
    block: @ViewDslMarker Container.(UIWindow) -> Unit = {},
): UIWindow = UIWindow(title, width, height).addTo(this).apply(configure).also { block(it.container.container, it) }

@KorgeExperimental
class UIWindow(title: String, width: Double = 256.0, height: Double = 256.0) : UIContainer(width, height) {
    private val titleHeight = 32.0
    private val buttonSeparation = 6.0
    val isFocused get() = this.index == (parent?.numChildren ?: 0) -1
    private val colorBg = Colors["#6f6e85"]
    private val colorBgTitle = Colors["#6f6e85"]
    private val borderColorFocused = Colors["#471175"]
    private val borderColorNoFocused = Colors.BLACK
    var minWidth = 128.0
    var minHeight = 64.0
    var maxWidth = 4096.0
    var maxHeight = 4096.0

    private val bgMaterial = uiMaterialLayer(width, height) {
        radius = RectCorners(12.0)
        colorMul = if (isFocused) Colors["#394674"] else Colors["#999"]
        shadowColor = Colors.BLACK.withAd(0.9)
        shadowRadius = 20.0
    }
    private val bg = renderableView(width, height, ViewRenderer {
        val isFocused = this@UIWindow.isFocused
        //ctx2d.rect(0.0, 0.0, this@UIWindow.width, this@UIWindow.height, colorBg.withAd(renderAlpha))
        //ctx2d.rect(0.0, 0.0, this@UIWindow.width, titleHeight.toDouble(), colorBgTitle.withAd(renderAlpha))
        val borderSize = if (isFocused) 2.0 else 1.0
        val borderColor = if (isFocused) borderColorFocused else borderColorNoFocused
        //ctx2d.rectOutline(-borderSize, -borderSize, this@UIWindow.width + borderSize * 2, this@UIWindow.height + borderSize * 2, borderSize, borderColor.withAd(renderAlpha))
    })
    private val titleContainer = fixedSizeContainer(width, titleHeight)
    private val titleView = titleContainer.textBlock(RichTextData(title), align = TextAlignment.MIDDLE_LEFT).xy(6, 0).size(width, titleHeight)
    private val closeButton = titleContainer.uiButton("X", width = titleHeight - buttonSeparation * 2, height = titleHeight - buttonSeparation * 2) {
        radius = 100.percent
        colorMul = Colors["#b2434e"]
        onClick { closeAnimated() }
    }
    var title: String by titleView::plainText
    val container = uiScrollable(width, height - titleHeight).position(0.0, titleHeight)
    var isCloseable: Boolean = true
        set(value) {
            field = value
            closeButton.visible = value
        }

    class ScaleHandler(val window: UIWindow, val anchor: Anchor) {
        val isCorner = (anchor.sx == anchor.sy)

        val view = window.solidRect(0.0, 0.0, Colors.TRANSPARENT_BLACK) {
            val sh = this
            anchor(Anchor.CENTER)
            cursor = GameWindow.Cursor.fromAnchorResize(anchor)
            // @TODO: clamping shouldn't affect (we should use it.start and get initial values to compute based on start and not on deltas)
            sh.draggable {
                val obounds = window.getGlobalBounds().clone()
                val bounds = obounds.clone()
                when {
                    anchor.sx < 0.5 -> {
                        bounds.leftKeepingRight = it.cx
                        if (bounds.width !in window.minWidth..window.maxWidth) {
                            bounds.leftKeepingRight = obounds.leftKeepingRight
                        }
                    }
                    anchor.sx > 0.5 -> {
                        bounds.right = it.cx
                        bounds.width = bounds.width.clamp(window.minWidth, window.maxWidth)
                    }
                    else -> Unit
                }
                when {
                    anchor.sy < 0.5 -> {
                        bounds.topKeepingBottom = it.cy
                        if (bounds.height !in window.minHeight..window.maxHeight) {
                            bounds.topKeepingBottom = obounds.topKeepingBottom
                        }
                    }
                    anchor.sy > 0.5 -> {
                        bounds.bottom = it.cy
                        bounds.height = bounds.height.clamp(window.minHeight, window.maxHeight)
                    }
                    else -> Unit
                }
                window.setGlobalBounds(bounds)
            }
        }

        private fun getExpectedX(): Double = window.width * anchor.sx + when (anchor.sx) {
            0.0 -> -2.0
            1.0 -> +2.0
            else -> 0.0
        }
        private fun getExpectedY(): Double = window.height * anchor.sy + when (anchor.sy) {
            0.0 -> -2.0
            1.0 -> +2.0
            else -> 0.0
        }

        fun resized(width: Double, height: Double) {
            view
                .position(getExpectedX(), getExpectedY())
                .size(
                    when {
                        anchor.sx == 0.5 -> width
                        //corner -> 14.0
                        else -> 10.0
                    }, when {
                        anchor.sy == 0.5 -> height
                        //corner -> 14.0
                        else -> 10.0
                    }
                )
            //view.bounds(width / 2, 0.0, width, 10.0)
        }
    }

    private val anchors = listOf(
        Anchor.TOP_LEFT, Anchor.TOP, Anchor.TOP_RIGHT, Anchor.RIGHT,
        Anchor.BOTTOM_RIGHT, Anchor.BOTTOM, Anchor.BOTTOM_LEFT, Anchor.LEFT
    )

    private val scaleHandlers = anchors.map { ScaleHandler(this, it) }
    override fun renderInternal(ctx: RenderContext) {
        ctx.flush()
        super.renderInternal(ctx)
        ctx.flush()
    }

    /*
    private val scaleHandlerTop = solidRect(10.0, 10.0, Colors.TRANSPARENT_BLACK) {
        val sh = this
        anchor(Anchor.MIDDLE_LEFT)
        position(0.0, 0.0)
        cursor = GameWindow.Cursor.RESIZE_NORTH
        sh.draggable {
            sh.x = 0.0
            sh.y = 0.0

            val newHeight = (this@UIWindow.scaledHeight + it.deltaDy).clamp(minHeight, maxHeight)
            val realDelta = this@UIWindow.scaledHeight - newHeight

            this@UIWindow.y += realDelta
            this@UIWindow.scaledHeight = newHeight
            ////this@UIWindow.scaledWidth = sh.x
        }
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
    */

    var dragProcessor: (info: DraggableInfo) -> Unit = {
        it.view.xy(it.viewNextXY)
    }

    init {
        this.mouse.down { this.bringToTop() }
        this.draggable(titleContainer, autoMove = false) { dragProcessor(it) }
        onSizeChanged()
    }

    override fun onSizeChanged() {
        bgMaterial.setSize(width, height)
        bg.setSize(width, height)
        titleContainer.setSize(width, titleHeight)
        container.setSize(width, height - titleHeight)
        closeButton.position(width - titleHeight - buttonSeparation, buttonSeparation)
        scaleHandlers.fastForEach { it.resized(width, height) }
    }

    fun close() {
        removeFromParent()
    }

    suspend fun closeAnimated() {
        tween(this::height[0.0], this::alpha[0.0], time = 300.milliseconds)
        removeFromParent()
    }
}
