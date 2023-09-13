package korlibs.image.bitmap

import korlibs.image.atlas.AtlasPacker
import korlibs.image.format.PNG
import korlibs.image.format.readBitmap
import korlibs.io.async.suspendTestNoBrowser
import korlibs.io.file.std.resourcesVfs
import korlibs.math.geom.*
import kotlin.test.Test
import kotlin.test.assertEquals

class NinePatchBitmap32Test {
    @Test
    fun testAtlasFromBmpSlice() = suspendTestNoBrowser {
        val bmp = resourcesVfs["bubble-chat.9.png"].readBitmap(PNG)
        val atlas = AtlasPacker.pack(listOf(bmp.slice(name = "bmp1"), bmp.slice(name = "bmp2")))
        val bmp1 = atlas["bmp1"]
        val bmp2 = atlas["bmp2"]
        val ninePatch1 = bmp1.asNinePatch()
        val ninePatch2 = bmp2.asNinePatch()

        for (ninePatch in listOf(ninePatch1, ninePatch2)) {
            assertEquals(
                listOf(false to 0..89, true to 90..156, false to 157..199),
                ninePatch.info.xsegments.map { it.scaled to it.range }
            )
            assertEquals(
                listOf(false to 0..55, true to 56..107, false to 108..199),
                ninePatch1.info.ysegments.map { it.scaled to it.range }
            )
        }
    }

    @Test
    fun name() = suspendTestNoBrowser {
        val ninePatch = resourcesVfs["bubble-chat.9.png"].readNinePatch(PNG)

        assertEquals(
            listOf(false to 0..89, true to 90..156, false to 157..199),
            ninePatch.info.xsegments.map { it.scaled to it.range }
        )
        assertEquals(
            listOf(false to 0..55, true to 56..107, false to 108..199),
            ninePatch.info.ysegments.map { it.scaled to it.range }
        )
        assertEquals(202, ninePatch.bmpSlice.bmp.width)
        assertEquals(202, ninePatch.bmpSlice.bmp.height)

        assertEquals(200, ninePatch.info.width)
        assertEquals(200, ninePatch.info.height)

        assertEquals(133, ninePatch.info.fixedWidth)
        assertEquals(148, ninePatch.info.fixedHeight)


        fun genComputeScale(new: Boolean): String {
            return arrayListOf<String>().apply {
                val log = this
                for (rect in listOf(
                    RectangleInt(0, 0, 512, 256),
                    RectangleInt(0, 0, 256, 512),
                    RectangleInt(0, 0, 100, 100),
                    RectangleInt(0, 0, 0, 0)
                )) {
                    log += "$rect:"
                    ninePatch.info.computeScale(rect, new = new) { seg, x, y, width, height ->
                        log += " - ${seg.rect}:$x,$y,$width,$height"
                    }
                }
            }.joinToString("\n")
        }

        assertEquals(
            """
                Rectangle(x=0, y=0, width=512, height=256):
                 - Rectangle(x=0, y=0, width=90, height=56):0,0,90,56
                 - Rectangle(x=90, y=0, width=67, height=56):90,0,379,56
                 - Rectangle(x=157, y=0, width=43, height=56):469,0,43,56
                 - Rectangle(x=0, y=56, width=90, height=52):0,56,90,108
                 - Rectangle(x=90, y=56, width=67, height=52):90,56,379,108
                 - Rectangle(x=157, y=56, width=43, height=52):469,56,43,108
                 - Rectangle(x=0, y=108, width=90, height=92):0,164,90,92
                 - Rectangle(x=90, y=108, width=67, height=92):90,164,379,92
                 - Rectangle(x=157, y=108, width=43, height=92):469,164,43,92
                Rectangle(x=0, y=0, width=256, height=512):
                 - Rectangle(x=0, y=0, width=90, height=56):0,0,90,56
                 - Rectangle(x=90, y=0, width=67, height=56):90,0,123,56
                 - Rectangle(x=157, y=0, width=43, height=56):213,0,43,56
                 - Rectangle(x=0, y=56, width=90, height=52):0,56,90,364
                 - Rectangle(x=90, y=56, width=67, height=52):90,56,123,364
                 - Rectangle(x=157, y=56, width=43, height=52):213,56,43,364
                 - Rectangle(x=0, y=108, width=90, height=92):0,420,90,92
                 - Rectangle(x=90, y=108, width=67, height=92):90,420,123,92
                 - Rectangle(x=157, y=108, width=43, height=92):213,420,43,92
                Rectangle(x=0, y=0, width=100, height=100):
                 - Rectangle(x=0, y=0, width=90, height=56):0,0,45,28
                 - Rectangle(x=90, y=0, width=67, height=56):45,0,34,28
                 - Rectangle(x=157, y=0, width=43, height=56):79,0,21,28
                 - Rectangle(x=0, y=56, width=90, height=52):0,28,45,26
                 - Rectangle(x=90, y=56, width=67, height=52):45,28,34,26
                 - Rectangle(x=157, y=56, width=43, height=52):79,28,21,26
                 - Rectangle(x=0, y=108, width=90, height=92):0,54,45,46
                 - Rectangle(x=90, y=108, width=67, height=92):45,54,34,46
                 - Rectangle(x=157, y=108, width=43, height=92):79,54,21,46
                Rectangle(x=0, y=0, width=0, height=0):
                 - Rectangle(x=0, y=0, width=90, height=56):0,0,0,0
                 - Rectangle(x=90, y=0, width=67, height=56):0,0,0,0
                 - Rectangle(x=157, y=0, width=43, height=56):0,0,0,0
                 - Rectangle(x=0, y=56, width=90, height=52):0,0,0,0
                 - Rectangle(x=90, y=56, width=67, height=52):0,0,0,0
                 - Rectangle(x=157, y=56, width=43, height=52):0,0,0,0
                 - Rectangle(x=0, y=108, width=90, height=92):0,0,0,0
                 - Rectangle(x=90, y=108, width=67, height=92):0,0,0,0
                 - Rectangle(x=157, y=108, width=43, height=92):0,0,0,0
			""".trimIndent(),
            genComputeScale(new = true)
        )

        assertEquals(
            """
                Rectangle(x=0, y=0, width=512, height=256):
                 - Rectangle(x=0, y=0, width=90, height=56):0,0,90,56
                 - Rectangle(x=90, y=0, width=67, height=56):90,0,379,56
                 - Rectangle(x=157, y=0, width=43, height=56):469,0,43,56
                 - Rectangle(x=0, y=56, width=90, height=52):0,56,90,108
                 - Rectangle(x=90, y=56, width=67, height=52):90,56,379,108
                 - Rectangle(x=157, y=56, width=43, height=52):469,56,43,108
                 - Rectangle(x=0, y=108, width=90, height=92):0,164,90,92
                 - Rectangle(x=90, y=108, width=67, height=92):90,164,379,92
                 - Rectangle(x=157, y=108, width=43, height=92):469,164,43,92
                Rectangle(x=0, y=0, width=256, height=512):
                 - Rectangle(x=0, y=0, width=90, height=56):0,0,90,56
                 - Rectangle(x=90, y=0, width=67, height=56):90,0,123,56
                 - Rectangle(x=157, y=0, width=43, height=56):213,0,43,56
                 - Rectangle(x=0, y=56, width=90, height=52):0,56,90,364
                 - Rectangle(x=90, y=56, width=67, height=52):90,56,123,364
                 - Rectangle(x=157, y=56, width=43, height=52):213,56,43,364
                 - Rectangle(x=0, y=108, width=90, height=92):0,420,90,92
                 - Rectangle(x=90, y=108, width=67, height=92):90,420,123,92
                 - Rectangle(x=157, y=108, width=43, height=92):213,420,43,92
                Rectangle(x=0, y=0, width=100, height=100):
                 - Rectangle(x=0, y=0, width=90, height=56):0,0,45,28
                 - Rectangle(x=90, y=0, width=67, height=56):45,0,33,28
                 - Rectangle(x=157, y=0, width=43, height=56):78,0,21,28
                 - Rectangle(x=0, y=56, width=90, height=52):0,28,45,26
                 - Rectangle(x=90, y=56, width=67, height=52):45,28,33,26
                 - Rectangle(x=157, y=56, width=43, height=52):78,28,21,26
                 - Rectangle(x=0, y=108, width=90, height=92):0,54,45,46
                 - Rectangle(x=90, y=108, width=67, height=92):45,54,33,46
                 - Rectangle(x=157, y=108, width=43, height=92):78,54,21,46
                Rectangle(x=0, y=0, width=0, height=0):
                 - Rectangle(x=0, y=0, width=90, height=56):0,0,0,0
                 - Rectangle(x=90, y=0, width=67, height=56):0,0,0,0
                 - Rectangle(x=157, y=0, width=43, height=56):0,0,0,0
                 - Rectangle(x=0, y=56, width=90, height=52):0,0,0,0
                 - Rectangle(x=90, y=56, width=67, height=52):0,0,0,0
                 - Rectangle(x=157, y=56, width=43, height=52):0,0,0,0
                 - Rectangle(x=0, y=108, width=90, height=92):0,0,0,0
                 - Rectangle(x=90, y=108, width=67, height=92):0,0,0,0
                 - Rectangle(x=157, y=108, width=43, height=92):0,0,0,0
			""".trimIndent(),
            genComputeScale(new = false)
        )

        //val bmp = NativeImage(512, 256)
        //val bmp = NativeImage(202, 202)

        //for (segment in ninePatch.segments.flatMap { it }) showImageAndWait(segment.bmp)

        //ninePatch.drawTo(bmp, RectangleInt.fromBounds(0, 0, 202, 202))
        //ninePatch.drawTo(bmp, RectangleInt.fromBounds(0, 0, 512, 202))
        //ninePatch.drawTo(bmp, RectangleInt.fromBounds(0, 0, 32, 202))

        //showImageAndWait(ninePatch.rendered(800, 800))
        //showImageAndWait(ninePatch.rendered(512, 256))
        //showImageAndWait(ninePatch.rendered(256, 512))
        //showImageAndWait(ninePatch.rendered(100, 100))
        //showImageAndWait(ninePatch.rendered(32, 100))
        //bmp.writeTo("/tmp/file.tga".uniVfs, defaultImageFormats)
    }
}
