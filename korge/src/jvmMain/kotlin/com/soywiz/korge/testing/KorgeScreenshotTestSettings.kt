package com.soywiz.korge.testing

data class KorgeScreenshotTestSettings(
    val validators: List<KorgeScreenshotValidator> = listOf(
        DefaultValidator
    )
)
