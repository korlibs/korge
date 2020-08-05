package com.soywiz.korge.view

import com.soywiz.korev.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.render.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.*

/**
 * Singleton root [View] and [Container] that contains a reference to the [Views] singleton and doesn't have any parent.
 */
class Stage(override val views: Views) : Container()
    , View.Reference
    , CoroutineScope by views
    , EventDispatcher by EventDispatcher.Mixin()
    , ViewsScope, ViewsContainer
    , KorgeDebugNode
{
    val injector get() = views.injector
    val ag get() = views.ag
    val gameWindow get() = views.gameWindow
    override val stage: Stage = this

    /** Mouse coordinates relative to the [Stage] singleton */
    val mouseXY: Point = Point(0.0, 0.0)
        get() {
            field.setTo(mouseX, mouseY)
            return field
        }
    /** Mouse X coordinate relative to the [Stage] singleton */
    val mouseX get() = localMouseX(views)
    /** Mouse Y coordinate relative to the [Stage] singleton */
    val mouseY get() = localMouseY(views)

    override fun getLocalBoundsInternal(out: Rectangle) {
        out.setTo(0.0, 0.0, views.virtualWidth, views.virtualHeight)
    }

    //override fun hitTest(x: Double, y: Double): View? = super.hitTest(x, y) ?: this

    override fun renderInternal(ctx: RenderContext) {
        if (views.clipBorders) {
            ctx.ctx2d.scissor(x, y, (views.virtualWidth * scaleX), (views.virtualHeight * scaleY)) {
                super.renderInternal(ctx)
            }
        } else {
            super.renderInternal(ctx)
        }
    }

    override fun getDebugProperties(views: Views): EditableNode? = EditableSection("Views") {
        add(views::virtualWidth.toEditableProperty().also {
            it.onChange.add { views.resized() }
        })
        add(views::virtualHeight.toEditableProperty().also {
            it.onChange.add { views.resized() }
        })
    }
}
