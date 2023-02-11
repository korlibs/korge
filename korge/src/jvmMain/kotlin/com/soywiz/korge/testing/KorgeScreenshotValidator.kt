package com.soywiz.korge.testing

import com.soywiz.korim.bitmap.*
import kotlin.math.*

sealed class KorgeScreenshotValidatorResult {
    object Success : KorgeScreenshotValidatorResult()
    data class Error(val errorMessaage: String) : KorgeScreenshotValidatorResult()
}

interface KorgeScreenshotValidator {
    fun validate(
        goldenName: String,
        oldBitmap: Bitmap,
        newBitmap: Bitmap?
    ): KorgeScreenshotValidatorResult
}

object DeletedGoldenValidator : KorgeScreenshotValidator {
    override fun validate(
        goldenName: String,
        oldBitmap: Bitmap,
        newBitmap: Bitmap?
    ): KorgeScreenshotValidatorResult {
        if (newBitmap == null) return KorgeScreenshotValidatorResult.Error("Deleted golden `$goldenName`.")
        return KorgeScreenshotValidatorResult.Success
    }

}

object DefaultValidator : KorgeScreenshotValidator {
    override fun validate(
        goldenName: String,
        oldBitmap: Bitmap,
        newBitmap: Bitmap?
    ): KorgeScreenshotValidatorResult {
        if (newBitmap == null) return DeletedGoldenValidator.validate(goldenName, oldBitmap, newBitmap)

        val result = contentCompare(oldBitmap, newBitmap)

        if (result.reasonablySimilar) {
        //if (result.strictEquals) {
            return KorgeScreenshotValidatorResult.Success
        }
        return KorgeScreenshotValidatorResult.Error("Content not equal : $result")
    }

    // @TODO: Do SSIM, PSNR
    data class CompareResult(
        val pixelDiffCount: Int = 0,
        val pixelTotalDistance: Int = 0,
        val pixelMaxDistance: Int = 0,
        val psnr: Double = 0.0,
    ) {
        val strictEquals: Boolean get() = pixelDiffCount == 0
        val reasonablySimilar: Boolean get() = pixelMaxDistance <= 3 || psnr >= 45.0
    }

    private fun contentCompare(left: Bitmap, right: Bitmap): CompareResult {
        if (left.width != right.width) return CompareResult(-1, -1, -1)
        if (left.height != right.height) return CompareResult(-1, -1, -1)
        var pixelDiffCount = 0
        var pixelTotalDistance = 0
        var pixelMaxDistance = 0
        loop@ for (y in 0 until left.height) for (x in 0 until left.width) {
            val lc = left.getRgbaRaw(x, y)
            val rc = right.getRgbaRaw(x, y)
            val Rdiff = (lc.r - rc.r).absoluteValue
            val Gdiff = (lc.g - rc.g).absoluteValue
            val Bdiff = (lc.b - rc.b).absoluteValue
            val Adiff = (lc.a - rc.a).absoluteValue
            pixelTotalDistance += Rdiff + Gdiff + Bdiff + Adiff
            pixelMaxDistance = maxOf(pixelMaxDistance, Rdiff)
            pixelMaxDistance = maxOf(pixelMaxDistance, Gdiff)
            pixelMaxDistance = maxOf(pixelMaxDistance, Bdiff)
            pixelMaxDistance = maxOf(pixelMaxDistance, Adiff)
            if (lc != rc) {
                pixelDiffCount++
            }
        }
        val psnr = Bitmap32.computePsnr(left.toBMP32(), right.toBMP32())

        return CompareResult(pixelDiffCount, pixelTotalDistance, pixelMaxDistance, psnr)
    }
}

class AbsolutePixelDifferenceValidator(private val pixelTolerance: Long) :
    KorgeScreenshotValidator {
    override fun validate(
        goldenName: String,
        oldBitmap: Bitmap,
        newBitmap: Bitmap?
    ): KorgeScreenshotValidatorResult {
        // Passthrough for other validators
        if (newBitmap == null ||
            oldBitmap.width != newBitmap.width || oldBitmap.height != newBitmap.height
        ) return KorgeScreenshotValidatorResult.Success
        val diff = getNumPixelDifference(oldBitmap, newBitmap)
        if (diff > pixelTolerance) {
            return KorgeScreenshotValidatorResult.Error("Difference in pixels greater than allowed tolerance ($pixelTolerance): $diff")
        }
        return KorgeScreenshotValidatorResult.Success
    }

    private fun getNumPixelDifference(left: Bitmap, right: Bitmap): Long {
        require(left.width == right.width && left.height == right.height)
        var numDiff = 0L
        for (y in 0 until left.height) for (x in 0 until left.width) {
            if (left.getRgbaRaw(x, y) != right.getRgbaRaw(x, y)) numDiff++
        }
        return numDiff
    }
}
