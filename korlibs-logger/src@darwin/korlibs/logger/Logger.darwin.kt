package korlibs.logger

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.Foundation.NSLog

actual object Console : BaseConsole() {
    override fun logInternal(kind: Kind, vararg msg: Any?) {
        NSLog("%s", logToString(kind, *msg))
    }

    override fun logToString(kind: Kind, vararg msg: Any?): String {
        return msg.joinToString(", ")
    }
}

actual object DefaultLogOutput : Logger.Output {
    actual override fun output(logger: Logger, level: Logger.Level, msg: Any?) = Logger.ConsoleLogOutput.output(logger, level, msg)
}

internal actual val miniEnvironmentVariables: Map<String, String> by lazy {
    autoreleasepool { NSProcessInfo.processInfo.environment.map { it.key.toString() to it.value.toString() }.toMap() }
}
