package com.soywiz.korge.testing

import com.soywiz.korim.bitmap.Bitmap

data class KorgeScreenshotValidationSettings(
    val validators: List<KorgeScreenshotValidator> = listOf(
        DefaultValidator
    )
) {
    fun validate(goldenName: String, oldBitmap: Bitmap, newBitmap: Bitmap?): List<KorgeScreenshotValidatorResult> {
        return validators.map {
            it.validate(goldenName, oldBitmap, newBitmap)
        }
    }
}
