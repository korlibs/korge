package com.soywiz.korge.testing

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.file.VfsFile

data class KorgeScreenshotTestResult(
    val goldenName: String,
    val oldBitmap: Bitmap,
    val newBitmap: Bitmap?,
    val validationResults: List<KorgeScreenshotValidatorResult>
) {
    val hasValidationErrors
        get() = validationResults.any {
            it is KorgeScreenshotValidatorResult.Error
        }

    val validationErrors get() = validationResults.filterIsInstance<KorgeScreenshotValidatorResult.Error>()
}
