package com.soywiz.korim.format

import com.soywiz.klock.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class QOITest {
    val formats = ImageFormats(PNG, QOI)

    @Test
    fun qoiTest() = suspendTestNoBrowser {
        repeat(4) { resourcesVfs["testcard_rgba.png"].readBitmapOptimized() }
        repeat(4) { resourcesVfs["testcard_rgba.png"].readBitmapNoNative(formats) }
        repeat(4) { resourcesVfs["testcard_rgba.qoi"].readBitmapNoNative(formats) }

        val pngBytes = resourcesVfs["dice.png"].readBytes()
        val qoiBytes = resourcesVfs["dice.qoi"].readBytes()

        val (expectedNative, expectedNativeTime) = measureTimeWithResult { nativeImageFormatProvider.decode(pngBytes) }
        val (expected, expectedTime) = measureTimeWithResult { PNG.decode(pngBytes) }
        val (output, outputTime) = measureTimeWithResult { QOI.decode(qoiBytes) }

        //QOI=4.280875ms, PNG=37.361000000000004ms, PNG_native=24.31941600036621ms
        //println("QOI=$outputTime, PNG=$expectedTime, PNG_native=$expectedNativeTime")
        //AtlasPacker.pack(listOf(output.slice(), expected.slice())).atlases.first().tex.showImageAndWait()

        assertEquals(0, output.matchContentsDistinctCount(expected))

        for (imageName in listOf("dice.qoi", "testcard_rgba.qoi", "kodim23.qoi")) {
            val original = QOI.decode(resourcesVfs[imageName])
            val reencoded = QOI.decode(QOI.encode(original))
            assertEquals(0, reencoded.matchContentsDistinctCount(original))
        }
    }
}
