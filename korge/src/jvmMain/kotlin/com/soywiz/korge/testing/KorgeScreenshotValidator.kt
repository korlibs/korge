package com.soywiz.korge.testing

import com.soywiz.korim.bitmap.Bitmap

interface KorgeScreenshotValidator {
    fun validate(result: KorgeScreenshotTestResult): ValidatorResult
}

sealed class ValidatorResult {
    object Success : ValidatorResult()
    data class Error(val errorMessaage: String) : ValidatorResult()
}

object DefaultValidator : KorgeScreenshotValidator {
    override fun validate(result: KorgeScreenshotTestResult): ValidatorResult {
        if (result.newBitmap == null) return ValidatorResult.Error("Deleted bitmap.")
        if (contentEquals(result.oldBitmap, result.newBitmap)) {
            return ValidatorResult.Success
        }
        return ValidatorResult.Error("Content not equal")
    }

    private fun contentEquals(left: Bitmap, other: Bitmap): Boolean {
        if (left.width != other.width) return false
        if (left.height != other.height) return false
        for (y in 0 until left.height) for (x in 0 until left.width) {
            if (left.getRgbaRaw(x, y) != other.getRgbaRaw(x, y)) return false
        }
        return true
    }
}


