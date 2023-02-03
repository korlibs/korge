package com.soywiz.korge.testing

data class KorgeScreenshotTestResults(
    val testMethodName: String,
    val results: MutableList<KorgeScreenshotTestResult> = mutableListOf()
)
