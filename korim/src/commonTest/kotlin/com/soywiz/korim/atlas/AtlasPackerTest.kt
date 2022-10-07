package com.soywiz.korim.atlas

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korim.bitmap.slice
import com.soywiz.korim.bitmap.trimmed
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.showImageAndWait
import com.soywiz.korio.async.suspendTest
import com.soywiz.korma.geom.RectangleInt
import kotlin.test.Test

class AtlasPackerTest {
    @Test
    fun test() = suspendTest {
        val bitmaps: List<BmpSlice> = listOf(
            Bitmap32(64, 64, Colors.TRANSPARENT_BLACK).also { it.fill(Colors.RED, 22, 23, 24, 25) }.slice(name = "bmp1"),
            Bitmap32(64, 64, Colors.TRANSPARENT_BLACK).also { it.fill(Colors.BLUE, 22, 23, 24, 25) }.slice(name = "bmp2"),
        )
        //println(bitmaps[0])
        //println(bitmaps[0].findNonTransparentBounds())
        println(bitmaps[0].trimmed())
        //println("-----")
        //return@suspendTest
        //println(bitmaps[1])
        //println(bitmaps[1].findNonTransparentBounds())
        println(bitmaps[1].trimmed())
        //return@suspendTest
        val packer = AtlasPacker.pack(bitmaps.map { it.trimmed() }, borderSize = 8)
        //val packer = AtlasPacker.pack(bitmaps.map { it.trimmed() }, borderSize = 2)
        //val packer = AtlasPacker.pack(bitmaps.map { it.trimmed() }, borderSize = 0)
        //val packer = AtlasPacker.pack(bitmaps, borderSize = 8)
        val atlas = packer.atlases.first()
        println(atlas.packedItems.map { it.slice })
        println(atlas.atlas.info.frames.map { it.frame })
        println(atlas.atlasInfo.toJsonString())
        atlas.atlas.texture.showImageAndWait()
    }

    /*
    fun expandBitmapBorder(out: Bitmap32, rect: RectangleInt, borderSize: Int) {
        val borderSize2 = borderSize * 2
        val x0 = rect.left
        val y0 = rect.top
        val width = rect.width
        val height = rect.height
        val x1 = x0 + width - 1
        val y1 = y0 + height - 1
        for (i in 1..borderSize) {
            Bitmap32.copyRect(out, x0, y0, out, x0 - i, y0, 1, height)
            Bitmap32.copyRect(out, x1, y0, out, x1 + i, y0, 1, height)
        }
        for (i in 1..borderSize) {
            Bitmap32.copyRect(out, x0 - borderSize, y0, out, x0 - borderSize, y0 - i, width + borderSize2, 1)
            Bitmap32.copyRect(out, x0 - borderSize, y1, out, x0 - borderSize, y1 + i, width + borderSize2, 1)
        }
    }
    */
}
