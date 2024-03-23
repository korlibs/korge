package korlibs.logger

import android.util.Log

actual object Console : BaseConsole() {
    private var logIsWorking: Boolean? = null

    override fun logInternal(kind: Kind, vararg msg: Any?) {
        when (logIsWorking) {
            null -> {
                logIsWorking = try {
                    _logInternal(kind, *msg)
                    true
                } catch (e: Throwable) {
                    false
                }
            }
            true -> _logInternal(kind, *msg)
            false -> Unit
        }

    }

    private fun _logInternal(kind: Kind, vararg msg: Any?) {
        val tag = "Klogger"
        val str = logToString(kind, *msg)
        when (kind) {
            Kind.ERROR -> Log.e(tag, str)
            Kind.WARN -> Log.w(tag, str)
            Kind.INFO -> Log.i(tag, str)
            Kind.DEBUG -> Log.d(tag, str)
            Kind.TRACE -> Log.v(tag, str)
            Kind.LOG -> Log.i(tag, str)
        }
    }
}

actual object DefaultLogOutput : Logger.Output {
    actual override fun output(logger: Logger, level: Logger.Level, msg: Any?) {
        if (level == Logger.Level.NONE) return
        Log.println(when (level) {
            Logger.Level.NONE -> Log.VERBOSE
            Logger.Level.FATAL -> Log.ERROR
            Logger.Level.ERROR -> Log.ERROR
            Logger.Level.WARN -> Log.WARN
            Logger.Level.INFO -> Log.INFO
            Logger.Level.DEBUG -> Log.DEBUG
            Logger.Level.TRACE -> Log.VERBOSE
        }, logger.name, msg.toString())
    }
}

internal actual val miniEnvironmentVariables: Map<String, String> by lazy { System.getenv() }
