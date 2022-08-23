package com.soywiz.korge.testing

import com.soywiz.korim.bitmap.Bitmap

data class KorgeScreenshotTestResults(
    val testMethodName: String,
    val results: MutableList<KorgeScreenshotTestResult> = mutableListOf()
)
