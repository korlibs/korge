package korlibs.image.format

import korlibs.time.measureTimeWithResult
import korlibs.memory.UByteArrayInt
import korlibs.image.bitmap.Bitmap32
import korlibs.image.bitmap.matchContentsDistinctCount
import korlibs.io.async.suspendTestNoBrowser
import korlibs.io.file.std.resourcesVfs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class QOITest {
    val formats = ImageFormats(PNG, QOI)

    @Test
    fun qoiTest() = suspendTestNoBrowser {
        RegisteredImageFormats.register(PNG) // Required for readBitmapOptimized

        val pngBytes = resourcesVfs["dice.png"].readBytes()
        val qoiBytes = resourcesVfs["dice.qoi"].readBytes()

        //val (expectedNative, expectedNativeTime) = measureTimeWithResult { nativeImageFormatProvider.decode(pngBytes) }
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

    @Test
    fun qoiTestWithPreAllocatedArray() = suspendTestNoBrowser {
        RegisteredImageFormats.register(PNG) // Required for readBitmapOptimized

        val preallocatedArray = UByteArrayInt(QOI.calculateMaxSize(1000, 1000))

        repeat(4) { resourcesVfs["testcard_rgba.png"].readBitmap() }
        repeat(4) { resourcesVfs["testcard_rgba.png"].readBitmapNoNative(formats) }
        repeat(4) { resourcesVfs["testcard_rgba.qoi"].readBitmapNoNative(formats) }

        val pngBytes = resourcesVfs["dice.png"].readBytes()
        val qoiBytes = resourcesVfs["dice.qoi"].readBytes()

        val (expectedNative, expectedNativeTime) = measureTimeWithResult {
            nativeImageFormatProvider.decode(
                pngBytes
            )
        }
        val (expected, expectedTime) = measureTimeWithResult { PNG.decode(pngBytes) }
        val (output, outputTime) = measureTimeWithResult { QOI.decode(qoiBytes) }

        //QOI=2.6177000122070315ms, PNG=42.07829998779297ms, PNG_native=25.59229998779297ms
//        println("QOI=$outputTime, PNG=$expectedTime, PNG_native=$expectedNativeTime")
        //AtlasPacker.pack(listOf(output.slice(), expected.slice())).atlases.first().tex.showImageAndWait()

        assertEquals(0, output.matchContentsDistinctCount(expected))
        val props = ImageEncodingProps {
            preAllocatedArrayForQOI = preallocatedArray
        }

        for (imageName in listOf("dice.qoi", "testcard_rgba.qoi", "kodim23.qoi")) {
            val original = QOI.decode(resourcesVfs[imageName])
            val reencoded = QOI.decode(
                QOI.encode(
                    original,
                    props
                )
            )
            assertEquals(0, reencoded.matchContentsDistinctCount(original))
        }
    }

    @Test
    fun qoiToOutputBitmapTest() = suspendTestNoBrowser {
        RegisteredImageFormats.register(PNG) // Required for readBitmapOptimized

        repeat(4) { resourcesVfs["testcard_rgba.png"].readBitmap() }
        repeat(4) { resourcesVfs["testcard_rgba.png"].readBitmapNoNative(formats) }
        repeat(4) { resourcesVfs["testcard_rgba.qoi"].readBitmapNoNative(formats) }

        val qoiOutBitmap = Bitmap32(800, 600, premultiplied = false)

        val pngBytes = resourcesVfs["dice.png"].readBytes()
        val qoiBytes = resourcesVfs["dice.qoi"].readBytes()

        val (expectedNative, expectedNativeTime) = measureTimeWithResult {
            nativeImageFormatProvider.decode(
                pngBytes
            )
        }
        val (expected, expectedTime) = measureTimeWithResult { PNG.decode(pngBytes) }
        val (output, outputTime) = measureTimeWithResult {
            QOI.decode(qoiBytes, ImageDecodingProps.DEFAULT.copy(out = qoiOutBitmap))
        }

        // QOI=1.3790999755859374ms, PNG=28.038300048828127ms, PNG_native=22.283200012207033ms
//        println("QOI=$outputTime, PNG=$expectedTime, PNG_native=$expectedNativeTime")
//        AtlasPacker.pack(listOf(output.slice(), expected.slice())).atlases.first().tex.showImageAndWait()

        assertEquals(0, output.matchContentsDistinctCount(expected))
        assertSame(qoiOutBitmap, output)
    }

    @Test
    fun qoiToOutputBitmapWidthMismatchNewBitmapReturnedInstead() = suspendTestNoBrowser {
        RegisteredImageFormats.register(PNG) // Required for readBitmapOptimized

        val qoiOutBitmap = Bitmap32(666, 600, premultiplied = false)

        val qoiBytes = resourcesVfs["dice.qoi"].readBytes()
        val (output, outputTime) = measureTimeWithResult {
            QOI.decode(qoiBytes, ImageDecodingProps.DEFAULT.copy(out = qoiOutBitmap))
        }

        assertNotSame(qoiOutBitmap, output)
    }

    @Test
    fun qoiToOutputBitmapHeightMismatchNewBitmapReturnedInstead() = suspendTestNoBrowser {
        RegisteredImageFormats.register(PNG) // Required for readBitmapOptimized

        val qoiOutBitmap = Bitmap32(800, 666, premultiplied = false)

        val qoiBytes = resourcesVfs["dice.qoi"].readBytes()
        val (output, outputTime) = measureTimeWithResult {
            QOI.decode(qoiBytes, ImageDecodingProps.DEFAULT.copy(out = qoiOutBitmap))
        }

        assertNotSame(qoiOutBitmap, output)
    }

    @Test
    fun qoiToOutputBitmapPremultipliedSettingSwitchedToFalse() = suspendTestNoBrowser {
        RegisteredImageFormats.register(PNG) // Required for readBitmapOptimized

        val qoiOutBitmap = Bitmap32(800, 600, premultiplied = true)

        val qoiBytes = resourcesVfs["dice.qoi"].readBytes()
        val (output, outputTime) = measureTimeWithResult {
            QOI.decode(qoiBytes, ImageDecodingProps.DEFAULT.copy(out = qoiOutBitmap))
        }
        assertSame(qoiOutBitmap, output)
        assertFalse(qoiOutBitmap.premultiplied)
    }

    @Test
    fun providingSmallPreAllocatedArrayResultsInError() = suspendTestNoBrowser {
        val original = QOI.decode(resourcesVfs["dice.qoi"])
        val preallocatedArray = UByteArrayInt(4)
        val props = ImageEncodingProps {
            preAllocatedArrayForQOI = preallocatedArray
        }
        assertFailsWith<IllegalArgumentException> {
            QOI.encode(
                original,
                props
            )
        }
    }
}
