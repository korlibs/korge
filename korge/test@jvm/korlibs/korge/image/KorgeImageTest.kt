package korlibs.korge.image

import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.korge.Korge
import korlibs.korge.testing.korgeScreenshotTestV2
import korlibs.korge.view.image
import korlibs.math.geom.slice.*
import kotlin.test.*

class KorgeImageTest {
    @Test
    fun test() = suspendTest {
        val imageInfo = resourcesVfs["Exif5-2x.png"].readImageInfo(PNG)
        assertEquals(SliceOrientation.MIRROR_HORIZONTAL_ROTATE_270, imageInfo?.orientationSure)
        imageInfo?.orientation = SliceOrientation.MIRROR_HORIZONTAL_ROTATE_0
        assertEquals(SliceOrientation(rotation = SliceRotation.R0, flipX = true), imageInfo?.orientationSure)
    }

    @Test
    fun renderGrayscaleJpegImage() = korgeScreenshotTestV2(Korge()) {
        val img = image(resourcesVfs["img1_grayscale.jpg"].readBitmapNative())
        it.recordGolden(img, "grayscale_jpg")
        it.endTest()
    }
}
