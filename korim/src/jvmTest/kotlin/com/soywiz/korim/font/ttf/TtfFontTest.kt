package com.soywiz.korim.font.ttf

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.stream.*
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
            DefaultTtfFont.getGlyphByChar(':')!!.path!!.toSvgString()
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
