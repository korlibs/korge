package com.soywiz.korge.ui

import com.soywiz.korge.render.*
import com.soywiz.korge.view.*

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
    width: Double = 192.0,
    block: @ViewDslMarker UIVerticalStack.() -> Unit = {}
) = UIVerticalStack(width).addTo(this).apply(block)

open class UIVerticalStack(width: Double = 128.0) : UIContainer(width, 0.0) {
    override fun relayout() {
        var y = 0.0
        forEachChild {
            it.y = y
            it.scaledWidth = width
            y += it.height
        }
    }
}

inline fun Container.uiHorizontalStack(
    height: Double = 20.0,
    block: @ViewDslMarker UIHorizontalStack.() -> Unit = {}
) = UIHorizontalStack(width).addTo(this).apply(block)

open class UIHorizontalStack(height: Double = 20.0) : UIContainer(0.0, height) {
    override fun relayout() {
        var x = 0.0
        forEachChild {
            it.x = x
            it.scaledHeight = height
            x += it.width
        }
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
            it.x = x
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
            it.y = y
            it.scaledWidth = width
            it.height = elementHeight
            y += elementHeight
        }
    }
}
