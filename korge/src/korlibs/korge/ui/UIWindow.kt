package korlibs.korge.ui

import korlibs.datastructure.iterators.*
import korlibs.image.color.*
import korlibs.image.text.*
import korlibs.korge.annotations.*
import korlibs.korge.input.*
import korlibs.korge.internal.*
import korlibs.korge.render.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*
import korlibs.render.*
import korlibs.time.*


@KorgeExperimental
inline fun Container.uiWindow(
    title: String,
    size: Size = Size(256, 256),
    configure: @ViewDslMarker UIWindow.() -> Unit = {},
    block: @ViewDslMarker Container.(UIWindow) -> Unit = {},
): UIWindow = UIWindow(title, size).addTo(this).apply(configure).also { block(it.container.container, it) }

@KorgeExperimental
class UIWindow(title: String, size: Size = Size(256, 256)) : UIContainer(size) {
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

    private val bgMaterial = uiMaterialLayer(size) {
        radius = RectCorners(12.0)
        colorMul = if (isFocused) Colors["#394674"] else Colors["#999"]
        shadowColor = Colors.BLACK.withAd(0.9)
        shadowRadius = 20.0
    }
    private val bg = renderableView(size, ViewRenderer {
        val isFocused = this@UIWindow.isFocused
        //ctx2d.rect(0.0, 0.0, this@UIWindow.width, this@UIWindow.height, colorBg.withAd(renderAlpha))
        //ctx2d.rect(0.0, 0.0, this@UIWindow.width, titleHeight.toDouble(), colorBgTitle.withAd(renderAlpha))
        val borderSize = if (isFocused) 2.0 else 1.0
        val borderColor = if (isFocused) borderColorFocused else borderColorNoFocused
        //ctx2d.rectOutline(-borderSize, -borderSize, this@UIWindow.width + borderSize * 2, this@UIWindow.height + borderSize * 2, borderSize, borderColor.withAd(renderAlpha))
    })
    private val titleContainer = fixedSizeContainer(Size(width, titleHeight))
    private val titleView = titleContainer.textBlock(RichTextData(title), align = TextAlignment.MIDDLE_LEFT).xy(12, 0).size(width, titleHeight)
    private val closeButton = titleContainer.uiButton(icon = UIIcons.CROSS, size = Size(titleHeight - buttonSeparation * 2, titleHeight - buttonSeparation * 2)) {
        radiusRatio = Ratio.ONE
        elevation = false
        bgColorOut = MaterialColors.RED_600
        bgColorOver = MaterialColors.RED_800
        onClick { closeAnimated() }
    }
    var title: String by titleView::plainText
    val container = uiScrollable(Size(width, height - titleHeight)).position(0.0, titleHeight).also {
        it.backgroundColor = Colors["#161a1d"]
    }
    var isCloseable: Boolean = true
        set(value) {
            field = value
            closeButton.visible = value
        }

    class ScaleHandler(val window: UIWindow, val anchor: Anchor) {
        val isCorner = (anchor.sx == anchor.sy)

        @OptIn(KorgeUntested::class)
        val view = window.solidRect(Size.ZERO, Colors.TRANSPARENT) {
            val sh = this
            val anchor = this@ScaleHandler.anchor
            anchor(Anchor.CENTER)
            cursor = Cursor.fromAnchorResize(anchor)
            // @TODO: clamping shouldn't affect (we should use it.start and get initial values to compute based on start and not on deltas)
            sh.draggable {
                val obounds: Rectangle = window.getGlobalBounds()
                var bounds: Rectangle = obounds
                when {
                    anchor.sx < 0.5f -> {
                        bounds = bounds.copyBounds(left = it.cx)
                        if (bounds.width !in window.minWidth..window.maxWidth) {
                            bounds = bounds.copyBounds(left = obounds.left)
                        }
                    }

                    anchor.sx > 0.5f -> {
                        bounds = bounds.copyBounds(right = it.cx)
                        bounds = bounds.copy(width = bounds.width.clamp(window.minWidth, window.maxWidth))
                    }

                    else -> Unit
                }
                when {
                    anchor.sy < 0.5f -> {
                        bounds = bounds.copyBounds(top = it.cy)
                        if (bounds.height !in window.minHeight..window.maxHeight) {
                            bounds = bounds.copyBounds(top = obounds.top)
                        }
                    }

                    anchor.sy > 0.5f -> {
                        bounds = bounds.copyBounds(bottom = it.cy)
                        bounds = bounds.copy(height = bounds.height.clamp(window.minHeight, window.maxHeight))
                    }

                    else -> Unit
                }
                window.setGlobalBounds(bounds)
                if (it.end) {
                    resized()
                }
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

        private fun resized() {
            resized(window.width, window.height)
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
        cursor = Cursor.RESIZE_NORTH
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
        cursor = Cursor.RESIZE_EAST
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
        cursor = Cursor.RESIZE_SOUTH
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
        cursor = Cursor.RESIZE_SOUTH_EAST
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
        bgMaterial.size(width, height)
        bg.size(width, height)
        titleContainer.size(width, titleHeight)
        container.size(width, height - titleHeight)
        closeButton.position(width - titleHeight - buttonSeparation, buttonSeparation)
        scaleHandlers.fastForEach { it.resized(width, height) }
    }

    fun close() {
        removeFromParent()
    }

    suspend fun closeAnimated() {
        tween(this::height[0.0], this::alphaF[0.0f], time = 300.milliseconds)
        removeFromParent()
    }
}
