package korlibs.logger.test

import korlibs.logger.*
import kotlin.test.*

class AnsiEscapeTest {
    @Test
    fun testSimple() {
        println(AnsiEscape { ("hello".red.bold + " world".blue.underline) })
        println(AnsiEscape { "hello".color256(33).bgColor256(185) })
        println(AnsiEscape { "this is ${"heavy".bold}".underline.colorReversed })
    }

    @Test
    fun testColorSequence() {
        for (i in 0 until 16) {
            for (j in 0 until 16) {
                val code = i * 16 + j
                print(AnsiEscape { "$code".padStart(4, ' ').color256(code).bgColor256((code + 10) % 256) })
            }
            println()
        }
    }

    @Test
    fun testBgColorSequence() {
        for (i in 0 until 16) {
            for (j in 0 until 16) {
                val code = i * 16 + j
                print(AnsiEscape { "$code".padStart(4, ' ').bgColor256(code) })
            }
            println()
        }
    }
}
