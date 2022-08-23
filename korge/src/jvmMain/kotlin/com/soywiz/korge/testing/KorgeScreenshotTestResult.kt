package com.soywiz.korge.testing

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.file.VfsFile

sealed class KorgeScreenshotTestResult(
    open val testMethodName: String,
    open val goldenName: String,
    open val oldBitmap: Bitmap?,
    open val newBitmap: Bitmap?
) {
    data class Diff(
        override val testMethodName: String,
        override val goldenName: String,
        val testGoldensVfs: VfsFile,
        val tempVfs: VfsFile,
        override val oldBitmap: Bitmap,
        override val newBitmap: Bitmap
    ) : KorgeScreenshotTestResult(testMethodName, goldenName, oldBitmap, newBitmap)

    data class Deleted(
        override val testMethodName: String,
        override val goldenName: String,
        val testGoldensVfs: VfsFile,
        override val oldBitmap: Bitmap,
    ) : KorgeScreenshotTestResult(testMethodName, goldenName, oldBitmap, null)
}
