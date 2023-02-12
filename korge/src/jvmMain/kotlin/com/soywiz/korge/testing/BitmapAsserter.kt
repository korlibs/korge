package com.soywiz.korge.testing

import com.soywiz.kds.*
import com.soywiz.korge.view.*
import com.soywiz.korim.awt.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import java.awt.*
import java.io.*
import java.nio.file.Files
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
        if (left.premultiplied != right.premultiplied) {
            return CompareResult(error = "premultiplied left=${left.premultiplied}, right=${right.premultiplied}")
        }
        if (left.width != right.width || left.height != right.height) {
            return CompareResult(
                error = "dimensions left=${left.width}x${left.height}, right=${right.width}x${right.height}"
            )
        }
        var pixelDiffCount = 0
        var pixelTotalDistance = 0
        var pixelMaxDistance = 0
        loop@ for (y in 0 until left.height) for (x in 0 until left.width) {
            val lc = left.getRgbaRaw(x, y)
            val rc = right.getRgbaRaw(x, y)
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
        val psnr = Bitmap32.computePsnr(left.toBMP32(), right.toBMP32())

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

suspend fun OffscreenStage.assertScreenshot(
    view: View = this,
    name: String = "$testIndex",
    psnr: Double = 40.0,
    //scale: Double = 1.0,
    posterize: Int = 0,
    includeBackground: Boolean = true
) {
    testIndex++
    val updateTestRef = Environment["UPDATE_TEST_REF"] == "true"
    val interactive = Environment["INTERACTIVE_SCREENSHOT"] == "true"
    val context = injector.getSyncOrNull<OffscreenContext>() ?: OffscreenContext()
    val outFile = File("testGoldens/${context.testClassName}/${context.testMethodName}_$name.png")
    val actualBitmap = views.ag.startEndFrame { view.unsafeRenderToBitmapSync(views.renderContext, bgcolor = if (includeBackground) views.clearColor else Colors.TRANSPARENT).depremultiplied().posterizeInplace(posterize) }
    var doAccept: Boolean? = false
    var updateReference = updateTestRef
    if (outFile.exists()) {
        val referenceBitmap = runBlockingNoJs { outFile.toVfs().readNativeImage(ImageDecodingProps.DEFAULT_STRAIGHT).toBMP32().posterizeInplace(posterize) }
        //val ref = referenceBitmap.scaleLinear(scale, scale)
        //val act = actualBitmap.scaleLinear(scale)
        val result = BitmapComparer.compare(referenceBitmap, actualBitmap)
        if (!updateTestRef) {
            val similar = result.psnr >= psnr
            if (!similar && interactive) {
                updateReference = showBitmapDiffDialog(referenceBitmap, actualBitmap, "Bitmaps are not equal $referenceBitmap-$actualBitmap\n$result\n${result.error}")
            }
            if (!updateReference) {
                val baseName = "${context.testClassName}_${context.testMethodName}_$name.png"
                val tempFile = File(Environment.tempPath, baseName)
                val tempDiffFile = File(Environment.tempPath, "diff_$baseName")
                if (!similar) {
                    tempFile.writeBytes(PNG.encode(actualBitmap.tryToExactBitmap8() ?: actualBitmap, ImageEncodingProps(quality = 1.0)))
                    tempDiffFile.writeBytes(PNG.encode(Bitmap32.diffEx(actualBitmap, referenceBitmap), ImageEncodingProps(quality = 1.0)))
                }
                assert(similar) {
                    "Bitmaps are not equal $referenceBitmap-$actualBitmap : $result.\n" +
                        "${result.error}\n" +
                        "Run ./gradlew jvmTestFix to update goldens\n" +
                        "Or set INTERACTIVE_SCREENSHOT=true\n" +
                        "\n" +
                        "Generated: ${tempFile.absoluteFile}\n" +
                        "Diff: ${tempDiffFile.absoluteFile}\n" +
                        "Expected Directory: ${outFile.parentFile.absoluteFile}\n" +
                        "Expected File: ${outFile.absoluteFile}"
                }
            }
        }
    }
    if (updateReference) {
        outFile.parentFile.mkdirs()
        outFile.writeBytes(PNG.encode(actualBitmap.tryToExactBitmap8() ?: actualBitmap, ImageEncodingProps(quality = 1.0)))
        println("Folder ${outFile.parentFile.absoluteFile}")
        println("Updated ${outFile.absoluteFile}")
    }
}
