package korlibs.korge.ui

import korlibs.korge.view.*
import korlibs.korge.view.property.*
import korlibs.math.geom.*

/**
 * Equivalent to CSS flexbox `justify-content` values.
 *
 * See: <https://css-tricks.com/wp-content/uploads/2022/02/css-flexbox-poster.png>
 */
enum class JustifyContent {
    /**
     * `ABC------------`
     *
     * All elements are grouped together in the start without spacing between them.
     */
    START,
    /**
     * `------------ABC`
     *
     * All elements are grouped together in the end without spacing between them.
     */
    END,
    /**
     * `------ABC------`
     *
     * All elements are centered together without spacing between them.
     */
    CENTER,
    /**
     * `A------B------C`
     *
     * Space between elements are the same, and no space on tips.
     */
    SPACE_BETWEEN,
    /**
     * `--A----B----C--`
     *
     * Space between elements are the same, and tips, half the space.
     */
    SPACE_AROUND,
    /**
     * `---A---B---C---`
     *
     * Space between elements, and tips are the same.
     */
    SPACE_EVENLY,
}

/**
 * Equivalent to CSS `align-items` and `align-self` properties:
 *
 * See: <https://css-tricks.com/wp-content/uploads/2022/02/css-flexbox-poster.png>
 */
enum class AlignItems {
    /**
     * ```
     * . A  B  C  D
     * .    B
     * .    B
     * ```
     */
    START,
    /**
     * ```
     * .    B
     * .    B
     * . A  B  C  D
     * ```
     */
    END,
    /**
     * ```
     * .    B
     * . A  B  C  D
     * .    B
     * ```
     */
    CENTER,
    /**
     * ```
     * . A  B  C  D
     * . A  B  C  D
     * . A  B  C  D
     * ```
     */
    STRETCH,
    /**
     * ```
     * .
     * . A  B  C  D   __ Text is here
     * .    B
     * ```
     */
    BASELINE,
}

inline fun Container.uiContainer(
    size: Size = Size(128, 128),
    block: @ViewDslMarker UIContainer.() -> Unit = {}
) = UIContainer(size).addTo(this).apply(block)

open class UIContainer(size: Size) : UIBaseContainer(size) {
    override fun relayout() {}
}

abstract class UIBaseContainer(size: Size) : UIView(size) {
    override fun onChildAdded(view: View) {
        super.onChildAdded(view)
        relayout()
    }

    override fun onChildChangedSize(view: View) {
        super.onChildChangedSize(view)
        relayout()
    }

    override fun onSizeChanged() {
        super.onSizeChanged()
        relayout()
    }

    abstract fun relayout()

    var deferredRendering: Boolean? = true
    //var deferredRendering: Boolean? = false

    //override fun renderInternal(ctx: RenderContext) {
    //    ctx.batch.mode(if (deferredRendering == true) BatchBuilder2D.RenderMode.DEFERRED else null) {
    //        super.renderInternal(ctx)
    //    }
    //    //ctx.flush()
    //}
}

inline fun Container.uiVerticalStack(
    width: Float? = null,
    padding: Float = UI_DEFAULT_PADDING,
    adjustSize: Boolean = true,
    block: @ViewDslMarker UIVerticalStack.() -> Unit = {}
) = UIVerticalStack(width, padding, adjustSize).addTo(this).apply(block)

open class UIVerticalStack(
    forcedWidth: Float? = null,
    padding: Float = UI_DEFAULT_PADDING,
    adjustSize: Boolean = true,
) : UIVerticalHorizontalStack(Size(forcedWidth ?: 100f, 0f), padding, adjustSize) {
    var forcedWidth: Float? = forcedWidth
        set(value) {
            if (field != value) {
                field = value
                relayout()
                //(parent as? UIContainer?)?.relayout()
            }
        }

    override fun relayout() {
        var y = 0f
        var bb = BoundsBuilder()
        forEachChild {
            it.y = y
            if (adjustSize && forcedWidth != null) it.scaledWidthD = widthD
            y += it.height + padding
            bb += it.getBounds(this@UIVerticalStack)
        }
        unscaledSize = Size(if (forcedWidth == null) bb.xmax else forcedWidth!!, y)
    }
}

inline fun Container.uiHorizontalStack(
    height: Float? = null,
    padding: Float = UI_DEFAULT_PADDING,
    adjustHeight: Boolean = true,
    block: @ViewDslMarker UIHorizontalStack.() -> Unit = {}
) = UIHorizontalStack(height, padding, adjustHeight).addTo(this).apply(block)

open class UIHorizontalStack(
    forcedHeight: Float? = null,
    padding: Float = UI_DEFAULT_PADDING,
    adjustHeight: Boolean = true
) : UIVerticalHorizontalStack(Size(0f, forcedHeight ?: 32f), padding, adjustHeight) {
    var forcedHeight: Float? = forcedHeight
        set(value) {
            if (field != value) {
                field = value
                relayout()
                //(parent as? UIContainer?)?.relayout()
            }
        }

    override fun relayout() {
        var x = 0f
        var bb = BoundsBuilder()
        forEachChild {
            it.x = x
            if (adjustSize && forcedHeight != null) it.scaledHeightD = heightD
            x += it.width + padding
            bb += it.getBounds(this@UIHorizontalStack)
        }
        //println("forcedHeight=$forcedHeight!")
        //val newUnscaledSize =
        //println("UIHorizontalStack.relayout: unscaledSize=$unscaledSize, newUnscaledSize=$newUnscaledSize")
        unscaledSize = Size(x, if (forcedHeight == null) bb.ymax else forcedHeight!!)
    }
}

abstract class UIVerticalHorizontalStack(size: Size = UI_DEFAULT_SIZE, padding: Float = UI_DEFAULT_PADDING, val adjustSize: Boolean) : UIContainer(size) {
    var padding: Float = padding
        set(value) {
            field = value
            relayout()
        }
}

inline fun Container.uiHorizontalFill(
    size: Size = Size(128, 20),
    block: @ViewDslMarker UIHorizontalFill.() -> Unit = {}
) = UIHorizontalFill(size).addTo(this).apply(block)

open class UIHorizontalFill(size: Size = Size(128, 20)) : UIContainer(size) {
    override fun relayout() {
        var x = 0.0
        val elementWidth = width / numChildren
        forEachChild {
            it.xD = x
            it.scaledHeightD = heightD
            it.unscaledWidth = elementWidth
            x += elementWidth
        }
    }
}

inline fun Container.uiVerticalFill(
    size: Size = Size(128, 20),
    block: @ViewDslMarker UIVerticalFill.() -> Unit = {}
) = UIVerticalFill(size).addTo(this).apply(block)

open class UIVerticalFill(size: Size = Size(128, 128)) : UIContainer(size) {
    override fun relayout() {
        var y = 0.0
        val elementHeight = height / numChildren
        forEachChild {
            it.yD = y
            it.scaledWidthD = widthD
            it.unscaledHeight = elementHeight
            y += elementHeight
        }
    }
}

inline fun Container.uiGridFill(
    size: Size = Size(128, 128),
    cols: Int = 3,
    rows: Int = 3,
    padding: Margin = Margin.ZERO,
    direction: UIDirection = UIDirection.ROW,
    block: @ViewDslMarker UIGridFill.() -> Unit = {}
) = UIGridFill(size, cols, rows, padding, direction).addTo(this).apply(block)

open class UIGridFill(
    size: Size = Size(128, 128),
    cols: Int = 3, rows: Int = 3,
    padding: Margin = Margin.ZERO,
    direction: UIDirection = UIDirection.ROW,
) : UIContainer(size) {
    @ViewProperty
    var cols: Int by UIObservable(cols) { relayout() }
    @ViewProperty
    var rows: Int by UIObservable(rows) { relayout() }
    @ViewProperty
    var padding: Margin by UIObservable(padding) { relayout() }
    @ViewProperty
    var direction: UIDirection by UIObservable(direction) { relayout() }

    override fun relayout() {
        val width = widthD
        val height = heightD
        val paddingH = padding.leftPlusRight
        val paddingV = padding.topPlusBottom
        val elementHeight = (height - paddingV * (rows - 1)) / rows
        val elementWidth = (width - paddingH * (cols - 1)) / cols
        val elementHeightP = elementHeight + paddingV
        val elementWidthP = elementWidth + paddingH
        forEachChildWithIndex { index, view ->
            val ex: Int = when {
                direction.isHorizontal -> index % cols
                else -> index / rows
            }
            val ey: Int = when {
                direction.isHorizontal -> index / cols
                else -> index % rows
            }

            val px = ex * elementWidthP
            val py = ey * elementHeightP

            val rpx = if (direction.isHorizontal && direction.isReverse) width - px - elementWidth else px
            val rpy = if (direction.isVertical && direction.isReverse) height - py - elementHeight else py

            view.xy(rpx, rpy)
            view.size(elementWidth, elementHeight)
        }
    }
}

inline fun Container.uiFillLayeredContainer(
    size: Size = Size(128, 20),
    block: @ViewDslMarker UIFillLayeredContainer.() -> Unit = {}
) = UIFillLayeredContainer(size).addTo(this).apply(block)

open class UIFillLayeredContainer(size: Size = Size(128, 20)) : UIContainer(size) {
    override fun relayout() {
        val width = this.widthD
        val height = this.heightD
        forEachChild {
            it.xy(0, 0)
            it.size(width, height)
        }
    }
}
