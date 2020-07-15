package com.soywiz.korim.color

import kotlin.test.*

class PaletteColorFormatTest {
    @Test
    fun test() {
        val fmt = PaletteColorFormat(RgbaArray(arrayOf(Colors.RED, Colors.GREEN, Colors.BLUE)))
        assertEquals(Colors.RED, fmt.toRGBA(0))
        assertEquals(Colors.GREEN, fmt.toRGBA(1))
        assertEquals(Colors.BLUE, fmt.toRGBA(2))
    }
}
