package com.soywiz.korim.paint

import com.soywiz.kmem.clamp01
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.RGBAPremultiplied
import com.soywiz.korim.color.RgbaPremultipliedArray
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.CycleMethod
import com.soywiz.korma.geom.*

abstract class BaseFiller {
    abstract fun getColor(x: Float, y: Float): RGBAPremultiplied
    fun getColor(x: Int, y: Int): RGBAPremultiplied = getColor(x.toFloat(), y.toFloat())
    abstract fun fill(data: RgbaPremultipliedArray, offset: Int, x0: Int, x1: Int, y: Int)
    fun fill(bmp: Bitmap32, rect: IRectangleInt = bmp.bounds) {
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
    private var cycleX: CycleMethod = CycleMethod.NO_CYCLE
    private var cycleY: CycleMethod = CycleMethod.NO_CYCLE

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
    private var transform: MMatrix = MMatrix()
    private var linear: Boolean = true
    private val compTrans = MMatrix()

    fun set(fill: BitmapPaint, state: Context2d.State) = this.apply {
        this.cycleX = fill.cycleX
        this.cycleY = fill.cycleY
        this.texture = fill.bmp32
        this.transform = fill.transform
        this.linear = fill.smooth
        compTrans.apply {
            identity()
            premultiply(state.transform)
            premultiply(fill.transform)
            invert()
        }
    }

    fun lookupLinear(x: Float, y: Float): RGBA = texture.getRgbaSampled(x, y)
    fun lookupNearest(x: Float, y: Float): RGBA = texture[x.toInt(), y.toInt()]

    override fun fill(data: RgbaPremultipliedArray, offset: Int, x0: Int, x1: Int, y: Int) {
        val yFloat = y.toFloat()
        for (n in x0..x1) {
            data[offset + n] = getColor(n.toFloat(), yFloat)
        }
    }

    override fun getColor(x: Float, y: Float): RGBAPremultiplied {
        val mat = compTrans
        val tx = cycleX.apply(mat.transformXf(x.toFloat(), y) * iTexWidth) * texWidth
        val ty = cycleY.apply(mat.transformYf(x.toFloat(), y) * iTexHeight) * texHeight
        return if (linear) lookupLinear(tx, ty).premultiplied else lookupNearest(tx, ty).premultiplied
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
        this.fill = fill.copy(transform = MMatrix().apply {
            identity()
            preconcat(fill.transform)
            preconcat(state.transform)
        })
        //println("state.transform=${state.transform}")
        //this.stateTransformInv.copyFromInverted(state.transform)
        return this
    }

    private fun color(ratio: Float): RGBAPremultiplied = colors[(ratio.clamp01() * (NCOLORS - 1)).toInt()]

    fun getRatio(x: Double, y: Double): Double = getRatio(x.toFloat(), y.toFloat()).toDouble()
    fun getRatio(x: Float, y: Float): Float = fill.getRatioAt(x, y)
    override fun getColor(x: Float, y: Float): RGBAPremultiplied = color(getRatio(x, y))

    // @TODO: Radial gradient
    // @TODO: This doesn't seems to work properly
    override fun fill(data: RgbaPremultipliedArray, offset: Int, x0: Int, x1: Int, y: Int) {
        for (n in x0..x1) data[offset + n] = getColor(n.toFloat(), y.toFloat())
    }
}
