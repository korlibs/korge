package korlibs.logger.test

import korlibs.logger.Logger
import kotlin.test.Test

class Logger2Test {
    companion object {
        val mylogger = Logger("demo")
    }

    @Test
    fun test() {
        mylogger.level = Logger.Level.INFO
    }
}
