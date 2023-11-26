package korlibs.image.bitmap

import korlibs.io.lang.invalidOp
import kotlin.math.log10
import kotlin.math.sqrt

object PSNR {
    fun MSE(a: Bitmap32, b: Bitmap32, c: BitmapChannel): Double {
        if (a.size != b.size) invalidOp("${a.size} != ${b.size}")
        val area = a.area
        var sum = 0.0
        for (n in 0 until area) {
            val v = c.extract(a.getRgbaAtIndex(n)) - c.extract(b.getRgbaAtIndex(n))
            sum += v * v
        }
        return sum / area.toDouble()
    }

    fun MSE(a: Bitmap32, b: Bitmap32): Double {
        return BitmapChannel.ALL.map { MSE(a, b, it) }.sum() / 4.0
    }

    private fun PSNR(a: Bitmap32, b: Bitmap32, mse: Double): Double {
        return 20.0 * log10(0xFF.toDouble() / sqrt(mse))
    }

    operator fun invoke(a: Bitmap32, b: Bitmap32): Double = PSNR(a, b, MSE(a, b))
    operator fun invoke(a: Bitmap32, b: Bitmap32, c: BitmapChannel): Double = PSNR(a, b, MSE(a, b, c))
}

fun Bitmap32.psnrDiffTo(that: Bitmap32): Double = Bitmap32.computePsnr(this, that)
fun Bitmap32.Companion.computePsnr(a: Bitmap32, b: Bitmap32): Double = PSNR(a, b)
