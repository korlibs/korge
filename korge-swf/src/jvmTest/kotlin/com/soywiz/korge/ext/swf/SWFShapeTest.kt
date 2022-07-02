package com.soywiz.korge.ext.swf

import com.soywiz.korge.animate.*
import com.soywiz.korge.tests.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class SWFShapeTest : ViewsForTesting() {
    @Test
    fun test() = suspendTest {
        //val swf = resourcesVfs["swf/main.swf"].readSWF(views, defaultConfig = SWFExportConfig(rasterizerMethod = ShapeRasterizerMethod.NONE))
        //val swf = resourcesVfs["swf/main.swf"].readSWF(views, defaultConfig = SWFExportConfig(rasterizerMethod = ShapeRasterizerMethod.X1))
        for (method in listOf(ShapeRasterizerMethod.NONE, ShapeRasterizerMethod.X1, ShapeRasterizerMethod.X4)) {
            val swf = resourcesVfs["swf/main.swf"].readSWF(
                AnLibrary.Context(views),
                defaultConfig = SWFExportConfig(rasterizerMethod = method, generateTextures = true)
            )
            val img = (swf.symbolsById[10] as AnSymbolShape).textureWithBitmap!!.bitmapSlice.extract().toBMP32()
            assertEquals(Colors.BLUE, img[58, 10])
            assertEquals(Colors.TRANSPARENT_BLACK, img[58, 20])
        }
    }
}
