import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.korge.Korge
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.anchor
import com.soywiz.korge.view.position
import com.soywiz.korge.view.renderToBitmap
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.text
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.format.writeBitmap
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.localCurrentDirVfs
import com.soywiz.korma.geom.ISizeInt
import com.soywiz.korma.geom.degrees
import kotlinx.coroutines.sync.Mutex
import org.junit.Test

data class KorgeTestResult(
    val testMethodName: String,
    val goldenName: String
)

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

    val recordedGoldenFileNames = mutableSetOf<String>()

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

    fun makeGoldenFileNameWithExtension(goldenName: String) = "${testMethodName}_$goldenName.png"

    // name: The name of the golden. (e.g: "cool_view").
    //  Note: Do not add a file extension to the end.
    suspend fun recordGolden(view: View, goldenName: String) {
        val bitmap = view.renderToBitmap(views)
        val fileName = makeGoldenFileNameWithExtension(goldenName)
        require(!recordedGoldenFileNames.contains(fileName)) {
            """
                Golden collision for name: $fileName
                Please rename your golden!
            """.trimIndent()
        }
        recordedGoldenFileNames.add(fileName)
        tempVfs[fileName].writeBitmap(bitmap, PNG)
    }

    suspend fun processGoldenResults() {
        println("Processing golden results")
        recordedGoldenFileNames.forEach {
            println("Processing: $it")
            val testGoldenImage = testGoldensVfs[it]
            val existsGoldenInGoldenDirectory = testGoldenImage.exists()
            println("existsGoldenInGoldenDirectory: $existsGoldenInGoldenDirectory")
            if (existsGoldenInGoldenDirectory) {
                val oldBitmap = testGoldenImage.readBitmap(PNG)
                val newBitmap = tempVfs.readBitmap(PNG)
                TODO()
            } else {
                tempVfs[it].copyToTree(testGoldenImage)
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
        val korge = Korge(korgeConfig)
        //        val korge = Korge(korgeConfig) {
        //            val korgeTester = KorgeTester(views)
        //            korgeTester.init()
        //            callback(this, korgeTester)
        //        }

//        while (testingLock.isLocked) { }
//        val config = Korge.Config(
//            windowSize = ISizeInt.invoke(640, 480),
//            virtualSize = ISizeInt.invoke(640, 480),
//            main = {
//                views.gameWindow.exitProcessOnExit = false
//                text("Hello world")
//            })
//        Korge(config)

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

        //            while (true) {
        //                println("STEP")
        //                image.tween(image::rotation[minDegrees], time = 0.5.seconds, easing = Easing.EASE_IN_OUT)
        //                image.tween(image::rotation[maxDegrees], time = 0.5.seconds, easing = Easing.EASE_IN_OUT)
        //                views.gameWindow.close() // We close the window, finalizing the test here
        //            }

        //                views.gameWindow.close()
        //        }

        it.endTest()
    }

}
