package com.soywiz.korge.testing

import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.renderToBitmap
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.format.writeBitmap
import com.soywiz.korio.file.std.localCurrentDirVfs
import kotlinx.coroutines.sync.Mutex

class KorgeScreenshotTester(
    val views: Views,
    val testClassName: String,
    val testMethodName: String,
    // We will unlock this mutex after the test ends (via `endTest`).
    // This is to ensure that dependents have a safe way on waiting on the tester to finish.
    val testingLock: Mutex,
    val testResultsOutput: KorgeScreenshotTestResults
) {
    val testGoldensVfs = localCurrentDirVfs["testGoldens/$testClassName"]
    val localTestTime = DateTime.nowLocal()
    val tempVfs = localCurrentDirVfs["build/tmp/testGoldens/$testClassName/${
        PATH_DATE_FORMAT.format(localTestTime)
    }"]

    lateinit var existingGoldenNames: Set<String>

    val recordedGoldenNames = mutableSetOf<String>()

    init {
        require(testingLock.isLocked) {
            "Please provide a locked testing mutex."
        }
        val currentTime = DATE_FORMAT.format(localTestTime)
        println("=".repeat(LINE_BREAK_WIDTH))
        println("Korge Tester initializing...")
        println("Local test time: $currentTime")
        println("Goldens directory: ${testGoldensVfs.absolutePath}")
        println("Temp directory: ${tempVfs.absolutePath}")
        println("=".repeat(LINE_BREAK_WIDTH))
    }

    suspend fun init() {
        testGoldensVfs.mkdirs()
        tempVfs.mkdirs()

        val prefix = "${testMethodName}_"

        existingGoldenNames = testGoldensVfs.listNames().filter {
            it.startsWith(prefix)
        }.map { it.removeSurrounding(prefix, ".png") }.toSet()

        println("Existing golden names")
        println(existingGoldenNames)
    }

    // name: The name of the golden. (e.g: "cool_view").
    //  Note: Do not add a file extension to the end.
    suspend fun recordGolden(view: View, goldenName: String) {
        val bitmap = view.renderToBitmap(views)
        require(!recordedGoldenNames.contains(goldenName)) {
            """
                Golden collision for name: $goldenName
                Please rename your golden!
            """.trimIndent()
        }
        recordedGoldenNames.add(goldenName)
        val fileName = KorgeScreenshotTesterUtils.makeGoldenFileNameWithExtension(testMethodName, goldenName)
        tempVfs[fileName].writeBitmap(bitmap, PNG)
    }

    private suspend fun processGoldenResults() {
        println("Processing golden results")
        recordedGoldenNames.forEach { goldenName ->
            println("Processing: $goldenName")
            val goldenFileName =
                KorgeScreenshotTesterUtils.makeGoldenFileNameWithExtension(testMethodName, goldenName)
            val testGoldenImage = testGoldensVfs[goldenFileName]
            val existsGoldenInGoldenDirectory = testGoldenImage.exists()
            if (existsGoldenInGoldenDirectory) {
                val oldBitmap = testGoldenImage.readBitmap(PNG)
                val newBitmap = tempVfs[goldenFileName].readBitmap(PNG)
                if (!oldBitmap.contentEquals(newBitmap)) {
                    testResultsOutput.results.add(
                        KorgeScreenshotTestResult.Diff(
                            testMethodName,
                            goldenName,
                            testGoldensVfs,
                            tempVfs,
                            oldBitmap,
                            newBitmap
                        )
                    )
                }
            } else {
                // This is a new golden, so we can just save it.
                tempVfs[goldenFileName].copyTo(testGoldenImage)
            }
        }

        // Process any deleted goldens.
        (existingGoldenNames - recordedGoldenNames).forEach { goldenName ->
            val goldenFileName =
                KorgeScreenshotTesterUtils.makeGoldenFileNameWithExtension(testMethodName, goldenName)
            val oldBitmap = testGoldensVfs[goldenFileName].readBitmap(PNG)
            testResultsOutput.results.add(
                KorgeScreenshotTestResult.Deleted(
                    testMethodName,
                    goldenName,
                    testGoldensVfs,
                    oldBitmap
                )
            )
        }
    }

    private fun contentEquals(left: Bitmap, other: Bitmap): Boolean {
        if (left.width != other.width) return false
        if (left.height != other.height) return false
        for (y in 0 until left.height) for (x in 0 until left.width) {
            if (left.getRgbaRaw(x, y) != other.getRgbaRaw(x, y)) return false
        }
        return true
    }

    suspend fun endTest() {
        println("Ending test")
        processGoldenResults()

        views.gameWindow.exitProcessOnExit = false
        views.gameWindow.close()
        testingLock.unlock()
    }

    companion object {
        val DATE_FORMAT = DateFormat("yyyy-dd-MM HH:mm:ss z")
        val PATH_DATE_FORMAT = DateFormat("yyyyMMdd_HH_mm_ss_z")
        private val LINE_BREAK_WIDTH = 100
    }
}
