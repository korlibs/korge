package korlibs.image.bitmap

import korlibs.image.color.RGBA
import korlibs.image.color.RGBAf
import korlibs.math.*
import kotlin.math.*

class FloatBitmap32(
    width: Int,
    height: Int,
    val data: FloatArray = FloatArray(width * height * 4),
    premultiplied: Boolean = false
) : Bitmap(width, height, 32, premultiplied, data) {
    private fun index4(x: Int, y: Int) = index(x, y) * 4

    override fun setRgbaRaw(x: Int, y: Int, v: RGBA) {
        val rindex = index4(x, y)
        data[rindex + 0] = v.rf
        data[rindex + 1] = v.gf
        data[rindex + 2] = v.bf
        data[rindex + 3] = v.af
    }
    override fun getRgbaRaw(x: Int, y: Int): RGBA {
        val rindex = index4(x, y)
        return RGBA.float(data[rindex + 0], data[rindex + 1], data[rindex + 2], data[rindex + 3])
    }

    fun getRed(x: Int, y: Int): Float = data[index4(x, y) + 0]
    fun getGreen(x: Int, y: Int): Float = data[index4(x, y) + 1]
    fun getBlue(x: Int, y: Int): Float = data[index4(x, y) + 2]
    fun getAlpha(x: Int, y: Int): Float = data[index4(x, y) + 3]

    fun setRed(x: Int, y: Int, r: Float) {
        data[index4(x, y) + 0] = r
    }
    fun setGreen(x: Int, y: Int, g: Float) {
        data[index4(x, y) + 1] = g
    }
    fun setBlue(x: Int, y: Int, b: Float) {
        data[index4(x, y) + 2] = b
    }
    fun setAlpha(x: Int, y: Int, a: Float) {
        data[index4(x, y) + 3] = a
    }

    fun setRgbaf(x: Int, y: Int, r: Float, g: Float, b: Float, a: Float) {
        val rindex = index4(x, y)
        data[rindex + 0] = r
        data[rindex + 1] = g
        data[rindex + 2] = b
        data[rindex + 3] = a
    }
    fun setRgbaf(x: Int, y: Int, rgbaf: RGBAf) {
        setRgbaf(x, y, rgbaf.r, rgbaf.g, rgbaf.b, rgbaf.a)
    }

    fun setRgbaf(x: Int, y: Int, v: FloatArray): Unit = setRgbaf(x, y, v[0], v[1], v[2], v[3])
    fun getRgbaf(x: Int, y: Int, out: RGBAf = RGBAf()): RGBAf {
        val rindex = index4(x, y)
        out.r = data[rindex + 0]
        out.g = data[rindex + 1]
        out.b = data[rindex + 2]
        out.a = data[rindex + 3]
        return out
    }

    fun getMinMax(out: FloatArray = FloatArray(2)): FloatArray {
        var min = Float.POSITIVE_INFINITY
        var max = Float.NEGATIVE_INFINITY
        val temp = RGBAf()
        forEach { n, x, y ->
            val c = getRgbaf(x, y, temp)
            min = korlibs.math.min(min, c.r, c.g, c.b, c.a)
            max = korlibs.math.max(max, c.r, c.g, c.b, c.a)
        }
        out[0] = min
        out[1] = max
        return out
    }

    fun convertRange(minSrc: Float, maxSrc: Float, minDst: Float = 0f, maxDst: Float = 1f): Unit {
        val out = RGBAf()
        forEach { n, x, y ->
            val rgbaf = getRgbaf(x, y, out)
            setRgbaf(x, y,
                rgbaf.r.convertRange(minSrc, maxSrc, minDst, maxDst),
                rgbaf.g.convertRange(minSrc, maxSrc, minDst, maxDst),
                rgbaf.b.convertRange(minSrc, maxSrc, minDst, maxDst),
                rgbaf.a.convertRange(minSrc, maxSrc, minDst, maxDst),
            )
        }
    }

    fun normalize(): Unit {
        val (min, max) = getMinMax()
        convertRange(min, max, 0f, 1f)
    }

    fun normalizeUniform() {
        val (min, max) = getMinMax()
        val range = max(min.absoluteValue, max.absoluteValue)
        convertRange(-range, +range)
    }

    fun scale(scale: Float): Unit = scale(scale, scale, scale, scale)

    fun scale(scaleRed: Float = 1f, scaleGreen: Float = 1f, scaleBlue: Float = 1f, scaleAlpha: Float = 1f): Unit {
        forEach { n, x, y ->
            setRgbaf(
                x, y,
                getRed(x, y) * scaleRed,
                getGreen(x, y) * scaleGreen,
                getBlue(x, y) * scaleBlue,
                getAlpha(x, y) * scaleAlpha,
            )
        }
    }

    fun clamp(min: Float, max: Float): Unit {
        updateComponent { _, value ->
            value.clamp(min, max)
        }
    }

    override fun contentEquals(other: Bitmap): Boolean = (other is FloatBitmap32) && (this.width == other.width) && (this.height == other.height) && data.contentEquals(other.data)
    override fun contentHashCode(): Int = (width * 31 + height) + data.contentHashCode() + premultiplied.toInt()

    inline fun updateComponent(block: (component: Int, value: Float) -> Float): Unit {
        forEach { n, x, y ->
            setRgbaf(
                x, y,
                block(0, getRed(x, y)),
                block(1, getGreen(x, y)),
                block(2, getBlue(x, y)),
                block(3, getAlpha(x, y)),
            )
        }
    }
}

fun Bitmap.toFloatBMP32(out: FloatBitmap32 = FloatBitmap32(width, height, premultiplied = premultiplied)): FloatBitmap32 {
    for (y in 0 until height) {
        for (x in 0 until width) {
            val col = this.getRgbaRaw(x ,y)
            out.setRgbaf(x, y, col.rf, col.gf, col.bf, col.af)
        }
    }
    return out
}
