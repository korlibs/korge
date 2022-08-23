package com.soywiz.korge.testing

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.file.VfsFile

sealed class KorgeScreenshotTestResult(
    open val testMethodName: String,
    open val goldenName: String
) {
    abstract suspend fun acceptChange()

    val goldenFileNameWithExt: String
        get() = KorgeScreenshotTesterUtils.makeGoldenFileNameWithExtension(testMethodName, goldenName)

    data class Diff(
        override val testMethodName: String,
        override val goldenName: String,
        val testGoldensVfs: VfsFile,
        val tempVfs: VfsFile,
        val oldBitmap: Bitmap,
        val newBitmap: Bitmap
    ) : KorgeScreenshotTestResult(testMethodName, goldenName) {
        override suspend fun acceptChange() {
            tempVfs[goldenFileNameWithExt].copyTo(testGoldensVfs[goldenFileNameWithExt])
        }
    }

    data class Deleted(
        override val testMethodName: String,
        override val goldenName: String,
        val testGoldensVfs: VfsFile,
        val oldBitmap: Bitmap,
    ) : KorgeScreenshotTestResult(testMethodName, goldenName) {
        override suspend fun acceptChange() {
            println("Deleting golden! $goldenFileNameWithExt")
            testGoldensVfs[goldenFileNameWithExt].delete()
        }
    }
}
