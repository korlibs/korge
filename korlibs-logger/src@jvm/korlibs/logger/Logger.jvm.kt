package korlibs.logger

import korlibs.logger.Console.color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.logging.*

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
    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    override fun output(logger: Logger, level: Logger.Level, msg: Any?) {
        if (logger.nativeLogger == null) {
            logger.nativeLogger = java.util.logging.Logger.getLogger(logger.name).also { nativeLogger ->
                nativeLogger.useParentHandlers = true
                if (nativeLogger.handlers.isEmpty()) {
                    nativeLogger.addHandler(object : Handler() {
                        override fun publish(record: LogRecord) {
                            val out = if (record.level.intValue() >= Level.WARNING.intValue()) System.err else System.out
                            val color = when {
                                record.level.intValue() >= Level.SEVERE.intValue() -> AnsiEscape.Color.RED
                                record.level.intValue() >= Level.WARNING.intValue() -> AnsiEscape.Color.YELLOW
                                else -> AnsiEscape.Color.WHITE
                            }
                            val time = DATE_FORMAT.format(Date(System.currentTimeMillis()))
                            out.println("$time[${Thread.currentThread()}]: ${record.level}: ${record.loggerName} - ${record.message}".color(color))
                        }
                        override fun flush() = Unit
                        override fun close() = Unit
                    })
                }
            }
        }
        val nativeLogger = logger.nativeLogger as java.util.logging.Logger
        nativeLogger.level = logger.level.toJava()
        //println("logger.level=${logger.level}, nativeLogger.level=${nativeLogger.level}, level=$level")
        //println("logger=$logger, level=$level, msg=$msg")
        nativeLogger.log(level.toJava(), msg.toString())
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
