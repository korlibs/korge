package com.soywiz.korim.font.ttf

import com.soywiz.korim.font.*
import com.soywiz.korim.vector.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import kotlin.test.*

class TtfFontTest {
    lateinit var root: VfsFile

    fun ttfTest(callback: suspend () -> Unit) = suspendTest {
        for (path in listOf(applicationVfs["src/test/resources"], resourcesVfs)) {
            root = path
            if (root["kotlin8.png"].exists()) break
        }
        callback()
    }

    @Test
    fun testColon() {
        assertEquals(
            "M300,0 L100,0 L100,-200 L300,-200 L300,0 Z M300,-800 L100,-800 L100,-1000 L300,-1000 L300,-800 Z",
            DefaultTtfFont.getGlyphByChar(':')!!.path.toSvgString()
        )
    }

    @Test
    fun testColorFont() = suspendTest {
        println(SystemFont.getEmojiFont().name)
        val smileyGlyph = SystemFont.getEmojiFont().ttf[WString("😀")[0]]
        //Colors["#ffc83dff"]
        //println("smileyGlyph=${smileyGlyph?.codePoint},$smileyGlyph")
        //println(smileyGlyph?.colorEntry)
        //for (path in smileyGlyph!!.paths) println("path = $path")
    }

    @Test
    fun testScaleOrder() = suspendTest {
        assertEquals(
            "M675 -800L475 -800L475 -1000L675 -1000L675 -800ZM200 0Q200 104, 288 177Q376 250, 500 250Q666 250, 783 177Q814 158, 836 136L940 240Q917 262, 889 283Q728 400, 500 400Q314 400, 182 283Q50 166, 50 0Q50 -230, 275 -340Q500 -450, 500 -600L650 -600Q650 -370, 425 -260Q200 -150, 200 0Z",
            DefaultTtfFont.getGlyphPath(16.0, '¿'.code)!!.path.toSvgPathString()
        )
    }

    //@Test
    //fun name() = ttfTest {
    //    val font = TtfFont(root["Comfortaa-Regular.ttf"].readAll().openSync())
    //    NativeImage(512, 128).apply {
    //        getContext2d()
    //            .fillText(
    //                "HELLO WORLD. This 0123 ñáéíóúç",
    //                font = font,
    //                size = 32.0,
    //                x = 0.0,
    //                y = 0.0,
    //                color = Colors.RED,
    //                origin = TtfFont.Origin.TOP
    //            )
    //    }.showImageAndWait()
    //}
}
