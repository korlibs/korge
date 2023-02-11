package com.soywiz.korge.testing

import com.soywiz.korge.view.*
import com.soywiz.korim.awt.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import java.awt.*
import java.io.*
import javax.swing.*
import kotlin.math.*

object BitmapComparer {
    data class CompareResult(
        val pixelDiffCount: Int = 0,
        val pixelTotalDistance: Int = 0,
        val pixelMaxDistance: Int = 0,
        val psnr: Double = 0.0,
    ) {
        val strictEquals: Boolean get() = pixelDiffCount == 0
        val reasonablySimilar: Boolean get() = pixelMaxDistance <= 3 || psnr >= 45.0
    }

    fun compare(left: Bitmap, right: Bitmap): CompareResult {
        if (left.premultiplied != right.premultiplied) error("premultiplied left=${left.premultiplied}, right=${right.premultiplied}")
        if (left.width != right.width || left.height != right.height) error("dimensions left=${left.width}x${left.height}, right=${right.width}x${right.height}")
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

private fun showBitmapDiffDialog(referenceBitmap: Bitmap32, actualBitmap: Bitmap32, title: String): Boolean? {
    var doAccept: Boolean? = null
    val diff = Bitmap32.diff(actualBitmap, referenceBitmap)
    var maxR = 0
    var maxG = 0
    var maxB = 0

    diff.forEach { n, x, y ->
        val c = diff[x, y]
        maxR = max(maxR, c.r)
        maxG = max(maxG, c.g)
        maxB = max(maxB, c.b)
    }
    diff.forEach { n, x, y ->
        val c = diff[x, y]
        diff[x, y] = RGBA.float(
            if (maxR == 0) 0f else c.rf / maxR.toFloat(),
            if (maxG == 0) 0f else c.gf / maxG.toFloat(),
            if (maxB == 0) 0f else c.bf / maxB.toFloat(),
            1f
        )
    }
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
    return doAccept
}

suspend fun Stage.assertScreenshot(view: View, name: String, psnr: Double = 50.0, scale: Double = 1.0, posterize: Int = 0) {
    val updateTestRef = Environment["UPDATE_TEST_REF"] == "true"
    val context = injector.getOrNull<OffscreenContext>() ?: OffscreenContext()
    val outFile = File("testGoldens/${context.testClassName}/${context.testMethodName}_$name.png")
    val actualBitmap = view.renderToBitmap(views).depremultiplied().posterizeInplace(posterize)
    var doAccept: Boolean? = null
    var updateReference = true
    if (outFile.exists()) {
        val referenceBitmap = outFile.toVfs().readNativeImage(ImageDecodingProps.DEFAULT_STRAIGHT).toBMP32().posterizeInplace(posterize)
        val ref = referenceBitmap.scaleLinear(scale, scale)
        val act = actualBitmap.scaleLinear(scale)
        val result = BitmapComparer.compare(ref, act)
        if (!updateTestRef) {
            val similar = result.psnr >= psnr
            if (!similar) {
            //if (true) {
                if (Environment["INTERACTIVE_SCREENSHOT"] == "true") {
                //if (true) {
                    doAccept = showBitmapDiffDialog(referenceBitmap, actualBitmap, "Bitmaps are not equal $referenceBitmap-$actualBitmap\n$result")

                }
            }
            if (doAccept != null) {
                assert(similar) { "Bitmaps are not equal $ref-$act : $result.\nRun ./gradlew jvmTestFix to update goldens\nOr set INTERACTIVE_SCREENSHOT=true" }
            }

            if (doAccept == true) {
                updateReference = false
            }
        }
    }
    if (updateReference) {
        outFile.writeBytes(PNG.encode(actualBitmap.tryToExactBitmap8() ?: actualBitmap, ImageEncodingProps(quality = 1.0)))
    }
}
