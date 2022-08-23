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

data class KorgeScreenshotTestingContext(
    val testClassName: String,
    val testMethodName: String,
) {
    val testGoldensVfs = localCurrentDirVfs["testGoldens/${testClassName}"]
    private val localTestTime = DateTime.nowLocal()
    val tempGoldensVfs =
        localCurrentDirVfs["build/tmp/testGoldens/${testClassName}/${
            KorgeScreenshotTester.PATH_DATE_FORMAT.format(localTestTime)
        }"]

    lateinit var existingGoldenNames: Set<String>

    init {
        val currentTime = DATE_FORMAT.format(localTestTime)
        println("=".repeat(LINE_BREAK_WIDTH))
        println("Korge Tester initializing...")
        println("Local test time: $currentTime")
        println("Goldens directory: ${testGoldensVfs.absolutePath}")
        println("Temp directory: ${tempGoldensVfs.absolutePath}")
        println("=".repeat(LINE_BREAK_WIDTH))
    }

    suspend fun init() {
        testGoldensVfs.mkdirs()
        tempGoldensVfs.mkdirs()

        val prefix = "${testMethodName}_"

        existingGoldenNames = testGoldensVfs.listNames().filter {
            it.startsWith(prefix)
        }.map { it.removeSurrounding(prefix, ".png") }.toSet()

        println("Existing golden names")
        println(existingGoldenNames)
    }

    fun makeGoldenFileNameWithExtension(goldenName: String) =
        "${testMethodName}_$goldenName.png"

    companion object {
        val DATE_FORMAT = DateFormat("yyyy-dd-MM HH:mm:ss z")
        private const val LINE_BREAK_WIDTH = 100

    }
}

class KorgeScreenshotTester(
    val views: Views,
    val context: KorgeScreenshotTestingContext,
    private val settings: KorgeScreenshotTestSettings,
    // We will unlock this mutex after the test ends (via `endTest`).
    // This is to ensure that dependents have a safe way on waiting on the tester to finish.
    private val testingLock: Mutex,
    private val testResultsOutput: KorgeScreenshotTestResults,
) {
    val recordedGoldenNames = mutableSetOf<String>()

    init {
        require(testingLock.isLocked) {
            "Please provide a locked testing mutex."
        }
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
        val fileName =
            context.makeGoldenFileNameWithExtension(goldenName)
        context.tempGoldensVfs[fileName].writeBitmap(bitmap, PNG)
    }

    private suspend fun processGoldenResults() {
        println("Processing golden results")
        recordedGoldenNames.forEach { goldenName ->
            println("Processing: $goldenName")
            val goldenFileName =
                context.makeGoldenFileNameWithExtension(
                    goldenName
                )
            val testGoldenImage = context.testGoldensVfs[goldenFileName]
            val existsGoldenInGoldenDirectory = testGoldenImage.exists()
            if (existsGoldenInGoldenDirectory) {
                val oldBitmap = testGoldenImage.readBitmap(PNG)
                val newBitmap = context.tempGoldensVfs[goldenFileName].readBitmap(PNG)
                if (!oldBitmap.contentEquals(newBitmap)) {
                    testResultsOutput.results.add(
                        KorgeScreenshotTestResult(
                            goldenName,
                            oldBitmap,
                            newBitmap
                        )
                    )
                }
            } else {
                // This is a new golden, so we can just save it.
                context.tempGoldensVfs[goldenFileName].copyTo(testGoldenImage)
            }
        }

        // Process any deleted goldens.
        (context.existingGoldenNames - recordedGoldenNames).forEach { goldenName ->
            val goldenFileName =
                context.makeGoldenFileNameWithExtension(
                    goldenName
                )
            val oldBitmap = context.testGoldensVfs[goldenFileName].readBitmap(PNG)
            testResultsOutput.results.add(
                KorgeScreenshotTestResult(
                    goldenName,
                    oldBitmap,
                    null
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
        val PATH_DATE_FORMAT = DateFormat("yyyyMMdd_HH_mm_ss_z")
        private const val LINE_BREAK_WIDTH = 100
    }
}
