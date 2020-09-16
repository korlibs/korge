package com.soywiz.klogger

private var Logger_loggers: LinkedHashMap<String, Logger> = LinkedHashMap()
private var Logger_defaultLevel: Logger.Level? = null
private var Logger_defaultOutput: Logger.Output = DefaultLogOutput

private var Logger_levels: LinkedHashMap<String, Logger.Level?> = LinkedHashMap()
private var Logger_outputs: LinkedHashMap<String, Logger.Output?> = LinkedHashMap()

/**
 * Utility to log messages.
 */
class Logger private constructor(val name: String, val dummy: Boolean) {
    init {
        Logger_loggers[name] = this
        Logger_levels[name] = null
        Logger_outputs[name] = null
    }

    /** [Level] of this [Logger]. If not set, it will use the [Logger.defaultLevel] */
    var level: Level
        set(value) = run { Logger_levels[name] = value }
        get() = Logger_levels[name] ?: Logger.defaultLevel ?: Level.WARN

    /** [Output] of this [Logger]. If not set, it will use the [Logger.defaultOutput] */
    var output: Output
        set(value) = run { Logger_outputs[name] = value }
        get() = Logger_outputs[name] ?: Logger.defaultOutput

    /** Check if the [level] is set for this [Logger] */
    val isLocalLevelSet: Boolean get() = Logger_levels[name] != null

    /** Check if the [output] is set for this [Logger] */
    val isLocalOutputSet: Boolean get() = Logger_outputs[name] != null

    companion object {
        /** The default [Level] used for all [Logger] that doesn't have its [Logger.level] set */
        var defaultLevel: Level?
            get() = Logger_defaultLevel
            set(value) = run { Logger_defaultLevel = value }

        /** The default [Output] used for all [Logger] that doesn't have its [Logger.output] set */
        var defaultOutput: Output
            get() = Logger_defaultOutput
            set(value) = run { Logger_defaultOutput = value }

        /** Gets a [Logger] from its [name] */
        operator fun invoke(name: String) = Logger_loggers[name] ?: Logger(name, true)

        /** Gets a [Logger] from its [KClass.simpleName] */
        inline operator fun <reified T : Any> invoke() = invoke(T::class.simpleName ?: "NoClassName")
    }

    /** Logging [Level] */
    enum class Level(val index: Int) {
        NONE(0), FATAL(1), ERROR(2),
        WARN(3), INFO(4), DEBUG(5), TRACE(6)
    }

    /** Logging [Output] to handle logs */
    interface Output {
        fun output(logger: Logger, level: Logger.Level, msg: Any?)
    }

    /** Default [Output] to emit logs over the [Console] */
    object ConsoleLogOutput : Logger.Output {
        override fun output(logger: Logger, level: Logger.Level, msg: Any?) {
            when (level) {
                Logger.Level.ERROR -> Console.error(logger.name, msg)
                else -> Console.log(logger.name, msg)
            }
        }
    }

    /** Returns if this [Logger] has at least level [Level] */
    fun isEnabled(level: Level) = level.index <= this.level.index

    /** Returns if this [Logger] has at least level [Level.FATAL] */
    inline val isFatalEnabled get() = isEnabled(Level.FATAL)

    /** Returns if this [Logger] has at least level [Level.ERROR] */
    inline val isErrorEnabled get() = isEnabled(Level.ERROR)

    /** Returns if this [Logger] has at least level [Level.WARN] */
    inline val isWarnEnabled get() = isEnabled(Level.WARN)

    /** Returns if this [Logger] has at least level [Level.INFO] */
    inline val isInfoEnabled get() = isEnabled(Level.INFO)

    /** Returns if this [Logger] has at least level [Level.DEBUG] */
    inline val isDebugEnabled get() = isEnabled(Level.DEBUG)

    /** Returns if this [Logger] has at least level [Level.TRACE] */
    inline val isTraceEnabled get() = isEnabled(Level.TRACE)

    /** Traces the lazily executed [msg] if the [Logger.level] is at least [level] */
    inline fun log(level: Level, msg: () -> Any?) = run { if (isEnabled(level)) actualLog(level, msg()) }

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

    @PublishedApi
    internal fun actualLog(level: Level, msg: Any?) = run { output.output(this, level, msg) }
}

/** Sets the [Logger.level] */
fun Logger.setLevel(level: Logger.Level): Logger = this.apply { this.level = level }

/** Sets the [Logger.output] */
fun Logger.setOutput(output: Logger.Output): Logger = this.apply { this.output = output }
