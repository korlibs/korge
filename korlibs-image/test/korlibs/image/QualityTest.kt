package korlibs.image

import korlibs.io.util.*
import korlibs.number.*
import korlibs.platform.*
import kotlin.test.*

class QualityTest {
    @Test
    fun testLevels() {
        if (Platform.isWasm) {
            println("Skipping in wasm as Float.toString is not working as the rest of the targets")
            return
        }

        fun Quality.result(): String = "$this: ${level.niceStr(2, zeroSuffix = true)}: $isLow, $isMedium, $isHigh : ${this <= Quality.MEDIUM}, ${this >= Quality.MEDIUM}"

        assertEquals(
            """
                LOWEST: 0.0: true, false, false : true, false
                CUSTOM1: 0.1: true, false, false : true, false
                LOW: 0.25: true, false, false : true, false
                MEDIUM: 0.5: false, true, false : true, true
                HIGH: 0.75: false, false, true : false, true
                CUSTOM9: 0.9: false, false, true : false, true
                HIGHEST: 1.0: false, false, true : false, true
            """.trimIndent(),
            """
                ${Quality.LOWEST.result()}
                ${Quality(.1f, name = "CUSTOM1").result()}
                ${Quality.LOW.result()}
                ${Quality.MEDIUM.result()}
                ${Quality.HIGH.result()}
                ${Quality(.9f, name = "CUSTOM9").result()}
                ${Quality.HIGHEST.result()}
            """.trimIndent()
        )
    }

    @Test
    fun testLevelsList() {
        assertEquals(
            listOf(Quality.LOWEST, Quality.LOW, Quality.MEDIUM, Quality.HIGH, Quality.HIGHEST),
            Quality.LIST
        )
    }
}
