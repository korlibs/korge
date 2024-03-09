package korlibs.image.bitmap

import korlibs.memory.*
import korlibs.image.color.*
import korlibs.io.async.*
import korlibs.platform.*
import kotlin.test.*

class Context2DCommonTest {
    @Test
    fun testFillAlpha() = suspendTest({ !Platform.isAndroid }) {
        val semiTransparentAlpha = Colors.FUCHSIA.withAd(0.5)
        val image = NativeImage(10, 10).context2d {
            fill(semiTransparentAlpha) {
                rect(-1, -1, 11, 11)
            }
        }.toBMP32().depremultipliedIfRequired()
        assertEquals(semiTransparentAlpha, image[5, 5])
    }

    @Test
    fun testFillAlpha2() = suspendTest({ !Platform.isAndroid }) {
        //val image = NativeImage(3, 3).context2d {
        val image = Bitmap32(3, 3, premultiplied = true).context2d {
            fill(Colors.WHITE) {
                rect(1, 1, 1, 1)
            }
        }.toBMP32().depremultipliedIfRequired()
        assertEquals(
            listOf(
                Colors.TRANSPARENT, Colors.TRANSPARENT, Colors.TRANSPARENT,
                Colors.TRANSPARENT, Colors.WHITE, Colors.TRANSPARENT,
                Colors.TRANSPARENT, Colors.TRANSPARENT, Colors.TRANSPARENT
            ), RgbaArray(image.ints).toList())
    }

    //@Test
    //fun testSweepGradient() = suspendTest {
    //    try {
    //        val bmp = Bitmap32Context2d(300, 300) {
    //            fill(createSweepGradient(100, 100).add(Colors.RED, Colors.GREEN, Colors.BLUE)) {
    //                //fill(createPattern(bmp, transform = MMatrix().translate(100, 100))) {
    //                //this.rect(100, 100, 200, 200)
    //                this.rect(0, 0, 200, 200)
    //            }
    //        }
    //        //bmp.showImageAndWait()
    //    } catch (e: Throwable) {
    //        e.printStackTrace()
    //        throw e
    //    }
    //}
}
