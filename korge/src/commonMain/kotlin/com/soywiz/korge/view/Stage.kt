package com.soywiz.korge.view

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korag.annotation.KoragExperimental
import com.soywiz.korev.EventDispatcher
import com.soywiz.korge.debug.findObservableProperties
import com.soywiz.korge.debug.uiCollapsibleSection
import com.soywiz.korge.debug.uiEditableValue
import com.soywiz.korge.render.RenderContext
import com.soywiz.korio.resources.ResourcesContainer
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.setTo
import com.soywiz.korui.UiContainer
import kotlinx.coroutines.CoroutineScope

/**
 * Singleton root [View] and [Container] that contains a reference to the [Views] singleton and doesn't have any parent.
 */
class Stage(override val views: Views) : Container()
    , View.Reference
    , CoroutineScope by views
    , EventDispatcher by EventDispatcher.Mixin()
    , ViewsContainer
    , ResourcesContainer
    , BoundsProvider by views.bp
{
    val keys get() = views.input.keys
    val input get() = views.input
    val injector get() = views.injector
    val ag get() = views.ag
    val gameWindow get() = views.gameWindow
    override val stage: Stage = this
    override val resources get() = views.resources

    @KoragExperimental
    fun <T> runBlockingNoJs(block: suspend () -> T): T =
        gameWindow.runBlockingNoJs(this.coroutineContext, block)

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
            ctx.useCtx2d { ctx2d ->
                ctx.rectPool.alloc { _tempWindowBounds ->
                    ctx2d.scissor(views.globalToWindowBounds(this.globalBounds, _tempWindowBounds)) {
                        super.renderInternal(ctx)
                    }
                }
            }
        } else {
            super.renderInternal(ctx)
        }
    }

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiCollapsibleSection("Stage") {
            uiEditableValue(Pair(views::virtualWidthDouble, views::virtualHeightDouble), name = "virtualSize", min = 0.0, max = 2000.0).findObservableProperties().fastForEach {
                it.onChange {
                    views.resized()
                }
            }
        }
        super.buildDebugComponent(views, container)
    }
}
