package korlibs.logger

import java.util.logging.Level

actual object Console : BaseConsole() {
    override fun logInternal(kind: Kind, vararg msg: Any?) {
        val stream = if (kind == Kind.ERROR) System.err else System.out
        stream.println(logToString(kind, *msg))
    }

    override fun logToString(kind: Kind, vararg msg: Any?): String = buildString {
        val color = kind.color
        if (color != null) appendFgColor(color)
        append('#')
        append(Thread.currentThread().id)
        append(": ")
        msg.joinTo(this, ", ")
        if (color != null) appendReset()
    }
}

internal actual val miniEnvironmentVariables: Map<String, String> by lazy { System.getenv() }

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
