package korlibs.korge.view

import korlibs.graphics.*
import korlibs.graphics.annotation.*
import korlibs.inject.*
import korlibs.io.resources.*
import korlibs.korge.input.*
import korlibs.korge.view.property.*
import korlibs.math.annotations.*
import korlibs.math.geom.*
import korlibs.render.*
import kotlinx.coroutines.*

/**
 * Singleton root [View] and [Container] that contains a reference to the [Views] singleton and doesn't have any parent.
 */
@RootViewDslMarker
open class Stage internal constructor(override val views: Views) : FixedSizeContainer()
    , View.Reference
    , CoroutineScope by views
    , ViewsContainer
    , ResourcesContainer
    , BoundsProvider by views.bp
    , InvalidateNotifier
{
    override var clip: Boolean by views::clipBorders

    override var unscaledSize: Size by views::virtualSizeFloat

    val keys: InputKeys get() = views.input.keys
    val input: Input get() = views.input
    val injector: AsyncInjector get() = views.injector
    val ag: AG get() = views.ag
    val gameWindow: GameWindow get() = views.gameWindow
    override val resources get() = views.resources
    override val stage: Stage get() = this
    override val _invalidateNotifierForChildren: InvalidateNotifier get() = this

    init {
        this._stage = this
        this._invalidateNotifier = this
    }

    @KoragExperimental
    fun <T> runBlockingNoJs(block: suspend () -> T): T =
        gameWindow.runBlockingNoJs(this.coroutineContext, block)

    /** Mouse coordinates relative to the [Stage] singleton */
    val mousePos: Point get() = localMousePos(views)

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
    private var virtualSize: Point
        get() = Point(views.virtualWidthDouble, views.virtualHeightDouble)
        set(value) {
            views.virtualWidthFloat = value.x
            views.virtualHeightFloat = value.y
            views.gameWindow.queue {
                views.resized()
            }
        }

    override fun invalidatedView(view: BaseView?) {
        views.invalidatedView(view)
    }

    override fun toString(): String = "Stage"
}
