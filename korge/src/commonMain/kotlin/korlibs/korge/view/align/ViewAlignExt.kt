package korlibs.korge.view.align

import korlibs.korge.view.*


/** Chainable method returning this that sets [this] View in the middle between [x1] and [x2] */
fun <T : View> T.centerXBetween(x1: Double, x2: Double): T {
    this.xD = (x2 + x1 - this.widthD) / 2
    return this
}
fun <T : View> T.centerXBetween(x1: Float, x2: Float): T = centerXBetween(x1.toDouble(), x2.toDouble())
fun <T : View> T.centerXBetween(x1: Int, x2: Int): T = centerXBetween(x1.toDouble(), x2.toDouble())

/** Chainable method returning this that sets [this] View in the middle between [y1] and [y2] */
fun <T : View> T.centerYBetween(y1: Double, y2: Double): T {
    this.yD = (y2 + y1 - this.heightD) / 2
    return this
}
fun <T : View> T.centerYBetween(y1: Float, y2: Float): T = centerYBetween(y1.toDouble(), y2.toDouble())
fun <T : View> T.centerYBetween(y1: Int, y2: Int): T = centerYBetween(y1.toDouble(), y2.toDouble())

/**
 * Chainable method returning this that sets [this] View
 * in the middle between [x1] and [x2] and in the middle between [y1] and [y2]
 */
fun <T : View> T.centerBetween(x1: Double, y1: Double, x2: Double, y2: Double): T = this.centerXBetween(x1, x2).centerYBetween(y1, y2)
fun <T : View> T.centerBetween(x1: Float, y1: Float, x2: Float, y2: Float): T = centerBetween(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())
fun <T : View> T.centerBetween(x1: Int, y1: Int, x2: Int, y2: Int): T = centerBetween(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())

/**
 * Chainable method returning this that sets [View.xD] so that
 * [this] View is centered on the [other] View horizontally
 */
fun <T : View> T.centerXOn(other: View): T = this.alignX(other, 0.5, true)

/**
 * Chainable method returning this that sets [View.yD] so that
 * [this] View is centered on the [other] View vertically
 */
fun <T : View> T.centerYOn(other: View): T = this.alignY(other, 0.5, true)

/**
 * Chainable method returning this that sets [View.xD] and [View.yD]
 * so that [this] View is centered on the [other] View
 */
fun <T : View> T.centerOn(other: View): T = this.centerXOn(other).centerYOn(other)

fun <T : View> T.centerXOnStage(): T = this.centerXOn(root)
fun <T : View> T.centerYOnStage(): T = this.centerYOn(root)
fun <T : View> T.centerOnStage(): T = this.centerXOnStage().centerYOnStage()

fun <T : View> T.alignXY(other: View, ratio: Double, inside: Boolean, doX: Boolean, padding: Double = 0.0): T {
    //val parent = this.parent
    //val bounds = other.getBoundsNoAnchoring(this)
    val bounds = other.getBoundsNoAnchoring(parent)
    val localBounds = this.getLocalBounds()

    //bounds.setTo(other.x, other.y, other.unscaledWidth, other.unscaledHeight)
    val ratioM1_1 = (ratio * 2 - 1)
    val rratioM1_1 = if (inside) ratioM1_1 else -ratioM1_1
    val iratio = if (inside) ratio else 1.0 - ratio
    //println("this: $this, other: $other, bounds=$bounds, scaledWidth=$scaledWidth, scaledHeight=$scaledHeight, width=$width, height=$height, scale=$scale, $scaleX, $scaleY")
    if (doX) {
        xD = (bounds.x + (bounds.width * ratio) - localBounds.left) - (this.scaledWidthD * iratio) - (padding * rratioM1_1)
    } else {
        yD = (bounds.y + (bounds.height * ratio) - localBounds.top) - (this.scaledHeightD * iratio) - (padding * rratioM1_1)
    }
    return this
}

fun <T : View> T.alignX(other: View, ratio: Double, inside: Boolean, padding: Double = 0.0): T {
    return alignXY(other, ratio, inside, doX = true, padding = padding)
}

fun <T : View> T.alignY(other: View, ratio: Double, inside: Boolean, padding: Double = 0.0): T {
    return alignXY(other, ratio, inside, doX = false, padding = padding)
}

/**
 * Chainable method returning this that sets [View.xD] so that
 * [this] View's left side is aligned with the [other] View's left side
 */
// @TODO: What about rotations? we might need to adjust y too?
fun <T : View> T.alignLeftToLeftOf(other: View, padding: Double = 0.0): T = alignX(other, 0.0, inside = true, padding = padding)
fun <T : View> T.alignLeftToLeftOf(other: View, padding: Float): T = alignLeftToLeftOf(other, padding.toDouble())
fun <T : View> T.alignLeftToLeftOf(other: View, padding: Int): T = alignLeftToLeftOf(other, padding.toDouble())

/**
 * Chainable method returning this that sets [View.xD] so that
 * [this] View's left side is aligned with the [other] View's right side
 */
fun <T : View> T.alignLeftToRightOf(other: View, padding: Double = 0.0): T = alignX(other, 1.0, inside = false, padding = padding)
fun <T : View> T.alignLeftToRightOf(other: View, padding: Float): T = alignLeftToRightOf(other, padding.toDouble())
fun <T : View> T.alignLeftToRightOf(other: View, padding: Int): T = alignLeftToRightOf(other, padding.toDouble())

/**
 * Chainable method returning this that sets [View.xD] so that
 * [this] View's right side is aligned with the [other] View's left side
 */
fun <T : View> T.alignRightToLeftOf(other: View, padding: Double = 0.0): T = alignX(other, 0.0, inside = false, padding = padding)
fun <T : View> T.alignRightToLeftOf(other: View, padding: Float): T = alignRightToLeftOf(other, padding.toDouble())
fun <T : View> T.alignRightToLeftOf(other: View, padding: Int): T = alignRightToLeftOf(other, padding.toDouble())

/**
 * Chainable method returning this that sets [View.xD] so that
 * [this] View's right side is aligned with the [other] View's right side
 */
fun <T : View> T.alignRightToRightOf(other: View, padding: Double = 0.0): T = alignX(other, 1.0, inside = true, padding = padding)
fun <T : View> T.alignRightToRightOf(other: View, padding: Float): T = alignRightToRightOf(other, padding.toDouble())
fun <T : View> T.alignRightToRightOf(other: View, padding: Int): T = alignRightToRightOf(other, padding.toDouble())

/**
 * Chainable method returning this that sets [View.yD] so that
 * [this] View's top side is aligned with the [other] View's top side
 */
fun <T : View> T.alignTopToTopOf(other: View, padding: Double = 0.0): T = alignY(other, 0.0, inside = true, padding = padding)
fun <T : View> T.alignTopToTopOf(other: View, padding: Float): T = alignTopToTopOf(other, padding.toDouble())
fun <T : View> T.alignTopToTopOf(other: View, padding: Int): T = alignTopToTopOf(other, padding.toDouble())

/**
 * Chainable method returning this that sets [View.yD] so that
 * [this] View's top side is aligned with the [other] View's bottom side
 */
fun <T : View> T.alignTopToBottomOf(other: View, padding: Double = 0.0): T = alignY(other, 1.0, inside = false, padding = padding)
fun <T : View> T.alignTopToBottomOf(other: View, padding: Float): T = alignTopToBottomOf(other, padding.toDouble())
fun <T : View> T.alignTopToBottomOf(other: View, padding: Int): T = alignTopToBottomOf(other, padding.toDouble())

/**
 * Chainable method returning this that sets [View.yD] so that
 * [this] View's bottom side is aligned with the [other] View's top side
 */
fun <T : View> T.alignBottomToTopOf(other: View, padding: Double = 0.0): T = alignY(other, 0.0, inside = false, padding = padding)
fun <T : View> T.alignBottomToTopOf(other: View, padding: Float): T = alignBottomToTopOf(other, padding.toDouble())
fun <T : View> T.alignBottomToTopOf(other: View, padding: Int): T = alignBottomToTopOf(other, padding.toDouble())

/**
 * Chainable method returning this that sets [View.yD] so that
 * [this] View's bottom side is aligned with the [other] View's bottom side
 */
fun <T : View> T.alignBottomToBottomOf(other: View, padding: Double = 0.0): T = alignY(other, 1.0, inside = true, padding = padding)
fun <T : View> T.alignBottomToBottomOf(other: View, padding: Float): T = alignBottomToBottomOf(other, padding.toDouble())
fun <T : View> T.alignBottomToBottomOf(other: View, padding: Int): T = alignBottomToBottomOf(other, padding.toDouble())
