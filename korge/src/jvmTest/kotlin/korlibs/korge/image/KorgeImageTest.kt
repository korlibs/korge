package korlibs.korge.image

import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.io.file.std.*
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
}