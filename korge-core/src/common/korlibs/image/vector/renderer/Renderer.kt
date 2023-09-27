package korlibs.image.vector.renderer

import korlibs.image.bitmap.*
import korlibs.image.paint.*
import korlibs.image.vector.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*

abstract class Renderer {
    var debug: Boolean = false
    abstract val width: Int
    abstract val height: Int

    inline fun <T> buffering(callback: () -> T): T {
        bufferingStart()
        try {
            return callback()
        } finally {
            bufferingEnd()
        }
    }

    private var bufferingLevel = 0
    protected open fun isBuffering() = bufferingLevel > 0
    protected open fun flush() = Unit
    fun bufferingStart() = bufferingLevel++
    fun bufferingEnd() {
        bufferingLevel--
        if (bufferingLevel == 0) {
            flush()
        }
    }
    open fun render(state: Context2d.State, fill: Boolean, winding: Winding? = null) {
        var rstate = state
        if (fill && !state.fillStyle.isPaintSupported()) {
            rstate = state.clone()
            rstate.fillStyle = rstate.fillStyle.toBitmapPaint(state)
        }
        renderFinal(rstate, fill, winding)
    }

    protected open fun renderFinal(state: Context2d.State, fill: Boolean, winding: Winding? = null): Unit = Unit

    open fun Paint.isPaintSupported(): Boolean = when {
        //this is GradientPaint -> false // For debugging gradients in other targets ie. CG and GDI+
        else -> true
    }

    open fun drawImage(image: Bitmap, pos: Point, size: Size = image.size.toFloat(), transform: Matrix = Matrix.IDENTITY) {
        render(
            Context2d.State(
                transform = transform,
                path = VectorPath().apply {
                    if (transform.type == MatrixType.IDENTITY) {
                        rect(pos, size)
                    } else {
                        transformed(transform) {
                            rect(pos, size)
                        }
                    }
                },
                fillStyle = BitmapPaint(
                    image,
                    transform = Matrix.IDENTITY
                        .scaled(size / image.size.toFloat())
                        .translated(pos)
                )
            ),
            fill = true
        )
    }

    open fun dispose() {
        flush()
    }
}

open class DummyRenderer(override val width: Int, override val height: Int) : Renderer() {
    companion object : DummyRenderer(128, 128)
}

abstract class BufferedRenderer : Renderer() {
    abstract fun flushCommands(commands: List<RenderCommand>)

    data class RenderCommand(val state: Context2d.State, val fill: Boolean, val winding: Winding?) {
        val stroke: Boolean get() = !fill
    }
    private val commands = arrayListOf<RenderCommand>()

    final override fun renderFinal(state: Context2d.State, fill: Boolean, winding: Winding?) {
        commands += RenderCommand(state.clone(), fill, winding)
        if (!isBuffering()) flush()
    }

    //final override fun renderText(state: Context2d.State, font: Font, fontSize: Double, text: String, x: Double, y: Double, fill: Boolean) {
    //    commands += RenderCommand(state.clone(), fill, font, fontSize, text, x, y)
    //    if (!isBuffering()) flush()
    //}

    final override fun flush() {
        flushCommands(commands)
        commands.clear()
    }
}
