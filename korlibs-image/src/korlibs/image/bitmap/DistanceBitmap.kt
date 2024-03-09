package korlibs.image.bitmap

import korlibs.datastructure.*
import korlibs.math.clamp01
import korlibs.math.convertRange
import korlibs.image.color.RGBA
import korlibs.image.color.RgbaArray
import korlibs.io.lang.assert
import kotlin.math.max
import kotlin.math.min

typealias SDFBitmap = DistanceBitmap

// http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.102.7988&rep=rep1&type=pdf
// https://github.com/dy/bitmap-sdf/blob/78de3569d32404a7009f62bea3befca55838118a/index.js
// Dead Reckoning Algorithm
class DistanceBitmap(
    val width: Int,
    val height: Int,
    val d: FloatArray = FloatArray(width * height),
    val px: IntArray = IntArray(width * height),
    val py: IntArray = IntArray(width * height)
) {
    val area = width * height
    init {
        assert(d.size >= area)
        assert(px.size >= area)
        assert(py.size >= area)
    }

    fun inBoundsX(x: Int) = (x >= 0) && (x < width)
    fun inBoundsY(y: Int) = (y >= 0) && (y < height)
    fun inBounds(x: Int, y: Int) = inBoundsX(x) && inBoundsY(y)
    fun index(x: Int, y: Int) = y * width + x

    fun toFloatArray2(): FloatArray2 = FloatArray2(width, height, d)

    //fun getRealDist(x: Int, y: Int): Float = if (inBounds(x, y)) kotlin.math.hypot((x - this.px[index(x, y)]).toDouble(), (y - this.py[index(x, y)]).toDouble()).toFloat().withSign(getDist(x, y)) else Float.POSITIVE_INFINITY
    fun getDist(x: Int, y: Int): Float = if (inBounds(x, y)) this.d[index(x, y)] else Float.POSITIVE_INFINITY
    fun getPosX(x: Int, y: Int): Int = if (inBounds(x, y)) this.px[index(x, y)] else -1
    fun getPosY(x: Int, y: Int): Int = if (inBounds(x, y)) this.py[index(x, y)] else -1
    fun getRPosX(x: Int, y: Int): Int = if (inBounds(x, y)) x - this.px[index(x, y)] else 0
    fun getRPosY(x: Int, y: Int): Int = if (inBounds(x, y)) y - this.py[index(x, y)] else 0

    fun setDist(x: Int, y: Int, d: Float) {
        if (!inBounds(x, y)) return
        this.d[index(x, y)]  = d
    }

    fun setPosXY(x: Int, y: Int, px: Int, py: Int) {
        if (!inBounds(x, y)) return
        val index = index(x, y)
        this.px[index] = px
        this.py[index] = py
    }

    fun setPosRXY(x: Int, y: Int, dx: Int, dy: Int) {
        return setPosXY(x, y, x + dx, y + dy)
    }

    fun setPosXYRel(x: Int, y: Int, px: Int, py: Int) {
        return setPosXY(x, y, getPosX(px, py), getPosY(px, py))
    }

    companion object {
        private const val d1 = 1f
        private const val d2 = 1.4142135623730951f // kotlin.math.hypot(1f, 1f)
    }

    fun setFromBitmap(bmp: Bitmap, thresold: Double = 0.5) {
        assert(width == bmp.width)
        assert(height == bmp.height)

        val thresoldInt = (thresold.clamp01() * 255).toInt()

        fun Bitmap.getAlpha(x: Int, y: Int, min: Int): Boolean {
            return if (inBounds(x, y)) this.getRgbaRaw(x, y).a >= min else false
        }

        fun pass(x: Int, y: Int, dx: Int, dy: Int, d: Float) {
            if (getDist(x + dx, y + dy) + d < getDist(x, y)) {
                setPosXYRel(x, y, x + dx, y + dy)
                setDist(x, y, kotlin.math.hypot((x - getPosX(x, y)).toDouble(), (y - getPosY(x, y)).toDouble()).toFloat())
            }
        }

        // initialize d
        for (y in 0 until height) {
            for (x in 0 until width) {
                this.setDist(x, y, Float.POSITIVE_INFINITY)
                this.setPosXY(x, y, -1, -1)
            }
        }

        // initialize immediate interior & exterior elements
        for (y in 0 until height) {
            for (x in 0 until width) {
                val cur = bmp.getAlpha(x, y, thresoldInt)
                val left = bmp.getAlpha(x - 1, y, thresoldInt)
                val right = bmp.getAlpha(x + 1, y, thresoldInt)
                val up = bmp.getAlpha(x, y - 1, thresoldInt)
                val down = bmp.getAlpha(x, y + 1, thresoldInt)
                if (left != cur || right != cur || up != cur || down != cur) {
                    this.setDist(x, y, 0f)
                    this.setPosXY(x, y, x, y)
                }
            }
        }

        // perform the first pass
        for (y in 0 until height) {
            for (x in 0 until width) {
                pass(x, y, -1, -1, d2)
                pass(x, y, 0, -1, d1)
                pass(x, y, +1, -1, d2)
                pass(x, y, -1, 0, d1)
            }
        }

        // perform the final pass
        for (y in height - 1 downTo 0) {
            for (x in width - 1 downTo 0) {
                pass(x, y, +1, 0, d1)
                pass(x, y, -1, +1, d2)
                pass(x, y, 0, +1, d1)
                pass(x, y, +1, +1, d2)
            }
        }

        // indicate inside & outside
        for (y in height - 1 downTo 0) {
            for (x in width - 1 downTo 0) {
                if (bmp.getAlpha(x, y, thresoldInt)) {
                    setDist(x, y, -getDist(x, y))
                }
            }
        }
    }

    fun toNormalizedDistanceBitmap8(): Bitmap8 {
        val out = Bitmap8(width, height, palette = RgbaArray(256) { RGBA(it, it, it, 255) })
        var min = Float.MAX_VALUE
        var max = Float.MIN_VALUE
        for (n in 0 until area) {
            min = min(min, d[n])
            max = max(max, d[n])
        }
        for (n in 0 until area) {
            out.data[n] = d[n].convertRange(min, max, 0f, 255f).toInt().toByte()
        }
        //var n = 0
        //for (y in 0 until height) {
        //    for (x in 0 until width) {
        //        out.data[n] = getRealDist(x, y).convertRange(min, max, 0f, 255f).toInt().toByte()
        //        n++
        //    }
        //}
        return out
    }
}

fun Bitmap.distanceMap(out: DistanceBitmap = DistanceBitmap(width, height), thresold: Double = 0.5): DistanceBitmap = out.also { it.setFromBitmap(this, thresold) }
fun Bitmap.sdf(out: DistanceBitmap = DistanceBitmap(width, height), thresold: Double = 0.5): DistanceBitmap = out.also { it.setFromBitmap(this, thresold) }
