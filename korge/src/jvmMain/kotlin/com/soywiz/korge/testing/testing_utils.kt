package com.soywiz.korge.testing

import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.korge.Korge
import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korge.input.onClick
import com.soywiz.korge.testing.KorgeTesterUtils.makeGoldenFileNameWithExtension
import com.soywiz.korge.ui.uiButton
import com.soywiz.korge.ui.uiScrollable
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.ScalingOption
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.alignLeftToRightOf
import com.soywiz.korge.view.alignTopToBottomOf
import com.soywiz.korge.view.centerOn
import com.soywiz.korge.view.centerXOn
import com.soywiz.korge.view.centerXOnStage
import com.soywiz.korge.view.container
import com.soywiz.korge.view.image
import com.soywiz.korge.view.renderToBitmap
import com.soywiz.korge.view.scaleWhileMaintainingAspect
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.text
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.format.writeBitmap
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.localCurrentDirVfs
import com.soywiz.korma.geom.ISizeInt
import kotlinx.coroutines.sync.Mutex

object KorgeTesterUtils {
    fun makeGoldenFileNameWithExtension(testMethodName: String, goldenName: String) =
        "${testMethodName}_$goldenName.png"
}

sealed class KorgeTestResult(
    open val testMethodName: String,
    open val goldenName: String
) {
    abstract suspend fun acceptChange()

    val goldenFileNameWithExt: String
        get() = makeGoldenFileNameWithExtension(testMethodName, goldenName)

    data class Diff(
        override val testMethodName: String,
        override val goldenName: String,
        val testGoldensVfs: VfsFile,
        val tempVfs: VfsFile,
        val oldBitmap: Bitmap,
        val newBitmap: Bitmap
    ) : KorgeTestResult(testMethodName, goldenName) {
        override suspend fun acceptChange() {
            tempVfs[goldenFileNameWithExt].copyTo(testGoldensVfs[goldenFileNameWithExt])
        }
    }

    data class Deleted(
        override val testMethodName: String,
        override val goldenName: String,
        val testGoldensVfs: VfsFile,
        val oldBitmap: Bitmap,
    ) : KorgeTestResult(testMethodName, goldenName) {
        override suspend fun acceptChange() {
            println("Deleting golden! $goldenFileNameWithExt")
            testGoldensVfs[goldenFileNameWithExt].delete()
        }
    }
}

data class KorgeTestResults(
    val testMethodName: String,
    val results: MutableList<KorgeTestResult> = mutableListOf()
)

class KorgeTester(
    val views: Views,
    val testClassName: String,
    val testMethodName: String,
    // We will unlock this mutex after the test ends (via `endTest`).
    // This is to ensure that dependents have a safe way on waiting on the tester to finish.
    val testingLock: Mutex,
    val testResultsOutput: KorgeTestResults
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
        val fileName = makeGoldenFileNameWithExtension(testMethodName, goldenName)
        tempVfs[fileName].writeBitmap(bitmap, PNG)
    }

    suspend fun processGoldenResults() {
        println("Processing golden results")
        recordedGoldenNames.forEach { goldenName ->
            println("Processing: $goldenName")
            val goldenFileName = makeGoldenFileNameWithExtension(testMethodName, goldenName)
            val testGoldenImage = testGoldensVfs[goldenFileName]
            val existsGoldenInGoldenDirectory = testGoldenImage.exists()
            if (existsGoldenInGoldenDirectory) {
                val oldBitmap = testGoldenImage.readBitmap(PNG)
                val newBitmap = tempVfs[goldenFileName].readBitmap(PNG)
                if (!oldBitmap.contentEquals(newBitmap)) {
                    testResultsOutput.results.add(
                        KorgeTestResult.Diff(
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
            val goldenFileName = makeGoldenFileNameWithExtension(testMethodName, goldenName)
            val oldBitmap = testGoldensVfs[goldenFileName].readBitmap(PNG)
            testResultsOutput.results.add(
                KorgeTestResult.Deleted(
                    testMethodName,
                    goldenName,
                    testGoldensVfs,
                    oldBitmap
                )
            )
        }
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

@OptIn(KorgeExperimental::class)
inline fun Any.korgeTest(
    korgeConfig: Korge.Config,
    crossinline callback: suspend Stage.(korgeTester: KorgeTester) -> Unit,
) {
    System.setProperty("java.awt.headless", "false")
    val throwable = Throwable()
    val testClassName = throwable.stackTrace[0].className
    val testMethodName = throwable.stackTrace[0].methodName
    val existingMain = korgeConfig.main
    val results = KorgeTestResults(testMethodName)
    // Keep locked while tester is running
    val testingLock = Mutex(locked = true)
    korgeConfig.main = {
        val korgeTester = KorgeTester(views, testClassName, testMethodName, testingLock, results)
        korgeTester.init()
        // If there already exists a main, run that first.
        existingMain?.invoke(this)
        callback(this, korgeTester)
    }
    suspendTest {
        Korge(korgeConfig)

        while (testingLock.isLocked) {
            println("Waiting for test to end.")
        }

        // No diffs, no need to show UI to update goldens.
        if (results.results.isEmpty()) return@suspendTest

        val config = Korge.Config(
            bgcolor = Colors.LIGHTGRAY,
            windowSize = ISizeInt.invoke(1280, 720),
            virtualSize = ISizeInt.invoke(700, 480),
            main = {
                views.gameWindow.exitProcessOnExit = false

                uiScrollable(700.0, 480.0) {
                    it.backgroundColor = Colors.LIGHTGRAY
                    var prevContainer: Container? = null
                    results.results.forEach { testResult ->
                        container {
                            val description = text("Test method name: ${results.testMethodName}")
                            container {
                                val goldenNameText = text("Golden name: ${testResult.goldenName}")
                                val diffSection = container {
                                    val fn = { headerText: String, bitmap: Bitmap? ->
                                        container {
                                            val headerText = if (bitmap == null) {
                                                text("$headerText")
                                            } else {
                                                text("$headerText (${bitmap.size.width.toInt()} x ${bitmap.size.height.toInt()})")
                                            }

                                            val rect =
                                                solidRect(
                                                    320,
                                                    240,
                                                    color = Colors.BLACK.withAd(0.75)
                                                ) {
                                                    alignTopToBottomOf(headerText)
                                                }
                                            if (bitmap == null) {
                                                text("Deleted") {
                                                    centerOn(rect)
                                                }
                                            } else {
                                                image(bitmap).apply {
                                                    scaleWhileMaintainingAspect(
                                                        ScalingOption.ByWidthAndHeight(
                                                            310.0,
                                                            230.0
                                                        )
                                                    )
                                                    centerOn(rect)
                                                }
                                            }

                                        }
                                    }
                                    when (testResult) {
                                        is KorgeTestResult.Diff -> {
                                            val oldImage = fn("Old Image", testResult.oldBitmap)
                                            val newImage =
                                                fn("New Image", testResult.newBitmap).apply {
                                                    alignLeftToRightOf(oldImage, padding = 5.0)
                                                }
                                        }
                                        is KorgeTestResult.Deleted -> {
                                            val oldImage = fn("Old Image", testResult.oldBitmap)
                                            val newImage =
                                                fn("New Image", null).apply {
                                                    alignLeftToRightOf(oldImage, padding = 5.0)
                                                }
                                        }
                                    }
                                    alignTopToBottomOf(goldenNameText)
                                }


                                uiButton("Accept change?", width = 125.0) {
                                    alignTopToBottomOf(diffSection)
                                    centerXOn(this@container)
                                    onClick {
                                        testResult.acceptChange()
                                        disable()
                                    }
                                }
                                alignTopToBottomOf(description)
                            }

                            val sectionBg =
                                solidRect(scaledWidth, scaledHeight, Colors.DARKSLATEGRAY)
                            sendChildToBack(sectionBg)

                            centerXOnStage()

                            if (prevContainer != null) {
                                alignTopToBottomOf(prevContainer!!, padding = 10.0)
                            }
                            prevContainer = this
                        }

                    }
                }

            })
        Korge(config)

    }
}
