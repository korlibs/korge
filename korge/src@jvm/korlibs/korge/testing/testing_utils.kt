package korlibs.korge.testing

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.io.lang.*
import korlibs.korge.*
import korlibs.korge.annotations.*
import korlibs.korge.input.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge.view.Container
import korlibs.korge.view.align.*
import korlibs.math.geom.*
import kotlinx.coroutines.sync.*
import java.awt.*

@OptIn(KorgeExperimental::class)
inline fun korgeScreenshotTestV2(
    korgeConfig: Korge,
    settings: KorgeScreenshotValidationSettings = KorgeScreenshotValidationSettings(),
    crossinline callback: suspend Stage.(korgeScreenshotTester: KorgeScreenshotTester) -> Unit = {},
) {
//    System.setProperty("java.awt.headless", "false")
    val throwable = Throwable()
    val testClassName = throwable.stackTrace[0].className
    val testMethodName = throwable.stackTrace[0].methodName
    val context = KorgeScreenshotTestingContext(testClassName, testMethodName)
    if (Environment["DISABLE_HEADLESS_TEST"] == "true") {
        System.err.println("Ignoring test $context because env DISABLE_HEADLESS_TEST=true")
        return
    }

    val existingMain = korgeConfig.main
    val results = KorgeScreenshotTestResults(testMethodName)
    // Keep locked while tester is running
    val testingLock = Mutex(locked = true)
    val finalKorgeConfig = korgeConfig.copy(main = {
        context.init()
        val korgeScreenshotTester =
            KorgeScreenshotTester(views, context, settings, testingLock, results)
        // If there already exists a main, run that first.
        // This allows people to test their existing modules.
        existingMain.invoke(this)
        callback(this, korgeScreenshotTester)
    })
    suspendTestWithOffscreenAG(fboSize = finalKorgeConfig.windowSize) suspendTest@{
        try {
            KorgeHeadless.invoke(config = finalKorgeConfig, entry = finalKorgeConfig.main, ag = it)
        } catch (exception: HeadlessException) {
            // Running in a headless environment (e.g github tests).
            // Return to mark as passing.
            return@suspendTest
        }

        while (testingLock.isLocked) {
            println("Waiting for test to end...")
        }

        val resultsWithErrors = results.results.filter { result ->
            result.hasValidationErrors
        }

        // No diffs, no need to show UI to update goldens.
        if (resultsWithErrors.isEmpty()) return@suspendTest

        println("Diffs found...")
        val interactive = Environment["INTERACTIVE_SCREENSHOT"] == "true"
        if (interactive) {
            val config = Korge(
                backgroundColor = Colors.LIGHTGRAY,
                windowSize = Size(1280, 720),
                virtualSize = Size(700, 480),
                main = {
                    views.gameWindow.exitProcessOnClose = false

                    uiScrollable(size = Size(700.0, 480.0)) { uiScrollable ->
                        uiScrollable.backgroundColor = Colors.LIGHTGRAY
                        var prevContainer: Container? = null
                        resultsWithErrors.forEach { testResult ->
                            val viewsToAlign = mutableListOf<View>()
                            container {
                                viewsToAlign += text("Test method name: ${results.testMethodName}")
                                //                            val testResultSection = container {
                                viewsToAlign += text("Golden name: ${testResult.goldenName}")
                                viewsToAlign += container {
                                    val fn = { headerText: String, bitmap: Bitmap? ->
                                        container {
                                            val headerText = if (bitmap == null) {
                                                text(headerText)
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
                                    val oldImage = fn("Old Image", testResult.oldBitmap)
                                    val newImage =
                                        fn("New Image", testResult.newBitmap).apply {
                                            alignLeftToRightOf(oldImage, padding = 5.0)
                                        }
                                }

                                val separator = "\n * "
                                viewsToAlign += container {
                                    val textResultString = testResult.validationErrors.joinToString(
                                        separator = separator,
                                        prefix = separator
                                    ) {
                                        it.errorMessaage
                                    }
                                    text("Validation errors:$textResultString")
                                }

                                viewsToAlign += uiButton("Accept change?") {
                                    centerXOn(this@container)
                                    onClickSuspend {
                                        val goldenFileNameWithExt =
                                            context.makeGoldenFileNameWithExtension(testResult.goldenName)
                                        if (testResult.newBitmap != null) {
                                            context.tempGoldensVfs[goldenFileNameWithExt].copyTo(
                                                context.testGoldensVfs[goldenFileNameWithExt]
                                            )
                                        } else {
                                            // Bitmap was deleted
                                            context.testGoldensVfs[goldenFileNameWithExt].delete()
                                        }
                                        disable()
                                    }
                                }

                                viewsToAlign.windowed(2) {
                                    it[1].alignTopToBottomOf(it[0])
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
            config.start()
        } else {
            println("Diffs found... Update goldens with INTERACTIVE_SCREENSHOT=true.")
        }
    }
}
