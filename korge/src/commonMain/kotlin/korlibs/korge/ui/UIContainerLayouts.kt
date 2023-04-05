package korlibs.korge.ui

import korlibs.korge.view.*

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
    width: Float = 128f,
    height: Float = 128f,
    block: @ViewDslMarker UIContainer.() -> Unit = {}
) = UIContainer(width, height).addTo(this).apply(block)

open class UIContainer(width: Float, height: Float) : UIBaseContainer(width, height) {
    override fun relayout() {}
}

abstract class UIBaseContainer(width: Float, height: Float) : UIView(width, height) {
    override fun onChildAdded(view: View) {
        relayout()
    }

    override fun onChildChangedSize(view: View) {
        super.onChildChangedSize(view)
        relayout()
    }

    override fun onSizeChanged() {
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
    width: Float = UI_DEFAULT_WIDTH,
    padding: Float = UI_DEFAULT_PADDING,
    adjustSize: Boolean = true,
    block: @ViewDslMarker UIVerticalStack.() -> Unit = {}
) = UIVerticalStack(width, padding, adjustSize).addTo(this).apply(block)

open class UIVerticalStack(
    width: Float = UI_DEFAULT_WIDTH,
    padding: Float = UI_DEFAULT_PADDING,
    adjustSize: Boolean = true,
) : UIVerticalHorizontalStack(width, 0f, padding, adjustSize) {
    override fun relayout() {
        var y = 0f
        forEachChild {
            it.y = y
            if (adjustSize) it.scaledWidthD = widthD
            y += it.height + padding
        }
        height = y
    }
}

inline fun Container.uiHorizontalStack(
    height: Float = UI_DEFAULT_HEIGHT,
    padding: Float = UI_DEFAULT_PADDING,
    adjustHeight: Boolean = true,
    block: @ViewDslMarker UIHorizontalStack.() -> Unit = {}
) = UIHorizontalStack(height, padding, adjustHeight).addTo(this).apply(block)

open class UIHorizontalStack(height: Float = UI_DEFAULT_HEIGHT, padding: Float = UI_DEFAULT_PADDING, adjustHeight: Boolean = true) : UIVerticalHorizontalStack(0f, height, padding, adjustHeight) {
    override fun relayout() {
        var x = 0f
        forEachChild {
            it.x = x
            if (adjustSize) it.scaledHeightD = heightD
            x += it.width + padding
        }
        width = x
    }
}

abstract class UIVerticalHorizontalStack(width: Float = UI_DEFAULT_WIDTH, height: Float = UI_DEFAULT_HEIGHT, padding: Float = UI_DEFAULT_PADDING, val adjustSize: Boolean) : UIContainer(width, height) {
    var padding: Float = padding
        set(value) {
            field = value
            relayout()
        }
}

inline fun Container.uiHorizontalFill(
    width: Float = 128f,
    height: Float = 20f,
    block: @ViewDslMarker UIHorizontalFill.() -> Unit = {}
) = UIHorizontalFill(width, height).addTo(this).apply(block)

open class UIHorizontalFill(width: Float = 128f, height: Float = 20f) : UIContainer(width, height) {
    override fun relayout() {
        var x = 0.0
        val elementWidth = widthD / numChildren
        forEachChild {
            it.xD = x
            it.scaledHeightD = heightD
            it.widthD = elementWidth
            x += elementWidth
        }
    }
}

inline fun Container.uiVerticalFill(
    width: Float = 128f,
    height: Float = 128f,
    block: @ViewDslMarker UIVerticalFill.() -> Unit = {}
) = UIVerticalFill(width, height).addTo(this).apply(block)

open class UIVerticalFill(width: Float = 128f, height: Float = 128f) : UIContainer(width, height) {
    override fun relayout() {
        var y = 0.0
        val elementHeight = heightD / numChildren
        forEachChild {
            it.yD = y
            it.scaledWidthD = widthD
            it.heightD = elementHeight
            y += elementHeight
        }
    }
}

inline fun Container.uiGridFill(
    width: Float = 128f,
    height: Float = 128f,
    cols: Int = 3,
    rows: Int = 3,
    block: @ViewDslMarker UIGridFill.() -> Unit = {}
) = UIGridFill(width, height, cols, rows).addTo(this).apply(block)

open class UIGridFill(width: Float = 128f, height: Float = 128f, cols: Int = 3, rows: Int = 3) : UIContainer(width, height) {
    var cols: Int = cols
    var rows: Int = rows

    override fun relayout() {
        val elementHeight = heightD / rows
        val elementWidth = widthD / cols
        forEachChildWithIndex { index, view ->
            val ex = index % cols
            val ey = index / cols
            view.xy(ex * elementWidth, ey * elementHeight)
            view.size(elementWidth, elementHeight)
        }
    }
}

inline fun Container.uiFillLayeredContainer(
    width: Float = 128f,
    height: Float = 20f,
    block: @ViewDslMarker UIFillLayeredContainer.() -> Unit = {}
) = UIFillLayeredContainer(width, height).addTo(this).apply(block)

open class UIFillLayeredContainer(width: Float = 128f, height: Float = 20f) : UIContainer(width, height) {
    override fun relayout() {
        val width = this.widthD
        val height = this.heightD
        forEachChild {
            it.xy(0, 0)
            it.size(width, height)
        }
    }
}
