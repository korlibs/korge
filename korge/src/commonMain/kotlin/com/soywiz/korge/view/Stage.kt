package com.soywiz.korge.view

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korag.AG
import com.soywiz.korag.annotation.KoragExperimental
import com.soywiz.korev.EventDispatcher
import com.soywiz.korge.baseview.*
import com.soywiz.korge.debug.findObservableProperties
import com.soywiz.korge.debug.uiCollapsibleSection
import com.soywiz.korge.debug.uiEditableValue
import com.soywiz.korge.input.Input
import com.soywiz.korge.input.InputKeys
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.property.*
import com.soywiz.korgw.GameWindow
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korio.annotations.*
import com.soywiz.korio.resources.ResourcesContainer
import com.soywiz.korma.annotations.RootViewDslMarker
import com.soywiz.korma.geom.*
import com.soywiz.korui.UiContainer
import kotlinx.coroutines.CoroutineScope

/**
 * Singleton root [View] and [Container] that contains a reference to the [Views] singleton and doesn't have any parent.
 */
@RootViewDslMarker
class Stage(override val views: Views) : FixedSizeContainer()
    , View.Reference
    , CoroutineScope by views
    , EventDispatcher by EventDispatcher.Mixin()
    , ViewsContainer
    , ResourcesContainer
    , BoundsProvider by views.bp
    , InvalidateNotifier
{
    override var clip: Boolean by views::clipBorders
    override var width: Double by views::virtualWidthDouble
    override var height: Double by views::virtualHeightDouble

    val keys: InputKeys get() = views.input.keys
    val input: Input get() = views.input
    val injector: AsyncInjector get() = views.injector
    val ag: AG get() = views.ag
    val gameWindow: GameWindow get() = views.gameWindow
    override val resources get() = views.resources
    override val stage: Stage get() = this

    init {
        this._stage = this
        this._invalidateNotifier = this
    }

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

    //override fun getLocalBoundsInternal(out: Rectangle) {
    //    out.setTo(0.0, 0.0, views.virtualWidth, views.virtualHeight)
    //}
    ////override fun hitTest(x: Double, y: Double): View? = super.hitTest(x, y) ?: this
    //override fun renderInternal(ctx: RenderContext) {
    //    if (views.clipBorders) {
    //        ctx.useCtx2d { ctx2d ->
    //            ctx.rectPool.alloc { _tempWindowBounds ->
    //                ctx2d.scissor(views.globalToWindowBounds(this.globalBounds, _tempWindowBounds)) {
    //                    super.renderInternal(ctx)
    //                }
    //            }
    //        }
    //    } else {
    //        super.renderInternal(ctx)
    //    }
    //}

    @Suppress("unused")
    @ViewProperty(min = 0.0, max = 2000.0, groupName = "Stage")
    private var virtualSize: IPoint
        get() = Point(views.virtualWidthDouble, views.virtualHeightDouble)
        set(value) {
            views.virtualWidthDouble = value.x
            views.virtualHeightDouble = value.y
            views.gameWindow.queue {
                views.resized()
            }
        }

    override fun invalidatedView(view: BaseView?) {
        views.invalidatedView(view)
    }

    override fun toString(): String = "Stage"
}
