package korlibs.korge.testing

import korlibs.image.bitmap.*
import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.korge.annotations.*
import korlibs.korge.view.*
import korlibs.time.*
import kotlinx.coroutines.sync.*

data class KorgeScreenshotValidationSettings(
    val validators: List<KorgeScreenshotValidator> = listOf(
        DefaultValidator
    )
) {
    fun validate(goldenName: String, oldBitmap: Bitmap, newBitmap: Bitmap?): List<KorgeScreenshotValidatorResult> {
        return validators.map {
            it.validate(goldenName, oldBitmap, newBitmap)
        }
    }
}

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
    private val context: KorgeScreenshotTestingContext,
    private val defaultValidationSettings: KorgeScreenshotValidationSettings,
    // We will unlock this mutex after the test ends (via `endTest`).
    // This is to ensure that dependents have a safe way on waiting on the tester to finish.
    private val testingLock: Mutex,
    private val testResultsOutput: KorgeScreenshotTestResults,
) {
    private val recordedGoldenNames = mutableMapOf<String, KorgeScreenshotValidationSettings>()

    init {
        require(testingLock.isLocked) {
            "Please provide a locked testing mutex."
        }
    }

    // name: The name of the golden. (e.g: "cool_view").
    // Note: You do not need to add a file extension to the end.
    @OptIn(KorgeExperimental::class)
    suspend fun recordGolden(
        view: View,
        goldenName: String,
        settingOverride: KorgeScreenshotValidationSettings = defaultValidationSettings,
        includeBackground: Boolean = true
    ) {
        val bitmap = views.simulateRenderFrame(view, includeBackground = includeBackground)
        require(!recordedGoldenNames.contains(goldenName)) {
            """
                Golden collision for name: $goldenName
                Please rename your golden!
            """.trimIndent()
        }
        recordedGoldenNames[goldenName] = settingOverride
        val fileName =
            context.makeGoldenFileNameWithExtension(goldenName)
        context.tempGoldensVfs[fileName].writeBitmap(bitmap, PNG)
    }

    private suspend fun processGoldenResults() {
        println("Processing golden results")
        recordedGoldenNames.forEach { (goldenName, validationSetting) ->
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
                testResultsOutput.results.add(
                    KorgeScreenshotTestResult(
                        goldenName,
                        oldBitmap,
                        newBitmap,
                        validationSetting.validate(goldenName, oldBitmap, newBitmap)
                    )
                )
            } else {
                // This is a new golden, so we can just save it.
                context.tempGoldensVfs[goldenFileName].copyTo(testGoldenImage)
            }
        }

        // Process any deleted goldens.
        (context.existingGoldenNames - recordedGoldenNames.keys).forEach { goldenName ->
            val goldenFileName =
                context.makeGoldenFileNameWithExtension(
                    goldenName
                )
            val oldBitmap = context.testGoldensVfs[goldenFileName].readBitmap(PNG)
            testResultsOutput.results.add(
                KorgeScreenshotTestResult(
                    goldenName,
                    oldBitmap,
                    null,
                    listOf(DeletedGoldenValidator.validate(goldenName, oldBitmap, null))
                )
            )
        }
    }

    suspend fun endTest() {
        println("Ending test")
        processGoldenResults()

        views.gameWindow.exitProcessOnClose = false
        views.gameWindow.close()
        testingLock.unlock()
    }

    companion object {
        val PATH_DATE_FORMAT = DateFormat("yyyyMMdd_HH_mm_ss_z")
        private const val LINE_BREAK_WIDTH = 100
    }
}
