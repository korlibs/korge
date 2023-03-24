package korlibs.logger

import java.util.logging.Level

actual object DefaultLogOutput : Logger.Output {
    override fun output(logger: Logger, level: Logger.Level, msg: Any?) {
        if (logger.nativeLogger == null) {
            logger.nativeLogger = java.util.logging.Logger.getLogger(logger.name)
        }
        //println("logger=$logger, level=$level, msg=$msg")
        (logger.nativeLogger as java.util.logging.Logger).log(level.toJava(), msg.toString())
    }
}

fun Logger.Level.toJava(): Level = when (this) {
    Logger.Level.NONE -> Level.OFF
    Logger.Level.FATAL -> Level.SEVERE
    Logger.Level.ERROR -> Level.SEVERE
    Logger.Level.WARN -> Level.WARNING
    Logger.Level.INFO -> Level.FINE
    Logger.Level.DEBUG -> Level.FINEST
    Logger.Level.TRACE -> Level.ALL
}