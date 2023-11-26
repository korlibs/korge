package korlibs.io.lang

import kotlin.test.Test
import kotlin.test.assertEquals

class WStringTest {
    @Test
    fun smokeTest() {
        val string = WString("\uD83D\uDE00") // 😀 U+1F600 // "😀".codePointAt(0).toString(16)
        assertEquals(1, string.length)
        assertEquals(128512, string[0].codePoint)
        assertEquals(1, WString("😀").length)
        assertEquals(128512, WString("😀")[0].codePoint)
        assertEquals(WString("😀"), WString("😀").toString().toWString())
        assertEquals(WString("Ｇ"), WString("Ｇ").toString().toWString())
    }
}
