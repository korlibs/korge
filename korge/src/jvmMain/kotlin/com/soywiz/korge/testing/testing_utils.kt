package com.soywiz.korge.testing

import com.soywiz.korge.Korge
import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korge.input.onClick
import com.soywiz.korge.ui.uiButton
import com.soywiz.korge.ui.uiScrollable
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.ScalingOption
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.alignLeftToRightOf
import com.soywiz.korge.view.alignTopToBottomOf
import com.soywiz.korge.view.centerOn
import com.soywiz.korge.view.centerXOn
import com.soywiz.korge.view.centerXOnStage
import com.soywiz.korge.view.container
import com.soywiz.korge.view.image
import com.soywiz.korge.view.scaleWhileMaintainingAspect
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.text
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.suspendTest
import com.soywiz.korma.geom.ISizeInt
import kotlinx.coroutines.sync.Mutex
import java.awt.HeadlessException

//object KorgeScreenshotTesterUtils {
//    fun makeGoldenFileNameWithExtension(testMethodName: String, goldenName: String) =
//        "${testMethodName}_$goldenName.png"
//}

@OptIn(KorgeExperimental::class)
inline fun korgeScreenshotTest(
    korgeConfig: Korge.Config,
    settings: KorgeScreenshotTestSettings = KorgeScreenshotTestSettings(),
    crossinline callback: suspend Stage.(korgeScreenshotTester: KorgeScreenshotTester) -> Unit,
) {
    System.setProperty("java.awt.headless", "false")
    val throwable = Throwable()
    val testClassName = throwable.stackTrace[0].className
    val testMethodName = throwable.stackTrace[0].methodName
    val existingMain = korgeConfig.main
    val results = KorgeScreenshotTestResults(testMethodName)
    // Keep locked while tester is running
    val testingLock = Mutex(locked = true)
    val context = KorgeScreenshotTestingContext(testClassName, testMethodName)
    val finalKorgeConfig = korgeConfig.copy(main = {
        context.init()
        val korgeScreenshotTester =
            KorgeScreenshotTester(views, context, settings, testingLock, results)
        // If there already exists a main, run that first.
        // This allows people to test their existing modules.
        existingMain?.invoke(this)
        callback(this, korgeScreenshotTester)
    })
    suspendTest {
        try {
            Korge(finalKorgeConfig)
        } catch (exception: HeadlessException) {
            // Running in a headless environment (e.g github tests).
            // Return to mark as passing.
            return@suspendTest
        }

        while (testingLock.isLocked) {
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
                                    val oldImage = fn("Old Image", testResult.oldBitmap)
                                    val newImage =
                                        fn("New Image", testResult.newBitmap).apply {
                                            alignLeftToRightOf(oldImage, padding = 5.0)
                                        }
                                    alignTopToBottomOf(goldenNameText)
                                }


                                uiButton("Accept change?", width = 125.0) {
                                    alignTopToBottomOf(diffSection)
                                    centerXOn(this@container)
                                    onClick {
                                        val goldenFileNameWithExt =
                                            context.makeGoldenFileNameWithExtension(testResult.goldenName)
                                        if (testResult.oldBitmap != null && testResult.newBitmap != null) {
                                            context.tempGoldensVfs[goldenFileNameWithExt].copyTo(
                                                context.testGoldensVfs[goldenFileNameWithExt]
                                            )
                                        } else if (testResult.oldBitmap != null && testResult.newBitmap == null) {
                                            // Bitmap was deleted
                                            context.testGoldensVfs[goldenFileNameWithExt].delete()
                                        }
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
