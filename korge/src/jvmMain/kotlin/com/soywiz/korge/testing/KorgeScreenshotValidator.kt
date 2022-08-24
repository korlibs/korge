package com.soywiz.korge.testing

import com.soywiz.korim.bitmap.Bitmap

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
        if (contentEquals(oldBitmap, newBitmap)) {
            return KorgeScreenshotValidatorResult.Success
        }
        return KorgeScreenshotValidatorResult.Error("Content not equal")
    }

    private fun contentEquals(left: Bitmap, right: Bitmap): Boolean {
        if (left.width != right.width) return false
        if (left.height != right.height) return false
        for (y in 0 until left.height) for (x in 0 until left.width) {
            if (left.getRgbaRaw(x, y) != right.getRgbaRaw(x, y)) return false
        }
        return true
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
