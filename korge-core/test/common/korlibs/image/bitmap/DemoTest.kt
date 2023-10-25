@file:Suppress("UnusedImport")

package korlibs.image.bitmap

import korlibs.datastructure.IntMap
import korlibs.datastructure.toIntMap
import korlibs.image.bitmap.Bitmap32
import korlibs.image.bitmap.slice
import korlibs.image.font.BitmapFont
import korlibs.image.format.PNG
import korlibs.image.format.readBitmap
import korlibs.io.async.suspendTest
import korlibs.io.async.suspendTestNoBrowser
import korlibs.io.file.std.MemoryVfs
import korlibs.io.stream.openAsync
import korlibs.math.geom.*
import korlibs.encoding.fromBase64
import kotlin.native.concurrent.ThreadLocal
import kotlin.test.Test
import kotlin.test.assertEquals

class DemoTest {
    object DebugBitmapFont {
        val DEBUG_FONT_BYTES: ByteArray = "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".fromBase64()
    }
    
    @Test
    fun test() = suspendTestNoBrowser {
        val bmp1 = PNG.decode(DebugBitmapFont.DEBUG_FONT_BYTES).toBMP32().premultipliedIfRequired()
        val bmp2 = MemoryVfs(mapOf("image.png" to DebugBitmapFont.DEBUG_FONT_BYTES.openAsync()))["image.png"].readBitmap(PNG).toBMP32().premultipliedIfRequired()
        assertEquals(bmp1.size, bmp2.size)
        val result = Bitmap32.matchesWithResult(bmp1, bmp2)
        assertEquals(Bitmap32.Companion.MatchResult(sizeMatches = true, differentPixels = 0, samePixels = bmp1.area), result)
    }

    @Test
    fun test2() = suspendTest {
        assertEquals(SizeInt(8, 8), debugBmpFont.glyphs.values.first()!!.bmp.size)
    }
}

object DebugBitmapFont {
    val DEBUG_FONT_BYTES: ByteArray = "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".fromBase64()
}

val debugBmpFont: BitmapFont by lazy {
    val tex = PNG.decode(DebugBitmapFont.DEBUG_FONT_BYTES).toBMP32().premultiplied().slice()
    val fntAdvance = 7.0
    val fntWidth = 8.0
    val fntHeight = 8.0

    val fntBlockX = 2.0
    val fntBlockY = 2.0
    val fntBlockWidth = 12.0
    val fntBlockHeight = 12.0

    BitmapFont(fntHeight, fntHeight, fntHeight, (0 until 256).associateWith {
        val x = it % 16
        val y = it / 16
        BitmapFont.Glyph(
            fntHeight,
            it,
            tex.sliceWithSize(
                (x * fntBlockWidth + fntBlockX).toInt(),
                (y * fntBlockHeight + fntBlockY).toInt(),
                fntWidth.toInt(), fntHeight.toInt()
            ),
            0, 0, fntAdvance.toInt()
        )
    }.toIntMap(), IntMap())
}
