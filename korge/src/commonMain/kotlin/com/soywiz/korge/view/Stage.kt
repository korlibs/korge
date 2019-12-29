package com.soywiz.korge.view

import com.soywiz.korge.render.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.*

/**
 * Singleton root [View] and [Container] that contains a reference to the [Views] singleton and doesn't have any parent.
 */
class Stage(val views: Views) : Container(), View.Reference, CoroutineScope by views {
    val ag get() = views.ag
    override val stage: Stage = this

    override fun getLocalBoundsInternal(out: Rectangle) {
        out.setTo(views.actualVirtualLeft, views.actualVirtualTop, views.actualVirtualWidth, views.actualVirtualHeight)
    }

    override fun hitTest(x: Double, y: Double): View? = super.hitTest(x, y) ?: this

    override fun renderInternal(ctx: RenderContext) {
        if (views.clipBorders) {
            ctx.ctx2d.scissor(x, y, (views.virtualWidth * scaleX), (views.virtualHeight * scaleY)) {
                super.renderInternal(ctx)
            }
        } else {
            super.renderInternal(ctx)
        }
    }
}
