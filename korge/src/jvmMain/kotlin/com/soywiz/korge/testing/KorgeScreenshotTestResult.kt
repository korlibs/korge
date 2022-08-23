package com.soywiz.korge.testing

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.file.VfsFile

data class KorgeScreenshotTestResult(
    val goldenName: String,
    val oldBitmap: Bitmap,
    val newBitmap: Bitmap?
)
