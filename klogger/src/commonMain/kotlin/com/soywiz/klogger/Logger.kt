package com.soywiz.klogger

import com.soywiz.klogger.atomic.*
import com.soywiz.klogger.internal.*

/**
 * Utility to log messages.
 *
 * In order to set loggers at runtime. Set environment variables:
 *
 * ```
 * LOG_$loggerName=debug
 * ```
 */
class Logger private constructor(val name: String, val dummy: Boolean) {
    val normalizedName = normalizeName(name)

    init {
        Logger_loggers[normalizedName] = Logger_loggers[normalizedName] ?: this
    }

    /** [Level] of this [Logger]. If not set, it will use the [Logger.defaultLevel] */
    var level: Level
        set(value) { Logger_levels[normalizedName] = value }
        get() = Logger_levels[normalizedName] ?: Logger.defaultLevel ?: Level.WARN

    /** [Output] of this [Logger]. If not set, it will use the [Logger.defaultOutput] */
    var output: Output
        set(value) { Logger_outputs[normalizedName] = value }
        get() = Logger_outputs[normalizedName] ?: Logger.defaultOutput

    /** Check if the [level] is set for this [Logger] */
    val isLocalLevelSet: Boolean get() = Logger_levels[normalizedName] != null

    /** Check if the [output] is set for this [Logger] */
    val isLocalOutputSet: Boolean get() = Logger_outputs[normalizedName] != null

    companion object {
        private val Logger_loggers: AtomicMap<String, Logger> = AtomicMap(emptyMap())
        private val Logger_levels: AtomicMap<String, Level?> by lazy {
            AtomicMap<String, Level?>(emptyMap()).also { Logger_levels ->
                //println("environmentVariables=${environmentVariables.keys}")
                for ((key, value) in miniEnvironmentVariables) {
                    if (key.startsWith("log_", ignoreCase = true)) {
                        val normalizedName = normalizeName(key.substring(4))
                        Logger_levels[normalizedName] = Level[value.uppercase()]
                    }
                }
            }
        }
        private val Logger_outputs: AtomicMap<String, Output?> = AtomicMap(emptyMap())

        /** The default [Level] used for all [Logger] that doesn't have its [Logger.level] set */
        var defaultLevel: Level? by KloggerAtomicRef(null)

        /** The default [Output] used for all [Logger] that doesn't have its [Logger.output] set */
        var defaultOutput: Output by KloggerAtomicRef(DefaultLogOutput)

        /** Gets a [Logger] from its [name] */
        operator fun invoke(name: String) = Logger_loggers[name] ?: Logger(name, true)

        private fun normalizeName(name: String): String = name.replace('.', '_').replace('/', '_').uppercase()

        /** Gets a [Logger] from its [KClass.simpleName] */
        inline operator fun <reified T : Any> invoke() = invoke(T::class.simpleName ?: "NoClassName")
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

    @PublishedApi
    internal fun actualLog(level: Level, msg: Any?) { output.output(this, level, msg) }
}

/** Sets the [Logger.level] */
fun Logger.setLevel(level: Logger.Level): Logger = this.apply { this.level = level }

/** Sets the [Logger.output] */
fun Logger.setOutput(output: Logger.Output): Logger = this.apply { this.output = output }

private typealias AtomicMap<K, V> = KloggerAtomicRef<Map<K, V>>

private inline operator fun <K, V> AtomicMap<K, V>.get(key: K) = value[key]
private inline operator fun <K, V> AtomicMap<K, V>.set(key: K, value: V) {
    this.update { HashMap(it).also { nmap -> nmap[key] = value } }
}

