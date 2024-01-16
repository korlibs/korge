package korlibs.logger

import kotlinx.atomicfu.locks.*
import kotlin.time.*

/**
 * Utility to log messages.
 *
 * In order to set loggers at runtime. Set environment variables:
 *
 * ```
 * LOG_$loggerName=debug
 * ```
 *
 * For setting the default log level to something else:
 *
 * ```
 * LOG_LEVEL=debug
 * ```
 */
class Logger private constructor(val name: String, val normalizedName: String, val dummy: Boolean) {
    var nativeLogger: Any? = null

    var optLevel: Level? = null
    var optOutput: Output? = null

    /** [Level] of this [Logger]. If not set, it will use the [Logger.defaultLevel] */
    var level: Level
        get() = optLevel ?: defaultLevel ?: Level.WARN
        set(value) { optLevel = value }

    /** [Output] of this [Logger]. If not set, it will use the [Logger.defaultOutput] */
    var output: Output
        get() = optOutput ?: defaultOutput
        set(value) { optOutput = value }

    ///** Check if the [level] is set for this [Logger] */
    val isLocalLevelSet: Boolean get() = optLevel != null
    ///** Check if the [output] is set for this [Logger] */
    val isLocalOutputSet: Boolean get() = optOutput != null

    companion object {
        private val Logger_lock = SynchronizedObject()
        private val Logger_loggers = HashMap<String, Logger>()

        /** The default [Level] used for all [Logger] that doesn't have its [Logger.level] set */
        var defaultLevel: Level? = null

        /** The default [Output] used for all [Logger] that doesn't have its [Logger.output] set */
        var defaultOutput: Output = DefaultLogOutput

        /** Gets a [Logger] from its [name] */
        operator fun invoke(name: String): Logger = synchronized(Logger_lock) {
            val normalizedName = normalizeName(name)
            if (Logger_loggers[normalizedName] == null) {
                val logger = Logger(name, normalizedName, true)
                miniEnvironmentVariablesUC["LOG_$normalizedName"]?.also {
                    logger.level = Level[it]
                }
                if (Logger_loggers.isEmpty()) {
                    miniEnvironmentVariablesUC["LOG_LEVEL"]?.also {
                        defaultLevel = Level[it]
                    }
                }
                Logger_loggers[normalizedName] = logger
            }
            return Logger_loggers[normalizedName]!!
        }

        private fun normalizeName(name: String): String = name.replace('.', '_').replace('/', '_').uppercase()

        /** Gets a [Logger] from its [KClass.simpleName] */
        inline operator fun <reified T : Any> invoke(): Logger = invoke(T::class.simpleName ?: "NoClassName")
    }

    /** Logging [Level] */
    enum class Level(val index: Int) {
        NONE(0), FATAL(1), ERROR(2),
        WARN(3), INFO(4), DEBUG(5), TRACE(6);

        companion object {
            val BY_NAME = values().associateBy { it.name }
            operator fun get(name: String): Level = BY_NAME[name.uppercase()] ?: NONE
        }
    }

    /** Logging [Output] to handle logs */
    interface Output {
        fun output(logger: Logger, level: Level, msg: Any?)
    }

    /** Default [Output] to emit logs over the [Console] */
    object ConsoleLogOutput : Output {
        override fun output(logger: Logger, level: Level, msg: Any?) {
            when (level) {
                Level.ERROR -> Console.error(logger.name, msg)
                Level.WARN -> Console.warn(logger.name, msg)
                else -> Console.log(logger.name, msg)
            }
        }
    }

    /** Returns if this [Logger] has at least level [Level] */
    fun isEnabled(level: Level): Boolean = level.index <= this.level.index

    /** Returns if this [Logger] has at least level [Level.FATAL] */
    inline val isFatalEnabled: Boolean get() = isEnabled(Level.FATAL)

    /** Returns if this [Logger] has at least level [Level.ERROR] */
    inline val isErrorEnabled: Boolean get() = isEnabled(Level.ERROR)

    /** Returns if this [Logger] has at least level [Level.WARN] */
    inline val isWarnEnabled: Boolean get() = isEnabled(Level.WARN)

    /** Returns if this [Logger] has at least level [Level.INFO] */
    inline val isInfoEnabled: Boolean get() = isEnabled(Level.INFO)

    /** Returns if this [Logger] has at least level [Level.DEBUG] */
    inline val isDebugEnabled: Boolean get() = isEnabled(Level.DEBUG)

    /** Returns if this [Logger] has at least level [Level.TRACE] */
    inline val isTraceEnabled: Boolean get() = isEnabled(Level.TRACE)

    /** Traces the lazily executed [msg] if the [Logger.level] is at least [level] */
    inline fun log(level: Level, msg: () -> Any?) { if (isEnabled(level)) actualLog(level, msg()) }

    /** Traces the lazily executed [msg] if the [Logger.level] is at least [Level.FATAL] */
    inline fun fatal(msg: () -> Any?) = log(Level.FATAL, msg)

    /** Traces the lazily executed [msg] if the [Logger.level] is at least [Level.ERROR] */
    inline fun error(msg: () -> Any?) = log(Level.ERROR, msg)

    /** Traces the lazily executed [msg] if the [Logger.level] is at least [Level.WARN] */
    inline fun warn(msg: () -> Any?) = log(Level.WARN, msg)

    /** Traces the lazily executed [msg] if the [Logger.level] is at least [Level.INFO] */
    inline fun info(msg: () -> Any?) = log(Level.INFO, msg)

    /** Traces the lazily executed [msg] if the [Logger.level] is at least [Level.DEBUG] */
    inline fun debug(msg: () -> Any?) = log(Level.DEBUG, msg)

    /** Traces the lazily executed [msg] if the [Logger.level] is at least [Level.TRACE] */
    inline fun trace(msg: () -> Any?) = log(Level.TRACE, msg)

    inline fun <T> logTime(name: String, level: Level = Level.INFO, block: () -> T): T {
        val (value, time) = measureTimedValue(block)
        log(level) { "$name: $time" }
        return value
    }

    @PublishedApi
    internal fun actualLog(level: Level, msg: Any?) { output.output(this, level, msg) }
}

/** Sets the [Logger.level] */
fun Logger.setLevel(level: Logger.Level): Logger = this.apply { this.level = level }

/** Sets the [Logger.output] */
fun Logger.setOutput(output: Logger.Output): Logger = this.apply { this.output = output }

internal expect val miniEnvironmentVariables: Map<String, String> //= mapOf()
private var _miniEnvironmentVariablesUC: Map<String, String>? = null

internal val miniEnvironmentVariablesUC: Map<String, String> get() {
    if (_miniEnvironmentVariablesUC == null) {
        _miniEnvironmentVariablesUC = miniEnvironmentVariables.mapKeys { it.key.uppercase() }
    }
    return _miniEnvironmentVariablesUC!!
}

expect object DefaultLogOutput : Logger.Output
