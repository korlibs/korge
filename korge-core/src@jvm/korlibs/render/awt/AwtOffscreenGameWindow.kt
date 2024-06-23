package korlibs.render.awt

import korlibs.graphics.gl.*
import korlibs.kgl.*
import korlibs.math.geom.*
import korlibs.render.*

class AwtOffscreenGameWindow(
    var size: Size = Size(640, 480),
    val context: KmlGlContext = OffsetKmlGlContext(size.width.toInt(), size.height.toInt(), doUnset = true),
    val draw: Boolean = false,
    override val ag: AGOpengl = AGOpenglAWT(context = context),
    exitProcessOnClose: Boolean = false,
    override val devicePixelRatio: Double = 1.0,
) : GameWindow() {
    constructor(
        config: GameWindowCreationConfig,
        size: Size = Size(640, 480),
        context: KmlGlContext = OffsetKmlGlContext(size.width.toInt(), size.height.toInt(), doUnset = true),
    ) : this(
        size = size,
        //draw = config.draw,
        context = context,
        ag = AGOpenglAWT(config, context),
        //exitProcessOnClose = config.exitProcessOnClose,
        //devicePixelRatio = config.devicePixelRatio
    )

    override val width: Int get() = size.width.toInt()
    override val height: Int get() = size.height.toInt()

    init {
        this.exitProcessOnClose = exitProcessOnClose
    }

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        super.loop {
            entry()
        }
    }

    //override val ag: AG = if (draw) AGSoftware(width, height) else DummyAG(width, height)
    //override val ag: AG = AGDummy(width, height)
}
