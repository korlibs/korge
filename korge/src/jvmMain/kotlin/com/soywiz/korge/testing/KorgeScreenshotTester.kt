package com.soywiz.korge.testing

import com.soywiz.klock.*
import com.soywiz.klogger.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.sync.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

private val logger = Logger("KorgeScreenshotTester")

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
        logger.info { "=".repeat(LINE_BREAK_WIDTH) }
        logger.info { "Korge Tester initializing..." }
        logger.info { "Local test time: $currentTime" }
        logger.info { "Goldens directory: ${testGoldensVfs.absolutePath}" }
        logger.info { "Temp directory: ${tempGoldensVfs.absolutePath}" }
        logger.info { "=".repeat(LINE_BREAK_WIDTH) }
    }

    suspend fun init() {
        testGoldensVfs.mkdirs()
        tempGoldensVfs.mkdirs()

        val prefix = "${testMethodName}_"

        existingGoldenNames = testGoldensVfs.listNames().filter {
            it.startsWith(prefix)
        }.map { it.removeSurrounding(prefix, ".png") }.toSet()

        logger.info { "Existing golden names" }
        logger.info { existingGoldenNames }
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
    //  Note: Do not add a file extension to the end.
    suspend fun recordGolden(
        view: View,
        goldenName: String,
        settingOverride: KorgeScreenshotValidationSettings = defaultValidationSettings,
        includeBackgroundColor: Boolean = true,
        posterize: Int = 0
    ) {
        val bitmap: Bitmap32 = view.renderToBitmap(views, includeBackground = includeBackgroundColor)
        require(!recordedGoldenNames.contains(goldenName)) {
            """
                Golden collision for name: $goldenName
                Please rename your golden!
            """.trimIndent()
        }
        recordedGoldenNames[goldenName] = settingOverride
        val fileName =
            context.makeGoldenFileNameWithExtension(goldenName)

        if (posterize > 0) {
            bitmap.posterizeInplace(posterize)
        }

        val bmp = bitmap.tryToExactBitmap8() ?: bitmap.toBMP32IfRequired()

        context.tempGoldensVfs[fileName].writeBitmap(bmp, PNG, ImageEncodingProps(quality = 1.0))

        //try {
        //    context.tempGoldensVfs[fileName].writeBytes(compress(bmp.toAwt(), "png").readBytes())
        //} catch (e: Throwable) {
        //    e.printStackTrace()
        //    context.tempGoldensVfs[fileName].writeBitmap(bmp, PNG)
        //}
    }

    // @TODO: Create java native writer
    //@Throws(Exception::class)
    //protected fun compress(image: BufferedImage, formatName: String): InputStream {
    //    var image = image
    //    var formatName = formatName
    //    if (formatName.lowercase(Locale.getDefault()) == "jpeg" || formatName.lowercase(Locale.getDefault()) == "jpg") {
    //        //image = stripAlpha(image)
    //    }
    //    val byteArrayOutputStream = ByteArrayOutputStream()
    //    val bos = BufferedOutputStream(byteArrayOutputStream)
    //    if (formatName.lowercase(Locale.getDefault()) == "gif") {
    //        formatName = "png"
    //    }
    //    val writerIter = ImageIO.getImageWritersByFormatName(formatName)
    //    val writer = writerIter.next()
    //    val iwp = writer.defaultWriteParam
    //    if (formatName.lowercase(Locale.getDefault()) == "jpeg" || formatName.lowercase(Locale.getDefault()) == "jpg") {
    //        iwp.compressionMode = ImageWriteParam.MODE_EXPLICIT
    //        iwp.compressionQuality = 0.85f
    //        iwp.progressiveMode = ImageWriteParam.MODE_DEFAULT
    //    }
    //    val output = MemoryCacheImageOutputStream(bos)
    //    writer.output = output
    //    val iomage = IIOImage(image, null, null)
    //    writer.write(null, iomage, iwp)
    //    bos.flush()
    //    return ByteArrayInputStream(byteArrayOutputStream.toByteArray())
    //}

    private suspend fun processGoldenResults() {
        logger.info { "Processing golden results" }
        recordedGoldenNames.forEach { (goldenName, validationSetting) ->
            logger.info { "Processing: $goldenName" }
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
        logger.info { "Ending test" }
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
