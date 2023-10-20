package korlibs.korge.testing

import korlibs.datastructure.*
import korlibs.graphics.gl.*
import korlibs.image.awt.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.lang.*
import korlibs.korge.annotations.*
import korlibs.korge.view.*
import kotlinx.coroutines.*
import java.awt.*
import java.io.*
import javax.swing.*
import kotlin.math.*

object BitmapComparer {
    data class CompareResult(
        val pixelDiffCount: Int = -1,
        val pixelTotalDistance: Int = -1,
        val pixelMaxDistance: Int = -1,
        val psnr: Double = 0.0,
        val error: String = ""
    ) {
        //val strictEquals: Boolean get() = pixelDiffCount == 0
        //val reasonablySimilar: Boolean get() = pixelMaxDistance <= 3 || psnr >= 45.0
    }

    fun compare(left: Bitmap, right: Bitmap): CompareResult {
        val L = left.toBMP32().premultipliedIfRequired()
        val R = right.toBMP32().premultipliedIfRequired()
        if (L.premultiplied != R.premultiplied) {
            return CompareResult(error = "premultiplied left=${L.premultiplied}, right=${R.premultiplied}")
        }
        if (L.width != R.width || L.height != R.height) {
            return CompareResult(
                error = "dimensions left=${L.width}x${L.height}, right=${R.width}x${R.height}"
            )
        }
        var pixelDiffCount = 0
        var pixelTotalDistance = 0
        var pixelMaxDistance = 0
        loop@ for (y in 0 until L.height) for (x in 0 until L.width) {
            val lc = L.getRgbaPremultiplied(x, y)
            val rc = R.getRgbaPremultiplied(x, y)
            val Rdiff = (lc.r - rc.r).absoluteValue
            val Gdiff = (lc.g - rc.g).absoluteValue
            val Bdiff = (lc.b - rc.b).absoluteValue
            val Adiff = (lc.a - rc.a).absoluteValue
            pixelTotalDistance += Rdiff + Gdiff + Bdiff + Adiff
            pixelMaxDistance = maxOf(pixelMaxDistance, Rdiff)
            pixelMaxDistance = maxOf(pixelMaxDistance, Gdiff)
            pixelMaxDistance = maxOf(pixelMaxDistance, Bdiff)
            pixelMaxDistance = maxOf(pixelMaxDistance, Adiff)
            if (lc != rc) {
                pixelDiffCount++
            }
        }
        val psnr = Bitmap32.computePsnr(L.toBMP32(), R.toBMP32())

        return CompareResult(pixelDiffCount, pixelTotalDistance, pixelMaxDistance, psnr)
    }
}

private fun showBitmapDiffDialog(referenceBitmap: Bitmap32, actualBitmap: Bitmap32, title: String): Boolean {
    var doAccept: Boolean? = null
    val diff = Bitmap32.diffEx(actualBitmap, referenceBitmap)
    val frame = JFrame()
    lateinit var accept: JButton
    lateinit var discard: JButton

    frame.add(JLabel(title.replace("\n", "<br/>")), BorderLayout.PAGE_START)
    frame.add(JLabel(ImageIcon(referenceBitmap.scaleNearest(2, 2).toAwt())), BorderLayout.LINE_START)
    frame.add(JLabel(ImageIcon(diff.scaleNearest(2, 2).toAwt())), BorderLayout.CENTER)
    frame.add(JLabel(ImageIcon(actualBitmap.scaleNearest(2, 2).toAwt())), BorderLayout.LINE_END)
    frame.add(JPanel().also {
        it.add(JButton("Accept & Mark as valid").also { accept = it }.also { it.addActionListener { doAccept = true; frame.isVisible = false } })
        it.add(JButton("Discard & Error").also { discard = it }.also { it.addActionListener { doAccept = false; frame.isVisible = false } })
    }, BorderLayout.PAGE_END)
    frame.pack()
    frame.rootPane.defaultButton = discard
    frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
    frame.setLocationRelativeTo(null)
    //frame.isAlwaysOnTop = true
    frame.isVisible = true
    frame.toFront()
    frame.repaint()
    while (frame.isVisible) Thread.sleep(100L)
    return doAccept ?: false
}

private var OffscreenStage.testIndex: Int by extraProperty { 0 }

private fun Bitmap.toBitmap8Or32(): Bitmap = this.toBMP32().tryToExactBitmap8() ?: this
//private fun Bitmap.toBitmap8Or32(): Bitmap = this.toBMP32()

suspend fun OffscreenStage.simulateContextLost() {
    //println("----simulateContextLost")
    (views.ag as? AGOpengl?)?.let {
        val fboWidth = ag.mainFrameBuffer.width
        val fboHeight = ag.mainFrameBuffer.height
        it.context = createKmlGlContext(fboWidth, fboHeight)
        it.contextsToFree += it.context
        it.context?.set()
        ag.mainFrameBuffer.setSize(fboWidth, fboHeight)
        it.contextLost()
    }
}

fun OffscreenStage.simulateRenderFrame(
    view: View = this,
    posterize: Int = 0,
    includeBackground: Boolean = true,
    useTexture: Boolean = true,
): Bitmap32 = views.simulateRenderFrame(view, posterize, includeBackground, useTexture)

@OptIn(KorgeExperimental::class)
fun Views.simulateRenderFrame(
    view: View,
    posterize: Int = 0,
    includeBackground: Boolean = true,
    useTexture: Boolean = true,
): Bitmap32 {
    return views.ag.startEndFrame {
        //val currentFrameBuffer = views.renderContext.currentFrameBuffer
        //Bitmap32(currentFrameBuffer.width, currentFrameBuffer.height).also { ag.readColor(currentFrameBuffer, it) }
        views.renderContext.beforeRender()
        try {
            view.unsafeRenderToBitmapSync(
                views.renderContext,
                bgcolor = if (includeBackground) views.clearColor else Colors.TRANSPARENT,
                useTexture = useTexture
            ).depremultiplied().posterizeInplace(posterize)
        } finally {
            views.renderContext.afterRender()
        }
    }
}

suspend fun OffscreenStage.assertScreenshot(
    view: View = this,
    name: String = "$testIndex",
    psnr: Double = 40.0,
    //scale: Double = 1.0,
    posterize: Int = 0,
    includeBackground: Boolean = true,
    useTexture: Boolean = true,
) {
    testIndex++
    val updateTestRef = Environment["UPDATE_TEST_REF"] == "true"
    val interactive = Environment["INTERACTIVE_SCREENSHOT"] == "true"
    val context = injector.getOrNull<OffscreenContext>() ?: OffscreenContext()
    val outFile = File("testscreenshots/${context.testClassName.replace(".", "/")}/${context.testMethodName}_$name.png")
    val actualBitmap = simulateRenderFrame(view, posterize, includeBackground, useTexture)

    var updateReference = updateTestRef
    if (outFile.exists()) {
        //val expectedBitmap = runBlockingNoJs { outFile.toVfs().readNativeImage(ImageDecodingProps.DEFAULT_STRAIGHT).toBMP32() }
        val expectedBitmap =
            runBlocking { PNG.decode(outFile.readBytes(), ImageDecodingProps.DEFAULT_STRAIGHT).toBMP32() }
        //val ref = referenceBitmap.scaleLinear(scale, scale)
        //val act = actualBitmap.scaleLinear(scale)
        val result = BitmapComparer.compare(expectedBitmap, actualBitmap)
        if (!updateReference) {
            val similar = result.psnr >= psnr
            if (similar) {
                updateReference = false
            }
            if (!similar && interactive) {
                updateReference = showBitmapDiffDialog(
                    expectedBitmap,
                    actualBitmap,
                    "Bitmaps are not equal $expectedBitmap-$actualBitmap\n$result\n${result.error}"
                )
            }
            if (!updateReference) {
                val baseName = "${context.testClassName}_${context.testMethodName}_$name"
                val base = File("build/reports/screenshotTest").also { it.mkdirs() }
                val expectedFile = File(base, "$baseName.expt.png")
                val actualFile = File(base, "$baseName.actual.png")
                val diffFile = File(base, "$baseName.diff.png")
                if (!similar) {
                    expectedFile.writeBytes(
                        PNG.encode(
                            expectedBitmap.toBitmap8Or32(),
                            ImageEncodingProps(quality = 1.0)
                        )
                    )
                    actualFile.writeBytes(PNG.encode(actualBitmap.toBitmap8Or32(), ImageEncodingProps(quality = 1.0)))
                    kotlin.runCatching {
                        diffFile.writeBytes(
                            PNG.encode(
                                Bitmap32.diffEx(actualBitmap, expectedBitmap),
                                ImageEncodingProps(quality = 1.0)
                            )
                        )
                    }
                }
                assert(similar) {
                    "Bitmaps are not equal $expectedBitmap-$actualBitmap : $result.\n" +
                        "${result.error}\n" +
                        "Run ./gradlew jvmTestFix to update goldens\n" +
                        "Or set INTERACTIVE_SCREENSHOT=true\n" +
                        "\n" +
                        "Generated: file://${actualFile.absoluteFile}\n" +
                        "Diff: file://${diffFile.absoluteFile}\n" +
                        "Expected Directory: file://${outFile.parentFile.absoluteFile}\n" +
                        "Expected File: file://${outFile.absoluteFile}"
                }
            }
        }
    }
    if (updateReference || !outFile.exists()) {
        outFile.parentFile.mkdirs()
        val bytes = PNG.encode(actualBitmap.toBitmap8Or32(), ImageEncodingProps(quality = 1.0))
        if (!bytes.contentEquals(outFile.takeIf { it.exists() }?.readBytes())) {
            outFile.writeBytes(bytes)
            println("Folder ${outFile.parentFile.absoluteFile}")
            println("Updated ${outFile.absoluteFile}")
        }
    }
}
