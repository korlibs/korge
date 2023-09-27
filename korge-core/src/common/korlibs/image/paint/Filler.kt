package korlibs.image.paint

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.vector.*
import korlibs.math.*
import korlibs.math.geom.*

abstract class BaseFiller {
    abstract fun getColor(x: Float, y: Float): RGBAPremultiplied
    fun getColor(x: Int, y: Int): RGBAPremultiplied = getColor(x.toFloat(), y.toFloat())
    open fun fill(data: RgbaPremultipliedArray, offset: Int, x0: Int, x1: Int, y: Int) {
        val yFloat = y.toFloat()
        for (n in x0..x1) {
            data[offset + n] = getColor(n.toFloat(), yFloat)
        }
    }
    fun fill(bmp: Bitmap32, rect: RectangleInt = bmp.bounds) {
        val cols = RgbaPremultipliedArray(bmp.ints)
        for (y in rect.top until rect.bottom) {
            val offset = y * bmp.width
            fill(cols, offset, rect.left, rect.right - 1, y)
        }
    }
}

fun Paint.toFiller(state: Context2d.State): BaseFiller = when (this) {
    is NonePaint -> NoneFiller
    is ColorPaint -> ColorFiller().also { it.set(this, state) }
    is GradientPaint -> GradientFiller().also { it.set(this, state) }
    is BitmapPaint -> BitmapFiller().also { it.set(this, state) }
    else -> TODO()
}

object NoneFiller : BaseFiller() {
    override fun getColor(x: Float, y: Float): RGBAPremultiplied = RGBAPremultiplied(0)
    override fun fill(data: RgbaPremultipliedArray, offset: Int, x0: Int, x1: Int, y: Int) = Unit
}

class ColorFiller : BaseFiller() {
    private var color: RGBAPremultiplied = Colors.RED.premultiplied

    fun set(fill: ColorPaint, state: Context2d.State) = this.apply {
        this.color = fill.color.premultiplied
        //println("ColorFiller: $color")
    }

    override fun getColor(x: Float, y: Float): RGBAPremultiplied = color

    override fun fill(data: RgbaPremultipliedArray, offset: Int, x0: Int, x1: Int, y: Int) {
        data.fill(color, offset + x0, offset + x1 + 1)
    }
}

class BitmapFiller : BaseFiller() {
    private var cycle: CycleMethodPair = CycleMethodPair(CycleMethod.NO_CYCLE, CycleMethod.NO_CYCLE)

    private var texWidth = 0f
    private var texHeight = 0f
    private var iTexWidth = 0f
    private var iTexHeight = 0f

    private var texture: Bitmap32 = Bitmaps.transparent.bmp
        set(value) {
            field = value
            texWidth = texture.width.toFloat()
            texHeight = texture.height.toFloat()
            iTexWidth = 1f / texWidth
            iTexHeight = 1f / texHeight
        }
    private var transform: Matrix = Matrix.IDENTITY
    private var linear: Boolean = true
    private var compTrans = Matrix.IDENTITY

    fun set(fill: BitmapPaint, state: Context2d.State) = this.apply {
        this.cycle = CycleMethodPair(fill.cycleX, fill.cycleY)
        this.texture = fill.bmp32
        this.transform = fill.transform
        this.linear = fill.smooth
        compTrans = Matrix.IDENTITY
            .premultiplied(state.transform)
            .premultiplied(fill.transform)
            .inverted()

    }

    fun lookupLinear(x: Float, y: Float): RGBA = texture.getRgbaSampled(x, y)
    fun lookupNearest(x: Float, y: Float): RGBA = texture[x.toInt(), y.toInt()]

    override fun getColor(x: Float, y: Float): RGBAPremultiplied {
        val mat = compTrans
        val p = mat.transform(Point(x, y)) * Point(iTexWidth, iTexHeight)
        val t = cycle.apply(p) * Point(texWidth, texHeight)
        return if (linear) lookupLinear(t.x.toFloat(), t.y.toFloat()).premultiplied else lookupNearest(t.x.toFloat(), t.y.toFloat()).premultiplied
    }
}

class GradientFiller : BaseFiller() {
    companion object {
        const val NCOLORS = 256
    }
    private val colors = RgbaPremultipliedArray(NCOLORS)
    private lateinit var fill: GradientPaint
    //private val stateTransformInv = Matrix()

    fun set(fill: GradientPaint, state: Context2d.State): GradientFiller {
        fill.fillColors(colors)
        this.fill = fill.copy(transform = Matrix.IDENTITY
            .preconcated(fill.transform)
            .preconcated(state.transform)
        )
        //println("state.transform=${state.transform}")
        //this.stateTransformInv.copyFromInverted(state.transform)
        return this
    }

    private fun color(ratio: Float): RGBAPremultiplied = colors[(ratio.clamp01() * (NCOLORS - 1)).toInt()]
    fun getRatio(x: Double, y: Double): Double = getRatio(x.toFloat(), y.toFloat()).toDouble()
    fun getRatio(x: Float, y: Float): Float = fill.getRatioAt(Point(x, y))
    override fun getColor(x: Float, y: Float): RGBAPremultiplied = color(getRatio(x, y))
}
