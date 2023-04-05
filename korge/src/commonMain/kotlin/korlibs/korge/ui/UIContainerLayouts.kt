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
    width: Double = 128.0,
    height: Double = 128.0,
    block: @ViewDslMarker UIContainer.() -> Unit = {}
) = UIContainer(width, height).addTo(this).apply(block)

open class UIContainer(width: Double, height: Double) : UIBaseContainer(width, height) {
    override fun relayout() {}
}

abstract class UIBaseContainer(width: Double, height: Double) : UIView(width, height) {
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
    width: Double = UI_DEFAULT_WIDTH,
    padding: Double = UI_DEFAULT_PADDING,
    adjustSize: Boolean = true,
    block: @ViewDslMarker UIVerticalStack.() -> Unit = {}
) = UIVerticalStack(width, padding, adjustSize).addTo(this).apply(block)

open class UIVerticalStack(
    width: Double = UI_DEFAULT_WIDTH,
    padding: Double = UI_DEFAULT_PADDING,
    adjustSize: Boolean = true,
) : UIVerticalHorizontalStack(width, 0.0, padding, adjustSize) {
    override fun relayout() {
        var y = 0.0
        forEachChild {
            it.yD = y
            if (adjustSize) it.scaledWidth = width
            y += it.height + padding
        }
        height = y
    }
}

inline fun Container.uiHorizontalStack(
    height: Double = UI_DEFAULT_HEIGHT,
    padding: Double = UI_DEFAULT_PADDING,
    adjustHeight: Boolean = true,
    block: @ViewDslMarker UIHorizontalStack.() -> Unit = {}
) = UIHorizontalStack(height, padding, adjustHeight).addTo(this).apply(block)

open class UIHorizontalStack(height: Double = UI_DEFAULT_HEIGHT, padding: Double = UI_DEFAULT_PADDING, adjustHeight: Boolean = true) : UIVerticalHorizontalStack(0.0, height, padding, adjustHeight) {
    override fun relayout() {
        var x = 0.0
        forEachChild {
            it.xD = x
            if (adjustSize) it.scaledHeight = height
            x += it.width + padding
        }
        width = x
    }
}

abstract class UIVerticalHorizontalStack(width: Double = UI_DEFAULT_WIDTH, height: Double = UI_DEFAULT_HEIGHT, padding: Double = UI_DEFAULT_PADDING, val adjustSize: Boolean) : UIContainer(width, height) {
    var padding: Double = padding
        set(value) {
            field = value
            relayout()
        }
}

inline fun Container.uiHorizontalFill(
    width: Double = 128.0,
    height: Double = 20.0,
    block: @ViewDslMarker UIHorizontalFill.() -> Unit = {}
) = UIHorizontalFill(width, height).addTo(this).apply(block)

open class UIHorizontalFill(width: Double = 128.0, height: Double = 20.0) : UIContainer(width, height) {
    override fun relayout() {
        var x = 0.0
        val elementWidth = width / numChildren
        forEachChild {
            it.xD = x
            it.scaledHeight = height
            it.width = elementWidth
            x += elementWidth
        }
    }
}

inline fun Container.uiVerticalFill(
    width: Double = 128.0,
    height: Double = 128.0,
    block: @ViewDslMarker UIVerticalFill.() -> Unit = {}
) = UIVerticalFill(width, height).addTo(this).apply(block)

open class UIVerticalFill(width: Double = 128.0, height: Double = 128.0) : UIContainer(width, height) {
    override fun relayout() {
        var y = 0.0
        val elementHeight = height / numChildren
        forEachChild {
            it.yD = y
            it.scaledWidth = width
            it.height = elementHeight
            y += elementHeight
        }
    }
}

inline fun Container.uiGridFill(
    width: Double = 128.0,
    height: Double = 128.0,
    cols: Int = 3,
    rows: Int = 3,
    block: @ViewDslMarker UIGridFill.() -> Unit = {}
) = UIGridFill(width, height, cols, rows).addTo(this).apply(block)

open class UIGridFill(width: Double = 128.0, height: Double = 128.0, cols: Int = 3, rows: Int = 3) : UIContainer(width, height) {
    var cols: Int = cols
    var rows: Int = rows

    override fun relayout() {
        val elementHeight = height / rows
        val elementWidth = width / cols
        forEachChildWithIndex { index, view ->
            val ex = index % cols
            val ey = index / cols
            view.xy(ex * elementWidth, ey * elementHeight)
            view.size(elementWidth, elementHeight)
        }
    }
}

inline fun Container.uiFillLayeredContainer(
    width: Double = 128.0,
    height: Double = 20.0,
    block: @ViewDslMarker UIFillLayeredContainer.() -> Unit = {}
) = UIFillLayeredContainer(width, height).addTo(this).apply(block)

open class UIFillLayeredContainer(width: Double = 128.0, height: Double = 20.0) : UIContainer(width, height) {
    override fun relayout() {
        val width = this.width
        val height = this.height
        forEachChild {
            it.xy(0, 0)
            it.size(width, height)
        }
    }
}
