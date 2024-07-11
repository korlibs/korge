package korlibs.korge.atlas

import korlibs.memory.*
import korlibs.image.atlas.Atlas
import korlibs.image.atlas.AtlasInfo
import korlibs.image.atlas.readAtlas
import korlibs.image.bitmap.*
import korlibs.io.async.suspendTest
import korlibs.io.file.std.resourcesVfs
import korlibs.math.geom.*
import korlibs.platform.*
import kotlin.test.Test
import kotlin.test.assertEquals

class AtlasInfoTest {
    @Test
    fun name() = suspendTest({ Platform.isJvm }) {
        val atlas = AtlasInfo.loadJsonSpriter(resourcesVfs["demo.json"].readString())
        assertEquals("Spriter", atlas.app)
        assertEquals("r10", atlas.version)
        assertEquals("demo.png", atlas.image)
        assertEquals("RGBA8888", atlas.format)
        assertEquals(124, atlas.frames.size)
        assertEquals(1.0, atlas.scale)
        assertEquals(SizeInt(1021, 1003), atlas.size)

        val firstFrame = atlas.frames.first()
        assertEquals("arms/forearm_jump_0.png", atlas.frames.map { it.name }.first())
        assertEquals(RectangleInt(993, 319, 28, 41), firstFrame.frame)
        assertEquals(SizeInt(55, 47), firstFrame.sourceSize)
        assertEquals(RectangleInt(8, 7, 28, 41), firstFrame.spriteSourceSize)
        assertEquals(true, firstFrame.rotated)
        assertEquals(true, firstFrame.trimmed)
    }

    @Test
    fun atlasTestXml() = suspendTest({ Platform.isJvm }) {
        testAtlas(resourcesVfs["atlas-test.xml"].readAtlas())
    }

    @Test
    fun atlasTestJson() = suspendTest({ Platform.isJvm }) {
        testAtlas(resourcesVfs["atlas-test.json"].readAtlas(), ".png")
    }

    @Test
    fun atlasTestTxt() = suspendTest({ Platform.isJvm }) {
        testAtlas(resourcesVfs["atlas-test-spine.atlas.txt"].readAtlas())
    }

    fun testAtlas(atlas: Atlas, ext:String = "") {
        // On the test textures, the edges are colorized with different intensity to test the transform
        testSlice(atlas["1$ext"], true, false, false, 0, 0) // red
        testSlice(atlas["2$ext"], false, true, false, 0, 0) // green
        testSlice(atlas["3$ext"], false, false, true, 10, 0) // blue
        testSlice(atlas["4$ext"], true, true, false, 10, 10)  // yellow
        testSlice(atlas["5$ext"], true, false, true, 0, 10)  // magenta
        testSlice(atlas["6$ext"], false, true, true, 5, 5)  // cyan

        testExtract(atlas["1$ext"], true, false, false, 0, 0) // red
        testExtract(atlas["2$ext"], false, true, false, 0, 0) // green
        testExtract(atlas["3$ext"], false, false, true, 10, 0) // blue
        testExtract(atlas["4$ext"], true, true, false, 10, 10)  // yellow
        testExtract(atlas["5$ext"], true, false, true, 0, 10)  // magenta
        testExtract(atlas["6$ext"], false, true, true, 5, 5)  // cyan
    }

    fun testSlice(slice: BmpSlice, r: Boolean, g: Boolean, b: Boolean, xOffset: Int, yOffset: Int) {
        //when (slice.name?.substr(0, 1)) {
        //    "1" -> assert(slice.virtFrame == null || slice.virtFrame == RectangleInt(0, 0, 32, 46), { "Slice ${slice.name} failed on virtFrame" })
        //    "2" -> assert(slice.virtFrame == RectangleInt(xOffset, yOffset, 42, 56), { "Slice ${slice.name} failed on virtFrame" })
        //    "3" -> assert(slice.virtFrame == RectangleInt(xOffset, yOffset, 42, 56), { "Slice ${slice.name} failed on virtFrame" })
        //    "4" -> assert(slice.virtFrame == RectangleInt(xOffset, yOffset, 42, 56), { "Slice ${slice.name} failed on virtFrame" })
        //    "5" -> assert(slice.virtFrame == RectangleInt(xOffset, yOffset, 42, 56), { "Slice ${slice.name} failed on virtFrame" })
        //    "6" -> assert(slice.virtFrame == RectangleInt(xOffset, yOffset, 42, 56), { "Slice ${slice.name} failed on virtFrame" })
        //}
        assertEquals(32, slice.width, message = "Slice ${slice.name} failed on width")
        assertEquals(46, slice.height, message = "Slice ${slice.name} failed on height")

        //for (i in 0 until 4) {
        //    val col = when(i) {
        //        1 -> slice.getRgba(xOffset + 31, yOffset + 0)
        //        2 -> slice.getRgba(xOffset + 31, yOffset + 45)
        //        3 -> slice.getRgba(xOffset + 0, yOffset + 45)
        //        else -> slice.getRgba(xOffset + 0, yOffset + 0)
        //    }
        //    val c = 250 - i * 50
        //    assert(col.r == if (r) c else 0, { "Slice ${slice.name} failed on pos $i at red" })
        //    assert(col.g == if (g) c else 0, { "Slice ${slice.name} failed on pos $i at green" })
        //    assert(col.b == if (b) c else 0, { "Slice ${slice.name} failed on pos $i at blue" })
        //}
    }

    fun testExtract(slice: BmpSlice, r: Boolean, g: Boolean, b: Boolean, xOffset: Int, yOffset: Int) {
        val bmp = slice.extract()

        if (slice.name?.startsWith("1") == true) {
            check(bmp.width == 32, { "Extracted slice ${slice.name} failed on width" })
            check(bmp.height == 46, { "Extracted slice ${slice.name} failed on height" })
        } else {
            check(bmp.width == 42, { "Extracted slice ${slice.name} failed on width" })
            check(bmp.height == 56, { "Extracted slice ${slice.name} failed on height" })
        }

        for (i in 0 until 4) {
            val col = when(i) {
                1 -> bmp.getRgba(xOffset + 31, yOffset + 0)
                2 -> bmp.getRgba(xOffset + 31, yOffset + 45)
                3 -> bmp.getRgba(xOffset + 0, yOffset + 45)
                else -> bmp.getRgba(xOffset + 0, yOffset + 0)
            }
            val c = 250 - i * 50
            check(col.r == if (r) c else 0, { "Extracted slice ${slice.name} failed on pos $i at red" })
            check(col.g == if (g) c else 0, { "Extracted slice ${slice.name} failed on pos $i at green" })
            check(col.b == if (b) c else 0, { "Extracted slice ${slice.name} failed on pos $i at blue" })
        }
    }
}
