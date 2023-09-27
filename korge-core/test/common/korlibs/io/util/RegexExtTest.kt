package korlibs.io.util

import kotlin.test.Test
import kotlin.test.assertEquals

class RegexExtTest {
	@Test
	fun test() {
		assertEquals("""\*\+""", Regex.quote("*+"))
		assertEquals("""\.\?\*\+\^\\\[\]\(\)\{\}\|\-\${'$'}""", Regex.quote(""".?*+^\[](){}|-${'$'}"""))

		assertEquals("""abc\[a\]""", Regex.quote("abc[a]"))


		// @TODO: Inconsistent among targets! JS/JVM
		//assertEquals("""\Q*+\E""", Regex.escape("*+"))
		//assertEquals("""*+""", Regex.escapeReplacement("*+"))
		//assertEquals("""*+""", Regex.fromLiteral("*+").pattern)
	}

    @Test
    fun testGlob() {
        assertEquals(true, Regex.fromGlob("*.txt").matches("hello.txt"))
        assertEquals(false, Regex.fromGlob("*.txt").matches("hello.txt2"))
    }
}
