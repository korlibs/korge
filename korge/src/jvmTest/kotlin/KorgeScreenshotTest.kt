import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.korge.Korge
import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korge.input.onClick
import com.soywiz.korge.ui.uiButton
import com.soywiz.korge.ui.uiContainer
import com.soywiz.korge.ui.uiScrollable
import com.soywiz.korge.ui.uiVerticalStack
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.ScalingOption
import com.soywiz.korge.view.SolidRect
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.alignBottomToTopOf
import com.soywiz.korge.view.alignLeftToRightOf
import com.soywiz.korge.view.alignRightToRightOf
import com.soywiz.korge.view.alignTopToBottomOf
import com.soywiz.korge.view.anchor
import com.soywiz.korge.view.centerOn
import com.soywiz.korge.view.centerXOn
import com.soywiz.korge.view.centerXOnStage
import com.soywiz.korge.view.container
import com.soywiz.korge.view.image
import com.soywiz.korge.view.position
import com.soywiz.korge.view.renderToBitmap
import com.soywiz.korge.view.scaleWhileMaintainingAspect
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.text
import com.soywiz.korge.view.util.distributeEvenlyHorizontally
import com.soywiz.korge.view.util.distributeEvenlyVertically
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.format.writeBitmap
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.localCurrentDirVfs
import com.soywiz.korma.geom.ISizeInt
import com.soywiz.korma.geom.degrees
import kotlinx.coroutines.sync.Mutex
import org.junit.Test

fun makeGoldenFileNameWithExtension(testMethodName: String, goldenName: String) = "${testMethodName}_$goldenName.png"

data class KorgeTestResult(
    val testMethodName: String,
    val goldenName: String,
    val testGoldensVfs: VfsFile,
    val tempVfs: VfsFile,
    val oldBitmap: Bitmap,
    val newBitmap: Bitmap
) {
    suspend fun updateGoldenFileToNewBitmap() {
        val goldenFileName = makeGoldenFileNameWithExtension(testMethodName, goldenName)
        tempVfs[goldenFileName].copyTo(testGoldensVfs[goldenFileName])
    }
}

data class KorgeTestResults(
    val results: MutableList<KorgeTestResult> = mutableListOf()
)

class KorgeTester(
    val views: Views,
    val testClassName: String,
    val testMethodName: String,
    // We will unlock this mutex after the test ends (via `endTest`).
    // This is to ensure that dependents have a safe way on waiting on the tester to finish.
    val testingLock: Mutex,
    val testResultsOutput: KorgeTestResults,
    // If this is the first time we've seen the golden, then we will create it automatically.
    val createGoldenIfNotExists: Boolean = true
) {
    val testGoldensVfs = localCurrentDirVfs["testGoldens/$testClassName"]
    val localTestTime = DateTime.nowLocal()
    val tempVfs = localCurrentDirVfs["build/tmp/testGoldens/$testClassName/${
        PATH_DATE_FORMAT.format(localTestTime)
    }"]

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

        println(this::class)
        println(this::class.java)
        println(this::class.java.enclosingClass)
        //        println(this::class.java.getResource())
    }

    suspend fun init() {
        testGoldensVfs.mkdirs()
        tempVfs.mkdirs()
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
            println("existsGoldenInGoldenDirectory: $existsGoldenInGoldenDirectory")
            if (existsGoldenInGoldenDirectory) {
                val oldBitmap = testGoldenImage.readBitmap(PNG)
                val newBitmap = tempVfs[goldenFileName].readBitmap(PNG)
                if (!oldBitmap.contentEquals(newBitmap)) {
                    testResultsOutput.results.add(
                        KorgeTestResult(
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
                tempVfs[goldenFileName].copyToTree(testGoldenImage)
            }
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
    val results = KorgeTestResults()
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
        //        val korge = Korge(korgeConfig) {
        //            val korgeTester = KorgeTester(views)
        //            korgeTester.init()
        //            callback(this, korgeTester)
        //        }

        while (testingLock.isLocked) {
        }
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
                            val description = container {
                                distributeEvenlyVertically(
                                    listOf(
                                        text("Test method name: ${testResult.testMethodName}"),
                                        text("Golden name: ${testResult.goldenName}")
                                    ),
                                    35.0
                                )

                            }
                            container {
                                val fn = { headerText: String, bitmap: Bitmap ->
                                    container {
                                        val text =
                                            text("$headerText (${bitmap.size.width.toInt()} x ${bitmap.size.height.toInt()})")
                                        val rect =
                                            solidRect(320, 240, color = Colors.BLACK.withAd(0.75)) {
                                                alignTopToBottomOf(text)
                                            }
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
                                val oldImage = fn("Old Image", testResult.oldBitmap)
                                val newImage = fn("New Image", testResult.newBitmap).apply {
                                    alignLeftToRightOf(oldImage, padding = 5.0)
                                }
                                uiButton("Accept change?", width = 125.0) {
                                    alignTopToBottomOf(oldImage)
                                    centerXOn(this@container)
                                    onClick {
                                        testResult.updateGoldenFileToNewBitmap()
                                        disable()
                                    }
                                }
                                alignTopToBottomOf(description)
                            }

                            val sectionBg =
                                solidRect(scaledWidth, scaledHeight, Colors.DARKSLATEGRAY)
                            sendChildToBack(sectionBg)

                            println(
                                """
                                scaledWidth: $scaledWidth,
                                scaledHeight: $scaledHeight
                            """.trimIndent()
                            )

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

class KorgeScreenshotTest {
    @Test
    fun test1() = korgeTest(
        Korge.Config(
            windowSize = ISizeInt.invoke(512, 512),
            virtualSize = ISizeInt(512, 512),
            bgcolor = Colors.RED
        )
    ) {
        //        val gameWindow = Korge(width = 512, height = 512, bgcolor = Colors.RED) {
        val views = injector.get<Views>()
        val minDegrees = (-16).degrees
        val maxDegrees = (+16).degrees

        val image = solidRect(100, 100, Colors.YELLOW) {
            rotation = maxDegrees
            anchor(.5, .5)
            scale = 0.8
            position(256, 256)
        }

        //            println("applicationVfs: $applicationVfs")
        //            println("localCurrentDirVfs: $localCurrentDirVfs")
        //            println("rootLocalVfs: $rootLocalVfs")
        //            println("tempVfs: $tempVfs")
        //            println("standardVfs: $standardVfs")
        //            println("applicationVfs[\"goldens\"]: ${applicationVfs["goldens"]}")
        //            val fileTest = localVfs(".")
        //            println("fileTest.absolutePath: ${fileTest.absolutePath}")
        //            val fileTest2 = localVfs("")
        //            println("fileTest2: ${fileTest2}")

        //        val ss1 = renderToBitmap(views)
        //        //                val ss1 = (gameWindow as KorgeHeadless.HeadlessGameWindow).bitmap
        //        resourcesVfs["ss2.png"].writeBitmap(ss1, PNG)

        it.recordGolden(this, "initial1")

        val rect2 = solidRect(150, 150, Colors.YELLOW) {
            rotation = maxDegrees
            anchor(.5, .5)
            scale = 0.8
            position(350, 350)
        }

        it.recordGolden(rect2, "initial2")


        it.endTest()
    }

}
