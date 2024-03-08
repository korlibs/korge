package korlibs.io.lang

import korlibs.logger.Console
import kotlin.test.Test
import kotlin.test.assertEquals

class ExceptionExtTest {
	@Test
	fun test() {
        val logs = Console.capture {
            printStackTrace()
        }
        assertEquals(1, logs.size)
	}
}
