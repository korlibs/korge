package com.soywiz.korim.font

import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.format.showImageAndWait
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import kotlin.test.Test
import kotlin.test.assertEquals

class WoffTest {
    @Test
    fun test() = suspendTest {
        val font = resourcesVfs["font.woff"].readWoffFont()
        assertEquals(
            listOf("OS/2", "PCLT", "cmap", "cvt ", "fpgm", "glyf", "hdmx", "head", "hhea", "hmtx", "kern", "loca", "maxp", "name", "post", "prep"),
            font.getTableNames().toList()
        )
        //NativeImage(500, 500).context2d { drawText("HELLO WORLD", 100.0, 100.0, font = font, size = 64.0, fill = true) }.showImageAndWait()
    }
}
