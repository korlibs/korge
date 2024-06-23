package korlibs.render.awt

import korlibs.graphics.gl.*
import korlibs.kgl.*
import korlibs.math.geom.*
import korlibs.render.*

class AwtOffscreenGameWindow(
    var size: Size = Size(640, 480),
    val context: OffscreenKmlGlContext = NewOffsetKmlGlContext(size.width.toInt(), size.height.toInt(), doUnset = true),
    val draw: Boolean = false,
    override val ag: AGOpengl = AGOpenglAWT(context = context.ctx),
    exitProcessOnClose: Boolean = false,
    override val devicePixelRatio: Double = 1.0,
) : GameWindow() {
    constructor(
        config: GameWindowCreationConfig,
        size: Size = Size(640, 480),
        context: OffscreenKmlGlContext = NewOffsetKmlGlContext(size.width.toInt(), size.height.toInt(), doUnset = true),
    ) : this(
        size = size,
        //draw = config.draw,
        context = context,
        ag = AGOpenglAWT(config, context.ctx),
        //exitProcessOnClose = config.exitProcessOnClose,
        //devicePixelRatio = config.devicePixelRatio
    )

    override val width: Int get() = context.width
    override val height: Int get() = context.height

    init {
        this.exitProcessOnClose = exitProcessOnClose
        //onEvent(ReshapeEvent) {
        //    context.setSize(it.width, it.height)
        //    //size = Size(it.width.toDouble(), it.height.toDouble())
        //}
    }

    override fun setSize(width: Int, height: Int) {
        //println("OFFSCREEN: setSize: $width, $height")
        context.setSize(width, height)
        context.doClear()
        dispatchReshapeEvent(0, 0, width, height)
    }

    //override val ag: AG = if (draw) AGSoftware(width, height) else DummyAG(width, height)
    //override val ag: AG = AGDummy(width, height)
}
