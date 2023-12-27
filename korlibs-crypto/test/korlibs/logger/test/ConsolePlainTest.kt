package korlibs.logger.test

import korlibs.logger.Console
import kotlin.test.Ignore
import kotlin.test.Test

class ConsolePlainTest {
    @Test
    @Ignore // java.lang.RuntimeException: Method i in android.util.Log not mocked. See http://g.co/androidstudio/not-mocked for details
    fun test() {
        Console.log("log", "hello", "world", 42)
        Console.warn("warn", "hello", "world", 42)
        Console.error("warn", "hello", "world", 42)
    }
}
